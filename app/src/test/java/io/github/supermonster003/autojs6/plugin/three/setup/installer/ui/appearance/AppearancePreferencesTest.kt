package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance

import org.junit.Assert.*
import org.junit.Test

class AppearancePreferencesTest {
    private val host = HostAppearance("ar", true, 0xff445566.toInt(), 0xff778899.toInt())

    @Test fun defaultsFollowHostAndFallBackWithoutRetainingAnUnavailableHost() {
        val preferences = AppearancePreferences()
        assertEquals(host, preferences.resolve(host, "en", false))
        val fallback = preferences.resolve(null, "ja", false)
        assertEquals("ja", fallback.language)
        assertFalse(fallback.dark)
        assertEquals(0xffffdead.toInt(), fallback.primary)
        assertEquals(fallback.primary, fallback.accent)
    }

    @Test fun eachPreferenceOverridesOnlyItsOwnHostField() {
        val language = AppearancePreferences(language = "en").resolve(host, "fr", false)
        assertEquals("en", language.language)
        assertTrue(language.dark)
        assertEquals(host.primary, language.primary)
        assertEquals(host.accent, language.accent)
        val night = AppearancePreferences(darkMode = "system").resolve(host, "fr", false)
        assertEquals("ar", night.language)
        assertFalse(night.dark)
        val seed = AppearancePreferences(color = 0x00112233).resolve(host, "fr", false)
        assertEquals("ar", seed.language)
        assertTrue(seed.dark)
        assertEquals(0xff112233.toInt(), seed.primary)
        assertEquals(seed.primary, seed.accent)
    }

    @Test fun localChoicesSurviveHostDisappearanceAndSystemChanges() {
        val preferences = AppearancePreferences("zh-Hant-HK", "light", 0xfff44336.toInt())
        assertEquals(preferences.resolve(host, "en", true), preferences.resolve(null, "ko", false))
        val system = AppearancePreferences("system", "system").resolve(host, "ru", false)
        assertEquals("ru", system.language)
        assertFalse(system.dark)
    }

    @Test fun unrecognizedPreferenceValuesUseTheDefaultSource() {
        assertEquals(host, AppearancePreferences("unsupported", "old-night-value").resolve(host, "en", false))
        val fallback = AppearancePreferences("unsupported", "old-night-value").resolve(null, "en", false)
        assertEquals("en", fallback.language)
        assertFalse(fallback.dark)
    }
}
