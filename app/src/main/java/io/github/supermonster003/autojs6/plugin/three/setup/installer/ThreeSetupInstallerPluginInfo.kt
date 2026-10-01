package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.content.Context
import android.os.Build
import android.os.Bundle
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.installer.api.InstallerCapabilityKeys
import org.autojs.plugin.installer.api.InstallerContract

/** Collects the installed package version and the localized metadata of this plugin. */
internal fun Context.threeSetupInstallerPluginRuntimeInfo(): ThreeSetupInstallerPluginRuntimeInfo {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toLong()
    }
    return ThreeSetupInstallerPluginRuntimeInfo(
        name = getString(R.string.app_name),
        description = getString(R.string.plugin_description),
        instruction = resources.openRawResource(R.raw.plugin_instruction)
            .bufferedReader()
            .use { it.readText() },
        versionName = packageInfo.versionName.orEmpty(),
        versionCode = versionCode,
        versionDate = getString(R.string.plugin_version_date),
    )
}

/** Maps the pure-data view onto the host contract parcelable. */
internal fun ThreeSetupInstallerPluginRuntimeInfo.toPluginInfo(): PluginInfo {
    val runtimeInfo = this
    return PluginInfo().apply {
        name = runtimeInfo.name
        description = runtimeInfo.description
        instruction = runtimeInfo.instruction
        author = runtimeInfo.author
        collaborators = null
        versionName = runtimeInfo.versionName
        versionCode = runtimeInfo.versionCode
        versionDate = runtimeInfo.versionDate
        id = runtimeInfo.id
        engine = runtimeInfo.engine
        variant = runtimeInfo.variant
        supportedAbis = runtimeInfo.supportedAbis
        capabilities = runtimeInfo.capabilitiesBundle()
    }
}

/**
 * Capability negotiation bundle: the minimum host build and the installer contract version the
 * plugin implements. The external URI entry owns best-effort deletion; a host PFD still leaves
 * deletion with its caller. Default selection now targets the formal external entry.
 */
internal fun ThreeSetupInstallerPluginRuntimeInfo.capabilitiesBundle(): Bundle = Bundle().apply {
    putLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION, requiresHostVersion)
    putInt(InstallerCapabilityKeys.CONTRACT_VERSION, InstallerContract.MIN_CONTRACT_VERSION)
    putInt(InstallerCapabilityKeys.MAX_CONTRACT_VERSION, InstallerContract.MAX_CONTRACT_VERSION)
    putStringArray(InstallerCapabilityKeys.AUTHORIZERS, InstallerContract.AUTHORIZERS.toTypedArray())
    putStringArray(InstallerCapabilityKeys.FEATURES_KEY, arrayOf(InstallerCapabilityKeys.FEATURE_BATCH,
        InstallerCapabilityKeys.FEATURE_SPLITS, InstallerCapabilityKeys.FEATURE_SILENT_UNINSTALL,
        InstallerCapabilityKeys.FEATURE_USERS, InstallerCapabilityKeys.FEATURE_INSPECT,
        InstallerCapabilityKeys.FEATURE_DELETE_SOURCE, InstallerCapabilityKeys.FEATURE_DEFAULT_INSTALLER,
        InstallerCapabilityKeys.FEATURE_PERSISTENT_DEFAULT_INSTALLER, InstallerCapabilityKeys.FEATURE_NOTIFICATION_INSTALL))
    putInt(InstallerCapabilityKeys.MAX_BATCH, InstallerContract.MAX_BATCH_SOURCES)
    putInt(InstallerCapabilityKeys.MAX_SPLITS, InstallerContract.MAX_SPLITS_PER_PACKAGE)
}
