package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Explicit, non-exported notification actions. An old token never recreates an operation. */
class InstallNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val token = intent.getStringExtra(EXTRA_TOKEN) ?: return
        when (intent.action) {
            ACTION_APPROVE -> {
                val prompt = intent.getStringExtra(EXTRA_PROMPT) ?: return
                val record = InstallPresentation.find(token)
                if (record == null) InstallNotifications.remove(context, token)
                else if (record.acceptFromNotification(prompt)) InstallNotifications.resumeForeground(context)
            }
            ACTION_CANCEL_SYSTEM -> {
                UserActionBridge.cancelFromNotification(token)
                InstallNotifications.dismissAction(context, token)
            }
        }
    }

    companion object {
        private const val PREFIX = "io.github.supermonster003.autojs6.plugin.three.setup.installer.notification."
        private const val ACTION_APPROVE = PREFIX + "APPROVE"
        private const val ACTION_CANCEL_SYSTEM = PREFIX + "CANCEL_SYSTEM"
        private const val EXTRA_TOKEN = "notificationOwner"
        private const val EXTRA_PROMPT = "notificationPrompt"

        internal fun approval(context: Context, token: String, prompt: String): PendingIntent =
            pending(context, ACTION_APPROVE, token, prompt)

        internal fun cancelSystem(context: Context, token: String): PendingIntent =
            pending(context, ACTION_CANCEL_SYSTEM, token, null)

        private fun pending(context: Context, action: String, token: String, prompt: String?): PendingIntent {
            // The presentation token AND per-item prompt identify the action, not its extras or
            // hash code. A repeated/late approval cannot approve the next item in a batch.
            val intent = Intent(context, InstallNotificationActionReceiver::class.java)
                .setAction(action)
                .setData(Uri.Builder().scheme("three-setup-notification").authority("action")
                    .appendPath(action).appendPath(token).appendPath(prompt ?: "system").build())
                .putExtra(EXTRA_TOKEN, token).putExtra(EXTRA_PROMPT, prompt)
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
