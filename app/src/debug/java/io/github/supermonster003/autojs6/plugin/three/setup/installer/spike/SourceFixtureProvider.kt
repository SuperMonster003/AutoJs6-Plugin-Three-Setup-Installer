package io.github.supermonster003.autojs6.plugin.three.setup.installer.spike

import android.content.ContentProvider
import android.content.ContentValues
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.CancellationSignal
import android.os.Bundle
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Non-exported debug-only provider for real ContentResolver and pipe source tests. */
class SourceFixtureProvider : ContentProvider() {
    override fun onCreate() = true
    override fun getType(uri: Uri) = "application/vnd.android.package-archive"

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method !in setOf("fixtureCancellationState", "fixtureDeletionState")) return super.call(method, arg, extras)
        val uri = Uri.parse(arg ?: throw IllegalArgumentException("A fixture URI is required"))
        file(uri)
        return Bundle().apply {
            if (method == "fixtureCancellationState") putBoolean("waiting", uri.toString() in waitingForCancellation)
            else {
                putString("fixtureUri", uri.toString())
                putInt("deleteAttempts", deleteAttempts[uri.toString()]?.get() ?: 0)
            }
        }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val source = file(uri)
        val columns = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        return MatrixCursor(columns).apply {
            addRow(Array<Any?>(columns.size) { index -> when (columns[index]) {
                OpenableColumns.DISPLAY_NAME -> source.name
                OpenableColumns.SIZE -> if (uri.getQueryParameter("pipe") == "1") null else source.length()
                else -> null
            } })
        }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?,
        sortOrder: String?, cancellationSignal: CancellationSignal?): Cursor {
        if (uri.getQueryParameter("waitForCancel") == "query") waitForCancellation(uri, cancellationSignal)
        return query(uri, projection, selection, selectionArgs, sortOrder)
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("Read-only fixture provider")
        val source = file(uri)
        // Deliberately hostile, private fixture: a provider need not honor its requested mode.
        // SecurityBoundaryDeviceTest verifies the consumer rejects and closes this handle.
        if (uri.getQueryParameter("writable") == "1") return ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_WRITE)
        if (uri.getQueryParameter("pipe") != "1") return ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY)
        val pipe = ParcelFileDescriptor.createPipe()
        Thread({
            runCatching { ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { output -> source.inputStream().use { it.copyTo(output) } } }
        }, "source-fixture-writer").apply { isDaemon = true; start() }
        return pipe[0]
    }

    override fun openFile(uri: Uri, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
        if (uri.getQueryParameter("waitForCancel") == "open") waitForCancellation(uri, signal)
        return openFile(uri, mode)
    }

    // ContentResolver.openFileDescriptor uses the asset path for content URIs. The framework's
    // default asset implementation calls the two-argument openFile and drops cancellation.
    override fun openAssetFile(uri: Uri, mode: String, signal: CancellationSignal?): AssetFileDescriptor =
        AssetFileDescriptor(openFile(uri, mode, signal), 0, AssetFileDescriptor.UNKNOWN_LENGTH)

    override fun openTypedAssetFile(uri: Uri, mimeTypeFilter: String, opts: Bundle?, signal: CancellationSignal?): AssetFileDescriptor {
        if (mimeTypeFilter != "*/*" && mimeTypeFilter != getType(uri)) throw FileNotFoundException("Unsupported fixture type")
        // Read-only content opens may use this MIME-aware path first. Its framework default also
        // falls back to the two-argument asset method, so explicitly preserve the test signal.
        return openAssetFile(uri, "r", signal)
    }

    private fun waitForCancellation(uri: Uri, signal: CancellationSignal?) {
        // Validate the private fixture before entering the artificial stall. The timeout keeps
        // a broken test from leaving a provider call blocked indefinitely.
        file(uri)
        val cancellation = signal ?: throw FileNotFoundException("A fixture cancellation signal is required")
        val cancelled = CountDownLatch(1)
        cancellation.setOnCancelListener { cancelled.countDown() }
        waitingForCancellation += uri.toString()
        try {
            if (!cancelled.await(15, TimeUnit.SECONDS)) throw FileNotFoundException("Fixture cancellation was not delivered")
            cancellation.throwIfCanceled()
            throw FileNotFoundException("Fixture cancellation did not cancel the call")
        } finally {
            waitingForCancellation -= uri.toString()
            cancellation.setOnCancelListener(null)
        }
    }

    private fun file(uri: Uri): File {
        val parts = uri.pathSegments
        if (parts.size != 2 || !parts[0].matches(Regex("p2-source-fixtures-[0-9a-f-]+")) || parts[1] != "fixture.apk") {
            throw FileNotFoundException("Invalid fixture path")
        }
        val source = File(File(requireNotNull(context).cacheDir, parts[0]), parts[1])
        if (!source.isFile) throw FileNotFoundException("Fixture is unavailable")
        return source
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException("Read-only")
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException("Read-only")
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        file(uri)
        // Observe the real ContentResolver route while retaining the provider's read-only contract.
        // Each test uses a new private directory and this count is never persisted.
        deleteAttempts.computeIfAbsent(uri.toString()) { AtomicInteger() }.incrementAndGet()
        throw UnsupportedOperationException("Read-only")
    }

    companion object {
        private val waitingForCancellation: MutableSet<String> = ConcurrentHashMap.newKeySet()
        private val deleteAttempts = ConcurrentHashMap<String, AtomicInteger>()
    }
}
