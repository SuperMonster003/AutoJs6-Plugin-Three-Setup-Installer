package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.ReleaseHistoryAssetPolicy
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.ReleaseHistoryEntry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.loadReleaseHistory
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.HostAppearanceActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.kit.ThemeAccentRoles

/**
 * Renders the bundled changelog for the current locale (English when that one is missing) as one group per released
 * version, under the title "Release history" with the plugin name as the subtitle
 * (AUTOJS6_PLUGIN_RELEASE_HISTORY_AGENTS.md); reached from the settings page, the About page and the update dialog.
 * Not exported and takes no data. The language, night mode and colors are those of the settings page
 * ([HostAppearanceActivity]); a change of the host appearance redraws the page with the new colors.
 */
class ReleaseHistoryActivity : HostAppearanceActivity() {
    override val dialogTheme = false
    private lateinit var entries: List<ReleaseHistoryEntry>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        entries = load()
        render()
    }

    override fun onAppearanceChanged() {
        if (!::entries.isInitialized) return
        // The language may have changed with the appearance; the changelog follows it.
        entries = load()
        render()
    }

    private fun load(): List<ReleaseHistoryEntry> {
        val candidates = ReleaseHistoryAssetPolicy.candidatesFor(resources.configuration.locales[0])
        return loadReleaseHistory(candidates) { path ->
            assets.open(path).use { stream -> stream.readBytes().toString(Charsets.UTF_8) }
        }
    }

    private fun render() {
        val palette = kit.palette
        val colors = ReleaseHistoryColors(
            dark = palette.isDark,
            primary = palette.accent,
            primaryContainer = ThemeAccentRoles.fromSeed(appearance.accent, palette.isDark).container,
            // The settings page draws its top bar on the page background; this page does the same.
            background = palette.background,
            surface = palette.background,
            onSurface = palette.text,
            onSurfaceVariant = palette.muted,
            outlineVariant = palette.divider,
        )
        setContent {
            ReleaseHistoryTheme(colors) {
                ReleaseHistoryScreen(entries = entries, onBack = ::finish)
            }
        }
    }
}
