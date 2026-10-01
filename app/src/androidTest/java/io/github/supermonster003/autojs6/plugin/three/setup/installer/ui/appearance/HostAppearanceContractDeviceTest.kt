package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance

import android.content.res.Configuration
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.autojs.plugin.common.api.AutoJs6HostSettingsContract as C
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HostAppearanceContractDeviceTest {
    @Test fun validSnapshotPreservesIndependentSeedsAndAppliesLocaleAndNightTogether() {
        val snapshot = requireNotNull(HostAppearanceReader.decode(validBundle()))
        assertEquals("ar", snapshot.language)
        assertEquals(0xffffdead.toInt(), snapshot.primary)
        assertEquals(0xff112233.toInt(), snapshot.accent)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val original = Configuration(context.resources.configuration)
        val wrapped = snapshot.wrap(context)
        assertEquals("ar", wrapped.resources.configuration.locales[0].language)
        assertEquals(View.LAYOUT_DIRECTION_RTL, wrapped.resources.configuration.layoutDirection)
        assertEquals(Configuration.UI_MODE_NIGHT_YES, wrapped.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
        assertEquals(original, context.resources.configuration)
    }

    @Test fun unknownProtocolHostMissingFieldsAndWrongTypesAreRejected() {
        assertNull(HostAppearanceReader.decode(validBundle().apply { putInt(C.KEY_PROTOCOL_VERSION, C.PROTOCOL_VERSION + 1) }))
        assertNull(HostAppearanceReader.decode(validBundle().apply { putString(C.KEY_HOST_PACKAGE_NAME, "untrusted.host") }))
        assertNull(HostAppearanceReader.decode(validBundle().apply { remove(C.KEY_DARK_MODE_ACTIVE) }))
        assertNull(HostAppearanceReader.decode(validBundle().apply { putString(C.KEY_DARK_MODE_ACTIVE, "true") }))
        assertNull(HostAppearanceReader.decode(validBundle().apply { putLong(C.KEY_THEME_COLOR_ACCENT, 1L) }))
        assertNull(HostAppearanceReader.decode(validBundle().apply { putString(C.KEY_PROTOCOL_VERSION, "1") }))
    }

    @Test fun malformedOrUnboundedLanguageTagsCannotPartiallyOverrideTheLocale() {
        for (language in listOf("", "en--US", "en_US", "en-badlongsubtag", "x-private", "a".repeat(81))) {
            assertNull(language, HostAppearanceReader.decode(validBundle().apply { putString(C.KEY_RESOLVED_LANGUAGE_TAG, language) }))
        }
        for (language in listOf("en", "zh-Hans", "zh-Hant-HK", "zh-Hant-TW", "ar", "en-US-u-nu-latn")) {
            assertEquals(language, HostAppearanceReader.decode(validBundle().apply { putString(C.KEY_RESOLVED_LANGUAGE_TAG, language) })?.language)
        }
    }

    @Test fun fileDescriptorsAreNeverAcceptedAsPartOfAnAppearanceSnapshot() {
        val descriptors = ParcelFileDescriptor.createPipe()
        try {
            assertNull(HostAppearanceReader.decode(validBundle().apply { putParcelable("unexpectedDescriptor", descriptors[0]) }))
        } finally {
            descriptors.forEach(ParcelFileDescriptor::close)
        }
    }

    private fun validBundle() = Bundle().apply {
        putInt(C.KEY_PROTOCOL_VERSION, C.PROTOCOL_VERSION)
        putString(C.KEY_HOST_PACKAGE_NAME, C.HOST_PACKAGE_NAME)
        putString(C.KEY_RESOLVED_LANGUAGE_TAG, "ar")
        putBoolean(C.KEY_DARK_MODE_ACTIVE, true)
        putInt(C.KEY_THEME_COLOR_PRIMARY, 0xffffdead.toInt())
        putInt(C.KEY_THEME_COLOR_ACCENT, 0xff112233.toInt())
    }
}
