package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.nio.charset.StandardCharsets

/**
 * Plugin-side decoding of the request documents of the installer contract (protocol document,
 * "JSON Documents"). Every ceiling of `InstallerContract` is enforced here, so the Binder layer
 * refuses a malformed request with `INVALID_ARGUMENT` before any descriptor is read. Pure Kotlin on
 * top of Gson; the JVM tests cover the decision table.
 *
 * zh-CN: 请求文档的插件侧解码与 D31 上限校验, 违反时抛 INVALID_ARGUMENT.
 */
internal object RequestDocuments {

    fun parseObject(json: String?, what: String): JsonObject {
        if (json == null) throw invalid("$what is missing")
        if (json.toByteArray(StandardCharsets.UTF_8).size > InstallerContract.MAX_JSON_BYTES) throw invalid("$what exceeds ${InstallerContract.MAX_JSON_BYTES} bytes")
        val element = try {
            JsonReader(StringReader(json)).use { reader ->
                reader.strictness = Strictness.STRICT
                JsonParser.parseReader(reader).also {
                    if (reader.peek() != JsonToken.END_DOCUMENT) throw invalid("$what contains trailing data")
                }
            }
        } catch (failure: JsonSyntaxException) {
            throw invalid("$what is not valid JSON: ${failure.message}")
        } catch (failure: IllegalStateException) {
            throw invalid("$what is not valid JSON: ${failure.message}")
        } catch (failure: java.io.IOException) {
            throw invalid("$what is not valid JSON: ${failure.message}")
        }
        if (!element.isJsonObject) throw invalid("$what must be a JSON object")
        return element.asJsonObject
    }

    fun invalid(message: String): InstallFailure = InstallFailure(InstallerErrorCodes.INVALID_ARGUMENT, message)

    private fun JsonObject.typed(key: String, type: String, accepts: (JsonElement) -> Boolean): JsonElement? {
        val value = get(key)?.takeUnless { it.isJsonNull } ?: return null
        if (!accepts(value)) throw invalid("$key must be $type")
        return value
    }

    fun JsonObject.string(key: String): String? = typed(key, "a string") { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

    fun JsonObject.long(key: String): Long? = typed(key, "an integer") { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.let {
        try { it.asBigDecimal.longValueExact() } catch (_: ArithmeticException) { throw invalid("$key is outside the integer range") }
            catch (_: NumberFormatException) { throw invalid("$key is not an integer") }
    }

    fun JsonObject.int(key: String): Int? = long(key)?.let { if (it in Int.MIN_VALUE..Int.MAX_VALUE) it.toInt() else throw invalid("$key is outside the integer range") }

    fun JsonObject.boolean(key: String): Boolean? = typed(key, "a boolean") { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }?.asBoolean

    fun JsonObject.obj(key: String): JsonObject? = typed(key, "an object", JsonElement::isJsonObject)?.asJsonObject

    fun JsonObject.array(key: String): List<JsonElement>? = typed(key, "an array", JsonElement::isJsonArray)?.asJsonArray?.toList()

    fun authorizerOf(value: String?, what: String): String {
        val authorizer = value ?: InstallerContract.AUTHORIZER_AUTO
        if (!InstallerContract.isAuthorizer(authorizer)) throw invalid("$what: unknown authorizer '$authorizer'")
        return authorizer
    }

    fun interactionOf(value: String?, what: String): String {
        val interaction = value ?: InstallerContract.INTERACTION_AUTO
        if (!InstallerContract.isInteraction(interaction)) throw invalid("$what: unknown interaction '$interaction'")
        return interaction
    }

    fun userOf(value: String?, what: String): String {
        val user = value ?: InstallerContract.USER_CURRENT
        val valid = user == InstallerContract.USER_CURRENT || user == InstallerContract.USER_ALL || user.toIntOrNull()?.let { it >= 0 } == true
        if (!valid) throw invalid("$what: unknown user '$user'")
        return user
    }

    fun timeoutOf(value: Long?, what: String): Long {
        val timeout = value ?: InstallerContract.DEFAULT_SESSION_TIMEOUT_MILLIS
        if (timeout <= 0L || timeout > InstallerContract.MAX_SESSION_TIMEOUT_MILLIS) throw invalid("$what: timeoutMillis out of range")
        return timeout
    }

    private val PACKAGE_NAME = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")

    fun packageNameOf(value: String?, what: String, required: Boolean): String? {
        if (value == null) {
            if (required) throw invalid("$what: packageName is missing")
            return null
        }
        if (value.length > InstallerContract.MAX_INSTALLER_PACKAGE_LENGTH || !PACKAGE_NAME.matches(value)) throw invalid("$what: invalid package name")
        return value
    }
}

/** The `options` object of an install request with the contract defaults. */
internal data class InstallOptions(
    val authorizer: String = InstallerContract.AUTHORIZER_AUTO,
    val allowDowngrade: Boolean = false,
    val allowTestOnly: Boolean = false,
    val bypassLowTargetSdk: Boolean = false,
    val installer: String? = null,
    val user: String = InstallerContract.USER_CURRENT,
    val deleteSource: Boolean = false,
    val continueOnError: Boolean = true,
    val timeoutMillis: Long = InstallerContract.DEFAULT_SESSION_TIMEOUT_MILLIS,
) {
    /** Names of the options that only a privileged authorizer can honour (protocol "Operations"). */
    val privilegedOptions: List<String>
        get() = buildList {
            if (allowDowngrade) add(InstallerContract.FIELD_ALLOW_DOWNGRADE)
            if (allowTestOnly) add(InstallerContract.FIELD_ALLOW_TEST_ONLY)
            if (bypassLowTargetSdk) add(InstallerContract.FIELD_BYPASS_LOW_TARGET_SDK)
            if (installer != null) add(InstallerContract.FIELD_INSTALLER)
            if (user != InstallerContract.USER_CURRENT) add(InstallerContract.FIELD_USER)
        }

    companion object {
        fun parse(root: JsonObject?, what: String): InstallOptions = with(RequestDocuments) {
            if (root == null) return InstallOptions()
            InstallOptions(
                authorizer = authorizerOf(root.string(InstallerContract.FIELD_AUTHORIZER), what),
                allowDowngrade = root.boolean(InstallerContract.FIELD_ALLOW_DOWNGRADE) ?: false,
                allowTestOnly = root.boolean(InstallerContract.FIELD_ALLOW_TEST_ONLY) ?: false,
                bypassLowTargetSdk = root.boolean(InstallerContract.FIELD_BYPASS_LOW_TARGET_SDK) ?: false,
                installer = packageNameOf(root.string(InstallerContract.FIELD_INSTALLER), "$what: installer", required = false),
                user = userOf(root.string(InstallerContract.FIELD_USER), what),
                deleteSource = root.boolean(InstallerContract.FIELD_DELETE_SOURCE) ?: false,
                continueOnError = root.boolean(InstallerContract.FIELD_CONTINUE_ON_ERROR) ?: true,
                timeoutMillis = timeoutOf(root.long(InstallerContract.FIELD_TIMEOUT_MILLIS), what),
            )
        }
    }
}

/** One descriptor of an install request; [descriptor] is its position in the `sources` array. */
internal data class SourceEntry(val descriptor: Int, val item: Int, val displayName: String, val size: Long)

/** A decoded and validated install request (`requestJson` of `openSession`). */
internal data class InstallRequest(
    val id: String,
    val sources: List<SourceEntry>,
    val interaction: String,
    val options: InstallOptions,
    val isBatch: Boolean = sources.map { it.item }.distinct().size > 1,
    val origin: String = InstallerContract.SOURCE_HOST,
) {
    /** The request items in first-appearance order; each holds the descriptors of one package. */
    val items: List<List<SourceEntry>> = sources.groupBy { it.item }.values.toList()

    companion object {
        private const val WHAT = "install request"

        /** Decodes [json]; [descriptorCount] is the length of the descriptor array the request arrived with. */
        fun parse(json: String?, descriptorCount: Int): InstallRequest = with(RequestDocuments) {
            if (descriptorCount !in 1..InstallerContract.MAX_BATCH_SOURCES * InstallerContract.MAX_SPLITS_PER_PACKAGE) throw invalid("$WHAT: invalid descriptor count")
            val root = parseObject(json, WHAT)
            val id = root.string(InstallerContract.FIELD_ID)?.takeIf { it.isNotBlank() && it.length <= 128 } ?: throw invalid("$WHAT: id is missing")
            val sources = root.array(InstallerContract.FIELD_SOURCES) ?: throw invalid("$WHAT: sources is missing")
            if (sources.isEmpty()) throw invalid("$WHAT: sources is empty")
            if (sources.size != descriptorCount) throw invalid("$WHAT: sources describes ${sources.size} descriptors but ${descriptorCount} arrived")
            val entries = sources.mapIndexed { index, element ->
                val source = element.takeIf(JsonElement::isJsonObject)?.asJsonObject ?: throw invalid("$WHAT: sources[$index] must be an object")
                val item = source.int(InstallerContract.FIELD_ITEM) ?: index
                if (item < 0) throw invalid("$WHAT: sources[$index].item is negative")
                val displayName = source.string(InstallerContract.FIELD_DISPLAY_NAME)?.takeIf { it.isNotBlank() } ?: throw invalid("$WHAT: sources[$index].displayName is missing")
                if (displayName.length > InstallerContract.MAX_DISPLAY_NAME_LENGTH) throw invalid("$WHAT: sources[$index].displayName is too long")
                val size = source.long(InstallerContract.FIELD_SIZE) ?: -1L
                if (size < -1L) throw invalid("$WHAT: sources[$index].size is invalid")
                SourceEntry(index, item, displayName, size)
            }
            val items = entries.groupBy { it.item }
            if (items.size > InstallerContract.MAX_BATCH_SOURCES) throw invalid("$WHAT: at most ${InstallerContract.MAX_BATCH_SOURCES} packages per session")
            items.values.firstOrNull { it.size > InstallerContract.MAX_SPLITS_PER_PACKAGE }?.let {
                throw invalid("$WHAT: at most ${InstallerContract.MAX_SPLITS_PER_PACKAGE} descriptors per package")
            }
            val isBatch = root.get(InstallerContract.FIELD_BATCH)?.let { value ->
                if (!value.isJsonPrimitive || !value.asJsonPrimitive.isBoolean) throw invalid("$WHAT: batch must be a boolean")
                value.asBoolean
            } ?: (items.size > 1)
            if (!isBatch && items.size != 1) throw invalid("$WHAT: a non-batch request must contain exactly one package")
            val origin = root.string(InstallerContract.FIELD_SOURCE_ORIGIN) ?: InstallerContract.SOURCE_HOST
            if (origin !in setOf(InstallerContract.SOURCE_HOST, InstallerContract.SOURCE_SCRIPT)) throw invalid("$WHAT: invalid source origin")
            InstallRequest(
                id = id,
                sources = entries,
                interaction = interactionOf(root.string(InstallerContract.FIELD_INTERACTION), WHAT),
                options = InstallOptions.parse(root.obj(InstallerContract.FIELD_OPTIONS), WHAT),
                isBatch = isBatch,
                origin = origin,
            )
        }
    }
}

/** A decoded uninstall request (`requestJson` of `uninstall`). */
internal data class UninstallRequest(
    val packageName: String,
    val keepData: Boolean,
    val user: String,
    val authorizer: String,
    val interaction: String,
    val timeoutMillis: Long,
) {
    val privilegedOptions: List<String>
        get() = buildList {
            if (keepData) add(InstallerContract.FIELD_KEEP_DATA)
            if (user != InstallerContract.USER_CURRENT) add(InstallerContract.FIELD_USER)
        }

    companion object {
        private const val WHAT = "uninstall request"

        fun parse(json: String?): UninstallRequest = with(RequestDocuments) {
            val root = parseObject(json, WHAT)
            UninstallRequest(
                packageName = packageNameOf(root.string(InstallerContract.FIELD_PACKAGE_NAME), WHAT, required = true)!!,
                keepData = root.boolean(InstallerContract.FIELD_KEEP_DATA) ?: false,
                user = userOf(root.string(InstallerContract.FIELD_USER), WHAT),
                authorizer = authorizerOf(root.string(InstallerContract.FIELD_AUTHORIZER), WHAT),
                interaction = interactionOf(root.string(InstallerContract.FIELD_INTERACTION), WHAT),
                timeoutMillis = timeoutOf(root.long(InstallerContract.FIELD_TIMEOUT_MILLIS), WHAT),
            )
        }
    }
}

/** The `{ authorizer }` request of `getUsers` and `setDefaultInstaller`. */
internal data class AuthorizerRequest(val authorizer: String) {
    companion object {
        fun parse(json: String?, what: String): AuthorizerRequest = with(RequestDocuments) {
            val root = if (json == null) JsonObject() else parseObject(json, what)
            AuthorizerRequest(authorizerOf(root.string(InstallerContract.FIELD_AUTHORIZER), what))
        }
    }
}

/** The `{ displayName, size }` request of `inspect`. */
internal data class InspectRequest(val displayName: String, val size: Long) {
    companion object {
        fun parse(json: String?): InspectRequest = with(RequestDocuments) {
            val root = parseObject(json, "inspect request")
            val displayName = root.string(InstallerContract.FIELD_DISPLAY_NAME)?.takeIf { it.isNotBlank() } ?: "package.apk"
            if (displayName.length > InstallerContract.MAX_DISPLAY_NAME_LENGTH) throw invalid("inspect request: displayName is too long")
            val size = root.long(InstallerContract.FIELD_SIZE) ?: -1L
            if (size < -1L) throw invalid("inspect request: size is invalid")
            InspectRequest(displayName, size)
        }
    }
}
