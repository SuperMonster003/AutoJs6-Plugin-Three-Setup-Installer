package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import org.autojs.plugin.installer.api.InstallerContract as C
import org.autojs.plugin.installer.api.InstallerErrorCodes

/** Closed public option vocabulary and pre-session platform checks, independent of Android objects. */
internal object AdvancedInstallOptions {
    val keys = setOf(C.FIELD_GRANT_ALL_REQUESTED_PERMISSIONS, C.FIELD_REQUEST_UPDATE_OWNERSHIP,
        C.FIELD_DEXOPT, C.FIELD_INSTALL_REASON, C.FIELD_PACKAGE_SOURCE)

    fun dexopt(value: String?): String = choice(value ?: C.DEXOPT_NONE, C.DEXOPT_MODES, C.FIELD_DEXOPT)!!
    fun installReason(value: String?): String? = choice(value, C.INSTALL_REASONS, C.FIELD_INSTALL_REASON)
    fun packageSource(value: String?): String? = choice(value, C.PACKAGE_SOURCES, C.FIELD_PACKAGE_SOURCE)

    private fun choice(value: String?, allowed: Collection<String>, field: String): String? {
        if (value != null && value !in allowed) throw RequestDocuments.invalid("Unknown $field: $value")
        return value
    }

    fun validate(options: InstallOptions, authorizer: Authorizer, sdk: Int) {
        dexopt(options.dexopt); installReason(options.installReason); packageSource(options.packageSource)
        if ((options.grantAllRequestedPermissions || options.dexopt != C.DEXOPT_NONE) &&
            authorizer !in setOf(Authorizer.SHIZUKU, Authorizer.ROOT)) {
            throw InstallFailure(InstallerErrorCodes.AUTHORIZER_REQUIRED, "Permission grants and dex optimization require Shizuku or Root")
        }
        if (options.requestUpdateOwnership && sdk < 34) throw RequestDocuments.invalid("requestUpdateOwnership requires Android API 34 or later")
        if (options.dexopt == C.DEXOPT_VERIFY && sdk < 26) throw RequestDocuments.invalid("dexopt verify requires Android API 26 or later")
        if (options.installReason != null && sdk < 26) throw RequestDocuments.invalid("installReason requires Android API 26 or later")
        if (options.packageSource != null && sdk < 33) throw RequestDocuments.invalid("packageSource requires Android API 33 or later")
    }

    fun reasonValue(value: String): Int = when (installReason(value)) {
        C.INSTALL_REASON_UNKNOWN -> 0
        C.INSTALL_REASON_POLICY -> 1
        C.INSTALL_REASON_DEVICE_RESTORE -> 2
        C.INSTALL_REASON_DEVICE_SETUP -> 3
        C.INSTALL_REASON_USER -> 4
        else -> error("Missing install reason")
    }

    fun sourceValue(value: String): Int = when (packageSource(value)) {
        C.PACKAGE_SOURCE_UNSPECIFIED -> 0
        C.PACKAGE_SOURCE_OTHER -> 1
        C.PACKAGE_SOURCE_STORE -> 2
        C.PACKAGE_SOURCE_LOCAL_FILE -> 3
        C.PACKAGE_SOURCE_DOWNLOADED_FILE -> 4
        else -> error("Missing package source")
    }
}
