package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.view.KeyEvent
import android.view.View
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.ReleaseHistoryAssetPolicy
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.ReleaseInfo
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.ReleaseInfoCodec
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.loadReleaseHistory
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.AboutActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.ReleaseHistoryActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.SettingsActivity
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The release history page (AUTOJS6_PLUGIN_RELEASE_HISTORY_AGENTS.md): its title is "Release history" with the plugin
 * name as the subtitle, the newest release starts expanded, and the settings row, the About page and the update dialog
 * open it. Writes nothing outside the plugin's update cache, which it restores.
 */
@RunWith(AndroidJUnit4::class)
class ReleaseHistoryPageInstrumentationTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val updates get() = context.getSharedPreferences("installer_updates", Context.MODE_PRIVATE)
    private lateinit var savedUpdates: Map<String, *>

    @Before fun saveUpdateCache() {
        savedUpdates = updates.all.toMap()
    }

    @After fun finishActivitiesAndRestore() {
        instrumentation.setInTouchMode(true)
        instrumentation.runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).forEach(Activity::finish)
        }
        instrumentation.waitForIdleSync()
        val editor = updates.edit().clear()
        savedUpdates.forEach { (key, value) ->
            @Suppress("UNCHECKED_CAST")
            when (value) {
                is Long -> editor.putLong(key, value)
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Set<*> -> editor.putStringSet(key, value as Set<String>)
            }
        }
        editor.commit()
    }

    /** Unmerged: the count chips sit inside the clickable header, which merges its children. */
    private fun exists(tag: String): Boolean =
        runCatching { compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)

    private fun resumed(): Activity? {
        var activity: Activity? = null
        instrumentation.runOnMainSync {
            activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()
        }
        return activity
    }

    private fun assertThePageOpened() {
        compose.waitUntil(TIMEOUT) { resumed() is ReleaseHistoryActivity && exists("$TAG.releaseHistory.list") }
        assertTitleAndSubtitle()
    }

    private fun assertTitleAndSubtitle() {
        val inTopBar = hasAnyAncestor(hasTestTag("$TAG.topBar"))
        compose.onNode(hasText(context.getString(R.string.release_history)) and inTopBar).assertIsDisplayed()
        compose.onNode(hasText(context.getString(R.string.app_name)) and inTopBar).assertIsDisplayed()
    }

    private fun <T : Activity> start(type: Class<T>): T =
        type.cast(instrumentation.startActivitySync(Intent(context, type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)))!!

    /** Waits until the View page has its window focus and the control tagged [tag] is laid out, then clicks it. */
    private fun clickView(activity: Activity, tag: String) {
        compose.waitUntil(TIMEOUT) {
            var ready = false
            instrumentation.runOnMainSync {
                ready = activity.hasWindowFocus() && activity.window.decorView.findViewWithTag<View>(tag)?.isLaidOut == true
            }
            ready
        }
        instrumentation.runOnMainSync { activity.window.decorView.findViewWithTag<View>(tag).performClick() }
    }

    @Test fun theTitleIsReleaseHistoryAndTheSubtitleThePluginName() {
        ActivityScenario.launch(ReleaseHistoryActivity::class.java).use {
            compose.waitUntil(TIMEOUT) { exists("$TAG.releaseHistory.list") }
            assertTitleAndSubtitle()
            assertFalse(exists("$TAG.releaseHistory.empty"))
        }
    }

    private fun versions(scenario: ActivityScenario<ReleaseHistoryActivity>): List<String> {
        var versions = emptyList<String>()
        scenario.onActivity { activity ->
            val candidates = ReleaseHistoryAssetPolicy.candidatesFor(activity.resources.configuration.locales[0])
            versions = loadReleaseHistory(candidates) { path ->
                activity.assets.open(path).use { it.readBytes().toString(Charsets.UTF_8) }
            }.map { it.version }
        }
        return versions
    }

    /** Brings a release header on screen first: the newest release starts expanded and can push the next one below. */
    private fun clickEntry(version: String) {
        compose.onNodeWithTag("$TAG.releaseHistory.list").performScrollToNode(hasTestTag("$TAG.releaseHistory.entry.$version"))
        compose.onNodeWithTag("$TAG.releaseHistory.entry.$version").performClick()
    }

    /**
     * The newest release starts expanded, the others show their category counts; a header expands or collapses its
     * release, the top bar expands or collapses all of them, and the choice survives recreation.
     */
    @Test fun releasesExpandAndCollapse() {
        ActivityScenario.launch(ReleaseHistoryActivity::class.java).use { scenario ->
            compose.waitUntil(TIMEOUT) { exists("$TAG.releaseHistory.list") }
            val versions = versions(scenario)
            assertTrue("releases: ${versions.size}", versions.size > 2)
            val (newest, second) = versions
            assertTrue(exists("$TAG.releaseHistory.body.$newest"))
            assertFalse(exists("$TAG.releaseHistory.counts.$newest"))
            compose.onNodeWithTag("$TAG.releaseHistory.list").performScrollToNode(hasTestTag("$TAG.releaseHistory.entry.$second"))
            assertFalse(exists("$TAG.releaseHistory.body.$second"))
            assertTrue(exists("$TAG.releaseHistory.counts.$second"))

            clickEntry(second)
            compose.waitUntil(TIMEOUT) { exists("$TAG.releaseHistory.body.$second") }
            clickEntry(newest)
            compose.waitUntil(TIMEOUT) { !exists("$TAG.releaseHistory.body.$newest") && exists("$TAG.releaseHistory.counts.$newest") }

            scenario.recreate()
            compose.waitUntil(TIMEOUT) { exists("$TAG.releaseHistory.list") }
            assertTrue(exists("$TAG.releaseHistory.body.$second"))
            assertFalse(exists("$TAG.releaseHistory.body.$newest"))

            compose.onNodeWithTag("$TAG.releaseHistory.toggleAll")
                .assertContentDescriptionEquals(context.getString(R.string.release_history_expand_all))
                .performClick()
            compose.waitUntil(TIMEOUT) { exists("$TAG.releaseHistory.body.$newest") }
            compose.onNodeWithTag("$TAG.releaseHistory.toggleAll")
                .assertContentDescriptionEquals(context.getString(R.string.release_history_collapse_all))
                .performClick()
            compose.waitUntil(TIMEOUT) { !exists("$TAG.releaseHistory.body.$newest") && !exists("$TAG.releaseHistory.body.$second") }
        }
    }

    @Test fun theSettingsRowOpensThePage() {
        clickView(start(SettingsActivity::class.java), "settings-history")
        assertThePageOpened()
    }

    @Test fun theAboutLinkOpensThePage() {
        clickView(start(AboutActivity::class.java), "about-history")
        assertThePageOpened()
    }

    /** The cached result of a recent check (no network): a newer release, whose dialog offers the release history. */
    @Test fun theUpdateDialogOpensThePage() {
        val release = ReleaseInfo(
            tag = "v99.0.0",
            url = "https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases/tag/v99.0.0",
            notes = "* `Feature` Note",
            publishedAt = "2026-11-19T00:00:00Z",
        )
        assertTrue(updates.edit().clear()
            .putLong("last_attempt", System.currentTimeMillis())
            .putString("release", ReleaseInfoCodec.encode(release))
            .commit())
        val page = start(SettingsActivity::class.java)
        clickView(page, "settings-update")
        compose.waitUntil(TIMEOUT) {
            var ready = false
            instrumentation.runOnMainSync {
                val dialog = page.updates?.dialog
                ready = dialog?.isShowing == true && dialog.window?.decorView?.findViewWithTag<View>("update-history") != null
            }
            ready
        }
        instrumentation.runOnMainSync {
            requireNotNull(page.updates?.dialog?.window).decorView.findViewWithTag<View>("update-history").performClick()
        }
        assertThePageOpened()
    }

    private fun key(code: Int) {
        instrumentation.sendKeyDownUpSync(code)
        compose.waitForIdle()
    }

    /**
     * Leaves touch mode as a keyboard user does. Newer versions take [android.app.Instrumentation.setInTouchMode] at
     * once; older ones (API 24) learn it only from a key, so an up arrow goes first.
     */
    private fun leaveTouchMode() {
        instrumentation.setInTouchMode(false)
        key(KeyEvent.KEYCODE_DPAD_UP)
    }

    /**
     * The test tag of the focused control (or of its nearest tagged ancestor), null when nothing has focus. The back
     * button has no tag of its own, so inside the top bar it is told apart by its content description.
     */
    private fun focusedControl(): String? {
        val node = compose.onAllNodes(isFocused(), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull() ?: return null
        val tag = generateSequence(node) { it.parent }.mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }.firstOrNull()
        if (tag == "$TAG.topBar") {
            val description = compose.onNode(isFocused()).fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription)
            if (description == listOf(context.getString(R.string.settings_back))) return BACK
        }
        return tag ?: UNTAGGED
    }

    /** The window with input focus as `dumpsys window` reports it; for failure messages only (it connects UiAutomation). */
    private fun focusedWindow(): String = runCatching {
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("dumpsys window windows"))
            .bufferedReader().useLines { lines -> lines.map(String::trim).filter { it.startsWith("mCurrentFocus=") }.distinct().joinToString() }
            .ifEmpty { "mCurrentFocus not in dumpsys" }
    }.getOrElse { "dumpsys failed: $it" }

    /** Keys go to the focused window: while a system dialog (an ANR, say) has focus, every Tab would be lost. */
    private fun awaitWindowFocus(scenario: ActivityScenario<*>, visited: List<String> = emptyList()) {
        var focused = false
        try {
            compose.waitUntil(TIMEOUT) { scenario.onActivity { focused = it.hasWindowFocus() }; focused }
        } catch (timeout: ComposeTimeoutException) {
            fail("the page has no window focus after $TIMEOUT ms; visited: $visited; ${focusedWindow()}")
        }
    }

    /** Presses Tab until focus returns to a control seen before; every focused control must be on screen. */
    private fun tabCycle(scenario: ActivityScenario<*>): List<String> {
        val visited = mutableListOf<String>()
        repeat(MAX_TABS) {
            awaitWindowFocus(scenario, visited)
            key(KeyEvent.KEYCODE_TAB)
            val control = focusedControl() ?: return@repeat
            if (control in visited) return visited
            compose.onNode(isFocused(), useUnmergedTree = true).assertIsDisplayed()
            visited += control
        }
        return visited
    }

    /**
     * Keyboard users read the whole page (AUTOJS6_PLUGIN_RELEASE_HISTORY_AGENTS.md section 10): Tab passes the back
     * button and then every version header in order, down to the oldest release.
     */
    @Test fun theReleaseHistoryCanBeReadToTheEndFromTheKeyboard() {
        ActivityScenario.launch(ReleaseHistoryActivity::class.java).use { scenario ->
            compose.waitUntil(TIMEOUT) { exists("$TAG.releaseHistory.list") }
            val versions = versions(scenario)
            assertTrue("releases: ${versions.size}", versions.size > 2)
            awaitWindowFocus(scenario)
            leaveTouchMode()
            val visited = tabCycle(scenario)
            fun expect(ok: Boolean, what: String) { if (!ok) fail("$what; visited: $visited; ${focusedWindow()}") }
            expect(BACK in visited, "Tab never reached the back button")
            expect(UNTAGGED !in visited, "something focusable has no name")
            val expected = versions.map { "$TAG.releaseHistory.entry.$it" }
            val entries = visited.filter { it.startsWith("$TAG.releaseHistory.entry.") }
            expect(entries == expected, "the releases in Tab order: expected $expected, got $entries")
        }
    }

    private companion object {
        const val TAG = "setup"
        const val TIMEOUT = 15_000L
        const val MAX_TABS = 120
        const val UNTAGGED = "(untagged)"
        const val BACK = "(back)"
    }
}
