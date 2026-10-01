package io.github.supermonster003.autojs6.plugin.three.setup.installer.source

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.system.ErrnoException
import android.system.Os
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.StorageErrors
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Opens a source without copying when Android permits a seekable descriptor view (P2.1, D16).
 *
 * The shared parser reads through `ZipFile(File)`. A session-owned link to a held descriptor
 * preserves its display-name extension, without resolving or reopening the original source path.
 * Pipes are copied once. If Android disallows reopening an otherwise seekable descriptor through
 * procfs, a bounded positional copy is necessary for the File-only parser and is reported in the
 * returned view. Copies are cancellable and never close the caller's descriptor.
 *
 * zh-CN: 可随机访问的来源优先持有描述符视图, 管道或平台限制重开描述符时才暂存.
 */
internal object PackageStaging {

    const val MAX_SOURCE_BYTES = 4L * 1024L * 1024L * 1024L
    private const val MINIMUM_FREE_BYTES = 128L * 1024L * 1024L
    private const val BUFFER_BYTES = 256 * 1024
    private val directories = StagingDirectories(makeDirectory = ::createDirectory)

    fun open(
        context: Context,
        directory: File,
        source: PackageSource,
        cancelled: AtomicBoolean = AtomicBoolean(),
        progress: (Long) -> Unit = {},
        checkActive: () -> Unit = {},
    ): SeekableSource {
        val check = {
            if (cancelled.get() || Thread.currentThread().isInterrupted) throw InstallFailure(InstallerErrorCodes.CANCELLED, "Source opening cancelled")
            checkActive()
        }
        check()
        return when (source) {
            is PackageSource.Descriptor -> openDescriptor(directory, source.descriptor, source.displayName, source.declaredSize, null, cancelled, progress, check)
            is PackageSource.LocalFile -> {
                val file = source.file
                if (!file.isFile) throw InstallFailure(InstallerErrorCodes.SOURCE_NOT_FOUND, "Package source does not exist: ${file.name}")
                if (!file.canRead()) throw InstallFailure(InstallerErrorCodes.SOURCE_UNREADABLE, "Package source cannot be read: ${file.name}")
                try {
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                        openDescriptor(directory, descriptor, file.name, file.length(), file, cancelled, progress, check)
                    }
                } catch (failure: java.io.FileNotFoundException) {
                    throw InstallFailure(InstallerErrorCodes.SOURCE_UNREADABLE, "Package source cannot be opened: ${file.name}", cause = failure)
                }
            }
            is PackageSource.Content -> {
                val resolver = context.contentResolver
                var name: String? = null
                var size = -1L
                runCatching {
                    resolver.query(source.uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (nameIndex >= 0 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex)
                            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                        }
                    }
                }
                check()
                val descriptor = try {
                    resolver.openFileDescriptor(source.uri, "r")
                        ?: throw InstallFailure(InstallerErrorCodes.SOURCE_NOT_FOUND, "Package source cannot be opened")
                } catch (failure: SecurityException) {
                    throw InstallFailure(InstallerErrorCodes.SOURCE_UNREADABLE, "Package source is not readable", cause = failure)
                } catch (failure: java.io.FileNotFoundException) {
                    throw InstallFailure(InstallerErrorCodes.SOURCE_NOT_FOUND, "Package source was not found", cause = failure)
                }
                descriptor.use {
                    if (size < 0L) size = runCatching { it.statSize }.getOrDefault(-1L)
                    val displayName = name?.takeIf(String::isNotBlank) ?: source.uri.lastPathSegment ?: "package.apk"
                    openDescriptor(directory, it, displayName, size, null, cancelled, progress, check)
                }
            }
        }
    }

    private fun openDescriptor(
        directory: File,
        descriptor: ParcelFileDescriptor,
        displayName: String,
        declaredSize: Long,
        ownedFile: File?,
        cancelled: AtomicBoolean,
        progress: (Long) -> Unit,
        checkActive: () -> Unit,
    ): SeekableSource {
        validateSize(declaredSize)
        val owned = try { ParcelFileDescriptor.dup(descriptor.fileDescriptor) } catch (failure: IOException) {
            throw InstallFailure(InstallerErrorCodes.SOURCE_UNREADABLE, "Package source descriptor cannot be read", cause = failure)
        }
        val input = ParcelFileDescriptor.AutoCloseInputStream(owned)
        val target = uniqueTarget(directory, displayName)
        var linked = false
        try {
            checkActive()
            val channel = input.channel
            val seekable = runCatching { channel.position(); true }.getOrDefault(false)
            if (seekable) {
                val actualSize = channel.size()
                validateSize(actualSize)
                if (declaredSize >= 0 && actualSize != declaredSize) {
                    throw InstallFailure(InstallerErrorCodes.INVALID_PACKAGE, "Package source size differs from its declared size")
                }
                val reopened = runCatching {
                    Os.symlink("/proc/self/fd/${owned.fd}", target.absolutePath)
                    linked = true
                    target.inputStream().use { check(it.channel.size() == actualSize) }
                    true
                }.getOrDefault(false)
                if (reopened) {
                    return SeekableSource(target, displayName, actualSize, ownedFile, null) {
                        try { target.delete() } finally { input.close() }
                    }
                }
                if (linked) {
                    if (!target.delete()) throw InstallFailure(InstallerErrorCodes.SOURCE_UNREADABLE, "Cannot replace the descriptor view with staging")
                    linked = false
                }
            }
            val reason = if (seekable) SeekableSource.StagingReason.DESCRIPTOR_REOPEN_UNAVAILABLE else SeekableSource.StagingReason.NOT_SEEKABLE
            if (seekable) {
                // Positional reads start at zero without changing the host's shared open-file offset.
                copy(PositionedChannelInputStream(channel, checkActive), target, declaredSize, cancelled) { checkActive(); progress(it) }
            } else {
                CancellableDescriptorInputStream(owned, checkActive).use { pipe ->
                    copy(pipe, target, declaredSize, cancelled) { checkActive(); progress(it) }
                }
            }
            input.close()
            return SeekableSource(target, displayName, target.length(), ownedFile, reason)
        } catch (failure: Throwable) {
            if (linked) target.delete()
            runCatching { input.close() }
            throw failure
        }
    }

    private class PositionedChannelInputStream(private val channel: FileChannel, private val checkActive: () -> Unit) : InputStream() {
        private var offset = 0L
        override fun read(): Int = ByteArray(1).let { if (read(it, 0, 1) < 0) -1 else it[0].toInt() and 0xff }
        override fun read(bytes: ByteArray, start: Int, length: Int): Int {
            if (start < 0 || length < 0 || start > bytes.size - length) throw IndexOutOfBoundsException()
            if (length == 0) return 0
            checkActive()
            return channel.read(ByteBuffer.wrap(bytes, start, length), offset).also { if (it > 0) offset += it }
        }
    }

    private fun validateSize(size: Long) {
        if (size < -1L || size > MAX_SOURCE_BYTES) throw InstallFailure(InstallerErrorCodes.INVALID_ARGUMENT, "Package source size exceeds the supported range")
        if (size == 0L) throw InstallFailure(InstallerErrorCodes.INVALID_PACKAGE, "Package source is empty")
    }

    private fun uniqueTarget(directory: File, displayName: String): File {
        val name = safeName(displayName)
        val target = File(directory, name)
        return if (name !in directory.list().orEmpty()) target else File(directory, "${name.substringBeforeLast('.')}-${UUID.randomUUID()}.${name.substringAfterLast('.')}")
    }

    fun root(context: Context): File = File(context.cacheDir, "staging")

    /** A fresh directory for one session; the caller deletes it through [discard]. */
    fun newDirectory(context: Context, sessionId: String = UUID.randomUUID().toString()): File =
        directories.create(root(context), sessionId)

    fun newChildDirectory(parent: File, name: String): File = File(parent, name).also(::createDirectory)

    /** Keep errno when mkdir fails; File.mkdir() would discard ENOSPC and EDQUOT. */
    private fun createDirectory(directory: File) {
        try {
            Os.mkdir(directory.absolutePath, 448) // 0700, app-private staging only.
        } catch (failure: ErrnoException) {
            if (StorageErrors.isInsufficientStorage(failure)) throw StorageErrors.failure(failure)
            throw IOException("Unable to create the staging directory", failure)
        }
    }

    fun discard(directory: File?) {
        directories.discard(directory)
    }

    /** Deletes day-old residual directories, preserving this process's live source views. Worker only. */
    fun cleanStale(context: Context) = directories.cleanStale(root(context))

    /** Stages a host descriptor; [declaredSize] is the size the host reported and the copy must match it. */
    fun stageDescriptor(
        directory: File,
        descriptor: ParcelFileDescriptor,
        displayName: String,
        declaredSize: Long,
        cancelled: AtomicBoolean,
        progress: (Long) -> Unit = {},
        checkActive: () -> Unit = {},
    ): File {
        val target = File(directory, safeName(displayName))
        val check = {
            if (cancelled.get() || Thread.currentThread().isInterrupted) throw InstallFailure(InstallerErrorCodes.CANCELLED, "Staging cancelled")
            checkActive()
        }
        CancellableDescriptorInputStream(descriptor, check).use { input ->
            copy(input, target, declaredSize, cancelled) { copied -> check(); progress(copied) }
        }
        return target
    }

    /** Stages a `content://` or `file://` source of the plugin's own entry points; returns the file and the declared size. */
    fun stageUri(context: Context, directory: File, uri: Uri, cancelled: AtomicBoolean, progress: (Long) -> Unit = {}): StagedSource {
        PackageSource.fromUri(uri) // Reject unsupported schemes before querying any provider.
        if (uri.scheme.equals("file", ignoreCase = true)) {
            val file = File(uri.path ?: throw IOException("Package source has no path"))
            if (!file.isFile || !file.canRead()) throw InstallFailure(InstallerErrorCodes.SOURCE_UNREADABLE, "Package source cannot be read: ${file.name}")
            val target = File(directory, safeName(file.name))
            file.inputStream().use { input -> copy(input, target, file.length(), cancelled, progress) }
            return StagedSource(target, file.name, file.length(), ownedFile = file)
        }
        val resolver = context.contentResolver
        var name: String? = null
        var size = -1L
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                }
            }
        }
        val descriptor = try {
            resolver.openFileDescriptor(uri, "r") ?: throw InstallFailure(InstallerErrorCodes.SOURCE_NOT_FOUND, "Package source cannot be opened")
        } catch (failure: SecurityException) {
            throw InstallFailure(InstallerErrorCodes.SOURCE_UNREADABLE, "Package source is not readable: ${failure.message}")
        } catch (failure: java.io.FileNotFoundException) {
            throw InstallFailure(InstallerErrorCodes.SOURCE_NOT_FOUND, "Package source not found: ${failure.message}")
        }
        descriptor.use {
            if (size <= 0L) size = runCatching { it.statSize }.getOrDefault(-1L)
            val displayName = name?.takeIf(String::isNotBlank) ?: uri.lastPathSegment ?: "package.apk"
            val target = File(directory, safeName(displayName))
            CancellableDescriptorInputStream(it) {
                if (cancelled.get() || Thread.currentThread().isInterrupted) throw InstallFailure(InstallerErrorCodes.CANCELLED, "Staging cancelled")
            }.use { input -> copy(input, target, size, cancelled, progress) }
            return StagedSource(target, displayName, target.length(), ownedFile = null)
        }
    }

    data class StagedSource(val file: File, val displayName: String, val size: Long, val ownedFile: File?)

    private fun copy(input: InputStream, target: File, declaredSize: Long, cancelled: AtomicBoolean, progress: (Long) -> Unit) {
        if (declaredSize > MAX_SOURCE_BYTES) throw InstallFailure(InstallerErrorCodes.INVALID_ARGUMENT, "Package source exceeds ${MAX_SOURCE_BYTES / (1024 * 1024)} MiB")
        val usable = target.parentFile?.usableSpace ?: 0L
        if (declaredSize > 0L && usable > 0L && declaredSize > usable - MINIMUM_FREE_BYTES) {
            throw InstallFailure(InstallerErrorCodes.INSUFFICIENT_STORAGE, "Not enough cache space to stage the package")
        }
        try {
            target.outputStream().buffered(BUFFER_BYTES).use { output ->
                val buffer = ByteArray(BUFFER_BYTES)
                var copied = 0L
                while (true) {
                    if (cancelled.get() || Thread.currentThread().isInterrupted) throw InstallFailure(InstallerErrorCodes.CANCELLED, "Staging cancelled")
                    val count = input.read(buffer)
                    if (count < 0) break
                    copied += count
                    if (copied > MAX_SOURCE_BYTES || (declaredSize >= 0L && copied > declaredSize)) {
                        throw InstallFailure(InstallerErrorCodes.INVALID_PACKAGE, "Package source is larger than declared")
                    }
                    output.write(buffer, 0, count)
                    progress(copied)
                }
                if (declaredSize >= 0L && copied != declaredSize) {
                    throw InstallFailure(InstallerErrorCodes.INVALID_PACKAGE, "Package source is shorter than declared")
                }
                if (copied == 0L) throw InstallFailure(InstallerErrorCodes.INVALID_PACKAGE, "Package source is empty")
            }
        } catch (failure: IOException) {
            target.delete()
            if (StorageErrors.isInsufficientStorage(failure)) throw StorageErrors.failure(failure)
            throw InstallFailure(InstallerErrorCodes.SOURCE_UNREADABLE, "Cannot read the package source: ${failure.message}", cause = failure)
        } catch (failure: Throwable) {
            target.delete()
            throw failure
        }
    }

    /** Display names come from untrusted sources; only the extension is kept for the parser's format detection. */
    fun safeName(displayName: String): String {
        val extension = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT).takeIf { it.matches(Regex("[a-z0-9]{1,8}")) } ?: "bin"
        val stem = displayName.substringBeforeLast('.').replace(Regex("[^A-Za-z0-9._-]"), "_").trim('.', '_').take(48).ifEmpty { "package" }
        return "$stem.$extension"
    }
}
