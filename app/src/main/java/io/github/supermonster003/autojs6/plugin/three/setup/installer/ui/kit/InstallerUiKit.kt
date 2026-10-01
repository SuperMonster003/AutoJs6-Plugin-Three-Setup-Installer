package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.kit

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator

/** Factories tint new controls as they are created, including rows added after scrolling. */
internal class InstallerUiKit(val context: Context, val palette: InstallerPalette) {
    fun dp(value: Int) = (value * context.resources.displayMetrics.density + 0.5f).toInt()
    fun string(resource: Int, vararg arguments: Any) = context.getString(resource, *arguments)

    fun roundedFill(color: Int, radiusDp: Int = 24): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    fun text(value: CharSequence?, size: Float = 14f, color: Int = palette.text, medium: Boolean = false): TextView = TextView(context).apply {
        text = value
        textSize = size
        setTextColor(color)
        setLinkTextColor(palette.accent)
        highlightColor = InstallerColorPolicy.withAlpha(palette.accent, 0x55)
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        gravity = Gravity.START
        if (medium) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setLineSpacing(0f, 1.12f)
    }

    fun textButton(label: CharSequence, tag: String? = null, danger: Boolean = false, onClick: () -> Unit): MaterialButton =
        MaterialButton(context, null, androidx.appcompat.R.attr.borderlessButtonStyle).apply {
            text = label
            this.tag = tag
            isAllCaps = false
            textSize = 14f
            setSingleLine(false)
            minHeight = dp(48)
            minimumHeight = dp(48)
            minWidth = dp(48)
            minimumWidth = dp(48)
            insetTop = 0
            insetBottom = 0
            setPaddingRelative(dp(12), dp(8), dp(12), dp(8))
            backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            setTextColor(states(palette.disabledText, if (danger) palette.danger else palette.accent))
            iconTint = states(palette.disabledText, if (danger) palette.danger else palette.accent)
            rippleColor = ColorStateList.valueOf(palette.ripple)
            setOnClickListener { onClick() }
        }

    fun checkBox(label: CharSequence, checked: Boolean, tag: String? = null, onChanged: (Boolean) -> Unit = {}): MaterialCheckBox =
        MaterialCheckBox(context).apply {
            text = label
            this.tag = tag
            textSize = 16f
            setTextColor(states(palette.disabledText, palette.text))
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            buttonTintList = choiceTint()
            buttonIconTintList = states(palette.disabledText, palette.onAccent)
            minHeight = dp(56)
            setPaddingRelative(dp(4), dp(8), dp(4), dp(8))
            background = RippleDrawable(ColorStateList.valueOf(palette.ripple), null, roundedFill(Color.WHITE, 8))
            isChecked = checked
            setOnCheckedChangeListener { _, value -> onChanged(value) }
        }

    fun switch(label: CharSequence, checked: Boolean, tag: String? = null, onChanged: (Boolean) -> Unit = {}): MaterialSwitch =
        MaterialSwitch(context).apply {
            text = label
            this.tag = tag
            textSize = 16f
            setTextColor(states(palette.disabledText, palette.text))
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            minHeight = dp(56)
            setPaddingRelative(0, dp(8), 0, dp(8))
            val choices = arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf(android.R.attr.state_checked), intArrayOf())
            thumbTintList = ColorStateList(choices, intArrayOf(palette.disabledText, palette.onPrimary, palette.muted))
            trackTintList = ColorStateList(choices, intArrayOf(palette.disabledFill, palette.primary, palette.surfaceVariant))
            thumbIconTintList = ColorStateList(choices, intArrayOf(palette.disabledFill, palette.primary, palette.surfaceVariant))
            trackDecorationTintList = ColorStateList(choices, intArrayOf(palette.disabledText, palette.primary, palette.outline))
            isChecked = checked
            setOnCheckedChangeListener { _, value -> onChanged(value) }
        }

    fun progressBar(): LinearProgressIndicator = LinearProgressIndicator(context).apply {
        setIndicatorColor(palette.accent)
        trackColor = InstallerColorPolicy.blend(palette.surface, palette.accent, 0.16)
        trackThickness = dp(4)
        trackCornerRadius = dp(2)
    }

    fun choiceTint(): ColorStateList = ColorStateList(
        arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf(android.R.attr.state_checked), intArrayOf()),
        intArrayOf(palette.disabledText, palette.accent, palette.muted),
    )

    private fun states(disabled: Int, enabled: Int) = ColorStateList(
        arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()), intArrayOf(disabled, enabled),
    )

    fun dialog(title: CharSequence): InstallerDialogLayout = createInstallerDialog(this, title)
}
