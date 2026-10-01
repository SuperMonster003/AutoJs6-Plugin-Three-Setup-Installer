package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import io.github.supermonster003.autojs6.plugin.three.setup.installer.R
import org.autojs.plugin.installer.api.InstallerContract as C

/** UI choices retain explicit unsupported requests; the engine reports their platform limits. */
internal object AdvancedInstallUi {
    val dexopt = listOf(C.DEXOPT_NONE to R.string.advanced_dexopt_none, C.DEXOPT_VERIFY to R.string.advanced_dexopt_verify,
        C.DEXOPT_SPEED_PROFILE to R.string.advanced_dexopt_speed_profile, C.DEXOPT_SPEED to R.string.advanced_dexopt_speed)
    val reasons = listOf(null to R.string.advanced_default, C.INSTALL_REASON_UNKNOWN to R.string.advanced_reason_unknown,
        C.INSTALL_REASON_DEVICE_RESTORE to R.string.advanced_reason_restore, C.INSTALL_REASON_DEVICE_SETUP to R.string.advanced_reason_setup,
        C.INSTALL_REASON_USER to R.string.advanced_reason_user, C.INSTALL_REASON_POLICY to R.string.advanced_reason_policy)
    val sources = listOf(null to R.string.advanced_default, C.PACKAGE_SOURCE_UNSPECIFIED to R.string.advanced_source_unspecified,
        C.PACKAGE_SOURCE_STORE to R.string.advanced_source_store, C.PACKAGE_SOURCE_LOCAL_FILE to R.string.advanced_source_local,
        C.PACKAGE_SOURCE_DOWNLOADED_FILE to R.string.advanced_source_downloaded, C.PACKAGE_SOURCE_OTHER to R.string.advanced_source_other)
}
