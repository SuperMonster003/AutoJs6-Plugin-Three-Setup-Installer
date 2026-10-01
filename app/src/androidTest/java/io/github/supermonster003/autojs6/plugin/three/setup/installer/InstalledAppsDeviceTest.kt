package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.KeyguardManager
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.materialswitch.MaterialSwitch
import io.github.supermonster003.autojs6.plugin.three.setup.installer.installedapps.InstalledApp
import io.github.supermonster003.autojs6.plugin.three.setup.installer.installedapps.InstalledAppsRepository
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstalledAppsActivity
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Read-only package browsing and UI draft tests. No uninstall button or package engine is invoked. */
@RunWith(AndroidJUnit4::class)
class InstalledAppsDeviceTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @Test fun catalogLoadsCurrentUserMetadataAndCachesOnlyRequestedIcons() {
        InstalledAppsRepository(context).use { repository ->
            val apps = repository.load {}
            val own = apps.single { it.packageName == context.packageName }
            assertTrue(own.label.isNotBlank())
            assertTrue(own.versionCode > 0)
            assertFalse(own.system)
            assertNull(repository.cachedIcon(own))
            val ready = CountDownLatch(1)
            val loaded = AtomicReference<android.graphics.Bitmap>()
            repository.requestIcon(own) { loaded.set(it); ready.countDown() }.use {
                assertTrue("The plugin icon did not load", ready.await(10, TimeUnit.SECONDS))
                assertSame(loaded.get(), repository.cachedIcon(own))
                assertTrue(loaded.get().allocationByteCount <= InstalledAppsRepository.ICON_CACHE_BYTES)
            }
            assertThrows(InterruptedException::class.java) { repository.load { throw InterruptedException() } }
        }
    }

    @Test fun searchSortAndSystemSwitchSurviveRecreationWithoutStartingAnOperation() {
        assumeFalse("Unlock the device for the application browser", context.getSystemService(KeyguardManager::class.java).isKeyguardLocked)
        ActivityScenario.launch<InstalledAppsActivity>(Intent(context, InstalledAppsActivity::class.java)).use { scenario ->
            waitUntil {
                var ready = false
                scenario.onActivity { ready = loaded(it) && displayed(it).any { app -> app.packageName == context.packageName } }
                ready
            }
            var newest = ""
            var cancel = ""
            var confirm = ""
            var oldSort = ""
            scenario.onActivity { activity ->
                activity.window.decorView.findViewWithTag<EditText>(InstalledAppsActivity.TAG_SEARCH).setText(context.packageName)
                activity.window.decorView.findViewWithTag<MaterialSwitch>(InstalledAppsActivity.TAG_SYSTEM).isChecked = true
                newest = activity.getString(R.string.apps_sort_updated)
                cancel = activity.getString(R.string.action_cancel)
                confirm = activity.getString(R.string.apps_confirm)
                val sort = activity.window.decorView.findViewWithTag<TextView>(InstalledAppsActivity.TAG_SORT)
                oldSort = sort.text.toString()
                sort.performClick()
            }
            clickDialogText(newest)
            scenario.onActivity { activity ->
                assertEquals(oldSort, activity.window.decorView.findViewWithTag<TextView>(InstalledAppsActivity.TAG_SORT).text.toString())
            }
            clickDialogText(cancel)
            scenario.onActivity { activity ->
                val sort = activity.window.decorView.findViewWithTag<TextView>(InstalledAppsActivity.TAG_SORT)
                assertEquals(oldSort, sort.text.toString())
                sort.performClick()
            }
            clickDialogText(newest)
            clickDialogText(confirm)
            waitUntil {
                var ready = false
                scenario.onActivity { activity ->
                    ready = loaded(activity) && displayed(activity).isNotEmpty() &&
                        displayed(activity).all { it.packageName.contains(context.packageName, ignoreCase = true) }
                }
                ready
            }
            scenario.recreate()
            waitUntil {
                var ready = false
                scenario.onActivity { ready = loaded(it) && displayed(it).isNotEmpty() }
                ready
            }
            scenario.onActivity { activity ->
                val decor = activity.window.decorView
                assertEquals(context.packageName, decor.findViewWithTag<EditText>(InstalledAppsActivity.TAG_SEARCH).text.toString())
                assertTrue(decor.findViewWithTag<MaterialSwitch>(InstalledAppsActivity.TAG_SYSTEM).isChecked)
                assertEquals(activity.getString(R.string.apps_sort, newest), decor.findViewWithTag<TextView>(InstalledAppsActivity.TAG_SORT).text.toString())
                val apps = displayed(activity)
                assertTrue(apps.any { it.packageName == context.packageName })
                assertTrue(apps.all { it.packageName.contains(context.packageName, ignoreCase = true) })
                assertEquals(apps.map { it.lastUpdateTime }.sortedDescending(), apps.map { it.lastUpdateTime })
                assertEquals(View.GONE, decor.findViewWithTag<View>(InstalledAppsActivity.TAG_OPERATION).visibility)
                assertEquals(activity.resources.configuration.layoutDirection,
                    decor.findViewWithTag<ListView>(InstalledAppsActivity.TAG_LIST).layoutDirection)
            }
        }
    }

    private fun displayed(activity: InstalledAppsActivity): List<InstalledApp> {
        val adapter = activity.window.decorView.findViewWithTag<ListView>(InstalledAppsActivity.TAG_LIST).adapter
        return (0 until adapter.count).mapNotNull { adapter.getItem(it) as? InstalledApp }
    }

    private fun loaded(activity: InstalledAppsActivity): Boolean {
        val text = activity.window.decorView.findViewWithTag<TextView>(InstalledAppsActivity.TAG_STATUS).text.toString()
        assertNotEquals(activity.getString(R.string.apps_load_failed), text)
        return text != activity.getString(R.string.apps_loading)
    }

    private fun clickDialogText(text: String) = waitUntil {
        val root = instrumentation.uiAutomation.rootInActiveWindow ?: return@waitUntil false
        if (root.packageName?.toString() != context.packageName) return@waitUntil false
        root.findAccessibilityNodeInfosByText(text).firstOrNull { it.text?.toString() == text && it.isClickable }
            ?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return
            SystemClock.sleep(40)
        }
        fail("The installed application browser did not reach the expected state")
    }
}
