package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import android.content.Intent
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.io.Closeable
import java.util.concurrent.Executor

/** Owns one request, its duplicated sources and its worker. Binder ownership belongs to P2.6. */
internal class InstallSession(
    val request: InstallRequest,
    private val environment: Environment,
    private val listener: Listener,
    private val clock: () -> Long,
) : Closeable {
    data class Target(val authorizer: Authorizer, val userId: Int, val engine: InstallEngine)
    data class Version(val name: String?, val code: Long)
    data class Status(val stage: String, val index: Int, val progress: Float)
    data class Selection(val prepared: PreparedPackage, val target: Target, val options: InstallOptions)
    data class SourceCleanup(val deleted: Boolean = false, val notes: List<String> = emptyList())

    interface Environment : Closeable {
        /** A mandatory presentation may become unavailable while the worker is waiting/writing. */
        fun checkAvailable() = Unit
        fun resolve(request: InstallRequest, deadlineMillis: Long, checkActive: () -> Unit): Target
        fun prepare(index: Int, sources: List<SourceEntry>, checkActive: () -> Unit): PreparedPackage
        fun installedVersion(packageName: String, target: Target): Version?
        /** P3 supplies the plugin confirmation. Explicit dialog must never silently bypass it. */
        fun confirm(prepared: PreparedPackage, target: Target, deadlineMillis: Long, checkActive: () -> Unit)
        fun configure(index: Int, prepared: PreparedPackage, target: Target, request: InstallRequest,
            deadlineMillis: Long, checkActive: () -> Unit): Selection {
            confirm(prepared, target, deadlineMillis, checkActive)
            return Selection(prepared, target, request.options)
        }
        fun onInstalled(index: Int, options: InstallOptions): SourceCleanup = SourceCleanup()
        fun onUserAction(intent: Intent)
        fun discardItem(index: Int)
    }

    interface Listener {
        fun onStage(stage: String, detail: JsonObject) = Unit
        fun onProgress(progress: Float, detail: JsonObject) = Unit
        fun onItemResult(index: Int, result: JsonObject) = Unit
        fun onCompleted(result: JsonObject)
        fun onFailed(failure: InstallFailure)
    }

    private val lock = Any()
    private var started = false
    private var cancelled = false
    private var terminal = false
    private var worker: Thread? = null
    private var status = Status(InstallerContract.STAGE_PENDING, 0, 0f)
    private val deadline = clock() + request.options.timeoutMillis

    fun status(): Status = synchronized(lock) { status }

    fun start(executor: Executor) {
        synchronized(lock) {
            check(!started) { "Session already started or closed" }
            started = true
        }
        try {
            executor.execute(::run)
        } catch (failure: java.util.concurrent.RejectedExecutionException) {
            finish(null, InstallFailure(InstallerErrorCodes.INTERNAL, "Installation worker unavailable", cause = failure))
        }
    }

    fun cancel(): Boolean = synchronized(lock) {
        if (terminal) return false
        cancelled = true
        // The worker clears its identity under the same lock before it can return to the pool.
        worker?.interrupt()
        true
    }

    override fun close() {
        val neverStarted = synchronized(lock) {
            if (terminal) return
            cancelled = true
            worker?.interrupt()
            (!started).also { started = true }
        }
        if (neverStarted) finish(null, cancellation())
    }

    private fun run() {
        synchronized(lock) { worker = Thread.currentThread() }
        try {
            executeItems()
        } catch (failure: Exception) {
            finish(null, InstallFailure.from(failure))
        } finally {
            synchronized(lock) {
                worker = null
                // An engine restores InterruptedException; that cancellation must not poison a
                // later request scheduled on this same pooled thread.
                Thread.interrupted()
            }
        }
    }

    private fun executeItems() {
        checkActive()
        val initialTarget = environment.resolve(request, deadline, ::checkActive)
        checkActive()
        val results = mutableListOf<JsonObject>()
        for ((index, sources) in request.items.withIndex()) {
            synchronized(lock) { status = status.copy(index = index) }
            val startedAt = clock()
            var prepared: PreparedPackage? = null
            var target = initialTarget
            var options = request.options
            var packageLease: Closeable? = null
            try {
                checkActive()
                stage(InstallerContract.STAGE_PREPARING, index, null)
                prepared = environment.prepare(index, sources, ::checkActive)
                checkActive()
                prepared.failure()?.let { throw it }
                if (request.interaction == InstallerContract.INTERACTION_DIALOG || request.interaction == InstallerContract.INTERACTION_NOTIFICATION) {
                    stage(InstallerContract.STAGE_CONFIRMING, index, prepared.packageName)
                    val selection = environment.configure(index, prepared, target, request, deadline, ::checkActive)
                    prepared = selection.prepared
                    target = selection.target
                    options = selection.options
                    prepared.failure()?.let { throw it }
                    checkActive()
                    // The confirmation has been consumed. A same-package wait is preparation,
                    // not another request for approval, and must retain only the cancel action.
                    stage(InstallerContract.STAGE_PREPARING, index, prepared.packageName)
                }
                val current = prepared
                // Confirmation may change the target and can take minutes. Acquire only once it
                // finishes, then keep version sampling and the entire platform session together.
                packageLease = PackageInstallLocks.acquire(current.packageName, ::checkActive)
                val previous = prepared.packageName?.let { environment.installedVersion(it, target) }
                checkActive()
                val installed = target.engine.install(
                    InstallEngine.Request(current, options, target.userId, request.interaction, deadline),
                    object : InstallEngine.Listener {
                        override fun onStage(stage: String) = stage(stage, index, current.packageName)
                        override fun onProgress(bytesWritten: Long, totalBytes: Long) {
                            checkActive()
                            val progress = if (totalBytes <= 0) 0f else (bytesWritten.toDouble() / totalBytes).toFloat().coerceIn(0f, 1f)
                            synchronized(lock) { status = status.copy(progress = progress) }
                            notifyListener { listener.onProgress(progress, InstallDocuments.progressDetail(index, bytesWritten, totalBytes)) }
                        }
                        override fun onUserAction(intent: Intent) = environment.onUserAction(intent)
                    },
                    ::checkActive,
                )
                // Installation is now confirmed. Optional metadata/cleanup, or a late cancel,
                // must never convert an installed package into a failed result.
                val packageName = installed.packageName ?: prepared.packageName
                val notes = installed.notes.toMutableList()
                val cleanup = try { environment.onInstalled(index, options) } catch (_: Exception) {
                    SourceCleanup(notes = listOf("Installation succeeded, but source cleanup could not be completed"))
                }
                notes += cleanup.notes
                val version = try {
                    packageName?.let { environment.installedVersion(it, target) }
                } catch (_: Exception) {
                    notes += "Installed version could not be read; archive version is reported"
                    null
                }
                val result = InstallDocuments.installResult(packageName, version?.name ?: prepared.versionName,
                    version?.code ?: prepared.versionCode, previous?.code, target.authorizer.id, installed.interaction,
                    (clock() - startedAt).coerceAtLeast(0), cleanup.deleted, notes)
                results += result
                runCatching { listener.onItemResult(index, result.deepCopy()) }
            } catch (failure: Exception) {
                val error = InstallFailure.from(failure, prepared?.packageName)
                val result = InstallDocuments.failedItem(error, prepared?.packageName, target.authorizer.id, null,
                    (clock() - startedAt).coerceAtLeast(0))
                runCatching { listener.onItemResult(index, result.deepCopy()) }
                if (!request.isBatch || !options.continueOnError) throw error
                results += result
                if (error.code == InstallerErrorCodes.CANCELLED) cancel()
            } finally {
                runCatching { environment.discardItem(index) }
                packageLease?.close()
            }
        }
        finish(if (request.isBatch) InstallDocuments.batch(results) else results.single(), null)
    }

    private fun checkActive() {
        if (synchronized(lock) { cancelled } || Thread.currentThread().isInterrupted) throw cancellation()
        if (clock() >= deadline) throw InstallFailure(InstallerErrorCodes.TIMEOUT, "The installation session timed out")
        environment.checkAvailable()
    }

    private fun stage(stage: String, index: Int, packageName: String?) {
        checkActive()
        synchronized(lock) { status = Status(stage, index, if (stage == InstallerContract.STAGE_PREPARING) 0f else status.progress) }
        notifyListener { listener.onStage(stage, InstallDocuments.stageDetail(index, packageName)) }
    }

    private fun notifyListener(block: () -> Unit) {
        try { block() } catch (failure: Exception) {
            cancel()
            throw InstallFailure(InstallerErrorCodes.CANCELLED, "Session callback disconnected", cause = failure)
        }
    }

    private fun finish(result: JsonObject?, failure: InstallFailure?) {
        val boundedResult = result?.let(InstallDocuments::boundedResult)
        synchronized(lock) {
            if (terminal) return
            terminal = true
        }
        runCatching { environment.close() }
        val end = when (failure?.code) {
            null -> InstallerContract.STAGE_COMPLETED
            InstallerErrorCodes.CANCELLED -> InstallerContract.STAGE_CANCELLED
            else -> InstallerContract.STAGE_FAILED
        }
        synchronized(lock) { status = status.copy(stage = end) }
        runCatching { listener.onStage(end, InstallDocuments.stageDetail(status().index, failure?.packageName)) }
        // Both callback attempts happen only after all owned descriptors and staging are closed.
        runCatching { if (failure == null) listener.onCompleted(requireNotNull(boundedResult)) else listener.onFailed(failure) }
    }

    private fun cancellation() = InstallFailure(InstallerErrorCodes.CANCELLED, "Installation session cancelled")
}
