package io.github.supermonster003.autojs6.plugin.three.setup.installer.policy

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** Local install policy. No rule or dialog exception is an option supplied by a script. */
internal data class InstallSafetyPolicy(
    val revision: String = INITIAL_REVISION,
    val packages: Set<String> = emptySet(),
    val sharedUsers: Set<String> = emptySet(),
    val readable: Boolean = true,
) {
    enum class Block { UNREADABLE, PACKAGE, SHARED_USER, INSTALLED_IDENTITY_UNKNOWN }
    fun block(packageName: String, sharedUserId: String?, installed: InstalledSigningFact? = null): Block? = when {
        !readable -> Block.UNREADABLE
        packageName in packages -> Block.PACKAGE
        sharedUserId != null && sharedUserId in sharedUsers -> Block.SHARED_USER
        installed != null && !installed.known && sharedUsers.isNotEmpty() -> Block.INSTALLED_IDENTITY_UNKNOWN
        installed != null && !installed.found && !installed.global && sharedUsers.isNotEmpty() -> Block.INSTALLED_IDENTITY_UNKNOWN
        installed?.sharedUserId != null && installed.sharedUserId in sharedUsers -> Block.SHARED_USER
        else -> null
    }

    companion object {
        const val INITIAL_REVISION = "unconfigured"
        const val MAX_RULES = 256
        const val MAX_IDENTIFIER_LENGTH = 255
        const val MAX_EDITOR_LENGTH = 32_768
        private val IDENTIFIER = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)*")

        fun validIdentifier(value: String): Boolean = value.length in 1..MAX_IDENTIFIER_LENGTH && IDENTIFIER.matches(value)

        fun parseLines(value: String): Set<String> {
            require(value.length <= MAX_EDITOR_LENGTH) { "The rule list is too long" }
            return value.lineSequence().map(String::trim).filter(String::isNotEmpty).toSet().also { rules ->
                require(rules.size <= MAX_RULES && rules.all(::validIdentifier)) { "Invalid package or shared-user identifier" }
            }
        }

        fun encode(rules: Set<String>): String = JsonArray().apply { rules.sorted().forEach { add(it) } }.toString()

        fun decode(revision: String?, packages: String?, sharedUsers: String?): InstallSafetyPolicy {
            if (revision == null && packages == null && sharedUsers == null) return InstallSafetyPolicy()
            return runCatching {
                require(revision != null && UUID.fromString(revision).toString() == revision)
                fun rules(text: String?): Set<String> {
                    require(text != null && text.length <= 65_536)
                    val array = JsonReader(StringReader(text)).use { reader ->
                        reader.strictness = Strictness.STRICT
                        JsonParser.parseReader(reader).also { require(reader.peek() == JsonToken.END_DOCUMENT) }.asJsonArray
                    }
                    require(array.size() <= MAX_RULES)
                    val result = array.map { element ->
                        require(element.isJsonPrimitive && element.asJsonPrimitive.isString)
                        element.asString.also { require(validIdentifier(it)) }
                    }
                    require(result.size == result.distinct().size)
                    return result.toSet()
                }
                val packageRules = rules(packages)
                val sharedRules = rules(sharedUsers)
                require(packageRules.size + sharedRules.size <= MAX_RULES)
                InstallSafetyPolicy(revision, packageRules, sharedRules)
            }.getOrElse { InstallSafetyPolicy("unreadable", readable = false) }
        }
    }
}

internal data class ApkSafetyDigest(val name: String, val size: Long, val sha256: String)

/** Empty source signers or a failed installed-package query are unknown, never a new install. */
internal data class InstalledSigningFact(
    val known: Boolean,
    val found: Boolean,
    val installedForUser: Boolean = false,
    val signers: Set<String> = emptySet(),
    val versionCode: Long? = null,
    val versionName: String? = null,
    val lastUpdateTime: Long? = null,
    val sharedUserId: String? = null,
    val global: Boolean = true,
)

internal enum class SignatureRisk { NONE, MISMATCH, UNKNOWN }

/** Complete immutable identity reviewed by the user, sampled again after the package lock. */
internal data class InstallSafetyBinding(
    val itemIndex: Int,
    val packageName: String,
    val sharedUserId: String?,
    val apks: List<ApkSafetyDigest>,
    val userId: Int,
    val requestedUser: String,
    val authorizer: String,
    val policyRevision: String,
    val sourceSigners: Set<String>,
    val installed: InstalledSigningFact,
) {
    val signatureRisk: SignatureRisk get() = when {
        sourceSigners.isEmpty() || !installed.known || installed.found && installed.signers.isEmpty() -> SignatureRisk.UNKNOWN
        !installed.found || sourceSigners == installed.signers -> SignatureRisk.NONE
        else -> SignatureRisk.MISMATCH
    }
}

/** Process-local, one item and one prompt only; deliberately not Parcelable or JSON serializable. */
internal class DialogSafetyApproval private constructor(
    private val ownerToken: String,
    val promptToken: String,
    private val binding: InstallSafetyBinding,
) {
    private val consumed = AtomicBoolean()
    fun consume(owner: String, current: InstallSafetyBinding): Boolean =
        consumed.compareAndSet(false, true) && owner == ownerToken && current == binding

    companion object {
        fun fromDialog(ownerToken: String, promptToken: String, binding: InstallSafetyBinding): DialogSafetyApproval {
            require(ownerToken.isNotBlank() && UUID.fromString(promptToken).toString() == promptToken)
            require(binding.signatureRisk != SignatureRisk.NONE)
            return DialogSafetyApproval(ownerToken, promptToken, binding.copy(apks = binding.apks.toList(),
                sourceSigners = binding.sourceSigners.toSet(), installed = binding.installed.copy(signers = binding.installed.signers.toSet())))
        }
    }
}
