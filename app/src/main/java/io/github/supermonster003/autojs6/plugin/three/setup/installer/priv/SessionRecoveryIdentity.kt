package io.github.supermonster003.autojs6.plugin.three.setup.installer.priv

import android.content.pm.PackageInstaller
import android.os.Bundle
import androidx.annotation.RequiresApi

/** Complete framework identity. Never substitute the calling UID for the actual session owner. */
internal data class SessionRecoveryIdentity(
    val sessionId: Int,
    val ownerUid: Int,
    val installerUid: Int,
    val userId: Int,
    val createdMillis: Long,
    val installer: String,
    val packageName: String,
    val size: Long,
) {
    fun toBundle() = Bundle().apply {
        putInt("sessionId", sessionId)
        putInt("ownerUid", ownerUid)
        putInt("installerUid", installerUid)
        putInt("userId", userId)
        putLong("createdMillis", createdMillis)
        putString("installer", installer)
        putString("packageName", packageName)
        putLong("size", size)
    }

    companion object {
        private val KEYS = setOf("sessionId", "ownerUid", "installerUid", "userId", "createdMillis", "installer", "packageName", "size")

        @RequiresApi(30)
        fun read(info: PackageInstaller.SessionInfo, serviceUid: Int): SessionRecoveryIdentity? {
            // AOSP only exposes this field from API 33. Old/OEM variants without it cannot
            // safely prove the real owner, even when the service itself happens to run as Root.
            val installerUid = runCatching {
                PackageInstaller.SessionInfo::class.java.getField("installerUid").getInt(info)
            }.getOrNull() ?: return null
            if (installerUid != serviceUid) return null
            val installer = info.installerPackageName?.takeIf { it.isNotBlank() } ?: return null
            val packageName = info.appPackageName?.takeIf { it.isNotBlank() } ?: return null
            if (info.originatingUid < 0 || info.createdMillis <= 0 || info.size <= 0) return null
            // SessionInfo.getUser() is public, but UserHandle.getIdentifier() is hidden.
            // The framework's public hidden field has the same int shape on API 30+.
            val user = PackageInstaller.SessionInfo::class.java.getField("userId").getInt(info)
            return SessionRecoveryIdentity(info.sessionId, info.originatingUid, installerUid, user,
                info.createdMillis, installer, packageName, info.size)
        }

        @Suppress("DEPRECATION")
        fun decode(bundle: Bundle): SessionRecoveryIdentity {
            require(bundle.keySet() == KEYS) { "Invalid session recovery fields" }
            fun int(key: String) = bundle.get(key) as? Int ?: throw IllegalArgumentException("Invalid recovery integer")
            fun long(key: String) = bundle.get(key) as? Long ?: throw IllegalArgumentException("Invalid recovery length or timestamp")
            fun text(key: String) = bundle.get(key) as? String ?: throw IllegalArgumentException("Invalid recovery package")
            return SessionRecoveryIdentity(int("sessionId"), int("ownerUid"), int("installerUid"), int("userId"),
                long("createdMillis"), text("installer"), text("packageName"), long("size"))
        }
    }
}
