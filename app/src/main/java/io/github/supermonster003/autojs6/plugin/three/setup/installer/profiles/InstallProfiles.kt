package io.github.supermonster003.autojs6.plugin.three.setup.installer.profiles

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.RequestDocuments
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.InstallerPreferences
import org.autojs.plugin.installer.api.InstallerContract as C
import java.util.Collections
import java.util.UUID

internal object InstallProfileSources {
    const val ANY = "any"
    val ALL = listOf(ANY, C.SOURCE_HOST, C.SOURCE_SCRIPT, C.SOURCE_EXTERNAL)
}

/** A canonical string owns the patch; editor collections and returned JSON cannot mutate it. */
internal class InstallProfileOverrides private constructor(private val encoded: String) {
    val keys: Set<String> get() = document().keySet().toSet()
    fun contains(key: String): Boolean = document().has(key)
    operator fun get(key: String): JsonElement? = document().get(key)
    fun document(): JsonObject = RequestDocuments.parseObject(encoded, "profile options")
    fun with(key: String, value: JsonElement?): InstallProfileOverrides = parse(document().apply {
        require(key in ALLOWED_KEYS)
        if (value == null) remove(key) else add(key, value.deepCopy())
    })

    fun apply(base: InstallOptions, explicit: Set<String>): InstallOptions {
        val merged = InstallerPreferences.optionsDocument(base)
        document().entrySet().forEach { (key, value) -> if (key !in explicit) merged.add(key, value) }
        return InstallOptions.parse(merged, "effective installation options")
    }

    override fun equals(other: Any?): Boolean = other is InstallProfileOverrides && encoded == other.encoded
    override fun hashCode(): Int = encoded.hashCode()

    companion object {
        val BOOLEAN_KEYS = setOf(C.FIELD_ALLOW_DOWNGRADE, C.FIELD_ALLOW_TEST_ONLY, C.FIELD_BYPASS_LOW_TARGET_SDK,
            C.FIELD_DELETE_SOURCE, C.FIELD_GRANT_ALL_REQUESTED_PERMISSIONS, C.FIELD_REQUEST_UPDATE_OWNERSHIP)
        val NULLABLE_KEYS = setOf(C.FIELD_INSTALLER, C.FIELD_INSTALL_REASON, C.FIELD_PACKAGE_SOURCE)
        val ALLOWED_KEYS = BOOLEAN_KEYS + NULLABLE_KEYS + setOf(C.FIELD_AUTHORIZER, C.FIELD_USER, C.FIELD_DEXOPT)
        fun empty() = InstallProfileOverrides("{}")
        fun parse(value: JsonObject): InstallProfileOverrides {
            require(value.keySet().all { it in ALLOWED_KEYS }) { "Unknown or session-wide profile option" }
            value.entrySet().forEach { (key, item) ->
                require(when {
                    item.isJsonNull -> key in NULLABLE_KEYS
                    key in BOOLEAN_KEYS -> item.isJsonPrimitive && item.asJsonPrimitive.isBoolean
                    else -> item.isJsonPrimitive && item.asJsonPrimitive.isString
                }) { "Invalid profile option type: $key" }
            }
            InstallOptions.parse(value, "profile options")
            val canonical = JsonObject().apply { value.keySet().sorted().forEach { add(it, value[it].deepCopy()) } }
            return InstallProfileOverrides(canonical.toString())
        }
    }
}

internal data class InstallProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val enabled: Boolean = true,
    val source: String = InstallProfileSources.ANY,
    val packagePrefix: String = "",
    val overrides: InstallProfileOverrides = InstallProfileOverrides.empty(),
) {
    fun matches(origin: String, packageName: String): Boolean = enabled &&
        (source == InstallProfileSources.ANY || source == origin) && packageName.startsWith(packagePrefix)
}

internal object InstallProfileRules {
    const val MAX_PROFILES = 32
    const val MAX_NAME_LENGTH = 64
    const val MAX_PREFIX_LENGTH = 255
    const val MAX_DOCUMENT_BYTES = 65_536
    private val PREFIX = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)*\\.?")
    fun validName(value: String) = value.length in 1..MAX_NAME_LENGTH && value == value.trim() &&
        value.none { it.code < 32 || it.code == 127 }
    fun validPrefix(value: String) = value.length <= MAX_PREFIX_LENGTH && (value.isEmpty() || PREFIX.matches(value))
    fun validate(profile: InstallProfile) {
        require(UUID.fromString(profile.id).toString() == profile.id)
        require(validName(profile.name)) { "Invalid profile name" }
        require(profile.source in InstallProfileSources.ALL) { "Invalid profile source" }
        require(validPrefix(profile.packagePrefix)) { "Invalid package prefix" }
        InstallProfileOverrides.parse(profile.overrides.document())
    }
    fun validate(profiles: List<InstallProfile>) {
        require(profiles.size <= MAX_PROFILES)
        require(profiles.map { it.id }.distinct().size == profiles.size)
        profiles.forEach(::validate)
    }
}

internal data class InstallProfileSnapshot(
    val revision: String = "unconfigured",
    val profiles: List<InstallProfile> = emptyList(),
    val readable: Boolean = true,
) {
    fun frozen() = copy(profiles = Collections.unmodifiableList(profiles.toList()))
    fun hasCandidates(origin: String) = profiles.any { it.enabled && (it.source == InstallProfileSources.ANY || it.source == origin) }
    fun match(origin: String, packageName: String): InstallProfile? = profiles.firstOrNull { it.matches(origin, packageName) }
}

/** Strict persisted schema, separate from request JSON and from mandatory installation policy. */
internal object InstallProfileCodec {
    fun encode(value: InstallProfileSnapshot): String {
        require(value.readable && UUID.fromString(value.revision).toString() == value.revision)
        InstallProfileRules.validate(value.profiles)
        return JsonObject().apply {
            addProperty("format", 1)
            addProperty("revision", value.revision)
            add("profiles", com.google.gson.JsonArray().apply { value.profiles.forEach { profile -> add(JsonObject().apply {
                addProperty("id", profile.id); addProperty("name", profile.name); addProperty("enabled", profile.enabled)
                addProperty("source", profile.source); addProperty("packagePrefix", profile.packagePrefix)
                add("options", profile.overrides.document())
            }) } })
        }.toString().also { require(it.toByteArray(Charsets.UTF_8).size <= InstallProfileRules.MAX_DOCUMENT_BYTES) }
    }

    fun decode(value: String): InstallProfileSnapshot {
        require(value.toByteArray(Charsets.UTF_8).size <= InstallProfileRules.MAX_DOCUMENT_BYTES)
        requireUniqueMembers(value)
        val root = RequestDocuments.parseObject(value, "installation profiles")
        require(root.keySet() == setOf("format", "revision", "profiles"))
        require(root["format"].isJsonPrimitive && root["format"].asJsonPrimitive.isNumber && root["format"].asString == "1")
        fun text(objectValue: JsonObject, key: String): String = objectValue[key].also {
            require(it != null && it.isJsonPrimitive && it.asJsonPrimitive.isString)
        }.asString
        val revision = text(root, "revision")
        require(UUID.fromString(revision).toString() == revision)
        val array = root["profiles"].also { require(it.isJsonArray) }.asJsonArray
        require(array.size() <= InstallProfileRules.MAX_PROFILES)
        val profiles = array.map { item ->
            val entry = item.also { require(it.isJsonObject) }.asJsonObject
            require(entry.keySet() == setOf("id", "name", "enabled", "source", "packagePrefix", "options"))
            require(entry["enabled"].isJsonPrimitive && entry["enabled"].asJsonPrimitive.isBoolean)
            require(entry["options"].isJsonObject)
            InstallProfile(text(entry, "id"), text(entry, "name"), entry["enabled"].asBoolean,
                text(entry, "source"), text(entry, "packagePrefix"), InstallProfileOverrides.parse(entry["options"].asJsonObject))
        }
        InstallProfileRules.validate(profiles)
        return InstallProfileSnapshot(revision, profiles).frozen()
    }

    private fun requireUniqueMembers(value: String) {
        com.google.gson.stream.JsonReader(java.io.StringReader(value)).use { reader ->
            reader.strictness = com.google.gson.Strictness.STRICT
            fun visit(depth: Int) {
                require(depth <= 8) { "Profile document is too deeply nested" }
                when (reader.peek()) {
                    com.google.gson.stream.JsonToken.BEGIN_OBJECT -> {
                        reader.beginObject()
                        val keys = hashSetOf<String>()
                        while (reader.hasNext()) { require(keys.add(reader.nextName())) { "Duplicate profile field" }; visit(depth + 1) }
                        reader.endObject()
                    }
                    com.google.gson.stream.JsonToken.BEGIN_ARRAY -> {
                        reader.beginArray()
                        while (reader.hasNext()) visit(depth + 1)
                        reader.endArray()
                    }
                    com.google.gson.stream.JsonToken.STRING, com.google.gson.stream.JsonToken.NUMBER -> reader.nextString()
                    com.google.gson.stream.JsonToken.BOOLEAN -> reader.nextBoolean()
                    com.google.gson.stream.JsonToken.NULL -> reader.nextNull()
                    else -> error("Invalid profile JSON value")
                }
            }
            visit(0)
            require(reader.peek() == com.google.gson.stream.JsonToken.END_DOCUMENT)
        }
    }
}
