package io.github.supermonster003.autojs6.plugin.three.setup.installer.profiles

import android.content.Context
import java.io.File
import java.security.MessageDigest
import java.util.UUID

internal enum class InstallProfileSaveResult { SAVED, CONFLICT, INVALID, FAILED }

/** One atomic preferences document and compare-before-save; readers keep an immutable snapshot. */
internal object InstallProfilePreferences {
    private const val FILE = "installation_profiles"
    private const val DOCUMENT = "document"
    private val writers = mutableMapOf<String, InstallProfilePreferenceWriter>()

    @Synchronized fun read(context: Context): InstallProfileSnapshot {
        val file = File(context.applicationInfo.dataDir, "shared_prefs/$FILE.xml")
        writers[file.path]?.unreadableSnapshot?.let {
            // A failed disk write still updates SharedPreferences' memory. Do not let an
            // unproved rollback become effective defaults; this identity also participates in CAS.
            return it
        }
        return try {
            val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            val value = prefs.getString(DOCUMENT, null)
            if (value == null && !file.exists() && !File(file.path + ".bak").exists()) InstallProfileSnapshot()
            else if (value == null) unreadable(file)
            else try { InstallProfileCodec.decode(value) } catch (_: Exception) {
                InstallProfileSnapshot("unreadable:" + digest(value.toByteArray(Charsets.UTF_8)), readable = false)
            }
        } catch (_: Exception) { unreadable(file) }
    }

    @Synchronized fun save(context: Context, expected: InstallProfileSnapshot, profiles: List<InstallProfile>): InstallProfileSaveResult {
        val encoded = try { InstallProfileCodec.encode(InstallProfileSnapshot(UUID.randomUUID().toString(), profiles.toList())) }
            catch (_: Exception) { return InstallProfileSaveResult.INVALID }
        if (read(context) != expected) return InstallProfileSaveResult.CONFLICT
        val file = File(context.applicationInfo.dataDir, "shared_prefs/$FILE.xml")
        val writer = writers.getOrPut(file.path) { InstallProfilePreferenceWriter() }
        val storage = object : InstallProfilePreferenceWriter.Storage {
            private fun preferences() = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            override fun values(): Map<String, *> = preferences().all
            override fun fileExists() = file.exists() || File(file.path + ".bak").exists() || File(file.path + ".new").exists()
            override fun commit(field: InstallProfilePreferenceWriter.Field): Boolean = preferences().edit().apply {
                if (!field.present) remove(DOCUMENT)
                else when (val value = field.value) {
                    is String -> putString(DOCUMENT, value)
                    is Boolean -> putBoolean(DOCUMENT, value)
                    is Int -> putInt(DOCUMENT, value)
                    is Long -> putLong(DOCUMENT, value)
                    is Float -> putFloat(DOCUMENT, value)
                    is Set<*> -> putStringSet(DOCUMENT, value.map { require(it is String); it }.toSet())
                    else -> error("Unsupported original profile preference type")
                }
            }.commit()
            override fun deleteEmptyFile(): Boolean {
                // Never delete an unrelated preference key added during the failed save.
                if (preferences().all.isNotEmpty()) return false
                return context.deleteSharedPreferences(FILE) && preferences().all.isEmpty() && !fileExists()
            }
        }
        return if (writer.write(storage, encoded, expected == InstallProfileSnapshot()))
            InstallProfileSaveResult.SAVED else InstallProfileSaveResult.FAILED
    }

    private fun unreadable(file: File): InstallProfileSnapshot {
        // A damaged XML file can look empty to SharedPreferences. Keep it distinguishable
        // from an absent file and bind an explicit reset to the observed file state.
        val identity = runCatching {
            val actual = File(file.path + ".bak").takeIf { it.exists() } ?: file
            val prefix = actual.inputStream().use { input ->
                val buffer = ByteArray(256 * 1024)
                var size = 0
                while (size < buffer.size) {
                    val count = input.read(buffer, size, buffer.size - size)
                    if (count < 0) break
                    size += count
                }
                buffer.copyOf(size)
            }
            "${actual.length()}:${actual.lastModified()}:" + digest(prefix)
        }.getOrDefault("unavailable")
        return InstallProfileSnapshot("unreadable:$identity", readable = false)
    }

    private fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}

/** Failure recovery owns one randomly revisioned document, never the entire preferences map. */
internal class InstallProfilePreferenceWriter {
    data class Field(val present: Boolean, val value: Any?)
    interface Storage {
        fun values(): Map<String, *>
        fun fileExists(): Boolean
        fun commit(field: Field): Boolean
        /** Called only for an originally absent, empty file after a confirmed rollback. */
        fun deleteEmptyFile(): Boolean
    }

    var failureRevision: String? = null
        private set
    val unreadableSnapshot: InstallProfileSnapshot?
        get() = failureRevision?.let { InstallProfileSnapshot("unreadable:save:$it", readable = false) }

    fun write(storage: Storage, encoded: String, wasUnconfigured: Boolean): Boolean {
        val priorFailure = failureRevision
        val before: Map<String, *>
        val original: Field
        val mayRemoveEmptyFile: Boolean
        try {
            before = storage.values().toMap()
            original = field(before)
            mayRemoveEmptyFile = wasUnconfigured && before.isEmpty() && !storage.fileExists()
        } catch (_: Exception) {
            uncertain()
            return false
        }
        val written = Field(true, encoded)
        val committed = runCatching { storage.commit(written) }.getOrDefault(false)
        val current = runCatching { field(storage.values()) }.getOrNull()
        if (committed && current == written) {
            failureRevision = null
            return true
        }
        uncertain()
        // An external replacement is never ours to undo, even if the preceding write failed.
        if (current != written) return false
        val rolledBack = runCatching { field(storage.values()) == written && storage.commit(original) }.getOrDefault(false)
        if (!rolledBack || runCatching { field(storage.values()) }.getOrNull() != original) return false
        if (mayRemoveEmptyFile) {
            val removed = runCatching { storage.values().isEmpty() && storage.deleteEmptyFile() &&
                storage.values().isEmpty() && !storage.fileExists() }.getOrDefault(false)
            if (!removed) return false
        }
        // Restoring a previously uncertain value cannot certify it. Only a later explicit
        // successful save clears that earlier failure; a proved rollback to an ordinary baseline can.
        failureRevision = priorFailure
        return false
    }

    private fun uncertain() { if (failureRevision == null) failureRevision = UUID.randomUUID().toString() }

    private fun field(values: Map<String, *>): Field {
        val value = values["document"]
        return Field(values.containsKey("document"), if (value is Set<*>) value.toSet() else value)
    }
}
