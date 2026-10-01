package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.File
import java.util.concurrent.Executors

/** One IO queue per process. No snapshot or file IO is performed by a byte-progress callback. */
internal object InstallRecoveryPersistence {
    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor { Thread(it, "installation-recovery").apply { isDaemon = true } }
    @Volatile private var instance: InstallRecoveryWriter? = null

    @Synchronized fun writer(context: Context): InstallRecoveryWriter {
        instance?.let { return it }
        val application = context.applicationContext
        val store by lazy { InstallRecoveryStore(File(application.noBackupFilesDir, "installation-ui"), System::currentTimeMillis) }
        return InstallRecoveryWriter({ store }, io).also { instance = it }
    }
    fun load(context: Context, token: String, result: (InstallRecoverySnapshot?) -> Unit) {
        writer(context).read(token) { value -> main.post { result(value) } }
    }
    fun remove(context: Context, token: String, completed: () -> Unit = {}) {
        writer(context).remove(token) { main.post(completed) }
    }
    fun clear(context: Context, completed: () -> Unit = {}) { writer(context).clear { main.post(completed) } }
    fun mainThread() = Looper.myLooper() == Looper.getMainLooper()
}
