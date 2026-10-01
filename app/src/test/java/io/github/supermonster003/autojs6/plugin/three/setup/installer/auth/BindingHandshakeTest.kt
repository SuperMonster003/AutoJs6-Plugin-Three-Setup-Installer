package io.github.supermonster003.autojs6.plugin.three.setup.installer.auth

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeoutException

class BindingHandshakeTest {
    private class DeadConnection : Exception()

    @Test fun `a dead read-only handshake retires only that connection and binds once more`() {
        val first = Any()
        val replacement = Any()
        val retired = mutableListOf<Any>()
        val budgets = mutableListOf<Long>()
        var now = 0L
        val actual = bindingHandshake<Any>(1_000, { now }, acquire = { remaining ->
            budgets += remaining
            if (budgets.size == 1) first else replacement
        }, verify = {
            if (it === first) { now = 350; throw DeadConnection() }
        }, invalidate = retired::add, disconnected = { it is DeadConnection })
        assertSame(replacement, actual)
        assertEquals(listOf(first), retired)
        assertEquals(listOf(1_000L, 650L), budgets)
    }

    @Test fun `two dead handshakes fail without an unbounded retry loop`() {
        var connections = 0
        var retired = 0
        val failure = DeadConnection()
        assertSame(failure, assertThrows(DeadConnection::class.java) {
            bindingHandshake<Any>(1_000, { 0L }, acquire = { connections++; Any() },
                verify = { throw failure }, invalidate = { retired++ }, disconnected = { it is DeadConnection })
        })
        assertEquals(2, connections)
        assertEquals(2, retired)
    }

    @Test fun `transport death before delivery permits one retry but explicit closure does not`() {
        var attempts = 0
        val actual = Any()
        assertSame(actual, bindingHandshake<Any>(1_000, { 0L }, acquire = {
            if (++attempts == 1) throw SharedBindingCache.ConnectionDied()
            actual
        }, verify = {}, invalidate = { fail("No value was returned to retire") },
            disconnected = { it is SharedBindingCache.ConnectionDied }))
        assertEquals(2, attempts)
        attempts = 0
        assertThrows(IllegalStateException::class.java) {
            bindingHandshake<Any>(1_000, { 0L }, acquire = { attempts++; error("Explicitly closed") },
                verify = {}, invalidate = {}, disconnected = { it is SharedBindingCache.ConnectionDied })
        }
        assertEquals(1, attempts)
    }

    @Test fun `a failed attempt never receives a fresh timeout budget`() {
        var now = 0L
        var attempts = 0
        assertThrows(TimeoutException::class.java) {
            bindingHandshake<Any>(1_000, { now }, acquire = { attempts++; Any() },
                verify = { now = 1_000; throw DeadConnection() }, invalidate = {}, disconnected = { it is DeadConnection })
        }
        assertEquals(1, attempts)
    }

    @Test fun `late success after the connection deadline remains a timeout`() {
        var now = 0L
        var attempts = 0
        assertThrows(TimeoutException::class.java) {
            bindingHandshake<Any>(1_000, { now }, acquire = { attempts++; Any() },
                verify = { now = 1_001 }, invalidate = {}, disconnected = { it is DeadConnection })
        }
        assertEquals(1, attempts)
    }

    @Test fun `permission failures and interruption do not initiate another bind`() {
        for (failure in listOf(SecurityException("Denied"), InterruptedException("Cancelled"))) {
            var attempts = 0
            try {
                bindingHandshake<Any>(1_000, { 0L }, acquire = { attempts++; Any() },
                    verify = { throw failure }, invalidate = { fail("Not a Binder death") }, disconnected = { it is DeadConnection })
                fail("Expected failure")
            } catch (actual: Exception) { assertSame(failure, actual) }
            assertEquals(1, attempts)
        }
    }
}
