package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.KeyguardManager
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.view.KeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.ExternalSources
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.ExternalInstaller
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallDialogActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.UserActionActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.UserActionBridge
import org.junit.Assert.*
import org.junit.AssumptionViolatedException
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.Closeable
import java.security.MessageDigest
import java.util.zip.ZipFile

/** Opt-in checks of the real unknown-source Settings flow; permission changes are restored. */
@RunWith(AndroidJUnit4::class)
class UnknownSourcePermissionDeviceTest {
    @Test fun grantingUnknownSourcePermissionInSettingsContinuesTheSameNoneInstallation() {
        assumeTrue(Build.VERSION.SDK_INT >= 26 && InstrumentationRegistry.getArguments().getString("unknownSourceGrant") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assumeTrue("Unlock the test device", !context.getSystemService(KeyguardManager::class.java).isKeyguardLocked)
        UnknownSourcePermissionState(context).useWithVerifiedCleanup { permission ->
            FixturePackageOwnership(setOf(FixtureInstallUi.PACKAGE_NAME)).use { ownership ->
                UnknownSourceGrantTrace(context).use { trace ->
                    val folder = File(context.cacheDir, "unknown-source-grant-${System.nanoTime()}").apply { check(mkdir()) }
                    val source = File(folder, "fixture.apk")
                    var record: InstallPresentation.Record? = null
                    var permissionUi: OwnedUnknownSourceUi? = null
                    var primaryFailure: Throwable? = null
                    try {
                        instrumentation.context.assets.open("fixture-v1.apk").use { input -> source.outputStream().use(input::copyTo) }
                        @Suppress("DEPRECATION")
                        val archive = requireNotNull(context.packageManager.getPackageArchiveInfo(source.absolutePath, 0))
                        assertEquals(FixtureInstallUi.PACKAGE_NAME, archive.packageName)
                        assertEquals(0, requireNotNull(archive.applicationInfo).flags and ApplicationInfo.FLAG_HAS_CODE)
                        ZipFile(source).use { zip -> assertFalse(zip.entries().asSequence().any { Regex("classes(?:[0-9]+)?\\.dex").matches(it.name) }) }
                        val digest = MessageDigest.getInstance("SHA-256").digest(source.readBytes())
                        permission.denyForTest()
                        ownership.installationStarted()
                        val token = ExternalInstaller.start(context,
                            ExternalSources.fromIntent(Intent(Intent.ACTION_VIEW, Uri.fromFile(source))),
                            InstallOptions(authorizer = "none", deleteSource = false, timeoutMillis = 60_000))
                        val pending = requireNotNull(InstallPresentation.find(token)).also { record = it }
                        val deadline = SystemClock.elapsedRealtime() + 55_000
                        assertTrue("The fixed fixture did not reach its plugin confirmation", waitUntil(deadline) {
                            pending.snapshot().prompt?.metadata?.packageName == FixtureInstallUi.PACKAGE_NAME &&
                                FixtureInstallUi.clickInstall(InstallDialogActivity.TAG_CONFIRM, token = token, packageName = FixtureInstallUi.PACKAGE_NAME)
                        })
                        assertTrue("The request did not open its unknown-source Settings child", waitUntil(deadline) {
                            trace.bridgeTokens.size == 1 && trace.settingsPackages.size == 1 && trace.createdSessions.size == 1
                        })
                        assertEquals(listOf("package:${context.packageName}"), trace.settingsPackages.toList())
                        val actionToken = trace.bridgeTokens.single()
                        val ownedUi = OwnedUnknownSourceUi(context, actionToken).also { permissionUi = it }
                        assertTrue("No Settings child belongs to this live installation token", waitUntil(deadline) {
                            ownedUi.hasSettingsChild()
                        })
                        ownedUi.requireRecognizedSettings()
                        val sessionId = trace.originalSession().sessionId
                        assertTrue("System confirmation must wait for unknown-source permission", trace.confirmationSessions.isEmpty())
                        UnknownSourcePermissionState.evidence("Settings shown for ${context.packageName}; platformSession=$sessionId bridge=$actionToken")

                        // This is the only grant action. Unknown OEM nodes are never clicked.
                        val switchDeadline = minOf(deadline, SystemClock.elapsedRealtime() + 8_000)
                        val switchClicked = waitUntil(switchDeadline) {
                            ownedUi.clickAllowFromThisSource()
                        }
                        if (!switchClicked) ownedUi.reportSettingsCheckables()
                        assumeTrue("The owned Settings page has no supported, unambiguous allow-from-source switch", switchClicked)
                        assertTrue("The UI switch did not enable this plugin's permission", waitUntil(deadline) {
                            ownedUi.additionalOemPermissionComponent()?.let { component ->
                                UnknownSourcePermissionState.evidence("Additional OEM permission component=$component")
                                assumeTrue("Additional OEM permission consent was not automatically performed; permission grant completion and installation success are not verified on this device", false)
                            }
                            context.packageManager.canRequestPackageInstalls()
                        })
                        permission.assertGrantedBySettings()
                        assertEquals(sessionId, trace.originalSession().sessionId)
                        UnknownSourcePermissionState.evidence("Permission granted by Settings switch; continuing platformSession=$sessionId")
                        if (ownedUi.canReturnFromSettings()) {
                            val settingsTask = requireNotNull(ownedUi.settingsTask())
                            val beforeBack = requireNotNull(runCatching { settingsTask.taskInfo }.getOrNull())
                            assertEquals(actionToken, beforeBack.baseIntent.getStringExtra(UserActionBridge.EXTRA_TOKEN))
                            assertTrue(UserActionBridge.isAttached(actionToken))
                            val downTime = SystemClock.uptimeMillis()
                            val downAccepted = instrumentation.uiAutomation.injectInputEvent(
                                KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0), true)
                            val upAccepted = instrumentation.uiAutomation.injectInputEvent(
                                KeyEvent(downTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, 0), true)
                            assertTrue("UiAutomation rejected the owned Settings Back key down", downAccepted)
                            assertTrue("UiAutomation rejected the owned Settings Back key up", upAccepted)
                            val afterBack = requireNotNull(runCatching { settingsTask.taskInfo }.getOrNull())
                            @Suppress("DEPRECATION")
                            assertEquals(beforeBack.id, afterBack.id)
                            assertEquals(UserActionActivity::class.java.name, afterBack.baseActivity?.className)
                            assertEquals(actionToken, afterBack.baseIntent.getStringExtra(UserActionBridge.EXTRA_TOKEN))
                            assertTrue(UserActionBridge.isAttached(actionToken))
                        }

                        var systemApproved = false
                        val confirmationAccepted = waitUntil(deadline) {
                            if (!systemApproved && ownedUi.canApproveFixture()) {
                                assertEquals(listOf(sessionId), trace.confirmationSessions.toList())
                                assertEquals(sessionId, trace.originalSession().sessionId)
                                systemApproved = FixtureInstallUi.acceptSystemFixture(FixtureInstallUi.LABEL)
                            }
                            systemApproved
                        }
                        if (!confirmationAccepted) {
                            val diagnostic = grantDiagnostic(trace, ownedUi, pending)
                            UnknownSourcePermissionState.evidence("system-confirmation timeout: $diagnostic")
                            fail("The same session did not show the fixed fixture's system confirmation: $diagnostic")
                        }
                        var extraScanDeclined = false
                        val finished = waitUntil(deadline) {
                            if (pending.snapshot().terminal) true else {
                                if (!extraScanDeclined) {
                                    extraScanDeclined = ownedUi.declineFixedFixtureScan {
                                        assertEquals(listOf(sessionId), trace.createdSessions.toList())
                                        assertEquals(listOf(sessionId), trace.confirmationSessions.toList())
                                        assertEquals(sessionId, trace.originalSession().sessionId)
                                    }
                                    if (extraScanDeclined) UnknownSourcePermissionState.evidence(
                                        "Declined the fixed fixture's additional Play Protect scan consent; platformSession=$sessionId")
                                }
                                false
                            }
                        }
                        if (!finished) {
                            val diagnostic = grantDiagnostic(trace, ownedUi, pending)
                            UnknownSourcePermissionState.evidence("installation-terminal timeout: $diagnostic")
                            fail("The original installation did not finish: $diagnostic")
                        }
                        val completed = pending.snapshot()
                        val result = requireNotNull(completed.items.single().result)
                        if (extraScanDeclined) {
                            assertFalse("Declining extra scan consent must not claim installation success", result["ok"].asBoolean)
                            assertTrue("The declined scan did not finish the original platform session", waitUntil(SystemClock.elapsedRealtime() + 5_000) {
                                (sessionId to false) in trace.finishedSessions
                            })
                            assertEquals(listOf(sessionId), trace.createdSessions.toList())
                            assertEquals(listOf(sessionId), trace.confirmationSessions.toList())
                            assertEquals(listOf(actionToken), trace.bridgeTokens.toList())
                            val installedAfterDecline = try {
                                @Suppress("DEPRECATION")
                                context.packageManager.getPackageInfo(FixtureInstallUi.PACKAGE_NAME, 0)
                                true
                            } catch (_: PackageManager.NameNotFoundException) { false }
                            assertFalse("The fixture was installed despite declining the extra scan", installedAfterDecline)
                            assertTrue(source.isFile)
                            assertArrayEquals(digest, MessageDigest.getInstance("SHA-256").digest(source.readBytes()))
                            assumeTrue("Additional Play Protect upload/scan consent was not performed; the owned fixture was declined and its session finished. Installation success is not verified on this device", false)
                        }
                        assertNull(completed.failure)
                        assertTrue("$result", result["ok"].asBoolean)
                        assertEquals("none", result["authorizer"].asString)
                        assertEquals(FixtureInstallUi.PACKAGE_NAME, result["packageName"].asString)
                        assertFalse(result["sourceDeleted"].asBoolean)
                        assertTrue(source.isFile)
                        assertArrayEquals(digest, MessageDigest.getInstance("SHA-256").digest(source.readBytes()))
                        @Suppress("DEPRECATION")
                        val installed = context.packageManager.getPackageInfo(FixtureInstallUi.PACKAGE_NAME, 0)
                        @Suppress("DEPRECATION")
                        assertEquals(1L, if (Build.VERSION.SDK_INT >= 28) installed.longVersionCode else installed.versionCode.toLong())
                        assertTrue("No successful terminal callback for the original platform session", waitUntil(deadline) {
                            (sessionId to true) in trace.finishedSessions
                        })
                        assertEquals(listOf(sessionId), trace.createdSessions.toList())
                        assertEquals(listOf(sessionId), trace.confirmationSessions.toList())
                        assertEquals(listOf(actionToken), trace.bridgeTokens.toList())
                        assertEquals(listOf("package:${context.packageName}"), trace.settingsPackages.toList())
                        assertSame(pending, InstallPresentation.find(token))
                        UnknownSourcePermissionState.evidence("SUCCESS platformSession=$sessionId created=1 systemConfirmationStarts=1 sourcePreserved=true authorizer=none")
                    } catch (failure: Throwable) {
                        primaryFailure = failure
                        runCatching { UnknownSourcePermissionState.evidence("primary failure=${failure.javaClass.simpleName}: ${grantDiagnostic(trace, permissionUi, record)}") }
                        throw failure
                    } finally {
                        var cleanupFailure: Throwable? = null
                        fun cleanup(phase: String, action: () -> Unit) {
                            try { action() } catch (failure: Throwable) {
                                runCatching { UnknownSourcePermissionState.evidence("cleanup failure phase=$phase type=${failure.javaClass.simpleName}: ${grantDiagnostic(trace, permissionUi, record)}") }
                                val primary = primaryFailure
                                if (primary != null) primary.addSuppressed(failure)
                                else if (cleanupFailure == null) cleanupFailure = failure
                                else cleanupFailure!!.addSuppressed(failure)
                            }
                        }
                        cleanup("record") { record?.close() }
                        cleanup("owned-task") { permissionUi?.closeOwnedTask() }
                        cleanup("platform-session") { trace.settleOwnedFixtureSessions() }
                        cleanup("fixture-source") { folder.deleteRecursively() }
                        if (primaryFailure == null) cleanupFailure?.let { throw it }
                    }
                }
            }
        }
    }

    @Test fun closingTheActualUnknownSourceSettingsTaskCancelsWithoutGrantingPermission() {
        assumeTrue(Build.VERSION.SDK_INT >= 26 && InstrumentationRegistry.getArguments().getString("unknownSourceRefusal") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assumeTrue("This check preserves an existing denied permission", !context.packageManager.canRequestPackageInstalls())
        assumeTrue("Unlock the test device", !context.getSystemService(KeyguardManager::class.java).isKeyguardLocked)
        val folder = File(context.cacheDir, "unknown-source-${System.nanoTime()}").apply { check(mkdir()) }
        val source = File(folder, "fixture.apk")
        instrumentation.context.assets.open("fixture-v1.apk").use { input -> source.outputStream().use(input::copyTo) }
        var record: InstallPresentation.Record? = null
        FixturePackageOwnership(setOf(FixtureInstallUi.PACKAGE_NAME)).use { ownership ->
            try {
                ownership.installationStarted()
                val token = ExternalInstaller.start(context, ExternalSources.fromIntent(Intent(Intent.ACTION_VIEW, Uri.fromFile(source))),
                    InstallOptions(authorizer = "none", timeoutMillis = 45_000))
                record = requireNotNull(InstallPresentation.find(token))
                val deadline = SystemClock.elapsedRealtime() + 30_000
                var settingsSeen = false
                while (!record.snapshot().terminal && SystemClock.elapsedRealtime() < deadline) {
                    if (record.snapshot().prompt?.metadata?.packageName == FixtureInstallUi.PACKAGE_NAME) {
                        FixtureInstallUi.clickInstall(InstallDialogActivity.TAG_CONFIRM, token = token, packageName = FixtureInstallUi.PACKAGE_NAME)
                    }
                    // OEM settings may omit the app label from accessibility nodes. Verify the
                    // actual settings child of our live token-owned task, without touching any switch.
                    val owned = context.getSystemService(ActivityManager::class.java).appTasks.firstOrNull { task ->
                        val info = runCatching { task.taskInfo }.getOrNull() ?: return@firstOrNull false
                        val token = info.baseIntent.getStringExtra(UserActionBridge.EXTRA_TOKEN)
                        info.baseActivity?.className == UserActionActivity::class.java.name &&
                            info.topActivity?.packageName in setOf("com.android.settings", "com.miui.securitycenter") &&
                            token != null && UserActionBridge.isAttached(token)
                    }
                    if (owned != null) {
                        settingsSeen = true
                        instrumentation.sendStatus(0, android.os.Bundle().apply {
                            putString("unknown-source-settings", owned.taskInfo?.topActivity?.flattenToString())
                        })
                        owned.finishAndRemoveTask()
                        break
                    }
                    SystemClock.sleep(50)
                }
                assertTrue("No real permission page for this plugin appeared", settingsSeen)
                while (!record.snapshot().terminal && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(50)
                assertTrue(record.snapshot().terminal)
                assertEquals("USER_CANCELLED", record.snapshot().failure?.code)
                assertFalse(context.packageManager.canRequestPackageInstalls())
                assertTrue(source.isFile)
            } finally { record?.close(); folder.deleteRecursively() }
        }
    }

    private fun waitUntil(deadline: Long, condition: () -> Boolean): Boolean {
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return true
            SystemClock.sleep(50)
        }
        return false
    }

    private fun grantDiagnostic(trace: UnknownSourceGrantTrace, ownedUi: OwnedUnknownSourceUi?, record: InstallPresentation.Record?): String {
        val snapshot = record?.snapshot()
        return "bridges=${trace.bridgeTokens} createdSessions=${trace.createdSessions} " +
            "confirmationSessions=${trace.confirmationSessions} finishedSessions=${trace.finishedSessions} " +
            "${ownedUi?.diagnostic() ?: "ownedTask=unavailable"} " +
            "recordStage=${snapshot?.stage} recordTerminal=${snapshot?.terminal} recordFailure=${snapshot?.failure?.code}"
    }

    /** A skipped UI capability check must not hide failed session/package/permission cleanup. */
    private inline fun <T : Closeable, R> T.useWithVerifiedCleanup(block: (T) -> R): R = try {
        use(block)
    } catch (skipped: AssumptionViolatedException) {
        if (skipped.suppressed.isNotEmpty()) {
            throw AssertionError("The opt-in test cannot be skipped because cleanup failed", skipped)
        }
        throw skipped
    }
}
