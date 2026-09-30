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
 * plugin implements. The authorizer and feature arrays are declared together with the real
 * Binder routing of roadmap P2.6, so the host never sees a capability the plugin cannot serve.
 */
internal fun ThreeSetupInstallerPluginRuntimeInfo.capabilitiesBundle(): Bundle = Bundle().apply {
    putLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION, requiresHostVersion)
    putInt(InstallerCapabilityKeys.CONTRACT_VERSION, InstallerContract.CONTRACT_VERSION)
}
