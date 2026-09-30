package io.github.supermonster003.autojs6.plugin.three.setup.installer.priv

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import com.topjohnwu.superuser.ipc.RootService
import io.github.supermonster003.autojs6.plugin.three.setup.installer.BuildConfig
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.HiddenApiAccess
import rikka.shizuku.Shizuku
import java.io.Closeable

/** Main-thread owned binding. The caller handles permission prompts before bind(). */
internal class PrivilegedServiceBinding(
    private val context: Context,
    private val root: Boolean,
    private val connected: (IPrivilegedInstaller) -> Unit,
    private val disconnected: () -> Unit,
) : ServiceConnection, Closeable {
    private val token = Binder()
    private var bound = false
    private var serviceConnected = false
    private val args = Shizuku.UserServiceArgs(ComponentName(context, ShizukuUserService::class.java))
        .processNameSuffix("three-setup-installer-privileged").daemon(false)
        .version(BuildConfig.VERSION_CODE).debuggable(BuildConfig.DEBUG)

    fun bind() {
        check(Looper.myLooper() == Looper.getMainLooper())
        check(!bound) { "Already bound" }
        HiddenApiAccess.initialize()
        bound = true
        try {
            if (root) RootService.bind(Intent(context, RootInstallerService::class.java), this)
            else {
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
            serviceConnected = true
            connected(installer)
        } catch (failure: Exception) {
            close()
            disconnected()
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        if (bound && serviceConnected) {
            serviceConnected = false
            disconnected()
        }
    }

    override fun close() {
        check(Looper.myLooper() == Looper.getMainLooper())
        if (!bound) return
        bound = false
        if (root) RootService.unbind(this) else Shizuku.unbindUserService(args, this, true)
    }
}
