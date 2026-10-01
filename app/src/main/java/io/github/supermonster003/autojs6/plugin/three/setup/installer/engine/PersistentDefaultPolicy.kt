package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.RootPersistentDefaults
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.DhizukuFramework
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.PackageManagerHidden

/** Receipt of the policy last set by this plugin; resolution is independently checked on every read. */
internal class PersistentDefaultPolicy(context: Context) {
    private val context = context.applicationContext
    private val preferences = context.getSharedPreferences("installer_persistent_default", Context.MODE_PRIVATE)
    fun isConfiguredForCurrentOwner(): Boolean {
        val owner = preferences.getString("owner", null) ?: return false
        if (owner == "root-system") return true
        val policy = context.getSystemService(DevicePolicyManager::class.java)
        return runCatching { policy.isDeviceOwnerApp(owner) || policy.isProfileOwnerApp(owner) }.getOrDefault(false)
    }
    fun setRoot(enable: Boolean, checkActive: () -> Unit): Int {
        checkActive()
        check(preferences.edit().putString("pendingOwner", "root-system").putBoolean("pending", true).commit()) {
            "Could not save the pending persistent-default operation"
        }
        val changed = RootPersistentDefaults.set(context, enable, checkActive)
        val edit = preferences.edit().clear()
        if (enable) edit.putString("owner", "root-system")
        check(edit.commit()) { "Could not save persistent-default state" }
        return changed
    }
    fun set(enable: Boolean, component: ComponentName, checkActive: () -> Unit): Int {
        require(component.packageName == context.packageName)
        if (Build.VERSION.SDK_INT >= 34) throw RequestDocuments.invalid(
            "Dhizuku persistent policy on API 34+ requires an owner PolicyUpdateReceiver result; this transport cannot verify it")
        val framework = DhizukuFramework(context)
        val manager = framework.checked { framework.policyManager() }
        checkActive()
        if (!enable) {
            check(preferences.edit().putString("pendingOwner", framework.owner.packageName).putBoolean("pending", true).commit()) {
                "Could not save the pending persistent-default operation"
            }
            framework.checked {
                checkActive()
                manager.clearPackagePersistentPreferredActivities(framework.owner, component.packageName)
            }
            check(preferences.edit().clear().commit()) { "Could not save persistent-default state" }
            return 0
        }
        val intents = PackageManagerHidden.INSTALL_ACTIONS.flatMap { action -> listOf("content", "file").map { action to it } }
        check(intents.all { (action, scheme) -> context.packageManager.queryIntentActivities(PackageManagerHidden.intent(action, scheme), PackageManager.MATCH_DEFAULT_ONLY)
            .any { ComponentName(it.activityInfo.packageName, it.activityInfo.name) == component } }) { "APK entry is not available" }
        // Persist uncertainty before the first write. DevicePolicyManager cannot enumerate or
        // remove one persistent filter, so failure must never clear an unknown older policy.
        check(preferences.edit().putString("pendingOwner", framework.owner.packageName).putBoolean("pending", true).commit()) {
            "Could not save the pending persistent-default operation"
        }
        try {
            // Cancellation is checked before starting the small policy group, not between its
            // writes. This is not a transaction; any Binder failure remains an uncertain result.
            framework.checked {
                checkActive()
                intents.forEach { (action, scheme) -> manager.addPersistentPreferredActivity(framework.owner, PackageManagerHidden.filter(action, scheme), component) }
            }
            check(intents.all { (action, scheme) -> context.packageManager.resolveActivity(PackageManagerHidden.intent(action, scheme), PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.let { ComponentName(it.packageName, it.name) } == component }) { "The policy did not select the plugin for every APK filter" }
            check(preferences.edit().putString("owner", framework.owner.packageName).remove("pendingOwner").putBoolean("pending", false).commit()) {
                "Could not save persistent-default state"
            }
        } catch (failure: Exception) {
            throw InstallFailure(org.autojs.plugin.installer.api.InstallerErrorCodes.INTERNAL,
                "Persistent policy update was not fully confirmed; a partial update may remain. No automatic clearing was attempted. Inspect the default state or explicitly clear this plugin's persistent policy", cause = failure)
        }
        return 4
    }
}
