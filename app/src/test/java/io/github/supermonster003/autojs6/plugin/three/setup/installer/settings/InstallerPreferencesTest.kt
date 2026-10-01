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
        val preference = AuthorizerPreferences(listOf(Authorizer.ROOT, Authorizer.NONE, Authorizer.SHIZUKU, Authorizer.DHIZUKU), setOf(Authorizer.ROOT, Authorizer.NONE))
        val restored = AuthorizerPreferences.decode(preference.encodeOrder(), preference.encodeEnabled())
        assertEquals(preference, restored)
        val states = Authorizer.entries.associateWith { AuthorizerState(it, true, true, true) }
        assertEquals(Authorizer.ROOT, AuthorizerResolver.resolve("auto", states, restored.order, restored.enabled))
        assertEquals(Authorizer.NONE, AuthorizerResolver.resolve("auto", states + (Authorizer.ROOT to AuthorizerState(Authorizer.ROOT, true, true, false)), restored.order, restored.enabled))
        assertEquals(Authorizer.SHIZUKU, AuthorizerResolver.resolve("shizuku", states, restored.order, restored.enabled))
    }

    @Test fun allLegacyOrdersMigrateWithoutEnablingNewPrivilegesOrChangingExistingSelection() {
        val legacy = listOf(Authorizer.SHIZUKU, Authorizer.ROOT, Authorizer.NONE)
        val orders = legacy.flatMap { first -> (legacy - first).map { second ->
            listOf(first, second, legacy.single { it != first && it != second })
        } }
        fun encode(values: List<Authorizer>) = values.joinToString(prefix = "[", postfix = "]") { "\"${it.id}\"" }
        val states = Authorizer.entries.associateWith { AuthorizerState(it, true, true, true) }
        for (order in orders) for (mask in 1..7) {
            val enabled = legacy.filterIndexed { index, _ -> mask and (1 shl index) != 0 }.toSet()
            val restored = AuthorizerPreferences.decode(encode(order), encode(order.filter { it in enabled }))
            assertEquals(order, restored.order.filterNot { it == Authorizer.DHIZUKU })
            assertEquals(restored.order.indexOf(Authorizer.NONE) - 1, restored.order.indexOf(Authorizer.DHIZUKU))
            assertEquals(enabled, restored.enabled)
            assertFalse(restored.enabled.contains(Authorizer.DHIZUKU))
            assertEquals(order.first { it in enabled }, AuthorizerResolver.resolve("auto", states, restored.order, restored.enabled))
            assertEquals(restored, AuthorizerPreferences.decode(restored.encodeOrder(), restored.encodeEnabled()))
        }
    }

    @Test fun freshPreferencesEnableFourAuthorizersButLegacyValuesCannotSneakInDhizuku() {
        val fresh = AuthorizerPreferences.decode(null, null)
        assertEquals(Authorizer.DEFAULT_ORDER, fresh.order)
        assertEquals(Authorizer.entries.toSet(), fresh.enabled)
        assertEquals(AuthorizerPreferences(), AuthorizerPreferences.decode("[\"root\",\"none\",\"shizuku\"]", "[\"root\",\"dhizuku\"]"))
        assertEquals(AuthorizerPreferences(), AuthorizerPreferences.decode("[\"shizuku\",\"dhizuku\",\"none\"]", "[\"none\"]"))
        val deliberate = AuthorizerPreferences(listOf(Authorizer.DHIZUKU, Authorizer.NONE, Authorizer.ROOT, Authorizer.SHIZUKU), setOf(Authorizer.DHIZUKU))
        assertEquals(deliberate, AuthorizerPreferences.decode(deliberate.encodeOrder(), deliberate.encodeEnabled()))
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
