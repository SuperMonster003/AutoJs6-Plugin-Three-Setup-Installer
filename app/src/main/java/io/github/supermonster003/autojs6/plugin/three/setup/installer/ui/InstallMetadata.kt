package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.PrivilegedClient
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.DeviceUsers
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallSession
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import org.autojs.plugin.installer.api.InstallerContract

/** Copies only display facts; the UI never retains a staged file or a live descriptor. */
internal object InstallMetadata {
    const val NOT_INSTALLED = "notInstalled"
    const val OTHER_USER = "otherUser"

    @Suppress("DEPRECATION")
    fun read(context: Context, prepared: PreparedPackage, userId: Int, authorizer: Authorizer? = null): InstallPresentation.Metadata {
        val packages = context.packageManager
        val flags = PackageManager.GET_SIGNATURES or if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else 0
        val archive = prepared.displayApk?.let { file -> runCatching { packages.getPackageArchiveInfo(file.absolutePath, flags) }.getOrNull() }
        val currentUser = userId == DeviceUsers(context).currentId
        var installedKnown = currentUser && prepared.packageName != null
        val installed = if (currentUser) prepared.packageName?.let {
            try { packages.getPackageInfo(it, flags) }
            catch (_: PackageManager.NameNotFoundException) { null }
            catch (_: Exception) { installedKnown = false; null }
        } else null
        val previous = if (currentUser) installed?.let {
            InstallSession.Version(it.versionName, if (Build.VERSION.SDK_INT >= 28) it.longVersionCode else it.versionCode.toLong())
        } else if (authorizer?.privileged == true && prepared.packageName != null) runCatching {
            val info = PrivilegedClient.get(context).acquire(authorizer, 3_000).getInstalledVersion(prepared.packageName, userId)
            installedKnown = true
            if (info.containsKey("versionCode")) InstallSession.Version(info.getString("versionName"), info.getLong("versionCode")) else null
        }.getOrNull() else null
        var label = prepared.label ?: prepared.displayName
        val icon = archive?.applicationInfo?.let { app ->
            prepared.displayApk?.let { file -> app.sourceDir = file.absolutePath; app.publicSourceDir = file.absolutePath }
            runCatching { packages.getApplicationLabel(app).toString() }.getOrNull()?.let { label = it }
            runCatching {
                Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888).also { bitmap ->
                    app.loadIcon(packages).apply { setBounds(0, 0, 96, 96); draw(Canvas(bitmap)) }
                }
            }.getOrNull()
        }
        val signature = when {
            !currentUser -> OTHER_USER
            !installedKnown -> InstallerContract.SIGNER_UNKNOWN
            installed == null -> NOT_INSTALLED
            signers(archive).isEmpty() || signers(installed).isEmpty() -> InstallerContract.SIGNER_UNKNOWN
            signers(archive) == signers(installed) -> InstallerContract.SIGNER_MATCH
            else -> InstallerContract.SIGNER_MISMATCH
        }
        return InstallPresentation.Metadata(label, prepared.packageName, prepared.versionName, prepared.versionCode,
            previous, userId, installedKnown, prepared.sourceSize, prepared.minSdk, prepared.targetSdk, signature, icon, prepared.format,
            prepared.apks.map { InstallPresentation.Split(it.name, it.size, true, it.splitName == null) } +
                prepared.splits.filterNot { it.selected }.map { InstallPresentation.Split(it.name, it.size, false, false) },
            prepared.aabModules.toList())
    }

    @Suppress("DEPRECATION")
    private fun signers(info: PackageInfo?): Set<String> {
        val modern = if (Build.VERSION.SDK_INT >= 28) info?.signingInfo?.apkContentsSigners?.takeIf { it.isNotEmpty() } else null
        return (modern ?: info?.signatures).orEmpty().mapTo(hashSetOf()) { it.toCharsString() }
    }
}
