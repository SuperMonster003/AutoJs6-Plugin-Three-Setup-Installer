package io.github.supermonster003.autojs6.plugin.three.setup.installer.policy

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import io.github.supermonster003.autojs6.plugin.three.setup.installer.R
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.PrivilegedClient
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.DeviceUsers
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallSession
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.StorageErrors
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PackageStaging
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PlannedApk
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import org.autojs.plugin.installer.api.InstallerContract as C
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.security.MessageDigest
import java.io.File
import java.util.UUID

/** All installation entrances use this guard. Display metadata never grants permission to write. */
internal class InstallSafetyGate(
    private val context: Context,
    private val ownerToken: String,
    private val review: ((InstallSafetyBinding, Long, () -> Unit) -> DialogSafetyApproval)? = null,
    private val remaining: () -> Long = { PrivilegedClient.BIND_TIMEOUT_MILLIS },
    private val stagingRoot: () -> File,
) {
    private val sourceSigningFacts = mutableMapOf<String, Set<String>>()
    fun checkRules(prepared: PreparedPackage) {
        val name = prepared.packageName ?: throw InstallFailure(InstallerErrorCodes.INVALID_PACKAGE, "Package identity is missing")
        val blocked = InstallSafetyPreferences.read(context).block(name, prepared.sharedUserId)
        if (blocked != null) fail(when (blocked) {
            InstallSafetyPolicy.Block.UNREADABLE -> R.string.policy_unreadable
            InstallSafetyPolicy.Block.PACKAGE -> R.string.policy_package_blocked
            InstallSafetyPolicy.Block.SHARED_USER -> R.string.policy_shared_user_blocked
            InstallSafetyPolicy.Block.INSTALLED_IDENTITY_UNKNOWN -> R.string.policy_installed_identity_unknown
        }, name)
    }

    fun review(index: Int, prepared: PreparedPackage, target: InstallSession.Target, options: InstallOptions,
        interaction: String, deadline: Long, onReviewRequired: () -> Unit, checkActive: () -> Unit): DialogSafetyApproval? {
        val facts = assess(index, prepared, target, options, checkActive)
        if (facts.signatureRisk == SignatureRisk.NONE) return null
        if (interaction != C.INTERACTION_DIALOG || review == null) fail(R.string.policy_signature_dialog_required, facts.packageName)
        onReviewRequired()
        return review.invoke(facts, deadline, checkActive)
    }

    /** Called only after acquiring the package lock and immediately before opening its session. */
    fun validate(index: Int, prepared: PreparedPackage, target: InstallSession.Target, options: InstallOptions,
        approval: DialogSafetyApproval?, checkActive: () -> Unit) {
        val facts = assess(index, prepared, target, options, checkActive)
        if (approval != null) {
            if (!approval.consume(ownerToken, facts)) fail(R.string.policy_review_changed, facts.packageName)
        } else if (facts.signatureRisk != SignatureRisk.NONE) fail(R.string.policy_signature_dialog_required, facts.packageName)
    }

    private fun assess(index: Int, prepared: PreparedPackage, target: InstallSession.Target,
        options: InstallOptions, checkActive: () -> Unit): InstallSafetyBinding {
        checkActive()
        val name = prepared.packageName ?: throw InstallFailure(InstallerErrorCodes.INVALID_PACKAGE, "Package identity is missing")
        val policy = InstallSafetyPreferences.read(context)
        policy.block(name, prepared.sharedUserId)?.let { blocked -> fail(when (blocked) {
            InstallSafetyPolicy.Block.UNREADABLE -> R.string.policy_unreadable
            InstallSafetyPolicy.Block.PACKAGE -> R.string.policy_package_blocked
            InstallSafetyPolicy.Block.SHARED_USER -> R.string.policy_shared_user_blocked
            InstallSafetyPolicy.Block.INSTALLED_IDENTITY_UNKNOWN -> R.string.policy_installed_identity_unknown
        }, name) }
        val digests = prepared.apks.map { apk ->
            val expected = apk.sha256 ?: throw InstallFailure(InstallerErrorCodes.INVALID_PACKAGE, "The selected APK has no verified content identity", packageName = name)
            val digest = MessageDigest.getInstance("SHA-256")
            var size = 0L
            apk.file.inputStream().use { input ->
                val buffer = ByteArray(1024 * 1024)
                while (true) {
                    checkActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    size += count
                    if (size > apk.size) throw changedSource(name)
                    digest.update(buffer, 0, count)
                }
            }
            if (size != apk.size || digest.digest().hex() != expected.lowercase(java.util.Locale.ROOT)) throw changedSource(name)
            ApkSafetyDigest(apk.name, apk.size, expected.lowercase(java.util.Locale.ROOT))
        }.sortedBy { it.name }
        checkActive()
        // The platform verifies the base APK. Scheme-presence detection and arbitrary split
        // metadata are not certificates. Android still verifies the final complete split set.
        val flags = signingFlags()
        val base = prepared.baseApk ?: throw InstallFailure(InstallerErrorCodes.INVALID_PACKAGE, "The base APK is missing", packageName = name)
        val baseHash = requireNotNull(base.sha256)
        val sourceSigners = sourceSigningFacts.getOrPut(baseHash) {
            val stable = stableBase(base, name, checkActive)
            try {
                runCatching { context.packageManager.getPackageArchiveInfo(stable.file.absolutePath, flags)
                    ?.takeIf { it.packageName == name }?.let(::signers).orEmpty() }.getOrDefault(emptySet())
            } finally { stable.close() }
        }
        checkActive()
        val installed = installed(name, target)
        checkActive()
        policy.block(name, prepared.sharedUserId, installed)?.let { blocked -> fail(when (blocked) {
            InstallSafetyPolicy.Block.UNREADABLE -> R.string.policy_unreadable
            InstallSafetyPolicy.Block.PACKAGE -> R.string.policy_package_blocked
            InstallSafetyPolicy.Block.SHARED_USER -> R.string.policy_shared_user_blocked
            InstallSafetyPolicy.Block.INSTALLED_IDENTITY_UNKNOWN -> R.string.policy_installed_identity_unknown
        }, name) }
        return InstallSafetyBinding(index, name, prepared.sharedUserId, digests, target.userId, options.user,
            target.authorizer.id, policy.revision, sourceSigners, installed)
    }

    /** A held descriptor pins an inode, not its bytes. Never combine one read's hash with
     * another read's certificates from a provider/host-writable inode. */
    private class StableBase(val file: File, private val temporary: Boolean) : java.io.Closeable {
        override fun close() { if (temporary) { file.delete(); file.parentFile?.delete() } }
    }

    private fun stableBase(apk: PlannedApk, name: String, checkActive: () -> Unit): StableBase {
        val root = stagingRoot().absoluteFile
        val privateRoot = root.canonicalFile
        val cache = context.cacheDir.canonicalFile
        check(privateRoot.path.startsWith(cache.path + File.separator))
        val path = apk.file.absoluteFile
        if (path.path.startsWith(root.path + File.separator)) {
            val relative = path.path.removePrefix(root.path + File.separator)
            // Permit Android's data-dir alias, but no symlink inside this request's own root.
            if (path.canonicalFile == File(privateRoot, relative).absoluteFile) return StableBase(path, false)
        }
        if (apk.size !in 1..PackageStaging.MAX_SOURCE_BYTES) throw changedSource(name)
        val folder = PackageStaging.newChildDirectory(root, "signature-${UUID.randomUUID()}")
        val snapshot = File(folder, "base.apk")
        try {
            if (folder.usableSpace < apk.size + 16L * 1024 * 1024) throw InstallFailure(InstallerErrorCodes.INSUFFICIENT_STORAGE,
                "Not enough temporary storage for signature verification", packageName = name)
            val digest = MessageDigest.getInstance("SHA-256")
            var copied = 0L
            apk.file.inputStream().use { input -> snapshot.outputStream().use { output ->
                val buffer = ByteArray(1024 * 1024)
                while (true) {
                    checkActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    copied += count
                    if (copied > apk.size) throw changedSource(name)
                    output.write(buffer, 0, count)
                    digest.update(buffer, 0, count)
                }
            } }
            if (copied != apk.size || digest.digest().hex() != apk.sha256) throw changedSource(name)
            check(snapshot.setReadOnly()) { "The private signature snapshot could not be sealed" }
            checkActive()
            return StableBase(snapshot, true)
        } catch (failure: Exception) {
            snapshot.delete()
            folder.delete()
            if (failure is InstallFailure) throw failure
            if (StorageErrors.isInsufficientStorage(failure)) throw StorageErrors.failure(failure, name)
            throw InstallFailure(InstallerErrorCodes.SOURCE_UNREADABLE, "A private signature snapshot could not be prepared", packageName = name, cause = failure)
        }
    }

    @Suppress("DEPRECATION")
    private fun installed(name: String, target: InstallSession.Target): InstalledSigningFact = try {
        if (target.authorizer !in setOf(Authorizer.SHIZUKU, Authorizer.ROOT)) {
            check(target.userId == DeviceUsers(context).currentId)
            val info = try { context.packageManager.getPackageInfo(name, signingFlags() or PackageManager.MATCH_UNINSTALLED_PACKAGES) }
                catch (_: PackageManager.NameNotFoundException) { null }
            if (info == null) InstalledSigningFact(true, false, global = false)
            else {
                check(info.packageName == name)
                InstalledSigningFact(true, true, (info.applicationInfo?.flags?.and(ApplicationInfo.FLAG_INSTALLED) ?: 0) != 0,
                    signers(info), if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong(),
                    info.versionName, info.lastUpdateTime, info.sharedUserId, global = false)
            }
        } else {
            check(target.authorizer in setOf(Authorizer.SHIZUKU, Authorizer.ROOT))
            val reply = PrivilegedClient.get(context).acquire(target.authorizer, remaining()).getInstalledSigningInfo(name, target.userId)
            check(reply.getString("packageName") == name && reply.containsKey("userId") && reply.getInt("userId") == target.userId && reply.containsKey("found"))
            if (!reply.getBoolean("found")) InstalledSigningFact(true, false)
            else {
                val values = reply.getParcelableArray("currentSigners").orEmpty().map { it as Signature }.toTypedArray()
                check(reply.containsKey("versionCode") && reply.containsKey("lastUpdateTime") && reply.containsKey("installedForUser"))
                InstalledSigningFact(true, true, reply.getBoolean("installedForUser"), fingerprints(values),
                    reply.getLong("versionCode"), reply.getString("versionName"), reply.getLong("lastUpdateTime"), reply.getString("sharedUserId"))
            }
        }
    } catch (_: Exception) {
        InstalledSigningFact(false, false)
    }

    @Suppress("DEPRECATION")
    private fun signers(info: PackageInfo): Set<String> = fingerprints(
        if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners.orEmpty() else info.signatures.orEmpty())

    private fun fingerprints(values: Array<out Signature>): Set<String> {
        if (values.isEmpty() || values.size > 64) return emptySet()
        return values.mapTo(sortedSetOf()) { value ->
            val bytes = value.toByteArray()
            require(bytes.size in 1..(1024 * 1024))
            MessageDigest.getInstance("SHA-256").digest(bytes).hex()
        }
    }

    @Suppress("DEPRECATION")
    private fun signingFlags() = PackageManager.GET_SIGNATURES or if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else 0
    private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }
    private fun changedSource(name: String) = InstallFailure(InstallerErrorCodes.INVALID_PACKAGE, "The selected APK changed before installation", packageName = name)
    private fun fail(message: Int, packageName: String): Nothing {
        val text = context.getString(message)
        throw InstallFailure(InstallerErrorCodes.BLOCKED_BY_POLICY, text, packageName = packageName, systemMessage = text)
    }
}
