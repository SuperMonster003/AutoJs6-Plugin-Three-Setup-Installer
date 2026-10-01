package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.io.Closeable
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * One gate per package across all installation entry points and authorizers in this process.
 * APK code is shared between Android users, so neither userId nor authorizer belongs in the key.
 * Only one item is held at a time; batch requests can never deadlock by taking several gates.
 * Waiting still consumes the caller's existing InstallSlots lease and original session deadline.
 */
internal object PackageInstallLocks {
    private class Entry {
        val permit = Semaphore(1, true)
        var users = 0
    }

    private val entries = mutableMapOf<String, Entry>()

    fun acquire(packageName: String?, checkActive: () -> Unit): Closeable {
        if (packageName.isNullOrBlank()) {
            throw InstallFailure(InstallerErrorCodes.INVALID_PACKAGE, "The package has no package name")
        }
        checkActive()
        val entry = synchronized(entries) { entries.getOrPut(packageName) { Entry() }.also { it.users++ } }
        var acquired = false
        try {
            do {
                checkActive()
                acquired = entry.permit.tryAcquire(100, TimeUnit.MILLISECONDS)
            } while (!acquired)
            // Cancellation may race with the previous installation releasing its permit.
            checkActive()
        } catch (failure: Throwable) {
            if (acquired) entry.permit.release()
            forget(packageName, entry)
            if (failure is InterruptedException) Thread.currentThread().interrupt()
            throw failure
        }
        val closed = AtomicBoolean()
        return Closeable {
            if (closed.compareAndSet(false, true)) {
                entry.permit.release()
                forget(packageName, entry)
            }
        }
    }

    private fun forget(packageName: String, entry: Entry) = synchronized(entries) {
        entry.users--
        if (entry.users == 0) entries.remove(packageName, entry)
    }
}
