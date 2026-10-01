package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/** One dataSync service protects all active writers. It never recreates or replays an install. */
class InstallForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        InstallNotifications.onServiceCreated(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        InstallNotifications.refresh(this)
        return START_NOT_STICKY
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
