package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.supermonster003.autojs6.plugin.three.setup.installer.R
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.kit.InstallerPalette
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.kit.InstallerUiKit
import java.lang.ref.WeakReference

/**
 * Refreshes only presentation. Subclasses retain their session/draft and redraw in [onAppearanceChanged].
 * Manifest consumers handle uiMode|locale|layoutDirection, so setting AppCompat's local night mode
 * never recreates an installation task. Ordinary rotation still follows Activity saved-state handling.
 */
abstract class HostAppearanceActivity : AppCompatActivity() {
    protected open val dialogTheme = true
    internal lateinit var appearance: HostAppearance
        private set
    internal lateinit var kit: InstallerUiKit
        private set
    private var refreshGeneration = 0
    private var changingAppearance = false

    override fun attachBaseContext(newBase: Context) {
        appearance = AppearancePreferences.resolve(newBase, HostAppearanceReader.cached())
        delegate.localNightMode = nightMode(appearance)
        super.attachBaseContext(appearance.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(appearanceTheme())
        super.onCreate(savedInstanceState)
        kit = InstallerUiKit(this, InstallerPalette.resolve(appearance))
        applyWindowAppearance()
    }

    override fun onResume() {
        super.onResume()
        applyAppearance(AppearancePreferences.resolve(this, HostAppearanceReader.cached()))
        val expected = ++refreshGeneration
        val owner = WeakReference(this)
        val application = applicationContext
        HostAppearanceReader.worker.execute {
            val snapshot = HostAppearanceReader.read(application)
            main.post {
                owner.get()?.takeIf { !it.isFinishing && !it.isDestroyed && it.refreshGeneration == expected }?.let { activity ->
                    HostAppearanceReader.accept(snapshot)
                    activity.applyAppearance(AppearancePreferences.resolve(application, snapshot))
                }
            }
        }
    }

    override fun onPause() {
        refreshGeneration++
        super.onPause()
    }

    override fun onDestroy() {
        refreshGeneration++
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!changingAppearance) {
            applyAppearance(AppearancePreferences.resolve(this, HostAppearanceReader.cached()), configurationChanged = true)
        }
    }

    /** Rebuild visible widgets from the existing session/draft. Never starts or resumes a task. */
    protected open fun onAppearanceChanged() = Unit

    @Suppress("DEPRECATION")
    private fun applyAppearance(next: HostAppearance, configurationChanged: Boolean = false) {
        if (!configurationChanged && appearance == next) return
        changingAppearance = true
        try {
            appearance = next
            // attachBaseContext created a private configuration context; application/system resources
            // remain untouched. Dialog strings and layout direction see the same override as AppCompat.
            resources.updateConfiguration(next.configure(resources.configuration), resources.displayMetrics)
            delegate.localNightMode = nightMode(next)
            theme.applyStyle(appearanceTheme(), true)
            kit = InstallerUiKit(this, InstallerPalette.resolve(next))
            applyWindowAppearance()
        } finally {
            changingAppearance = false
        }
        onAppearanceChanged()
    }

    private fun appearanceTheme() = if (dialogTheme) R.style.Theme_ThreeSetupInstaller_Dialog else R.style.Theme_ThreeSetupInstaller

    @Suppress("DEPRECATION")
    private fun applyWindowAppearance() {
        val decor = window.decorView
        val colors = kit.palette
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawable(ColorDrawable(if (dialogTheme) Color.TRANSPARENT else colors.background))
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = if (dialogTheme) Color.TRANSPARENT else colors.background
        window.navigationBarColor = when {
            Build.VERSION.SDK_INT < 26 -> 0xff121212.toInt()
            dialogTheme -> Color.TRANSPARENT
            else -> colors.background
        }
        WindowInsetsControllerCompat(window, decor).apply {
            isAppearanceLightStatusBars = !dialogTheme && !appearance.dark
            isAppearanceLightNavigationBars = !dialogTheme && !appearance.dark && Build.VERSION.SDK_INT >= 26
        }
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun nightMode(value: HostAppearance) = if (value.dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO

    private companion object {
        val main = Handler(Looper.getMainLooper())
    }
}
