package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Base64
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.ArchiveOpener
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PackageStaging
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PackageSource
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.SeekableSource
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.packagearchive.ApkSignatureDetector
import org.autojs.plugin.packagearchive.PackageDeviceSpec
import java.io.ByteArrayOutputStream

/** Reads and enriches an archive without installation; its staging ends before the result callback. */
internal class PackageInspector(context: Context) {
    private val context = context.applicationContext
    private val packages = context.packageManager

    @Suppress("DEPRECATION")
    fun inspect(source: ParcelFileDescriptor, request: InspectRequest, checkActive: () -> Unit): JsonObject =
        inspect(PackageSource.Descriptor(source, request.displayName, request.size), checkActive)

    @Suppress("DEPRECATION")
    fun inspect(source: PackageSource, checkActive: () -> Unit = {}): JsonObject {
        val directory = PackageStaging.newDirectory(context)
        var opened: SeekableSource? = null
        try {
            opened = PackageStaging.open(context, directory, source, checkActive = checkActive)
            checkActive()
            val prepared = ArchiveOpener.open(opened.file, opened.displayName, PackageDeviceSpec.from(context), directory, false, checkActive)
            checkActive()
            val flags = PackageManager.GET_SIGNATURES or if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else 0
            val archive = prepared.displayApk?.let { file -> runCatching { packages.getPackageArchiveInfo(file.absolutePath, flags) }.getOrNull() }
            val installed = prepared.packageName?.let { name ->
                try { packages.getPackageInfo(name, flags) } catch (_: PackageManager.NameNotFoundException) { null }
            }
            val installedJson = installed?.let { info -> JsonObject().apply {
                info.versionName?.let { addProperty(InstallerContract.FIELD_VERSION_NAME, it) }
                addProperty(InstallerContract.FIELD_VERSION_CODE, if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong())
                val expected = signers(archive)
                val actual = signers(info)
                addProperty(InstallerContract.FIELD_SIGNER_MATCH, when {
                    expected.isEmpty() || actual.isEmpty() -> InstallerContract.SIGNER_UNKNOWN
                    expected == actual -> InstallerContract.SIGNER_MATCH
                    else -> InstallerContract.SIGNER_MISMATCH
                })
                runCatching { if (Build.VERSION.SDK_INT >= 30) packages.getInstallSourceInfo(info.packageName).installingPackageName
                    else packages.getInstallerPackageName(info.packageName) }.getOrNull()?.let { addProperty(InstallerContract.FIELD_INSTALLER, it) }
            } }
            val document = ArchiveOpener.toInspectJson(prepared, installedJson, null)
            prepared.displayApk?.let { file ->
                runCatching { ApkSignatureDetector.detectSchemes(file) }.getOrNull()?.let { schemes ->
                    document.add(InstallerContract.FIELD_SIGNATURE_SCHEMES, JsonArray().apply {
                        schemes.lowercase(java.util.Locale.ROOT).split(',', ' ', '/', '+').filter { it in setOf("v1", "v2", "v3", "v4") }.forEach { add(it) }
                    })
                }
                archive?.applicationInfo?.let { app ->
                    app.sourceDir = file.absolutePath
                    app.publicSourceDir = file.absolutePath
                    runCatching { packages.getApplicationLabel(app).toString() }.getOrNull()?.let { document.addProperty(InstallerContract.FIELD_LABEL, it) }
                    val icon = runCatching {
                        val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
                        try {
                            app.loadIcon(packages).apply { setBounds(0, 0, 96, 96); draw(Canvas(bitmap)) }
                            ByteArrayOutputStream().use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream); stream.toByteArray() }
                        } finally { bitmap.recycle() }
                    }.getOrNull()
                    if (icon != null && icon.size <= InstallerContract.MAX_ICON_BYTES) {
                        document.addProperty(InstallerContract.FIELD_ICON, Base64.encodeToString(icon, Base64.NO_WRAP))
                        if (document.toString().toByteArray(Charsets.UTF_8).size > InstallerContract.MAX_JSON_BYTES) document.remove(InstallerContract.FIELD_ICON)
                    }
                }
            }
            checkActive()
            return document
        } finally {
            try { opened?.close() } finally { PackageStaging.discard(directory) }
        }
    }

    @Suppress("DEPRECATION")
    private fun signers(info: PackageInfo?): Set<String> {
        val modern = if (Build.VERSION.SDK_INT >= 28) info?.signingInfo?.apkContentsSigners?.takeIf { it.isNotEmpty() } else null
        return (modern ?: info?.signatures).orEmpty().mapTo(hashSetOf()) { it.toCharsString() }
    }
}
