package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/** Protects active writers and notification-mode source grants. Never recreates or replays an install. */
class InstallForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        InstallNotifications.onServiceCreated(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ownsSourceGrants = intent?.clipData != null && intent.flags and
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0
        if (ownsSourceGrants) {
            val token = requireNotNull(intent).getStringExtra(InstallPresentation.EXTRA_TOKEN)
            val owner = token?.let(InstallPresentation::find)
            if (flags and START_FLAG_REDELIVERY != 0 || owner == null || !owner.notificationMode || owner.snapshot().terminal) {
                // A framework redelivery is permission bookkeeping, never permission to recreate
                // an installation. If idle, stop before NOT_STICKY could discard the StartItem
                // owning the grant. Other live writers keep their starts until shared shutdown;
                // stopSelf(startId) would also revoke earlier starts. Do not inspect source URIs.
                val keptForOtherWriters = InstallNotifications.rejectSourceGrantStart(this)
                android.util.Log.i("InstallNotification", "Discarding source-grant service start: redelivered=${flags and START_FLAG_REDELIVERY != 0} otherWriters=$keptForOtherWriters")
                return if (keptForOtherWriters) START_REDELIVER_INTENT else START_NOT_STICKY
            }
        }
        InstallNotifications.refresh(this)
        // On API 31/33, NOT_STICKY removes the delivered StartItem without removing its URI
        // owner. Keep grant-bearing starts until stopSelf clears them. If the process dies, the
        // guard above rejects the redelivery without opening a URI or starting an engine.
        return if (ownsSourceGrants) START_REDELIVER_INTENT else START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    internal fun publish(notification: Notification): Boolean = try {
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(InstallNotifications.FOREGROUND_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(InstallNotifications.FOREGROUND_ID, notification)
        }
        true
    } catch (_: Exception) {
        // This includes FGS start restrictions and a revoked/missing type permission. Notification
        // presentation must never turn a completed package installation into a failed result.
        InstallNotifications.onPromotionRejected(this)
        stopSelf()
        false
    }

    internal fun finishForeground() {
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    /** Android 15's cumulative dataSync budget must be acknowledged within a few seconds. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        finishForeground()
        InstallNotifications.onForegroundTimeout(this)
    }

    override fun onDestroy() {
        InstallNotifications.onServiceDestroyed(this)
        super.onDestroy()
    }
}
