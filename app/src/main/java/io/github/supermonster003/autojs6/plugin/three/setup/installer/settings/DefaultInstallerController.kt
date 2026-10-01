package io.github.supermonster003.autojs6.plugin.three.setup.installer.settings

import android.app.Dialog
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.R
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.AndroidDefaultInstaller
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.DefaultInstaller
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.HostAppearanceActivity
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** Binder, Home and settings share the same serialized writes and transient requires-clear result. */
internal object DefaultInstallerLock {
    @Volatile private var shared: DefaultInstaller? = null
    fun get(context: Context): DefaultInstaller = shared ?: synchronized(this) {
        shared ?: DefaultInstaller(AndroidDefaultInstaller(context.applicationContext)).also { shared = it }
    }
}

internal data class DefaultInstallerState(val component: String?, val isSelf: Boolean, val method: String, val requiresClear: Boolean) {
    companion object {
        fun decode(value: JsonObject) = DefaultInstallerState(
            value.get(InstallerContract.FIELD_COMPONENT)?.takeUnless { it.isJsonNull }?.asString,
            value.get(InstallerContract.FIELD_IS_SELF).asBoolean,
            value.get(InstallerContract.FIELD_METHOD).asString,
            value.get(InstallerContract.FIELD_REQUIRES_CLEAR).asBoolean,
        )
    }
}

/** Lifecycle-bound UI adapter. Package queries, authorization and privileged IPC run on its worker. */
internal class DefaultInstallerController(
    private val activity: HostAppearanceActivity,
    private val onState: (DefaultInstallerState) -> Unit,
    private val onFailure: (Int) -> Unit,
    private val onBusy: (Boolean) -> Unit = {},
) : Closeable {
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "default-installer-ui") }
    private val main = Handler(Looper.getMainLooper())
    private val app = activity.applicationContext
    private var generation = 0
    private var pending: Future<*>? = null
    private var dialog: Dialog? = null
    @Volatile private var closed = false
    private var writing = false
    private var lastState: DefaultInstallerState? = null

    fun refresh() {
        if (closed || writing) return
        val expected = ++generation
        pending?.cancel(true)
        pending = worker.submit {
            val state = runCatching { DefaultInstallerState.decode(DefaultInstallerLock.get(app).state()) }
            deliver(expected) { state.fold(::receive) { onFailure(R.string.default_installer_read_failed) } }
        }
    }

    fun setDefault(enable: Boolean) {
        if (closed || writing) return
        val expected = ++generation
        pending?.cancel(true)
        onBusy(true)
        pending = worker.submit {
            val candidates = runCatching {
                val preferences = InstallerPreferences.read(app).authorizers
                val states = AuthorizerStates.states(app)
                preferences.order.filter { it in listOf(Authorizer.SHIZUKU, Authorizer.ROOT) && it in preferences.enabled && states[it]?.let { state -> state.available && state.running } == true }
            }.getOrDefault(emptyList())
            deliver(expected) {
                onBusy(false)
                if (candidates.isEmpty()) guide()
                else {
                    dialog?.dismiss()
                    dialog = SettingsUi(activity, activity.kit).confirmedChoice(
                        if (enable) R.string.default_installer_set else R.string.default_installer_clear,
                        candidates.map { SettingsChoice(activity.getString(if (it == Authorizer.SHIZUKU) R.string.settings_shizuku else R.string.settings_root),
                            activity.getString(R.string.default_installer_authorize_note)) }, 0,
                    ) { selected -> write(enable, candidates[selected]); true }
                }
            }
        }
    }

    private fun write(enable: Boolean, authorizer: Authorizer) {
        writing = true
        onBusy(true)
        val expected = ++generation
        pending = worker.submit {
            val result = runCatching {
                if (!AuthorizerStates.request(app, authorizer, 30_000)) throw InstallFailure(InstallerErrorCodes.AUTHORIZER_DENIED, "Authorization denied")
                fun active() { if (closed || Thread.currentThread().isInterrupted) throw InterruptedException() }
                active()
                val resolved = InstallerPreferences.resolveAuthorizer(app, authorizer.id)
                DefaultInstallerState.decode(DefaultInstallerLock.get(app).set(enable, resolved, ::active))
            }
            deliver(expected) {
                writing = false
                onBusy(false)
                result.fold({ state ->
                    receive(state)
                    if (state.requiresClear) guide(R.string.default_installer_requires_clear)
                }, { error ->
                    val failure = InstallFailure.from(error)
                    onFailure(when (failure.code) {
                        InstallerErrorCodes.AUTHORIZER_DENIED -> R.string.default_installer_denied
                        InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, InstallerErrorCodes.AUTHORIZER_REQUIRED -> R.string.default_installer_authorizer_unavailable
                        InstallerErrorCodes.TIMEOUT -> R.string.default_installer_timeout
                        else -> R.string.default_installer_oem_failed
                    })
                    refresh()
                })
            }
        }
    }

    private fun guide(message: Int = R.string.default_installer_guide) {
        dialog?.dismiss()
        val ui = SettingsUi(activity, activity.kit)
        dialog = ui.dialog(activity.getString(R.string.default_installer_title)) { layout, prompt ->
            layout.content.addView(activity.kit.text(activity.getString(message), 14f, activity.kit.palette.muted))
            layout.actions.addView(activity.kit.textButton(activity.getString(R.string.action_cancel)) { prompt.cancel() })
            layout.actions.addView(activity.kit.textButton(activity.getString(R.string.default_installer_open_settings)) { prompt.dismiss(); openSystemSettings() })
        }
    }

    fun openSystemSettings() {
        val currentPackage = lastState?.takeIf { it.requiresClear }?.component?.let(ComponentName::unflattenFromString)?.packageName
        val uri = Uri.fromParts("package", currentPackage ?: app.packageName, null)
        val action = if (Build.VERSION.SDK_INT >= 31) Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS else Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val success = runCatching { activity.startActivity(Intent(action, uri)); true }.getOrDefault(false) ||
            runCatching { activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)); true }.getOrDefault(false)
        if (!success) onFailure(R.string.default_installer_settings_unavailable)
    }

    private fun deliver(expected: Int, action: () -> Unit) = main.post {
        if (!closed && generation == expected && !activity.isFinishing && !activity.isDestroyed) action()
    }

    private fun receive(state: DefaultInstallerState) {
        lastState = state
        onState(state)
    }

    fun dismissDialogs() { dialog?.dismiss(); dialog = null }

    override fun close() {
        closed = true
        generation++
        pending?.cancel(true)
        dialog?.dismiss()
        dialog = null
        worker.shutdownNow()
    }
}
