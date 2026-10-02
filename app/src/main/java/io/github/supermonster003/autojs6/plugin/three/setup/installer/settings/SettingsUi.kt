package io.github.supermonster003.autojs6.plugin.three.setup.installer.settings

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.github.supermonster003.autojs6.plugin.three.setup.installer.R
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.HostAppearanceActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.kit.InstallerColorPolicy
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.kit.InstallerDialogLayout
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.kit.InstallerUiKit

/** Full page appearance, insets and dialog lifetime shared by standalone screens. */
abstract class SettingsPageActivity : HostAppearanceActivity() {
    final override val dialogTheme = false
    internal lateinit var settingsUi: SettingsUi
        private set
    internal var prompt: Dialog? = null
    protected abstract val pageTitle: Int
    protected abstract fun buildPage(content: LinearLayout)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        renderPage()
    }

    protected fun renderPage() {
        if (isFinishing || isDestroyed) return
        settingsUi = SettingsUi(this, kit)
        val page = settingsUi.page(getString(pageTitle)) { finish() }
        buildPage(page.content)
        setContentView(page.root)
    }

    override fun onAppearanceChanged() {
        prompt?.dismiss()
        prompt = null
        onPageAppearanceChanged()
        renderPage()
    }

    protected open fun onPageAppearanceChanged() = Unit

    override fun onStop() {
        prompt?.dismiss()
        prompt = null
        super.onStop()
    }

    internal fun notify(resource: Int) = Toast.makeText(this, resource, Toast.LENGTH_LONG).show()
}

internal data class SettingsPage(val root: LinearLayout, val content: LinearLayout, val toolbar: LinearLayout)
internal data class SettingsChoice(val label: CharSequence, val note: CharSequence? = null)

/** Standalone rows and confirmed selectors, reusing the installation palette without changing P3. */
internal class SettingsUi(private val owner: HostAppearanceActivity, val kit: InstallerUiKit) {
    private val context get() = kit.context
    private val palette get() = kit.palette
    fun dp(value: Int) = kit.dp(value)

    fun page(title: CharSequence, back: (() -> Unit)? = null): SettingsPage {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = context.resources.configuration.layoutDirection
            setBackgroundColor(palette.background)
        }
        val toolbar = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL; minimumHeight = dp(64) }
        val button = ImageView(context).apply {
            setImageResource(R.drawable.ic_back)
            imageTintList = tint(palette.text)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            contentDescription = context.getString(R.string.settings_back)
            isClickable = true; isFocusable = true
            background = RippleDrawable(ColorStateList.valueOf(palette.ripple), null, kit.roundedFill(Color.WHITE, 24))
            setOnClickListener { back?.invoke() }
        }
        if (back != null) toolbar.addView(button, LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginStart = dp(12); marginEnd = dp(8) })
        toolbar.addView(kit.text(title, 20f, medium = true).apply { ViewCompat.setAccessibilityHeading(this, true) },
            LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(24); if (back == null) marginStart = dp(24) })
        val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPaddingRelative(0, 0, 0, dp(24)) }
        val scroll = NestedScrollView(context).apply { isFillViewport = true; addView(content, ViewGroup.LayoutParams(-1, -2)) }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) { ViewCompat.requestApplyInsets(view) }
            override fun onViewDetachedFromWindow(view: View) = Unit
        })
        return SettingsPage(root, content, toolbar)
    }

    fun group(title: Int): TextView = kit.text(context.getString(title), 14f, palette.muted, medium = true).apply {
        setPaddingRelative(dp(24), dp(24), dp(24), dp(8))
        ViewCompat.setAccessibilityHeading(this, true)
    }

    fun caption(value: CharSequence): TextView = kit.text(value, 14f, palette.muted).apply {
        setPaddingRelative(dp(24), dp(12), dp(24), dp(12))
    }

    fun divider() = View(context).apply {
        setBackgroundColor(palette.divider)
        layoutParams = LinearLayout.LayoutParams(-1, dp(1)).apply { marginStart = dp(64); marginEnd = dp(24) }
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun row(title: Int, summary: CharSequence?, icon: Int = R.drawable.ic_settings_tune,
        tag: String? = null, click: (() -> Unit)? = null): LinearLayout = row(context.getString(title), summary, icon, tag, click)

    fun row(title: CharSequence, summary: CharSequence?, icon: Int = R.drawable.ic_settings_tune,
        tag: String? = null, click: (() -> Unit)? = null): LinearLayout = LinearLayout(context).apply {
        this.tag = tag
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(72)
        setPaddingRelative(dp(24), dp(12), dp(24), dp(12))
        addView(ImageView(context).apply {
            setImageResource(icon); imageTintList = tint(palette.muted)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(24), dp(24)).apply { marginEnd = dp(16) })
        val text = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(kit.text(title, 16f), LinearLayout.LayoutParams(-1, -2))
            if (!summary.isNullOrEmpty()) addView(kit.text(summary, 14f, palette.muted),
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
        }
        addView(text, LinearLayout.LayoutParams(0, -2, 1f))
        if (click != null) {
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_settings_chevron); imageTintList = tint(palette.muted)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(24), dp(24)).apply { marginStart = dp(16) })
            isClickable = true; isFocusable = true
            background = RippleDrawable(ColorStateList.valueOf(palette.ripple), null, ColorDrawable(Color.WHITE))
            setOnClickListener { click() }
        }
        ViewCompat.setScreenReaderFocusable(this, true)
    }

    @Suppress("DEPRECATION")
    fun dialog(title: CharSequence, create: (InstallerDialogLayout, Dialog) -> Unit): Dialog {
        val dialog = Dialog(context, R.style.Theme_ThreeSetupInstaller_Dialog)
        dialog.setOwnerActivity(owner)
        val layout = kit.dialog(title)
        layout.root.setOnClickListener { dialog.cancel() }
        layout.root.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        layout.surface.isClickable = true
        layout.surface.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        create(layout, dialog)
        dialog.setContentView(layout.root)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            WindowCompat.setDecorFitsSystemWindows(this, false)
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = if (android.os.Build.VERSION.SDK_INT < 26) 0xff121212.toInt() else Color.TRANSPARENT
            WindowInsetsControllerCompat(this, decorView).apply {
                isAppearanceLightStatusBars = !palette.isDark
                isAppearanceLightNavigationBars = !palette.isDark && android.os.Build.VERSION.SDK_INT >= 26
            }
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            setLayout(-1, -1)
        }
        return dialog
    }

    fun confirmedChoice(title: Int, choices: List<SettingsChoice>, current: Int, confirm: (Int) -> Boolean): Dialog =
        dialog(context.getString(title)) { layout, dialog ->
            var draft = current.coerceIn(0, choices.lastIndex)
            val buttons = mutableListOf<MaterialRadioButton>()
            choices.forEachIndexed { index, choice ->
                val radio = MaterialRadioButton(context).apply {
                    tag = "choice-$index"
                    text = choice.note?.let { note -> android.text.SpannableString("${choice.label}\n$note").apply {
                        setSpan(android.text.style.RelativeSizeSpan(14f / 16f), choice.label.length + 1, length, 0)
                        setSpan(android.text.style.ForegroundColorSpan(palette.muted), choice.label.length + 1, length, 0)
                    } } ?: choice.label
                    textSize = 16f; setTextColor(tint(palette.text)); buttonTintList = kit.choiceTint()
                    background = RippleDrawable(ColorStateList.valueOf(palette.ripple), null, kit.roundedFill(Color.WHITE, 8))
                    minimumHeight = dp(if (choice.note == null) 56 else 72)
                    setPaddingRelative(0, dp(8), 0, dp(8))
                    isChecked = index == draft
                    setOnClickListener { draft = index; buttons.forEachIndexed { i, item -> item.isChecked = i == draft } }
                }
                buttons += radio
                layout.content.addView(radio, LinearLayout.LayoutParams(-1, -2))
            }
            layout.actions.addView(kit.textButton(context.getString(R.string.action_cancel), "settings-cancel") { dialog.cancel() })
            layout.actions.addView(kit.textButton(context.getString(R.string.settings_confirm), "settings-confirm") {
                if (confirm(draft)) dialog.dismiss()
            })
            layout.scroll.post { layout.scroll.scrollTo(0, 0) }
        }

    fun input(title: Int, value: String, note: String, validate: (String) -> String?,
        multiline: Boolean = false, maxLength: Int = 255, confirm: (String) -> Boolean): Dialog =
        dialog(context.getString(title)) { layout, dialog ->
            layout.content.addView(kit.text(note, 14f, palette.muted))
            val field = TextInputLayout(context).apply {
                boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                defaultHintTextColor = ColorStateList.valueOf(palette.muted)
                hintTextColor = ColorStateList.valueOf(palette.accent)
                hint = context.getString(title)
                boxBackgroundColor = palette.surface
                setBoxStrokeColorStateList(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf(android.R.attr.state_focused), intArrayOf()),
                    intArrayOf(palette.disabledText, palette.accent, palette.outline)))
                setErrorTextColor(ColorStateList.valueOf(palette.danger))
                setBoxStrokeErrorColor(ColorStateList.valueOf(palette.danger))
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    cursorColor = ColorStateList.valueOf(palette.accent)
                    cursorErrorColor = ColorStateList.valueOf(palette.danger)
                }
            }
            val tintContext = LegacyInputTintContext(field.context, palette.accent)
            val input = TextInputEditText(tintContext).apply {
                tag = "settings-input"
                setText(value); setTextColor(palette.text); setHintTextColor(palette.muted)
                highlightColor = InstallerColorPolicy.withAlpha(palette.accent, 0x55)
                setSingleLine(!multiline)
                if (multiline) {
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    minLines = 3
                    maxLines = 9
                }
                filters = arrayOf(android.text.InputFilter.LengthFilter(maxLength))
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    textCursorDrawable = textCursorDrawable?.mutate()?.apply { setTint(palette.accent) }
                    textSelectHandle?.let { setTextSelectHandle(it.mutate().apply { setTint(palette.accent) }) }
                    textSelectHandleLeft?.let { setTextSelectHandleLeft(it.mutate().apply { setTint(palette.accent) }) }
                    textSelectHandleRight?.let { setTextSelectHandleRight(it.mutate().apply { setTint(palette.accent) }) }
                }
            }
            tintContext.register(input)
            field.addView(input, LinearLayout.LayoutParams(-1, -2))
            input.backgroundTintList = null
            layout.content.addView(field, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
            layout.actions.addView(kit.textButton(context.getString(R.string.action_cancel), "settings-cancel") { dialog.cancel() })
            val positive = kit.textButton(context.getString(R.string.settings_confirm), "settings-confirm") {
                if (validate(input.text.toString()) == null && confirm(input.text.toString())) dialog.dismiss()
            }
            layout.actions.addView(positive)
            fun refresh() { val error = validate(input.text.toString()); field.error = error; positive.isEnabled = error == null }
            input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) = refresh()
            })
            refresh()
        }

    fun message(title: Int, message: CharSequence): Dialog = dialog(context.getString(title)) { layout, dialog ->
        layout.content.addView(kit.text(message, 14f, palette.muted))
        layout.actions.addView(kit.textButton(context.getString(R.string.settings_confirm), "settings-confirm") { dialog.dismiss() })
    }

    private fun tint(color: Int) = ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()), intArrayOf(palette.disabledText, color))
}
