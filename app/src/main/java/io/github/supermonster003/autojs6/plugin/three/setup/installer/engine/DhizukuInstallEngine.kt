package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Process
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.DhizukuFramework
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PlannedApk
import org.autojs.plugin.installer.api.InstallerContract
import java.io.OutputStream

/** Streaming, verification, cancellation and status handling are shared with the other transports. */
internal class DhizukuInstallEngine(context: Context) : SessionInstallEngine(Authorizer.DHIZUKU) {
    private val context = context.applicationContext
    override fun afterInstallation(request: InstallEngine.Request, packageName: String,
        deadlineMillis: Long, checkActive: () -> Unit): InstallFollowUp =
        InstallFollowUp.readOwner(context, packageName, request.options.requestUpdateOwnership)
    override fun openSession(request: InstallEngine.Request, parameters: Parameters, deadlineMillis: Long): Session {
        check(android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) { "Installation must run on a worker" }
        val framework = DhizukuFramework(context)
        DhizukuPolicy.install(request.options, Process.myUid() / 100000, framework.owner.packageName)
        val journal = DhizukuSessionJournal(context)
        journal.recover(framework)
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setSize(parameters.totalBytes)
            request.prepared.packageName?.let(::setAppPackageName)
            if (Build.VERSION.SDK_INT >= 31) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            AdvancedSessionParameters.apply(this, request.options)
        }
        val lease = journal.create(framework, params, requireNotNull(request.prepared.packageName), parameters.totalBytes)
        val id = lease.record.id
        var opened: PackageInstaller.Session? = null
        try {
            val session = framework.openSession(id).also { opened = it }
            val ticket = InstallStatusBridge.open(context)
            return object : Session {
                override fun openWrite(apk: PlannedApk, checkActive: () -> Unit): OutputStream {
                    checkActive()
                    return framework.checked { session.openWrite(apk.name, 0, apk.size) }
                }
                override fun fsync(output: OutputStream) = framework.checked { session.fsync(output) }
                override fun commit() = framework.checked { session.commit(ticket.sender) }
                override fun await(deadlineMillis: Long, checkActive: () -> Unit, onUserAction: (Intent) -> Unit) =
                    ticket.await(InstallerContract.DEFAULT_USER_ACTION_TIMEOUT_MILLIS, deadlineMillis, {
                        checkActive(); framework.checked { Unit }
                    }, onUserAction).also { lease.completed() }
                override fun abandon() = lease.abandon(framework)
                override fun close() {
                    try { framework.checked { session.close() } }
                    finally { try { ticket.close() } finally { lease.close() } }
                }
            }
        } catch (failure: Exception) {
            runCatching { opened?.let { session -> framework.checked { session.close() } } }.onFailure(failure::addSuppressed)
            runCatching { lease.abandon(framework) }.onFailure(failure::addSuppressed)
            lease.close()
            throw failure
        }
    }
}

internal class DhizukuUninstallEngine(context: Context) : UninstallEngine(Authorizer.DHIZUKU) {
    private val context = context.applicationContext
    override fun perform(request: UninstallRequest, flags: Int, userId: Int, deadline: Long,
        checkActive: () -> Unit, onUserAction: (Intent) -> Unit): InstallStatusBridge.Status {
        DhizukuPolicy.uninstall(request, Process.myUid() / 100000)
        val framework = DhizukuFramework(context)
        DhizukuSessionJournal(context).recover(framework)
        checkActive()
        return InstallStatusBridge.open(context).use { ticket ->
            framework.checked { framework.installer.uninstall(request.packageName, ticket.sender) }
            ticket.await(InstallerContract.DEFAULT_USER_ACTION_TIMEOUT_MILLIS, deadline, {
                checkActive(); framework.checked { Unit }
            }, onUserAction)
        }
    }
}
