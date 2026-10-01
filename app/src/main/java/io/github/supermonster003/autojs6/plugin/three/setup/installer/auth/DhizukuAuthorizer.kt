package io.github.supermonster003.autojs6.plugin.three.setup.installer.auth

import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.io.Closeable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal interface DhizukuAccess {
    fun state(): AuthorizerState
    fun requestPermission(result: (Boolean) -> Unit): Closeable
}

/** One prompt shared by concurrent callers; retired callbacks cannot authorize a later request. */
internal class DhizukuAuthorizer(private val access: DhizukuAccess) {
    private class Request {
        val ready = CountDownLatch(1)
        var registration: Closeable? = null
        var waiters = 0
        var active = true
        var granted = false
        var answered = false
    }
    private val lock = Any()
    private var pending: Request? = null

    fun state(): AuthorizerState = access.state()

    fun request(timeoutMillis: Long): Boolean {
        require(timeoutMillis > 0)
        val state = state()
        if (state.usable) return true
        if (!state.available || !state.running) return false
        var start = false
        val request = synchronized(lock) {
            (pending ?: Request().also { pending = it; start = true }).also { it.waiters++ }
        }
        try {
            if (start) {
                try {
                    val registration = access.requestPermission { granted ->
                        synchronized(lock) {
                            if (pending === request && request.active && !request.answered) {
                                request.answered = true
                                request.granted = granted
                                request.ready.countDown()
                            }
                        }
                    }
                    synchronized(lock) {
                        if (request.active) request.registration = registration else registration.close()
                    }
                } catch (failure: Exception) {
                    synchronized(lock) { request.answered = true; request.ready.countDown() }
                    throw failure
                }
            }
            if (!request.ready.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
                throw InstallFailure(InstallerErrorCodes.TIMEOUT, "Dhizuku authorization timed out")
            }
            return synchronized(lock) { request.active && request.granted } && state().usable
        } finally {
            val close = synchronized(lock) {
                request.waiters--
                if (request.waiters == 0) {
                    request.active = false
                    if (pending === request) pending = null
                    request.registration.also { request.registration = null }
                } else null
            }
            close?.close()
        }
    }
}
