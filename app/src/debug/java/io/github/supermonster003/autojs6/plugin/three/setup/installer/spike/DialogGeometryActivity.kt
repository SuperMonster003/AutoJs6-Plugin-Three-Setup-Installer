package io.github.supermonster003.autojs6.plugin.three.setup.installer.spike

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.text.InputType
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.button.MaterialButton
import io.github.supermonster003.autojs6.plugin.three.setup.installer.R
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.AppearancePreferences
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.HostAppearance
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.kit.InstallerDialogLayout
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.kit.InstallerPalette
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.kit.InstallerUiKit
import java.util.Locale

/** Real-window geometry fixture, compiled only into debug. It never creates an installation. */
class DialogGeometryActivity : AppCompatActivity() {
    internal data class Overrides(val language: String? = null, val fontScale: Float? = null, val dark: Boolean? = null)

    internal lateinit var kit: InstallerUiKit
        private set
    internal lateinit var dialog: InstallerDialogLayout
        private set
    internal lateinit var editor: AppCompatEditText
        private set
    internal lateinit var lastRow: TextView
        private set
    internal lateinit var positive: MaterialButton
        private set
    internal lateinit var negative: MaterialButton
        private set
    internal var positiveClicks = 0
        private set
    internal var lastShowAccepted: Boolean? = null
        private set
    private var dark = false

    override fun attachBaseContext(newBase: Context) {
        val selected = activeOverrides
        val privateConfiguration = Configuration(newBase.resources.configuration).apply {
            selected.fontScale?.let { fontScale = it }
            selected.language?.let { tag ->
                val locale = Locale.forLanguageTag(tag)
                setLocales(LocaleList(locale))
                setLayoutDirection(locale)
            }
            selected.dark?.let { night ->
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
        }
        dark = privateConfiguration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        delegate.localNightMode = if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        // Only this Activity receives the override. No global Resources or preferences are changed,
        // and no host appearance refresh can replace the explicitly selected test configuration.
        super.attachBaseContext(newBase.createConfigurationContext(privateConfiguration))
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_ThreeSetupInstaller_Dialog)
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(false)
        val decor = window.decorView
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = if (Build.VERSION.SDK_INT < 26) 0xff121212.toInt() else Color.TRANSPARENT
        WindowInsetsControllerCompat(window, decor).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        val appearance = HostAppearance(resources.configuration.locales[0].toLanguageTag(), dark,
            AppearancePreferences.DEFAULT_COLOR, AppearancePreferences.DEFAULT_COLOR)
        kit = InstallerUiKit(this, InstallerPalette.resolve(appearance))
        dialog = kit.dialog(getString(R.string.confirm_install_title))
        dialog.root.isFocusableInTouchMode = true
        editor = AppCompatEditText(this).apply {
            tag = EDITOR_TAG
            id = EDITOR_ID
            inputType = InputType.TYPE_CLASS_NUMBER
            imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
            textSize = 16f
            minHeight = kit.dp(56)
            setPaddingRelative(kit.dp(8), kit.dp(12), kit.dp(8), kit.dp(12))
            setTextColor(kit.palette.text)
            setHintTextColor(kit.palette.muted)
            setText("0")
        }
        dialog.content.addView(editor, LinearLayout.LayoutParams(-1, -2))
        repeat(96) {
            lastRow = kit.text(getString(R.string.install_commit_notice), 16f)
            dialog.content.addView(lastRow, LinearLayout.LayoutParams(-1, -2))
        }
        negative = kit.textButton(getString(R.string.action_cancel), "geometry_cancel") { finish() }
        positive = kit.textButton(getString(R.string.action_install), "geometry_confirm") { positiveClicks++ }
        dialog.actions.addView(negative)
        dialog.actions.addView(positive)
        setContentView(dialog.root)
        dialog.root.requestFocus()
    }

    internal fun showKeyboard() {
        editor.requestFocus()
        editor.setSelection(editor.text?.length ?: 0)
        dialog.scroll.scrollTo(0, 0)
        lastShowAccepted = getSystemService(InputMethodManager::class.java).showSoftInput(editor, 0)
        WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.ime())
    }

    internal fun hideKeyboard() {
        WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.ime())
        getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(editor.windowToken, 0)
        editor.clearFocus()
        dialog.root.requestFocus()
    }

    override fun onDestroy() {
        if (::editor.isInitialized && isFinishing) hideKeyboard()
        super.onDestroy()
    }

    companion object {
        // The test owns this process-local scope until its Activity is destroyed. Recreation reads
        // the same values; the finally block restores the previous object, never device settings.
        @Volatile internal var activeOverrides = Overrides()
        const val EDITOR_TAG = "geometry_editor"
        private const val EDITOR_ID = 0x00f00101
    }
}
