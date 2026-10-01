package io.github.supermonster003.autojs6.plugin.three.setup.installer.auth

import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.io.Closeable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** The platform boundary keeps permission and Binder lifecycle races testable without Android. */
internal interface ShizukuAccess {
    fun isInstalled(): Boolean
    fun isRunning(): Boolean
    fun isPreV11(): Boolean
    fun isGranted(): Boolean
    fun shouldShowRationale(): Boolean
    fun binderIdentity(): Any?
    fun observeBinder(changed: () -> Unit): Closeable
    fun requestPermission(code: Int, result: (Int, Boolean) -> Unit): Closeable
}

/** One permission dialog per process, with independent deadlines for its waiting callers. */
internal class ShizukuAuthorizer(
    private val access: ShizukuAccess,
    private val invalidateBinding: () -> Unit,
) : Closeable {
    private class Request(val code: Int) {
        val ready = CountDownLatch(1)
        var registration: Closeable? = null
        var waiters = 0
        var granted = false
        var retired = false
    }

    private val lock = Any()
    private val codes = AtomicInteger(0x5e7)
    private var pending: Request? = null
    private var closed = false
    private var binderIdentity = access.binderIdentity()
    private val binderObservation = access.observeBinder {
        val current = access.binderIdentity()
        var invalidated = false
        val registration = synchronized(lock) {
            if (!closed && binderIdentity !== current) {
                // The initial received callback can arrive after state()/bind() already used this
                // Binder. Only death/replacement of a previously seen server invalidates work.
                invalidated = binderIdentity != null
                binderIdentity = current
                if (invalidated) pending?.let(::retire) else null
            } else null
        }
        registration?.close()
        if (invalidated) invalidateBinding()
    }

    fun state(): AuthorizerState {
        if (!runCatching(access::isInstalled).getOrDefault(false)) {
            return unavailable("Shizuku is not installed")
        }
        if (!runCatching(access::isRunning).getOrDefault(false)) {
            return AuthorizerState(Authorizer.SHIZUKU, true, false, false, "Shizuku is not running")
        }
        if (runCatching(access::isPreV11).getOrDefault(true)) {
            return unavailable("Shizuku API 11 or later is required", running = true)
        }
        val granted = runCatching(access::isGranted).getOrDefault(false)
        return AuthorizerState(Authorizer.SHIZUKU, true, true, granted,
            if (granted) null else "Shizuku permission is not granted")
    }

    fun request(timeoutMillis: Long): Boolean {
        require(timeoutMillis > 0)
        val before = state()
        if (before.usable) return true
        if (!before.available || !before.running) return false
        if (runCatching(access::shouldShowRationale).getOrDefault(true)) return false
        var shouldStart = false
        val request = synchronized(lock) {
            check(!closed) { "Shizuku authorizer is closed" }
            (pending ?: Request(codes.incrementAndGet()).also { pending = it; shouldStart = true })
                .also { it.waiters++ }
        }
        try {
            if (shouldStart) start(request)
            if (!request.ready.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
                throw InstallFailure(InstallerErrorCodes.TIMEOUT, "Shizuku authorization timed out")
            }
            return synchronized(lock) { !request.retired && request.granted } && state().usable
        } finally {
            synchronized(lock) {
                request.waiters--
                if (request.waiters == 0) retire(request) else null
            }?.close()
        }
    }

    private fun start(request: Request) {
        try {
            val registration = access.requestPermission(request.code) { code, granted ->
                synchronized(lock) {
                    if (!request.retired && code == request.code) {
                        request.granted = granted
                        request.ready.countDown()
                    }
                }
            }
            val retired = synchronized(lock) {
                if (!request.retired) request.registration = registration
                request.retired
            }
            if (retired) registration.close()
        } catch (_: Exception) {
            synchronized(lock) { retire(request) }?.close()
        }
    }

    private fun retire(request: Request): Closeable? {
        if (request.retired) return null
        request.retired = true
        if (pending === request) pending = null
        request.ready.countDown()
        return request.registration.also { request.registration = null }
    }

    override fun close() {
        val registration = synchronized(lock) {
            if (closed) return
            closed = true
            pending?.let(::retire)
        }
        registration?.close()
        binderObservation.close()
    }

    private fun unavailable(reason: String, running: Boolean = false) =
        AuthorizerState(Authorizer.SHIZUKU, false, running, false, reason)
}
