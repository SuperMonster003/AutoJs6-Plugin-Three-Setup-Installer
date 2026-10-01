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
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallSession
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.AppearancePreferences
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.HostAppearanceReader
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.wrap
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Bounded state, with tombstones so a late progress callback cannot resurrect a completed writer. */
internal class InstallationNoticeRegistry<T>(private val maximum: Int = InstallerContract.MAX_CONCURRENT_SESSIONS) {
    data class Entry<T>(val token: String, val name: String, val stage: String, val progress: Float,
        val payload: T, val writingStarted: Boolean, val cancellationRequested: Boolean = false,
        val mandatory: Boolean = false)
    data class Change(val accepted: Boolean, val beganWriting: Boolean, val beganForeground: Boolean = beganWriting)
    private val entries = linkedMapOf<String, Entry<T>>()
    private val completed = linkedSetOf<String>()

    @Synchronized
    fun update(token: String, name: String, stage: String, progress: Float, payload: T, mandatory: Boolean = false): Change {
        val previous = entries[token]
        if (token in completed || (previous == null && entries.size >= maximum)) return Change(false, false)
        val beganWriting = previous?.writingStarted != true && stage in setOf(InstallerContract.STAGE_WRITING, InstallerContract.STAGE_OPTIMIZING)
        val required = mandatory || previous?.mandatory == true
        val beganForeground = previous?.let { !it.writingStarted && !it.mandatory } != false && (beganWriting || required)
        entries[token] = Entry(token, name, stage, if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f,
            payload, beganWriting || previous?.writingStarted == true, previous?.cancellationRequested == true, required)
        return Change(true, beganWriting, beganForeground)
    }

    @Synchronized
    fun finish(token: String): Entry<T>? {
        completed += token
        while (completed.size > 128) completed.remove(completed.first())
        return entries.remove(token)
    }

    @Synchronized
    fun writers(): List<Entry<T>> = entries.values.filter { it.writingStarted || it.mandatory }

    @Synchronized
    fun isFinished(token: String): Boolean = token in completed

    @Synchronized
    fun contains(token: String): Boolean = token in entries

    /** Marks cancellation before handing callbacks to the caller; repeated taps are idempotent. */
    @Synchronized
    fun cancel(token: String?): List<T> {
        val targets = entries.values.filter { (it.writingStarted || it.mandatory) && !it.cancellationRequested && (token == null || it.token == token) }
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

/** Optional notices tolerate denial; explicit notification interaction validates availability. */
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
            val record = InstallPresentation.find(token)
            val mandatory = record?.notificationMode == true
            val change = publications.serially {
                entries.update(token, displayName.take(256), stage, progress,
                    Payload(if (mandatory) null else openIntent?.let(::Intent), if (mandatory) requireNotNull(record)::cancel else cancel), mandatory)
            }
            if (!change.accepted) return
            if (change.beganForeground) startWanted.set(true)
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
                    val hasFollowUp = InstallPresentation.find(token)?.snapshot()?.items?.any { InstallFollowUpUi.summary(localized, it.result).isNotEmpty() } == true
                    val body = (if (entry.mandatory || hasFollowUp) notificationResult(localized, token) else null)
                        ?: message?.take(512) ?: localized.getString(if (success) R.string.notification_install_complete else R.string.notification_install_failed)
                    val builder = builder(localized)
                        .setSmallIcon(if (success) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
                        .setContentTitle(entry.name.ifBlank { localized.getString(R.string.app_name) })
                        .setContentText(body)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                        .setAutoCancel(true)
                        .setTimeoutAfter(600_000)
                        .setCategory(NotificationCompat.CATEGORY_STATUS)
                    if (!entry.mandatory) (openIntent ?: entry.payload.intent)?.let { builder.setContentIntent(activityPendingIntent(application, token, "result", it)) }
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
    fun notifyAction(context: Context, token: String, intent: Intent, title: String? = null, message: String? = null,
        cancelIntent: PendingIntent? = null): Boolean = runCatching {
        publications.action(token) {
            val application = context.applicationContext
            if (!notificationsAllowed(application)) return@action false
            val localized = localized(application)
            val body = message ?: localized.getString(R.string.notification_action_required_body)
            val builder = builder(localized)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(title ?: localized.getString(R.string.notification_action_required))
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(activityPendingIntent(application, token, "action", intent))
                .setAutoCancel(cancelIntent == null)
                .setOngoing(cancelIntent != null)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
            cancelIntent?.let { builder.addAction(0, localized.getString(R.string.action_cancel), it) }
            notify(application, actionTag(token), ACTION_ID, builder.build())
        }
    }.getOrDefault(false)

    /** No Activity is involved: the immutable approval action is tied to this exact item prompt. */
    fun confirm(context: Context, token: String, prompt: InstallPresentation.Prompt, target: InstallSession.Target) {
        requireAvailable(context)
        val posted = publications.action(token) {
            val localized = localized(context)
            val metadata = prompt.metadata
            val body = localized.getString(R.string.notification_install_prompt,
                metadata.packageName ?: metadata.label, metadata.versionName ?: metadata.versionCode?.toString() ?: "?", target.userId)
            val notice = builder(localized).setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(metadata.label)
                .setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setOngoing(true).setCategory(NotificationCompat.CATEGORY_STATUS)
                .addAction(0, localized.getString(R.string.action_install), InstallNotificationActionReceiver.approval(context, token, prompt.token))
                .addAction(0, localized.getString(R.string.action_cancel), cancelPendingIntent(context, token))
                .build()
            notify(context, actionTag(token), ACTION_ID, notice)
        }
        if (!posted) throw InstallFailure(InstallerErrorCodes.NOTIFICATION_UNAVAILABLE, "The installation-confirmation notification could not be posted")
    }

    /** Transfer only validated source grants before the external NoDisplay Activity finishes. */
    fun retainSourceGrants(context: Context, token: String, grants: Intent) {
        requireAvailable(context)
        val record = InstallPresentation.find(token)
        check(record?.notificationMode == true && !record.snapshot().terminal) { "No live notification installation owns these grants" }
        try {
            val intent = Intent(context, InstallForegroundService::class.java)
                .putExtra(InstallPresentation.EXTRA_TOKEN, token).apply {
                    clipData = grants.clipData
                    addFlags(grants.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
                }
            val started = if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
            if (started == null) throw IllegalStateException("The source-grant service was not started")
        } catch (failure: Exception) {
            throw InstallFailure(InstallerErrorCodes.NOTIFICATION_UNAVAILABLE,
                "The notification service could not retain package-source access", systemMessage = failure.message, cause = failure)
        }
    }

    fun requireAvailable(context: Context) {
        if (!available(context)) throw InstallFailure(InstallerErrorCodes.NOTIFICATION_UNAVAILABLE,
            localized(context).getString(R.string.notification_unavailable))
    }

    /** Read-only channel policy shared by installation and isolated channel acceptance checks. */
    internal fun requireChannelAvailable(context: Context, channelId: String) {
        if (!channelAvailable(context, channelId)) throw InstallFailure(InstallerErrorCodes.NOTIFICATION_UNAVAILABLE,
            localized(context).getString(R.string.notification_unavailable))
    }

    internal fun available(context: Context): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= 26 && manager(context).getNotificationChannel(CHANNEL_ID) == null) ensureChannel(context)
        channelAvailable(context, CHANNEL_ID)
    }.getOrDefault(false)

    private fun channelAvailable(context: Context, channelId: String): Boolean = runCatching {
        (Build.VERSION.SDK_INT < 26 || manager(context).getNotificationChannel(channelId) != null) && notificationsAllowed(context, channelId)
    }.getOrDefault(false)

    private fun notificationResult(context: Context, token: String): String? {
        val snapshot = InstallPresentation.find(token)?.snapshot() ?: return null
        return buildList {
            snapshot.items.forEachIndexed { index, item ->
                val result = item.result
                val status = if (result?.get(InstallerContract.FIELD_OK)?.asBoolean == true) context.getString(R.string.install_success)
                    else result?.getAsJsonObject(InstallerContract.FIELD_ERROR)?.get(InstallerContract.FIELD_ERROR_CODE)?.asString
                        ?: context.getString(R.string.install_failed)
                add(context.getString(R.string.install_item_status, index + 1, item.metadata?.label ?: item.displayName, status))
                addAll(InstallFollowUpUi.summary(context, result))
            }
            snapshot.failure?.let { add(context.getString(R.string.install_error_code, it.code)) }
        }.joinToString("\n").take(4_096)
    }

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
        val writers = entries.writers()
        if (writers.isEmpty()) {
            created.finishForeground()
            return@serially
        }
        val notification = runCatching { foregroundNotification(created, writers) }.getOrNull()
        if (notification != null && created.publish(notification)) {
            service = created
            startWanted.set(false)
        } else {
            created.finishForeground()
        }
    }

    internal fun onServiceDestroyed(destroyed: InstallForegroundService) {
        if (service === destroyed) {
            service = null
            startPending = false
            reportSourceServiceLoss()
        }
        // Do not loop on FGS restrictions; the next user-visible resume explicitly permits a retry.
        refresh(destroyed)
    }

    /** Never revoke another live start's grants merely because a stale start was delivered. */
    internal fun rejectSourceGrantStart(created: InstallForegroundService): Boolean = publications.serially {
        if (entries.writers().isNotEmpty()) {
            refresh(created)
            true
        } else {
            if (service === created) service = null
            created.finishForeground()
            false
        }
    }

    internal fun onPromotionRejected(rejected: InstallForegroundService) {
        if (service === rejected) service = null
        startPending = false
        startWanted.set(false)
        reportSourceServiceLoss()
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

    private fun reportSourceServiceLoss() {
        entries.writers().forEach { InstallPresentation.find(it.token)?.onNotificationServiceLost() }
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
        val showOptionalProgress = io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.InstallerPreferences.read(context).progressNotifications
        if (!startPending && notificationsAllowed(context)) {
            writers.forEach { entry ->
                if (showOptionalProgress || entry.mandatory) notify(context, progressTag(entry.token), PROGRESS_ID, progressNotification(context, entry, fallback = true))
                else cancelNotice(context, progressTag(entry.token), PROGRESS_ID)
            }
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
        InstallerContract.STAGE_OPTIMIZING -> R.string.install_optimizing
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
                if (token != null && !entries.contains(token)) remove(context, token) else cancel(token)
                refresh(context)
            }
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            manager(context).createNotificationChannel(NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_installation), NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun notificationsAllowed(context: Context, channelId: String = CHANNEL_ID): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) false
        else if (!manager(context).areNotificationsEnabled()) false
        else if (Build.VERSION.SDK_INT < 26) true
        else {
            val channel = manager(context).getNotificationChannel(channelId)
            channel?.importance != NotificationManager.IMPORTANCE_NONE &&
                (Build.VERSION.SDK_INT < 28 || channel?.group == null || manager(context).getNotificationChannelGroup(channel.group)?.isBlocked != true)
        }
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
