package io.github.supermonster003.autojs6.plugin.three.setup.installer.auth

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class SharedBindingCacheTest {
    @Test fun `concurrent first acquisition shares one binding and later calls reuse its identity`() = fixture { f ->
        val calls = (0..3).map { f.workers.submit<Connection> { f.cache.acquire("root", 5_000) } }
        val binding = f.next()
        val service = Connection()
        binding.connected(service)
        calls.forEach { assertSame(service, it.get(3, TimeUnit.SECONDS)) }
        assertSame(service, f.cache.acquire("root", 1))
        assertNull(f.created.poll())
    }
    @Test fun `one waiter timing out leaves another waiter connected`() = fixture { f ->
        val patient = f.workers.submit<Connection> { f.cache.acquire("root", 5_000) }
        val binding = f.next()
        assertThrows(TimeoutException::class.java) { f.cache.acquire("root", 20) }
        assertEquals(1L, binding.closed.count)
        val service = Connection()
        binding.connected(service)
        assertSame(service, patient.get(3, TimeUnit.SECONDS))
    }
    @Test fun `last interrupted waiter closes its pending transport`() = fixture { f ->
        val done = CountDownLatch(1)
        val worker = Thread {
            try { f.cache.acquire("root", 5_000); fail("Expected interruption") }
            catch (_: InterruptedException) { done.countDown() }
        }.apply { start() }
        val binding = f.next()
        worker.interrupt()
        assertTrue(done.await(3, TimeUnit.SECONDS))
        assertTrue(binding.closed.await(3, TimeUnit.SECONDS))
        worker.join(3_000)
        val next = f.workers.submit<Connection> { f.cache.acquire("root", 5_000) }
        val fresh = f.next()
        fresh.connected(Connection())
        next.get(3, TimeUnit.SECONDS)
    }
    @Test fun `dead cached service is replaced and stale callbacks cannot replace the new connection`() = fixture { f ->
        val oldCall = f.workers.submit<Connection> { f.cache.acquire("root", 5_000) }
        val old = f.next()
        val oldService = Connection()
        old.connected(oldService)
        oldCall.get(3, TimeUnit.SECONDS)
        oldService.alive = false
        val newCall = f.workers.submit<Connection> { f.cache.acquire("root", 5_000) }
        val fresh = f.next()
        assertTrue(old.closed.await(3, TimeUnit.SECONDS))
        old.connected(Connection())
        old.disconnected()
        val service = Connection()
        fresh.connected(service)
        assertSame(service, newCall.get(3, TimeUnit.SECONDS))
    }
    @Test fun `release all wakes waiters and a late connection cannot become cached`() = fixture { f ->
        val call = f.workers.submit<Connection> { f.cache.acquire("root", 5_000) }
        val binding = f.next()
        f.cache.close()
        assertThrows(java.util.concurrent.ExecutionException::class.java) { call.get(3, TimeUnit.SECONDS) }
        binding.connected(Connection())
        val next = f.workers.submit<Connection> { f.cache.acquire("root", 5_000) }
        val fresh = f.next()
        val service = Connection()
        fresh.connected(service)
        assertSame(service, next.get(3, TimeUnit.SECONDS))
    }
    @Test fun `authorizer invalidation preserves the other authorizers cached binding`() = fixture { f ->
        val root = f.workers.submit<Connection> { f.cache.acquire("root", 5_000) }
        val rootBinding = f.next()
        val rootService = Connection()
        rootBinding.connected(rootService)
        assertSame(rootService, root.get(3, TimeUnit.SECONDS))
        val shizuku = f.workers.submit<Connection> { f.cache.acquire("shizuku", 5_000) }
        val shizukuBinding = f.next()
        f.cache.invalidate("shizuku")
        assertThrows(java.util.concurrent.ExecutionException::class.java) { shizuku.get(3, TimeUnit.SECONDS) }
        assertTrue(shizukuBinding.closed.await(3, TimeUnit.SECONDS))
        assertSame(rootService, f.cache.acquire("root", 1))
        assertEquals(1L, rootBinding.closed.count)
    }
    @Test fun `last timed out waiter closes the pending connection`() = fixture { f ->
        assertThrows(TimeoutException::class.java) { f.cache.acquire("root", 20) }
        val old = f.next()
        assertTrue(old.closed.await(3, TimeUnit.SECONDS))
        val retry = f.workers.submit<Connection> { f.cache.acquire("root", 5_000) }
        val fresh = f.next()
        val service = Connection()
        old.connected(Connection())
        fresh.connected(service)
        assertSame(service, retry.get(3, TimeUnit.SECONDS))
    }
    private fun fixture(block: (Fixture) -> Unit) {
        val f = Fixture()
        try { block(f) } finally {
            f.cache.close()
            f.workers.shutdownNow()
            f.transport.shutdown()
            assertTrue(f.workers.awaitTermination(3, TimeUnit.SECONDS))
            assertTrue(f.transport.awaitTermination(3, TimeUnit.SECONDS))
        }
    }
    private class Connection { @Volatile var alive = true }
    private class FakeBinding(val connected: (Connection) -> Unit, val disconnected: () -> Unit) : SharedBindingCache.Binding {
        val closed = CountDownLatch(1)
        override fun bind() = Unit
        override fun close() { closed.countDown() }
    }
    private class Fixture {
        val transport = Executors.newSingleThreadExecutor()
        val workers = Executors.newFixedThreadPool(4)
        val created = LinkedBlockingQueue<FakeBinding>()
        val cache = SharedBindingCache<String, Connection>({ transport.execute(it) }, { it.alive }, { _, connected, disconnected ->
            FakeBinding(connected, disconnected).also { created.offer(it) }
        })
        fun next(): FakeBinding = requireNotNull(created.poll(3, TimeUnit.SECONDS)) { "No binding created" }
    }
}
