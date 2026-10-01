package io.github.supermonster003.autojs6.plugin.three.setup.installer.auth

import java.util.concurrent.TimeoutException

/**
 * Only the read-only connection handshake may be repeated. The entire handshake shares one
 * deadline and at most one replacement; callers perform all session mutations after this returns.
 */
internal fun <T : Any> bindingHandshake(
    timeoutMillis: Long,
    clock: () -> Long,
    acquire: (Long) -> T,
    verify: (T) -> Unit,
    invalidate: (T) -> Unit,
    disconnected: (Exception) -> Boolean,
): T {
    require(timeoutMillis > 0)
    val started = clock()
    repeat(2) { attempt ->
        val remaining = timeoutMillis - (clock() - started).coerceAtLeast(0)
        if (remaining <= 0) throw TimeoutException("Privileged service connection timed out")
        var connection: T? = null
        try {
            val acquired = acquire(remaining).also { connection = it }
            verify(acquired)
            if (clock() - started >= timeoutMillis) throw TimeoutException("Privileged service connection timed out")
            return acquired
        } catch (failure: Exception) {
            if (!disconnected(failure)) throw failure
            connection?.let(invalidate)
            if (attempt == 1) throw failure
        }
    }
    error("Unreachable connection retry")
}
