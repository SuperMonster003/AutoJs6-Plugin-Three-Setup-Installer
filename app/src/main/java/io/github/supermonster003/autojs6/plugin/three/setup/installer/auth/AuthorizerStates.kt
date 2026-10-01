package io.github.supermonster003.autojs6.plugin.three.setup.installer.auth

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import com.topjohnwu.superuser.Shell
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ThreeSetupInstallerPlugin
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import org.autojs.plugin.installer.api.InstallerErrorCodes
import rikka.shizuku.Shizuku
import java.io.Closeable
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** Process-scoped authorizers. State reads never create a shell or request permission. */
internal object AuthorizerStates {
    private class Authorizers(context: Context) {
        val shizuku = ShizukuAuthorizer(AndroidShizukuAccess(context)) {
            PrivilegedClient.invalidate(Authorizer.SHIZUKU)
        }
        val root = RootAuthorizer(AndroidRootAccess(context))
    }

    @Volatile private var shared: Authorizers? = null
    private fun get(context: Context) = shared ?: synchronized(this) {
        shared ?: Authorizers(context.applicationContext).also { shared = it }
    }

    fun states(context: Context): Map<Authorizer, AuthorizerState> = Authorizer.entries.associateWith { state(context, it) }

    fun state(context: Context, authorizer: Authorizer): AuthorizerState = when (authorizer) {
        Authorizer.NONE -> AuthorizerState(authorizer, available = true, running = true, granted = true)
        Authorizer.SHIZUKU -> get(context).shizuku.state()
        Authorizer.ROOT -> get(context).root.state()
    }

    /** Only a worker may wait for authorization; timeouts and interruption preserve their error codes. */
    fun request(context: Context, authorizer: Authorizer, timeoutMillis: Long): Boolean {
        require(timeoutMillis > 0)
        if (authorizer == Authorizer.NONE) return true
        check(Looper.myLooper() != Looper.getMainLooper()) { "Authorization must run on a worker" }
        try {
            return when (authorizer) {
                Authorizer.NONE -> true
                Authorizer.SHIZUKU -> get(context).shizuku.request(timeoutMillis)
                Authorizer.ROOT -> get(context).root.request(timeoutMillis)
            }
        } catch (failure: InterruptedException) {
            Thread.currentThread().interrupt()
            throw InstallFailure(InstallerErrorCodes.CANCELLED, "Authorization interrupted", cause = failure)
        }
    }

    private class AndroidShizukuAccess(private val context: Context) : ShizukuAccess {
        private val main = Handler(Looper.getMainLooper())
        override fun isInstalled() = runCatching {
            context.packageManager.getPackageInfo(ThreeSetupInstallerPlugin.SHIZUKU_PACKAGE_NAME, 0)
        }.isSuccess
        override fun isRunning() = Shizuku.pingBinder()
        override fun isPreV11() = Shizuku.isPreV11()
        override fun isGranted() = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        override fun shouldShowRationale() = Shizuku.shouldShowRequestPermissionRationale()
        override fun binderIdentity(): Any? = Shizuku.getBinder()

        override fun observeBinder(changed: () -> Unit): Closeable {
            val received = Shizuku.OnBinderReceivedListener(changed)
            val dead = Shizuku.OnBinderDeadListener(changed)
            Shizuku.addBinderReceivedListener(received)
            Shizuku.addBinderDeadListener(dead)
            return Closeable {
                Shizuku.removeBinderReceivedListener(received)
                Shizuku.removeBinderDeadListener(dead)
            }
        }

        override fun requestPermission(code: Int, result: (Int, Boolean) -> Unit): Closeable {
            val active = AtomicBoolean(true)
            val listener = Shizuku.OnRequestPermissionResultListener { returned, permission ->
                if (active.get()) result(returned, permission == PackageManager.PERMISSION_GRANTED)
            }
            Shizuku.addRequestPermissionResultListener(listener)
            val prompt = Runnable {
                if (active.get()) runCatching {
                    // Recheck after posting: the server may have changed before main runs this.
                    if (!Shizuku.pingBinder() || Shizuku.isPreV11()) result(code, false)
                    else Shizuku.requestPermission(code)
                }.onFailure { result(code, false) }
            }
            if (!main.post(prompt)) result(code, false)
            return Closeable {
                active.set(false)
                main.removeCallbacks(prompt)
                Shizuku.removeRequestPermissionResultListener(listener)
            }
        }
    }

    private class AndroidRootAccess(private val context: Context) : RootAccess {
        override fun hasSuBinary() = (listOf("/system/bin", "/system/xbin", "/sbin", "/su/bin",
            "/system/sbin", "/vendor/bin", "/odm/bin") + System.getenv("PATH").orEmpty().split(':'))
            .filter { it.isNotEmpty() }.distinct().any { File(it, "su").canExecute() }

        override fun granted(): Boolean? = Shell.getCachedShell()?.takeIf { it.isRoot }?.let { true }
            ?: Shell.isAppGrantedRoot()

        override fun request() = RootShellAccess.submit(context, { it.isRoot })
    }
}
