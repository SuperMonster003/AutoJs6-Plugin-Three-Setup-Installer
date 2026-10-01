package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import android.content.Context
import android.os.Build
import android.os.Bundle
import org.autojs.plugin.installer.api.InstallerContract as C

/** Optional work after the platform has confirmed installation. It cannot undo that outcome. */
internal data class InstallFollowUp(
    val updateOwnerRead: Boolean = false,
    val updateOwner: String? = null,
    val dexopt: Optimization? = null,
    val notes: List<String> = emptyList(),
) {
    data class Optimization(val filter: String, val status: String, val message: String? = null)

    companion object {
        fun readOwner(context: Context, packageName: String, requested: Boolean): InstallFollowUp {
            if (!requested) return InstallFollowUp()
            if (Build.VERSION.SDK_INT < 34) return InstallFollowUp(notes = listOf("Update ownership could not be observed on this Android version"))
            return try {
                val owner = context.packageManager.getInstallSourceInfo(packageName).updateOwnerPackageName
                InstallFollowUp(updateOwnerRead = true, updateOwner = owner,
                    notes = if (owner == null) listOf("Android returned no visible update owner; ownership may be absent or hidden from this caller") else emptyList())
            } catch (_: Exception) {
                InstallFollowUp(notes = listOf("Installation succeeded, but its update owner could not be read"))
            }
        }

        fun decode(options: InstallOptions, bundle: Bundle): InstallFollowUp {
            val notes = bundle.getStringArrayList("notes").orEmpty().map { it.take(512) }.take(8).toMutableList()
            val observed = options.requestUpdateOwnership && bundle.getBoolean("updateOwnerRead", false)
            if (options.requestUpdateOwnership && !observed) notes += "Installation succeeded, but its update owner could not be read"
            val optimization = if (options.dexopt == C.DEXOPT_NONE) null else {
                val status = bundle.getString("dexoptStatus")?.takeIf { it in C.DEXOPT_STATUSES } ?: C.DEXOPT_STATUS_UNKNOWN
                val message = bundle.getString("dexoptMessage")?.take(512)
                Optimization(options.dexopt, status, message).also {
                    if (status != C.DEXOPT_STATUS_ACCEPTED) notes += "Installation succeeded; dex optimization outcome: $status"
                }
            }
            return InstallFollowUp(observed, if (observed) bundle.getString("updateOwnerPackageName")?.take(255) else null,
                optimization, notes)
        }

        fun failed(options: InstallOptions, failure: Throwable): InstallFollowUp {
            val status = when {
                failure is InterruptedException || Thread.currentThread().isInterrupted -> C.DEXOPT_STATUS_CANCELLED
                failure is InstallFailure && failure.code == org.autojs.plugin.installer.api.InstallerErrorCodes.CANCELLED -> C.DEXOPT_STATUS_CANCELLED
                failure is InstallFailure && failure.code == org.autojs.plugin.installer.api.InstallerErrorCodes.TIMEOUT -> C.DEXOPT_STATUS_TIMEOUT
                else -> C.DEXOPT_STATUS_UNKNOWN
            }
            return InstallFollowUp(dexopt = options.dexopt.takeUnless { it == C.DEXOPT_NONE }?.let { Optimization(it, status) },
                notes = listOf("Installation succeeded; optional ownership/optimization follow-up was not completed"))
        }
    }
}
