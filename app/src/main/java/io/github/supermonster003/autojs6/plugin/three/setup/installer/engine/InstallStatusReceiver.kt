package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.SystemClock
import android.util.Log
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ThreeSetupInstallerPlugin
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
    private const val POLL_MILLIS = 500L

    private val waiters = ConcurrentHashMap<String, LinkedBlockingQueue<Intent>>()
    private val requestCodes = AtomicInteger(0x3510)

    /** One platform status answer. */
    internal data class Status(val status: Int, val message: String?, val packageName: String?, val userActionIntent: Intent?) {
        val pendingUserAction: Boolean get() = status == InstallStatusMapper.STATUS_PENDING_USER_ACTION
    }

    internal class Ticket(private val token: String, private val pending: PendingIntent, private val queue: LinkedBlockingQueue<Intent>) : Closeable {
        val sender: IntentSender get() = pending.intentSender

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
                checkCancelled()
                val now = SystemClock.elapsedRealtime()
                if (now >= deadlineMillis) throw InstallFailure(InstallerErrorCodes.TIMEOUT, "The session timed out while waiting for the package installer")
                if (userActionDeadline in 0..now) throw InstallFailure(InstallerErrorCodes.USER_ACTION_TIMEOUT, "The user did not answer the installation confirmation in time")
                val intent = queue.poll(POLL_MILLIS, TimeUnit.MILLISECONDS) ?: continue
                val status = decode(intent)
                if (!status.pendingUserAction) return status
                val userAction = status.userActionIntent
                if (userAction == null) {
                    throw InstallFailure(InstallerErrorCodes.INSTALL_FAILED, "The package installer asked for a confirmation without providing one", status = status.status)
                }
                if (userActionDeadline < 0) {
                    userActionDeadline = now + userActionTimeoutMillis
                    onUserAction(userAction)
                }
            }
        }

        override fun close() {
            waiters.remove(token)
            runCatching { pending.cancel() }
        }
    }

    fun open(context: Context): Ticket {
        val token = UUID.randomUUID().toString()
        val queue = LinkedBlockingQueue<Intent>()
        waiters[token] = queue
        val intent = Intent(context, InstallStatusReceiver::class.java)
            .setAction(ACTION_PREFIX + token)
            .putExtra(EXTRA_TOKEN, token)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
        val pending = PendingIntent.getBroadcast(context, requestCodes.incrementAndGet(), intent, flags)
        return Ticket(token, pending, queue)
    }

    fun deliver(intent: Intent) {
        val token = intent.getStringExtra(EXTRA_TOKEN) ?: intent.action?.removePrefix(ACTION_PREFIX)
        val queue = token?.let(waiters::get)
        if (queue == null) {
            Log.w(TAG, "status for an unknown session: ${intent.action}")
            return
        }
        queue.offer(intent)
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
 * Starts the system confirmation of a `none` installation or uninstallation. Roadmap P3.3 replaces
 * the delegate with `UserActionActivity` (a task of its own, cancel result) and P3.4 adds the
 * notification fallback for the case the plugin may not start activities from the background.
 */
internal object UserActionLauncher {

    private const val TAG = "UserActionLauncher"

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
        if (!delegate(context, intent)) {
            throw InstallFailure(InstallerErrorCodes.INSTALL_FAILED, "The installation confirmation could not be shown")
        }
    }
}
