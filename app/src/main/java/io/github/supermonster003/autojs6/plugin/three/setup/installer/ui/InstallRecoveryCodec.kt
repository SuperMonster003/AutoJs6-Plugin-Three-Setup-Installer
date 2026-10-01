package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/** Versioned allow-list. Unknown fields/types and corrupt files never become executable requests. */
internal object InstallRecoveryCodec {
    fun encode(value: InstallRecoverySnapshot): ByteArray {
        InstallRecoverySnapshot.validate(value)
        fun error(value: InstallRecoverySnapshot.Failure) = JsonObject().apply {
            addProperty("code", value.code)
            value.status?.let { addProperty("status", it) }
            value.platformCode?.let { addProperty("platform", it) }
        }
        val json = JsonObject().apply {
            addProperty("format", 1); addProperty("token", value.token); addProperty("revision", value.revision)
            addProperty("savedAt", value.savedAt); addProperty("expiresAt", value.expiresAt)
            addProperty("terminal", value.terminal); addProperty("stage", value.stage); addProperty("index", value.index)
            addProperty("canDeleteSource", value.canDeleteSource)
            value.failure?.let { add("failure", error(it)) }
            add("items", JsonArray().apply { value.items.forEach { item -> add(JsonObject().apply {
                addProperty("label", item.label)
                item.packageName?.let { addProperty("package", it) }
                item.versionName?.let { addProperty("versionName", it) }
                item.versionCode?.let { addProperty("versionCode", it) }
                item.previousVersionCode?.let { addProperty("previousVersionCode", it) }
                addProperty("user", item.user); addProperty("deleteRequested", item.deleteRequested)
                item.ok?.let { addProperty("ok", it) }; item.sourceDeleted?.let { addProperty("sourceDeleted", it) }
                item.failure?.let { add("failure", error(it)) }
            }) } })
        }
        return json.toString().toByteArray(Charsets.UTF_8).also { require(it.size <= InstallRecoverySnapshot.MAX_BYTES) }
    }

    fun decode(bytes: ByteArray): InstallRecoverySnapshot {
        require(bytes.size in 1..InstallRecoverySnapshot.MAX_BYTES)
        val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
        val root = JsonReader(StringReader(text)).use { reader ->
            reader.strictness = Strictness.STRICT
            JsonParser.parseReader(reader).also { require(reader.peek() == JsonToken.END_DOCUMENT) }.asJsonObject
        }
        fun fields(value: JsonObject, names: Set<String>) { require(value.keySet().all { it in names }) }
        fun string(value: JsonObject, key: String): String? = value.get(key)?.let { require(it.isJsonPrimitive && it.asJsonPrimitive.isString); it.asString }
        fun number(value: JsonObject, key: String): Long? = value.get(key)?.let {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isNumber); it.asBigDecimal.longValueExact()
        }
        fun boolean(value: JsonObject, key: String): Boolean? = value.get(key)?.let { require(it.isJsonPrimitive && it.asJsonPrimitive.isBoolean); it.asBoolean }
        fun integer(value: JsonObject, key: String): Int? = number(value, key)?.let { require(it in Int.MIN_VALUE..Int.MAX_VALUE); it.toInt() }
        fun error(value: JsonObject): InstallRecoverySnapshot.Failure {
            fields(value, setOf("code", "status", "platform"))
            return InstallRecoverySnapshot.Failure(requireNotNull(string(value, "code")), integer(value, "status"), string(value, "platform"))
        }
        fields(root, setOf("format", "token", "revision", "savedAt", "expiresAt", "terminal", "stage", "index", "items", "failure", "canDeleteSource"))
        require(number(root, "format") == 1L)
        val items = requireNotNull(root.getAsJsonArray("items"))
        require(items.size() in 1..org.autojs.plugin.installer.api.InstallerContract.MAX_BATCH_SOURCES)
        return InstallRecoverySnapshot(requireNotNull(string(root, "token")), requireNotNull(number(root, "revision")),
            requireNotNull(number(root, "savedAt")), requireNotNull(number(root, "expiresAt")), requireNotNull(boolean(root, "terminal")),
            requireNotNull(string(root, "stage")), requireNotNull(integer(root, "index")), items.map { element ->
                val item = element.asJsonObject
                fields(item, setOf("label", "package", "versionName", "versionCode", "previousVersionCode", "user", "deleteRequested", "ok", "sourceDeleted", "failure"))
                InstallRecoverySnapshot.Item(requireNotNull(string(item, "label")), string(item, "package"), string(item, "versionName"),
                    number(item, "versionCode"), number(item, "previousVersionCode"), requireNotNull(string(item, "user")),
                    requireNotNull(boolean(item, "deleteRequested")), boolean(item, "ok"), boolean(item, "sourceDeleted"), item.getAsJsonObject("failure")?.let(::error))
            }, root.getAsJsonObject("failure")?.let(::error), requireNotNull(boolean(root, "canDeleteSource")))
            .also(InstallRecoverySnapshot::validate)
    }
}
