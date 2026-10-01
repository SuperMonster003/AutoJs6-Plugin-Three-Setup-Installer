package io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden

import android.os.Build
import android.os.Bundle
import android.os.Process
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.PrivilegedOptions
import java.io.Closeable
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

/** Bounded optional work. It never retries an install/compile or reports an install as undone. */
internal class PackagePostInstall(private val packages: PackageManagerHidden) : Closeable {
    private val active = linkedSetOf<Task>()
    @Volatile private var closed = false

    fun run(packageName: String, userId: Int, dexopt: String, readUpdateOwner: Boolean, timeoutMillis: Long): Bundle {
        PrivilegedOptions.validatePackage(packageName)
        require(userId >= 0)
        require(timeoutMillis in 0..PrivilegedOptions.MAX_POST_INSTALL_TIMEOUT)
        PrivilegedOptions.validateDexopt(dexopt, Build.VERSION.SDK_INT)
        val task = Task(packageName, userId, dexopt, readUpdateOwner)
        if (dexopt == "none" && !readUpdateOwner) return task.bundle(notRequested())
        if (timeoutMillis == 0L) return task.incomplete("timeout", "No time remained for optional post-install work")
        if (Process.myUid() !in setOf(0, 2000)) return task.incomplete("unavailable", "Post-install work requires the authorized Root or Shizuku process")
        val started = System.nanoTime()
        task.work = FutureTask {
            if (task.cancelled.get() || closed) return@FutureTask task.incomplete("cancelled", "Post-install work was cancelled before it started")
            if (packages.packageInfo(packageName, userId) == null) {
                return@FutureTask task.incomplete("unavailable", "The installed package is no longer available for the requested user")
            }
            if (readUpdateOwner) {
                task.owner = if (Build.VERSION.SDK_INT < 34) Owner(false, null, "Update ownership metadata requires Android 14 or newer")
                else try {
                    Owner(true, packages.updateOwner(packageName, userId), "")
                } catch (failure: Exception) {
                    Owner(false, null, "Update ownership could not be read: ${failure.message.orEmpty().take(200)}")
                }
            }
            if (dexopt == "none") return@FutureTask task.bundle(notRequested())
            val remaining = (timeoutMillis - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)).coerceAtLeast(0)
            task.bundle(PostInstallCommand().run(packageName, dexopt, Build.VERSION.SDK_INT, remaining) { task.cancelled.get() || closed })
        }
        val worker = Thread({
            try { task.work.run() }
            finally {
                synchronized(active) { active.remove(task) }
                task.finished.countDown()
            }
        }, "installer-post-install").apply { isDaemon = true }
        synchronized(active) {
            if (closed) return task.incomplete("cancelled", "The privileged client has closed")
            if (active.size >= PrivilegedOptions.MAX_SESSIONS) return task.incomplete("unavailable", "The post-install worker limit has been reached")
            active += task
            try { worker.start() } catch (failure: RuntimeException) {
                active.remove(task)
                return task.incomplete("unavailable", "The post-install worker could not start")
            }
        }
        return try {
            task.work.get(timeoutMillis, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            task.cancelAndWait()
            task.incomplete("timeout", "Optional post-install work timed out; its final framework outcome may be unverified")
        } catch (_: CancellationException) {
            task.cancelAndWait()
            task.incomplete("cancelled", "Optional post-install work was cancelled; its final framework outcome may be unverified")
        } catch (_: InterruptedException) {
            task.cancelAndWait()
            Thread.currentThread().interrupt()
            task.incomplete("cancelled", "Optional post-install work was interrupted; its final framework outcome may be unverified")
        } catch (failure: ExecutionException) {
            task.incomplete(if (failure.cause is SecurityException) "unavailable" else "unknown",
                "Optional post-install result could not be read: ${failure.cause?.message.orEmpty().take(200)}")
        }
    }

    override fun close() {
        val tasks = synchronized(active) { closed = true; active.toList() }
        // Each worker owns its one compile process and its optional, uniquely identified ART job.
        // Interrupting a metadata Binder call may not stop the framework; no such call is replayed.
        tasks.forEach(Task::cancel)
    }

    private data class Owner(val read: Boolean, val packageName: String?, val message: String)
    private class Task(val packageName: String, val userId: Int, val dexopt: String, val readOwner: Boolean) {
        val cancelled = AtomicBoolean()
        val finished = CountDownLatch(1)
        lateinit var work: FutureTask<Bundle>
        @Volatile var owner = Owner(false, null, "")
        fun cancel() { cancelled.set(true); work.cancel(true) }
        fun cancelAndWait() {
            cancel()
            val interrupted = Thread.interrupted()
            try { finished.await(1_500, TimeUnit.MILLISECONDS) }
            catch (_: InterruptedException) { Thread.currentThread().interrupt() }
            finally { if (interrupted) Thread.currentThread().interrupt() }
        }
        fun incomplete(status: String, message: String): Bundle {
            if (readOwner && !owner.read && owner.message.isEmpty()) owner = Owner(false, null, message)
            return bundle(if (dexopt == "none") notRequested() else PostInstallCommand.Result(status, message))
        }
        fun bundle(result: PostInstallCommand.Result): Bundle {
            val observed = owner
            return Bundle().apply {
                putString("packageName", packageName); putInt("userId", userId)
                putString("dexoptFilter", dexopt); putString("dexoptStatus", result.status)
                putString("dexoptMessage", result.message)
                result.exitCode?.let { putInt("dexoptExitCode", it) }
                putBoolean("updateOwnerRead", observed.read)
                putString("updateOwnerPackageName", observed.packageName)
                putString("updateOwnerMessage", observed.message)
                putStringArrayList("notes", ArrayList<String>().apply {
                    if (readOwner && !observed.read && observed.message.isNotEmpty()) add(observed.message)
                    if (result.status !in setOf("accepted", "not-requested")) add(result.message)
                })
            }
        }
    }
    companion object {
        private fun notRequested() = PostInstallCommand.Result("not-requested", "Compilation was not requested")
    }
}
