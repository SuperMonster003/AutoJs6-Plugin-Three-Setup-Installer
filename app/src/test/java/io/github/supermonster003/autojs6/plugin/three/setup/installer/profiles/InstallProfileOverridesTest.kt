package io.github.supermonster003.autojs6.plugin.three.setup.installer.profiles

import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import org.autojs.plugin.installer.api.InstallerContract as C
import org.junit.Assert.*
import org.junit.Test

class InstallProfileOverridesTest {
    private fun patch(json: String) = InstallProfileOverrides.parse(JsonParser.parseString(json).asJsonObject)
    private fun rejected(action: () -> Unit) { assertThrows(Exception::class.java) { action() } }

    @Test fun partialPatchInheritsEveryUnspecifiedValueAndNeverChangesSessionWideControls() {
        val base = InstallOptions(authorizer = "shizuku", allowDowngrade = true, allowTestOnly = true,
            bypassLowTargetSdk = true, installer = "org.example.installer", user = "10", deleteSource = false,
            continueOnError = false, timeoutMillis = 45_000, grantAllRequestedPermissions = false,
            requestUpdateOwnership = true, dexopt = "verify", installReason = "policy", packageSource = "store")
        val selected = patch("""{"allowDowngrade":false,"grantAllRequestedPermissions":true}""")
        assertEquals(base.copy(allowDowngrade = false, grantAllRequestedPermissions = true), selected.apply(base, emptySet()))
        assertEquals(base, InstallProfileOverrides.empty().apply(base, emptySet()))
        for (json in listOf("""{"timeoutMillis":1}""", """{"continueOnError":true}""", """{"interaction":"silent"}""",
            """{"applySourceProfiles":true}""", """{"signatureApproval":true}""", """{"unknown":null}""")) {
            rejected { patch(json) }
        }
    }

    @Test fun explicitFalseAutoCurrentNoneAndNullWinEvenThoughTheyLookLikeDefaults() {
        val value = patch("""{
            "authorizer":"root","user":"all","dexopt":"speed",
            "allowDowngrade":true,"allowTestOnly":true,"bypassLowTargetSdk":true,"deleteSource":true,
            "grantAllRequestedPermissions":true,"requestUpdateOwnership":true,
            "installer":"org.example.owner","installReason":"user","packageSource":"other"
        }""")
        val explicitDefaults = InstallOptions(continueOnError = false, timeoutMillis = 31_000)
        assertEquals(12, value.keys.size)
        assertEquals(explicitDefaults, value.apply(explicitDefaults, value.keys))
        val inherited = value.apply(explicitDefaults, emptySet())
        assertEquals("root", inherited.authorizer); assertEquals("all", inherited.user); assertEquals("speed", inherited.dexopt)
        assertTrue(inherited.allowDowngrade && inherited.allowTestOnly && inherited.bypassLowTargetSdk && inherited.deleteSource)
        assertTrue(inherited.grantAllRequestedPermissions && inherited.requestUpdateOwnership)
        assertEquals("org.example.owner", inherited.installer); assertEquals("user", inherited.installReason); assertEquals("other", inherited.packageSource)
        assertFalse(inherited.continueOnError); assertEquals(31_000L, inherited.timeoutMillis)
    }

    @Test fun explicitMaskProtectsOnlyItsOwnFieldsAndCannotMutateAnAlreadyResolvedResult() {
        val value = patch("""{"authorizer":"root","deleteSource":true,"grantAllRequestedPermissions":true,"dexopt":"speed"}""")
        val explicit = mutableSetOf(C.FIELD_AUTHORIZER, C.FIELD_DELETE_SOURCE)
        val base = InstallOptions()
        val resolved = value.apply(base, explicit)
        assertEquals(base.copy(grantAllRequestedPermissions = true, dexopt = "speed"), resolved)
        explicit.clear()
        assertEquals("auto", resolved.authorizer); assertFalse(resolved.deleteSource)
        assertEquals("root", value.apply(base, explicit).authorizer)
        assertTrue(value.apply(base, explicit).deleteSource)
    }

    @Test fun jsonNullResetsOnlyNullableMetadataAndRemovingAPatchRestoresInheritance() {
        val base = InstallOptions(installer = "org.example.installer", installReason = "policy", packageSource = "store")
        val clear = patch("""{"installer":null,"installReason":null,"packageSource":null}""")
        assertEquals(base.copy(installer = null, installReason = null, packageSource = null), clear.apply(base, emptySet()))
        assertTrue(clear.contains(C.FIELD_INSTALLER)); assertTrue(clear[C.FIELD_INSTALLER]!!.isJsonNull)
        val inheritInstaller = clear.with(C.FIELD_INSTALLER, null)
        assertFalse(inheritInstaller.contains(C.FIELD_INSTALLER))
        assertEquals(base.copy(installReason = null, packageSource = null), inheritInstaller.apply(base, emptySet()))
        assertNull(inheritInstaller[C.FIELD_INSTALLER])
        assertNull(inheritInstaller.with(C.FIELD_INSTALLER, JsonNull.INSTANCE).apply(base, emptySet()).installer)
        for (key in InstallProfileOverrides.BOOLEAN_KEYS + setOf(C.FIELD_AUTHORIZER, C.FIELD_USER, C.FIELD_DEXOPT)) {
            rejected { InstallProfileOverrides.parse(JsonObject().apply { add(key, JsonNull.INSTANCE) }) }
        }
    }

    @Test fun patchTypesAndEnumValuesAreValidatedBeforeTheyCanInfluenceAuthorization() {
        for (key in InstallProfileOverrides.BOOLEAN_KEYS) {
            for (value in listOf(JsonPrimitive("true"), JsonPrimitive(1))) rejected {
                InstallProfileOverrides.parse(JsonObject().apply { add(key, value) })
            }
        }
        for (json in listOf("""{"authorizer":true}""", """{"authorizer":"ROOT"}""", """{"user":0}""",
            """{"user":"-1"}""", """{"installer":"../other"}""", """{"dexopt":"speed;id"}""",
            """{"installReason":"rollback"}""", """{"packageSource":3}""", """{"packageSource":"internet"}""")) {
            rejected { patch(json) }
        }
    }

    @Test fun canonicalPatchOwnsItsJsonAndDoesNotExposeAMutableEditorAlias() {
        val input = JsonParser.parseString("""{"deleteSource":true,"authorizer":"root"}""").asJsonObject
        val value = InstallProfileOverrides.parse(input)
        val expected = patch("""{"authorizer":"root","deleteSource":true}""")
        input.addProperty("authorizer", "none"); input.remove("deleteSource")
        val returned = value.document()
        returned.addProperty("authorizer", "none"); returned.remove("deleteSource")
        runCatching { (value.keys as MutableSet<String>).clear() }
        assertEquals(expected, value); assertEquals(expected.hashCode(), value.hashCode())
        assertEquals("root", value[C.FIELD_AUTHORIZER]!!.asString)
        assertTrue(value[C.FIELD_DELETE_SOURCE]!!.asBoolean)
        val changed = value.with(C.FIELD_DELETE_SOURCE, JsonPrimitive(false))
        assertFalse(changed[C.FIELD_DELETE_SOURCE]!!.asBoolean)
        assertTrue(value[C.FIELD_DELETE_SOURCE]!!.asBoolean)
        assertEquals(InstallProfileOverrides.empty(), value.with(C.FIELD_DELETE_SOURCE, null).with(C.FIELD_AUTHORIZER, null))
    }
}
