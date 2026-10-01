package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.LocaleList
import android.os.SystemClock
import org.autojs.plugin.common.api.AutoJs6HostSettingsContract as C
import java.util.Locale
import java.util.concurrent.Executors

internal fun HostAppearance.configure(configuration: Configuration): Configuration = Configuration(configuration).apply {
    val locale = Locale.forLanguageTag(language)
    setLocales(LocaleList(locale))
    setLayoutDirection(locale)
    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
        if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
}

internal fun HostAppearance.wrap(context: Context): Context = context.createConfigurationContext(configure(context.resources.configuration))

/** The official Provider is the only source of host appearance. Missing access is a normal fallback. */
internal object HostAppearanceReader {
    private data class Cached(val value: HostAppearance, val readAt: Long)
    private const val CACHE_TTL_MILLIS = 30_000L
    @Volatile private var cache: Cached? = null
    val worker = Executors.newSingleThreadExecutor { task -> Thread(task, "installer-host-appearance").apply { isDaemon = true } }

    fun cached(): HostAppearance? = cache?.takeIf { SystemClock.elapsedRealtime() - it.readAt < CACHE_TTL_MILLIS }?.value

    /** Called only after a still-resumed Activity accepts the refresh, including a missing host. */
    fun accept(value: HostAppearance?) {
        cache = value?.let { Cached(it, SystemClock.elapsedRealtime()) }
    }

    /** Must run on [worker]; keeps no Activity reference, and closes the unstable client. */
    fun read(context: Context): HostAppearance? = runCatching {
        context.applicationContext.contentResolver.acquireUnstableContentProviderClient(Uri.parse(C.CONTENT_URI))?.use { provider ->
            provider.call(C.METHOD_GET_SETTINGS, null, null)?.let(::decode)
        }
    }.getOrNull()

    @Suppress("DEPRECATION")
    fun decode(value: Bundle): HostAppearance? = runCatching {
        require(!value.hasFileDescriptors())
        require(value.get(C.KEY_PROTOCOL_VERSION) is Int && value.getInt(C.KEY_PROTOCOL_VERSION) == C.PROTOCOL_VERSION)
        require(value.getString(C.KEY_HOST_PACKAGE_NAME) == C.HOST_PACKAGE_NAME)
        require(value.get(C.KEY_DARK_MODE_ACTIVE) is Boolean)
        require(value.get(C.KEY_THEME_COLOR_PRIMARY) is Int && value.get(C.KEY_THEME_COLOR_ACCENT) is Int)
        val language = requireNotNull(value.getString(C.KEY_RESOLVED_LANGUAGE_TAG))
        require(language.length in 2..80 && language.matches(Regex("[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*")))
        // forLanguageTag alone silently truncates malformed subtags; Builder validates the whole tag.
        val locale = Locale.Builder().setLanguageTag(language).build()
        require(locale.language.isNotEmpty())
        HostAppearance(language, value.getBoolean(C.KEY_DARK_MODE_ACTIVE),
            value.getInt(C.KEY_THEME_COLOR_PRIMARY) or -0x1000000, value.getInt(C.KEY_THEME_COLOR_ACCENT) or -0x1000000)
    }.getOrNull()
}
