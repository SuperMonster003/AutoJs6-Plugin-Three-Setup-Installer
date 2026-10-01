package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ThreeSetupInstallerPlugin
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.UserActionLauncher
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.io.Closeable
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Non-exported owner of one PackageInstaller confirmation. Only an in-process ticket can provide
 * its system intent. Recreation reconnects to that ticket instead of launching another install.
 */
class UserActionActivity : Activity() {
    private var token: String? = null
    private var ticket: UserActionBridge.Ticket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra(UserActionBridge.EXTRA_TOKEN)
        val pending = id?.let { UserActionBridge.attach(it, this) }
        if (pending == null) { finish(); return }
        token = id
        ticket = pending
        perform(pending.start(canRequestInstalls()))
    }

    @Suppress("DEPRECATION")
    private fun perform(action: UserActionState.Action) {
        val pending = ticket ?: return
        try {
            when (action) {
                UserActionState.Action.REQUEST_PERMISSION -> startActivityForResult(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")), REQUEST_PERMISSION)
                UserActionState.Action.CONFIRM -> startActivityForResult(pending.confirmationForResult(), REQUEST_CONFIRMATION)
                UserActionState.Action.CANCELLED, UserActionState.Action.ACCEPTED, UserActionState.Action.RETURNED -> finish()
                UserActionState.Action.NONE -> Unit
            }
        } catch (failure: Exception) {
            pending.fail(InstallFailure(
                if (failure is SecurityException) InstallerErrorCodes.BLOCKED_BY_POLICY else InstallerErrorCodes.INSTALL_FAILED,
                "The system confirmation could not be opened", systemMessage = failure.message, cause = failure))
            finish()
        }
    }

    private fun canRequestInstalls(): Boolean {
        if (ticket?.requiresUnknownSourcesPermission != true || Build.VERSION.SDK_INT < 26) return true
        return try {
            packageManager.canRequestPackageInstalls()
        } catch (failure: Exception) {
            ticket?.fail(InstallFailure(InstallerErrorCodes.BLOCKED_BY_POLICY,
                "Permission to install from this source could not be checked", systemMessage = failure.message, cause = failure))
            finish()
            false
        }
    }

    @Deprecated("Platform activity result required for API 24 compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val pending = ticket ?: return
        when (requestCode) {
            REQUEST_PERMISSION -> perform(pending.permissionResult(canRequestInstalls()))
            REQUEST_CONFIRMATION -> {
                pending.confirmationResult(resultCode, data)
                finish()
            }
        }
    }

    override fun onDestroy() {
        token?.let { UserActionBridge.detach(it, this, isChangingConfigurations) }
        ticket = null
        super.onDestroy()
    }

    internal fun dismissConfirmation() {
        // The worker owns cancellation/deadlines. Dismiss both possible children without replay.
        runCatching { finishActivity(REQUEST_PERMISSION) }
        runCatching { finishActivity(REQUEST_CONFIRMATION) }
        // A system confirmation may open descendants that do not return to our request code.
        // Remove only the task rooted at this bridge, never a task that belongs to its caller.
        if (isTaskRoot) finishAndRemoveTask() else finish()
    }

    companion object {
        private const val REQUEST_PERMISSION = 1
        private const val REQUEST_CONFIRMATION = 2
    }
}

/**
 * Holds only active confirmation tickets. Callbacks enqueue into the installation's existing
 * status wait; they never wait for an Activity result on the worker's onUserAction callback.
 */
internal object UserActionBridge {
    const val EXTRA_TOKEN = "${ThreeSetupInstallerPlugin.PACKAGE_NAME}.extra.USER_ACTION_TOKEN"
    private val main = Handler(Looper.getMainLooper())
    private val tickets = ConcurrentHashMap<String, Ticket>()

    internal class Ticket(
        confirmation: Intent,
        val requiresUnknownSourcesPermission: Boolean,
        private val onReturned: () -> Unit,
        private val onFailure: (InstallFailure) -> Unit,
        val context: Context? = null,
    ) {
        private val confirmation = Intent(confirmation)
        private val state = UserActionState(requiresUnknownSourcesPermission)
        private val closed = AtomicBoolean()
        val isPending: Boolean get() = !closed.get() && state.pending
        @Volatile
        var activity = WeakReference<UserActionActivity>(null)
        val awaitingPlatformResult: Boolean get() = state.awaitingPlatformResult

        fun start(permissionGranted: Boolean): UserActionState.Action =
            if (closed.get()) UserActionState.Action.NONE else state.start(permissionGranted)

        fun permissionResult(granted: Boolean): UserActionState.Action {
            if (closed.get()) return UserActionState.Action.NONE
            val action = state.permissionResult(granted)
            if (action == UserActionState.Action.CANCELLED) {
                onFailure(InstallFailure(InstallerErrorCodes.USER_CANCELLED, "Permission to install from this source was not granted"))
            }
            return action
        }

        fun confirmationResult(resultCode: Int, data: Intent?) {
            if (closed.get()) return
            // Older AOSP installers finish a confirmed session without setResult(RESULT_OK).
            // RESULT_CANCELED is therefore ambiguous: only the platform status broadcast can
            // distinguish approval from rejection. Treat neither Activity result as install success.
            val action = if (resultCode == Activity.RESULT_CANCELED) state.confirmationDismissed()
                else state.confirmationResult(resultCode == Activity.RESULT_OK)
            when (action) {
                UserActionState.Action.ACCEPTED, UserActionState.Action.RETURNED -> onReturned()
                UserActionState.Action.CANCELLED -> {
                    val message = data?.takeIf { it.hasExtra(EXTRA_INSTALL_RESULT) }?.let {
                        "System confirmation result: ${it.getIntExtra(EXTRA_INSTALL_RESULT, 0)}"
                    }
                    onFailure(InstallFailure(
                        InstallerErrorCodes.INSTALL_FAILED, message ?: "The system confirmation failed", systemMessage = message))
                }
                else -> Unit
            }
        }

        fun fail(failure: InstallFailure) {
            if (!closed.get() && state.cancel()) onFailure(failure)
        }

        fun confirmationForResult(): Intent = Intent(confirmation).apply {
            // A result cannot be delivered when NEW_TASK or FORWARD_RESULT moves ownership away.
            flags = flags and (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_FORWARD_RESULT).inv()
        }

        fun close() {
            if (closed.compareAndSet(false, true)) state.close()
        }
    }

    internal class Handle(val token: String, val intent: Intent, private val ticket: Ticket) : Closeable {
        val awaitingPlatformResult: Boolean get() = ticket.awaitingPlatformResult

        override fun close() {
            if (!tickets.remove(token, ticket)) return
            ticket.close()
            ticket.context?.let { UserActionLauncher.dismissNotification(it, token) }
            main.post { ticket.activity.get()?.dismissConfirmation() }
        }
    }

    fun open(context: Context, confirmation: Intent, requiresUnknownSourcesPermission: Boolean,
        onReturned: () -> Unit, onFailure: (InstallFailure) -> Unit): Handle {
        val token = UUID.randomUUID().toString()
        val ticket = Ticket(confirmation, requiresUnknownSourcesPermission, onReturned, onFailure, context.applicationContext)
        tickets[token] = ticket
        return Handle(token, requireNotNull(activityIntent(context, token)), ticket)
    }

    /** Used by notification fallback after a background activity start was silently rejected. */
    fun activityIntent(context: Context, token: String): Intent? = tickets[token]?.takeIf { it.isPending }?.let {
        Intent(context, UserActionActivity::class.java).putExtra(EXTRA_TOKEN, token)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    }

    fun isAttached(token: String): Boolean = tickets[token]?.activity?.get()?.let { !it.isFinishing && !it.isDestroyed } == true

    fun attach(token: String, activity: UserActionActivity): Ticket? {
        val ticket = tickets[token] ?: return null
        val attached = synchronized(ticket) {
            if (!ticket.isPending) return@synchronized null
            val previous = ticket.activity.get()
            if (previous != null && previous !== activity && !previous.isChangingConfigurations &&
                !previous.isFinishing && !previous.isDestroyed) return@synchronized null
            ticket.activity = WeakReference(activity)
            ticket
        }
        if (attached != null) ticket.context?.let { UserActionLauncher.dismissNotification(it, token) }
        return attached
    }

    fun detach(token: String, activity: UserActionActivity, changingConfigurations: Boolean) {
        val ticket = tickets[token] ?: return
        synchronized(ticket) {
            if (ticket.activity.get() !== activity) return
            ticket.activity.clear()
            if (!changingConfigurations) ticket.fail(InstallFailure(InstallerErrorCodes.USER_CANCELLED, "The system confirmation was closed"))
        }
    }

    private const val EXTRA_INSTALL_RESULT = "android.intent.extra.INSTALL_RESULT"
}
