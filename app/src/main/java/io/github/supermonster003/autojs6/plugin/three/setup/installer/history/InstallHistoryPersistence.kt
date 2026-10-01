package io.github.supermonster003.autojs6.plugin.three.setup.installer.history

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors

internal object InstallHistoryPersistence {
    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor { Thread(it, "installation-history").apply { isDaemon = true } }
    @Volatile private var instance: InstallHistoryStore? = null

    @Synchronized fun get(context: Context): InstallHistoryStore {
        instance?.let { return it }
        // Only the application path is retained, never the Activity or its grants.
        val file = File(context.applicationContext.noBackupFilesDir, "installation-history/history.json")
        return InstallHistoryStore(InstallHistoryFile(file), io, Executor { main.post(it) }).also { instance = it }
    }
}
