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
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PackageStaging
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.PluginConfirmation
import org.autojs.plugin.installer.api.*
import java.io.Closeable
import java.util.concurrent.Executors

/** The real V1 router. Only metadata discovery is public to holders of a delegated Binder. */
internal class InstallerBinder(context: Context, private val guard: CallerGuard = HostCallerGuard(context)) : IInstallerPlugin.Stub(), Closeable {
    private val context = context.applicationContext
    private val queue = InstallerWorkQueue()
    private val reaper = Executors.newSingleThreadScheduledExecutor()
    private val sessions = SessionRegistry(SystemClock::elapsedRealtime, scheduler = reaper)
    private val defaults = DefaultInstaller(AndroidDefaultInstaller(context))
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
                if (decoded.interaction == InstallerContract.INTERACTION_DIALOG && selected.privileged) {
                    PluginConfirmation.uninstall(context, decoded, selected, userId, deadline, check)
                }
                val engine = if (selected.privileged) PrivilegedUninstallEngine(context, selected) else NoneUninstallEngine(context)
                val result = engine.uninstall(decoded, userId, object : InstallEngine.Listener {
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
            var id = InstallerBundles.hostId(request)
            try {
                val decoded = InstallRequest.parse(InstallerBundles.request(request), sources?.size ?: 0)
                if (id.isEmpty() || id != decoded.id) throw RequestDocuments.invalid("hostSessionId must match the request id")
                id = decoded.id
                lease = sessions.reserve(owner, id)
                val descriptors = requireNotNull(sources).map { SourceDescriptors.validate(it); requireNotNull(it) }
                environment = DescriptorInstallEnvironment.acquire(context, descriptors) { prepared, target, deadline, check ->
                    PluginConfirmation.install(context, decoded, prepared, target, deadline, check)
                }
                val reserved = lease
                lateinit var core: InstallSession
                val death = CallbackDeath(callback.asBinder()) { reserved.close() }
                core = InstallSession(decoded, environment, object : InstallSession.Listener {
                    override fun onStage(stage: String, detail: com.google.gson.JsonObject) =
                        callback.onStage(id, stage, InstallerBundles.document(InstallerContract.KEY_DETAIL_JSON, detail))
                    override fun onProgress(progress: Float, detail: com.google.gson.JsonObject) =
                        callback.onProgress(id, progress, InstallerBundles.document(InstallerContract.KEY_DETAIL_JSON, detail))
                    override fun onCompleted(result: com.google.gson.JsonObject) {
                        reserved.finished()
                        try { callback.onCompleted(id, InstallerBundles.document(InstallerContract.KEY_RESULT_JSON, result)) }
                        catch (failure: InstallFailure) { callback.onFailed(id, InstallerBundles.error(failure)) }
                    }
                    override fun onFailed(failure: InstallFailure) { reserved.finished(); callback.onFailed(id, InstallerBundles.error(failure)) }
                }, SystemClock::elapsedRealtime)
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
                core.start(queue.executor)
                return handle
            } catch (failure: Exception) {
                environment?.close()
                lease?.close()
                lease?.finished()
                runCatching { callback.onFailed(id, InstallerBundles.error(InstallFailure.from(failure))) }
                return null
            }
        } finally { if (remote) sources?.forEach { runCatching { it?.close() } } }
    }

    private fun resolve(value: String): Authorizer = AuthorizerResolver.resolve(value, AuthorizerStates.states(context))
    private fun authorizer(value: String?): Authorizer = Authorizer.fromId(value) ?: throw RequestDocuments.invalid("Unknown explicit authorizer")
    private inline fun withCallback(callback: IInstallerCallback?, action: (IInstallerCallback) -> Unit) {
        requireNotNull(callback) { "Callback is required" }
        try { action(callback) } catch (failure: Exception) { queue.reject(callback, InstallFailure.from(failure)) }
    }
    override fun close() { sessions.close(); reaper.shutdownNow(); queue.close() }
}
