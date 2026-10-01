package io.github.supermonster003.autojs6.plugin.three.setup.installer.history

import android.content.Context
import java.io.Closeable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Bounded process cache with serialized disk IO. Deletion retires an item's writer immediately,
 * including writes queued before the IO thread observes the deletion. Clearing retires all old
 * tickets; a still-running installation cannot repopulate deliberately cleared history.
 */
internal class InstallHistoryStore(
    private val file: InstallHistoryFile,
    private val executor: Executor,
    private val callbacks: Executor = Executor { it.run() },
    private val clock: () -> Long = System::currentTimeMillis,
) {
    class Completion {
        private val latch = CountDownLatch(1)
        @Volatile private var successful = false
        internal fun complete(value: Boolean) { successful = value; latch.countDown() }
        fun await(): Boolean {
            var interrupted = false
            while (true) try { latch.await(); break } catch (_: InterruptedException) { interrupted = true }
            if (interrupted) Thread.currentThread().interrupt()
            return successful
        }
    }
    inner class Ticket internal constructor(val token: String, internal val epoch: Long) {
        internal var revision = -1L
        internal var retired = false
        internal val deleted = mutableSetOf<String>()
        fun save(entries: List<InstallHistoryEntry>, revision: Long, finish: Boolean = false): Completion =
            save(this, entries, revision, finish)
    }
    private class Subscription(@Volatile var action: (() -> Unit)?)
    private val lock = Any()
    private val entries = linkedMapOf<String, InstallHistoryEntry>()
    private val tickets = mutableMapOf<String, Ticket>()
    private val observers = mutableSetOf<Subscription>()
    private val updateQueued = AtomicBoolean()
    private var generation = 0L
    private var mutation = 0L
    @Volatile var loaded: Boolean = false
        private set

    init {
        executor.execute {
            val restored = runCatching { file.read() }.getOrDefault(emptyList())
            val now = clock()
            synchronized(lock) {
                restored.map { it.interrupted(now) }.forEach { entries[it.id] = it }
                trim()
                loaded = true
            }
            // A restart never recreates a queue. Its cancelled records are durable immediately.
            if (restored.any { !it.terminal }) persist()
            changed()
        }
    }

    /** Main-thread safe; first use is empty until the asynchronous initial load notifies observers. */
    fun list(): List<InstallHistoryEntry> = synchronized(lock) { sorted() }

    fun observe(observer: () -> Unit): Closeable {
        val subscription = Subscription(observer)
        synchronized(lock) { observers += subscription }
        if (loaded) callbacks.execute { subscription.action?.invoke() }
        return Closeable { synchronized(lock) { subscription.action = null; observers -= subscription } }
    }

    fun begin(token: String): Ticket = synchronized(lock) {
        require(InstallHistoryEntry.validToken(token) && token !in tickets)
        Ticket(token, generation).also { tickets[token] = it }
    }

    private fun save(ticket: Ticket, values: List<InstallHistoryEntry>, revision: Long, finish: Boolean): Completion {
        val completion = Completion()
        synchronized(lock) {
            if (!valid(ticket) || revision < ticket.revision || values.any { it.token != ticket.token }) {
                completion.complete(false); return completion
            }
            ticket.revision = revision
            mutation++
        }
        executor.execute {
            val accepted = synchronized(lock) {
                if (!valid(ticket) || revision != ticket.revision) false else {
                    values.filter { it.id !in ticket.deleted }.forEach { value ->
                        val previous = entries[value.id]
                        // Completion can be published both per item and per session. Its first
                        // confirmed time and outcome survive later session-level cancellation.
                        if (previous?.terminal != true || value.terminal) {
                            entries[value.id] = if (previous?.terminal == true && value.terminal) value.copy(
                                finishedAt = previous.finishedAt, updatedAt = maxOf(value.updatedAt, previous.updatedAt)) else value
                        }
                    }
                    trim()
                    true
                }
            }
            val written = accepted && persist()
            if (finish) synchronized(lock) {
                if (tickets[ticket.token] === ticket && revision == ticket.revision) {
                    tickets.remove(ticket.token); ticket.retired = true
                }
            }
            completion.complete(written)
            if (accepted) changed()
        }
        return completion
    }

    fun remove(id: String, completed: (Boolean) -> Unit = {}) {
        synchronized(lock) {
            tickets.values.firstOrNull { id.startsWith(it.token + ":") }?.deleted?.add(id)
            mutation++
        }
        executor.execute {
            synchronized(lock) { entries.remove(id) }
            val written = persist()
            changed()
            callbacks.execute { completed(written) }
        }
    }

    fun clear(completed: (Boolean) -> Unit = {}) {
        synchronized(lock) {
            generation++
            mutation++
            tickets.values.forEach { it.retired = true }
            tickets.clear()
        }
        executor.execute {
            synchronized(lock) { entries.clear() }
            val written = persist()
            changed()
            callbacks.execute { completed(written) }
        }
    }

    private fun valid(ticket: Ticket) = !ticket.retired && ticket.epoch == generation && tickets[ticket.token] === ticket
    private fun sorted(): List<InstallHistoryEntry> = entries.values.sortedWith(
        compareByDescending<InstallHistoryEntry> { it.finishedAt ?: it.updatedAt }.thenByDescending { it.startedAt }
            .thenBy { it.token }.thenBy { it.itemIndex })

    private fun trim() {
        val removed = sorted().drop(InstallHistoryEntry.MAX_ENTRIES)
        removed.forEach { entries.remove(it.id); tickets[it.token]?.deleted?.add(it.id) }
    }

    private fun persist(): Boolean {
        val (revision, values) = synchronized(lock) { mutation to sorted() }
        // Failure to save history never changes a confirmed PackageInstaller outcome.
        return runCatching { file.write(values) { synchronized(lock) { mutation == revision } } }.getOrDefault(false)
    }

    private fun changed() {
        if (!updateQueued.compareAndSet(false, true)) return
        callbacks.execute {
            updateQueued.set(false)
            val subscriptions = synchronized(lock) { observers.toList() }
            subscriptions.forEach { subscription -> runCatching { subscription.action?.invoke() } }
        }
    }

    companion object {
        fun get(context: Context): InstallHistoryStore = InstallHistoryPersistence.get(context)
    }
}
