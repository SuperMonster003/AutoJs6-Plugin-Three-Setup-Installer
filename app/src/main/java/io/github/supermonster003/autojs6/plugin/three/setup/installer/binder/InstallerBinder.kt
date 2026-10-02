package io.github.supermonster003.autojs6.plugin.three.setup.installer.binder

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import io.github.supermonster003.autojs6.plugin.three.setup.installer.*
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.*
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.*
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.InstallerPreferences
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PackageStaging
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.PluginConfirmation
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallationUi
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallNotifications
import org.autojs.plugin.installer.api.*
import java.io.Closeable
import java.util.concurrent.Executors

/** V1 routes plus capability-negotiated V2 operations. Only metadata discovery is public. */
internal class InstallerBinder(context: Context, private val guard: CallerGuard = HostCallerGuard(context)) : IInstallerPlugin.Stub(), Closeable {
    private val context = context.applicationContext
    private val queue = InstallerWorkQueue()
    private val reaper = Executors.newSingleThreadScheduledExecutor()
    private val sessions = SessionRegistry(SystemClock::elapsedRealtime, scheduler = reaper)
    private val defaults = io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.DefaultInstallerLock.get(context)
    init {
        queue.executor.execute { PackageStaging.cleanStale(this.context) }
    }

    override fun getInfo() = context.threeSetupInstallerPluginRuntimeInfo().toPluginInfo()
    override fun getCapabilities() = context.threeSetupInstallerPluginRuntimeInfo().capabilitiesBundle()
    override fun getAuthorizerState(authorizer: String?): Bundle {
        guard.enforceHost()
        return try {
            InstallerBundles.document(InstallerContract.KEY_STATUS_JSON, InstallDocuments.authorizerState(AuthorizerStates.state(context, authorizer(authorizer))))
        } catch (failure: InstallFailure) { InstallerBundles.error(failure) }
    }
    override fun getDefaultInstallerState(): Bundle {
        guard.enforceHost()
        return InstallerBundles.document(InstallerContract.KEY_STATUS_JSON, defaults.state())
    }

    override fun requestAuthorizer(authorizer: String?, callback: IInstallerCallback?) {
        guard.enforceHost()
        withCallback(callback) { answer ->
            val selected = authorizer(authorizer)
            queue.submit(answer, InstallerContract.DEFAULT_USER_ACTION_TIMEOUT_MILLIS) { _, check ->
                check()
                val granted = AuthorizerStates.request(context, selected, InstallerContract.DEFAULT_USER_ACTION_TIMEOUT_MILLIS)
                check()
                if (!granted) {
                    val state = AuthorizerStates.state(context, selected)
                    throw InstallFailure(if (!state.available || !state.running) InstallerErrorCodes.AUTHORIZER_UNAVAILABLE else InstallerErrorCodes.AUTHORIZER_DENIED,
                        state.reason ?: "Authorization was not granted")
                }
                InstallDocuments.granted(true, selected.id)
            }
        }
    }

    override fun inspect(source: ParcelFileDescriptor?, request: Bundle?, callback: IInstallerCallback?) {
        val remote = Binder.getCallingPid() != Process.myPid()
        try {
            guard.enforceHost()
            withCallback(callback) { answer ->
                val decoded = InspectRequest.parse(InstallerBundles.request(request))
                SourceDescriptors.validate(source)
                val owned = ParcelFileDescriptor.dup(requireNotNull(source).fileDescriptor)
                queue.submit(answer, resource = owned) { _, check -> PackageInspector(context).inspect(owned, decoded, check) }
            }
        } finally { if (remote) runCatching { source?.close() } }
    }

    override fun getUsers(request: Bundle?, callback: IInstallerCallback?) {
        guard.enforceHost()
        withCallback(callback) { answer ->
            val decoded = AuthorizerRequest.parse(InstallerBundles.request(request), "users request")
            queue.submit(answer) { _, check ->
                val selected = resolve(decoded.authorizer)
                check()
                val users = DeviceUsers(context).list(selected)
                check()
                InstallDocuments.users(users)
            }
        }
    }

    override fun setDefaultInstaller(enable: Boolean, request: Bundle?, callback: IInstallerCallback?) {
        guard.enforceHost()
        withCallback(callback) { answer ->
            val decoded = AuthorizerRequest.parse(InstallerBundles.request(request), "default installer request")
            queue.submit(answer) { _, check -> check(); defaults.set(enable, resolve(decoded.authorizer), check) }
        }
    }

    override fun setDefaultInstallerV2(enable: Boolean, request: Bundle?, callback: IInstallerCallback?) {
        guard.enforceHost()
        withCallback(callback) { answer ->
            val json = InstallerBundles.request(request)
            if ((request?.getInt(InstallerContract.KEY_CONTRACT_VERSION) ?: 0) < 2) throw RequestDocuments.invalid("Persistent defaults require installer contract version 2 or later")
            val decoded = DefaultModeRequest.parse(json)
            queue.submit(answer) { _, check ->
                check()
                if (decoded.mode == InstallerContract.DEFAULT_MODE_PERSISTENT) {
                    val preferences = InstallerPreferences.read(context).authorizers
                    val selected = PersistentAuthorizer.resolve(decoded.authorizer, AuthorizerStates.states(context),
                        preferences.order, preferences.enabled, android.os.Build.VERSION.SDK_INT, Process.myUid() / 100000)
                    defaults.setPersistent(enable, selected, check)
                } else defaults.set(enable, resolve(decoded.authorizer), check)
            }
        }
    }

    override fun uninstall(request: Bundle?, callback: IInstallerCallback?) {
        guard.enforceHost()
        withCallback(callback) { answer ->
            val decoded = UninstallRequest.parse(InstallerBundles.request(request))
            queue.submit(answer, decoded.timeoutMillis) { deadline, check ->
                val selected = resolve(decoded.authorizer)
                val userId = DeviceUsers(context).resolve(decoded.user, selected, minOf(decoded.timeoutMillis, PrivilegedClient.BIND_TIMEOUT_MILLIS))
                check()
                // Validate privilege requirements before displaying any confirmation.
                UninstallEngine.flags(decoded, selected, userId, DeviceUsers(context).currentId)
                val approved = if (decoded.interaction == InstallerContract.INTERACTION_DIALOG && selected.privileged) {
                    PluginConfirmation.uninstall(context, decoded, selected, userId, deadline, check)
                } else decoded
                val engine = when {
                    selected == Authorizer.DHIZUKU -> DhizukuUninstallEngine(context)
                    selected.privileged -> PrivilegedUninstallEngine(context, selected)
                    else -> NoneUninstallEngine(context)
                }
                val result = engine.uninstall(approved, userId, object : InstallEngine.Listener {
                    override fun onUserAction(intent: Intent) = UserActionLauncher.launch(context, intent)
                }, check, deadline)
                InstallDocuments.uninstallResult(result.packageName, result.authorizer)
            }
        }
    }

    override fun openSession(sources: Array<out ParcelFileDescriptor?>?, request: Bundle?, callback: IInstallerSessionCallback?): IInstallerSession? {
        val remote = Binder.getCallingPid() != Process.myPid()
        try {
            val owner = guard.enforceHost()
            requireNotNull(callback) { "Session callback is required" }
            var lease: SessionRegistry.Lease? = null
            var environment: DescriptorInstallEnvironment? = null
            var slot: Closeable? = null
            var presentation: InstallPresentation.Record? = null
            var id = InstallerBundles.hostId(request)
            try {
                val decoded = InstallRequest.parse(InstallerBundles.request(request), sources?.size ?: 0)
                if (id.isEmpty() || id != decoded.id) throw RequestDocuments.invalid("hostSessionId must match the request id")
                id = decoded.id
                lease = sessions.reserve(owner, id)
                val descriptors = requireNotNull(sources).map { SourceDescriptors.validate(it); requireNotNull(it) }
                slot = InstallSlots.acquire()
                val capacity = slot
                val reserved = lease
                lateinit var core: InstallSession
                val earlyCancellation = java.util.concurrent.atomic.AtomicBoolean()
                val cancellableCore = java.util.concurrent.atomic.AtomicReference<InstallSession?>()
                // AUTO is decided per parsed item, after its profile has selected the identity.
                val interactive = decoded.interaction == InstallerContract.INTERACTION_DIALOG
                val actual = decoded
                // Silent requests have a passive record for notification taps, never an automatic popup.
                val record = InstallPresentation.create(context, actual,
                    InstallPresentation.Callbacks(cancel = {
                        earlyCancellation.set(true)
                        cancellableCore.get()?.cancel()
                        Unit
                    }, close = { reserved.close() }))
                presentation = record
                lateinit var owned: DescriptorInstallEnvironment
                owned = DescriptorInstallEnvironment.acquire(context, descriptors,
                    configuration = { index, prepared, target, selectedRequest, deadline, check ->
                        InstallationUi.configure(context, record, owned, index, prepared, target, selectedRequest, deadline, check)
                    },
                    preparedListener = { index, prepared -> record?.onPrepared(index, prepared) },
                    availability = record::checkNotificationAvailable,
                    userAction = record::showSystemConfirmation,
                    safetyOwnerToken = record.token,
                    safetyReview = record::reviewSafety,
                )
                environment = owned
                val notificationToken = record?.token ?: java.util.UUID.randomUUID().toString()
                val death = CallbackDeath(callback.asBinder()) { reserved.close() }
                core = InstallSession(actual, owned, object : InstallSession.Listener {
                    override fun onStage(stage: String, detail: com.google.gson.JsonObject) {
                        record?.onStage(stage, detail)
                        InstallNotifications.update(context, notificationToken, actual.sources.first().displayName, stage,
                            core.status().progress, record?.activityIntent()) { core.cancel() }
                        callback.onStage(id, stage, InstallerBundles.document(InstallerContract.KEY_DETAIL_JSON, detail))
                    }
                    override fun onProgress(progress: Float, detail: com.google.gson.JsonObject) {
                        record?.onProgress(progress, detail)
                        InstallNotifications.update(context, notificationToken, actual.sources.first().displayName,
                            InstallerContract.STAGE_WRITING, progress, record?.activityIntent()) { core.cancel() }
                        callback.onProgress(id, progress, InstallerBundles.document(InstallerContract.KEY_DETAIL_JSON, detail))
                    }
                    override fun onItemResult(index: Int, result: com.google.gson.JsonObject) { record?.onItemResult(index, result) }
                    override fun onInstalled(index: Int, result: com.google.gson.JsonObject) { record?.onInstalled(index, result) }
                    override fun onOptionsResolved(index: Int, options: io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions,
                        interaction: String, profileName: String?) { record.onOptionsResolved(index, options, interaction, profileName) }
                    override fun onCompleted(result: com.google.gson.JsonObject) {
                        capacity.close()
                        record?.onCompleted(result)
                        val results = result.getAsJsonArray(InstallerContract.FIELD_RESULTS)?.map { it.asJsonObject } ?: listOf(result)
                        InstallNotifications.complete(context, notificationToken, results.all { it[InstallerContract.FIELD_OK]?.asBoolean == true }, openIntent = record?.activityIntent())
                        reserved.finished()
                        try { callback.onCompleted(id, InstallerBundles.document(InstallerContract.KEY_RESULT_JSON, result)) }
                        catch (failure: InstallFailure) { callback.onFailed(id, InstallerBundles.error(failure)) }
                    }
                    override fun onFailed(failure: InstallFailure) {
                        capacity.close()
                        record?.onFailed(failure)
                        InstallNotifications.complete(context, notificationToken, false, openIntent = record?.activityIntent())
                        reserved.finished()
                        callback.onFailed(id, InstallerBundles.error(failure))
                    }
                }, SystemClock::elapsedRealtime)
                cancellableCore.set(core)
                if (earlyCancellation.get()) core.cancel()
                val handle = object : IInstallerSession.Stub() {
                    override fun getId(): String { guard.enforceOwner(owner); return id }
                    override fun getStatus(): Bundle {
                        guard.enforceOwner(owner)
                        val status = core.status()
                        return InstallerBundles.document(InstallerContract.KEY_STATUS_JSON, InstallDocuments.sessionStatus(status.stage, status.index, status.progress))
                    }
                    override fun cancel(): Boolean { guard.enforceOwner(owner); return core.cancel() }
                    override fun close() { guard.enforceOwner(owner); reserved.close() }
                }
                reserved.attach { core.cancel(); death.close() }
                death.link()
                if (interactive) InstallationUi.show(context, record)
                core.start(queue.executor)
                return handle
            } catch (failure: Exception) {
                environment?.close()
                slot?.close()
                presentation?.let {
                    it.onFailed(InstallFailure.from(failure))
                    InstallNotifications.complete(context, it.token, false, openIntent = it.activityIntent())
                }
                lease?.close()
                lease?.finished()
                runCatching { callback.onFailed(id, InstallerBundles.error(InstallFailure.from(failure))) }
                return null
            }
        } finally { if (remote) sources?.forEach { runCatching { it?.close() } } }
    }

    private fun resolve(value: String): Authorizer = io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.InstallerPreferences.resolveAuthorizer(context, value)
    private fun authorizer(value: String?): Authorizer = Authorizer.fromId(value) ?: throw RequestDocuments.invalid("Unknown explicit authorizer")
    private inline fun withCallback(callback: IInstallerCallback?, action: (IInstallerCallback) -> Unit) {
        requireNotNull(callback) { "Callback is required" }
        try { action(callback) } catch (failure: Exception) { queue.reject(callback, InstallFailure.from(failure)) }
    }
    override fun close() { sessions.close(); reaper.shutdownNow(); queue.close() }
}
