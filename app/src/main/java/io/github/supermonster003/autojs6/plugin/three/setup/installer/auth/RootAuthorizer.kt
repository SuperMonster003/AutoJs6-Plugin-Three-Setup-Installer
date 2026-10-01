package io.github.supermonster003.autojs6.plugin.three.setup.installer.auth

import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

internal interface RootAccess {
    /** These queries must never open a shell or display a root permission prompt. */
    fun hasSuBinary(): Boolean
    fun granted(): Boolean?
    fun request(): Future<Boolean>
}

internal class RootAuthorizer(private val access: RootAccess) {
    private class Request(val future: Future<Boolean>) { var waiters = 0 }
    private val lock = Any()
    private var pending: Request? = null

    fun state(): AuthorizerState {
        val granted = access.granted()
        val available = granted == true || access.hasSuBinary()
        return AuthorizerState(Authorizer.ROOT, available, running = true, granted = granted == true,
            reason = when {
                granted == true -> null
                !available -> "No executable su binary was found"
                granted == false -> "Root access was denied to the plugin"
                else -> "Root access has not been granted yet"
            })
    }

    fun request(timeoutMillis: Long): Boolean {
        require(timeoutMillis > 0)
        if (!state().available) return false
        val request = synchronized(lock) {
            (pending ?: Request(access.request()).also { pending = it }).also { it.waiters++ }
        }
        try {
            return request.future.get(minOf(timeoutMillis, TIMEOUT_MILLIS), TimeUnit.MILLISECONDS)
        } catch (failure: TimeoutException) {
            throw InstallFailure(InstallerErrorCodes.TIMEOUT, "Root authorization timed out", cause = failure)
        } catch (failure: ExecutionException) {
            throw InstallFailure(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE,
                "Root authorization failed: ${failure.cause?.message ?: failure.message}", cause = failure.cause ?: failure)
        } finally {
            synchronized(lock) {
                request.waiters--
                if (request.waiters == 0) {
                    if (!request.future.isDone) request.future.cancel(true)
                    if (pending === request) pending = null
                }
            }
        }
    }

    companion object { const val TIMEOUT_MILLIS = 10_000L }
}
