package io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.os.Bundle
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.PrivilegedOptions

/** Framework certificates, never a display-layer digest or an inferred signing lineage. */
internal object InstalledSigningInfo {
    // AOSP API 26+: MATCH_UNINSTALLED alone can still require installation for the target user.
    // MATCH_ANY_USER is hidden and requires cross-user access; only the privileged caller uses it.
    private const val MATCH_ANY_USER = 0x00400000

    @Suppress("DEPRECATION")
    fun flags(includeOtherUsers: Boolean = false): Int = PackageManager.GET_SIGNATURES or
        PackageManager.MATCH_UNINSTALLED_PACKAGES or
        (if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else 0) or
        (if (includeOtherUsers && Build.VERSION.SDK_INT >= 26) MATCH_ANY_USER else 0)

    @Suppress("DEPRECATION")
    fun encode(packageName: String, userId: Int, info: PackageInfo?): Bundle {
        PrivilegedOptions.validatePackage(packageName)
        require(userId >= 0)
        check(info == null || info.packageName == packageName) { "Package manager returned another package" }
        val current: Array<out Signature>
        val history: Array<out Signature>
        val multiple: Boolean
        if (info != null && Build.VERSION.SDK_INT >= 28) {
            val signing = info.signingInfo
            current = signing?.apkContentsSigners.orEmpty()
            multiple = signing?.hasMultipleSigners() ?: false
            history = if (multiple) emptyArray() else signing?.signingCertificateHistory.orEmpty()
        } else {
            current = info?.signatures.orEmpty()
            multiple = current.size > 1
            history = emptyArray()
        }
        // Bound the private Binder payload. Missing certificates remain unknown to the gate.
        require(current.size <= 64 && history.size <= 64)
        require((current.asList() + history.asList()).sumOf { it.toByteArray().size.toLong() } <= 512 * 1024)
        return Bundle().apply {
            putString("packageName", packageName); putInt("userId", userId)
            putBoolean("found", info != null)
            putBoolean("installedForUser", ((info?.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_INSTALLED) != 0)
            putBoolean("multipleSigners", multiple)
            putParcelableArray("currentSigners", current)
            putParcelableArray("signingHistory", history)
            putString("sharedUserId", info?.sharedUserId)
            if (info != null) {
                putLong("versionCode", if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong())
                putString("versionName", info.versionName); putLong("lastUpdateTime", info.lastUpdateTime)
            }
        }
    }
}
