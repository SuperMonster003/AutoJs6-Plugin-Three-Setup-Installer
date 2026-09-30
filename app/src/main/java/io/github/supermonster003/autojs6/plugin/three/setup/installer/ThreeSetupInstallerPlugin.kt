package io.github.supermonster003.autojs6.plugin.three.setup.installer

import org.autojs.plugin.common.api.PluginActions

/**
 * Identity constants shared by the manifest, the Binder services, the documentation, and the
 * tests. They must stay identical to the host-side registration (see `ROADMAP.md`, decision D13
 * and phase P1.5); the JVM manifest contract test fails when the manifest drifts from them.
 *
 * Roadmap P1.2 replaces the literal action, category, descriptor and host version with the
 * constants of the host `installer-api` module (`InstallerActions` / `InstallerIds`), exactly
 * as 3-Stove Agent references `ThreeStoveAgentActions` / `ThreeStoveAgentIds`.
 */
object ThreeSetupInstallerPlugin {

    const val PACKAGE_NAME = "io.github.supermonster003.autojs6.plugin.three.setup.installer"
    const val HOST_PACKAGE_NAME = "org.autojs.autojs6"
    const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"

    const val ID = "three-setup-installer"
    const val ENGINE = "installer"
    const val VARIANT = "default"
    const val AUTHOR = "SuperMonster003"

    /** Discovery contract of [ThreeSetupInstallerPluginService]. */
    const val SERVICE_ACTION = "org.autojs.plugin.INSTALLER"
    const val SERVICE_CATEGORY = "installer"

    /** Discovery contract of [ThreeSetupInstallerPluginInfoService]. */
    const val INFO_ACTION = PluginActions.INFO

    /**
     * Binder descriptor of the `IInstallerPlugin` AIDL that the host `installer-api` module will
     * define (roadmap P1.2). The placeholder Binder of P0 already carries it so that the contract
     * test asserts the same descriptor before and after the real stub lands.
     */
    const val SERVICE_DESCRIPTOR = "org.autojs.plugin.installer.api.IInstallerPlugin"

    /**
     * Minimum AutoJs6 `versionCode`. 5298 (AutoJs6 6.8.0) is the build the skeleton was written
     * against; roadmap P1.5 replaces it with the build that ships `installer-api`.
     */
    const val REQUIRED_HOST_VERSION = 5298L
}
