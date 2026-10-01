package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.ExternalSources
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.ExternalInstaller
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallDialogActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallNotifications
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.UserActionBridge
import org.autojs.plugin.installer.api.InstallerContract as C
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/** Explicit opt-in real fixture installs. No user package, default or notification setting is changed. */
@RunWith(AndroidJUnit4::class)
class NotificationInstallFlowDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val arguments = InstrumentationRegistry.getArguments()
    private val notifications = context.getSystemService(NotificationManager::class.java)

    @Test fun realInstallationIsApprovedFromNotificationsAndReturnsTheInstalledPackage() {
        assumeTrue("Explicit -e notificationFlowAuthorizer shizuku|none is required",
            arguments.getString("notificationFlowAuthorizer") in listOf("shizuku", "none"))
        val authorizer = requireNotNull(Authorizer.fromId(arguments.getString("notificationFlowAuthorizer")))
        check(!context.getSystemService(KeyguardManager::class.java).isKeyguardLocked) { "Unlock the device before notification acceptance" }
        check(InstallNotifications.available(context)) { "Notification permission and channel must already be enabled" }
        check(InstallPresentation.snapshots().isEmpty()) { "An unrelated installation is active" }
        if (authorizer == Authorizer.SHIZUKU) check(AuthorizerStates.state(context, authorizer).usable) { "Shizuku must already be running and authorized" }
        if (authorizer == Authorizer.NONE && Build.VERSION.SDK_INT >= 26) check(context.packageManager.canRequestPackageInstalls()) {
            "Prepare the existing installation-permission journal before this none acceptance"
        }
        val preferred = NotificationFixtureAudit.preferred(instrumentation)
        val sessions = context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet()
        val folder = File(context.cacheDir, "p2-source-fixtures-${UUID.randomUUID()}")
        check(folder.mkdir())
        val source = File(folder, "fixture.apk")
        val bytes = instrumentation.context.assets.open("fixture-v1.apk").use { it.readBytes() }
        check(digest(bytes) == FIXTURE_SHA256) { "Only the fixed code-free fixture may be installed" }
        source.writeBytes(bytes)
        var record: InstallPresentation.Record? = null
        try {
            FixturePackageOwnership(setOf(FixtureInstallUi.PACKAGE_NAME)).use { packageOwnership ->
                FixtureHistoryOwnership(context).use { history ->
                    val uri = Uri.parse("content://${context.packageName}.source-fixtures/${folder.name}/fixture.apk")
                    val current = requireNotNull(InstallPresentation.find(ExternalInstaller.start(context,
                        ExternalSources.fromUris(listOf(uri), 0), InstallOptions(authorizer = authorizer.id, timeoutMillis = 180_000),
                        interaction = C.INTERACTION_NOTIFICATION)))
                    record = current
                    history.track(current)
                    await("The package was not presented in a notification", 20_000) {
                        checkNoInstallDialog(current.token)
                        check(!current.snapshot().terminal) { "Installation failed before approval: ${current.snapshot().failure}" }
                        current.snapshot().prompt?.metadata?.packageName == FixtureInstallUi.PACKAGE_NAME &&
                            notifications.activeNotifications.any { it.tag == "installation.action:${current.token}" }
                    }
                    assertFalse(installed())
                    val approval = notifications.activeNotifications.single { it.tag == "installation.action:${current.token}" }.notification
                    assertNull(approval.contentIntent)
                    assertEquals(2, approval.actions.size)
                    assertEquals(FixtureInstallUi.PACKAGE_NAME, current.snapshot().prompt!!.metadata.packageName)
                    packageOwnership.installationStarted()
                    approval.actions[0].actionIntent.send()
                    evidence("INITIAL_APPROVAL authorizer=${authorizer.id} token=${current.token} action=notificationPendingIntent package=${FixtureInstallUi.PACKAGE_NAME}")
                    var systemToken: String? = null
                    var platformSessionId: Int? = null
                    var ownedUi: OwnedUnknownSourceUi? = null
                    var systemApproved = false
                    var scanAccepted = false
                    var safeScanAccepted = false
                    val until = SystemClock.elapsedRealtime() + 170_000
                    while (!current.snapshot().terminal && SystemClock.elapsedRealtime() < until) {
                        checkNoInstallDialog(current.token)
                        if (systemToken == null) {
                            val action = notifications.activeNotifications.firstOrNull { notice ->
                                val token = notice.tag?.removePrefix("installation.action:")
                                notice.tag?.startsWith("installation.action:") == true && token != current.token &&
                                    token != null && UserActionBridge.activityIntent(context, token) != null
                            }
                            if (action != null) {
                                check(authorizer == Authorizer.NONE) { "The privileged fixture unexpectedly required system confirmation" }
                                val token = action.tag.removePrefix("installation.action:")
                                assertFalse(UserActionBridge.isAttached(token))
                                val platform = context.packageManager.packageInstaller.mySessions.single {
                                    it.appPackageName == FixtureInstallUi.PACKAGE_NAME && it.installerPackageName == context.packageName
                                }
                                platformSessionId = platform.sessionId
                                // A pending bridge must stay unattached before the notification action.
                                SystemClock.sleep(300)
                                assertFalse(UserActionBridge.isAttached(token))
                                checkNoInstallDialog(current.token)
                                systemToken = token
                                ownedUi = OwnedUnknownSourceUi(context, token, FixtureInstallUi.LABEL)
                                requireNotNull(action.notification.contentIntent).send()
                                evidence("SYSTEM_NOTIFICATION_OPENED token=$token sessionId=${platform.sessionId} noActivityBeforeTap=true action=notificationPendingIntent")
                            }
                        }
                        val confirmation = ownedUi
                        if (confirmation != null) {
                            if (!systemApproved && confirmation.canApproveFixture()) {
                                systemApproved = FixtureInstallUi.acceptSystemFixture(FixtureInstallUi.LABEL)
                            }
                            val id = platformSessionId
                            if (systemApproved && id != null && context.packageManager.packageInstaller.getSessionInfo(id) != null &&
                                arguments.getString("allowPlayProtectScan") == "true") {
                                fun verifySession() {
                                    val active = requireNotNull(context.packageManager.packageInstaller.getSessionInfo(id))
                                    check(active.appPackageName == FixtureInstallUi.PACKAGE_NAME && active.installerPackageName == context.packageName)
                                    check(digest(source.readBytes()) == FIXTURE_SHA256)
                                }
                                if (!scanAccepted && !safeScanAccepted) scanAccepted = confirmation.handleFixedFixtureScan(true, ::verifySession)
                                if (!safeScanAccepted) safeScanAccepted = confirmation.acceptFixedFixtureSafeScan(::verifySession)
                            }
                        }
                        SystemClock.sleep(40)
                    }
                    val outcome = current.snapshot()
                    assertTrue("Notification install did not reach a terminal result", outcome.terminal)
                    assertNull("Notification install failed: ${outcome.failure}", outcome.failure)
                    val result = requireNotNull(outcome.items.single().result)
                    assertTrue(result.toString(), result.get(C.FIELD_OK).asBoolean)
                    assertEquals(C.INTERACTION_NOTIFICATION, result.get(C.FIELD_INTERACTION).asString)
                    assertEquals(authorizer.id, result.get(C.FIELD_AUTHORIZER).asString)
                    assertEquals(FixtureInstallUi.PACKAGE_NAME, result.get(C.FIELD_PACKAGE_NAME).asString)
                    val installed = context.packageManager.getPackageInfo(FixtureInstallUi.PACKAGE_NAME, 0)
                    @Suppress("DEPRECATION") val version = if (Build.VERSION.SDK_INT >= 28) installed.longVersionCode else installed.versionCode.toLong()
                    assertEquals(1L, version)
                    assertEquals(authorizer == Authorizer.NONE, systemApproved)
                    checkNoInstallDialog(current.token)
                    await("The result notification was not published", 5_000) {
                        notifications.activeNotifications.any { it.tag == "installation.result:${current.token}" }
                    }
                    assertNull(notifications.activeNotifications.single { it.tag == "installation.result:${current.token}" }.notification.contentIntent)
                    assertEquals(FIXTURE_SHA256, digest(source.readBytes()))
                    evidence("SUCCESS authorizer=${authorizer.id} token=${current.token} versionCode=$version interaction=notification systemApproved=$systemApproved " +
                        "scanAccepted=$scanAccepted safeScanAccepted=$safeScanAccepted noInstallDialog=true sourceRetained=true")
                }
            }
        } finally {
            record?.close()
            await("The owned platform session was not released", 10_000) {
                context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet() == sessions
            }
            assertEquals("A package default/last-chosen record changed", preferred, NotificationFixtureAudit.preferred(instrumentation))
            check(source.isFile && digest(source.readBytes()) == FIXTURE_SHA256)
            check(source.delete() && folder.delete())
        }
    }

    @Test fun disabledNotificationCapabilityRefusesARealRequestWithoutInstallingOrOpeningASource() {
        assumeTrue("Explicit -e notificationUnavailableFixture true on an isolated test device is required",
            arguments.getString("notificationUnavailableFixture") == "true")
        check(!InstallNotifications.available(context)) { "The controlled unavailable notification state has not been prepared" }
        FixturePackageOwnership(setOf(FixtureInstallUi.PACKAGE_NAME)).use {
            FixtureHistoryOwnership(context).use {
                val before = context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet()
                val failure = assertThrows(InstallFailure::class.java) {
                    ExternalInstaller.start(context, ExternalSources.fromUris(listOf(Uri.parse("content://${context.packageName}.source-fixtures/p2-source-fixtures-00000000/fixture.apk")), 0),
                        InstallOptions(authorizer = "none"), interaction = C.INTERACTION_NOTIFICATION)
                }
                assertEquals(InstallerErrorCodes.NOTIFICATION_UNAVAILABLE, failure.code)
                assertEquals(before, context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet())
                assertFalse(installed())
                evidence("UNAVAILABLE_REFUSED code=${failure.code} sourceOpened=false platformSessionCreated=false historyUnchanged=true")
            }
        }
    }

    private fun installed(): Boolean = try { context.packageManager.getPackageInfo(FixtureInstallUi.PACKAGE_NAME, 0); true }
        catch (_: PackageManager.NameNotFoundException) { false }

    private fun checkNoInstallDialog(token: String) = instrumentation.runOnMainSync {
        val stages = listOf(Stage.CREATED, Stage.STARTED, Stage.RESUMED, Stage.PAUSED, Stage.STOPPED)
        assertFalse(stages.flatMap { ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(it) }
            .filterIsInstance<InstallDialogActivity>().any { it.intent.getStringExtra(InstallPresentation.EXTRA_TOKEN) == token })
    }

    private fun await(message: String, timeout: Long, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeout
        while (!condition()) { if (SystemClock.elapsedRealtime() >= deadline) fail(message); SystemClock.sleep(25) }
    }

    private fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
        .bufferedReader().use { it.readText() }
    private fun evidence(value: String) = instrumentation.sendStatus(0, Bundle().apply { putString("notification-flow", value) })
    private fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private companion object { const val FIXTURE_SHA256 = "fec583a389978fdc298e9d85979b95feceb93e6fb3fd3f581511c826401e8b69" }
}
