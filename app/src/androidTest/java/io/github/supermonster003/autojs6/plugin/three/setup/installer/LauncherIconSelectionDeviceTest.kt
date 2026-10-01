package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Process
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.LauncherIconMode
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.LauncherIconStatePolicy
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.LauncherIcons
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.SettingsActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class LauncherIconSelectionDeviceTest {
    @Test fun confirmedModesRetainTheProcessOneAliasAndMutableShortcutOwnership() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packages = context.packageManager
        val before = LauncherIconMode.entries.associateWith { packages.getComponentEnabledSetting(it.component(context)) }
        val originalShortcuts = if (Build.VERSION.SDK_INT >= 25) context.getSystemService(ShortcutManager::class.java)?.let {
            (it.dynamicShortcuts + it.pinnedShortcuts).distinctBy { info -> info.id }
                .filterNot { info -> info.isDeclaredInManifest || (Build.VERSION.SDK_INT >= 30 && info.isImmutable) }
        }.orEmpty() else emptyList()
        val shortcutId = "settings-icon-test-${Process.myPid()}-${System.nanoTime()}"
        val real = ComponentName(context.packageName, "${context.packageName}.ui.HomeActivity")
        val pid = Process.myPid()
        try {
            LauncherIcons.normalize(context)
            if (Build.VERSION.SDK_INT >= 25) {
                val manager = context.getSystemService(ShortcutManager::class.java)
                assertTrue(manager.addDynamicShortcuts(listOf(ShortcutInfo.Builder(context, shortcutId)
                    .setShortLabel("Icon test").setActivity(LauncherIcons.launcherActivity(context))
                    .setIntent(Intent(Intent.ACTION_VIEW).setComponent(real))
                    .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher_system_auto)).build())))
            }
            ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
                for (mode in LauncherIconMode.entries) {
                    scenario.onActivity { activity ->
                        activity.findViewById<View>(android.R.id.content).findViewWithTag<View>("settings-icon").performClick()
                        val previous = LauncherIcons.current(context)
                        activity.prompt!!.window!!.decorView.findViewWithTag<View>("choice-${mode.ordinal}").performClick()
                        assertEquals(previous, LauncherIcons.current(context))
                        activity.prompt!!.window!!.decorView.findViewWithTag<View>("settings-confirm").performClick()
                        assertEquals(pid, Process.myPid())
                        assertEquals(mode, LauncherIcons.current(context))
                        val entries = packages.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName), 0)
                        assertEquals(1, entries.size)
                        assertEquals(mode.component(context).className, entries.single().activityInfo.name)
                        assertEquals(real.className, entries.single().activityInfo.targetActivity)
                        assertTrue(packages.getActivityInfo(real, 0).enabled)
                        if (Build.VERSION.SDK_INT >= 25) {
                            val shortcut = context.getSystemService(ShortcutManager::class.java).dynamicShortcuts.single { it.id == shortcutId }
                            assertEquals(mode.component(context), shortcut.activity)
                            assertEquals(real, shortcut.intent?.component)
                        }
                    }
                    scenario.recreate()
                    assertEquals(mode, LauncherIcons.current(context))
                }
            }
        } finally {
            val drained = CountDownLatch(1)
            LauncherIcons.normalizeAsync(context) { drained.countDown() }
            assertTrue(drained.await(10, TimeUnit.SECONDS))
            before.entries.sortedBy { if (LauncherIconStatePolicy.enabled(it.key, it.value)) 0 else 1 }.forEach { (mode, state) ->
                packages.setComponentEnabledSetting(mode.component(context), state, PackageManager.DONT_KILL_APP)
            }
            if (Build.VERSION.SDK_INT >= 25) {
                context.getSystemService(ShortcutManager::class.java).apply {
                    removeDynamicShortcuts(listOf(shortcutId))
                    if (originalShortcuts.isNotEmpty()) assertTrue(updateShortcuts(originalShortcuts))
                }
            }
        }
    }
}
