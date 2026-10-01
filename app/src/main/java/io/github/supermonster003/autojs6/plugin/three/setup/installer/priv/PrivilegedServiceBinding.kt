package io.github.supermonster003.autojs6.plugin.three.setup.installer.priv

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.topjohnwu.superuser.ipc.RootService
import io.github.supermonster003.autojs6.plugin.three.setup.installer.BuildConfig
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.RootShellAccess
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.HiddenApiAccess
import rikka.shizuku.Shizuku
import java.io.Closeable
import java.util.UUID
import java.util.concurrent.Future

/** Main-thread owned binding. The caller handles permission prompts before bind(). */
internal class PrivilegedServiceBinding(
    private val context: Context,
    private val root: Boolean,
    private val connected: (IPrivilegedInstaller) -> Unit,
    private val disconnected: () -> Unit,
) : ServiceConnection, Closeable {
    private val token = Binder()
    private val main = Handler(Looper.getMainLooper())
    private var bound = false
    private var rootLaunch: Future<*>? = null
    private var serviceBinder: IBinder? = null
    private val deathRecipient = IBinder.DeathRecipient { main.post { failBinding() } }
    private val args = Shizuku.UserServiceArgs(ComponentName(context, ShizukuUserService::class.java))
        // remove=true destroys remotely, but Shizuku 13.1.5 retains its local connection until the
        // delayed died() callback. A new generation must not subscribe to that retiring container.
        .tag("three-setup-installer-${UUID.randomUUID()}")
        .processNameSuffix("three-setup-installer-privileged").daemon(false)
        .version(BuildConfig.VERSION_CODE).debuggable(BuildConfig.DEBUG)

    fun bind() {
        check(Looper.myLooper() == Looper.getMainLooper())
        check(!bound) { "Already bound" }
        HiddenApiAccess.initialize()
        bound = true
        try {
            if (root) {
                val task = RootService.bindOrTask(Intent(context, RootInstallerService::class.java),
                    { command -> main.post(command) }, this)
                if (task != null) rootLaunch = RootShellAccess.submit(context, { shell ->
                    check(shell.isRoot) { "Root access was not granted" }
                    shell.execTask(task)
                }, { main.post { failBinding() } })
            } else {
                check(!Shizuku.isPreV11()) { "Shizuku API 11 or later is required" }
                Shizuku.bindUserService(args, this)
            }
        } catch (failure: Exception) {
            bound = false
            throw failure
        }
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        if (!bound) return
        try {
            val installer = IPrivilegedInstaller.Stub.asInterface(service)
            installer.attachClient(token)
            service.linkToDeath(deathRecipient, 0)
            serviceBinder = service
            connected(installer)
        } catch (failure: Exception) {
            failBinding()
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        failBinding()
    }

    override fun onBindingDied(name: ComponentName) = failBinding()
    override fun onNullBinding(name: ComponentName) = failBinding()

    private fun failBinding() {
        if (!bound) return
        close()
        disconnected()
    }

    override fun close() {
        check(Looper.myLooper() == Looper.getMainLooper())
        if (!bound) return
        bound = false
        val attached = serviceBinder
        attached?.let { runCatching { it.unlinkToDeath(deathRecipient, 0) } }
        serviceBinder = null
        rootLaunch?.cancel(true)
        rootLaunch = null
        // The authorization server may already be dead. Its cleanup failure must not suppress
        // the disconnection callback that wakes callers waiting for the privileged Binder.
        val detached = runCatching {
            if (root) RootService.unbind(this) else Shizuku.unbindUserService(args, this, true)
        }
        if (!root && detached.isFailure && attached?.isBinderAlive == true) {
            // Losing the server does not grant access to any other process. Only the service
            // attached by this binding can receive its existing owner-checked shutdown method.
            runCatching { IPrivilegedInstaller.Stub.asInterface(attached).destroy() }
        }
    }
}
