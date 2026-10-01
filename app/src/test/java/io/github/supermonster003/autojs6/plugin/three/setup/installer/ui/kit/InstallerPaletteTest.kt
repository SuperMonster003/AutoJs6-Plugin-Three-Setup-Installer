package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.kit

import android.annotation.SuppressLint
import com.google.android.material.color.utilities.Hct
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.HostAppearance
import org.junit.Assert.*
import org.junit.Test

@SuppressLint("RestrictedApi")
class InstallerPaletteTest {
    private val seeds = listOf(0xffffdead, 0xff000000, 0xffffffff, 0xff808080, 0xffff0000, 0xff00ff00, 0xff0000ff, 0xff12abcd, 0xffe91e63).map(Long::toInt)

    @Test fun hctRolesUseTheSharedTonesForLightAndDarkIncludingAchromaticSeeds() {
        for (seed in seeds) {
            val light = ThemeAccentRoles.fromSeed(seed, false)
            val dark = ThemeAccentRoles.fromSeed(seed, true)
            assertEquals(40.0, Hct.fromInt(light.primary).tone, 0.6)
            assertEquals(100.0, Hct.fromInt(light.onPrimary).tone, 0.6)
            assertEquals(80.0, Hct.fromInt(dark.primary).tone, 0.6)
            assertEquals(20.0, Hct.fromInt(dark.onPrimary).tone, 0.6)
            assertTrue(InstallerColorPolicy.contrastRatio(light.primary, light.onPrimary) >= 4.5)
            assertTrue(InstallerColorPolicy.contrastRatio(dark.primary, dark.onPrimary) >= 4.5)
        }
        assertTrue(Hct.fromInt(ThemeAccentRoles.fromSeed(0xff808080.toInt(), false).primary).chroma < 4.0)
    }

    @Test fun everyAccentIsReadableOnAllNeutralSurfacesAndDoesNotRecolorThem() {
        for (dark in listOf(false, true)) {
            val reference = palette(seeds.first(), dark)
            for (seed in seeds) {
                val value = palette(seed, dark)
                assertEquals(reference.background, value.background)
                assertEquals(reference.surface, value.surface)
                assertEquals(reference.outline, value.outline)
                assertEquals(reference.divider, value.divider)
                assertEquals(reference.danger, value.danger)
                for (surface in listOf(value.background, value.surface, value.surfaceVariant)) {
                    assertTrue("accent ${seed.toUInt().toString(16)}, dark=$dark", InstallerColorPolicy.contrastRatio(value.accent, surface) >= 4.5)
                    assertTrue(InstallerColorPolicy.contrastRatio(value.text, surface) >= 4.5)
                    assertTrue(InstallerColorPolicy.contrastRatio(value.muted, surface) >= 4.5)
                }
                assertNotEquals(value.disabledFill, value.disabledText)
            }
        }
    }

    @Test fun hostPrimaryAndAccentAreIndependentAndSeedsRemainUnmodified() {
        val host = HostAppearance("en", false, 0xffffdead.toInt(), 0xff0000ff.toInt())
        val value = InstallerPalette.resolve(host)
        val blue = palette(host.accent, false)
        assertEquals(ThemeAccentRoles.fromSeed(host.primary, false).primary, value.primary)
        assertEquals(blue.accent, value.accent)
        assertNotEquals(value.primary, value.accent)
        assertEquals(0xffffdead.toInt(), host.primary)
        assertEquals(0xff0000ff.toInt(), host.accent)
    }

    private fun palette(seed: Int, dark: Boolean) = InstallerPalette.resolve(HostAppearance("en", dark, seed, seed))
}
