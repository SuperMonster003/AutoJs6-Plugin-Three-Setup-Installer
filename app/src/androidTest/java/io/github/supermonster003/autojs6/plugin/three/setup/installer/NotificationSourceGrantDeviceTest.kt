package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.notificationfixture.NotificationSourceProvider
import io.github.supermonster003.autojs6.plugin.three.setup.installer.notificationfixture.NotificationSourceRelayActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.ExternalInstallActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallNotifications
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallForegroundService
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation
import org.autojs.plugin.installer.api.InstallerContract as C
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID

/** Real cross-UID source grants through the production NoDisplay entry; cancellation precedes commit. */
@RunWith(AndroidJUnit4::class)
class NotificationSourceGrantDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val notifications = context.getSystemService(NotificationManager::class.java)

    @Test fun sourceCanBeReopenedAfterNoDisplayFinishesAndItsServiceReleasesTheGrantAtCompletion() {
        assumeTrue("Explicit -e notificationGrantFixture true is required", InstrumentationRegistry.getArguments().getString("notificationGrantFixture") == "true")
        check(InstallNotifications.available(context)) { "Notifications must already be allowed" }
        check(InstallPresentation.snapshots().isEmpty()) { "Another installation is active" }
        val preferences = context.getSharedPreferences("installer_settings", Context.MODE_PRIVATE)
        // The selected acceptance device has no stored preferences. Restrict the temporary
        // override to this exact case instead of replacing an existing user's choices.
        check(preferences.all.isEmpty()) { "This isolated source-grant test requires the recorded empty preference baseline" }
        val preferenceFile = File(context.applicationInfo.dataDir, "shared_prefs/installer_settings.xml")
        val hadPreferenceFile = preferenceFile.exists()
        val beforePreferred = NotificationFixtureAudit.preferred(instrumentation)
        val beforeSessions = context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet()
        val beforeTokens = InstallPresentation.snapshots(true).map { it.token }.toSet()
        val nonce = UUID.randomUUID().toString()
        val testPackage = instrumentation.context.packageName
        val authority = "$testPackage.notification-source"
        val uri = Uri.parse("content://$authority/$nonce/fixture.apk")
        val provider = requireNotNull(context.packageManager.resolveContentProvider(authority, 0))
        assertFalse(provider.exported)
        assertNotEquals(Process.myUid(), provider.applicationInfo.uid)
        assertEquals(PackageManager.PERMISSION_DENIED, context.checkUriPermission(uri, Process.myPid(), Process.myUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION))
        assertCannotRead(uri)
        val journal = File(context.filesDir, "p8-notification-grant-$nonce.json")
        check(journal.createNewFile())
        val saved = JsonObject().apply {
            addProperty("kind", "notification-grant-fixture")
            addProperty("packageName", context.packageName)
            addProperty("uid", Process.myUid())
            addProperty("sdk", Build.VERSION.SDK_INT)
            addProperty("fingerprint", Build.FINGERPRINT)
            addProperty("nonce", nonce)
            addProperty("sourceUri", uri.toString())
            addProperty("preferenceFileExisted", hadPreferenceFile)
            addProperty("originalPreferencesEmpty", true)
            addProperty("status", "pending")
        }
        fun persist() = journal.outputStream().use { stream -> stream.write(saved.toString().toByteArray()); stream.fd.sync() }
        persist()
        var record: InstallPresentation.Record? = null
        var primaryFailure: Throwable? = null
        try {
            check(preferences.edit().putString("default_interaction", C.INTERACTION_NOTIFICATION).commit())
            FixturePackageOwnership(setOf(FixtureInstallUi.PACKAGE_NAME)).use { fixture ->
                FixtureHistoryOwnership(context).use { history ->
                    fixture.installationStarted()
                    val reply = shell("am start -W -n $testPackage/${NotificationSourceRelayActivity::class.java.name} -d $uri")
                    check(reply.contains("Status: ok")) { "The grant-owning relay did not start: $reply" }
                    await("The external request did not create its presentation", 10_000) {
                        val candidates = InstallPresentation.snapshots(true).filter { it.token !in beforeTokens }
                        check(candidates.size <= 1) { "Another request started while the fixture source was being delivered" }
                        candidates.singleOrNull()?.let { record = InstallPresentation.find(it.token) }
                        record != null
                    }
                    val current = requireNotNull(record)
                    history.track(current)
                    saved.addProperty("token", current.token)
                    persist()
                    await("The fixed cross-UID source was not parsed for notification confirmation", 15_000) {
                        check(!current.snapshot().terminal) { "Cross-UID source failed: ${current.snapshot().failure}" }
                        current.snapshot().prompt?.metadata?.packageName == FixtureInstallUi.PACKAGE_NAME
                    }
                    await("The NoDisplay entry did not finish", 5_000) { noExternalActivity() }
                    await("The notification source service is not foreground", 5_000) {
                        notifications.activeNotifications.any { it.id == InstallNotifications.FOREGROUND_ID && it.tag == null &&
                            it.notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0 }
                    }
                    SystemClock.sleep(300)
                    assertTrue(noExternalActivity())
                    assertEquals(PackageManager.PERMISSION_GRANTED, context.checkUriPermission(uri, Process.myPid(), Process.myUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION))
                    val reopened = requireNotNull(context.contentResolver.openFileDescriptor(uri, "r"))
                    val bytes = ParcelFileDescriptor.AutoCloseInputStream(reopened).use { it.readBytes() }
                    assertEquals(NotificationSourceProvider.SHA256, NotificationSourceProvider.digest(bytes))
                    context.contentResolver.query(uri, arrayOf("provider_uid", "caller_uid"), null, null, null)!!.use { cursor ->
                        assertTrue(cursor.moveToFirst())
                        assertEquals(provider.applicationInfo.uid, cursor.getInt(0))
                        assertEquals(Process.myUid(), cursor.getInt(1))
                    }
                    assertEquals(beforeSessions, context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet())
                    evidence("SOURCE_REOPENED nonce=$nonce token=${current.token} providerUid=${provider.applicationInfo.uid} callerUid=${Process.myUid()} " +
                        "externalActivityDestroyed=true foreground=true sha256=${NotificationSourceProvider.SHA256} platformSessionCreated=false")
                    // A queued stale service start must not stop another legitimate writer or
                    // revoke the URI grants belonging to its earlier StartItem.
                    val stale = Intent(context, InstallForegroundService::class.java)
                        .putExtra(InstallPresentation.EXTRA_TOKEN, UUID.randomUUID().toString())
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).apply {
                            clipData = ClipData.newRawUri("Fixed stale service-start fixture", uri)
                        }
                    if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(stale) else context.startService(stale)
                    instrumentation.waitForIdleSync()
                    SystemClock.sleep(350)
                    assertFalse(current.snapshot().terminal)
                    current.checkNotificationAvailable()
                    assertTrue(notifications.activeNotifications.any { it.id == InstallNotifications.FOREGROUND_ID && it.tag == null })
                    assertEquals(PackageManager.PERMISSION_GRANTED, context.checkUriPermission(uri, Process.myPid(), Process.myUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION))
                    assertEquals(beforeSessions, context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet())
                    evidence("STALE_SERVICE_START_IGNORED nonce=$nonce currentToken=${current.token} foregroundPreserved=true grantPreserved=true noInstall=true")
                    val notice = notifications.activeNotifications.single { it.tag == "installation.action:${current.token}" }.notification
                    assertNull(notice.contentIntent)
                    assertEquals(2, notice.actions.size)
                    notice.actions[1].actionIntent.send()
                    await("The cancelled grant fixture did not finish", 10_000) { current.snapshot().terminal }
                    assertEquals(InstallerErrorCodes.USER_CANCELLED, current.snapshot().failure?.code)
                }
            }
            try {
                await("The foreground service did not release its source URI grant", 10_000) {
                    notifications.activeNotifications.none { it.id == InstallNotifications.FOREGROUND_ID && it.tag == null } &&
                        context.checkUriPermission(uri, Process.myPid(), Process.myUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION) == PackageManager.PERMISSION_DENIED
                }
            } catch (failure: Throwable) {
                evidence("SOURCE_RELEASE_WAIT_FAILED nonce=$nonce foregroundNotice=${notifications.activeNotifications.any { it.id == InstallNotifications.FOREGROUND_ID && it.tag == null }} " +
                    "uriPermission=${context.checkUriPermission(uri, Process.myPid(), Process.myUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION)}")
                val permissions = shell("dumpsys activity permissions").lines()
                val index = permissions.indexOfFirst { it.contains(nonce) }
                if (index >= 0) evidence("SOURCE_GRANT_OWNERS " + permissions.subList((index - 2).coerceAtLeast(0), (index + 12).coerceAtMost(permissions.size)).joinToString(" | "))
                throw failure
            }
            assertCannotRead(uri)
            evidence("SOURCE_RELEASED nonce=$nonce grantDenied=true foregroundStopped=true noInstall=true")
        } catch (failure: Throwable) {
            primaryFailure = failure
            throw failure
        } finally {
            try {
            record?.close()
            check(preferences.all == mapOf("default_interaction" to C.INTERACTION_NOTIFICATION)) {
                "Preferences changed outside the fixture; preserving the recovery journal instead of overwriting them"
            }
            check(preferences.edit().remove("default_interaction").commit() && preferences.all.isEmpty())
            if (!hadPreferenceFile) {
                check(!File(preferenceFile.path + ".bak").exists())
                check(!preferenceFile.exists() || preferenceFile.delete()) { "Cannot remove the test-created empty preference file" }
            }
            assertEquals(beforeSessions, context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet())
            assertEquals(beforePreferred, NotificationFixtureAudit.preferred(instrumentation))
            val sourcePath = "cache/notification-source-$nonce.apk"
            // UiAutomation uses Runtime.exec argument splitting, not an interactive shell.
            // Keep commands flat; shell quote syntax would be passed literally to run-as.
            check(shell("run-as $testPackage id").contains("uid=${provider.applicationInfo.uid}"))
            val present = shell("run-as $testPackage find cache -maxdepth 1 -name notification-source-$nonce.apk").trim()
            check(present.isEmpty() || present == sourcePath) { "Unexpected owned-provider source: $present" }
            if (present == sourcePath) {
                check(shell("run-as $testPackage sha256sum $sourcePath").trim().startsWith(NotificationSourceProvider.SHA256))
                check(shell("run-as $testPackage rm $sourcePath").isBlank())
            }
            saved.addProperty("status", "restored")
            persist()
            evidence("GRANT_CLEANUP nonce=$nonce journal=${journal.absolutePath} preferencesRestored=true preferredUnchanged=true sessionsUnchanged=true")
            } catch (cleanupFailure: Throwable) {
                if (primaryFailure != null) primaryFailure.addSuppressed(cleanupFailure) else throw cleanupFailure
            }
        }
    }

    private fun noExternalActivity(): Boolean {
        var absent = false
        instrumentation.runOnMainSync {
            absent = listOf(Stage.CREATED, Stage.STARTED, Stage.RESUMED, Stage.PAUSED, Stage.STOPPED)
                .flatMap { ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(it) }
                .none { it is ExternalInstallActivity && !it.isDestroyed }
        }
        return absent
    }
    private fun assertCannotRead(uri: Uri) {
        assertEquals(PackageManager.PERMISSION_DENIED, context.checkUriPermission(uri, Process.myPid(), Process.myUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION))
        assertNotNull(context.packageManager.resolveContentProvider(uri.authority!!, 0))
        val denied = assertThrows(Exception::class.java) { context.contentResolver.openFileDescriptor(uri, "r")?.close() }
        // Sony's CTA layer turns the framework's logged non-exported-provider SecurityException
        // into a null provider holder. Accept only that exact facade, not arbitrary source IO errors.
        assertTrue(denied.toString(), denied is SecurityException ||
            denied is FileNotFoundException && denied.message == "No content provider: $uri")
    }
    private fun await(message: String, timeout: Long, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeout
        while (!condition()) { if (SystemClock.elapsedRealtime() >= deadline) fail(message); SystemClock.sleep(25) }
    }
    private fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
        .bufferedReader().use { it.readText() }
    private fun evidence(value: String) = instrumentation.sendStatus(0, Bundle().apply { putString("notification-grant", value) })
}
