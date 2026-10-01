package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.PrivilegedClient
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.PrivilegedOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.PackageManagerHidden
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes

internal class DefaultInstaller(private val backend: Backend) {
    data class State(val component: String?, val isSelf: Boolean, val method: String, val entryAvailable: Boolean, val fingerprint: String,
        val persistentConfigured: Boolean = false)
    interface Backend {
        fun read(): State
        fun set(enable: Boolean, authorizer: Authorizer, checkActive: () -> Unit): Int
        fun setPersistent(enable: Boolean, authorizer: Authorizer, checkActive: () -> Unit): Int =
            throw InstallFailure(InstallerErrorCodes.AUTHORIZER_REQUIRED, "Persistent defaults require Dhizuku or a supported system-UID Root bridge")
    }
    @Volatile private var requiresClearFor: String? = null
    val available: Boolean get() = backend.read().entryAvailable

    fun state() = backend.read().let { value ->
        document(value, requiresClearFor == value.fingerprint)
    }

    @Synchronized fun set(enable: Boolean, authorizer: Authorizer, checkActive: () -> Unit = {}): com.google.gson.JsonObject {
        checkActive()
        if (!authorizer.privileged) throw InstallFailure(InstallerErrorCodes.AUTHORIZER_REQUIRED, "Selecting a default installer requires Shizuku or Root")
        if (enable && !backend.read().entryAvailable) throw RequestDocuments.invalid("The default-installer capability is unavailable until the APK entry is installed")
        val changed = backend.set(enable, authorizer, checkActive)
        val after = backend.read()
        requiresClearFor = if (changed == PrivilegedOptions.DEFAULT_REQUIRES_CLEAR) after.fingerprint else null
        if (enable && changed != PrivilegedOptions.DEFAULT_REQUIRES_CLEAR && (changed != 4 || !after.isSelf)) {
            throw InstallFailure(InstallerErrorCodes.INTERNAL, "The system did not select this installer for all APK intent filters")
        }
        return document(after, requiresClearFor == after.fingerprint)
    }

    @Synchronized fun setPersistent(enable: Boolean, authorizer: Authorizer, checkActive: () -> Unit = {}): com.google.gson.JsonObject {
        checkActive()
        if (enable && !backend.read().entryAvailable) throw RequestDocuments.invalid("Installer entry is unavailable")
        val changed = backend.setPersistent(enable, authorizer, checkActive)
        val after = backend.read()
        if (enable && (changed != 4 || !after.isSelf)) {
            throw InstallFailure(InstallerErrorCodes.INTERNAL, "The system did not apply the persistent APK policy")
        }
        // This response acknowledges this completed write; passive reads do not infer a live
        // system persistent policy from the local receipt.
        return document(if (enable) after.copy(method = InstallerContract.DEFAULT_METHOD_PERSISTENT) else after, false)
    }

    private fun document(state: State, requiresClear: Boolean) =
        InstallDocuments.defaultInstallerState(state.component, state.isSelf, state.method, requiresClear).apply {
            addProperty(InstallerContract.FIELD_PERSISTENT_CONFIGURED, state.persistentConfigured)
        }
}

/** Cheap reads use public package queries. Only writes bind a privileged service. */
internal class AndroidDefaultInstaller(context: Context,
    private val component: ComponentName = ComponentName(context.packageName, "${context.packageName}.ui.ExternalInstallActivity"),
) : DefaultInstaller.Backend {
    private val context = context.applicationContext
    private val packages = context.packageManager
    init { require(component.packageName == context.packageName) }

    @Suppress("DEPRECATION")
    override fun read(): DefaultInstaller.State {
        val intents = PackageManagerHidden.INSTALL_ACTIONS.flatMap { action -> listOf("content", "file").map { PackageManagerHidden.intent(action, it) } }
        val candidates = intents.map { intent -> packages.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .filter { it.activityInfo.enabled && it.activityInfo.exported }.map { ComponentName(it.activityInfo.packageName, it.activityInfo.name) } }
        val resolved = intents.mapIndexed { i, intent -> packages.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo
            ?.let { ComponentName(it.packageName, it.name) }?.takeIf { it in candidates[i] } }
        val primary = resolved.first()
        val filters = mutableListOf<IntentFilter>()
        val activities = mutableListOf<ComponentName>()
        if (primary != null) packages.getPreferredActivities(filters, activities, primary.packageName)
        val preferred = filters.indices.any { i -> activities.getOrNull(i) == primary && matches(filters[i], intents.first()) }
        return DefaultInstaller.State(primary?.flattenToString(), resolved.all { it == component },
            if (preferred) InstallerContract.DEFAULT_METHOD_PREFERRED else InstallerContract.DEFAULT_METHOD_NONE,
            candidates.all { component in it }, resolved.joinToString("|") { it?.flattenToString().orEmpty() },
            PersistentDefaultPolicy(context).isConfiguredForCurrentOwner())
    }

    override fun set(enable: Boolean, authorizer: Authorizer, checkActive: () -> Unit): Int {
        if (authorizer == Authorizer.DHIZUKU) throw RequestDocuments.invalid("Dhizuku uses persistent default mode, not preferred mode")
        val service = PrivilegedClient.get(context).acquire(authorizer)
        checkActive()
        return service.setDefaultInstaller(component, enable)
    }

    override fun setPersistent(enable: Boolean, authorizer: Authorizer, checkActive: () -> Unit): Int {
        if (authorizer == Authorizer.ROOT) return PersistentDefaultPolicy(context).setRoot(enable, checkActive)
        if (authorizer != Authorizer.DHIZUKU) throw InstallFailure(InstallerErrorCodes.AUTHORIZER_REQUIRED, "Persistent defaults require Dhizuku or a supported system-UID Root bridge")
        return PersistentDefaultPolicy(context).set(enable, component, checkActive)
    }

    private fun matches(filter: IntentFilter, intent: Intent) = filter.match(intent.action, intent.type, intent.scheme,
        intent.data, intent.categories, "ThreeSetup") >= 0
}
