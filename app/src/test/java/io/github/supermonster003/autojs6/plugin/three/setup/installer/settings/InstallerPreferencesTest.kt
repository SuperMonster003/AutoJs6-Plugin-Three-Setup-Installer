package io.github.supermonster003.autojs6.plugin.three.setup.installer.settings

import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerResolver
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerState
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import org.autojs.plugin.installer.api.InstallerContract
import org.junit.Assert.*
import org.junit.Test

class InstallerPreferencesTest {
    @Test fun explicitOrderAndDisabledMethodsRoundTripAndAffectAutomaticSelection() {
        val preference = AuthorizerPreferences(listOf(Authorizer.ROOT, Authorizer.NONE, Authorizer.SHIZUKU), setOf(Authorizer.ROOT, Authorizer.NONE))
        val restored = AuthorizerPreferences.decode(preference.encodeOrder(), preference.encodeEnabled())
        assertEquals(preference, restored)
        val states = Authorizer.entries.associateWith { AuthorizerState(it, true, true, true) }
        assertEquals(Authorizer.ROOT, AuthorizerResolver.resolve("auto", states, restored.order, restored.enabled))
        assertEquals(Authorizer.NONE, AuthorizerResolver.resolve("auto", states + (Authorizer.ROOT to AuthorizerState(Authorizer.ROOT, true, true, false)), restored.order, restored.enabled))
        assertEquals(Authorizer.SHIZUKU, AuthorizerResolver.resolve("shizuku", states, restored.order, restored.enabled))
    }

    @Test fun corruptIncompleteDuplicateAndUnknownValuesRestoreSafeDefaults() {
        for ((order, enabled) in listOf(null to null, "broken" to "[]", "[\"root\"]" to "[\"root\"]",
            "[\"root\",\"root\",\"none\"]" to "[\"root\"]", "[\"shizuku\",\"root\",\"none\"]" to "[]",
            "[\"shizuku\",\"root\",\"adb\"]" to "[\"root\"]", "[\"shizuku\",\"root\",\"none\"]" to "[3]")) {
            assertEquals(AuthorizerPreferences(), AuthorizerPreferences.decode(order, enabled))
        }
    }

    @Test fun localDefaultShowsDialogWithoutChangingEngineAutoContract() {
        assertEquals(InstallerContract.INTERACTION_DIALOG, InstallerPreferences().interaction)
        assertEquals(InstallerContract.AUTHORIZER_AUTO, InstallerPreferences().options.authorizer)
    }

    @Test fun optionsSerializeWithoutLosingInstallerFlagsOrTargetUser() {
        val options = InstallOptions(allowDowngrade = true, allowTestOnly = true, bypassLowTargetSdk = true,
            installer = "com.android.shell", user = "10", deleteSource = true, continueOnError = false)
        assertEquals(options, InstallOptions.parse(InstallerPreferences.optionsDocument(options), "defaults"))
        assertFalse(InstallerPreferences.optionsDocument(InstallOptions()).has(InstallerContract.FIELD_INSTALLER))
    }

    @Test fun colorInputPreservesOpaqueSeedsAndRejectsAlphaOverflowAndTrailingGarbage() {
        assertEquals(0xffffdead.toInt(), ThemeColorValue.parse(" #FFDEAD "))
        assertEquals(0xff1234ab.toInt(), ThemeColorValue.parse("1234ab"))
        assertEquals(0xff00ff40.toInt(), ThemeColorValue.parse("RGB(0, 255, 64)"))
        for (invalid in listOf("#fff", "#FFFFDEAD", "rgb(0,256,0)", "rgb(-1,0,0)", "rgb(1,2)", "rgb(1,2,3) x", "#FFDEAD\n#000000")) {
            assertNull(invalid, ThemeColorValue.parse(invalid))
        }
    }
}
