package io.github.supermonster003.autojs6.plugin.three.setup.installer.spike

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

/** Non-exported debug-only provider for real ContentResolver and pipe source tests. */
class SourceFixtureProvider : ContentProvider() {
    override fun onCreate() = true
    override fun getType(uri: Uri) = "application/vnd.android.package-archive"

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

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("Read-only fixture provider")
        val source = file(uri)
        if (uri.getQueryParameter("pipe") != "1") return ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY)
        val pipe = ParcelFileDescriptor.createPipe()
        Thread({
            runCatching { ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { output -> source.inputStream().use { it.copyTo(output) } } }
        }, "source-fixture-writer").apply { isDaemon = true; start() }
        return pipe[0]
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
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException("Read-only")
}
