package io.github.supermonster003.autojs6.plugin.three.setup.installer.auth

import android.content.Context
import android.os.DeadObjectException
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.IPrivilegedInstaller
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.PrivilegedServiceBinding
import org.autojs.plugin.installer.api.InstallerErrorCodes

/** Process-wide, shared in-flight and live bindings; transport lifecycle calls run on main. */
internal class PrivilegedClient(context: Context) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val bindings = SharedBindingCache<Authorizer, IPrivilegedInstaller>(
        dispatch = { block -> main.post { block() } },
        alive = { it.asBinder().isBinderAlive },
        create = { authorizer, connected, disconnected ->
            val binding = PrivilegedServiceBinding(appContext, authorizer == Authorizer.ROOT, connected, disconnected)
            object : SharedBindingCache.Binding {
                override fun bind() = binding.bind()
                override fun close() = binding.close()
            }
        },
    )

    fun acquire(authorizer: Authorizer, timeoutMillis: Long = BIND_TIMEOUT_MILLIS): IPrivilegedInstaller {
        require(authorizer in setOf(Authorizer.SHIZUKU, Authorizer.ROOT))
        check(Looper.myLooper() != Looper.getMainLooper()) { "Privileged binding must run on a worker" }
        try {
            return bindingHandshake(timeoutMillis, SystemClock::elapsedRealtime,
                acquire = { remaining -> bindings.acquire(authorizer, remaining) },
                // A genuine read-only Binder call closes the isBinderAlive/acquire race. No
                // createSession, write, commit or uninstall is ever inside this retry boundary.
                verify = { it.uid },
                invalidate = { bindings.invalidate(authorizer, it) },
                disconnected = { it is DeadObjectException || it is SharedBindingCache.ConnectionDied })
        } catch (failure: InterruptedException) {
            Thread.currentThread().interrupt()
            throw InstallFailure(InstallerErrorCodes.CANCELLED, "Privileged binding interrupted", cause = failure)
        } catch (failure: Exception) {
            throw InstallFailure(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE,
                "${authorizer.id} privileged service unavailable: ${failure.message}", cause = failure)
        }
    }

    /** Sessions must be idle; pending waiters are released as unavailable. */
    fun releaseAll() = bindings.close()

    companion object {
        const val BIND_TIMEOUT_MILLIS = 30_000L
        @Volatile private var shared: PrivilegedClient? = null
        /** A Shizuku server replacement cannot leave its previous UserService cached. */
        fun invalidate(authorizer: Authorizer) { shared?.bindings?.invalidate(authorizer) }
        fun get(context: Context): PrivilegedClient = shared ?: synchronized(this) {
            shared ?: PrivilegedClient(context).also { shared = it }
        }
    }
}
