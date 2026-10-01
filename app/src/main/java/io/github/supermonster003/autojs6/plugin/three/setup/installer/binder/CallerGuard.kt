package io.github.supermonster003.autojs6.plugin.three.setup.installer.binder

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ThreeSetupInstallerPlugin
import java.security.MessageDigest

internal interface CallerGuard {
    fun enforceHost(): Int
    fun enforceOwner(uid: Int) {
        if (enforceHost() != uid) throw SecurityException("The caller does not own this installation session")
    }
}

/** The public protocol requires the installed official host, matching signers and a supported build. */
internal object CallerPolicy {
    fun allowed(uid: Int, packages: Set<String>, hostUid: Int?, hostVersion: Long, hostSigners: Set<String>, pluginSigners: Set<String>): Boolean =
        uid == hostUid && ThreeSetupInstallerPlugin.HOST_PACKAGE_NAME in packages &&
            hostVersion >= ThreeSetupInstallerPlugin.REQUIRED_HOST_VERSION &&
            pluginSigners.isNotEmpty() && hostSigners == pluginSigners
}

internal class HostCallerGuard(context: Context) : CallerGuard {
    private val packages = context.applicationContext.packageManager
    private val plugin = context.applicationContext.packageName

    override fun enforceHost(): Int {
        val uid = Binder.getCallingUid()
        val host = packageInfo(ThreeSetupInstallerPlugin.HOST_PACKAGE_NAME)
        @Suppress("DEPRECATION")
        val version = host?.let { if (Build.VERSION.SDK_INT >= 28) it.longVersionCode else it.versionCode.toLong() } ?: 0
        if (!CallerPolicy.allowed(uid, packages.getPackagesForUid(uid).orEmpty().toSet(), host?.applicationInfo?.uid,
                version, signers(host), signers(packageInfo(plugin)))) {
            throw SecurityException("Caller is not the installed same-signer AutoJs6 host of the required version")
        }
        return uid
    }

    @Suppress("DEPRECATION")
    private fun packageInfo(name: String): PackageInfo? = try {
        packages.getPackageInfo(name, PackageManager.GET_SIGNATURES or if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else 0)
    } catch (_: PackageManager.NameNotFoundException) { null }

    @Suppress("DEPRECATION")
    private fun signers(info: PackageInfo?): Set<String> {
        val modern = if (Build.VERSION.SDK_INT >= 28) info?.signingInfo?.apkContentsSigners?.takeIf { it.isNotEmpty() } else null
        val signatures = modern ?: info?.signatures
        return signatures.orEmpty().mapTo(hashSetOf()) { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 0xff) }
        }
    }
}
