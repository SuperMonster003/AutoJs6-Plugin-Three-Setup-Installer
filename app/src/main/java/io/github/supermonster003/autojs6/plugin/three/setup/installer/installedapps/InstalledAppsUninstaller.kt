package io.github.supermonster003.autojs6.plugin.three.setup.installer.installedapps

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.PrivilegedClient
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.DeviceUsers
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallEngine
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.NoneUninstallEngine
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.PrivilegedUninstallEngine
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.UninstallEngine
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.UninstallRequest
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.UserActionLauncher
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.InstallerPreferences
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.PluginConfirmation
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

/** Local UI controller. Never enters the host-only Binder surface or requests authorization. */
internal class InstalledAppsUninstaller(context: Context) : Closeable {
    private val context = context.applicationContext
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val closed = AtomicBoolean()
    private val busy = AtomicBoolean()
    private var task: Future<*>? = null
    @Volatile private var listener: ((InstallFailure?) -> Unit)? = null

    val isBusy get() = busy.get()

    /** The only caller is the expanded row's explicit Uninstall action. Every request is confirmed. */
    fun start(packageName: String, onFinished: (InstallFailure?) -> Unit): Boolean {
        if (closed.get() || !busy.compareAndSet(false, true)) return false
        listener = onFinished
        task = worker.submit {
            val checkActive = {
                if (closed.get() || Thread.currentThread().isInterrupted) {
                    throw InstallFailure(InstallerErrorCodes.CANCELLED, "Uninstallation cancelled", packageName = packageName)
                }
            }
            val failure = try {
                checkActive()
                val selected = InstallerPreferences.resolveAuthorizer(context, InstallerContract.AUTHORIZER_AUTO)
                val request = UninstallRequest(packageName, false, InstallerContract.USER_CURRENT, selected.id,
                    InstallerContract.INTERACTION_DIALOG, TIMEOUT_MILLIS)
                val deadline = SystemClock.elapsedRealtime() + request.timeoutMillis
                // This page lists the current user's packages; a saved installation target cannot
                // silently redirect an uninstall to a different profile or to every user.
                val users = DeviceUsers(context)
                val userId = users.resolve(request.user, selected, PrivilegedClient.BIND_TIMEOUT_MILLIS)
                checkActive()
                UninstallEngine.flags(request, selected, userId, users.currentId)
                val approved = if (selected.privileged) {
                    PluginConfirmation.uninstall(context, request, selected, userId, deadline, checkActive)
                } else request
                checkActive()
                val engine = when {
                    selected == io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer.DHIZUKU ->
                        io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.DhizukuUninstallEngine(context)
                    selected.privileged -> PrivilegedUninstallEngine(context, selected)
                    else -> NoneUninstallEngine(context)
                }
                engine.uninstall(approved, userId, object : InstallEngine.Listener {
                    override fun onUserAction(intent: Intent) = UserActionLauncher.launch(context, intent)
                }, checkActive, deadline)
                null
            } catch (error: Exception) {
                if (error is InterruptedException) Thread.currentThread().interrupt()
                InstallFailure.from(error, packageName)
            }
            busy.set(false)
            main.post {
                if (!closed.get()) listener?.invoke(failure)
                listener = null
            }
        }
        return true
    }

    override fun close() {
        closed.set(true)
        listener = null
        task?.cancel(true)
        worker.shutdownNow()
        main.removeCallbacksAndMessages(null)
    }

    private companion object { const val TIMEOUT_MILLIS = 180_000L }
}
