package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Build
import java.util.concurrent.Executors

/** Stable components are the only source of the persisted launcher choice. */
internal enum class LauncherIconMode(val alias: String) {
    LIGHT("AdaptiveLightIconAlias"), DARK("AdaptiveDarkIconAlias"),
    AUTO("AdaptiveAutoIconAlias"), TRANSPARENT("TransparentIconAlias");

    fun component(context: Context) = ComponentName(context.packageName, "${context.packageName}.launcher.$alias")
}

internal object LauncherIconStatePolicy {
    fun enabled(mode: LauncherIconMode, state: Int) = state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
        (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && mode == LauncherIconMode.AUTO)

    fun resolve(states: Map<LauncherIconMode, Int>): LauncherIconMode {
        val explicit = LauncherIconMode.entries.filter { states.getValue(it) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED }
        return explicit.firstOrNull { it == LauncherIconMode.AUTO } ?: explicit.firstOrNull() ?: LauncherIconMode.AUTO
    }
}

/** Adapted from the project's own 3-Stove Agent implementation, with exact shortcut ownership rollback. */
internal object LauncherIcons {
    private val worker by lazy { Executors.newSingleThreadExecutor { Thread(it, "launcher-icon-normalize") } }
    private fun snapshot(context: Context) = LauncherIconMode.entries.associateWith {
        context.packageManager.getComponentEnabledSetting(it.component(context))
    }

    fun current(context: Context) = LauncherIconStatePolicy.resolve(snapshot(context))
    fun launcherActivity(context: Context) = current(context).component(context)
    fun prepare(context: Context) = normalizeAsync(context)
    @Synchronized fun normalize(context: Context) = select(context, current(context))

    fun normalizeAsync(context: Context, onComplete: (() -> Unit)? = null) {
        val app = context.applicationContext
        worker.execute {
            try { normalize(app) } catch (_: Exception) { /* Retry on the next foreground launch. */ }
            finally { onComplete?.invoke() }
        }
    }

    @Synchronized fun select(context: Context, mode: LauncherIconMode) {
        val pm = context.packageManager
        val before = snapshot(context)
        val shortcutBackup = mutableShortcuts(context)
        try {
            pm.setComponentEnabledSetting(mode.component(context), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            updateOwnership(context, shortcutBackup, mode.component(context))
            apply(context, LauncherIconMode.entries.associateWith {
                if (it == mode) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            })
        } catch (failure: Exception) {
            runCatching { apply(context, before) }
            if (Build.VERSION.SDK_INT >= 25 && shortcutBackup.isNotEmpty()) runCatching {
                check(context.getSystemService(ShortcutManager::class.java)?.updateShortcuts(shortcutBackup) != false)
            }
            throw failure
        }
        // Recovery is separate from ownership: never re-enable shortcuts the app disabled itself.
        if (Build.VERSION.SDK_INT >= 28) {
            val recovered = shortcutBackup.filter { !it.isEnabled && it.disabledReason == ShortcutInfo.DISABLED_REASON_APP_CHANGED }.map { it.id }
            if (recovered.isNotEmpty()) runCatching { context.getSystemService(ShortcutManager::class.java)?.enableShortcuts(recovered) }
        }
    }

    private fun apply(context: Context, states: Map<LauncherIconMode, Int>) {
        val pm = context.packageManager
        if (Build.VERSION.SDK_INT >= 33) {
            pm.setComponentEnabledSettings(states.map { (mode, state) ->
                PackageManager.ComponentEnabledSetting(mode.component(context), state, PackageManager.DONT_KILL_APP)
            })
        } else {
            states.entries.sortedBy { if (LauncherIconStatePolicy.enabled(it.key, it.value)) 0 else 1 }.forEach { (mode, state) ->
                pm.setComponentEnabledSetting(mode.component(context), state, PackageManager.DONT_KILL_APP)
            }
        }
    }

    private fun mutableShortcuts(context: Context): List<ShortcutInfo> {
        if (Build.VERSION.SDK_INT < 25) return emptyList()
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return emptyList()
        val ours = LauncherIconMode.entries.map { it.component(context) }.toSet() +
            ComponentName(context.packageName, "${context.packageName}.ui.HomeActivity")
        return (manager.dynamicShortcuts + manager.pinnedShortcuts).distinctBy { it.id }.filter {
            it.activity in ours && !it.isDeclaredInManifest && !(Build.VERSION.SDK_INT >= 30 && it.isImmutable)
        }
    }

    private fun updateOwnership(context: Context, originals: List<ShortcutInfo>, target: ComponentName) {
        if (Build.VERSION.SDK_INT < 25) return
        val aliases = LauncherIconMode.entries.map { it.component(context) }.toSet()
        val real = ComponentName(context.packageName, "${context.packageName}.ui.HomeActivity")
        val updates = originals.filter { it.activity != target || it.intents?.any { intent -> intent.component in aliases } == true }.map { shortcut ->
            ShortcutInfo.Builder(context, shortcut.id).setActivity(target).apply {
                shortcut.shortLabel?.let(::setShortLabel)
                shortcut.longLabel?.let(::setLongLabel)
                // Preserve stable real-activity targets and all source Intent fields.
                shortcut.intents?.map { original -> Intent(original).apply { if (component in aliases) component = real } }?.toTypedArray()?.let(::setIntents)
            }.build()
        }
        if (updates.isNotEmpty()) check(context.getSystemService(ShortcutManager::class.java)?.updateShortcuts(updates) != false) {
            "The launcher could not update shortcut ownership"
        }
    }
}

/** Package replacement only repairs local components and recoverable shortcuts. */
class LauncherIconUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        LauncherIcons.normalizeAsync(context) { pending.finish() }
    }
}
