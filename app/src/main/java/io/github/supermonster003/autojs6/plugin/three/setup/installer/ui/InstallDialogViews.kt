package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.LeadingMarginSpan
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import io.github.supermonster003.autojs6.plugin.three.setup.installer.R
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.kit.InstallerColorPolicy
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.kit.InstallerUiKit

/** What an approved installation does to the target user's copy of the package. */
internal enum class InstallKind {
    NEW, UPDATE, REINSTALL, DOWNGRADE;

    companion object {
        /** Null when the installed state of the target user or the archive version is not known. */
        fun of(installedKnown: Boolean, installedCode: Long?, archiveCode: Long?): InstallKind? = when {
            !installedKnown -> null
            installedCode == null -> NEW
            archiveCode == null -> null
            archiveCode > installedCode -> UPDATE
            archiveCode < installedCode -> DOWNGRADE
            else -> REINSTALL
        }
    }
}

/**
 * Building blocks of the installation dialog. Package facts read as one aligned block, and each
 * choice is a single row naming its current value, so the rarely changed ones stay folded.
 */
internal class InstallDialogViews(private val kit: InstallerUiKit) {
    private val context get() = kit.context
    private val palette get() = kit.palette

    data class Fact(val label: CharSequence, val value: CharSequence, val danger: Boolean = false, val tag: String? = null)

    enum class Trailing(val icon: Int, val rotation: Float) {
        /** Opens a selector above the dialog. */
        OPEN(R.drawable.ic_settings_chevron, 0f),
        /** Unfolds a section in place. */
        EXPAND(R.drawable.ic_dialog_expand, 0f),
        COLLAPSE(R.drawable.ic_dialog_expand, 180f),
    }

    fun header(icon: Bitmap?, label: CharSequence, packageName: CharSequence?, badge: View? = null): LinearLayout = LinearLayout(context).apply {
        gravity = Gravity.CENTER_VERTICAL
        icon?.let {
            addView(ImageView(context).apply {
                setImageBitmap(it)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(kit.dp(48), kit.dp(48)).apply { marginEnd = kit.dp(16) })
        }
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            // The name shrinks before its badge does: a wrapped parent hands the overflow to the weighted child.
            addView(LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(kit.text(label, 16f, medium = true).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END },
                    LinearLayout.LayoutParams(WRAP, WRAP, 1f))
                badge?.let { addView(it, LinearLayout.LayoutParams(WRAP, WRAP).apply { marginStart = kit.dp(8) }) }
            }, LinearLayout.LayoutParams(WRAP, WRAP))
            packageName?.let {
                addView(kit.text(it, 14f, palette.muted).apply { setTextIsSelectable(true) },
                    LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = kit.dp(2) })
            }
        }, LinearLayout.LayoutParams(0, WRAP, 1f))
    }

    fun badge(label: CharSequence, color: Int): TextView = kit.text(label, 12f, color, medium = true).apply {
        maxLines = 1
        setLineSpacing(0f, 1f)
        // The same tint ratio the palette already verified its accent against.
        background = kit.roundedFill(InstallerColorPolicy.blend(palette.surface, color, 0x1c / 255.0), 8)
        setPaddingRelative(kit.dp(8), kit.dp(2), kit.dp(8), kit.dp(2))
    }

    /** Labels share one column, so every value starts at the same edge in any language. */
    fun facts(rows: List<Fact>): TableLayout = TableLayout(context).apply {
        setColumnShrinkable(1, true)
        setColumnStretchable(1, true)
        background = kit.roundedFill(palette.surfaceVariant, 12)
        setPaddingRelative(kit.dp(16), kit.dp(12), kit.dp(16), kit.dp(12))
        rows.forEachIndexed { index, fact ->
            addView(TableRow(context).apply {
                // TableLayout sizes its columns by measuring cells before their row has resolved its direction;
                // an inherited direction would then fix this gap on the wrong side in right-to-left languages.
                addView(kit.text(fact.label, 14f, palette.muted).apply {
                    layoutDirection = context.resources.configuration.layoutDirection
                    setPaddingRelative(0, 0, kit.dp(16), 0)
                })
                addView(kit.text(fact.value, 14f, if (fact.danger) palette.danger else palette.text).apply { tag = fact.tag })
            }, TableLayout.LayoutParams(MATCH, WRAP).apply { if (index > 0) topMargin = kit.dp(8) })
        }
    }

    /** The ripple reaches 12 dp past the text on both sides while the text keeps the dialog's 24 dp edge. */
    fun row(title: CharSequence, summary: CharSequence?, tag: String, trailing: Trailing, enabled: Boolean = true, click: () -> Unit): LinearLayout =
        LinearLayout(context).apply {
            this.tag = tag
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = kit.dp(if (summary.isNullOrEmpty()) 56 else 72)
            setPaddingRelative(kit.dp(12), kit.dp(10), kit.dp(12), kit.dp(10))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(kit.text(title, 16f, if (enabled) palette.text else palette.disabledText), LinearLayout.LayoutParams(MATCH, WRAP))
                if (!summary.isNullOrEmpty()) {
                    addView(kit.text(summary, 14f, if (enabled) palette.muted else palette.disabledText).apply {
                        maxLines = 3; ellipsize = TextUtils.TruncateAt.END
                    }, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = kit.dp(4) })
                }
            }, LinearLayout.LayoutParams(0, WRAP, 1f))
            addView(ImageView(context).apply {
                setImageResource(trailing.icon)
                rotation = trailing.rotation
                imageTintList = ColorStateList.valueOf(if (enabled) palette.muted else palette.disabledText)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(kit.dp(24), kit.dp(24)).apply { marginStart = kit.dp(16) })
            isEnabled = enabled
            if (enabled) {
                isClickable = true
                isFocusable = true
                background = RippleDrawable(ColorStateList.valueOf(palette.ripple), null, kit.roundedFill(Color.WHITE, 12))
                setOnClickListener { click() }
                // Screen readers announce a folded section through its expand or collapse action.
                when (trailing) {
                    Trailing.EXPAND -> ViewCompat.replaceAccessibilityAction(this, AccessibilityActionCompat.ACTION_EXPAND, null) { _, _ -> click(); true }
                    Trailing.COLLAPSE -> ViewCompat.replaceAccessibilityAction(this, AccessibilityActionCompat.ACTION_COLLAPSE, null) { _, _ -> click(); true }
                    Trailing.OPEN -> Unit
                }
            }
            ViewCompat.setScreenReaderFocusable(this, true)
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { marginStart = -kit.dp(12); marginEnd = -kit.dp(12) }
        }

    /** A paragraph that keeps its distance from whatever precedes it. */
    fun note(value: CharSequence, color: Int = palette.muted, tag: String? = null, top: Int = 12): TextView = kit.text(value, 14f, color).apply {
        this.tag = tag
        layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = kit.dp(top) }
    }

    fun subheading(value: CharSequence): TextView = kit.text(value, 14f, palette.muted, medium = true).apply {
        ViewCompat.setAccessibilityHeading(this, true)
        layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = kit.dp(16); bottomMargin = kit.dp(4) }
    }

    /** One entry per line; a wrapped entry continues indented, so long identifiers stay tellable apart. */
    fun lines(entries: List<String>, tag: String? = null): TextView {
        val text = SpannableStringBuilder()
        entries.forEachIndexed { index, entry ->
            val start = text.length
            text.append(entry)
            text.setSpan(LeadingMarginSpan.Standard(0, kit.dp(12)), start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (index < entries.lastIndex) text.append("\n")
        }
        return kit.text(text, 14f, palette.text).apply {
            this.tag = tag
            setTextIsSelectable(true)
            background = kit.roundedFill(palette.surfaceVariant, 12)
            setPaddingRelative(kit.dp(16), kit.dp(12), kit.dp(16), kit.dp(12))
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = kit.dp(8); bottomMargin = kit.dp(8) }
        }
    }

    fun spaced(view: View, top: Int = 0, bottom: Int = 0): View = view.apply {
        layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = kit.dp(top); bottomMargin = kit.dp(bottom) }
    }

    private companion object {
        const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
        const val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT
    }
}
