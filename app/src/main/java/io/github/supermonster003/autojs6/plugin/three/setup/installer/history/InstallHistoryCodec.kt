package io.github.supermonster003.autojs6.plugin.three.setup.installer.history

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

internal object InstallHistoryCodec {
    // 200 records with bounded UTF-8 labels and system messages fit below this ceiling.
    const val MAX_BYTES = 2 * 1024 * 1024

    fun encode(entries: List<InstallHistoryEntry>): ByteArray {
        require(entries.size <= InstallHistoryEntry.MAX_ENTRIES && entries.map { it.id }.distinct().size == entries.size)
        entries.forEach(InstallHistoryEntry::validate)
        val root = JsonObject().apply {
            addProperty("format", 1)
            add("entries", JsonArray().apply { entries.forEach { entry -> add(JsonObject().apply {
                addProperty("id", entry.id); addProperty("token", entry.token); addProperty("itemIndex", entry.itemIndex)
                addProperty("label", entry.label); entry.packageName?.let { addProperty("packageName", it) }
                entry.versionName?.let { addProperty("versionName", it) }; entry.versionCode?.let { addProperty("versionCode", it) }
                entry.previousVersionName?.let { addProperty("previousVersionName", it) }
                entry.previousVersionCode?.let { addProperty("previousVersionCode", it) }
                addProperty("result", entry.result); addProperty("startedAt", entry.startedAt); addProperty("updatedAt", entry.updatedAt)
                entry.finishedAt?.let { addProperty("finishedAt", it) }; addProperty("origin", entry.origin)
                addProperty("authorizer", entry.authorizer); entry.errorCode?.let { addProperty("errorCode", it) }
                entry.systemMessage?.let { addProperty("systemMessage", it) }; entry.durationMillis?.let { addProperty("durationMillis", it) }
                addProperty("interrupted", entry.interrupted)
            }) } })
        }
        return root.toString().toByteArray(Charsets.UTF_8).also { require(it.size <= MAX_BYTES) }
    }

    fun decode(bytes: ByteArray): List<InstallHistoryEntry> {
        require(bytes.size in 1..MAX_BYTES)
        val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
        val root = JsonReader(StringReader(text)).use { reader ->
            reader.strictness = Strictness.STRICT
            JsonParser.parseReader(reader).also { require(reader.peek() == JsonToken.END_DOCUMENT) }.asJsonObject
        }
        fun fields(value: JsonObject, names: Set<String>) { require(value.keySet().all { it in names }) }
        fun string(value: JsonObject, key: String): String? = value.get(key)?.let {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isString); it.asString
        }
        fun number(value: JsonObject, key: String): Long? = value.get(key)?.let {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isNumber); it.asBigDecimal.longValueExact()
        }
        fun boolean(value: JsonObject, key: String): Boolean? = value.get(key)?.let {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isBoolean); it.asBoolean
        }
        fields(root, setOf("format", "entries"))
        require(number(root, "format") == 1L)
        val entries = requireNotNull(root.getAsJsonArray("entries"))
        require(entries.size() <= InstallHistoryEntry.MAX_ENTRIES)
        return entries.map { element ->
            val entry = element.asJsonObject
            fields(entry, setOf("id", "token", "itemIndex", "label", "packageName", "versionName", "versionCode",
                "previousVersionName", "previousVersionCode", "result", "startedAt", "updatedAt", "finishedAt", "origin",
                "authorizer", "errorCode", "systemMessage", "durationMillis", "interrupted"))
            val itemIndex = requireNotNull(number(entry, "itemIndex"))
            require(itemIndex in 0 until org.autojs.plugin.installer.api.InstallerContract.MAX_BATCH_SOURCES.toLong())
            InstallHistoryEntry(requireNotNull(string(entry, "id")), requireNotNull(string(entry, "token")), itemIndex.toInt(),
                requireNotNull(string(entry, "label")), string(entry, "packageName"), string(entry, "versionName"), number(entry, "versionCode"),
                string(entry, "previousVersionName"), number(entry, "previousVersionCode"), requireNotNull(string(entry, "result")),
                requireNotNull(number(entry, "startedAt")), requireNotNull(number(entry, "updatedAt")), number(entry, "finishedAt"),
                requireNotNull(string(entry, "origin")), requireNotNull(string(entry, "authorizer")), string(entry, "errorCode"),
                string(entry, "systemMessage"), number(entry, "durationMillis"), requireNotNull(boolean(entry, "interrupted")))
                .also(InstallHistoryEntry::validate)
        }.also { values -> require(values.map { it.id }.distinct().size == values.size) }
    }
}
