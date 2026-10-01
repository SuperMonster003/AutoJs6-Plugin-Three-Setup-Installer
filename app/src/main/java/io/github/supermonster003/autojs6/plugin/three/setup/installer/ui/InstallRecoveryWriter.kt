package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/** Coalesces ordinary updates; a retired ticket or cleared generation can never write again. */
internal class InstallRecoveryWriter(private val store: () -> InstallRecoveryStore, private val executor: Executor) {
    class Completion {
        private val ready = CountDownLatch(1)
        private val finished = AtomicBoolean()
        @Volatile private var successful = false
        fun complete(success: Boolean) { if (finished.compareAndSet(false, true)) { successful = success; ready.countDown() } }
        fun await(): Boolean {
            var interrupted = false
            while (true) try { ready.await(); break } catch (_: InterruptedException) { interrupted = true }
            if (interrupted) Thread.currentThread().interrupt()
            return successful
        }
    }
    inner class Ticket internal constructor(val token: String, internal val epoch: Long) {
        internal var closed = false
        internal var revision = -1L
        fun save(snapshot: InstallRecoverySnapshot, durable: Boolean = false): Completion? = submit(this, snapshot, durable)
        fun close(completed: () -> Unit = {}) = remove(token, completed)
    }
    private data class Pending(val ticket: InstallRecoveryWriter.Ticket, val snapshot: InstallRecoverySnapshot, val waiters: MutableList<Completion>)
    private val lock = Any()
    private val tickets = linkedMapOf<String, Ticket>()
    private val pending = linkedMapOf<String, Pending>()
    private val deletions = linkedMapOf<String, MutableList<() -> Unit>>()
    private var generation = 0L
    private var scheduled = false
    private var clearing = false
    private val clearCallbacks = mutableListOf<() -> Unit>()

    fun begin(token: String): Ticket = synchronized(lock) {
        require(InstallRecoverySnapshot.validToken(token))
        require(token !in tickets && token !in deletions)
        require(tickets.size < InstallRecoverySnapshot.MAX_ENTRIES)
        Ticket(token, generation).also { tickets[token] = it }
    }
    private fun valid(ticket: Ticket, revision: Long) = synchronized(lock) {
        !ticket.closed && ticket.epoch == generation && tickets[ticket.token] === ticket && ticket.revision == revision
    }
    private fun submit(ticket: Ticket, snapshot: InstallRecoverySnapshot, durable: Boolean): Completion? {
        val completion = if (durable) Completion() else null
        synchronized(lock) {
            if (ticket.closed || ticket.epoch != generation || tickets[ticket.token] !== ticket || snapshot.token != ticket.token || snapshot.revision < ticket.revision) {
                completion?.complete(false); return completion
            }
            ticket.revision = snapshot.revision
            val waiters = pending.remove(ticket.token)?.waiters ?: mutableListOf()
            completion?.let(waiters::add)
            pending[ticket.token] = Pending(ticket, snapshot, waiters)
            schedule()
        }
        return completion
    }
    fun remove(token: String, completed: () -> Unit = {}) {
        if (!InstallRecoverySnapshot.validToken(token)) { completed(); return }
        synchronized(lock) {
            tickets.remove(token)?.closed = true
            pending.remove(token)?.waiters?.forEach { it.complete(false) }
            deletions.getOrPut(token) { mutableListOf() }.add(completed)
            schedule()
        }
    }
    fun clear(completed: () -> Unit = {}) = synchronized(lock) {
        generation++
        tickets.values.forEach { it.closed = true }
        tickets.clear()
        pending.values.flatMap { it.waiters }.forEach { it.complete(false) }
        pending.clear()
        clearing = true
        clearCallbacks += completed
        schedule()
    }
    fun read(token: String, result: (InstallRecoverySnapshot?) -> Unit) {
        if (!InstallRecoverySnapshot.validToken(token)) { result(null); return }
        executor.execute { result(runCatching { store().read(token) }.getOrNull()) }
    }
    private fun schedule() {
        if (scheduled) return
        scheduled = true
        executor.execute(::drain)
    }
    private fun drain() {
        repeat(16) {
            var callbacks: List<() -> Unit> = emptyList()
            var token: String? = null
            var clear = false
            val next = synchronized(lock) {
                when {
                    clearing -> { clearing = false; clear = true; callbacks = clearCallbacks.toList(); clearCallbacks.clear(); null }
                    deletions.isNotEmpty() -> { val first = deletions.keys.first(); token = first; callbacks = deletions.remove(first).orEmpty(); null }
                    pending.isNotEmpty() -> pending.remove(pending.keys.first())
                    else -> { scheduled = false; return }
                }
            }
            if (clear) runCatching { store().clear() }
            else if (token != null) runCatching { store().delete(requireNotNull(token)) }
            else if (next != null) {
                val ok = runCatching { store().write(next.snapshot) { valid(next.ticket, next.snapshot.revision) } }.getOrDefault(false)
                synchronized(lock) {
                    val active = !next.ticket.closed && next.ticket.epoch == generation && tickets[next.ticket.token] === next.ticket
                    val newer = pending[next.ticket.token]
                    if (!ok && active && newer != null && newer.snapshot.revision > next.snapshot.revision) {
                        // A superseded durable update waits for the newer complete snapshot,
                        // which contains its outcomes, instead of acknowledging an aborted write.
                        newer.waiters.addAll(next.waiters)
                    } else {
                        next.waiters.forEach { it.complete(ok && active) }
                    }
                }
            }
            callbacks.forEach { runCatching(it) }
        }
        executor.execute(::drain)
    }
}
