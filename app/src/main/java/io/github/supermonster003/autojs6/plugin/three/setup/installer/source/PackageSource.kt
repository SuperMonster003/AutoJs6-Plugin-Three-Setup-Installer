package io.github.supermonster003.autojs6.plugin.three.setup.installer.source

import android.net.Uri
import android.os.ParcelFileDescriptor
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.io.Closeable
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** Host descriptors are borrowed. URI and file sources belong only to the plugin's own entry points. */
internal sealed class PackageSource {
    data class Descriptor(val descriptor: ParcelFileDescriptor, val displayName: String, val declaredSize: Long) : PackageSource()
    data class Content(val uri: Uri) : PackageSource() {
        init { require(uri.scheme.equals("content", ignoreCase = true)) }
    }
    data class LocalFile(val file: File) : PackageSource()

    companion object {
        fun fromUri(uri: Uri): PackageSource = when {
            uri.scheme.equals("content", ignoreCase = true) -> Content(uri)
            uri.scheme.equals("file", ignoreCase = true) || uri.scheme.isNullOrEmpty() -> LocalFile(
                File(uri.path?.takeIf { it.isNotBlank() }
                    ?: throw InstallFailure(InstallerErrorCodes.INVALID_ARGUMENT, "Package source has no path")),
            )
            else -> throw InstallFailure(InstallerErrorCodes.INVALID_ARGUMENT, "Unsupported package source scheme: ${uri.scheme}")
        }
    }
}

/**
 * A random-access view retained until inspection or installation ends. A descriptor-backed view
 * owns a dup and a private symbolic link; closing it unlinks only the view, never the source.
 */
internal class SeekableSource(
    val file: File,
    val displayName: String,
    val size: Long,
    val ownedFile: File?,
    val stagingReason: StagingReason?,
    private val release: () -> Unit = {},
) : Closeable {
    enum class StagingReason { NOT_SEEKABLE, DESCRIPTOR_REOPEN_UNAVAILABLE }

    val staged: Boolean get() = stagingReason != null
    private val closed = AtomicBoolean()

    override fun close() {
        if (closed.compareAndSet(false, true)) release()
    }
}
