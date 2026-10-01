package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import io.github.supermonster003.autojs6.plugin.three.setup.installer.R
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.AppearancePreferences
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.HostAppearanceReader
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.wrap
import org.autojs.plugin.installer.api.InstallerContract
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Bounded state, with tombstones so a late progress callback cannot resurrect a completed writer. */
internal class InstallationNoticeRegistry<T>(private val maximum: Int = InstallerContract.MAX_CONCURRENT_SESSIONS) {
    data class Entry<T>(val token: String, val name: String, val stage: String, val progress: Float,
        val payload: T, val writingStarted: Boolean, val cancellationRequested: Boolean = false)
    data class Change(val accepted: Boolean, val beganWriting: Boolean)
    private val entries = linkedMapOf<String, Entry<T>>()
    private val completed = linkedSetOf<String>()

    @Synchronized
    fun update(token: String, name: String, stage: String, progress: Float, payload: T): Change {
        val previous = entries[token]
        if (token in completed || (previous == null && entries.size >= maximum)) return Change(false, false)
        val beganWriting = previous?.writingStarted != true && stage == InstallerContract.STAGE_WRITING
        entries[token] = Entry(token, name, stage, if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f,
            payload, beganWriting || previous?.writingStarted == true, previous?.cancellationRequested == true)
        return Change(true, beganWriting)
    }

    @Synchronized
    fun finish(token: String): Entry<T>? {
        completed += token
        while (completed.size > 128) completed.remove(completed.first())
        return entries.remove(token)
    }

    @Synchronized
    fun writers(): List<Entry<T>> = entries.values.filter { it.writingStarted }

    @Synchronized
    fun isFinished(token: String): Boolean = token in completed

    /** Marks cancellation before handing callbacks to the caller; repeated taps are idempotent. */
    @Synchronized
    fun cancel(token: String?): List<T> {
        val targets = entries.values.filter { it.writingStarted && !it.cancellationRequested && (token == null || it.token == token) }
        targets.forEach { entries[it.token] = it.copy(cancellationRequested = true) }
        return targets.map { it.payload }
    }
}

/**
 * Serializes publication with ownership removal. Completion is synchronous: no queued closure can
 * outlive remove(), and the registry's existing bounded terminal records reject late action notices.
 */
internal class InstallationNoticePublications<T>(private val entries: InstallationNoticeRegistry<T>) {
    private val lock = Any()

    fun complete(token: String, publish: (InstallationNoticeRegistry.Entry<T>) -> Unit): Boolean = synchronized(lock) {
        val entry = entries.finish(token) ?: return@synchronized false
        publish(entry)
        true
    }

    fun remove(token: String, cancel: () -> Unit) = synchronized(lock) {
        entries.finish(token)
        cancel()
    }

    fun action(token: String, publish: () -> Boolean): Boolean = synchronized(lock) {
        if (entries.isFinished(token)) false else publish()
    }

    fun <R> serially(action: () -> R): R = synchronized(lock, action)
}

/** All entry points tolerate notification denial and may be called from an installation worker. */
internal object InstallNotifications {
    const val CHANNEL_ID = "installation"
    const val FOREGROUND_ID = 301
    private const val PROGRESS_ID = 302
    private const val RESULT_ID = 303
    private const val ACTION_ID = 304
    private const val CANCEL_ACTION = "io.github.supermonster003.autojs6.plugin.three.setup.installer.CANCEL_NOTICE"
    private const val EXTRA_TOKEN = "noticeToken"
    private const val EXTRA_CANCEL_ALL = "cancelAll"
    private const val UPDATE_INTERVAL_MILLIS = 250L
    private data class Payload(val intent: Intent?, val cancel: () -> Unit)
    private val entries = InstallationNoticeRegistry<Payload>()
    private val publications = InstallationNoticePublications(entries)
    private val main = Handler(Looper.getMainLooper())
    private val queued = AtomicBoolean()
    private val startWanted = AtomicBoolean()
    private val cancellationWorker = Executors.newSingleThreadExecutor { task -> Thread(task, "installer-notification-cancel").apply { isDaemon = true } }
    // All Service references and start flags below are accessed on the main thread only.
    private var service: InstallForegroundService? = null
    private var startPending = false

    fun update(context: Context, token: String, displayName: String, stage: String, progress: Float,
        openIntent: Intent? = null, cancel: () -> Unit) {
        runCatching {
            val change = entries.update(token, displayName.take(256), stage, progress, Payload(openIntent?.let(::Intent), cancel))
            if (!change.accepted) return
            if (change.beganWriting) startWanted.set(true)
            refresh(context)
        }
    }

    /** Call once with the actual engine result. UI/notification failures cannot change that result. */
    fun complete(context: Context, token: String, success: Boolean, message: String? = null, openIntent: Intent? = null) {
        runCatching {
            val application = context.applicationContext
            publications.complete(token) { entry ->
                cancelNotice(application, actionTag(token), ACTION_ID)
                cancelNotice(application, progressTag(token), PROGRESS_ID)
                if (notificationsAllowed(application)) {
                    val localized = localized(application)
                    val body = message?.take(512) ?: localized.getString(if (success) R.string.notification_install_complete else R.string.notification_install_failed)
                    val builder = builder(localized)
                        .setSmallIcon(if (success) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
                        .setContentTitle(entry.name.ifBlank { localized.getString(R.string.app_name) })
                        .setContentText(body)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                        .setAutoCancel(true)
                        .setTimeoutAfter(600_000)
                        .setCategory(NotificationCompat.CATEGORY_STATUS)
                    (openIntent ?: entry.payload.intent)?.let { builder.setContentIntent(activityPendingIntent(application, token, "result", it)) }
                    notify(application, resultTag(token), RESULT_ID, builder.build())
                }
            }
        }
        refresh(context)
    }

    /** Ownership cleanup without claiming an installation result. */
    fun remove(context: Context, token: String) {
        runCatching {
            publications.remove(token) {
                cancelNotice(context, actionTag(token), ACTION_ID)
                cancelNotice(context, progressTag(token), PROGRESS_ID)
                cancelNotice(context, resultTag(token), RESULT_ID)
            }
        }
        refresh(context)
    }

    /** A direct Activity PendingIntent avoids notification trampolines and grants only a user tap. */
    fun notifyAction(context: Context, token: String, intent: Intent, title: String? = null, message: String? = null): Boolean = runCatching {
        publications.action(token) {
            val application = context.applicationContext
            if (!notificationsAllowed(application)) return@action false
            val localized = localized(application)
            val body = message ?: localized.getString(R.string.notification_action_required_body)
            val notification = builder(localized)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(title ?: localized.getString(R.string.notification_action_required))
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(activityPendingIntent(application, token, "action", intent))
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .build()
            notify(application, actionTag(token), ACTION_ID, notification)
        }
    }.getOrDefault(false)

    fun dismissAction(context: Context, token: String) {
        publications.serially { cancelNotice(context, actionTag(token), ACTION_ID) }
    }

    /** A screen's onResume may retry only the service after a notification tap; no task is replayed. */
    fun resumeForeground(context: Context) {
        startWanted.set(true)
        refresh(context)
    }

    internal fun refresh(context: Context) {
        val application = context.applicationContext
        if (queued.compareAndSet(false, true)) {
            main.postDelayed({
                queued.set(false)
                runCatching { render(application) }
            }, UPDATE_INTERVAL_MILLIS)
        }
    }

    internal fun onServiceCreated(created: InstallForegroundService) = publications.serially {
        startPending = false
        val notification = runCatching { foregroundNotification(created, entries.writers()) }.getOrNull()
        if (notification != null && created.publish(notification)) {
            service = created
            startWanted.set(false)
        } else {
            created.finishForeground()
        }
    }

    internal fun onServiceDestroyed(destroyed: InstallForegroundService) {
        if (service === destroyed) service = null
        startPending = false
        // Do not loop on FGS restrictions; the next user-visible resume explicitly permits a retry.
        refresh(destroyed)
    }

    internal fun onPromotionRejected(rejected: InstallForegroundService) {
        if (service === rejected) service = null
        startPending = false
        startWanted.set(false)
        refresh(rejected)
    }

    internal fun onForegroundTimeout(timedOut: InstallForegroundService) {
        if (service === timedOut) service = null
        startPending = false
        startWanted.set(false)
        // The service has already stopped. Cancellation is asynchronous and the engine's confirmed
        // install-result boundary still wins over a late cancellation.
        cancel(null)
        refresh(timedOut)
    }

    private fun render(context: Context) = publications.serially { renderLocked(context) }

    private fun renderLocked(context: Context) {
        val writers = entries.writers()
        if (writers.isEmpty()) {
            startWanted.set(false)
            service?.let { active -> service = null; active.finishForeground() }
            return
        }
        val active = service
        if (active != null) {
            if (active.publish(foregroundNotification(context, writers))) {
                writers.forEach { manager(context).cancel(progressTag(it.token), PROGRESS_ID) }
            }
            return
        }
        if (!startPending && startWanted.getAndSet(false)) {
            try {
                val intent = Intent(context, InstallForegroundService::class.java)
                startPending = true
                val started = if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
                if (started != null) return
                startPending = false
            } catch (_: Exception) {
                // Android 12+ may deny a background start. Keep the engine's result authoritative,
                // show a regular notification if permitted, and retry only after a user opens UI.
                startPending = false
            }
        }
        if (!startPending && notificationsAllowed(context)) {
            writers.forEach { entry -> notify(context, progressTag(entry.token), PROGRESS_ID, progressNotification(context, entry, fallback = true)) }
        }
    }

    private fun foregroundNotification(context: Context, writers: List<InstallationNoticeRegistry.Entry<Payload>>): Notification {
        if (writers.size == 1) return progressNotification(context, writers.single(), fallback = false)
        val localized = localized(context)
        val builder = builder(localized).setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(localized.getString(if (writers.isEmpty()) R.string.notification_installing else R.string.notification_active_count, writers.size))
            .setContentText(writers.firstOrNull()?.let { "${it.name}: ${stage(localized, it.stage)}" } ?: localized.getString(R.string.notification_preparing))
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (writers.isNotEmpty()) {
            builder.setProgress(100, (writers.map { it.progress }.average() * 100).toInt(), writers.none { it.stage == InstallerContract.STAGE_WRITING })
            writers.firstNotNullOfOrNull { entry -> entry.payload.intent?.let { entry.token to it } }?.let { (token, intent) ->
                builder.setContentIntent(activityPendingIntent(context, token, "progress", intent))
            }
            builder.addAction(0, localized.getString(R.string.notification_cancel_all), cancelPendingIntent(context, null))
        }
        return builder.build()
    }

    private fun progressNotification(context: Context, entry: InstallationNoticeRegistry.Entry<Payload>, fallback: Boolean): Notification {
        val localized = localized(context)
        val body = if (fallback && entry.payload.intent != null) localized.getString(R.string.notification_open_for_progress) else stage(localized, entry.stage)
        val builder = builder(localized).setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(entry.name.ifBlank { localized.getString(R.string.notification_installing) })
            .setContentText(body)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setProgress(100, (entry.progress * 100).toInt(), entry.stage != InstallerContract.STAGE_WRITING)
        entry.payload.intent?.let { builder.setContentIntent(activityPendingIntent(context, entry.token, "progress", it)) }
        if (!entry.cancellationRequested) builder.addAction(0, localized.getString(R.string.action_cancel), cancelPendingIntent(context, entry.token))
        return builder.build()
    }

    private fun stage(context: Context, stage: String) = context.getString(when (stage) {
        InstallerContract.STAGE_WRITING -> R.string.notification_writing
        InstallerContract.STAGE_COMMITTING -> R.string.notification_installing
        InstallerContract.STAGE_CONFIRMING -> R.string.notification_action_required
        else -> R.string.notification_preparing
    })

    private fun builder(context: Context): NotificationCompat.Builder {
        ensureChannel(context)
        return NotificationCompat.Builder(context, CHANNEL_ID).setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW).setShowWhen(false).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
    }

    internal fun activityPendingIntent(context: Context, token: String, kind: String, source: Intent): PendingIntent {
        require(source.component?.packageName == context.packageName) { "Notification activities must belong to the installer" }
        val intent = Intent(source).addCategory("${context.packageName}.notice.$kind.$token")
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun cancelPendingIntent(context: Context, token: String?): PendingIntent {
        val intent = Intent(context, CancelReceiver::class.java).setAction(CANCEL_ACTION)
            .addCategory("${context.packageName}.notice.cancel.${token ?: "all"}")
            .putExtra(EXTRA_TOKEN, token).putExtra(EXTRA_CANCEL_ALL, token == null)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun cancel(token: String?) {
        entries.cancel(token).forEach { payload -> cancellationWorker.execute { runCatching(payload.cancel) } }
    }

    /** Explicit and non-exported in the Manifest. It only cancels current in-memory sessions. */
    class CancelReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != CANCEL_ACTION) return
            val token = intent.getStringExtra(EXTRA_TOKEN)
            if (token != null || intent.getBooleanExtra(EXTRA_CANCEL_ALL, false)) {
                cancel(token)
                refresh(context)
            }
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            manager(context).createNotificationChannel(NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_installation), NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun notificationsAllowed(context: Context): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) false
        else manager(context).areNotificationsEnabled() && (Build.VERSION.SDK_INT < 26 || manager(context).getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE)
    }.getOrDefault(false)

    @SuppressLint("MissingPermission") // The permission and channel are checked; denial never escapes.
    private fun notify(context: Context, tag: String, id: Int, notification: Notification): Boolean = runCatching {
        if (!notificationsAllowed(context)) return false
        manager(context).notify(tag, id, notification)
        true
    }.getOrDefault(false)

    private fun cancelNotice(context: Context, tag: String, id: Int) {
        runCatching { manager(context).cancel(tag, id) }
    }

    private fun localized(context: Context) = AppearancePreferences.resolve(context, HostAppearanceReader.cached()).wrap(context)
    private fun manager(context: Context) = context.getSystemService(NotificationManager::class.java)
    private fun progressTag(token: String) = "installation.progress:$token"
    private fun resultTag(token: String) = "installation.result:$token"
    private fun actionTag(token: String) = "installation.action:$token"
}
