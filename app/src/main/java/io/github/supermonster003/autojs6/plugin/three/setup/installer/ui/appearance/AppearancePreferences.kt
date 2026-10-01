package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/** A read-only host snapshot or the resolved appearance of this plugin. Colors keep their seed. */
internal data class HostAppearance(val language: String, val dark: Boolean, val primary: Int, val accent: Int)

/** Independent plugin preferences. P3 consumes them; their settings UI belongs to P5. */
internal data class AppearancePreferences(
    val language: String = FOLLOW_HOST,
    val darkMode: String = FOLLOW_HOST,
    val color: Int? = null,
) {
    fun resolve(host: HostAppearance?, systemLanguage: String, systemDark: Boolean): HostAppearance {
        val resolvedLanguage = when (language) {
            FOLLOW_HOST -> host?.language ?: systemLanguage
            FOLLOW_SYSTEM -> systemLanguage
            in LANGUAGES -> language
            else -> host?.language ?: systemLanguage
        }
        val dark = when (darkMode) {
            "light" -> false
            "dark" -> true
            FOLLOW_SYSTEM -> systemDark
            else -> host?.dark ?: systemDark
        }
        return HostAppearance(
            resolvedLanguage,
            dark,
            (color ?: host?.primary ?: DEFAULT_COLOR) or OPAQUE,
            (color ?: host?.accent ?: DEFAULT_COLOR) or OPAQUE,
        )
    }

    /** No preference is ever written to the host. Returns the actual persistence result. */
    fun save(context: Context): Boolean {
        require(language in LANGUAGES && darkMode in DARK_MODES)
        val editor = file(context).edit().putString("language", language).putString("darkMode", darkMode)
        if (color == null) editor.remove("color") else editor.putInt("color", color or OPAQUE)
        return editor.commit()
    }

    companion object {
        const val DEFAULT_COLOR: Int = -0x2153 // #FFDEAD, the host's default seed.
        const val FOLLOW_HOST = "host"
        const val FOLLOW_SYSTEM = "system"
        private const val OPAQUE = -0x1000000
        val LANGUAGES = listOf(FOLLOW_HOST, FOLLOW_SYSTEM, "zh-Hans", "zh-Hant-HK", "zh-Hant-TW", "en", "fr", "es", "ja", "ko", "ru", "ar")
        val DARK_MODES = listOf(FOLLOW_HOST, FOLLOW_SYSTEM, "light", "dark")

        fun read(context: Context): AppearancePreferences = runCatching {
            val prefs = file(context).all
            AppearancePreferences(
                (prefs["language"] as? String)?.takeIf { it in LANGUAGES } ?: FOLLOW_HOST,
                (prefs["darkMode"] as? String)?.takeIf { it in DARK_MODES } ?: FOLLOW_HOST,
                (prefs["color"] as? Int)?.or(OPAQUE),
            )
        }.getOrDefault(AppearancePreferences())

        fun resolve(context: Context, host: HostAppearance?): HostAppearance {
            // This configuration is independent of the locale/night override of any plugin Activity.
            val system = Resources.getSystem().configuration
            val language = if (system.locales.isEmpty) Locale.getDefault().toLanguageTag() else system.locales[0].toLanguageTag()
            return read(context).resolve(host, language, system.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
        }

        private fun file(context: Context) = context.applicationContext.getSharedPreferences("app-appearance", Context.MODE_PRIVATE)
    }
}
