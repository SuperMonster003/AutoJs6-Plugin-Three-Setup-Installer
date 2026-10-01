package io.github.supermonster003.autojs6.plugin.three.setup.installer.binder

import android.os.SystemClock
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import org.autojs.plugin.installer.api.IInstallerCallback
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.io.Closeable
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Bounded asynchronous work. Queued cancellations still run their cleanup, never Future.cancel(). */
internal class InstallerWorkQueue : Closeable {
    val executor = ThreadPoolExecutor(InstallerContract.MAX_CONCURRENT_SESSIONS, InstallerContract.MAX_CONCURRENT_SESSIONS,
        0, TimeUnit.MILLISECONDS, ArrayBlockingQueue(16))
    private val operations = ConcurrentHashMap.newKeySet<Operation>()
    private inner class Operation(val timeout: Long, val resource: Closeable?, val callback: IInstallerCallback) {
        val lock = Any()
        var worker: Thread? = null
        var cancelled = false
        val deadline = SystemClock.elapsedRealtime() + timeout
        val death = CallbackDeath(callback.asBinder(), ::cancel)
        fun cancel() = synchronized(lock) { cancelled = true; worker?.interrupt(); Unit }
        fun check() {
            if (synchronized(lock) { cancelled } || Thread.currentThread().isInterrupted) throw InstallFailure(InstallerErrorCodes.CANCELLED, "Installer operation cancelled")
            if (SystemClock.elapsedRealtime() >= deadline) throw InstallFailure(InstallerErrorCodes.TIMEOUT, "Installer operation timed out")
        }
    }
    fun submit(callback: IInstallerCallback, timeout: Long = 60_000, resource: Closeable? = null, action: (Long, () -> Unit) -> JsonObject) {
        val operation = Operation(timeout, resource, callback)
        operations += operation
        operation.death.link()
        val work = Runnable {
            synchronized(operation.lock) { operation.worker = Thread.currentThread() }
            var result: android.os.Bundle? = null
            var error: InstallFailure? = null
            try {
                operation.check()
                result = InstallerBundles.document(InstallerContract.KEY_RESULT_JSON, action(operation.deadline, operation::check))
            } catch (failure: Exception) { error = InstallFailure.from(failure) }
            finally {
                runCatching { resource?.close() }
                operation.death.close()
                synchronized(operation.lock) { operation.worker = null; Thread.interrupted() }
                operations -= operation
            }
            runCatching { if (error == null) callback.onResult(result) else callback.onError(InstallerBundles.error(error)) }
        }
        try { executor.execute(work) } catch (_: RejectedExecutionException) {
            operations -= operation
            operation.death.close()
            runCatching { resource?.close() }
            reject(callback, InstallFailure(InstallerErrorCodes.INVALID_ARGUMENT, "Installer worker capacity exceeded or service closed"))
        }
    }
    fun reject(callback: IInstallerCallback, failure: InstallFailure) { runCatching { callback.onError(InstallerBundles.error(failure)) } }
    override fun close() {
        operations.forEach { it.cancel() }
        executor.shutdown()
    }
}
