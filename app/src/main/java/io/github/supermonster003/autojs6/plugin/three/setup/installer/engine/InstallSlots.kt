package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import org.autojs.plugin.installer.api.InstallerContract
import java.io.Closeable
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean

/** One process-wide ceiling shared by host requests, external requests and user retries. */
internal object InstallSlots {
    private val slots = Semaphore(InstallerContract.MAX_CONCURRENT_SESSIONS)
    fun acquire(): Closeable {
        if (!slots.tryAcquire()) throw RequestDocuments.invalid("At most four installation sessions may run concurrently")
        val closed = AtomicBoolean()
        return Closeable { if (closed.compareAndSet(false, true)) slots.release() }
    }
}
