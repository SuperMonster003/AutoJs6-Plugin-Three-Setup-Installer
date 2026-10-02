package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.InputDevice
import android.view.Display
import android.view.KeyEvent
import android.view.KeyCharacterMap
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.NestedScrollView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.GsonBuilder
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.AboutActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.DefaultInstallerActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.HomeActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstalledAppsActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallProfilesActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.ReleaseHistoryActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.SettingsActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.AppearancePreferences
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.HostAppearanceActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.Closeable
import java.io.File
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

/** Real standalone windows and the existing IME. No installation, authorization, network or global settings writes. */
@RunWith(AndroidJUnit4::class)
class StandaloneAppearanceDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val pages = if (InstrumentationRegistry.getArguments().getString("profileAppearanceOnly") == "true")
        listOf(InstallProfilesActivity::class.java)
    else listOf(HomeActivity::class.java, SettingsActivity::class.java, DefaultInstallerActivity::class.java,
        InstalledAppsActivity::class.java, InstallProfilesActivity::class.java, AboutActivity::class.java, ReleaseHistoryActivity::class.java)

    @Test fun lightNavajoPagesKeepControlsInsideInsets() = withRestoredState {
        exercisePages(Case("en-light-font1", "en", 1f, false, null))
    }

    @Test fun largeArabicDarkNarrowPagesKeepControlsAndColorPickerAboveIme() = withRestoredState {
        exercisePages(Case("ar-dark-font2-content360", "ar", 2f, true, 360))
    }

    private fun exercisePages(case: Case) {
        assertTrue(AppearancePreferences(case.language, if (case.dark) "dark" else "light", AppearancePreferences.DEFAULT_COLOR).save(context))
        for (page in pages) {
            ActivityScenario.launch<HostAppearanceActivity>(Intent(context, page)).useOwnedWindow("${case.id}-${page.simpleName}") { scenario ->
                var original: Configuration? = null
                var originalMetrics: DisplayMetrics? = null
                var privateResources: Resources? = null
                var owner: HostAppearanceActivity? = null
                var contentConstraint: Closeable? = null
                var originallyKeptScreenOn = false
                var windowFailure: Throwable? = null
                try {
                    scenario.onActivity { activity ->
                        owner = activity
                        originallyKeptScreenOn = activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
                        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        privateResources = activity.resources
                        original = Configuration(activity.resources.configuration)
                        originalMetrics = DisplayMetrics().apply { setTo(activity.resources.displayMetrics) }
                        contentConstraint = applyPrivateConfiguration(activity, case)
                        hideKeyboard(activity.window)
                    }
                    // Let WindowManager apply the test-scoped keep-screen flag before measuring
                    // or injecting a tap. A DIM display can consume DOWN solely to become bright.
                    instrumentation.waitForIdleSync()
                    val top = stable(scenario, "${case.id}-${page.simpleName}-top") { activity -> pageSample(activity, bottom = false) }
                    report("${case.id}-${page.simpleName}-top", top)
                    checkPage(top, case)
                    scenario.onActivity { activity ->
                        when (val scroll = pageScroll(activity)) {
                            is NestedScrollView -> {
                                // fullScroll() may focus the selectable document and bring its
                                // caret back to the first line. Scroll by pixels without focusing it.
                                activity.window.decorView.findFocus()?.clearFocus()
                                activity.window.decorView.requestFocus()
                                val child = requireNotNull(scroll.getChildAt(0))
                                val margins = child.layoutParams as? ViewGroup.MarginLayoutParams
                                val contentHeight = child.height + (margins?.topMargin ?: 0) + (margins?.bottomMargin ?: 0)
                                val viewportHeight = scroll.height - scroll.paddingTop - scroll.paddingBottom
                                scroll.scrollTo(0, (contentHeight - viewportHeight).coerceAtLeast(0))
                            }
                            is ListView -> scroll.setSelection(scroll.count - 1)
                        }
                    }
                    val bottom = stable(scenario, "${case.id}-${page.simpleName}-bottom") { activity ->
                        pageSample(activity, bottom = true).also {
                            check(!pageScroll(activity).canScrollVertically(1)) { "The last row is not reached yet" }
                        }
                    }
                    report("${case.id}-${page.simpleName}-bottom", bottom)
                    checkPage(bottom, case)
                    assertEquals("Scrolling moved the fixed header", top.header, bottom.header)
                    when (page) {
                        HomeActivity::class.java -> exerciseHomeMenu(scenario, case)
                        SettingsActivity::class.java -> {
                            exerciseScrolledSettingsAction(scenario, case)
                            exerciseColorPicker(scenario, case)
                        }
                    }
                } catch (failure: Throwable) {
                    windowFailure = failure
                    UiTestFailures.capture("${case.id}-${page.simpleName}", failure)
                    runCatching { report("${case.id}-${page.simpleName}-failure", mapOf("failure" to failure.toString(), "cause" to failure.cause?.toString())) }
                    throw failure
                } finally {
                    preserveTestFailure(windowFailure) {
                        instrumentation.runOnMainSync {
                            contentConstraint?.close()
                            owner?.takeUnless { it.isDestroyed }?.let { activity ->
                                (activity as? SettingsActivity)?.prompt?.dismiss()
                                hideKeyboard(activity.window)
                                if (!originallyKeptScreenOn) activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            }
                            @Suppress("DEPRECATION")
                            original?.let { privateResources?.updateConfiguration(it, originalMetrics) }
                        }
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun applyPrivateConfiguration(activity: HostAppearanceActivity, case: Case): Closeable? {
        // HostAppearanceActivity has already attached its own createConfigurationContext. Update
        // only that Resources instance, then use the real production configuration-change path.
        assertNotSame(activity.applicationContext.resources, activity.resources)
        val metrics = DisplayMetrics().apply { setTo(activity.resources.displayMetrics) }
        val configuration = Configuration(activity.resources.configuration).apply {
            fontScale = case.fontScale
            case.widthDp?.let { requested ->
                val width = min(requested, (metrics.widthPixels / metrics.density).toInt())
                screenWidthDp = width
                smallestScreenWidthDp = min(smallestScreenWidthDp, width)
                metrics.widthPixels = (width * metrics.density + 0.5f).toInt()
            }
        }
        activity.resources.updateConfiguration(configuration, metrics)
        activity.onConfigurationChanged(configuration)
        if (case.widthDp != null) {
            // Constrain only app content. Resizing an ordinary non-floating Activity window can
            // activate OEM letterboxing/input transforms. This is not an OS multi-window audit.
            val content = activity.findViewById<ViewGroup>(android.R.id.content)
            val original = FrameLayout.LayoutParams(content.getChildAt(0).layoutParams as FrameLayout.LayoutParams)
            fun applyWidth() {
                val root = content.getChildAt(0) ?: return
                val params = root.layoutParams as FrameLayout.LayoutParams
                if (params.width != metrics.widthPixels || params.gravity != (Gravity.TOP or Gravity.CENTER_HORIZONTAL)) {
                    root.layoutParams = FrameLayout.LayoutParams(params).apply {
                        width = metrics.widthPixels
                        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    }
                }
            }
            applyWidth()
            val listener = android.view.ViewTreeObserver.OnGlobalLayoutListener(::applyWidth)
            content.viewTreeObserver.addOnGlobalLayoutListener(listener)
            instrumentation.sendStatus(0, Bundle().apply {
                putString("standaloneAppearanceScope", "narrow-content-only widthDp=${case.widthDp}; real Window unchanged; OS multi-window not tested")
            })
            return Closeable {
                content.viewTreeObserver.takeIf { it.isAlive }?.removeOnGlobalLayoutListener(listener)
                content.getChildAt(0)?.layoutParams = FrameLayout.LayoutParams(original)
            }
        }
        return null
    }

    private fun pageSample(activity: HostAppearanceActivity, bottom: Boolean): PageSample {
        val decor = activity.window.decorView
        check(activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
        check(context.getSystemService(PowerManager::class.java).isInteractive) { "The display is not interactive" }
        check(decor.hasWindowFocus() && !decor.isLayoutRequested) { "Page window has not settled" }
        val root = (activity.findViewById<ViewGroup>(android.R.id.content)).getChildAt(0)
        val headerText = descendants(root).filterIsInstance<TextView>().first { ViewCompat.isAccessibilityHeading(it) }
        val header = headerText.parent as View
        val scroll = pageScroll(activity)
        check(scroll.height > 0 && !scroll.isLayoutRequested)
        if (activity is HomeActivity || activity is DefaultInstallerActivity) {
            val loading = activity.getString(if (activity is HomeActivity) R.string.home_loading else R.string.default_installer_loading)
            check(descendants(root).filterIsInstance<TextView>().none { it.text.toString() == loading }) { "Local status cards are still loading" }
        }
        if (activity is InstalledAppsActivity) {
            val state = decor.findViewWithTag<TextView>(InstalledAppsActivity.TAG_STATUS).text.toString()
            check(state != activity.getString(R.string.apps_loading)) { "Application list is loading" }
            assertNotEquals(activity.getString(R.string.apps_load_failed), state)
        }
        if (activity is ReleaseHistoryActivity) check(decor.findViewWithTag<TextView>("document").text.toString() != activity.getString(R.string.default_installer_loading))
        val edgeRoot = if (scroll is ListView) {
            check(scroll.count > 0 && scroll.childCount > 0)
            val position = if (bottom) scroll.count - 1 else 0
            check(position in scroll.firstVisiblePosition..scroll.lastVisiblePosition) { "The requested adapter edge is not attached yet" }
            requireNotNull(scroll.getChildAt(position - scroll.firstVisiblePosition)) { "The actual adapter edge has no attached row" }
        } else scroll
        // Collapsed actions keep their child TextView.visibility == VISIBLE but have a GONE
        // ancestor and were never laid out. Sample the effective row contents, not those actions.
        val texts = descendants(edgeRoot).filterIsInstance<TextView>().filter { it.isShown && it.text.isNotBlank() }.toList()
        check(texts.isNotEmpty())
        val edge = if (bottom) texts.last() else texts.first()
        val layout = requireNotNull(edge.layout) { "The visible adapter-edge text has not completed layout: ${edge.text}" }
        val nonemptyLines = (0 until layout.lineCount).filter { line -> edge.text.substring(layout.getLineStart(line), layout.getLineEnd(line)).isNotBlank() }
        check(nonemptyLines.isNotEmpty())
        val line = if (bottom) nonemptyLines.last() else nonemptyLines.first()
        val geometry = windowGeometry(activity.window)
        check(!geometry.imeVisible) { "Unexpected keyboard on the page" }
        val config = activity.resources.configuration
        val extra = when (activity) {
            is HomeActivity -> listOf("home-more", "home-pick")
            is InstallProfilesActivity -> listOf("profiles-add", "profiles-cancel", "profiles-save")
            else -> emptyList()
        }.map { element(requireNotNull(decor.findViewWithTag<View>(it))) }
        return PageSample(geometry, element(root), element(header), element(headerText), element(scroll), lineBounds(edge, line),
            layout.getEllipsisCount(line), headerText.textSize,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20f, activity.resources.displayMetrics),
            config.fontScale, root.layoutDirection, config.uiMode and Configuration.UI_MODE_NIGHT_MASK,
            activity.kit.palette.background, activity.kit.palette.isDark, activity.appearance.primary,
            activity.resources.displayMetrics.density, activity.window.navigationBarColor, extra,
            "${activity.javaClass.simpleName}:${if (bottom) "last" else "first"} ${edge.text.take(80)}")
    }

    private fun checkPage(sample: PageSample, case: Case) {
        val detail = sample.toString()
        assertEquals(detail, case.fontScale, sample.fontScale, 0.001f)
        assertEquals(detail, if (case.language == "ar") View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR, sample.direction)
        assertEquals(detail, if (case.dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO, sample.night)
        assertEquals(detail, case.dark, sample.darkPalette)
        assertEquals(detail, AppearancePreferences.DEFAULT_COLOR, sample.seed)
        assertEquals(detail, if (case.dark) 0xff121212.toInt() else 0xfff3f4f5.toInt(), sample.background)
        assertEquals(detail, sample.expectedTitlePixels, sample.titlePixels, 0.5f)
        assertFullyVisible("header", sample.header, sample.geometry.safe, detail)
        assertFullyVisible("header text", sample.title, sample.header.bounds, detail)
        assertFullyVisible("scroll viewport", sample.scroll, sample.geometry.safe, detail)
        assertTrue("First/last text line is clipped: $detail", containsWithRounding(sample.scroll.bounds, sample.edgeLine))
        assertEquals("First/last text line was ellipsized: $detail", 0, sample.ellipsis)
        assertTrue("The header overlaps scrolling content: $detail", sample.header.bounds.bottom <= sample.scroll.bounds.top)
        sample.fixedActions.forEach { assertFullyVisible("fixed Home action", it, sample.geometry.safe, detail); assertTouchTarget(it, sample.density, detail) }
        case.widthDp?.let {
            val expected = min(it.toFloat(), sample.geometry.window.width() / sample.density)
            assertEquals("The root content was not constrained to the requested dp width: $detail", expected,
                sample.content.bounds.width() / sample.density, 1f)
            assertTrue("The narrow content escaped its real window: $detail", sample.geometry.window.contains(sample.content.bounds))
        }
        if (Build.VERSION.SDK_INT < 26) assertEquals(detail, 0xff121212.toInt(), sample.navigationColor)
    }

    private fun exerciseHomeMenu(scenario: ActivityScenario<HostAppearanceActivity>, case: Case) {
        var label = ""
        val target = stable(scenario, "Home menu target") { activity ->
            label = activity.getString(R.string.settings_title)
            element(activity.window.decorView.findViewWithTag("home-more"))
        }
        val beforeWindow = instrumentation.uiAutomation.rootInActiveWindow?.windowId ?: -1
        tap(scenario, "Home menu") { it.window.decorView.findViewWithTag("home-more") }
        await("The measured Home menu button did not open its menu") {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            root != null && root.windowId != beforeWindow && root.findAccessibilityNodeInfosByText(label).any { it.text?.toString() == label && it.isVisibleToUser }
        }
        report("${case.id}-HomeActivity-menu", target)
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        stable(scenario, "Home menu dismissal") { activity -> check(activity.window.decorView.hasWindowFocus()); element(activity.window.decorView.findViewWithTag("home-more")) }
    }

    private fun exerciseScrolledSettingsAction(scenario: ActivityScenario<HostAppearanceActivity>, case: Case) {
        val original = context.getSharedPreferences("installer_updates", Context.MODE_PRIVATE).all.toMap()
        var label = ""
        val row = stable(scenario, "last Settings row") { activity ->
            val view = activity.window.decorView.findViewWithTag<View>("settings-ignored")
            val element = element(view)
            assertFullyVisible("last Settings action", element, windowGeometry(activity.window).safe, element.toString())
            label = activity.getString(R.string.app_update_manage_ignored)
            element
        }
        val beforeWindow = instrumentation.uiAutomation.rootInActiveWindow?.windowId ?: -1
        tap(scenario, "last Settings row") { it.window.decorView.findViewWithTag("settings-ignored") }
        await("The last Settings row did not respond to a real touch") {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            root != null && root.windowId != beforeWindow && root.findAccessibilityNodeInfosByText(label).any { it.text?.toString() == label && it.isVisibleToUser }
        }
        report("${case.id}-SettingsActivity-last-row-dialog", row)
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        stable(scenario, "Settings dialog dismissal") { activity -> check(activity.window.decorView.hasWindowFocus()); element(activity.window.decorView.findViewWithTag("settings-ignored")) }
        assertEquals("Opening and cancelling ignored-version management changed preferences", original, context.getSharedPreferences("installer_updates", Context.MODE_PRIVATE).all)
    }

    private fun exerciseColorPicker(scenario: ActivityScenario<HostAppearanceActivity>, case: Case) {
        val before = context.getSharedPreferences("app-appearance", Context.MODE_PRIVATE).all.toMap()
        scenario.onActivity { activity ->
            val row = activity.window.decorView.findViewWithTag<View>("settings-color")
            row.requestRectangleOnScreen(Rect(0, 0, row.width, row.height), true)
        }
        stable(scenario, "theme selector row") { activity ->
            element(activity.window.decorView.findViewWithTag<View>("settings-color")).also {
                assertFullyVisible("theme selector", it, element(pageScroll(activity)).bounds, it.toString())
            }
        }
        tap(scenario, "theme selector") { it.window.decorView.findViewWithTag("settings-color") }
        stable(scenario, "theme picker creation") { activity ->
            val dialog = (activity as SettingsActivity).prompt as? AlertDialog ?: error("Color picker is not open")
            check(dialog.isShowing && dialog.window?.decorView?.hasWindowFocus() == true)
            element(dialog.getButton(AlertDialog.BUTTON_NEGATIVE))
        }
        scenario.onActivity { activity -> hideKeyboard(((activity as SettingsActivity).prompt as AlertDialog).window!!) }
        val hidden = stable(scenario, "theme picker without IME") { activity -> colorSample(activity as SettingsActivity).also { check(!it.geometry.imeVisible) } }
        report("${case.id}-SettingsActivity-color-hidden", hidden)
        checkColorPicker(hidden)
        scenario.onActivity { activity ->
            val dialog = (activity as SettingsActivity).prompt as AlertDialog
            val input = dialog.window!!.decorView.findViewWithTag<EditText>("theme-color-input")
            input.setText("#123456")
            assertTrue(input.requestFocus())
            input.setSelection(input.length())
            input.requestRectangleOnScreen(Rect(0, 0, input.width, input.height), true)
            activity.getSystemService(InputMethodManager::class.java).showSoftInput(input, 0)
            WindowInsetsControllerCompat(dialog.window!!, dialog.window!!.decorView).show(WindowInsetsCompat.Type.ime())
        }
        val shown = stable(scenario, "theme picker with real IME") { activity ->
            colorSample(activity as SettingsActivity).also {
                check(it.editorFocused && it.editorActive)
                check(it.geometry.safe.bottom < hidden.geometry.safe.bottom)
                check(it.geometry.imeVisible || Build.VERSION.SDK_INT < 30 && it.geometry.visibleFrame.bottom < hidden.geometry.visibleFrame.bottom)
            }
        }
        report("${case.id}-SettingsActivity-color-ime", shown)
        checkColorPicker(shown)
        assertEquals("A theme-color draft escaped the dialog", before, context.getSharedPreferences("app-appearance", Context.MODE_PRIVATE).all)
        tap(scenario, "color picker Cancel", window = { ((it as SettingsActivity).prompt as AlertDialog).window!! }) {
            ((it as SettingsActivity).prompt as AlertDialog).getButton(AlertDialog.BUTTON_NEGATIVE)
        }
        await("The visible Cancel button did not close the color picker") {
            var dismissed = false
            scenario.onActivity { dismissed = (it as SettingsActivity).prompt?.isShowing != true }
            dismissed
        }
        scenario.onActivity { hideKeyboard(it.window) }
        assertEquals("Cancelling a color draft saved appearance preferences", before, context.getSharedPreferences("app-appearance", Context.MODE_PRIVATE).all)
    }

    private fun colorSample(activity: SettingsActivity): ColorSample {
        val dialog = activity.prompt as? AlertDialog ?: error("Color picker is not open")
        val window = requireNotNull(dialog.window)
        check(window.decorView.hasWindowFocus() && !window.decorView.isLayoutRequested)
        val input = window.decorView.findViewWithTag<EditText>("theme-color-input")
        return ColorSample(windowGeometry(window), element(dialog.getButton(AlertDialog.BUTTON_NEGATIVE)),
            element(dialog.getButton(AlertDialog.BUTTON_POSITIVE)), element(requireNotNull(dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle))),
            element(input), input.hasFocus(), activity.getSystemService(InputMethodManager::class.java).isActive(input),
            activity.resources.displayMetrics.density)
    }

    private fun checkColorPicker(sample: ColorSample) {
        val detail = sample.toString()
        assertFullyVisible("color dialog title", sample.title, sample.geometry.safe, detail)
        for ((label, button) in listOf("Cancel" to sample.cancel, "Confirm" to sample.confirm)) {
            assertFullyVisible(label, button, sample.geometry.safe, detail)
            assertTouchTarget(button, sample.density, detail)
        }
        assertFalse("Dialog actions overlap: $detail", Rect.intersects(sample.cancel.bounds, sample.confirm.bounds))
    }

    private fun pageScroll(activity: HostAppearanceActivity): View = descendants(activity.window.decorView)
        .first { it is NestedScrollView || it is ListView }

    private fun descendants(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (index in 0 until view.childCount) yieldAll(descendants(view.getChildAt(index)))
    }

    private fun lineBounds(view: TextView, line: Int): Rect {
        val layout = requireNotNull(view.layout)
        val position = IntArray(2).also(view::getLocationOnScreen)
        return Rect(position[0] + view.totalPaddingLeft + floor(layout.getLineLeft(line).toDouble()).toInt(),
            position[1] + view.totalPaddingTop + layout.getLineTop(line),
            position[0] + view.totalPaddingLeft + ceil(layout.getLineRight(line).toDouble()).toInt(),
            position[1] + view.totalPaddingTop + layout.getLineBottom(line))
    }

    private fun element(view: View): Element {
        val position = IntArray(2).also(view::getLocationOnScreen)
        val bounds = Rect(position[0], position[1], position[0] + view.width, position[1] + view.height)
        val visible = Rect()
        val shown = view.getGlobalVisibleRect(visible)
        val rootPosition = IntArray(2).also(view.rootView::getLocationOnScreen)
        visible.offset(rootPosition[0], rootPosition[1])
        return Element(bounds, visible, shown)
    }

    private fun windowGeometry(window: Window): Geometry {
        val decor = window.decorView
        val bounds = element(decor).bounds
        val insets = requireNotNull(ViewCompat.getRootWindowInsets(decor))
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        val visible = Rect().also(decor::getWindowVisibleDisplayFrame)
        // Visible-frame edges are absolute screen coordinates, including a floating dialog's IME
        // boundary. The full Activity window stays unchanged for narrow-content fixtures.
        val safe = Rect(bounds)
        check(safe.intersect(visible)) { "The window has no visible safe region" }
        if (decor.width == visible.width() && Build.VERSION.SDK_INT >= 30) {
            safe.left = maxOf(safe.left, bounds.left + bars.left)
            safe.top = maxOf(safe.top, bounds.top + bars.top)
            safe.right = minOf(safe.right, bounds.right - bars.right)
            safe.bottom = minOf(safe.bottom, bounds.bottom - maxOf(bars.bottom, ime.bottom))
        }
        return Geometry(bounds, visible, safe, insets.isVisible(WindowInsetsCompat.Type.ime()))
    }

    private fun assertFullyVisible(role: String, value: Element, available: Rect, detail: String) {
        assertTrue("$role is not visible: $detail", value.shown)
        assertEquals("$role is clipped by a parent: $detail", value.bounds, value.visible)
        assertTrue("$role is outside the safe window: $detail", containsWithRounding(available, value.bounds))
    }

    private fun containsWithRounding(outer: Rect, inner: Rect) = Rect(outer).apply { inset(-1, -1) }.contains(inner)
    private fun assertTouchTarget(value: Element, density: Float, detail: String) {
        val minimum = (48 * density + 0.5f).toInt()
        assertTrue("Action target is smaller than 48 dp: $detail", value.bounds.width() >= minimum && value.bounds.height() >= minimum)
    }

    private fun hideKeyboard(window: Window) {
        val decor = window.decorView
        decor.findFocus()?.clearFocus()
        decor.isFocusableInTouchMode = true
        decor.requestFocus()
        WindowInsetsControllerCompat(window, decor).hide(WindowInsetsCompat.Type.ime())
        context.getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(decor.windowToken, 0)
    }

    private fun tap(scenario: ActivityScenario<HostAppearanceActivity>, label: String,
        window: (HostAppearanceActivity) -> Window = { it.window }, target: (HostAppearanceActivity) -> View) {
        instrumentation.waitForIdleSync()
        val measured = stable(scenario, "$label touch readiness") { activity ->
            val currentWindow = window(activity)
            val decor = currentWindow.decorView
            val view = target(activity)
            check(decor.hasWindowFocus() && decor.isAttachedToWindow && !decor.isLayoutRequested)
            check(view.isShown && view.isEnabled && view.isClickable && !view.isLayoutRequested)
            check(view.windowToken == decor.windowToken)
            val element = element(view)
            assertFullyVisible(label, element, windowGeometry(currentWindow).safe, element.toString())
            TouchSample(element, decor.display?.displayId ?: Display.DEFAULT_DISPLAY)
        }
        instrumentation.uiAutomation.waitForIdle(100, 3_000)
        scenario.onActivity { activity ->
            assertTrue("$label lost window focus before its real touch", window(activity).decorView.hasWindowFocus())
            assertEquals("$label moved after its touch coordinates were measured", measured.element, element(target(activity)))
        }
        val properties = arrayOf(MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_FINGER })
        val bounds = measured.element.bounds
        val coordinates = arrayOf(MotionEvent.PointerCoords().apply {
            x = bounds.exactCenterX(); y = bounds.exactCenterY(); pressure = 1f; size = 1f
        })
        val downTime = SystemClock.uptimeMillis()
        fun inject(action: Int): Boolean {
            coordinates[0].pressure = if (action == MotionEvent.ACTION_DOWN) 1f else 0f
            val event = if (Build.VERSION.SDK_INT >= 34) {
                requireNotNull(MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, 1, properties, coordinates,
                    0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, measured.displayId, 0, MotionEvent.CLASSIFICATION_NONE))
            } else {
                check(measured.displayId == Display.DEFAULT_DISPLAY) { "This API's public MotionEvent factory cannot select a secondary display" }
                MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, 1, properties, coordinates,
                    0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0)
            }
            try {
                val accepted = instrumentation.uiAutomation.injectInputEvent(event, true)
                instrumentation.sendStatus(0, Bundle().apply {
                    putString("standaloneAppearanceTouch", "$label action=${MotionEvent.actionToString(action)} accepted=$accepted " +
                        "source=${event.source} tool=${event.getToolType(0)} display=${measured.displayId} elapsed=${SystemClock.uptimeMillis() - downTime} bounds=$bounds")
                })
                return accepted
            } finally { event.recycle() }
        }
        var completed = false
        try {
            assertTrue("Touch DOWN was rejected for $label at $bounds, display=${measured.displayId}", inject(MotionEvent.ACTION_DOWN))
            SystemClock.sleep(80)
            assertTrue("Touch UP was rejected for $label at $bounds, display=${measured.displayId}", inject(MotionEvent.ACTION_UP))
            completed = true
        } finally {
            if (!completed) runCatching { inject(MotionEvent.ACTION_CANCEL) }
        }
    }

    private fun <T> stable(scenario: ActivityScenario<HostAppearanceActivity>, label: String, sample: (HostAppearanceActivity) -> T): T {
        var previous: T? = null
        var stableSince = 0L
        var failure: Throwable? = null
        val deadline = SystemClock.elapsedRealtime() + 20_000
        while (SystemClock.elapsedRealtime() < deadline) {
            var next: T? = null
            scenario.onActivity { activity -> runCatching { sample(activity) }.onSuccess { next = it }.onFailure { failure = it } }
            if (next != null && next == previous) {
                if (SystemClock.elapsedRealtime() - stableSince >= 400) return checkNotNull(next)
            } else stableSince = SystemClock.elapsedRealtime()
            previous = next
            SystemClock.sleep(80)
        }
        throw AssertionError("Standalone window did not become stable: $label; last=$previous", failure)
    }

    private fun await(message: String, ready: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (SystemClock.elapsedRealtime() < deadline) { if (ready()) return; SystemClock.sleep(50) }
        throw AssertionError(message)
    }

    private fun report(label: String, value: Any) {
        val directory = File(checkNotNull(context.getExternalFilesDir(null)), "p5-standalone-appearance").apply { check(isDirectory || mkdirs()) }
        val json = File(directory, "$label.json")
        json.writeText(GsonBuilder().setPrettyPrinting().create().toJson(value))
        instrumentation.sendStatus(0, Bundle().apply { putString("standaloneAppearance", "${Build.MODEL} API=${Build.VERSION.SDK_INT} $label $value"); putString("standaloneAppearanceGeometry", json.absolutePath) })
        if (InstrumentationRegistry.getArguments().getString("standaloneAppearanceCapture") != "false") {
            val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            val file = File(directory, "$label.png")
            try { file.outputStream().use { check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)) } }
            finally { screenshot.recycle() }
            instrumentation.sendStatus(0, Bundle().apply { putString("standaloneAppearanceScreenshot", file.absolutePath) })
        }
    }

    private fun withRestoredState(run: () -> Unit) {
        if (InstrumentationRegistry.getArguments().getString("standaloneAppearanceRestoreOnly") == "true") {
            val plan = StandaloneAppearanceRecovery.restore(context)
            recoveryStatus("RESTORE_ONLY", plan)
            return
        }
        // Wake without toggling power or altering timeout/brightness settings. The Activity flag
        // below maintains the interactive test window; neither operation dismisses a keyguard.
        val wakeTime = SystemClock.uptimeMillis()
        for (action in intArrayOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
            val event = KeyEvent(wakeTime, SystemClock.uptimeMillis(), action, KeyEvent.KEYCODE_WAKEUP, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_KEYBOARD)
            assertTrue("The test wake-up key was rejected", instrumentation.uiAutomation.injectInputEvent(event, true))
        }
        await("The test display did not become interactive") { context.getSystemService(PowerManager::class.java).isInteractive }
        check(!context.getSystemService(KeyguardManager::class.java).isKeyguardLocked) { "Unlock the display-test device" }
        val preferences = context.getSharedPreferences("app-appearance", Context.MODE_PRIVATE)
        val before = preferences.all.toMap()
        val unrelated = listOf("installer_settings", "installer_updates").associateWith { context.getSharedPreferences(it, Context.MODE_PRIVATE).all.toMap() }
        val global = globalSettings()
        val plan = StandaloneAppearanceRecovery.begin(context)
        var failure: Throwable? = null
        try {
            recoveryStatus("DISPLAY_AUDIT", plan)
            run()
        } catch (error: Throwable) {
            failure = error
            UiTestFailures.capture("StandaloneAppearance", error)
            throw error
        }
        finally {
            preserveTestFailure(failure) {
                StandaloneAppearanceRecovery.restore(context, plan.runId, drainPendingLauncherWork = true)
                recoveryStatus("RESTORED", plan)
                assertEquals("The test did not restore the exact plugin appearance keys", before, preferences.all)
                for ((file, saved) in unrelated) assertEquals("Display-only actions changed $file", saved, context.getSharedPreferences(file, Context.MODE_PRIVATE).all)
                assertEquals("The display test changed global/application appearance or the selected IME", global, globalSettings())
            }
        }
    }

    private fun recoveryStatus(mode: String, plan: StandaloneAppearanceRecovery.Plan) {
        instrumentation.sendStatus(0, Bundle().apply {
            putString("standaloneAppearanceMode", mode)
            putString("standaloneAppearanceRecoveryPlan", plan.internalPath)
            putString("standaloneAppearanceRecoveryCopy", plan.readableCopy)
            putString("standaloneAppearanceRecoveryRun", plan.runId)
        })
    }

    private fun globalSettings(): List<Any?> {
        val system = Resources.getSystem().configuration
        val application = context.applicationContext.resources.configuration
        return listOf(system.locales.toLanguageTags(), system.fontScale, system.uiMode and Configuration.UI_MODE_NIGHT_MASK,
            application.locales.toLanguageTags(), application.fontScale, application.uiMode and Configuration.UI_MODE_NIGHT_MASK,
            AppCompatDelegate.getDefaultNightMode(), Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD))
    }

    private data class Case(val id: String, val language: String, val fontScale: Float, val dark: Boolean, val widthDp: Int?)
    private data class Element(val bounds: Rect, val visible: Rect, val shown: Boolean)
    private data class TouchSample(val element: Element, val displayId: Int)
    private data class Geometry(val window: Rect, val visibleFrame: Rect, val safe: Rect, val imeVisible: Boolean)
    private data class PageSample(val geometry: Geometry, val content: Element, val header: Element, val title: Element, val scroll: Element, val edgeLine: Rect,
        val ellipsis: Int, val titlePixels: Float, val expectedTitlePixels: Float, val fontScale: Float, val direction: Int, val night: Int,
        val background: Int, val darkPalette: Boolean, val seed: Int, val density: Float, val navigationColor: Int,
        val fixedActions: List<Element>, val label: String)
    private data class ColorSample(val geometry: Geometry, val cancel: Element, val confirm: Element, val title: Element, val editor: Element,
        val editorFocused: Boolean, val editorActive: Boolean, val density: Float)
}
