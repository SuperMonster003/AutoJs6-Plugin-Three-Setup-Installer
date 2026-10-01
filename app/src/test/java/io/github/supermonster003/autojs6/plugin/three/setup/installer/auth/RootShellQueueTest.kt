package io.github.supermonster003.autojs6.plugin.three.setup.installer.auth

import org.junit.Assert.*
import org.junit.Test
import java.io.Closeable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class RootShellQueueTest {
    @Test fun `root authorization and service startup cannot close each others active shell`() = fixture { f ->
        val answer = CountDownLatch(1)
        val authorize = f.queue.submit(f::open, { shell -> answer.await(); assertFalse(shell.closed); true })
        val permissionShell = f.next()
        val bind = f.queue.submit(f::open, { shell ->
            assertTrue(permissionShell.closed)
            assertFalse(shell.closed)
            throw IllegalStateException("launch failed")
        })
        assertNull(f.opened.poll())
        assertFalse(permissionShell.closed)
        answer.countDown()
        assertTrue(authorize.get(3, TimeUnit.SECONDS))
        val serviceShell = f.next()
        assertThrows(java.util.concurrent.ExecutionException::class.java) { bind.get(3, TimeUnit.SECONDS) }
        assertTrue(permissionShell.closed)
        assertTrue(serviceShell.closed)
    }

    @Test fun `cancelling a queued bind preserves an in progress root permission request`() = fixture { f ->
        val answer = CountDownLatch(1)
        val authorize = f.queue.submit(f::open, { answer.await(); true })
        val shell = f.next()
        val bind = f.queue.submit(f::open, { fail("Cancelled bind must not start") })
        assertTrue(bind.cancel(true))
        assertFalse(shell.closed)
        answer.countDown()
        assertTrue(authorize.get(3, TimeUnit.SECONDS))
        assertTrue(shell.closed)
        assertEquals("retry", f.queue.submit(f::open, { "retry" }).get(3, TimeUnit.SECONDS))
        assertTrue(f.next().closed)
        assertNull(f.opened.poll())
    }

    @Test fun `interrupting a running shell task still closes its shell before the next task`() = fixture { f ->
        val started = CountDownLatch(1)
        val call = f.queue.submit(f::open, { started.countDown(); CountDownLatch(1).await() })
        val shell = f.next()
        assertTrue(started.await(3, TimeUnit.SECONDS))
        assertTrue(call.cancel(true))
        assertTrue(f.queue.submit(f::open, { shell.closed }).get(3, TimeUnit.SECONDS))
        assertTrue(f.next().closed)
    }

    private fun fixture(block: (Fixture) -> Unit) {
        val f = Fixture()
        try { block(f) } finally {
            f.worker.shutdownNow()
            assertTrue(f.worker.awaitTermination(3, TimeUnit.SECONDS))
        }
    }
    private class FakeShell : Closeable {
        @Volatile var closed = false
        override fun close() { check(!closed); closed = true }
    }
    private class Fixture {
        val worker = Executors.newSingleThreadExecutor()
        val queue = RootShellQueue<FakeShell>(worker)
        val opened = LinkedBlockingQueue<FakeShell>()
        fun open() = FakeShell().also { opened.offer(it) }
        fun next(): FakeShell = requireNotNull(opened.poll(3, TimeUnit.SECONDS)) { "Shell was not opened" }
    }
}
