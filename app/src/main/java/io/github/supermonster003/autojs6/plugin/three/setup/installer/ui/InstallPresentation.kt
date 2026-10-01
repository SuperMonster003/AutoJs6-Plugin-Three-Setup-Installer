package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.DeviceUser
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.DeviceUsers
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallRequest
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallSession
import io.github.supermonster003.autojs6.plugin.three.setup.installer.history.InstallHistoryCapture
import io.github.supermonster003.autojs6.plugin.three.setup.installer.history.InstallHistoryStore
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.lang.ref.WeakReference
import java.io.Closeable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/** Process-local presentation of an existing worker. A restored token never restarts an install. */
internal object InstallPresentation {
    const val EXTRA_TOKEN = "installationPresentationToken"
    private const val RESULT_RETENTION_MILLIS = 10 * 60 * 1000L
    private const val MAX_PRESENTATIONS = 128
    private val main = Handler(Looper.getMainLooper())
    private val records = ConcurrentHashMap<String, Record>()
    private class Observer(@Volatile var action: (() -> Unit)?)
    private val observers = mutableSetOf<Observer>()
    private val observerUpdate = AtomicBoolean()
    private var reaperScheduled = false
    private val reaper = object : Runnable {
        override fun run() {
            val now = SystemClock.elapsedRealtime()
            records.values.filter { it.expiresAt <= now }.forEach { it.close() }
            reaperScheduled = records.isNotEmpty()
            if (reaperScheduled) main.postDelayed(this, 60_000)
        }
    }

    data class Callbacks(val cancel: () -> Unit, val retry: ((Int) -> Boolean)? = null, val close: () -> Unit = {})
    data class Choice(val options: InstallOptions, val selectedApkNames: Set<String>)
    data class Split(val name: String, val size: Long, val selectable: Boolean, val base: Boolean)
    data class Metadata(
        val label: String,
        val packageName: String?,
        val versionName: String?,
        val versionCode: Long?,
        val previousVersion: InstallSession.Version?,
        val installedUserId: Int,
        val installedKnown: Boolean,
        val size: Long,
        val minSdk: Int?,
        val targetSdk: Int?,
        val signature: String,
        val icon: Bitmap?,
        val format: String,
        val splits: List<Split>,
        val aabModules: List<String>,
    )
    data class Item(
        val displayName: String,
        val stage: String = InstallerContract.STAGE_PENDING,
        val metadata: Metadata? = null,
        val result: JsonObject? = null,
        val options: InstallOptions? = null,
    )
    data class Snapshot(
        val revision: Long,
        val stage: String,
        val index: Int,
        val progress: Float,
        val items: List<Item>,
        val prompt: Prompt?,
        val terminal: Boolean,
        val failure: InstallFailure?,
        val canRetry: Boolean,
        val canDeleteSource: Boolean,
        val recovered: Boolean = false,
        val interrupted: Boolean = false,
    )
    /** Application-independent view used by Home; ownership remains in the installation worker. */
    data class TaskSnapshot(val token: String, val origin: String, val createdAt: Long, val state: Snapshot)
    class Prompt internal constructor(
        val index: Int,
        val metadata: Metadata,
        val choices: InstallChoices,
        val users: List<DeviceUser>,
    ) {
        internal val decision = InstallDecision<Choice>()
    }

    fun create(context: Context, request: InstallRequest, callbacks: Callbacks, canDeleteSource: Boolean = false): Record {
        val record = Record(context.applicationContext, request, callbacks, canDeleteSource)
        val evicted = synchronized(records) {
            val oldest = if (records.size >= MAX_PRESENTATIONS) records.values.filter { it.snapshot().terminal }.minByOrNull { it.expiresAt } else null
            if (records.size >= MAX_PRESENTATIONS && oldest == null) throw InstallFailure(InstallerErrorCodes.INVALID_ARGUMENT, "Too many active installation interfaces")
            if (oldest != null) records.remove(oldest.token, oldest)
            records[record.token] = record
            oldest
        }
        evicted?.close()
        record.startPersistence()
        notifyObservers()
        main.post {
            if (!reaperScheduled) {
                reaperScheduled = true
                main.postDelayed(reaper, 60_000)
            }
        }
        return record
    }

    fun find(token: String): Record? = records[token]

    fun snapshots(includeTerminal: Boolean = false): List<TaskSnapshot> = records.values.map { record ->
        TaskSnapshot(record.token, record.request.origin, record.createdAt, record.snapshot())
    }.filter { includeTerminal || !it.state.terminal }.sortedByDescending { it.createdAt }

    fun observe(observer: () -> Unit): Closeable {
        val entry = Observer(observer)
        synchronized(observers) { observers += entry }
        main.post { entry.action?.invoke() }
        return Closeable { synchronized(observers) { entry.action = null; observers -= entry } }
    }

    private fun notifyObservers() {
        if (!observerUpdate.compareAndSet(false, true)) return
        main.post {
            observerUpdate.set(false)
            synchronized(observers) { observers.toList() }.forEach { entry -> runCatching { entry.action?.invoke() } }
        }
    }

    class Record internal constructor(
        private val context: Context,
        val request: InstallRequest,
        private val callbacks: Callbacks,
        private val canDeleteSource: Boolean,
    ) {
        val token: String = UUID.randomUUID().toString()
        val createdAt: Long = System.currentTimeMillis()
        private val lock = Any()
        private var activity = WeakReference<InstallDialogActivity>(null)
        private var items = request.items.map { Item(it.first().displayName) }
        private var stage = InstallerContract.STAGE_PENDING
        private var index = 0
        private var progress = 0f
        private var revision = 0L
        private var prompt: Prompt? = null
        private var terminal = false
        private var failure: InstallFailure? = null
        private var closed = false
        private var shown = false
        private var retrying = false
        private var cancellationRequested = false
        private var recovery: InstallRecoveryWriter.Ticket? = null
        private var history: InstallHistoryStore.Ticket? = null
        private val recoveryDeadline = SystemClock.elapsedRealtime() + request.options.timeoutMillis
        private val queuedUpdate = AtomicBoolean()
        @Volatile internal var expiresAt = Long.MAX_VALUE
            private set
        val isAttached: Boolean get() = synchronized(lock) { activity.get()?.let { !it.isFinishing && !it.isDestroyed } == true }

        fun activityIntent(): Intent = Intent(context, InstallDialogActivity::class.java)
            .putExtra(EXTRA_TOKEN, token)
            .setData(Uri.parse("three-setup-install://session/$token"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)

        fun snapshot(): Snapshot = synchronized(lock) {
            Snapshot(revision, stage, index, progress, items.toList(), prompt, terminal, failure,
                terminal && !retrying && callbacks.retry != null, canDeleteSource)
        }

        internal fun startPersistence() {
            history = runCatching { InstallHistoryStore.get(context).begin(token) }.getOrNull()
            recovery = InstallRecoveryPersistence.writer(context).begin(token)
            persist()
        }

        private fun persist(durable: Boolean = false) {
            val state = snapshot()
            runCatching {
                val completion = history?.save(InstallHistoryCapture.capture(token, request, state, createdAt, System.currentTimeMillis()),
                    state.revision, finish = state.terminal)
                if (durable && !InstallRecoveryPersistence.mainThread()) completion?.await()
            }.onFailure { android.util.Log.w("InstallHistory", "Installation history could not be saved") }
            runCatching {
                if (synchronized(lock) { closed }) return
                val remaining = if (state.terminal) expiresAt - SystemClock.elapsedRealtime()
                    else recoveryDeadline - SystemClock.elapsedRealtime() + InstallRecoverySnapshot.RETENTION_MILLIS
                val saved = InstallRecoverySnapshot.capture(token, state, request.options, System.currentTimeMillis(), remaining)
                val completion = recovery?.save(saved, durable)
                if (durable && !InstallRecoveryPersistence.mainThread()) completion?.await()
            }.onFailure { android.util.Log.w("InstallRecovery", "Installation display snapshot could not be saved") }
        }

        /** Serialize the final eligibility check with close, completion and Activity attachment. */
        internal fun notifyIfWaitingForActivity(notification: () -> Unit) = synchronized(lock) {
            if (!closed && !terminal && !isAttached) notification()
        }

        /** May run on the caller/worker; Activity creation is the only presentation side effect. */
        fun show(grantIntent: Intent? = null) {
            synchronized(lock) {
                if (closed || shown) return
                shown = true
            }
            try {
                context.startActivity(activityIntent()
                    .apply {
                        // The external-entry controller validates the URI set before forwarding.
                        // Carry grants only; its component, data, actions and extras are untrusted.
                        clipData = grantIntent?.clipData
                        addFlags((grantIntent?.flags ?: 0) and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
                    })
            } catch (failure: Exception) {
                synchronized(lock) { shown = false }
                throw InstallFailure(InstallerErrorCodes.BLOCKED_BY_POLICY, "The installation interface could not be opened", systemMessage = failure.message)
            }
        }

        internal fun attach(owner: InstallDialogActivity) {
            synchronized(lock) { activity = WeakReference(owner); shown = true }
            InstallNotifications.dismissAction(context, token)
            owner.present(snapshot())
        }

        internal fun detach(owner: InstallDialogActivity) {
            synchronized(lock) { if (activity.get() === owner) activity.clear() }
        }

        /** Archive and PackageManager IO intentionally stays on the installation worker. */
        fun onPrepared(index: Int, prepared: PreparedPackage, targetUserId: Int? = null) {
            val metadata = InstallMetadata.read(context, prepared, targetUserId ?: DeviceUsers(context).currentId)
            synchronized(lock) {
                if (closed || terminal || index !in items.indices) return
                items = items.toMutableList().also { it[index] = it[index].copy(metadata = metadata) }
                revision++
            }
            persist()
            changed()
        }

        fun confirm(index: Int, prepared: PreparedPackage, target: InstallSession.Target, options: InstallOptions,
            deadline: Long, checkActive: () -> Unit): Choice {
            check(Looper.myLooper() != Looper.getMainLooper()) { "Installation confirmation must not block the main thread" }
            checkActive()
            val metadata = InstallMetadata.read(context, prepared, target.userId, target.authorizer)
            val users = runCatching { DeviceUsers(context).list(target.authorizer, (deadline - SystemClock.elapsedRealtime()).coerceIn(1, 3_000)) }
                .getOrDefault(listOf(DeviceUser(target.userId, null, false, true)))
            checkActive()
            val waiting = Prompt(index, metadata,
                InstallChoices(options, prepared.apks.map { InstallChoices.Apk(it.name, it.splitName == null) }, canDeleteSource), users)
            synchronized(lock) {
                if (closed || terminal) throw cancelled()
                check(prompt == null) { "Only one item can wait for confirmation" }
                prompt = waiting
                this.index = index
                stage = InstallerContract.STAGE_CONFIRMING
                items = items.toMutableList().also { it[index] = it[index].copy(metadata = metadata, stage = stage) }
                revision++
            }
            val expires = SystemClock.elapsedRealtime() + InstallerContract.DEFAULT_USER_ACTION_TIMEOUT_MILLIS
            try {
                persist()
                show()
                changed()
                while (true) {
                    checkActive()
                    val now = SystemClock.elapsedRealtime()
                    if (now >= deadline) throw InstallFailure(InstallerErrorCodes.TIMEOUT, "Installation session timed out")
                    if (now >= expires) throw InstallFailure(InstallerErrorCodes.USER_ACTION_TIMEOUT, "Installation confirmation timed out")
                    if (waiting.decision.await(minOf(100, deadline - now, expires - now))) {
                        val choice = waiting.decision.result() ?: throw InstallFailure(InstallerErrorCodes.USER_CANCELLED, "Installation was declined")
                        checkActive()
                        synchronized(lock) { items = items.toMutableList().also { it[index] = it[index].copy(options = choice.options) } }
                        return choice
                    }
                }
            } finally {
                synchronized(lock) {
                    if (prompt === waiting) {
                        prompt = null
                        revision++
                    }
                }
                changed()
            }
        }

        internal fun accept() {
            synchronized(lock) {
                if (closed || terminal || cancellationRequested) return
                val waiting = prompt ?: return
                if (!waiting.choices.valid()) return
                val value = waiting.choices.snapshot()
                waiting.decision.answer(Choice(value.options, value.selectedApkNames))
            }
        }

        fun onStage(value: String, detail: JsonObject) {
            synchronized(lock) {
                if (closed || terminal) return
                // A terminal stage callback arrives just before its authoritative outcome.
                // Keep the pending UI until onCompleted/onFailed provides that outcome.
                if (value in InstallerContract.TERMINAL_STAGES) return
                val nextIndex = detail.get(InstallerContract.FIELD_INDEX)?.asInt ?: index
                if (nextIndex !in items.indices) return
                index = nextIndex
                stage = value
                if (value == InstallerContract.STAGE_PREPARING) progress = 0f
                items = items.toMutableList().also { list ->
                    if (list[index].result == null) list[index] = list[index].copy(stage = value)
                }
                revision++
            }
            persist()
            changed()
        }

        fun onProgress(value: Float, detail: JsonObject) {
            synchronized(lock) {
                if (closed || terminal || !value.isFinite()) return
                progress = value.coerceIn(0f, 1f)
                detail.get(InstallerContract.FIELD_INDEX)?.asInt?.takeIf { it in items.indices }?.let { index = it }
            }
            changed()
        }

        fun onItemResult(index: Int, result: JsonObject) {
            synchronized(lock) {
                // Closing a window cannot discard the worker's authoritative package outcome.
                if (terminal || index !in items.indices) return
                val successful = result.get(InstallerContract.FIELD_OK)?.asBoolean == true
                items = items.toMutableList().also {
                    it[index] = it[index].copy(result = result.deepCopy(), stage = if (successful) InstallerContract.STAGE_COMPLETED else InstallerContract.STAGE_FAILED,
                        metadata = if (request.isBatch) it[index].metadata?.copy(icon = null) else it[index].metadata)
                }
                revision++
            }
            persist(durable = true)
            changed()
        }

        fun onCompleted(result: JsonObject) {
            val outcomes = result.getAsJsonArray(InstallerContract.FIELD_RESULTS)?.map { it.asJsonObject } ?: listOf(result)
            synchronized(lock) {
                if (terminal) return
                items = items.mapIndexed { i, item ->
                    outcomes.getOrNull(i)?.let { outcome ->
                        item.copy(result = outcome.deepCopy(), stage = if (outcome.get(InstallerContract.FIELD_OK)?.asBoolean == true)
                            InstallerContract.STAGE_COMPLETED else InstallerContract.STAGE_FAILED,
                            metadata = if (request.isBatch) item.metadata?.copy(icon = null) else item.metadata)
                    } ?: item
                }
                terminal = true
                stage = if (items.all { it.stage == InstallerContract.STAGE_COMPLETED }) InstallerContract.STAGE_COMPLETED else InstallerContract.STAGE_FAILED
                progress = 1f
                expiresAt = SystemClock.elapsedRealtime() + RESULT_RETENTION_MILLIS
                revision++
            }
            persist(durable = true)
            changed()
        }

        fun onFailed(value: InstallFailure) {
            synchronized(lock) {
                if (terminal) return
                failure = value
                terminal = true
                stage = if (value.code == InstallerErrorCodes.CANCELLED || value.code == InstallerErrorCodes.USER_CANCELLED)
                    InstallerContract.STAGE_CANCELLED else InstallerContract.STAGE_FAILED
                items = items.map { if (it.result == null) it.copy(stage = InstallerContract.STAGE_CANCELLED) else it }.toMutableList().also {
                    // An overall timeout/cancellation after an item result cannot undo that
                    // confirmed installation. Keep its result and expose the overall failure.
                    if (it[index].result == null) it[index] = it[index].copy(stage = stage, result = JsonObject().apply {
                            addProperty(InstallerContract.FIELD_OK, false)
                            add(InstallerContract.FIELD_ERROR, value.toJson())
                        })
                }
                prompt?.decision?.answer(null)
                prompt = null
                expiresAt = SystemClock.elapsedRealtime() + RESULT_RETENTION_MILLIS
                revision++
            }
            persist(durable = true)
            changed()
        }

        fun cancel() {
            val waiting = synchronized(lock) {
                if (closed || terminal || cancellationRequested) return
                cancellationRequested = true
                val current = prompt
                if (current != null && !request.isBatch) {
                    // A pending single-item prompt is a refusal (USER_CANCELLED). Once approval
                    // wins, cancellation must reach the worker even before it clears the prompt.
                    if (current.decision.answer(null) || current.decision.result() == null) return
                }
                current
            }
            // Run owner callbacks outside the presentation lock. For a batch, the cancellation
            // flag reaches the worker before its prompt is released; repeated taps call once.
            try { callbacks.cancel() } finally { waiting?.decision?.answer(null) }
        }

        internal fun retry(index: Int): Boolean {
            synchronized(lock) {
                if (closed || !terminal || retrying || callbacks.retry == null || index !in items.indices ||
                    items[index].result?.get(InstallerContract.FIELD_OK)?.asBoolean != false) return false
                retrying = true
            }
            val accepted = runCatching { callbacks.retry?.invoke(index) == true }.getOrDefault(false)
            synchronized(lock) { retrying = accepted; revision++ }
            changed()
            return accepted
        }

        internal fun dismiss() {
            if (!snapshot().terminal) cancel()
            close()
        }

        /** Idempotent; callbacks release sources and owners, never restart or mutate terminal results. */
        fun close() {
            synchronized(lock) {
                if (closed) return
                closed = true
                prompt?.decision?.answer(null)
                prompt = null
            }
            records.remove(token, this)
            notifyObservers()
            val released = AtomicBoolean()
            val removed = AtomicBoolean()
            val finishQueued = AtomicBoolean()
            val finish = {
                if (released.get() && removed.get() && finishQueued.compareAndSet(false, true)) {
                    main.post { activity.get()?.takeUnless { it.isDestroyed || it.isFinishing }?.finish() }
                }
                Unit
            }
            // Retire the writer ticket before releasing owners, which may perform IPC. A new
            // Record may already reserve this registry slot while that cleanup is in progress.
            recovery?.close { removed.set(true); finish() } ?: removed.set(true)
            runCatching { callbacks.close() }
            InstallNotifications.remove(context, token)
            released.set(true)
            finish()
        }

        private fun changed() {
            notifyObservers()
            if (!queuedUpdate.compareAndSet(false, true)) return
            main.post {
                queuedUpdate.set(false)
                val owner = synchronized(lock) { activity.get().takeUnless { closed } }
                owner?.present(snapshot())
            }
        }

        private fun cancelled() = InstallFailure(InstallerErrorCodes.CANCELLED, "Installation presentation is closed")
    }
}
