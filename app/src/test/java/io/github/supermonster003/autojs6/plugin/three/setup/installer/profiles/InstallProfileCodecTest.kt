package io.github.supermonster003.autojs6.plugin.three.setup.installer.profiles

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class InstallProfileCodecTest {
    @Test fun profileOptionFieldsMatchTheSharedHostContract() {
        assertEquals(org.autojs.plugin.installer.api.InstallerContract.PROFILE_OPTION_FIELDS.toSet(), InstallProfileOverrides.ALLOWED_KEYS)
    }

    private val revision = UUID(1, 2).toString()
    private fun profile(index: Int = 1, name: String = "Profile $index") = InstallProfile(UUID(0, index.toLong()).toString(), name)
    private fun document(): JsonObject = JsonParser.parseString(InstallProfileCodec.encode(
        InstallProfileSnapshot(revision, listOf(profile())))).asJsonObject
    private fun rejected(action: () -> Unit) { assertThrows(Exception::class.java) { action() } }

    @Test fun persistedDocumentRoundTripsThirtyTwoProfilesAndKeepsTheirOrder() {
        val profiles = (1..32).map { profile(it).copy(enabled = it % 2 == 0, packagePrefix = "org.example.p$it.") }
        val expected = InstallProfileSnapshot(revision, profiles).frozen()
        val encoded = InstallProfileCodec.encode(expected)
        assertEquals(expected, InstallProfileCodec.decode(encoded))
        assertEquals(profiles.map { it.id }, InstallProfileCodec.decode(encoded).profiles.map { it.id })
        assertEquals(encoded, InstallProfileCodec.encode(InstallProfileCodec.decode(encoded)))
        val tooMany = InstallProfileSnapshot(revision, profiles + profile(33))
        rejected { InstallProfileCodec.encode(tooMany) }
        val storedTooMany = document().apply {
            add("profiles", JsonArray().also { values ->
                val template = document().getAsJsonArray("profiles")[0].asJsonObject
                for (index in 1..33) values.add(template.deepCopy().apply { addProperty("id", profile(index).id) })
            })
        }
        rejected { InstallProfileCodec.decode(storedTooMany.toString()) }
    }

    @Test fun duplicateIdsAreRejectedEvenIfOneProfileIsDisabled() {
        val original = profile()
        rejected { InstallProfileCodec.encode(InstallProfileSnapshot(revision, listOf(original, original.copy(enabled = false)))) }
        val root = document()
        root.getAsJsonArray("profiles").add(root.getAsJsonArray("profiles")[0].deepCopy())
        rejected { InstallProfileCodec.decode(root.toString()) }
    }

    @Test fun rootAndProfileSchemasRejectMissingUnknownAndWronglyTypedFields() {
        for (key in listOf("format", "revision", "profiles")) rejected {
            InstallProfileCodec.decode(document().apply { remove(key) }.toString())
        }
        rejected { InstallProfileCodec.decode(document().apply { addProperty("extra", true) }.toString()) }
        for (format in listOf<JsonElement>(JsonPrimitive("1"), JsonPrimitive(true), JsonNull.INSTANCE,
            JsonParser.parseString("1.0"), JsonParser.parseString("1e0"), JsonPrimitive(2))) rejected {
            InstallProfileCodec.decode(document().apply { add("format", format) }.toString())
        }
        for (value in listOf("unconfigured", "unreadable", "ABCDEFAB-CDEF-ABCD-EFAB-CDEFABCDEFAB", "not-a-uuid")) rejected {
            InstallProfileCodec.decode(document().apply { addProperty("revision", value) }.toString())
        }
        for (key in listOf("id", "name", "enabled", "source", "packagePrefix", "options")) rejected {
            val root = document()
            root.getAsJsonArray("profiles")[0].asJsonObject.remove(key)
            InstallProfileCodec.decode(root.toString())
        }
        for ((key, value) in listOf("enabled" to JsonPrimitive("true"), "source" to JsonPrimitive(1),
            "packagePrefix" to JsonNull.INSTANCE, "options" to JsonArray(), "name" to JsonPrimitive(false), "id" to JsonNull.INSTANCE)) rejected {
            val root = document()
            root.getAsJsonArray("profiles")[0].asJsonObject.add(key, value)
            InstallProfileCodec.decode(root.toString())
        }
        rejected {
            val root = document()
            root.getAsJsonArray("profiles")[0].asJsonObject.addProperty("interaction", "silent")
            InstallProfileCodec.decode(root.toString())
        }
        for (invalid in listOf("null", "[]", "true", document().toString() + "{}")) rejected { InstallProfileCodec.decode(invalid) }
    }

    @Test fun namesSourcesPrefixesAndCanonicalIdsAreValidatedWithoutGuessing() {
        for (name in listOf("", " leading", "trailing ", "line\nbreak", "bad\u0000name", "bad\u007fname", "x".repeat(65))) {
            rejected { InstallProfileRules.validate(profile(name = name)) }
        }
        InstallProfileRules.validate(profile(name = "工作安装 " + "A".repeat(59)))
        InstallProfileRules.validate(profile(name = "x".repeat(64)))
        for (source in listOf("", "*", "HOST", "host ", "unknown", "home")) rejected {
            InstallProfileRules.validate(profile().copy(source = source))
        }
        for (prefix in listOf(" ", ".org", "org..example", "org.example.*", "org/example", "org.-example", "应用.example", "x".repeat(256))) rejected {
            InstallProfileRules.validate(profile().copy(packagePrefix = prefix))
        }
        for (prefix in listOf("", "org", "org.example.", "ORG.Example_2", "x".repeat(255))) {
            InstallProfileRules.validate(profile().copy(packagePrefix = prefix))
        }
        for (id in listOf("", "1-1-1-1-1", "ABCDEFAB-CDEF-ABCD-EFAB-CDEFABCDEFAB")) rejected {
            InstallProfileRules.validate(profile().copy(id = id))
        }
    }

    @Test fun decoderEnforcesUtf8BytesRatherThanTheStringCharacterCount() {
        val expected = InstallProfileSnapshot(revision, listOf(profile(name = "名".repeat(64)))).frozen()
        val encoded = InstallProfileCodec.encode(expected)
        val padding = InstallProfileRules.MAX_DOCUMENT_BYTES - encoded.toByteArray(Charsets.UTF_8).size
        val atLimit = " ".repeat(padding) + encoded
        assertTrue(atLimit.length < InstallProfileRules.MAX_DOCUMENT_BYTES)
        assertEquals(InstallProfileRules.MAX_DOCUMENT_BYTES, atLimit.toByteArray(Charsets.UTF_8).size)
        assertEquals(expected, InstallProfileCodec.decode(atLimit))
        val oneByteOver = " " + atLimit
        assertTrue(oneByteOver.length < InstallProfileRules.MAX_DOCUMENT_BYTES)
        rejected { InstallProfileCodec.decode(oneByteOver) }
    }

    @Test fun encoderRejectsAnOverBudgetDocumentEvenWhenEveryRuleIsIndividuallyValid() {
        // A numeric user string is legal; the complete persisted document still has its byte cap.
        val patch = InstallProfileOverrides.parse(JsonObject().apply { addProperty("user", "0".repeat(2_000)) })
        val profiles = (1..32).map { profile(it, "名".repeat(64)).copy(overrides = patch) }
        InstallProfileRules.validate(profiles)
        rejected { InstallProfileCodec.encode(InstallProfileSnapshot(revision, profiles)) }
    }

    @Test fun unreadableAndUnconfiguredSnapshotsCannotBeMistakenForSavedEmptyRules() {
        rejected { InstallProfileCodec.encode(InstallProfileSnapshot()) }
        rejected { InstallProfileCodec.encode(InstallProfileSnapshot(revision, readable = false)) }
        val savedEmpty = InstallProfileSnapshot(revision)
        assertEquals(savedEmpty, InstallProfileCodec.decode(InstallProfileCodec.encode(savedEmpty)))
        assertTrue(savedEmpty.readable)
    }

    @Test fun repeatedJsonMembersAreRejectedBeforeAParserCanKeepOnlyTheLastValue() {
        val root = document().toString()
        val duplicateRoot = root.replaceFirst("\"format\":1", "\"format\":1,\"format\":1")
        assertNotEquals(root, duplicateRoot)
        rejected { InstallProfileCodec.decode(duplicateRoot) }
        val duplicateProfile = root.replaceFirst("\"enabled\":true", "\"enabled\":false,\"enabled\":true")
        assertNotEquals(root, duplicateProfile)
        rejected { InstallProfileCodec.decode(duplicateProfile) }
        val duplicateOption = root.replace("\"options\":{}", "\"options\":{\"deleteSource\":false,\"deleteSource\":true}")
        assertNotEquals(root, duplicateOption)
        rejected { InstallProfileCodec.decode(duplicateOption) }
    }

    @Test fun deeplyNestedDocumentsAreRefusedWithoutEscapingAsAStackOverflow() {
        val nested = "[".repeat(2_000) + "0" + "]".repeat(2_000)
        val value = "{\"format\":1,\"revision\":\"$revision\",\"profiles\":$nested}"
        assertTrue(value.toByteArray(Charsets.UTF_8).size < InstallProfileRules.MAX_DOCUMENT_BYTES)
        rejected { InstallProfileCodec.decode(value) }
    }
}
