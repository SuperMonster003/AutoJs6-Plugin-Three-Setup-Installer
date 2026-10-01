package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** Private bounded files. All calls run on the recovery IO executor, never on the UI thread. */
internal class InstallRecoveryStore(
    private val directory: File,
    private val clock: () -> Long,
    private val maximum: Int = InstallRecoverySnapshot.MAX_ENTRIES,
    private val beforeCommit: () -> Unit = {},
) {
    init { require(maximum in 1..InstallRecoverySnapshot.MAX_ENTRIES) }

    @Synchronized fun read(token: String): InstallRecoverySnapshot? {
        if (!InstallRecoverySnapshot.validToken(token)) return null
        prune()
        return readValid(token)
    }

    @Synchronized fun write(snapshot: InstallRecoverySnapshot, canCommit: () -> Boolean = { true }): Boolean {
        val bytes = InstallRecoveryCodec.encode(snapshot)
        if (!InstallRecoverySnapshot.validAt(snapshot, clock()) || !canCommit()) return false
        ensureDirectory()
        val existing = prune().toMutableList()
        val old = existing.firstOrNull { it.token == snapshot.token }
        if (old != null && old.revision > snapshot.revision) return false
        if (old == null) while (existing.size >= maximum) {
            val victim = existing.filter { it.terminal }.minByOrNull { it.savedAt } ?: existing.minBy { it.savedAt }
            delete(victim.token)
            existing.remove(victim)
        }
        val target = file(snapshot.token)
        val pending = File(target.path + ".new")
        val backup = File(target.path + ".bak")
        try {
            FileOutputStream(pending).use { output -> output.write(bytes); output.fd.sync() }
            beforeCommit()
            if (!canCommit()) { pending.delete(); return false }
            if (target.exists() && !target.renameTo(backup)) throw IOException("Cannot retain previous recovery snapshot")
            if (!pending.renameTo(target)) throw IOException("Cannot commit recovery snapshot")
            if (backup.exists() && !backup.delete()) throw IOException("Cannot finish recovery snapshot commit")
            return true
        } catch (failure: Exception) {
            pending.delete()
            if (backup.exists()) { target.delete(); backup.renameTo(target) }
            throw failure
        }
    }

    @Synchronized fun delete(token: String) {
        if (!InstallRecoverySnapshot.validToken(token)) return
        val target = file(token)
        // Remove the recovery alternatives first: a later reader must not restore a backup after
        // an acknowledged deletion of the main file.
        listOf(File(target.path + ".new"), File(target.path + ".bak"), target).forEach {
            if (it.exists() && !it.delete()) throw IOException("Cannot delete recovery snapshot")
        }
    }

    @Synchronized fun clear() { tokens().forEach(::delete) }

    private fun ensureDirectory() {
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create private recovery directory")
    }
    private fun file(token: String): File {
        require(InstallRecoverySnapshot.validToken(token))
        return File(directory, "$token.json")
    }
    private fun tokens(): Set<String> = directory.listFiles().orEmpty().mapNotNull { child ->
        val name = child.name.removeSuffix(".new").removeSuffix(".bak").removeSuffix(".json")
        name.takeIf { InstallRecoverySnapshot.validToken(it) && child.name in setOf("$it.json", "$it.json.new", "$it.json.bak") }
    }.toSet()

    private fun readValid(token: String): InstallRecoverySnapshot? {
        val target = file(token)
        val backup = File(target.path + ".bak")
        val pending = File(target.path + ".new")
        return try {
            if (backup.exists()) {
                if (target.exists() && !target.delete()) throw IOException("Cannot recover snapshot")
                if (!backup.renameTo(target)) throw IOException("Cannot recover snapshot backup")
            }
            pending.delete()
            if (!target.isFile || target.length() !in 1..InstallRecoverySnapshot.MAX_BYTES.toLong()) {
                delete(token); return null
            }
            InstallRecoveryCodec.decode(target.readBytes()).takeIf {
                it.token == token && InstallRecoverySnapshot.validAt(it, clock())
            } ?: run { delete(token); null }
        } catch (_: Exception) { runCatching { delete(token) }; null }
    }

    private fun prune(): List<InstallRecoverySnapshot> {
        val values = tokens().mapNotNull(::readValid).sortedByDescending { it.savedAt }
        values.drop(maximum).forEach { delete(it.token) }
        return values.take(maximum)
    }
}
