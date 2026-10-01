package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.kit

import android.content.res.Configuration
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.R
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.HostAppearance
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.wrap
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Deterministic view measurement complements, but does not replace, real window/device checks. */
@RunWith(AndroidJUnit4::class)
class InstallerDialogGeometryDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun phoneTabletAndNarrowLandscapeKeepTitleAndActionsOutsideTheScrollableBody() = instrumentation.runOnMainSync {
        for ((width, height) in listOf(320 to 640, 1000 to 800, 480 to 320)) {
            val kit = kit("en", false, 1f)
            val dialog = populatedDialog(kit)
            measure(dialog, kit.dp(width), kit.dp(height))
            val geometry = report(dialog, "$width x $height dp, long body")
            assertTrue(geometry, dialog.surface.width <= kit.dp(560))
            assertTrue(geometry, dialog.surface.left >= kit.dp(24))
            assertTrue(geometry, dialog.root.width - dialog.surface.right >= kit.dp(24))
            assertTrue(geometry, dialog.surface.height <= (dialog.root.height * 0.85f).toInt())
            assertEquals(geometry, 0, dialog.scroll.scrollY)
            assertTrue(geometry, dialog.scroll.height > 0)
            assertTrue(geometry, dialog.content.height > dialog.scroll.height)
            assertEquals(geometry, dialog.title.bottom, dialog.scroll.top)
            assertEquals(geometry, dialog.scroll.bottom, dialog.actions.top)
            assertTrue(geometry, dialog.actions.bottom <= dialog.surface.height)
        }
    }

    @Test fun shortBodyWrapsNaturallyWithoutArtificialScrollingOrEmptyExpansion() = instrumentation.runOnMainSync {
        val kit = kit("en", false, 1f)
        val dialog = kit.dialog("Installation").also {
            it.content.addView(kit.text("Ready to install.", 16f))
            it.actions.addView(kit.textButton("Cancel") {})
            it.actions.addView(kit.textButton("Install") {})
        }
        measure(dialog, kit.dp(1000), kit.dp(800))
        val geometry = report(dialog, "1000 x 800 dp, short body")
        assertEquals(geometry, dialog.content.height, dialog.scroll.height)
        assertEquals(geometry, dialog.title.height + dialog.scroll.height + dialog.actions.height, dialog.surface.height)
        assertFalse(geometry, dialog.scroll.canScrollVertically(1))
        assertFalse(geometry, dialog.scroll.canScrollVertically(-1))
        assertTrue(geometry, dialog.surface.height < (dialog.root.height * 0.85f).toInt())
    }

    @Test fun rtlLargeFontAndLongLabelsWrapWithoutReducingTouchTargets() = instrumentation.runOnMainSync {
        val kit = kit("ar", true, 2f)
        val dialog = populatedDialog(kit, "تثبيت التطبيق", "إلغاء التثبيت")
        measure(dialog, kit.dp(320), kit.dp(640))
        report(dialog, "320 x 640 dp, RTL, fontScale=2")
        assertEquals(View.LAYOUT_DIRECTION_RTL, dialog.root.layoutDirection)
        assertEquals(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20f, kit.context.resources.displayMetrics), dialog.title.textSize, 0.5f)
        assertTrue(dialog.scroll.height > 0)
        for (index in 0 until dialog.actions.childCount) {
            val button = dialog.actions.getChildAt(index)
            assertTrue(button.height >= kit.dp(48))
            assertTrue(button.width >= kit.dp(48))
            assertTrue(button.bottom <= dialog.actions.height)
        }
    }

    @Test fun systemBarsCutoutsAndKeyboardReduceTheAvailableViewport() = instrumentation.runOnMainSync {
        val kit = kit("en", false, 1f)
        val dialog = populatedDialog(kit)
        val bars = Insets.of(kit.dp(12), kit.dp(30), kit.dp(8), kit.dp(24))
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.systemBars(), bars)
            .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, kit.dp(180)))
            .build()
        ViewCompat.dispatchApplyWindowInsets(dialog.root, insets)
        measure(dialog, kit.dp(400), kit.dp(800))
        report(dialog, "400 x 800 dp, injected insets")
        assertTrue(dialog.root.paddingTop >= bars.top)
        assertTrue(dialog.root.paddingBottom >= bars.bottom)
        assertTrue(dialog.surface.left >= dialog.root.paddingLeft)
        assertTrue(dialog.surface.right <= dialog.root.width - dialog.root.paddingRight)
        assertTrue(dialog.surface.top >= dialog.root.paddingTop)
        assertTrue(dialog.surface.bottom <= dialog.root.height - dialog.root.paddingBottom)
    }

    @Test fun controlsUseResolvedColorsWhenCreatedLaterAndExposeIndependentDisabledStates() = instrumentation.runOnMainSync {
        val kit = kit("en", true, 1f)
        val choice = kit.checkBox("Package", false)
        assertEquals(kit.palette.accent, choice.buttonTintList!!.getColorForState(intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked), 0))
        assertEquals(kit.palette.muted, choice.buttonTintList!!.getColorForState(intArrayOf(android.R.attr.state_enabled), 0))
        val button = kit.textButton("Install") {}
        assertEquals(kit.palette.accent, button.currentTextColor)
        button.isEnabled = false
        assertEquals(kit.palette.disabledText, button.currentTextColor)
        assertNotEquals(kit.palette.disabledFill, button.currentTextColor)
        assertEquals(kit.palette.accent, kit.progressBar().indicatorColor.single())
    }

    private fun kit(language: String, dark: Boolean, scale: Float): InstallerUiKit {
        val target = instrumentation.targetContext
        val config = Configuration(target.resources.configuration).apply { fontScale = scale }
        val appearance = HostAppearance(language, dark, 0xffffdead.toInt(), 0xffffdead.toInt())
        val context = ContextThemeWrapper(appearance.wrap(target.createConfigurationContext(config)), R.style.Theme_ThreeSetupInstaller)
        return InstallerUiKit(context, InstallerPalette.resolve(appearance))
    }

    private fun populatedDialog(kit: InstallerUiKit, positive: String = "Install", negative: String = "Cancel"): InstallerDialogLayout =
        kit.dialog("Installation").also { dialog ->
            // At the 560 dp cap this English sentence can occupy only one line. Twenty-four rows
            // may fit inside an 800 dp window; use enough content to exercise actual overflow.
            repeat(80) { dialog.content.addView(kit.text("Package details and an explanation that can wrap onto multiple lines.", 16f)) }
            dialog.actions.addView(kit.textButton(negative) {})
            dialog.actions.addView(kit.textButton(positive) {})
        }

    private fun measure(dialog: InstallerDialogLayout, width: Int, height: Int) {
        dialog.root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        dialog.root.layout(0, 0, width, height)
    }

    private fun report(dialog: InstallerDialogLayout, scenario: String): String {
        fun bounds(view: View) = "${view.left},${view.top}..${view.right},${view.bottom} (${view.width}x${view.height})"
        val root = dialog.root
        val description = "$scenario; density=${root.resources.displayMetrics.density}; fontScale=${root.resources.configuration.fontScale}; " +
            "root=${bounds(root)}; padding=${root.paddingLeft},${root.paddingTop},${root.paddingRight},${root.paddingBottom}; " +
            "surface=${bounds(dialog.surface)}; title=${bounds(dialog.title)}; scroll=${bounds(dialog.scroll)}; " +
            "body=${bounds(dialog.content)}; actions=${bounds(dialog.actions)}"
        instrumentation.sendStatus(0, Bundle().apply { putString("dialogGeometry", description) })
        return description
    }
}
