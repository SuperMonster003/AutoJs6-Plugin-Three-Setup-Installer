package io.github.supermonster003.autojs6.plugin.three.setup.installer.binder

import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SessionRegistryTest {
    @Test fun `closing live work does not free its slot before cleanup`() {
        val registry = SessionRegistry({ 0 })
        val leases = (0..3).map { registry.reserve(1, "$it") }
        var cancelled = 0
        leases[0].attach { cancelled++ }
        leases[0].close()
        leases[0].close()
        assertEquals(1, cancelled)
        assertEquals("INVALID_ARGUMENT", assertThrows(InstallFailure::class.java) { registry.reserve(1, "extra") }.code)
        leases[0].finished()
        registry.reserve(1, "extra")
    }
    @Test fun `terminal handles do not occupy workers and expire ten minutes after completion`() {
        var now = 0L
        val registry = SessionRegistry({ now })
        val lease = registry.reserve(1, "same")
        var closed = 0
        lease.attach { closed++ }
        now = 1_000
        lease.finished()
        (0..3).forEach { registry.reserve(2, "$it") }
        assertThrows(InstallFailure::class.java) { registry.reserve(1, "same") }
        now = 600_999
        registry.reap()
        assertEquals(0, closed)
        now++
        registry.reap()
        assertEquals(1, closed)
        registry.reap()
        assertEquals(1, closed)
    }
    @Test fun `owner death before attachment still cancels and service shutdown rejects new work`() {
        val registry = SessionRegistry({ 0 })
        val lease = registry.reserve(1, "same")
        registry.reserve(2, "same")
        lease.close()
        var stops = 0
        lease.attach { stops++ }
        assertEquals(1, stops)
        registry.close()
        assertThrows(InstallFailure::class.java) { registry.reserve(3, "later") }
    }
    @Test fun `retained handles have a memory ceiling independent of active sessions`() {
        val registry = SessionRegistry({ 0 })
        repeat(128) { registry.reserve(1, "$it").finished() }
        assertThrows(InstallFailure::class.java) { registry.reserve(1, "more") }
        registry.close()
    }

    @Test fun `closed terminal sessions do not accumulate delayed cleanup tasks`() {
        val scheduler = ScheduledThreadPoolExecutor(1).apply { removeOnCancelPolicy = true }
        val registry = SessionRegistry({ 0 }, scheduler = scheduler)
        try {
            var stops = 0
            repeat(2_048) { index ->
                val lease = registry.reserve(1, "$index")
                lease.attach { stops++ }
                lease.finished()
                lease.close()
            }
            assertEquals(2_048, stops)
            assertTrue("Only the registry reaper may remain scheduled", scheduler.queue.size <= 1)
            // The closed requests do not consume the independent terminal-handle budget either.
            repeat(128) { registry.reserve(1, "retained-$it").finished() }
            assertThrows(InstallFailure::class.java) { registry.reserve(1, "over-limit") }
            registry.close()
            assertTrue("Closing the registry cancels its reaper", scheduler.queue.isEmpty())
        } finally {
            registry.close()
            scheduler.shutdownNow()
        }
    }

    @Test fun `idle terminal sessions expire without another incoming request`() {
        val now = java.util.concurrent.atomic.AtomicLong()
        val expired = CountDownLatch(1)
        val scheduler = ScheduledThreadPoolExecutor(1)
        val registry = SessionRegistry(now::get, scheduler = scheduler)
        try {
            val lease = registry.reserve(1, "idle")
            lease.attach { expired.countDown() }
            lease.finished()
            now.set(600_000)
            assertTrue("Periodic reaping must reclaim idle terminal handles", expired.await(5, TimeUnit.SECONDS))
            registry.reserve(1, "idle")
        } finally {
            registry.close()
            scheduler.shutdownNow()
        }
    }

    @Test fun `concurrent completion and close cancel once and release capacity`() {
        val registry = SessionRegistry({ 0 })
        val workers = Executors.newFixedThreadPool(2)
        try {
            repeat(64) { index ->
                val lease = registry.reserve(1, "same")
                val stops = AtomicInteger()
                lease.attach { stops.incrementAndGet() }
                val start = CountDownLatch(1)
                val finished = workers.submit { start.await(); lease.finished() }
                val closed = workers.submit { start.await(); lease.close() }
                start.countDown()
                finished.get(5, TimeUnit.SECONDS)
                closed.get(5, TimeUnit.SECONDS)
                lease.close()
                assertEquals("Cancellation must only run once at iteration $index", 1, stops.get())
            }
        } finally {
            registry.close()
            workers.shutdownNow()
        }
    }

    @Test fun `concurrent attachment and close do not lose cancellation`() {
        val registry = SessionRegistry({ 0 })
        val workers = Executors.newFixedThreadPool(2)
        try {
            repeat(64) {
                val lease = registry.reserve(1, "same")
                val stops = AtomicInteger()
                val start = CountDownLatch(1)
                val attached = workers.submit { start.await(); lease.attach { stops.incrementAndGet() } }
                val closed = workers.submit { start.await(); lease.close() }
                start.countDown()
                attached.get(5, TimeUnit.SECONDS)
                closed.get(5, TimeUnit.SECONDS)
                lease.finished()
                assertEquals(1, stops.get())
            }
        } finally {
            registry.close()
            workers.shutdownNow()
        }
    }
}
