package io.github.supermonster003.autojs6.plugin.three.setup.installer

/**
 * Pure-data view of the metadata reported through `IPluginInfoProvider.getInfo()` (and, from
 * roadmap P1.2 on, `IInstallerPlugin.getInfo()` / `getCapabilities()`).
 *
 * Android-specific lookups (package version, localized strings, raw resources) happen in
 * [threeSetupInstallerPluginRuntimeInfo]; this class keeps the mapping itself testable on the JVM.
 */
data class ThreeSetupInstallerPluginRuntimeInfo(
    val name: String,
    val description: String,
    val instruction: String?,
    val versionName: String,
    val versionCode: Long,
    val versionDate: String,
) {
    val author: String get() = ThreeSetupInstallerPlugin.AUTHOR
    val id: String get() = ThreeSetupInstallerPlugin.ID
    val engine: String get() = ThreeSetupInstallerPlugin.ENGINE
    val variant: String get() = ThreeSetupInstallerPlugin.VARIANT

    /** Empty on purpose: the plugin ships no native code and runs on any ABI (roadmap D13). */
    val supportedAbis: Array<String> get() = emptyArray()

    val requiresHostVersion: Long get() = ThreeSetupInstallerPlugin.REQUIRED_HOST_VERSION
}
