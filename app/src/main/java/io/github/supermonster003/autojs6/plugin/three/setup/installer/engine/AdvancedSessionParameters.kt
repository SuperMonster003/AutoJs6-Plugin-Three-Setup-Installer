package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import android.content.pm.PackageInstaller
import android.os.Build

/** Public metadata setters shared by ordinary and owner transports; privileged flags stay private. */
internal object AdvancedSessionParameters {
    fun apply(params: PackageInstaller.SessionParams, options: InstallOptions) {
        if (Build.VERSION.SDK_INT >= 26) options.installReason?.let { params.setInstallReason(AdvancedInstallOptions.reasonValue(it)) }
        if (Build.VERSION.SDK_INT >= 33) params.setPackageSource(options.packageSource?.let(AdvancedInstallOptions::sourceValue)
            ?: PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE)
        if (Build.VERSION.SDK_INT >= 34 && options.requestUpdateOwnership) params.setRequestUpdateOwnership(true)
    }
}
