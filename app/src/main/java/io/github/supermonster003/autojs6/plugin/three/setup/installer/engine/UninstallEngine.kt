package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import android.content.Context
import android.content.Intent
import android.os.DeadObjectException
import android.os.Process
import android.os.SystemClock
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.PrivilegedClient
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.PrivilegedOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.UninstallDialogBridge
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes

/** Worker-side uninstall. Like installation, cancelling a committed operation cannot promise rollback. */
internal abstract class UninstallEngine(
    private val authorizer: Authorizer,
    private val currentUser: Int = Process.myUid() / 100000,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) {
    data class Result(val packageName: String, val authorizer: String)

    fun uninstall(request: UninstallRequest, userId: Int, listener: InstallEngine.Listener,
        checkCancelled: () -> Unit = {}, deadlineMillis: Long = Long.MAX_VALUE,
    ): Result {
        val flags = flags(request, authorizer, userId, currentUser)
        val deadline = minOf(deadlineMillis, clock() + request.timeoutMillis)
        val checkActive = {
            checkCancelled()
            if (Thread.currentThread().isInterrupted) throw InstallFailure(InstallerErrorCodes.CANCELLED, "Uninstallation interrupted")
            if (clock() >= deadline) throw InstallFailure(InstallerErrorCodes.TIMEOUT, "The uninstallation timed out")
        }
        try {
            checkActive()
            val status = perform(request, flags, userId, deadline, checkActive) { intent ->
                checkActive()
                if (request.interaction == InstallerContract.INTERACTION_SILENT) {
                    throw InstallFailure(InstallerErrorCodes.AUTHORIZER_REQUIRED, "The system requires confirmation for this silent uninstallation")
                }
                listener.onStage(InstallerContract.STAGE_CONFIRMING)
                listener.onUserAction(intent)
            }
            if (status.status != InstallStatusMapper.STATUS_SUCCESS) {
                throw InstallStatusMapper.toFailure(status.status, status.message, request.packageName, uninstall = true)
            }
            return Result(request.packageName, authorizer.id)
        } catch (failure: Exception) {
            if (failure is InterruptedException) Thread.currentThread().interrupt()
            val mapped = InstallFailure.from(failure, request.packageName)
            // The shared user-action launcher also serves installs; its generic launch failure
            // must retain the uninstall operation's public error vocabulary.
            if (mapped.code == InstallerErrorCodes.INSTALL_FAILED) {
                throw InstallFailure(InstallerErrorCodes.UNINSTALL_FAILED, mapped.message ?: "Uninstallation failed",
                    mapped.status, mapped.systemMessage, mapped.packageName, mapped)
            }
            throw mapped
        }
    }

    protected abstract fun perform(request: UninstallRequest, flags: Int, userId: Int, deadline: Long,
        checkActive: () -> Unit, onUserAction: (Intent) -> Unit): InstallStatusBridge.Status

    companion object {
        fun flags(request: UninstallRequest, authorizer: Authorizer, userId: Int, currentUser: Int): Int {
            RequestDocuments.packageNameOf(request.packageName, "uninstallation", required = true)
            RequestDocuments.timeoutOf(request.timeoutMillis, "uninstallation")
            RequestDocuments.userOf(request.user, "uninstallation")
            RequestDocuments.interactionOf(request.interaction, "uninstallation")
            if (request.authorizer != InstallerContract.AUTHORIZER_AUTO && request.authorizer != authorizer.id) {
                throw RequestDocuments.invalid("The selected engine does not match the requested authorizer")
            }
            if (!authorizer.privileged && (request.interaction == InstallerContract.INTERACTION_SILENT || request.privilegedOptions.isNotEmpty())) {
                throw InstallFailure(InstallerErrorCodes.AUTHORIZER_REQUIRED, "Silent uninstallation and privileged options require Shizuku or Root")
            }
            val expected = request.user.toIntOrNull() ?: currentUser
            if (userId < 0 || userId != expected) throw RequestDocuments.invalid("The resolved target user does not match the request")
            return (if (request.keepData) PrivilegedOptions.DELETE_KEEP_DATA else 0) or
                (if (request.user == InstallerContract.USER_ALL) PrivilegedOptions.DELETE_ALL_USERS else 0)
        }
    }
}

internal class PrivilegedUninstallEngine(context: Context, private val authorizer: Authorizer) : UninstallEngine(authorizer) {
    private val context = context.applicationContext
    init { require(authorizer.privileged) }

    override fun perform(request: UninstallRequest, flags: Int, userId: Int, deadline: Long,
        checkActive: () -> Unit, onUserAction: (Intent) -> Unit): InstallStatusBridge.Status {
        val remote = PrivilegedClient.get(context).acquire(authorizer,
            (deadline - SystemClock.elapsedRealtime()).coerceIn(1, PrivilegedClient.BIND_TIMEOUT_MILLIS))
        checkActive()
        return InstallStatusBridge.open(context).use { ticket ->
            remote.uninstall(request.packageName, flags, userId, ticket.sender)
            ticket.await(InstallerContract.DEFAULT_USER_ACTION_TIMEOUT_MILLIS, deadline, {
                checkActive()
                if (!remote.asBinder().isBinderAlive) throw DeadObjectException()
            }, onUserAction)
        }
    }
}

internal class NoneUninstallEngine(context: Context) : UninstallEngine(Authorizer.NONE) {
    private val context = context.applicationContext
    override fun perform(request: UninstallRequest, flags: Int, userId: Int, deadline: Long,
        checkActive: () -> Unit, onUserAction: (Intent) -> Unit): InstallStatusBridge.Status =
        nonePlatformCall {
            UninstallDialogBridge.await(context, request.packageName, deadline, checkActive, onUserAction)
        }
}
