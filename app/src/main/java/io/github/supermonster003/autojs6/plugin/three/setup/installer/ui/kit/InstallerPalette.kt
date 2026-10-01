package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.kit

import android.annotation.SuppressLint
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.TonalPalette
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.HostAppearance
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** The same fixed Material Color Utilities rule as the other standalone Three plugins. */
internal object ThemeAccentRoles {
    data class Roles(val primary: Int, val onPrimary: Int)

    @SuppressLint("RestrictedApi") // Material 1.13.0 supplies the pinned utilities used by the shared policy.
    fun fromSeed(seed: Int, dark: Boolean): Roles {
        val source = Hct.fromInt(seed or -0x1000000)
        val chroma = if (source.chroma < 4.0) 0.0 else max(source.chroma, 48.0).coerceAtMost(96.0)
        val tones = TonalPalette.fromHueAndChroma(source.hue, chroma)
        return Roles(tones.tone(if (dark) 80 else 40), tones.tone(if (dark) 20 else 100))
    }
}

/** Pure WCAG color math, matching the shared standalone palette's multi-surface fallback. */
internal object InstallerColorPolicy {
    fun luminance(color: Int): Double {
        fun channel(shift: Int): Double {
            val value = (color shr shift and 0xff) / 255.0
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    fun contrastRatio(first: Int, second: Int): Double =
        (max(luminance(first), luminance(second)) + 0.05) / (min(luminance(first), luminance(second)) + 0.05)

    fun onFilledColor(background: Int): Int =
        if (contrastRatio(-0x1000000, background) >= contrastRatio(-1, background)) -0x1000000 else -1

    fun readableAccent(color: Int, background: Int): Int {
        val opaque = color or -0x1000000
        if (contrastRatio(opaque, background) >= 4.5) return opaque
        fun toward(target: Int): Int? {
            if (contrastRatio(target, background) < 4.5) return null
            var low = 0.0
            var high = 1.0
            repeat(18) {
                val middle = (low + high) / 2.0
                if (contrastRatio(blend(opaque, target, middle), background) >= 4.5) high = middle else low = middle
            }
            return blend(opaque, target, high)
        }
        val source = luminance(opaque)
        return listOfNotNull(toward(-0x1000000), toward(-1)).minByOrNull { abs(luminance(it) - source) }
            ?: onFilledColor(background)
    }

    fun withAlpha(color: Int, alpha: Int) = (color and 0xffffff) or (alpha.coerceIn(0, 255) shl 24)

    fun blend(first: Int, second: Int, ratio: Double): Int {
        fun channel(shift: Int): Int {
            val start = first shr shift and 0xff
            val end = second shr shift and 0xff
            return (start + (end - start) * ratio).toInt().coerceIn(0, 255)
        }
        return -0x1000000 or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
}

/** Neutral surfaces never depend on a theme seed; primary/accent keep separate host roles. */
internal data class InstallerPalette(
    val primary: Int,
    val onPrimary: Int,
    val accent: Int,
    val onAccent: Int,
    val background: Int,
    val surface: Int,
    val surfaceVariant: Int,
    val text: Int,
    val muted: Int,
    val outline: Int,
    val divider: Int,
    val danger: Int,
    val isDark: Boolean,
) {
    val disabledText get() = InstallerColorPolicy.blend(surface, muted, 0.55)
    val disabledFill get() = surfaceVariant
    val ripple get() = InstallerColorPolicy.withAlpha(accent, 0x2e)

    companion object {
        fun resolve(appearance: HostAppearance): InstallerPalette {
            val dark = appearance.dark
            val background = if (dark) 0xff121212.toInt() else 0xfff3f4f5.toInt()
            val surface = if (dark) 0xff1e1e1e.toInt() else 0xffffffff.toInt()
            val variant = if (dark) 0xff292a2d.toInt() else 0xffebedef.toInt()
            val primary = ThemeAccentRoles.fromSeed(appearance.primary, dark)
            val accentRoles = ThemeAccentRoles.fromSeed(appearance.accent, dark)
            var accent = InstallerColorPolicy.readableAccent(accentRoles.primary, background)
            repeat(8) {
                for (reference in listOf(background, surface, variant,
                    InstallerColorPolicy.blend(background, accent, 0x1c / 255.0),
                    InstallerColorPolicy.blend(surface, accent, 0x1c / 255.0))) {
                    accent = InstallerColorPolicy.readableAccent(accent, reference)
                }
            }
            return InstallerPalette(
                primary.primary, primary.onPrimary, accent, accentRoles.onPrimary,
                background, surface, variant,
                if (dark) 0xffe6e1e5.toInt() else 0xff1d1b20.toInt(),
                if (dark) 0xffb9bac0.toInt() else 0xff5f6368.toInt(),
                if (dark) 0xff777a82.toInt() else 0xffc5c8ce.toInt(),
                if (dark) 0xff34363a.toInt() else 0xffe0e3e7.toInt(),
                if (dark) 0xffffb4ab.toInt() else 0xffb3261e.toInt(),
                dark,
            )
        }
    }
}
