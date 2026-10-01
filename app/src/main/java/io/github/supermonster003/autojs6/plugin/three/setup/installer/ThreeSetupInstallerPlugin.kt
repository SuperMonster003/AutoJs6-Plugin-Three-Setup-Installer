package io.github.supermonster003.autojs6.plugin.three.setup.installer

import org.autojs.plugin.common.api.PluginActions
import org.autojs.plugin.installer.api.IInstallerPlugin
import org.autojs.plugin.installer.api.InstallerActions
import org.autojs.plugin.installer.api.InstallerIds

/**
 * Identity constants shared by the manifest, the Binder services, the documentation, and the
 * tests. They must stay identical to the host-side registration (see `ROADMAP.md`, decision D13
 * and phase P1.5); the JVM manifest contract test fails when the manifest drifts from them.
 *
 * Since roadmap P1.2 the action, category, descriptor and host version come from the host
 * `installer-api` module (`InstallerActions` / `InstallerIds`, staged as a locked AAR), exactly
 * as 3-Stove Agent references `ThreeStoveAgentActions` / `ThreeStoveAgentIds`.
 */
object ThreeSetupInstallerPlugin {

    const val PACKAGE_NAME = "io.github.supermonster003.autojs6.plugin.three.setup.installer"
    const val HOST_PACKAGE_NAME = "org.autojs.autojs6"
    const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"

    const val ID = InstallerIds.PLUGIN_ID
    const val ENGINE = InstallerIds.ENGINE
    const val VARIANT = InstallerIds.VARIANT_DEFAULT
    const val AUTHOR = "SuperMonster003"

    /** Discovery contract of [ThreeSetupInstallerPluginService]. */
    const val SERVICE_ACTION = InstallerActions.SERVICE_ACTION
    const val SERVICE_CATEGORY = InstallerActions.SERVICE_CATEGORY

    /** Discovery contract of [ThreeSetupInstallerPluginInfoService]. */
    const val INFO_ACTION = PluginActions.INFO

    /**
     * Binder descriptor of the `IInstallerPlugin` AIDL of the host `installer-api` module. The
     * service implements it through the guarded P2.6 router.
     */
    const val SERVICE_DESCRIPTOR = IInstallerPlugin.DESCRIPTOR

    /**
     * Minimum AutoJs6 `versionCode`: the 6.8.0 host build that ships `installer-api`, the shared
     * package archive parser and the host client (roadmap P1.5).
     */
    const val REQUIRED_HOST_VERSION = InstallerIds.REQUIRED_HOST_VERSION_CODE
}
