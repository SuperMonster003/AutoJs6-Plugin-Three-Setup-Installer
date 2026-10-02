package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.kit

import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.TypedValue
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.UiTestFailures
import io.github.supermonster003.autojs6.plugin.three.setup.installer.preserveTestFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.readyForWindowInput
import io.github.supermonster003.autojs6.plugin.three.setup.installer.useOwnedWindow
import io.github.supermonster003.autojs6.plugin.three.setup.installer.spike.DialogGeometryActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.min

/** Real Android windows and the device's existing IME; no global display, locale or IME setting is written. */
@RunWith(AndroidJUnit4::class)
class InstallerDialogWindowDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun realPortraitImeKeepsTheTitleAndActionsVisibleAndRestoresTheWindow() =
        withWindow(DialogGeometryActivity.Overrides()) { scenario ->
            portrait(scenario)
            exerciseIme(scenario, "portrait-current")
        }

    @Test fun realRtlLargeTextNightWindowSupportsImeAndVisibleActions() =
        withWindow(DialogGeometryActivity.Overrides(language = "ar", fontScale = 2f, dark = true)) { scenario ->
            portrait(scenario)
            val initial = stable(scenario) { !it.imeVisible }
            assertEquals(initial.describe(), View.LAYOUT_DIRECTION_RTL, initial.direction)
            assertEquals(initial.describe(), 2f, initial.fontScale, 0.001f)
            assertTrue(initial.describe(), initial.dark)
            exerciseIme(scenario, "portrait-ar-font2-dark")
        }

    @Test fun realLandscapeWindowKeepsFixedActionsOutsideTheScrollableBody() =
        withWindow(DialogGeometryActivity.Overrides()) { scenario ->
            scenario.onActivity { it.hideKeyboard(); it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            val initial = stable(scenario) { it.orientation == Configuration.ORIENTATION_LANDSCAPE && !it.imeVisible }
            assertGeometry(initial)
            assertTrue(initial.describe(), initial.window.width() > initial.window.height())
            report("landscape-current", initial)
            exerciseScrollAndAction(scenario, initial, "landscape-scrolled")
        }

    private fun exerciseIme(scenario: ActivityScenario<DialogGeometryActivity>, label: String) {
        val before = stable(scenario) { !it.imeVisible && it.focus }
        assertGeometry(before)
        report("$label-hidden", before)
        scenario.onActivity { it.showKeyboard() }
        val shown = stable(scenario) { sample ->
            sample.editorActive && sample.editorFocused && sample.safe.bottom < before.safe.bottom &&
                // On pre-R resize windows, the visible frame is the independent keyboard boundary.
                // Modern windows additionally require the actual visible IME inset, not a fake resize.
                (sample.imeVisible && sample.ime.bottom > sample.bars.bottom ||
                    Build.VERSION.SDK_INT < 30 && sample.visibleFrame.bottom < before.visibleFrame.bottom)
        }
        assertGeometry(shown)
        assertTrue(shown.describe(), shown.safe.height() < before.safe.height())
        assertTrue(shown.describe(), shown.surface.bounds.height() < before.surface.bounds.height())
        assertFullyVisible("focused editor", shown.editor, shown.safe, shown)
        report("$label-ime", shown)
        instrumentation.sendStringSync("7")
        waitFor(5_000, "The focused editor did not receive input") {
            var accepted = false
            scenario.onActivity { accepted = it.editor.text?.toString()?.endsWith("7") == true }
            accepted
        }
        clickPositive(scenario, shown)
        scenario.onActivity { it.hideKeyboard() }
        val restored = stable(scenario) { !it.imeVisible && it.safe == before.safe }
        assertGeometry(restored)
        assertEquals(restored.describe(), before.safe, restored.safe)
        report("$label-restored", restored)
        exerciseScrollAndAction(scenario, restored, "$label-scrolled")
    }

    private fun exerciseScrollAndAction(scenario: ActivityScenario<DialogGeometryActivity>, before: Snapshot, label: String) {
        scenario.onActivity { it.dialog.scroll.fullScroll(View.FOCUS_DOWN) }
        val after = stable(scenario) { it.scrollY > 0 }
        assertGeometry(after)
        assertEquals(after.describe(), before.title.bounds, after.title.bounds)
        assertEquals(after.describe(), before.actions.bounds, after.actions.bounds)
        assertFullyVisible("last body row", after.lastRow, after.scroll.bounds, after)
        report(label, after)
        clickPositive(scenario, after)
    }

    private fun clickPositive(scenario: ActivityScenario<DialogGeometryActivity>, sample: Snapshot) {
        val current = stable(scenario) { it.orientation == sample.orientation && it.imeVisible == sample.imeVisible }
        assertGeometry(current)
        var previous = 0
        scenario.onActivity {
            check(it.window.decorView.readyForWindowInput()) { "The geometry window lost focus before the tap" }
            previous = it.positiveClicks
        }
        val bounds = current.positive.bounds
        val down = SystemClock.uptimeMillis()
        val press = MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, bounds.exactCenterX(), bounds.exactCenterY(), 0)
        press.source = InputDevice.SOURCE_TOUCHSCREEN
        try { assertTrue("Touch down was rejected: ${current.describe()}", instrumentation.uiAutomation.injectInputEvent(press, true)) }
        finally { press.recycle() }
        val release = MotionEvent.obtain(down, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, bounds.exactCenterX(), bounds.exactCenterY(), 0)
        release.source = InputDevice.SOURCE_TOUCHSCREEN
        try { assertTrue("Touch up was rejected: ${current.describe()}", instrumentation.uiAutomation.injectInputEvent(release, true)) }
        finally { release.recycle() }
        waitFor(5_000, "A visible fixed action did not receive its measured-screen-position tap") {
            var count = 0
            scenario.onActivity { count = it.positiveClicks }
            count == previous + 1
        }
    }

    private fun assertGeometry(value: Snapshot) {
        val detail = value.describe()
        val margin = (24 * value.density + 0.5f).toInt()
        val maximumWidth = min((560 * value.density + 0.5f).toInt(), value.safe.width() - 2 * margin)
        val maximumHeight = (value.safe.height() * 0.85f).toInt()
        assertTrue(detail, value.safe.width() > 0 && value.safe.height() > 0)
        assertTrue(detail, value.surface.bounds.left >= value.safe.left + margin)
        assertTrue(detail, value.surface.bounds.right <= value.safe.right - margin)
        assertTrue(detail, value.surface.bounds.width() <= maximumWidth + 1)
        assertTrue(detail, value.surface.bounds.height() <= maximumHeight + 1)
        // The fixture deliberately overflows, so extra window/IME padding must not shrink it twice.
        assertTrue("The long dialog did not use its available height: $detail", value.surface.bounds.height() >= maximumHeight - 1)
        assertTrue(detail, value.bodyHeight > value.scroll.bounds.height() && value.scroll.bounds.height() > 0)
        assertTrue(detail, value.title.bounds.bottom <= value.scroll.bounds.top)
        assertTrue(detail, value.scroll.bounds.bottom <= value.actions.bounds.top)
        assertFullyVisible("surface", value.surface, value.safe, value)
        assertFullyVisible("title", value.title, value.safe, value)
        assertFullyVisible("actions", value.actions, value.safe, value)
        assertFullyVisible("positive action", value.positive, value.safe, value)
        assertFullyVisible("negative action", value.negative, value.safe, value)
        val target = (48 * value.density + 0.5f).toInt()
        for (button in listOf(value.positive, value.negative)) {
            assertTrue(detail, button.bounds.width() >= target && button.bounds.height() >= target)
        }
        assertEquals(detail, value.expectedTitlePixels, value.titlePixels, 0.5f)
        if (Build.VERSION.SDK_INT < 26) assertEquals(detail, 0xff121212.toInt(), value.navigationColor)
    }

    private fun assertFullyVisible(role: String, element: Element, available: Rect, sample: Snapshot) {
        val detail = "$role; ${sample.describe()}"
        assertTrue(detail, element.visible)
        assertEquals(detail, element.bounds, element.visibleBounds)
        assertTrue(detail, available.contains(element.bounds))
    }

    private fun portrait(scenario: ActivityScenario<DialogGeometryActivity>) {
        scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        stable(scenario) { it.orientation == Configuration.ORIENTATION_PORTRAIT && it.focus }
    }

    private fun stable(scenario: ActivityScenario<DialogGeometryActivity>, ready: (Snapshot) -> Boolean): Snapshot {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        var previous: Snapshot? = null
        var stableSince = 0L
        var lastFailure: Throwable? = null
        while (SystemClock.elapsedRealtime() < deadline) {
            val next = runCatching {
                var value: Snapshot? = null
                scenario.onActivity { value = snapshot(it) }
                requireNotNull(value)
            }.onFailure { lastFailure = it }.getOrNull()
            if (next != null && next.focus && ready(next) && next == previous) {
                if (SystemClock.elapsedRealtime() - stableSince >= 400) return next
            } else {
                stableSince = SystemClock.elapsedRealtime()
            }
            if (next != null) previous = next
            SystemClock.sleep(80)
        }
        throw AssertionError("Window/IME did not become ready and stable; ${previous?.describe()}; " +
            "defaultIme=${Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)}", lastFailure)
    }

    @Suppress("DEPRECATION")
    private fun snapshot(activity: DialogGeometryActivity): Snapshot {
        val decor = activity.window.decorView
        check(decor.isAttachedToWindow && decor.isLaidOut && !decor.isLayoutRequested) { "The geometry window layout is not ready" }
        val dialog = activity.dialog
        val insets = requireNotNull(ViewCompat.getRootWindowInsets(decor)) { "Window insets are not attached yet" }
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        val visible = Rect().also(decor::getWindowVisibleDisplayFrame)
        val bounds = if (Build.VERSION.SDK_INT >= 30) Rect(activity.windowManager.currentWindowMetrics.bounds)
            else Point().also { activity.windowManager.defaultDisplay.getRealSize(it) }.let { Rect(0, 0, it.x, it.y) }
        val safe = Rect(bounds).apply {
            left += bars.left
            top += bars.top
            right -= bars.right
            bottom -= if (Build.VERSION.SDK_INT >= 30) maxOf(bars.bottom, ime.bottom) else bars.bottom
            if (Build.VERSION.SDK_INT < 30) check(intersect(visible)) { "Visible frame is outside the current display" }
        }
        val config = activity.resources.configuration
        val manager = activity.getSystemService(InputMethodManager::class.java)
        return Snapshot(bounds, visible, safe, element(dialog.root), element(dialog.surface), element(dialog.title), element(dialog.scroll),
            element(dialog.actions), element(activity.positive), element(activity.negative), element(activity.editor), element(activity.lastRow),
            dialog.content.height, dialog.scroll.scrollY, bars, ime, insets.isVisible(WindowInsetsCompat.Type.ime()),
            activity.hasWindowFocus(), activity.editor.hasFocus(), manager.isActive(activity.editor),
            config.orientation, config.layoutDirection, config.fontScale, config.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES,
            activity.resources.displayMetrics.density, dialog.title.textSize,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20f, activity.resources.displayMetrics), activity.window.navigationBarColor,
            activity.window.attributes.softInputMode, config.keyboard, config.hardKeyboardHidden, activity.lastShowAccepted)
    }

    private fun element(view: View): Element {
        val position = IntArray(2).also(view::getLocationOnScreen)
        val bounds = Rect(position[0], position[1], position[0] + view.width, position[1] + view.height)
        val visible = Rect()
        val shown = view.getGlobalVisibleRect(visible)
        // getGlobalVisibleRect is relative to the root View, whereas assertions use screen space.
        val rootPosition = IntArray(2).also(view.rootView::getLocationOnScreen)
        visible.offset(rootPosition[0], rootPosition[1])
        return Element(bounds, visible, shown)
    }

    private fun withWindow(overrides: DialogGeometryActivity.Overrides, run: (ActivityScenario<DialogGeometryActivity>) -> Unit) {
        check(!context.getSystemService(KeyguardManager::class.java).isKeyguardLocked) { "Unlock the selected geometry device" }
        val originalSettings = settings()
        val previousOverrides = DialogGeometryActivity.activeOverrides
        DialogGeometryActivity.activeOverrides = overrides
        var failure: Throwable? = null
        try {
            var originalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ActivityScenario.launch<DialogGeometryActivity>(Intent(context, DialogGeometryActivity::class.java)).useOwnedWindow(
                "DialogGeometry-${overrides.language}-${overrides.fontScale}-${overrides.dark}",
                beforeFinish = { it.hideKeyboard(); it.requestedOrientation = originalOrientation },
            ) { scenario ->
                scenario.onActivity { originalOrientation = it.requestedOrientation }
                run(scenario)
            }
        } catch (error: Throwable) {
            failure = error
            UiTestFailures.capture("DialogGeometry", error)
            throw error
        } finally {
            preserveTestFailure(failure) {
                DialogGeometryActivity.activeOverrides = previousOverrides
                assertEquals("The geometry fixture changed global/application appearance or the selected IME", originalSettings, settings())
            }
        }
    }

    private fun settings(): SettingsSnapshot {
        val system = Resources.getSystem().configuration
        val application = context.applicationContext.resources.configuration
        return SettingsSnapshot(system.locales.toLanguageTags(), system.fontScale, system.uiMode and Configuration.UI_MODE_NIGHT_MASK,
            application.locales.toLanguageTags(), application.fontScale, application.uiMode and Configuration.UI_MODE_NIGHT_MASK,
            AppCompatDelegate.getDefaultNightMode(), Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD))
    }

    private fun waitFor(timeoutMillis: Long, message: String, ready: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        while (!ready()) {
            check(SystemClock.elapsedRealtime() < deadline) { message }
            SystemClock.sleep(50)
        }
    }

    private fun report(label: String, value: Snapshot) {
        instrumentation.sendStatus(0, Bundle().apply { putString("realDialogGeometry", "${Build.MODEL} API=${Build.VERSION.SDK_INT} $label ${value.describe()}") })
        if (InstrumentationRegistry.getArguments().getString("geometryCapture") == "true") {
            val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            val file = File(context.cacheDir, "p3-geometry-$label.png")
            try { file.outputStream().use { check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)) } }
            finally { screenshot.recycle() }
            instrumentation.sendStatus(0, Bundle().apply { putString("realDialogScreenshot", file.absolutePath) })
        }
    }

    private data class Element(val bounds: Rect, val visibleBounds: Rect, val visible: Boolean)
    private data class Snapshot(
        val window: Rect, val visibleFrame: Rect, val safe: Rect, val root: Element, val surface: Element,
        val title: Element, val scroll: Element, val actions: Element, val positive: Element, val negative: Element,
        val editor: Element, val lastRow: Element, val bodyHeight: Int, val scrollY: Int, val bars: Insets, val ime: Insets,
        val imeVisible: Boolean, val focus: Boolean, val editorFocused: Boolean, val editorActive: Boolean,
        val orientation: Int, val direction: Int, val fontScale: Float, val dark: Boolean, val density: Float,
        val titlePixels: Float, val expectedTitlePixels: Float, val navigationColor: Int,
        val softInputMode: Int, val keyboard: Int, val hardKeyboardHidden: Int, val showAccepted: Boolean?,
    ) {
        fun describe() = "window=$window visibleFrame=$visibleFrame safe=$safe bars=$bars ime=$ime imeVisible=$imeVisible " +
            "root=${root.bounds} surface=$surface title=$title scroll=${scroll.bounds} bodyHeight=$bodyHeight scrollY=$scrollY " +
            "actions=$actions positive=$positive negative=$negative editor=$editor lastRow=$lastRow " +
            "focus=$focus editorFocused=$editorFocused editorActive=$editorActive orientation=$orientation direction=$direction " +
            "fontScale=$fontScale dark=$dark density=$density softInputMode=$softInputMode keyboard=$keyboard " +
            "hardKeyboardHidden=$hardKeyboardHidden showAccepted=$showAccepted"
    }
    private data class SettingsSnapshot(val systemLocales: String, val systemFont: Float, val systemNight: Int,
        val applicationLocales: String, val applicationFont: Float, val applicationNight: Int, val defaultNight: Int, val inputMethod: String?)
}
