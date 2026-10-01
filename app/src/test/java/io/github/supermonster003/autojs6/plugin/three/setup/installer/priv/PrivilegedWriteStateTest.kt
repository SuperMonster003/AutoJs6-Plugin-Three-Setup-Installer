package io.github.supermonster003.autojs6.plugin.three.setup.installer.priv

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PrivilegedWriteStateTest {
    @Test fun `the destination failure is observable before the pipe close becomes visible`() {
        val state = PrivilegedWriteState()
        val original = IOException("destination failure")
        val readerClosed = CountDownLatch(1)
        val finishClose = CountDownLatch(1)
        val worker = Executors.newSingleThreadExecutor()
        try {
            val work = worker.submit {
                assertSame(original, assertThrows(IOException::class.java) {
                    state.write(closeInput = {
                        readerClosed.countDown()
                        check(finishClose.await(5, TimeUnit.SECONDS))
                    }, onFailure = {}) { throw original }
                })
            }
            assertTrue(readerClosed.await(5, TimeUnit.SECONDS))
            assertFalse("Writer Future is still running", work.isDone)
            assertSame(original, assertThrows(IllegalStateException::class.java) { state.checkFailure() }.cause)
            finishClose.countDown()
            work.get(5, TimeUnit.SECONDS)
        } finally { finishClose.countDown(); worker.shutdownNow() }
    }

    @Test fun `later stream failures and logging errors do not replace the original cause`() {
        val state = PrivilegedWriteState()
        val first = IOException("first failure")
        var closed = 0
        assertSame(first, assertThrows(IOException::class.java) {
            state.write(closeInput = { closed++ }, onFailure = { throw IllegalStateException("logger") }) { throw first }
        })
        assertThrows(IOException::class.java) {
            state.write(closeInput = { closed++ }, onFailure = {}) { throw IOException("later failure") }
        }
        assertEquals(2, closed)
        assertSame(first, assertThrows(IllegalStateException::class.java) { state.checkFailure() }.cause)
    }

    @Test fun `successful copies publish no false failure and close exactly once`() {
        val state = PrivilegedWriteState()
        var closed = 0
        state.write(closeInput = { closed++ }, onFailure = { fail("Unexpected failure") }) { state.checkFailure() }
        state.checkFailure()
        assertEquals(1, closed)
    }
}
