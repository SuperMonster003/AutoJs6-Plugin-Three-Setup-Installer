package io.github.supermonster003.autojs6.plugin.three.setup.installer.source

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.autojs.plugin.packagearchive.AndroidPackageArchive
import org.autojs.plugin.packagearchive.AndroidPackageArchiveInspector
import org.autojs.plugin.packagearchive.AndroidPackageFormat
import org.autojs.plugin.packagearchive.AndroidPackageSubtype
import org.autojs.plugin.packagearchive.ApkSignatureDetector
import org.autojs.plugin.packagearchive.ArchiveProblem
import org.autojs.plugin.packagearchive.ArchiveProblemCode
import org.autojs.plugin.packagearchive.PackageDeviceSpec
import org.autojs.plugin.packagearchive.ManifestSummary
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale

/** One APK file the engines write into a session, under the generated split name the platform sees. */
internal data class PlannedApk(
    val name: String,
    val file: File,
    val size: Long,
    val splitName: String?,
    val sha256: String? = null,
    val manifest: ManifestSummary? = null,
)

/** One split of an inspected container, for the `splits` array of the PackageArchiveInfo document. */
internal data class SplitInfo(val name: String, val size: Long, val selected: Boolean, val reason: String?)

/**
 * What one request item became after inspection: the contract format, the manifest facts of the
 * base APK, the staged APK files with their generated names, and the problems the shared parser
 * reported. [installable] is false for AAB, unknown containers and every blocking problem.
 */
internal class PreparedPackage(
    val format: String,
    val displayName: String,
    val sourceSize: Long,
    val packageName: String?,
    val versionName: String?,
    val versionCode: Long?,
    val label: String?,
    val minSdk: Int?,
    val targetSdk: Int?,
    val apks: List<PlannedApk>,
    val splits: List<SplitInfo>,
    val signatureSchemes: List<String>?,
    val problems: List<ArchiveProblem>,
    val aabModules: List<String>,
    val installable: Boolean,
    val displayApk: File? = apks.firstOrNull { it.splitName == null }?.file,
) {
    val totalBytes: Long get() = apks.sumOf { it.size }

    val baseApk: PlannedApk? get() = apks.firstOrNull { it.splitName == null }

    /** The failure that explains why [installable] is false, or null when the package can be installed. */
    fun failure(): InstallFailure? = when {
        installable -> null
        format == InstallerContract.FORMAT_AAB -> InstallFailure(
            InstallerErrorCodes.UNSUPPORTED_FORMAT,
            "Android App Bundles cannot be installed directly; generate an .apks set with bundletool first",
            packageName = packageName,
        )
        format == InstallerContract.FORMAT_UNKNOWN -> InstallFailure(InstallerErrorCodes.UNSUPPORTED_FORMAT, "Unrecognized package container: $displayName")
        problems.any { it.blocking && it.code == ArchiveProblemCode.INCOMPATIBLE_DEVICE } -> InstallFailure(
            InstallerErrorCodes.INCOMPATIBLE_DEVICE,
            problems.first { it.code == ArchiveProblemCode.INCOMPATIBLE_DEVICE }.detail,
            packageName = packageName,
        )
        else -> InstallFailure(
            InstallerErrorCodes.INVALID_PACKAGE,
            problems.firstOrNull { it.blocking }?.detail ?: "The package contains no installable APK",
            packageName = packageName,
        )
    }
}

/**
 * Turns staged sources into [PreparedPackage]s with the shared parser (roadmap P2.1, D10): a single
 * APK is used in place, a container (`apks`, `xapk`, `apkm`, `apkz`) has its device-selected APKs
 * extracted next to it, an AAB is described but never staged (D9). Several APK descriptors of one
 * request item are combined into one split set by [combine].
 *
 * Pure JVM apart from [PackageDeviceSpec]; the JVM tests build their fixtures with text manifests.
 *
 * zh-CN: 用共享解析器把暂存的来源变成可安装的分包集合; 单个 APK 原地使用, 容器抽取设备匹配的分包,
 * AAB 只描述不安装; 同一请求项的多个 APK 描述符合并为一个分包集合.
 */
internal object ArchiveOpener {

    /** Extensions the parser accepts as containers; anything else is inspected by content only. */
    private val KNOWN_EXTENSIONS = setOf("apk", "apks", "xapk", "apkm", "apkz", "zip", "aab")

    /** Problems of a single APK that the surrounding split set resolves (see [combine]). */
    private val SET_RESOLVED_PROBLEMS = setOf(ArchiveProblemCode.NO_BASE_APK, ArchiveProblemCode.MISSING_SPLIT_DEPENDENCY)

    fun open(
        file: File,
        displayName: String,
        device: PackageDeviceSpec,
        stagingDirectory: File,
        prepareForInstallation: Boolean,
        checkActive: () -> Unit = {},
    ): PreparedPackage {
        checkActive()
        // A held descriptor pins its inode, not its bytes. Compare before/after inspection so a
        // same-size edit between manifest decoding and the parser's digest cannot be approved.
        val originalDigest = if (prepareForInstallation) sourceDigest(file, checkActive) else null
        val inspected = try {
            AndroidPackageArchiveInspector.inspect(file, device, prepareForInstallation)
        } catch (failure: IOException) {
            if (failure.message?.startsWith("Unsupported Android package container") == true) {
                return PreparedPackage(
                    format = InstallerContract.FORMAT_UNKNOWN, displayName = displayName, sourceSize = file.length(),
                    packageName = null, versionName = null, versionCode = null, label = null, minSdk = null, targetSdk = null,
                    apks = emptyList(), splits = emptyList(), signatureSchemes = null,
                    problems = listOf(ArchiveProblem(ArchiveProblemCode.INVALID_ARCHIVE, failure.message.orEmpty())),
                    aabModules = emptyList(), installable = false,
                )
            }
            throw InstallFailure(InstallerErrorCodes.INVALID_PACKAGE, "Cannot read the package: ${failure.message}", cause = failure)
        }
        val missing = missingDependencies(inspected.selectedApks.map { it.manifest })
        val archive = if (missing.isEmpty()) inspected else inspected.copy(
            problems = (inspected.problems + missing.map {
                ArchiveProblem(ArchiveProblemCode.MISSING_SPLIT_DEPENDENCY, "Missing required split APK: $it")
            }).distinctBy { Triple(it.code, it.detail, it.blocking) },
        )
        checkActive()
        if (originalDigest != null) {
            val inspectedDigest = if (archive.format == AndroidPackageFormat.APK) archive.selectedApks.singleOrNull()?.sha256
                else sourceDigest(file, checkActive)
            if (!originalDigest.equals(inspectedDigest, ignoreCase = true)) {
                throw InstallFailure(InstallerErrorCodes.INVALID_PACKAGE, "Package source changed while it was being inspected")
            }
        }
        val format = formatOf(archive, displayName)
        val manifest = archive.baseManifest
        if (archive.selectedApks.size > InstallerContract.MAX_SPLITS_PER_PACKAGE) {
            throw InstallFailure(InstallerErrorCodes.INVALID_ARGUMENT, "The selected APK set exceeds ${InstallerContract.MAX_SPLITS_PER_PACKAGE} components", packageName = manifest?.packageName)
        }
        val hasBase = archive.selectedApks.any { it.manifest.splitName.isNullOrBlank() }
        val apks: List<PlannedApk> = when {
            // A plain APK is used in place even when it is a split alone: combine() joins it with its base.
            archive.format == AndroidPackageFormat.APK -> {
                val splitName = manifest?.splitName?.takeIf { it.isNotBlank() }
                listOf(PlannedApk(if (splitName == null) "base.apk" else "split.apk", file, file.length(), splitName,
                    sha256 = archive.selectedApks.singleOrNull()?.sha256, manifest = manifest))
            }
            !archive.canInstall || !prepareForInstallation -> emptyList()
            else -> try {
                archive.stageSelectedApks(File(stagingDirectory, "apks")).map { staged ->
                    PlannedApk(staged.file.name, staged.file, staged.file.length(), staged.manifest.splitName?.takeIf { it.isNotBlank() }, manifest = staged.manifest)
                }
            } catch (failure: IOException) {
                throw InstallFailure(InstallerErrorCodes.INVALID_PACKAGE, "Cannot extract the package: ${failure.message}", packageName = manifest?.packageName, cause = failure)
            }
        }
        val selectedPaths = archive.selectedApks.map { it.archivePath }.toSet()
        val splits = archive.apkEntries.map { entry ->
            val name = if (archive.format == AndroidPackageFormat.APK) displayName else entry.archivePath
            SplitInfo(name, entry.size, entry.archivePath in selectedPaths, splitReason(entry.manifest.splitName, entry.manifest.featureSplit, entry.archivePath in selectedPaths))
        }
        val schemes = if (archive.format == AndroidPackageFormat.APK) runCatching { ApkSignatureDetector.detectSchemes(file) }.getOrNull()?.toSchemeList() else null
        return PreparedPackage(
            format = format,
            displayName = displayName,
            sourceSize = file.length(),
            packageName = manifest?.packageName,
            versionName = manifest?.versionName,
            versionCode = manifest?.versionCode,
            label = manifest?.applicationLabel,
            minSdk = manifest?.minSdk,
            targetSdk = manifest?.targetSdk,
            apks = apks,
            splits = splits,
            signatureSchemes = schemes,
            problems = archive.problems,
            aabModules = archive.aabModules,
            installable = archive.canInstall && hasBase && (apks.isNotEmpty() || !prepareForInstallation),
            displayApk = when {
                archive.format == AndroidPackageFormat.APK -> file
                prepareForInstallation -> apks.firstOrNull { it.splitName == null }?.file
                else -> runCatching { archive.createDisplayApk(stagingDirectory) }.getOrNull()
            },
        )
    }

    /**
     * Combines the APK descriptors of one request item (roadmap D20 `{ splits: [...] }`) into one
     * split set: every part must be a plain APK of the same package and version with exactly one
     * base; otherwise the item fails with `INVALID_PACKAGE`.
     */
    fun combine(parts: List<PreparedPackage>): PreparedPackage {
        if (parts.isEmpty() || parts.size > InstallerContract.MAX_SPLITS_PER_PACKAGE) {
            throw InstallFailure(InstallerErrorCodes.INVALID_ARGUMENT, "Invalid split set size")
        }
        if (parts.size == 1) return parts.single()
        parts.forEach { part ->
            if (part.format != InstallerContract.FORMAT_APK) {
                throw InstallFailure(InstallerErrorCodes.INVALID_PACKAGE, "A split set may only contain plain APK files: ${part.displayName} is ${part.format}", packageName = part.packageName)
            }
            // A split alone lacks its base and a base alone may require splits; the set resolves both.
            if (part.apks.size != 1 || part.problems.any { it.blocking && it.code !in SET_RESOLVED_PROBLEMS }) {
                throw part.failure() ?: InstallFailure(InstallerErrorCodes.INVALID_PACKAGE, "${part.displayName} is not a usable APK", packageName = part.packageName)
            }
        }
        val splitNames = parts.mapNotNull { it.apks.single().splitName }
        splitNames.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.firstOrNull()?.let { duplicate ->
            throw InstallFailure(InstallerErrorCodes.INVALID_PACKAGE, "Duplicate split APK: $duplicate", packageName = parts.first().packageName)
        }
        val packageNames = parts.mapNotNull { it.packageName }.distinct()
        if (packageNames.size != 1 || parts.any { it.packageName == null }) {
            throw InstallFailure(InstallerErrorCodes.INVALID_PACKAGE, "Split set mixes packages: ${parts.map { it.packageName ?: "?" }.distinct()}")
        }
        val versionCodes = parts.map { it.versionCode }.distinct()
        if (versionCodes.size > 1) {
            throw InstallFailure(InstallerErrorCodes.INVALID_PACKAGE, "Split set mixes version codes $versionCodes", packageName = packageNames.single())
        }
        val bases = parts.filter { it.baseApk != null }
        val base = bases.singleOrNull() ?: throw InstallFailure(
            InstallerErrorCodes.INVALID_PACKAGE,
            if (bases.isEmpty()) "Split set has no base APK" else "Split set has ${bases.size} base APKs",
            packageName = packageNames.single(),
        )
        val availableSplits = splitNames.toSet()
        val missing = missingDependencies(parts.mapNotNull { it.apks.single().manifest }, availableSplits)
        if (missing.isNotEmpty()) {
            throw InstallFailure(InstallerErrorCodes.INVALID_PACKAGE, "Missing required split APK: ${missing.joinToString()}", packageName = packageNames.single())
        }
        val ordered = listOf(base) + parts.filter { it !== base }
        val apks = ordered.mapIndexed { index, part ->
            val staged = part.apks.single()
            val splitName = staged.splitName ?: part.displayName.removeSuffix(".apk")
            val name = if (index == 0) "base.apk" else "${index.toString().padStart(3, '0')}-${safeSplit(splitName)}.apk"
            staged.copy(name = name, splitName = if (index == 0) null else splitName)
        }
        return PreparedPackage(
            format = InstallerContract.FORMAT_APK,
            displayName = ordered.joinToString(" + ") { it.displayName },
            sourceSize = ordered.sumOf { it.sourceSize },
            packageName = base.packageName,
            versionName = base.versionName,
            versionCode = base.versionCode,
            label = base.label,
            minSdk = base.minSdk,
            targetSdk = base.targetSdk,
            apks = apks,
            splits = ordered.mapIndexed { index, part -> SplitInfo(part.displayName, part.sourceSize, true, if (index == 0) "base" else "split") },
            signatureSchemes = base.signatureSchemes,
            problems = ordered.flatMap { it.problems }.filterNot { it.code in SET_RESOLVED_PROBLEMS },
            aabModules = emptyList(),
            installable = true,
        )
    }

    /** The PackageArchiveInfo document (protocol `inspect`); the caller adds the platform facts it can read. */
    fun toInspectJson(prepared: PreparedPackage, installed: JsonObject?, iconBase64: String?): JsonObject = JsonObject().apply {
        addProperty(InstallerContract.FIELD_FORMAT, prepared.format)
        addProperty(InstallerContract.FIELD_INSTALLABLE, prepared.installable)
        prepared.packageName?.let { addProperty(InstallerContract.FIELD_PACKAGE_NAME, it) }
        prepared.versionName?.let { addProperty(InstallerContract.FIELD_VERSION_NAME, it) }
        prepared.versionCode?.let { addProperty(InstallerContract.FIELD_VERSION_CODE, it) }
        prepared.label?.let { addProperty(InstallerContract.FIELD_LABEL, it) }
        prepared.minSdk?.let { addProperty(InstallerContract.FIELD_MIN_SDK, it) }
        prepared.targetSdk?.let { addProperty(InstallerContract.FIELD_TARGET_SDK, it) }
        addProperty(InstallerContract.FIELD_SIZE, prepared.sourceSize)
        iconBase64?.let { addProperty(InstallerContract.FIELD_ICON, it) }
        if (prepared.splits.isNotEmpty()) {
            add(InstallerContract.FIELD_SPLITS, JsonArray().apply {
                prepared.splits.forEach { split ->
                    add(JsonObject().apply {
                        addProperty(InstallerContract.FIELD_SPLIT_NAME, split.name)
                        addProperty(InstallerContract.FIELD_SIZE, split.size)
                        addProperty(InstallerContract.FIELD_SPLIT_SELECTED, split.selected)
                        split.reason?.let { addProperty(InstallerContract.FIELD_REASON, it) }
                    })
                }
            })
        }
        if (prepared.aabModules.isNotEmpty()) {
            add("modules", JsonArray().apply { prepared.aabModules.forEach { add(it) } })
        }
        prepared.signatureSchemes?.let { schemes -> add(InstallerContract.FIELD_SIGNATURE_SCHEMES, JsonArray().apply { schemes.forEach { add(it) } }) }
        installed?.let { add(InstallerContract.FIELD_INSTALLED, it) }
        add(InstallerContract.FIELD_PROBLEMS, JsonArray().apply {
            prepared.problems.forEach { add("${it.code.name.lowercase(Locale.ROOT).replace('_', ' ')}: ${it.detail}") }
            prepared.failure()?.takeIf { prepared.problems.isEmpty() }?.let { add(it.message) }
        })
    }

    private fun formatOf(archive: AndroidPackageArchive, displayName: String): String = when (archive.format) {
        AndroidPackageFormat.APK -> InstallerContract.FORMAT_APK
        AndroidPackageFormat.APKS -> if (archive.subtype == AndroidPackageSubtype.GENERIC_APKS && displayName.substringAfterLast('.', "").equals("zip", ignoreCase = true)) InstallerContract.FORMAT_ZIP else InstallerContract.FORMAT_APKS
        AndroidPackageFormat.XAPK -> InstallerContract.FORMAT_XAPK
        AndroidPackageFormat.APKM -> InstallerContract.FORMAT_APKM
        AndroidPackageFormat.APKZ -> InstallerContract.FORMAT_APKZ
        AndroidPackageFormat.AAB -> InstallerContract.FORMAT_AAB
    }

    private fun splitReason(splitName: String?, featureSplit: Boolean, selected: Boolean): String = when {
        splitName.isNullOrBlank() -> "base"
        !selected -> "not for this device"
        featureSplit -> "feature"
        else -> "config"
    }

    private fun safeSplit(displayName: String): String =
        displayName.removeSuffix(".apk").replace(Regex("[^A-Za-z0-9._-]"), "_").trim('.', '_', '-').take(48).ifEmpty { "split" }

    private fun missingDependencies(
        manifests: List<ManifestSummary>,
        availableSplits: Set<String> = manifests.mapNotNull { it.splitName }.toSet(),
    ): List<String> = manifests.flatMap { manifest ->
        manifest.usesSplits + listOfNotNull(manifest.configForSplit?.takeIf { it.isNotBlank() && it != "base" })
    }.filterNot { it in availableSplits }.distinct()

    /** The detector reports a comma separated list such as `v1, v2, v3`; the document carries an array. */
    private fun String.toSchemeList(): List<String> = split(',', ' ', '/').map { it.trim().lowercase(Locale.ROOT) }.filter { it.startsWith("v") }

    fun isKnownExtension(displayName: String): Boolean = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT) in KNOWN_EXTENSIONS

    private fun sourceDigest(file: File, checkActive: () -> Unit): String {
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            file.inputStream().use { input ->
                val buffer = ByteArray(256 * 1024)
                var bytes = 0L
                while (true) {
                    checkActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    bytes += count
                    if (bytes > PackageStaging.MAX_SOURCE_BYTES) {
                        throw InstallFailure(InstallerErrorCodes.INVALID_ARGUMENT, "Package source exceeds the supported size")
                    }
                    digest.update(buffer, 0, count)
                }
            }
        } catch (failure: IOException) {
            throw InstallFailure(InstallerErrorCodes.SOURCE_UNREADABLE, "Cannot read the package source", cause = failure)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
