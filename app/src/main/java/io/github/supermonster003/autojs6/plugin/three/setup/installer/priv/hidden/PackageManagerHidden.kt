package io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.IBinder
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.HiddenApiAccess.booleanType
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.HiddenApiAccess.intType

/** AOSP signature adapters; reflection is deliberately confined to priv/hidden. */
internal class PackageManagerHidden(wrap: (IBinder) -> IBinder = { it }) {
    private val remote = HiddenApiAccess.service("package", DESCRIPTOR, wrap)
    private val preferredTypes = arrayOf(IntentFilter::class.java, intType, Array<ComponentName>::class.java, ComponentName::class.java, intType)
    private val addWithReplacement = try {
        HiddenApiAccess.method(DESCRIPTOR, "addPreferredActivity", *preferredTypes, booleanType)
    } catch (_: NoSuchMethodException) {
        null
    }
    val canReplacePreferred: Boolean get() = addWithReplacement != null

    fun installer(): PackageInstallerHidden = PackageInstallerHidden(call("getPackageInstaller", emptyArray())!!)

    fun packageUid(name: String, user: Int): Int = call("getPackageUid",
        arrayOf(String::class.java, HiddenApiAccess.flagsType, intType), name, HiddenApiAccess.flags(0), user) as Int

    fun query(intent: Intent, user: Int): List<ResolveInfo> {
        val slice = call("queryIntentActivities", arrayOf(Intent::class.java, String::class.java, HiddenApiAccess.flagsType, intType),
            intent, intent.type, HiddenApiAccess.flags(PackageManager.MATCH_DEFAULT_ONLY), user)!!
        @Suppress("UNCHECKED_CAST")
        return HiddenApiAccess.invoke(slice, HiddenApiAccess.method("android.content.pm.ParceledListSlice", "getList")) as List<ResolveInfo>
    }

    fun resolve(intent: Intent, user: Int): ComponentName? {
        val info = call("resolveIntent", arrayOf(Intent::class.java, String::class.java, HiddenApiAccess.flagsType, intType),
            intent, intent.type, HiddenApiAccess.flags(PackageManager.MATCH_DEFAULT_ONLY), user) as ResolveInfo?
        return info?.activityInfo?.let { ComponentName(it.packageName, it.name) }
    }

    fun clearPreferred(packageName: String) {
        call("clearPackagePreferredActivities", arrayOf(String::class.java), packageName)
    }

    fun addPreferred(filter: IntentFilter, matches: List<ResolveInfo>, component: ComponentName, user: Int) {
        val args = arrayOf(filter, matches.maxOf { it.match }, matches.map {
            ComponentName(it.activityInfo.packageName, it.activityInfo.name)
        }.toTypedArray(), component, user)
        // OEMs may backport the removeExisting overload. Resolve only these two known signatures.
        if (addWithReplacement != null) HiddenApiAccess.invoke(remote, addWithReplacement, *args, true)
        else call("addPreferredActivity", preferredTypes, *args)
    }

    fun persistentPreferred(filter: IntentFilter, component: ComponentName, user: Int) {
        call("addPersistentPreferredActivity", arrayOf(IntentFilter::class.java, ComponentName::class.java, intType), filter, component, user)
    }

    fun clearPersistentPreferred(packageName: String, user: Int) {
        call("clearPackagePersistentPreferredActivities", arrayOf(String::class.java, intType), packageName, user)
    }

    fun preferredActivities(packageName: String): List<IntentFilter> {
        val filters = arrayListOf<IntentFilter>()
        call("getPreferredActivities", arrayOf(List::class.java, List::class.java, String::class.java), filters, arrayListOf<ComponentName>(), packageName)
        return filters
    }

    private fun call(name: String, types: Array<Class<*>>, vararg args: Any?): Any? =
        HiddenApiAccess.invoke(remote, HiddenApiAccess.method(DESCRIPTOR, name, *types), *args)

    companion object {
        private const val DESCRIPTOR = "android.content.pm.IPackageManager"
        const val APK_MIME = "application/vnd.android.package-archive"
        val INSTALL_ACTIONS = listOf(Intent.ACTION_VIEW, "android.intent.action.INSTALL_PACKAGE")
        fun intent(action: String, scheme: String) = Intent(action).addCategory(Intent.CATEGORY_DEFAULT)
            .setDataAndType(Uri.parse("$scheme://three-setup-installer/probe.apk"), APK_MIME)
        fun filter(action: String, scheme: String) = IntentFilter(action).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addDataScheme(scheme)
            addDataType(APK_MIME)
        }
    }
}
