package io.github.supermonster003.autojs6.plugin.three.setup.installer.binder

import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.RequestDocuments
import org.autojs.plugin.installer.api.InstallerContract
import java.io.Closeable
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/** Reserves capacity before acquiring sources. Closing a running session keeps its slot until cleanup. */
internal class SessionRegistry(
    private val clock: () -> Long,
    private val retentionMillis: Long = 600_000,
    scheduler: ScheduledExecutorService? = null,
) : Closeable {
    private val lock = Any()
    private val entries = linkedMapOf<String, Lease>()
    private var closed = false
    // One task only refers to this bounded registry. A per-session timer would retain already
    // closed sessions until its delay elapsed, bypassing the registry's retained-handle ceiling.
    private val reaper = scheduler?.scheduleWithFixedDelay(::reap, 1_000, 1_000, TimeUnit.MILLISECONDS)

    inner class Lease internal constructor(private val key: String) : Closeable {
        private var stop: (() -> Unit)? = null
        private var closed = false
        private var endedAt: Long? = null

        fun attach(stop: () -> Unit) {
            val invoke = synchronized(lock) {
                if (closed) true else { this.stop = stop; false }
            }
            if (invoke) stop()
        }
        fun finished() = synchronized(lock) {
            if (endedAt == null) endedAt = clock()
            if (closed) entries.remove(key, this)
        }
        override fun close() {
            val action = synchronized(lock) {
                if (closed) return
                closed = true
                if (endedAt != null) entries.remove(key, this)
                stop.also { stop = null }
            }
            action?.invoke()
        }
        internal fun active() = endedAt == null
        internal fun expired(now: Long) = endedAt?.let { now - it >= retentionMillis } == true
    }

    fun reserve(ownerUid: Int, id: String): Lease {
        reap()
        return synchronized(lock) {
            if (closed) throw RequestDocuments.invalid("Installer service is closed")
            val key = "$ownerUid:$id"
            if (key in entries) throw RequestDocuments.invalid("Duplicate active or retained session id")
            if (entries.values.count { it.active() } >= InstallerContract.MAX_CONCURRENT_SESSIONS) throw RequestDocuments.invalid("At most four installation sessions may run concurrently")
            // Terminal handles retain status for ten minutes. Bound that memory independently of workers.
            if (entries.size >= 128) throw RequestDocuments.invalid("Too many unclosed installation sessions")
            Lease(key).also { entries[key] = it }
        }
    }
    fun reap() {
        val expired = synchronized(lock) { entries.values.filter { it.expired(clock()) } }
        expired.forEach { it.close() }
    }
    override fun close() {
        val leases = synchronized(lock) { closed = true; entries.values.toList() }
        reaper?.cancel(false)
        leases.forEach { it.close() }
    }
}
