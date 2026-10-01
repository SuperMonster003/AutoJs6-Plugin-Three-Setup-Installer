package io.github.supermonster003.autojs6.plugin.three.setup.installer.source

import java.io.File
import java.io.IOException
import java.util.UUID

/** Owns this process's directories and never reclaims a live request during a later cleanup. */
internal class StagingDirectories(
    private val makeDirectory: (File) -> Unit = { file ->
        if (!file.isDirectory && !file.mkdirs()) throw IOException("Unable to create the staging directory")
    },
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val active = mutableSetOf<File>()
    private val cleaned = mutableSetOf<File>()

    @Synchronized
    fun create(root: File, sessionId: String): File {
        val parent = root.canonicalFile
        if (!parent.isDirectory) makeDirectory(parent)
        // Also covers cold starts through external VIEW/SEND and the standalone file picker.
        if (cleaned.add(parent)) cleanStale(parent)
        val safe = sessionId.replace(Regex("[^A-Za-z0-9._-]"), "_").trim('.').take(64).ifEmpty { "session" }
        val directory = File(parent, "$safe-${UUID.randomUUID()}")
        makeDirectory(directory)
        active += directory
        return directory
    }

    @Synchronized
    fun discard(directory: File?) {
        if (directory != null && active.remove(directory.absoluteFile)) deleteTree(directory)
    }

    @Synchronized
    fun cleanStale(root: File) {
        val parent = root.canonicalFile
        val expiredBefore = clock() - STALE_AFTER_MILLIS
        parent.listFiles()?.forEach { directory ->
            if (directory !in active && directory.isDirectory && directory.lastModified() < expiredBefore) {
                runCatching { deleteTree(directory) }
            }
        }
        cleaned += parent
    }

    /** Descriptor links and unexpected links are removed themselves, never followed recursively. */
    private fun deleteTree(file: File): Boolean {
        if (file.canonicalFile != file.absoluteFile) return file.delete()
        var complete = true
        if (file.isDirectory) {
            val children = file.listFiles() ?: return false
            children.forEach { if (!deleteTree(it)) complete = false }
        }
        return file.delete() && complete
    }

    companion object {
        const val STALE_AFTER_MILLIS = 24L * 60L * 60L * 1000L
    }
}
