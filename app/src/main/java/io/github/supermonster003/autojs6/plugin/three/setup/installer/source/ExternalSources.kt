package io.github.supermonster003.autojs6.plugin.three.setup.installer.source

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.CancellationSignal
import android.os.OperationCanceledException
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import io.github.supermonster003.autojs6.plugin.three.setup.installer.binder.SourceDescriptors
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.*
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.io.Closeable
import java.io.File
import java.util.Locale

/** Parses only package URIs; no caller-supplied options, components or nested intents are executed. */
internal class ExternalSources private constructor(val uris: List<Uri>, val grantIntent: Intent) {
    data class Opened(val entries: List<SourceEntry>, val descriptors: List<ParcelFileDescriptor>) : Closeable {
        override fun close() = descriptors.forEach { runCatching { it.close() } }
    }
    fun provisionalEntries(): List<SourceEntry> = uris.mapIndexed { index, uri ->
        SourceEntry(index, index, name(uri), -1)
    }
    fun open(context: Context, checkActive: () -> Unit): Opened = open(context, CancellationSignal(), checkActive)

    fun open(context: Context, cancellation: CancellationSignal, checkActive: () -> Unit): Opened {
        val descriptors = mutableListOf<ParcelFileDescriptor>()
        val entries = mutableListOf<SourceEntry>()
        try {
            uris.indices.forEach { index ->
                val opened = openItem(context, index, cancellation, checkActive)
                descriptors += opened.descriptor
                entries += SourceEntry(index, index, opened.displayName, opened.size)
            }
            return Opened(entries, descriptors)
        } catch (failure: Throwable) {
            descriptors.forEach { runCatching { it.close() } }
            throw failure
        }
    }

    /** Read failures belong to this item; callers may continue with other batch sources. */
    fun openItem(context: Context, index: Int, cancellation: CancellationSignal, checkActive: () -> Unit): DescriptorInstallEnvironment.SourceDescriptor {
        checkActive()
        if (cancellation.isCanceled) throw InstallFailure(InstallerErrorCodes.CANCELLED, "Source opening cancelled")
        val uri = uris[index]
        var displayName = name(uri)
        var size = -1L
        if (uri.scheme.equals("content", true)) {
            runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null, cancellation)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val n = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val s = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (n >= 0 && !cursor.isNull(n)) displayName = cursor.getString(n).take(InstallerContract.MAX_DISPLAY_NAME_LENGTH).ifBlank { displayName }
                        if (s >= 0 && !cursor.isNull(s)) size = cursor.getLong(s)
                    }
                }
            }
        }
        checkActive()
        val descriptor = try {
            if (uri.scheme.equals("content", true)) context.contentResolver.openFileDescriptor(uri, "r", cancellation)
            else ParcelFileDescriptor.open(File(requireNotNull(uri.path)), ParcelFileDescriptor.MODE_READ_ONLY)
        } catch (failure: Exception) {
            // The owner's deadline distinguishes timeout from explicit user cancellation.
            checkActive()
            if (failure is OperationCanceledException) throw InstallFailure(InstallerErrorCodes.CANCELLED, "Source opening cancelled", cause = failure)
            throw InstallFailure(InstallerErrorCodes.SOURCE_UNREADABLE, "Package source cannot be read", systemMessage = failure.message, cause = failure)
        } ?: throw InstallFailure(InstallerErrorCodes.SOURCE_UNREADABLE, "Package source cannot be opened")
        try {
            // A provider can ignore the requested "r" mode. Apply the same actual descriptor
            // boundary as the host Binder before retaining or reading the returned handle.
            SourceDescriptors.validate(descriptor)
            checkActive()
            if (size < 0) size = descriptor.statSize
            if (size > PackageStaging.MAX_SOURCE_BYTES) throw RequestDocuments.invalid("Package source is too large")
            return DescriptorInstallEnvironment.SourceDescriptor(descriptor, displayName, size.coerceAtLeast(-1))
        } catch (failure: Throwable) {
            runCatching { descriptor.close() }
            throw failure
        }
    }

    fun deleteInstalled(context: Context, index: Int, options: InstallOptions): InstallSession.SourceCleanup {
        if (!options.deleteSource) return InstallSession.SourceCleanup()
        val uri = uris[index]
        val key = sourceKey(uri)
        if (uris.count { sourceKey(it) == key } > 1) {
            // Later items have not opened their URI or chosen their final options yet.
            // Conservatively retain a shared source instead of deleting their input early.
            return InstallSession.SourceCleanup(notes = listOf("Installation succeeded; a source shared with another batch item was retained"))
        }
        val deleted = runCatching {
            if (uri.scheme.equals("content", true)) {
                if (DocumentsContract.isDocumentUri(context, uri)) DocumentsContract.deleteDocument(context.contentResolver, uri)
                else context.contentResolver.delete(uri, null, null) > 0
            } else File(requireNotNull(uri.path)).delete()
        }.getOrDefault(false)
        return InstallSession.SourceCleanup(deleted, if (deleted) emptyList() else listOf("Installation succeeded; the source provider did not allow source deletion"))
    }

    fun single(index: Int): ExternalSources = fromUris(listOf(uris[index]), grantIntent.flags)

    private fun sourceKey(uri: Uri): String = if (uri.scheme.equals("file", true))
        runCatching { "file:" + File(requireNotNull(uri.path)).canonicalPath }.getOrDefault(uri.normalizeScheme().toString())
        else uri.normalizeScheme().toString()

    companion object {
        fun fromIntent(intent: Intent): ExternalSources {
            @Suppress("DEPRECATION")
            val values = when (intent.action) {
                Intent.ACTION_VIEW, Intent.ACTION_INSTALL_PACKAGE -> listOfNotNull(intent.data)
                Intent.ACTION_SEND -> listOfNotNull(if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    else intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
                Intent.ACTION_SEND_MULTIPLE -> if (Build.VERSION.SDK_INT >= 33) intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
                    else intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
                else -> throw RequestDocuments.invalid("Unsupported package-opening action")
            }
            return fromUris(values, intent.flags)
        }

        /** Also used by the local SAF picker; callers supply only the returned grant flags. */
        fun fromUris(uris: List<Uri>, flags: Int): ExternalSources {
            if (uris.size !in 1..InstallerContract.MAX_BATCH_SOURCES) throw RequestDocuments.invalid("Invalid package source count")
            if (uris.any { it.scheme?.lowercase(Locale.ROOT) !in setOf("content", "file") || (it.scheme.equals("file", true) && it.path.isNullOrBlank()) }) {
                throw RequestDocuments.invalid("Only content and readable file package sources are accepted")
            }
            val clip = ClipData.newRawUri("Package sources", uris.first())
            uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
            return ExternalSources(uris.toList(), Intent().apply {
                clipData = clip
                addFlags(flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
            })
        }
        private fun name(uri: Uri) = uri.lastPathSegment?.take(InstallerContract.MAX_DISPLAY_NAME_LENGTH)?.ifBlank { null } ?: "package.apk"
    }
}
