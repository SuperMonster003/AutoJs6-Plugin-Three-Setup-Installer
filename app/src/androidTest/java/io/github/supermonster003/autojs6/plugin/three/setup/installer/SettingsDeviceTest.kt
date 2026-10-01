package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.radiobutton.MaterialRadioButton
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.InstallerPreferences
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.ReleaseHistory
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.LauncherIconMode
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.LauncherIconStatePolicy
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.LauncherIcons
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.SettingsActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class SettingsDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun selectionCancelConfirmationAndRecreationUseTheSamePersistedDefaults() = preservingState {
        val before = InstallerPreferences.read(context)
        val desired = !before.options.allowTestOnly
        val index = if (desired) 1 else 0
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<View>(android.R.id.content).findViewWithTag<View>("settings-test").performClick()
                val dialog = activity.prompt!!.window!!.decorView
                dialog.findViewWithTag<View>("choice-$index").performClick()
                assertEquals(before, InstallerPreferences.read(context))
                dialog.findViewWithTag<View>("settings-cancel").performClick()
                assertEquals(before, InstallerPreferences.read(context))
                activity.findViewById<View>(android.R.id.content).findViewWithTag<View>("settings-test").performClick()
                activity.prompt!!.window!!.decorView.findViewWithTag<View>("choice-$index").performClick()
                activity.prompt!!.window!!.decorView.findViewWithTag<View>("settings-confirm").performClick()
                assertEquals(desired, InstallerPreferences.read(context).options.allowTestOnly)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                activity.findViewById<View>(android.R.id.content).findViewWithTag<View>("settings-test").performClick()
                assertTrue(activity.prompt!!.window!!.decorView.findViewWithTag<MaterialRadioButton>("choice-$index").isChecked)
                activity.prompt!!.cancel()
            }
        }
    }

    @Test fun invalidInstallerDraftDisablesConfirmationAndCancelKeepsOriginalValue() = preservingState {
        val before = InstallerPreferences.read(context)
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<View>(android.R.id.content).findViewWithTag<View>("settings-installer").performClick()
                val dialog = activity.prompt!!.window!!.decorView
                val field = dialog.findViewWithTag<EditText>("settings-input")
                field.setText("not a package")
                assertFalse(dialog.findViewWithTag<View>("settings-confirm").isEnabled)
                assertEquals(before, InstallerPreferences.read(context))
                field.setText("com.android.shell")
                assertTrue(dialog.findViewWithTag<View>("settings-confirm").isEnabled)
                dialog.findViewWithTag<View>("settings-cancel").performClick()
                assertEquals(before, InstallerPreferences.read(context))
            }
        }
    }

    @Test fun everyBundledLanguageHasItsOwnHistoryAndLegalDocumentsStayOffline() {
        for (tag in listOf("zh-Hans", "zh-Hant-HK", "zh-Hant-TW", "en", "fr", "es", "ja", "ko", "ru", "ar")) {
            val candidates = ReleaseHistory.candidates(Locale.forLanguageTag(tag))
            val actual = context.assets.open(candidates.first()).bufferedReader().use { it.readText() }
            assertTrue(tag, actual.contains("1.0.0"))
        }
        for (path in listOf("legal/LICENSE", "legal/THIRD_PARTY_NOTICES.md")) {
            assertTrue(context.assets.open(path).bufferedReader().use { it.readText() }.isNotBlank())
        }
    }

    private fun preservingState(test: () -> Unit) {
        val preferences = context.getSharedPreferences("installer_settings", Context.MODE_PRIVATE)
        val before = preferences.all.toMap()
        val aliases = LauncherIconMode.entries.associateWith { context.packageManager.getComponentEnabledSetting(it.component(context)) }
        val shortcutManager = if (android.os.Build.VERSION.SDK_INT >= 25) context.getSystemService(android.content.pm.ShortcutManager::class.java) else null
        val shortcuts = if (android.os.Build.VERSION.SDK_INT >= 25) shortcutManager?.let {
            (it.dynamicShortcuts + it.pinnedShortcuts).distinctBy { entry -> entry.id }.filterNot { entry ->
                entry.isDeclaredInManifest || (android.os.Build.VERSION.SDK_INT >= 30 && entry.isImmutable)
            }
        }.orEmpty() else emptyList()
        try { test() }
        finally {
            restore(preferences, before)
            val drained = CountDownLatch(1)
            LauncherIcons.normalizeAsync(context) { drained.countDown() }
            assertTrue(drained.await(10, TimeUnit.SECONDS))
            aliases.entries.sortedBy { if (LauncherIconStatePolicy.enabled(it.key, it.value)) 0 else 1 }.forEach { (mode, state) ->
                context.packageManager.setComponentEnabledSetting(mode.component(context), state, android.content.pm.PackageManager.DONT_KILL_APP)
            }
            if (android.os.Build.VERSION.SDK_INT >= 25 && shortcutManager != null) {
                val after = (shortcutManager.dynamicShortcuts + shortcutManager.pinnedShortcuts).associateBy { it.id }
                val changed = shortcuts.filter { after[it.id]?.activity != it.activity }
                if (changed.isNotEmpty()) assertTrue(shortcutManager.updateShortcuts(changed))
            }
        }
    }

    private fun restore(preferences: SharedPreferences, values: Map<String, *>) {
        val editor = preferences.edit().clear()
        for ((key, value) in values) when (value) {
            is String -> editor.putString(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
        }
        assertTrue(editor.commit())
    }
}
