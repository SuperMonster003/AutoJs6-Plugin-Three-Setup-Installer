package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.KeyguardManager
import android.app.ActivityManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
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
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Observes and declines the real unknown-source settings page; no permission is changed. */
@RunWith(AndroidJUnit4::class)
class UnknownSourcePermissionDeviceTest {
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
}
