package io.github.supermonster003.autojs6.plugin.three.setup.installer.auth

import java.io.Closeable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** One in-flight connection per key. A caller timing out does not disconnect other waiters. */
internal class SharedBindingCache<K, V : Any>(
    private val dispatch: (() -> Unit) -> Unit,
    private val alive: (V) -> Boolean,
    private val create: (K, (V) -> Unit, () -> Unit) -> Binding,
) : Closeable {
    interface Binding : Closeable { fun bind() }
    /** A transport died, as distinct from explicit cancellation, refusal or a binding timeout. */
    class ConnectionDied : IllegalStateException("Privileged service disconnected")
    private class Entry<V> {
        val ready = CountDownLatch(1)
        var binding: Binding? = null
        var value: V? = null
        var failure: Exception? = null
        var waiters = 0
        var retired = false
    }
    private val lock = Any()
    private val entries = mutableMapOf<K, Entry<V>>()

    fun acquire(key: K, timeoutMillis: Long): V {
        require(timeoutMillis > 0)
        val entry = synchronized(lock) {
            entries[key]?.let { cached ->
                cached.value?.let { if (alive(it)) return it else retire(key, cached) }
            }
            (entries[key] ?: Entry<V>().also { fresh ->
                entries[key] = fresh
                dispatch { connect(key, fresh) }
            }).also { it.waiters++ }
        }
        try {
            if (!entry.ready.await(timeoutMillis, TimeUnit.MILLISECONDS)) throw TimeoutException("Privileged service connection timed out")
            return synchronized(lock) {
                entry.failure?.let { throw it }
                entry.value?.takeIf { !entry.retired && alive(it) }
                    ?: if (!entry.retired) throw ConnectionDied()
                    else throw IllegalStateException("Privileged service binding was closed")
            }
        } finally {
            synchronized(lock) {
                entry.waiters--
                if (entry.waiters == 0 && entry.value == null) retire(key, entry)
            }
        }
    }

    private fun connect(key: K, entry: Entry<V>) {
        synchronized(lock) { if (entry.retired) return }
        try {
            val binding = create(key, { value ->
                synchronized(lock) {
                    if (!entry.retired) { entry.value = value; entry.ready.countDown() }
                }
            }, { synchronized(lock) {
                // A refusal/null binding before any service was delivered is not evidence of
                // Binder death, and must not cause a second Root permission/startup attempt.
                if (!entry.retired) entry.failure = if (entry.value != null) ConnectionDied()
                    else IllegalStateException("Privileged service unavailable while binding")
                retire(key, entry)
            } })
            synchronized(lock) {
                if (entry.retired) { binding.close(); return }
                entry.binding = binding
            }
            binding.bind()
        } catch (failure: Exception) {
            synchronized(lock) { entry.failure = failure; retire(key, entry) }
        }
    }

    private fun retire(key: K, entry: Entry<V>) {
        if (entry.retired) return
        entry.retired = true
        if (entries[key] === entry) entries.remove(key)
        entry.value = null
        entry.ready.countDown()
        dispatch { runCatching { entry.binding?.close() } }
    }

    fun invalidate(key: K) = synchronized(lock) { entries[key]?.let { retire(key, it) } }

    /** A late failed handshake must not retire a replacement acquired by another caller. */
    fun invalidate(key: K, expected: V) = synchronized(lock) {
        entries[key]?.takeIf { it.value === expected }?.let { retire(key, it) }
    }

    override fun close() = synchronized(lock) {
        entries.toMap().forEach { (key, value) -> retire(key, value) }
    }
}
