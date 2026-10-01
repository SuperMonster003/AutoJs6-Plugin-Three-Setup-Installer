package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ThreeSetupInstallerPlugin
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.ConfirmationActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallNotifications
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.PluginConfirmation
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.UninstallDialogActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.UninstallDialogBridge
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.UserActionActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.UserActionBridge
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.io.Closeable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Receives the `PackageInstaller` status broadcasts of every session, privileged or not (roadmap
 * P2.3). The `IntentSender` handed to `commit` / `uninstall` is a mutable broadcast `PendingIntent`
 * aimed at this receiver, so the system fills in `EXTRA_STATUS`, `EXTRA_STATUS_MESSAGE`,
 * `EXTRA_PACKAGE_NAME` and, for a pending confirmation, `EXTRA_INTENT`.
 */
class InstallStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        InstallStatusBridge.deliver(intent)
    }
}

/**
 * Hands each status broadcast to the worker that committed the session. A [Ticket] owns one
 * `PendingIntent` with a unique action and request code (extras are not part of intent identity),
 * so concurrent sessions never collide.
 */
internal object InstallStatusBridge {

    private const val TAG = "InstallStatusBridge"
    private const val ACTION_PREFIX = "${ThreeSetupInstallerPlugin.PACKAGE_NAME}.action.INSTALL_STATUS."
    private const val EXTRA_TOKEN = "${ThreeSetupInstallerPlugin.PACKAGE_NAME}.extra.TOKEN"
    private const val POLL_MILLIS = 100L

    private val waiters = ConcurrentHashMap<String, LinkedBlockingQueue<Answer>>()
    private val requestCodes = AtomicInteger(0x3510)

    internal sealed interface Answer {
        data class Platform(val status: Status) : Answer
        data class Failure(val failure: InstallFailure) : Answer
        data object ConfirmationReturned : Answer
    }

    /** One platform status answer. */
    internal data class Status(val status: Int, val message: String?, val packageName: String?, val userActionIntent: Intent?) {
        val pendingUserAction: Boolean get() = status == InstallStatusMapper.STATUS_PENDING_USER_ACTION
    }

    internal class Ticket(
        private val context: Context,
        private val token: String,
        private val pending: PendingIntent,
        private val queue: LinkedBlockingQueue<Answer>,
        private val requiresUnknownSourcesPermission: Boolean,
    ) : Closeable {
        val sender: IntentSender get() = pending.intentSender
        private var userAction: UserActionBridge.Handle? = null

        /**
         * Waits for the terminal status. A pending user action is handed to [onUserAction] (which
         * launches the system confirmation) and the wait continues under [userActionTimeoutMillis];
         * [deadlineMillis] (elapsed realtime) bounds the whole wait; [checkCancelled] is polled.
         */
        fun await(
            userActionTimeoutMillis: Long,
            deadlineMillis: Long,
            checkCancelled: () -> Unit,
            onUserAction: (Intent) -> Unit,
        ): Status {
            var userActionDeadline = -1L
            while (true) {
                // A definitive platform result already received must survive a later Activity
                // cancellation, session cancellation, or a timeout noticed on this poll.
                queuedTerminal()?.let { return it }
                checkCancelled()
                val now = SystemClock.elapsedRealtime()
                if (now >= deadlineMillis) throw InstallFailure(InstallerErrorCodes.TIMEOUT, "The session timed out while waiting for the package installer")
                if (userActionDeadline in 0..now && userAction?.awaitingPlatformResult != true) {
                    throw InstallFailure(InstallerErrorCodes.USER_ACTION_TIMEOUT, "The user did not answer the installation confirmation in time")
                }
                val answer = queue.poll(POLL_MILLIS, TimeUnit.MILLISECONDS) ?: continue
                queuedTerminal()?.let { return it }
                val status = when (answer) {
                    is Answer.Platform -> answer.status
                    is Answer.Failure -> throw answer.failure
                    Answer.ConfirmationReturned -> continue
                }
                if (!status.pendingUserAction) return status
                val confirmation = status.userActionIntent
                if (confirmation == null) {
                    throw InstallFailure(InstallerErrorCodes.INSTALL_FAILED, "The package installer asked for a confirmation without providing one", status = status.status)
                }
                if (userAction == null) {
                    userActionDeadline = SystemClock.elapsedRealtime() + userActionTimeoutMillis
                    val action = UserActionBridge.open(context, confirmation, requiresUnknownSourcesPermission,
                        onReturned = { queue.offer(Answer.ConfirmationReturned) },
                        onFailure = { queue.offer(Answer.Failure(it)) })
                    userAction = action
                    onUserAction(action.intent)
                }
            }
        }

        private fun queuedTerminal(): Status? {
            val answer = queue.firstOrNull { it is Answer.Platform && !it.status.pendingUserAction } as? Answer.Platform ?: return null
            queue.remove(answer)
            return answer.status
        }

        override fun close() {
            waiters.remove(token)
            userAction?.close()
            userAction = null
            runCatching { pending.cancel() }
        }
    }

    fun open(context: Context, requiresUnknownSourcesPermission: Boolean = false): Ticket {
        val token = UUID.randomUUID().toString()
        val queue = LinkedBlockingQueue<Answer>()
        waiters[token] = queue
        val intent = Intent(context, InstallStatusReceiver::class.java)
            .setAction(ACTION_PREFIX + token)
            .putExtra(EXTRA_TOKEN, token)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
        val pending = try {
            PendingIntent.getBroadcast(context, requestCodes.incrementAndGet(), intent, flags)
        } catch (failure: Exception) {
            waiters.remove(token, queue)
            throw failure
        }
        return Ticket(context.applicationContext, token, pending, queue, requiresUnknownSourcesPermission)
    }

    fun deliver(intent: Intent) {
        val token = intent.getStringExtra(EXTRA_TOKEN) ?: intent.action?.removePrefix(ACTION_PREFIX)
        val queue = token?.let(waiters::get)
        if (queue == null) {
            Log.w(TAG, "status for an unknown session: ${intent.action}")
            return
        }
        queue.offer(Answer.Platform(decode(intent)))
    }

    fun decode(intent: Intent): Status = Status(
        status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, InstallStatusMapper.STATUS_FAILURE),
        message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE),
        packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME),
        userActionIntent = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_INTENT)
        },
    )
}

/**
 * Starts the token-bound owner Activity supplied by the install/uninstall result bridge. It returns
 * immediately so the worker keeps consuming status broadcasts. The delegate remains replaceable
 * for foreground routing and notification fallback when background activity starts are restricted.
 */
internal object UserActionLauncher {

    private const val TAG = "UserActionLauncher"
    private const val FALLBACK_DELAY_MILLIS = 1_500L
    private val main = Handler(Looper.getMainLooper())
    private val notificationLock = Any()
    private val scheduled = ConcurrentHashMap<String, Runnable>()

    interface NotificationDelegate {
        fun notifyAction(context: Context, token: String, intent: Intent): Boolean
        fun dismissAction(context: Context, token: String)
    }

    @Volatile
    var notifications: NotificationDelegate = object : NotificationDelegate {
        override fun notifyAction(context: Context, token: String, intent: Intent): Boolean =
            InstallNotifications.notifyAction(context, token, intent)
        override fun dismissAction(context: Context, token: String) = InstallNotifications.dismissAction(context, token)
    }

    @Volatile
    var delegate: (Context, Intent) -> Boolean = { context, intent ->
        try {
            context.startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (failure: Exception) {
            Log.w(TAG, "cannot start the confirmation", failure)
            false
        }
    }

    fun launch(context: Context, intent: Intent) {
        val owner = owner(context, intent)
        if (!delegate(context, intent)) {
            if (owner != null) {
                if (owner.pendingIntent() == null || owner.isAttached()) return
                if (notifyIfUnattached(context, intent)) return
                if (owner.pendingIntent() == null || owner.isAttached()) return
            }
            throw InstallFailure(InstallerErrorCodes.INSTALL_FAILED, "The installation confirmation could not be shown")
        }
        if (owner == null || owner.pendingIntent() == null || owner.isAttached()) return
        val application = context.applicationContext
        val action = Intent(intent)
        val fallback = object : Runnable {
            override fun run() {
                if (scheduled.remove(owner.token, this)) notifyIfUnattached(application, action)
            }
        }
        scheduled.put(owner.token, fallback)?.let(main::removeCallbacks)
        main.postDelayed(fallback, FALLBACK_DELAY_MILLIS)
    }

    /** Also used by tests to exercise a silent background-start rejection without timing races. */
    fun notifyIfUnattached(context: Context, intent: Intent): Boolean {
        val owner = owner(context, intent) ?: return false
        return synchronized(notificationLock) {
            val pending = owner.pendingIntent() ?: return@synchronized false
            if (owner.isAttached()) return@synchronized false
            try {
                notifications.notifyAction(context.applicationContext, owner.token, pending)
            } catch (failure: Exception) {
                Log.w(TAG, "cannot post the pending confirmation", failure)
                false
            }
        }
    }

    fun dismissNotification(context: Context, token: String) {
        scheduled.remove(token)?.let(main::removeCallbacks)
        synchronized(notificationLock) {
            runCatching { notifications.dismissAction(context.applicationContext, token) }
        }
    }

    private class Owner(val token: String, val pendingIntent: () -> Intent?, val isAttached: () -> Boolean)

    private fun owner(context: Context, intent: Intent): Owner? {
        val component = intent.component ?: return null
        if (component.packageName != context.packageName) return null
        val application = context.applicationContext
        return when (component.className) {
            UserActionActivity::class.java.name -> intent.getStringExtra(UserActionBridge.EXTRA_TOKEN)?.let { token ->
                Owner(token, { UserActionBridge.activityIntent(application, token) }, { UserActionBridge.isAttached(token) })
            }
            UninstallDialogActivity::class.java.name -> intent.getStringExtra(UninstallDialogBridge.EXTRA_TOKEN)?.let { token ->
                Owner(token, { UninstallDialogBridge.activityIntent(application, token) }, { UninstallDialogBridge.isAttached(token) })
            }
            ConfirmationActivity::class.java.name -> intent.getStringExtra(PluginConfirmation.EXTRA_TOKEN)?.let { token ->
                Owner(token, { PluginConfirmation.activityIntent(application, token) }, { PluginConfirmation.isAttached(token) })
            }
            else -> null
        }
    }
}
