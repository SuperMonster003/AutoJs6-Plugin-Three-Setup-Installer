package io.github.supermonster003.autojs6.plugin.three.setup.installer.history

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** A private atomic JSON file. Its caller serializes IO on a dedicated executor. */
internal class InstallHistoryFile(private val file: File, private val beforeCommit: () -> Unit = {}) {
    private val pending get() = File(file.path + ".new")
    private val backup get() = File(file.path + ".bak")

    fun read(): List<InstallHistoryEntry> {
        if (backup.exists()) {
            if (file.exists() && !file.delete()) throw IOException("Cannot recover installation history")
            if (!backup.renameTo(file)) throw IOException("Cannot recover installation history backup")
        }
        pending.delete()
        if (!file.exists()) return emptyList()
        require(file.isFile && file.length() in 1..InstallHistoryCodec.MAX_BYTES.toLong())
        return InstallHistoryCodec.decode(file.readBytes())
    }

    fun write(entries: List<InstallHistoryEntry>, canCommit: () -> Boolean = { true }): Boolean {
        val bytes = InstallHistoryCodec.encode(entries)
        if (!canCommit()) return false
        val parent = requireNotNull(file.parentFile)
        if (!parent.isDirectory && !parent.mkdirs()) throw IOException("Cannot create private history directory")
        try {
            FileOutputStream(pending).use { output -> output.write(bytes); output.fd.sync() }
            beforeCommit()
            if (!canCommit()) { pending.delete(); return false }
            if (file.exists() && !file.renameTo(backup)) throw IOException("Cannot retain installation history backup")
            if (!pending.renameTo(file)) throw IOException("Cannot commit installation history")
            if (backup.exists() && !backup.delete()) throw IOException("Cannot finish installation history commit")
            return true
        } catch (failure: Exception) {
            pending.delete()
            if (backup.exists()) { file.delete(); backup.renameTo(file) }
            throw failure
        }
    }
}
