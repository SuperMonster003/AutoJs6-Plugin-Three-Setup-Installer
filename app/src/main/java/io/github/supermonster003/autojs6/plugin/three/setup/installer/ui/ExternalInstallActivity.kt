package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.*
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.ExternalSources
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** External input is limited to package URIs. The visible dialog owns the forwarded URI grants. */
class ExternalInstallActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (savedInstanceState == null) {
                val sources = ExternalSources.fromIntent(intent)
                io.github.supermonster003.autojs6.plugin.three.setup.installer.queue.InstallQueue.start(this, sources,
                    origin = InstallerContract.SOURCE_EXTERNAL,
                    isBatch = intent.action == android.content.Intent.ACTION_SEND_MULTIPLE || sources.uris.size > 1)
            }
        } catch (failure: Exception) {
            ExternalInstaller.showFailure(this, InstallFailure.from(failure))
        } finally { finish() }
    }
}

/** A confirmed AUTO item must revisit its split selection and signature review on manual retry. */
internal fun installRetryInteraction(requested: String, confirmedOptions: Boolean): String =
    if (confirmedOptions && requested == InstallerContract.INTERACTION_AUTO) InstallerContract.INTERACTION_DIALOG else requested

internal object ExternalInstaller {
    private val workers = Executors.newFixedThreadPool(InstallerContract.MAX_CONCURRENT_SESSIONS) { Thread(it, "external-install").apply { isDaemon = true } }
    private val sourceDeadlines = ScheduledThreadPoolExecutor(1) { Thread(it, "external-source-deadline").apply { isDaemon = true } }
        .apply { removeOnCancelPolicy = true }
    private val sourceCancellations = Executors.newFixedThreadPool(InstallerContract.MAX_CONCURRENT_SESSIONS) {
        Thread(it, "external-source-cancel").apply { isDaemon = true }
    }
    private val tasks = ConcurrentHashMap<String, Task>()

    fun start(context: Context, sources: ExternalSources, options: InstallOptions? = null,
        origin: String = InstallerContract.SOURCE_EXTERNAL, isBatch: Boolean = sources.uris.size > 1,
        interaction: String? = null): String {
        val defaults = io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.InstallerPreferences.read(context)
        val actualOptions = options ?: defaults.options
        val actualInteraction = interaction ?: defaults.interaction
        require(origin == InstallerContract.SOURCE_EXTERNAL || origin == InstallerContract.SOURCE_HOME)
        require(InstallerContract.isInteraction(actualInteraction))
        val slot = InstallSlots.acquire()
        try {
            val request = InstallRequest(UUID.randomUUID().toString(), sources.provisionalEntries(), actualInteraction, actualOptions,
                isBatch = isBatch, origin = origin, applySourceProfiles = true,
                explicitOptions = if (options == null) emptySet() else io.github.supermonster003.autojs6.plugin.three.setup.installer.profiles.InstallProfileOverrides.ALLOWED_KEYS)
            val task = Task(context.applicationContext, request, sources, slot)
            tasks[task.presentation.token] = task
            try {
                InstallationUi.show(context, task.presentation, sources.grantIntent)
                workers.execute(task::run)
            } catch (failure: Exception) {
                task.presentation.onFailed(InstallFailure.from(failure))
                task.dispose()
                throw failure
            }
            return task.presentation.token
        } catch (failure: Throwable) { slot.close(); throw failure }
    }

    fun showFailure(context: Context, failure: InstallFailure, origin: String = InstallerContract.SOURCE_EXTERNAL) {
        if (InstallDefaults.interaction(context) == InstallerContract.INTERACTION_NOTIFICATION) {
            // A disabled notification channel cannot display its own error. The external caller
            // has no result callback; show a brief explicit failure without opening a dialog.
            android.widget.Toast.makeText(context, "${failure.code}: ${failure.message}", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        val request = InstallRequest(UUID.randomUUID().toString(), listOf(SourceEntry(0, 0, "package", -1)),
            InstallerContract.INTERACTION_DIALOG, InstallOptions(), origin = origin)
        runCatching {
            InstallPresentation.create(context, request, InstallPresentation.Callbacks(cancel = {})).apply {
                onFailed(failure)
                show()
            }
        }
    }

    private class Task(private val context: Context, private val request: InstallRequest,
        private val sources: ExternalSources, private val slot: java.io.Closeable) {
        private val cancelled = AtomicBoolean()
        private val sourceCancellation = android.os.CancellationSignal()
        private val sourceCancellationRequested = AtomicBoolean()
        private val finished = AtomicBoolean()
        private val disposed = AtomicBoolean()
        @Volatile private var core: InstallSession? = null
        @Volatile private var worker: Thread? = null
        private val workerLock = Any()
        private val deadline = SystemClock.elapsedRealtime() + request.options.timeoutMillis
        val presentation = InstallPresentation.create(context, request, InstallPresentation.Callbacks(
            cancel = ::cancel,
            retry = { index ->
                if (!finished.get() || disposed.get() || index !in sources.uris.indices) false else {
                    start(context, sources.single(index), retryOptions(index), origin = request.origin,
                        interaction = retryInteraction(index))
                    true
                }
            },
            close = ::dispose,
        ), canDeleteSource = true)
        // A blocked ContentResolver query/open does not return to checkActive on its own. This
        // deadline cancels its signal without setting the core's user-cancellation flag or
        // interrupting a pooled thread, so the reported failure remains TIMEOUT.
        private val sourceDeadline = sourceDeadlines.schedule(::cancelSource,
            (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0), TimeUnit.MILLISECONDS)

        private fun retryOptions(index: Int): InstallOptions? {
            val item = presentation.snapshot().items.getOrNull(index)
            // Confirmed choices retain their exact values. Unconfirmed defaults get a fresh
            // profile/default snapshot on explicit retry, never the earlier effective values.
            return if (item?.confirmedOptions == true) item.options
                else request.options.takeIf { request.explicitOptions.isNotEmpty() }
        }

        private fun retryInteraction(index: Int): String = installRetryInteraction(request.interaction,
            presentation.snapshot().items.getOrNull(index)?.confirmedOptions == true)

        fun run() {
            synchronized(workerLock) { worker = Thread.currentThread() }
            try {
                checkActive()
                val actual = request.copy(
                    options = request.options.copy(timeoutMillis = (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(1)))
                lateinit var environment: DescriptorInstallEnvironment
                environment = DescriptorInstallEnvironment.acquireSources(context,
                    sourceLoader = { source, check ->
                        checkActive()
                        sources.openItem(context, source.descriptor, sourceCancellation) { checkActive(); check() }
                    },
                    configuration = { index, prepared, target, selectedRequest, expires, check ->
                        InstallationUi.configure(context, presentation, environment, index, prepared, target, selectedRequest, expires, check)
                    },
                    preparedListener = { index, prepared -> presentation.onPrepared(index, prepared) },
                    installed = { index, options -> sources.deleteInstalled(context, index, options) },
                    availability = presentation::checkNotificationAvailable,
                    userAction = presentation::showSystemConfirmation,
                    safetyOwnerToken = presentation.token,
                    safetyReview = presentation::reviewSafety,
                )
                val session = InstallSession(actual, environment, object : InstallSession.Listener {
                    override fun onStage(stage: String, detail: JsonObject) {
                        presentation.onStage(stage, detail)
                        InstallNotifications.update(context, presentation.token, actual.sources.first().displayName, stage,
                            presentation.snapshot().progress, presentation.activityIntent(), ::cancel)
                    }
                    override fun onProgress(progress: Float, detail: JsonObject) {
                        presentation.onProgress(progress, detail)
                        InstallNotifications.update(context, presentation.token, actual.sources.first().displayName,
                            InstallerContract.STAGE_WRITING, progress, presentation.activityIntent(), ::cancel)
                    }
                    override fun onItemResult(index: Int, result: JsonObject) = presentation.onItemResult(index, result)
                    override fun onInstalled(index: Int, result: JsonObject) = presentation.onInstalled(index, result)
                    override fun onOptionsResolved(index: Int, options: InstallOptions, interaction: String, profileName: String?) =
                        presentation.onOptionsResolved(index, options, interaction, profileName)
                    override fun onCompleted(result: JsonObject) {
                        finished.set(true)
                        slot.close()
                        presentation.onCompleted(result)
                        InstallNotifications.complete(context, presentation.token, presentation.snapshot().items.all {
                            it.result?.get(InstallerContract.FIELD_OK)?.asBoolean == true
                        }, openIntent = presentation.activityIntent())
                    }
                    override fun onFailed(failure: InstallFailure) {
                        finished.set(true)
                        slot.close()
                        presentation.onFailed(failure)
                        InstallNotifications.complete(context, presentation.token, false, openIntent = presentation.activityIntent())
                    }
                }, SystemClock::elapsedRealtime)
                core = session
                if (cancelled.get()) session.cancel()
                // Already on the bounded worker pool: avoid nesting another queued worker.
                session.start(java.util.concurrent.Executor { it.run() })
            } catch (failure: Exception) {
                finished.set(true)
                presentation.onFailed(InstallFailure.from(failure))
                InstallNotifications.complete(context, presentation.token, false, openIntent = presentation.activityIntent())
            } finally {
                sourceDeadline.cancel(false)
                synchronized(workerLock) { worker = null; Thread.interrupted() }
                slot.close()
            }
        }

        private fun checkActive() {
            if (cancelled.get() || Thread.currentThread().isInterrupted) throw InstallFailure(InstallerErrorCodes.CANCELLED, "Installation cancelled")
            if (SystemClock.elapsedRealtime() >= deadline) throw InstallFailure(InstallerErrorCodes.TIMEOUT, "Installation timed out")
            presentation.checkNotificationAvailable()
        }
        private fun cancel() {
            cancelled.set(true)
            core?.cancel()
            synchronized(workerLock) { worker?.interrupt() }
            sourceDeadline.cancel(false)
            cancelSource()
        }
        private fun cancelSource() {
            if (!sourceCancellationRequested.compareAndSet(false, true)) return
            // A provider's cancellation callback may perform IPC. Keep that work off the UI,
            // deadline scheduler and installation worker, and never interrupt a reused thread.
            sourceCancellations.execute { runCatching { sourceCancellation.cancel() } }
        }
        fun dispose() {
            if (!disposed.compareAndSet(false, true)) return
            cancel()
            tasks.remove(presentation.token, this)
            InstallNotifications.remove(context, presentation.token)
            // The worker owns its sources and releases capacity only after cleanup.
            presentation.close()
        }
    }
}
