package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Bundle
import android.os.DeadObjectException
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.PrivilegedClient
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.IPrivilegedInstaller
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.PrivilegedOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.HiddenApiAccess
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PlannedApk
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.io.Closeable
import java.io.IOException
import java.io.OutputStream
import java.security.MessageDigest

/** Installs one prepared package on a worker thread. The caller owns staging and source deletion. */
internal interface InstallEngine {
    data class Request(
        val prepared: PreparedPackage,
        val options: InstallOptions,
        /** Resolved, validated device user; for `all`, pass the current user and set options.user. */
        val userId: Int,
        val interaction: String = InstallerContract.INTERACTION_AUTO,
        /** Optional earlier deadline from the enclosing batch, measured in elapsed realtime. */
        val deadlineMillis: Long = Long.MAX_VALUE,
    )

    data class Result(val packageName: String?, val notes: List<String>, val interaction: String,
        val followUp: InstallFollowUp = InstallFollowUp())

    interface Listener {
        fun onStage(stage: String) = Unit
        /** Bytes accepted by the session stream; commit validation and durable completion follow. */
        fun onProgress(bytesWritten: Long, totalBytes: Long) = Unit
        /** Authoritative package success, before optional work; never an invitation to retry. */
        fun onInstalled(result: Result) = Unit
        /** Must launch/delegate confirmation or throw. Never silently discard this callback. */
        fun onUserAction(intent: Intent)
    }

    /** Callbacks run on the calling worker. Throw InstallFailure(CANCELLED, ...) from checkCancelled. */
    fun install(request: Request, listener: Listener, checkCancelled: () -> Unit = {}): Result
}

/** Shared streaming and lifecycle logic; the two transports own only their platform session handles. */
internal abstract class SessionInstallEngine(
    private val authorizer: Authorizer,
    private val sdk: Int = Build.VERSION.SDK_INT,
    private val currentUser: Int = Process.myUid() / 100000,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) : InstallEngine {
    internal data class Parameters(val totalBytes: Long, val flags: Int, val notes: List<String>)

    internal interface Session : Closeable {
        fun openWrite(apk: PlannedApk, checkActive: () -> Unit): OutputStream
        fun fsync(output: OutputStream)
        fun commit()
        fun await(deadlineMillis: Long, checkActive: () -> Unit, onUserAction: (Intent) -> Unit): InstallStatusBridge.Status
        fun abandon()
    }

    protected abstract fun openSession(request: InstallEngine.Request, parameters: Parameters, deadlineMillis: Long): Session
    protected open fun afterInstallation(request: InstallEngine.Request, packageName: String,
        deadlineMillis: Long, checkActive: () -> Unit): InstallFollowUp = InstallFollowUp()

    final override fun install(request: InstallEngine.Request, listener: InstallEngine.Listener, checkCancelled: () -> Unit): InstallEngine.Result {
        var session: Session? = null
        var terminalReceived = false
        val name = request.prepared.packageName
        try {
            val parameters = validate(request)
            val notes = parameters.notes.toMutableList()
            var interaction = if (request.interaction == InstallerContract.INTERACTION_NOTIFICATION) {
                InstallerContract.INTERACTION_NOTIFICATION
            } else if (request.interaction == InstallerContract.INTERACTION_DIALOG || !authorizer.privileged) {
                InstallerContract.INTERACTION_DIALOG
            } else InstallerContract.INTERACTION_SILENT
            val deadline = minOf(request.deadlineMillis, clock() + request.options.timeoutMillis)
            val checkActive = {
                if (Thread.currentThread().isInterrupted) throw InstallFailure(InstallerErrorCodes.CANCELLED, "Installation interrupted")
                checkCancelled()
                if (clock() >= deadline) throw InstallFailure(InstallerErrorCodes.TIMEOUT, "The installation timed out")
            }
            checkActive()
            session = openSession(request, parameters, deadline)
            checkActive()
            listener.onStage(InstallerContract.STAGE_WRITING)
            listener.onProgress(0, parameters.totalBytes)
            var written = 0L
            val buffer = ByteArray(1024 * 1024)
            for (apk in request.prepared.apks) {
                checkActive()
                apk.file.inputStream().use { input ->
                    platformIo { session.openWrite(apk, checkActive) }.use { output ->
                        var copied = 0L
                        val digest = apk.sha256?.let { MessageDigest.getInstance("SHA-256") }
                        while (true) {
                            checkActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            if (count.toLong() > apk.size - copied) throw InstallFailure(InstallerErrorCodes.INVALID_PACKAGE, "APK grew after preparation: ${apk.name}")
                            platformIo { output.write(buffer, 0, count) }
                            digest?.update(buffer, 0, count)
                            copied += count
                            written += count
                            checkActive()
                            listener.onProgress(written, parameters.totalBytes)
                        }
                        if (copied != apk.size) throw InstallFailure(InstallerErrorCodes.INVALID_PACKAGE, "APK was truncated after preparation: ${apk.name}")
                        if (digest != null && digest.digest().joinToString("") { "%02x".format(it) } != apk.sha256) {
                            throw InstallFailure(InstallerErrorCodes.INVALID_PACKAGE, "APK changed after preparation: ${apk.name}")
                        }
                        checkActive()
                        platformIo { session.fsync(output) }
                    }
                }
            }
            checkActive()
            listener.onStage(InstallerContract.STAGE_COMMITTING)
            checkActive()
            session.commit()
            val status = session.await(deadline, checkActive) { intent ->
                checkActive()
                if (request.interaction == InstallerContract.INTERACTION_SILENT) {
                    throw InstallFailure(InstallerErrorCodes.AUTHORIZER_REQUIRED, "The system requires confirmation for this silent installation")
                }
                if (request.interaction == InstallerContract.INTERACTION_AUTO && authorizer.privileged && interaction != InstallerContract.INTERACTION_DIALOG) {
                    notes += "The system required confirmation despite the privileged authorizer"
                }
                if (request.interaction != InstallerContract.INTERACTION_NOTIFICATION) interaction = InstallerContract.INTERACTION_DIALOG
                listener.onStage(InstallerContract.STAGE_CONFIRMING)
                listener.onUserAction(intent)
            }
            terminalReceived = true
            if (status.status != InstallStatusMapper.STATUS_SUCCESS) {
                throw InstallStatusMapper.toFailure(status.status, status.message, status.packageName ?: name)
            }
            // The package lock stays held through optional work, which cannot undo a confirmed installation.
            val installedName = status.packageName ?: name
            if (request.options.requestUpdateOwnership || request.options.dexopt != InstallerContract.DEXOPT_NONE) {
                try { listener.onInstalled(InstallEngine.Result(installedName, notes.toList(), interaction)) }
                catch (_: Exception) { notes += "Installation succeeded, but its early outcome could not be recorded" }
            }
            val followUp = if (installedName == null || (!request.options.requestUpdateOwnership && request.options.dexopt == InstallerContract.DEXOPT_NONE)) {
                InstallFollowUp()
            } else try {
                if (request.options.dexopt != InstallerContract.DEXOPT_NONE) listener.onStage(InstallerContract.STAGE_OPTIMIZING)
                afterInstallation(request, installedName, deadline, checkActive)
            } catch (failure: Exception) {
                if (failure is InterruptedException) Thread.currentThread().interrupt()
                InstallFollowUp.failed(request.options, failure)
            }
            return InstallEngine.Result(installedName, notes + followUp.notes, interaction, followUp)
        } catch (failure: Exception) {
            if (failure is InterruptedException) Thread.currentThread().interrupt()
            throw InstallFailure.from(failure, name)
        } finally {
            if (!terminalReceived) runCatching { session?.abandon() }
            runCatching { session?.close() }
        }
    }

    private fun validate(request: InstallEngine.Request): Parameters {
        request.prepared.failure()?.let { throw it }
        val options = request.options
        AdvancedInstallOptions.validate(options, authorizer, sdk)
        if (!InstallerContract.isInteraction(request.interaction)) throw RequestDocuments.invalid("Unknown interaction: ${request.interaction}")
        if (options.authorizer != InstallerContract.AUTHORIZER_AUTO && options.authorizer != authorizer.id) {
            throw RequestDocuments.invalid("The selected engine does not match the requested authorizer")
        }
        RequestDocuments.timeoutOf(options.timeoutMillis, "installation")
        RequestDocuments.userOf(options.user, "installation")
        RequestDocuments.packageNameOf(options.installer, "installer", required = false)
        val silent = request.interaction == InstallerContract.INTERACTION_SILENT
        if (!authorizer.privileged && (silent || options.privilegedOptions.isNotEmpty())) {
            throw InstallFailure(InstallerErrorCodes.AUTHORIZER_REQUIRED, "Silent installation and privileged options require Shizuku or Root")
        }
        if (request.userId < 0 || (options.user in listOf(InstallerContract.USER_CURRENT, InstallerContract.USER_ALL) && request.userId != currentUser) ||
            (options.user.toIntOrNull()?.let { it != request.userId } == true)) {
            throw RequestDocuments.invalid("The resolved target user does not match the request")
        }
        val apks = request.prepared.apks
        if (apks.isEmpty() || apks.size > InstallerContract.MAX_SPLITS_PER_PACKAGE) throw RequestDocuments.invalid("Invalid APK count")
        val names = hashSetOf<String>()
        var total = 0L
        for (apk in apks) {
            PrivilegedOptions.validateName(apk.name)
            if (!names.add(apk.name) || apk.size <= 0 || apk.size > Long.MAX_VALUE - total) throw RequestDocuments.invalid("Invalid APK name or size")
            if (!apk.file.isFile) throw InstallFailure(InstallerErrorCodes.SOURCE_NOT_FOUND, "Missing staged APK: ${apk.name}")
            if (apk.file.length() != apk.size) throw InstallFailure(InstallerErrorCodes.INVALID_PACKAGE, "APK size changed after preparation: ${apk.name}")
            total += apk.size
        }
        var flags = PrivilegedOptions.INSTALL_REPLACE_EXISTING
        if (options.allowDowngrade) flags = flags or PrivilegedOptions.INSTALL_REQUEST_DOWNGRADE or PrivilegedOptions.INSTALL_ALLOW_DOWNGRADE
        if (options.allowTestOnly) flags = flags or PrivilegedOptions.INSTALL_ALLOW_TEST
        if (options.grantAllRequestedPermissions) flags = flags or PrivilegedOptions.INSTALL_GRANT_ALL_REQUESTED_PERMISSIONS
        if (options.user == InstallerContract.USER_ALL) flags = flags or PrivilegedOptions.INSTALL_ALL_USERS
        val notes = mutableListOf<String>()
        if (options.bypassLowTargetSdk) {
            if (sdk >= 34) flags = flags or PrivilegedOptions.INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK
            else notes += "bypassLowTargetSdk was ignored: Android API 34 or later is required"
        }
        return Parameters(total, flags, notes)
    }
}

internal class NoneInstallEngine(context: Context) : SessionInstallEngine(Authorizer.NONE) {
    private val context = context.applicationContext

    override fun afterInstallation(request: InstallEngine.Request, packageName: String,
        deadlineMillis: Long, checkActive: () -> Unit): InstallFollowUp =
        InstallFollowUp.readOwner(context, packageName, request.options.requestUpdateOwnership)

    override fun openSession(request: InstallEngine.Request, parameters: Parameters, deadlineMillis: Long): Session {
        requireWorkerThread()
        val installer = nonePlatformCall { context.packageManager.packageInstaller }
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setSize(parameters.totalBytes)
            request.prepared.packageName?.let(::setAppPackageName)
            if (Build.VERSION.SDK_INT >= 31) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
            AdvancedSessionParameters.apply(this, request.options)
        }
        val id = nonePlatformCall { platformIo { installer.createSession(params) } }
        var session: PackageInstaller.Session? = null
        try {
            val opened = nonePlatformCall { platformIo { installer.openSession(id) } }.also { session = it }
            val ticket = InstallStatusBridge.open(context, requiresUnknownSourcesPermission = true)
            return object : StatusSession(ticket) {
                override fun openWrite(apk: PlannedApk, checkActive: () -> Unit): OutputStream =
                    nonePlatformCall { opened.openWrite(apk.name, 0, apk.size) }
                override fun fsync(output: OutputStream) = nonePlatformCall { opened.fsync(output) }
                override fun commit() = nonePlatformCall { opened.commit(ticket.sender) }
                override fun abandon() = installer.abandonSession(id)
                override fun close() {
                    try { opened.close() } finally { ticket.close() }
                }
            }
        } catch (failure: Exception) {
            runCatching { session?.close() }
            runCatching { installer.abandonSession(id) }
            throw failure
        }
    }
}

internal class PrivilegedInstallEngine(
    context: Context,
    private val authorizer: Authorizer,
    private val acquireRecovery: (Long) -> IPrivilegedInstaller = { timeout -> PrivilegedClient.get(context).acquire(authorizer, timeout) },
    private val acquire: (Long) -> IPrivilegedInstaller = { timeout -> PrivilegedClient.get(context).acquire(authorizer, timeout) },
) : SessionInstallEngine(authorizer) {
    private val context = context.applicationContext
    private var cachedUid: Pair<IBinder, Int>? = null
    private var installedTransport: IPrivilegedInstaller? = null

    override fun afterInstallation(request: InstallEngine.Request, packageName: String,
        deadlineMillis: Long, checkActive: () -> Unit): InstallFollowUp {
        checkActive()
        val service = requireNotNull(installedTransport)
        val remaining = (deadlineMillis - SystemClock.elapsedRealtime()).coerceIn(0, 30_000)
        return InstallFollowUp.decode(request.options, service.postInstall(packageName, request.userId,
            request.options.dexopt, request.options.requestUpdateOwnership, remaining))
    }

    init { require(authorizer.privileged) { "A privileged engine requires Shizuku or Root" } }

    @Synchronized
    private fun serviceUid(service: IPrivilegedInstaller): Int {
        val binder = service.asBinder()
        cachedUid?.takeIf { it.first === binder }?.let { return it.second }
        return service.uid.also { cachedUid = binder to it }
    }

    override fun openSession(request: InstallEngine.Request, parameters: Parameters, deadlineMillis: Long): Session {
        requireWorkerThread()
        val remaining = deadlineMillis - SystemClock.elapsedRealtime()
        if (remaining <= 0) throw InstallFailure(InstallerErrorCodes.TIMEOUT, "The installation timed out before binding")
        val service = acquire(minOf(remaining, PrivilegedClient.BIND_TIMEOUT_MILLIS))
        if (SystemClock.elapsedRealtime() >= deadlineMillis) throw InstallFailure(InstallerErrorCodes.TIMEOUT, "The installation timed out while binding")
        val installer = request.options.installer ?: if (serviceUid(service) == 2000) "com.android.shell" else context.packageName
        val params = Bundle().apply {
            putInt(PrivilegedOptions.FLAGS, parameters.flags)
            putLong(PrivilegedOptions.SIZE, parameters.totalBytes)
            request.prepared.packageName?.let { putString(PrivilegedOptions.PACKAGE_NAME, it) }
            if (request.options.requestUpdateOwnership) putBoolean(PrivilegedOptions.REQUEST_UPDATE_OWNERSHIP, true)
            request.options.installReason?.let { putInt(PrivilegedOptions.INSTALL_REASON, AdvancedInstallOptions.reasonValue(it)) }
            request.options.packageSource?.let { putInt(PrivilegedOptions.PACKAGE_SOURCE, AdvancedInstallOptions.sourceValue(it)) }
        }
        val id = privilegedInstallerCall { service.createSession(params, installer, request.userId) }
        installedTransport = service
        try {
            val recovery = service.getSessionRecoveryInfo(id)
            val ticket = InstallStatusBridge.open(context)
            return object : StatusSession(ticket) {
                override fun openWrite(apk: PlannedApk, checkActive: () -> Unit): OutputStream =
                    CancellablePipeOutputStream(privilegedInstallerCall { service.openWrite(id, apk.name, apk.size) }, checkActive) {
                        privilegedInstallerCall { service.checkWriteStatus(id) }
                    }
                // The privileged service drains, validates and fsyncs each pipe before commit returns.
                override fun fsync(output: OutputStream) = Unit
                override fun commit() = privilegedInstallerCall { service.commit(id, ticket.sender) }
                override fun await(deadlineMillis: Long, checkActive: () -> Unit, onUserAction: (Intent) -> Unit): InstallStatusBridge.Status =
                    ticket.await(InstallerContract.DEFAULT_USER_ACTION_TIMEOUT_MILLIS, deadlineMillis, {
                        checkActive()
                        // A queued genuine terminal result still takes priority inside await.
                        // Losing the service must not turn into a misleading result timeout.
                        if (!service.asBinder().isBinderAlive) throw DeadObjectException()
                    }, onUserAction)
                override fun abandon() {
                    try { service.abandon(id) } catch (death: DeadObjectException) {
                        if (recovery == null || service.asBinder().isBinderAlive) throw death
                        // A killed process cannot run its cleanup. Reconnect only to abandon the
                        // already returned platform id; this never repeats an installation step.
                        val replacement = acquireRecovery(RECOVERY_BIND_TIMEOUT_MILLIS)
                        check(replacement.asBinder() !== service.asBinder()) { "The failed service was not replaced" }
                        replacement.abandonRecoveredSession(id, recovery)
                    }
                }
                override fun close() {
                    // Also release the privileged service's ownership record after a terminal result.
                    try { service.release(id) } finally { ticket.close() }
                }
            }
        } catch (failure: Exception) {
            runCatching { service.abandon(id) }
            throw failure
        }
    }

    private companion object { const val RECOVERY_BIND_TIMEOUT_MILLIS = 5_000L }
}

private abstract class StatusSession(private val ticket: InstallStatusBridge.Ticket) : SessionInstallEngine.Session {
    override fun await(deadlineMillis: Long, checkActive: () -> Unit, onUserAction: (Intent) -> Unit): InstallStatusBridge.Status =
        ticket.await(InstallerContract.DEFAULT_USER_ACTION_TIMEOUT_MILLIS, deadlineMillis, checkActive, onUserAction)
}

/** Pipe backpressure must not prevent cancellation or deadline checks if the remote reader stalls. */
internal class CancellablePipeOutputStream(
    private val descriptor: ParcelFileDescriptor,
    private val checkActive: () -> Unit,
    private val checkRemoteWrite: () -> Unit,
) : OutputStream() {
    constructor(descriptor: ParcelFileDescriptor, checkActive: () -> Unit) : this(descriptor, checkActive, {})

    init {
        try {
            HiddenApiAccess.setNonBlocking(descriptor.fileDescriptor)
        } catch (failure: Exception) {
            descriptor.close()
            throw failure
        }
    }

    override fun write(value: Int) = write(byteArrayOf(value.toByte()), 0, 1)

    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        if (offset < 0 || length < 0 || offset > buffer.size - length) throw IndexOutOfBoundsException()
        val poll = StructPollfd().apply { fd = descriptor.fileDescriptor; events = OsConstants.POLLOUT.toShort() }
        var written = 0
        while (written < length) {
            checkActive()
            try {
                val count = Os.write(descriptor.fileDescriptor, buffer, offset + written, length - written)
                if (count <= 0) throw IOException("The privileged APK pipe stopped accepting data")
                written += count
            } catch (failure: ErrnoException) {
                if (failure.errno == OsConstants.EINTR) continue
                if (failure.errno == OsConstants.EPIPE) {
                    // The remote publishes its destination error before closing the read side.
                    // A concurrent cancellation takes precedence over a stale storage error.
                    checkActive()
                    try {
                        checkRemoteWrite()
                    } catch (remoteFailure: Exception) {
                        checkActive()
                        if (remoteFailure is DeadObjectException) throw InstallFailure.from(remoteFailure)
                        if (remoteFailure is InstallFailure && remoteFailure.code == InstallerErrorCodes.INSUFFICIENT_STORAGE) throw remoteFailure
                        throw InstallFailure(InstallerErrorCodes.INSTALL_FAILED,
                            "The privileged installer stopped reading APK data",
                            systemMessage = remoteFailure.message, cause = remoteFailure)
                    }
                    checkActive()
                    throw InstallFailure(InstallerErrorCodes.INSTALL_FAILED,
                        "The privileged installer stopped reading APK data",
                        systemMessage = failure.message, cause = failure)
                }
                if (failure.errno != OsConstants.EAGAIN) throw IOException("Cannot write to the privileged APK pipe", failure)
                try { Os.poll(arrayOf(poll), 250) } catch (interrupted: ErrnoException) {
                    if (interrupted.errno != OsConstants.EINTR) throw IOException("Cannot poll the privileged APK pipe", interrupted)
                }
            }
        }
    }

    override fun close() = descriptor.close()
}

private fun requireWorkerThread() {
    check(Looper.myLooper() != Looper.getMainLooper()) { "InstallEngine must run on a worker thread" }
}

/** Do not recognize this marker outside calls to the plugin's own privileged installer Binder. */
internal inline fun <T> privilegedInstallerCall(block: () -> T): T = try {
    block()
} catch (failure: IllegalStateException) {
    if (failure.message == PrivilegedOptions.ERROR_INSUFFICIENT_STORAGE) throw StorageErrors.failure(failure)
    throw failure
}

/** Only platform operations of the regular installer use this mapping; caller callbacks do not. */
internal inline fun <T> nonePlatformCall(block: () -> T): T = try {
    block()
} catch (failure: SecurityException) {
    throw InstallFailure(InstallerErrorCodes.BLOCKED_BY_POLICY,
        failure.message ?: "Package installation was refused by system policy",
        systemMessage = failure.message, cause = failure)
}

private inline fun <T> platformIo(block: () -> T): T = try {
    block()
} catch (failure: IOException) {
    if (StorageErrors.isInsufficientStorage(failure)) throw StorageErrors.failure(failure)
    throw InstallFailure(InstallerErrorCodes.INSTALL_FAILED,
        failure.message ?: "Cannot write the package installation session", cause = failure)
}
