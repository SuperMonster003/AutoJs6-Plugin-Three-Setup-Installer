package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.kit

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import kotlin.math.min

internal data class InstallerDialogLayout(
    val root: FrameLayout,
    val title: TextView,
    val content: LinearLayout,
    val actions: LinearLayout,
    val scroll: NestedScrollView,
    val surface: LinearLayout,
)

/** The fixed regions are measured first, leaving only the middle of a tall dialog scrollable. */
private class DialogColumn(context: Context, private val maxWidth: Int) : LinearLayout(context) {
    init { orientation = VERTICAL }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = min(MeasureSpec.getSize(widthMeasureSpec), maxWidth)
        val height = (MeasureSpec.getSize(heightMeasureSpec) * 0.85f).toInt()
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST))
    }
}

/** Long translations and large fonts stack the same actions instead of clipping their labels. */
private class ResponsiveActions(context: Context) : LinearLayout(context) {
    init { gravity = Gravity.END or Gravity.CENTER_VERTICAL }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        var required = 0
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.visibility == View.GONE) continue
            child.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), heightMeasureSpec)
            val margins = child.layoutParams as? MarginLayoutParams
            required += child.measuredWidth + (margins?.leftMargin ?: 0) + (margins?.rightMargin ?: 0)
        }
        orientation = if (required > available) VERTICAL else HORIZONTAL
        for (index in 0 until childCount) {
            getChildAt(index).layoutParams.width = if (orientation == VERTICAL) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}

/** Full-window viewport: geometry uses the actual window, including split-screen, cutout and IME. */
internal fun createInstallerDialog(kit: InstallerUiKit, heading: CharSequence): InstallerDialogLayout {
    val context = kit.context
    val root = FrameLayout(context).apply {
        layoutDirection = context.resources.configuration.layoutDirection
        setPaddingRelative(kit.dp(24), 0, kit.dp(24), 0)
    }
    val surface = DialogColumn(context, kit.dp(560)).apply {
        background = kit.roundedFill(kit.palette.surface, 24)
        clipToOutline = true
    }
    val title = kit.text(heading, 20f, medium = true).apply {
        setPaddingRelative(kit.dp(24), kit.dp(24), kit.dp(24), kit.dp(12))
        ViewCompat.setAccessibilityHeading(this, true)
    }
    val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPaddingRelative(kit.dp(24), kit.dp(8), kit.dp(24), kit.dp(8))
    }
    val scroll = NestedScrollView(context).apply {
        isFillViewport = false
        addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
    val actions = ResponsiveActions(context).apply {
        setPaddingRelative(kit.dp(12), kit.dp(8), kit.dp(12), kit.dp(16))
    }
    surface.addView(title, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    surface.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
    surface.addView(actions, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    root.addView(surface, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
        // Insets are physical edges; the symmetric design margin has no directional semantics.
        view.setPadding(safe.left + kit.dp(24), safe.top, safe.right + kit.dp(24), safe.bottom)
        insets
    }
    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) {
            view.removeOnAttachStateChangeListener(this)
            ViewCompat.requestApplyInsets(view)
        }
        override fun onViewDetachedFromWindow(view: View) = Unit
    })
    return InstallerDialogLayout(root, title, content, actions, scroll, surface)
}
