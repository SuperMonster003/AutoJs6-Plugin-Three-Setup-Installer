package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.*
import io.github.supermonster003.autojs6.plugin.three.setup.installer.policy.*
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.*
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.*
import org.autojs.plugin.installer.api.InstallerContract as C
import org.autojs.plugin.installer.api.InstallerErrorCodes as E
import org.autojs.plugin.packagearchive.PackageDeviceSpec
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.Closeable
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/** Fixed, hash-locked fixtures only. All settings/history cleanup checks its exact ownership. */
@RunWith(AndroidJUnit4::class)
@androidx.test.filters.SdkSuppress(minSdkVersion = 28)
class InstallSafetyPolicyDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val authorizer = Authorizer.DHIZUKU

    @Test fun blacklistsRefuseEveryModeAndDeclaredSharedUidBeforeWriting() {
        ready()
        FixturePackageOwnership(setOf(PACKAGE, SHARED_PACKAGE)).use { owner ->
            owner.installationStarted()
            Rules().use { rules ->
                val before = activeSessions()
                rules.set(setOf(PACKAGE), emptySet())
                for (mode in listOf(C.INTERACTION_AUTO, C.INTERACTION_DIALOG, C.INTERACTION_SILENT, C.INTERACTION_NOTIFICATION)) {
                    external("v1.apk", mode) { record, _ ->
                        terminal(record)
                        assertEquals(E.BLOCKED_BY_POLICY, record.snapshot().failure?.code)
                        assertNull(record.snapshot().safetyReview)
                        assertEquals(before, activeSessions())
                    }
                }
                rules.set(emptySet(), setOf(SHARED))
                external("shared.apk", C.INTERACTION_DIALOG, SHARED_PACKAGE) { record, _ ->
                    terminal(record)
                    assertEquals(E.BLOCKED_BY_POLICY, record.snapshot().failure?.code)
                    assertEquals(before, activeSessions())
                }
                evidence("blacklistModes=4 declaredSharedUidBlocked=true platformSessionsUnchanged=true")
            }
        }
    }

    @Test fun realDialogNeedsItsUncheckedAcknowledgementAndAndroidStillRejectsDifferentSigningKeys() {
        ready()
        FixturePackageOwnership(setOf(PACKAGE)).use { owner ->
            owner.installationStarted()
            Rules().use {
                installVersionOne()
                external("v2-other.apk", C.INTERACTION_DIALOG) { record, _ ->
                    await { record.snapshot().prompt != null }
                    click(record, "install_permissions_expand")
                    onDialog(record) { activity ->
                        val permissions = activity.window.decorView.findViewWithTag<TextView>("install_permissions_list").text.toString()
                        assertTrue(permissions.contains("android.permission.READ_CALENDAR"))
                        assertTrue(permissions.contains("android.permission.INTERNET"))
                    }
                    click(record, InstallDialogActivity.TAG_CONFIRM)
                    await { record.snapshot().safetyReview != null }
                    waitControl(record, "install_signature_continue")
                    val review = requireNotNull(record.snapshot().safetyReview)
                    assertEquals(SignatureRisk.MISMATCH, review.binding.signatureRisk)
                    val before = activeSessions()
                    onDialog(record) { activity ->
                        val button = activity.window.decorView.findViewWithTag<View>("install_signature_continue")
                        assertFalse(button.isEnabled)
                        button.performClick()
                    }
                    SystemClock.sleep(100)
                    assertSame(review, record.snapshot().safetyReview)
                    assertEquals(before, activeSessions())
                    acknowledge(record)
                    terminal(record)
                    assertEquals(E.SIGNATURE_MISMATCH, record.snapshot().failure?.code)
                    assertEquals(1L, installedVersion())
                    assertEquals(before, activeSessions())
                    evidence("dialogUncheckedBlocked=true permissionsExpanded=true explicitOverrideReachedPlatform=true platformRejectedDifferentSigner=true installedVersion=1")
                }
            }
        }
    }

    @Test fun anAcknowledgedReviewCannotSurviveAChangedRuleRevision() {
        ready()
        FixturePackageOwnership(setOf(PACKAGE)).use { owner ->
            owner.installationStarted()
            Rules().use { rules ->
                installVersionOne()
                external("v2-other.apk", C.INTERACTION_DIALOG) { record, _ ->
                    await { record.snapshot().prompt != null }
                    click(record, InstallDialogActivity.TAG_CONFIRM)
                    await { record.snapshot().safetyReview != null }
                    val before = activeSessions()
                    rules.set(emptySet(), emptySet())
                    acknowledge(record)
                    terminal(record)
                    assertEquals(E.BLOCKED_BY_POLICY, record.snapshot().failure?.code)
                    assertEquals(1L, installedVersion())
                    assertEquals(before, activeSessions())
                    evidence("changedPolicyRevisionRejected=true installedVersion=1 platformSessionsUnchanged=true noSecondReview=true")
                }
            }
        }
    }

    @Test fun platformSignatureVerificationUsesAPrivateSnapshotOfTheHashedBytes() {
        ready()
        val root = File(context.cacheDir, "p9-signature-snapshot-${UUID.randomUUID()}").apply { check(mkdir()) }
        val source = File(context.cacheDir, "p9-mutable-base-${UUID.randomUUID()}.apk")
        try {
            source.writeBytes(asset("v1.apk"))
            val original = source.readBytes()
            val gate = InstallSafetyGate(context, "snapshot-audit", stagingRoot = { root })
            val method = gate.javaClass.getDeclaredMethod("stableBase", PlannedApk::class.java, String::class.java, kotlin.jvm.functions.Function0::class.java).apply { isAccessible = true }
            val stable = method.invoke(gate, PlannedApk("base.apk", source, source.length(), null, digest(original)), PACKAGE, {})
            val file = stable.javaClass.getDeclaredMethod("getFile").apply { isAccessible = true }.invoke(stable) as File
            try {
                assertNotEquals(source.canonicalFile, file.canonicalFile)
                assertTrue(file.canonicalPath.startsWith(root.canonicalPath + File.separator))
                source.writeBytes(asset("v2-other.apk"))
                assertEquals(HASHES.getValue("v1.apk"), digest(file.readBytes()))
                @Suppress("DEPRECATION") val checked = requireNotNull(context.packageManager.getPackageArchiveInfo(file.path,
                    android.content.pm.PackageManager.GET_SIGNATURES or android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES))
                assertEquals(PACKAGE, checked.packageName)
                assertEquals(1L, checked.longVersionCode)
                assertTrue(checked.signingInfo!!.apkContentsSigners.isNotEmpty())
                evidence("privateSnapshotRetainedOriginalBytes=true externalInodeChanged=true platformVerifiedVersion=1")
            } finally { (stable as Closeable).close() }
        } finally { check(!source.exists() || source.delete()); check(root.deleteRecursively()) }
    }

    private fun ready() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("policyFixtures") == "true")
        check(Build.VERSION.SDK_INT >= 28 && Process.myUid() / 100000 == 0)
        check(AuthorizerStates.state(context, authorizer).usable) { "The pre-authorized Dhizuku fixture device is required" }
        check(InstallPresentation.snapshots().isEmpty())
        check(InstallNotifications.available(context))
    }

    private fun installVersionOne() {
        val folder = File(context.cacheDir, "p9-policy-seed-${UUID.randomUUID()}").apply { check(mkdir()) }
        try {
            val file = File(folder, "fixture.apk").apply { writeBytes(asset("v1.apk")) }
            val prepared = ArchiveOpener.open(file, file.name, PackageDeviceSpec.from(context), folder, true)
            prepared.failure()?.let { throw it }
            DhizukuInstallEngine(context).install(InstallEngine.Request(prepared, InstallOptions(authorizer = authorizer.id), 0, C.INTERACTION_SILENT),
                object : InstallEngine.Listener { override fun onUserAction(intent: Intent) = fail("The owner fixture must not require confirmation") })
            assertEquals(1L, installedVersion())
        } finally { check(folder.deleteRecursively()) }
    }

    private fun external(name: String, mode: String, packageName: String = PACKAGE, action: (InstallPresentation.Record, File) -> Unit) {
        val folder = File(context.cacheDir, "p2-source-fixtures-${UUID.randomUUID()}").apply { check(mkdir()) }
        val file = File(folder, "fixture.apk").apply { writeBytes(asset(name)) }
        try {
            FixtureHistoryOwnership(context, packageName).use { history ->
                val uri = Uri.parse("content://${context.packageName}.source-fixtures/${folder.name}/fixture.apk")
                val token = ExternalInstaller.start(context, ExternalSources.fromUris(listOf(uri), 0),
                    InstallOptions(authorizer = authorizer.id, deleteSource = true, timeoutMillis = 30_000), interaction = mode)
                val record = requireNotNull(InstallPresentation.find(token))
                history.track(record)
                action(record, file)
                assertTrue(file.isFile)
                assertEquals(HASHES.getValue(name), digest(file.readBytes()))
                val deletion = context.contentResolver.call(uri, "fixtureDeletionState", uri.toString(), null)
                assertEquals(0, requireNotNull(deletion).getInt("deleteAttempts"))
            }
        } finally { check(folder.deleteRecursively()) }
    }

    private fun acknowledge(record: InstallPresentation.Record) {
        waitControl(record, "install_signature_acknowledge")
        onDialog(record) { activity ->
            activity.window.decorView.findViewWithTag<CompoundButton>("install_signature_acknowledge").isChecked = true
            val button = activity.window.decorView.findViewWithTag<View>("install_signature_continue")
            assertTrue(button.isEnabled)
            button.performClick()
        }
    }
    private fun click(record: InstallPresentation.Record, tag: String) {
        waitControl(record, tag)
        onDialog(record) { activity -> requireNotNull(activity.window.decorView.findViewWithTag<View>(tag)).performClick() }
    }
    private fun waitControl(record: InstallPresentation.Record, tag: String) = await {
        var present = false
        onDialog(record) { present = it.window.decorView.findViewWithTag<View>(tag) != null }
        present
    }
    private fun onDialog(record: InstallPresentation.Record, action: (InstallDialogActivity) -> Unit) {
        var owner: InstallDialogActivity? = null
        await { instrumentation.runOnMainSync {
            owner = listOf(Stage.RESUMED, Stage.STARTED).flatMap { ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(it) }
                .filterIsInstance<InstallDialogActivity>().firstOrNull { it.intent.getStringExtra(InstallPresentation.EXTRA_TOKEN) == record.token && !it.isFinishing }
        }; owner != null }
        instrumentation.runOnMainSync { action(requireNotNull(owner)) }
    }
    private fun terminal(record: InstallPresentation.Record) = await { record.snapshot().terminal }
    private fun await(condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (!condition()) { check(SystemClock.elapsedRealtime() < deadline) { "Policy fixture did not settle" }; SystemClock.sleep(30) }
    }
    @Suppress("DEPRECATION") private fun installedVersion() = context.packageManager.getPackageInfo(PACKAGE, 0).longVersionCode
    private fun activeSessions(): String {
        val dump = android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("dumpsys package")).bufferedReader().use { it.readText() }
        check(dump.contains("Active install sessions:") && dump.contains("Historical install sessions:"))
        return dump.substringAfter("Active install sessions:").substringBefore("Historical install sessions:").substringBefore("Finalized install sessions:").trim()
    }
    private fun asset(name: String) = instrumentation.context.assets.open("advanced-fixtures/$name").use { it.readBytes() }.also { assertEquals(HASHES.getValue(name), digest(it)) }
    private fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun evidence(message: String) = instrumentation.sendStatus(0, Bundle().apply { putString("policy-fixture", message) })

    private inner class Rules : Closeable {
        private val file = File(context.applicationInfo.dataDir, "shared_prefs/installation_policy.xml")
        private val journal = File(context.filesDir, "p9-policy-${UUID.randomUUID()}.json")
        private var owned = InstallSafetyPreferences.read(context)
        private val proof = JsonObject().apply { addProperty("kind", "p9-policy-fixture"); addProperty("uid", Process.myUid()); addProperty("fingerprint", Build.FINGERPRINT); addProperty("originalFileAbsent", true) }
        init {
            check(!file.exists() && !File(file.path + ".bak").exists() && owned == InstallSafetyPolicy()) { "Existing policy settings must be preserved" }
            check(journal.createNewFile())
            persist()
        }
        private fun persist() { journal.outputStream().use { it.write(proof.toString().toByteArray()); it.fd.sync() } }
        fun set(packages: Set<String>, sharedUsers: Set<String>) {
            proof.addProperty("nextPackages", InstallSafetyPolicy.encode(packages)); proof.addProperty("nextSharedUsers", InstallSafetyPolicy.encode(sharedUsers)); persist()
            check(InstallSafetyPreferences.save(context, owned, packages, sharedUsers))
            owned = InstallSafetyPreferences.read(context)
            proof.addProperty("ownedRevision", owned.revision); persist()
        }
        override fun close() {
            check(InstallSafetyPreferences.read(context) == owned) { "Policy changed outside the fixture; keeping the recovery journal" }
            check(context.deleteSharedPreferences("installation_policy"))
            check(!file.exists() && !File(file.path + ".bak").exists() && InstallSafetyPreferences.read(context) == InstallSafetyPolicy())
            check(journal.delete())
        }
    }

    companion object {
        private const val PACKAGE = "io.github.supermonster003.autojs6.installer.advanced.fixture"
        private const val SHARED_PACKAGE = "io.github.supermonster003.autojs6.installer.advanced.shared.fixture"
        private const val SHARED = "io.github.supermonster003.autojs6.installer.advanced.shared"
        private val HASHES = mapOf("v1.apk" to "bae693d55c5dd912ef22e2e87e5a3a6fb83a40dac15318fbca8a230e00b577a0",
            "v2-other.apk" to "171e52e90eac7b93d9406732fd7960ce47eea0c500a23f6089c7eb3ef8e8ad3e",
            "shared.apk" to "7833a3851db3fadfd3f6b4723a5b20f05b1ae2cd903aeec97f19be89e8ef84ad")
    }
}
