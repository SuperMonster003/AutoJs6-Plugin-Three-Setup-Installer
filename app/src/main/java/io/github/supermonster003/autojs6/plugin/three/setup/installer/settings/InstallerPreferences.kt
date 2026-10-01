package io.github.supermonster003.autojs6.plugin.three.setup.installer.settings

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerResolver
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerState
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.RequestDocuments
import org.autojs.plugin.installer.api.InstallerContract

/** Pure, strictly decoded ordering. A corrupt stored value restores the documented defaults. */
internal data class AuthorizerPreferences(
    val order: List<Authorizer> = Authorizer.DEFAULT_ORDER,
    val enabled: Set<Authorizer> = Authorizer.entries.toSet(),
) {
    init {
        require(order.size == Authorizer.entries.size && order.toSet() == Authorizer.entries.toSet())
        require(enabled.isNotEmpty() && enabled.all { it in order })
    }

    fun encodeOrder(): String = JsonArray().apply { order.forEach { add(it.id) } }.toString()
    fun encodeEnabled(): String = JsonArray().apply { order.filter { it in enabled }.forEach { add(it.id) } }.toString()

    companion object {
        fun decode(orderJson: String?, enabledJson: String?): AuthorizerPreferences = runCatching {
            fun parse(value: String?): List<Authorizer> {
                require(value != null && value.length <= 256)
                val array = JsonParser.parseString(value).asJsonArray
                require(array.size() in 1..Authorizer.entries.size)
                return array.map { item ->
                    require(item.isJsonPrimitive && item.asJsonPrimitive.isString)
                    requireNotNull(Authorizer.fromId(item.asString))
                }.also { require(it.distinct().size == it.size) }
            }
            if (orderJson == null && enabledJson == null) AuthorizerPreferences()
            else {
                val order = parse(orderJson)
                val enabled = parse(enabledJson).toSet()
                val legacy = setOf(Authorizer.SHIZUKU, Authorizer.ROOT, Authorizer.NONE)
                if (order.size == legacy.size && order.toSet() == legacy) {
                    require(enabled.isNotEmpty() && enabled.all { it in legacy })
                    // Add the new choice before the fallback, but do not enable it for existing users.
                    AuthorizerPreferences(order.flatMap { if (it == Authorizer.NONE) listOf(Authorizer.DHIZUKU, it) else listOf(it) }, enabled)
                } else AuthorizerPreferences(order, enabled)
            }
        }.getOrDefault(AuthorizerPreferences())
    }
}

/** Defaults are used for local requests; explicit host/script options retain their own semantics. */
internal data class InstallerPreferences(
    val options: InstallOptions = InstallOptions(),
    val interaction: String = InstallerContract.INTERACTION_DIALOG,
    val authorizers: AuthorizerPreferences = AuthorizerPreferences(),
    val progressNotifications: Boolean = true,
) {
    fun save(context: Context): Boolean {
        require(InstallerContract.isInteraction(interaction))
        val document = optionsDocument(options)
        InstallOptions.parse(document, "installation defaults")
        return file(context).edit()
            .putString("default_options", document.toString())
            .putString("default_interaction", interaction)
            .putString("authorizer_order", authorizers.encodeOrder())
            .putString("authorizer_enabled", authorizers.encodeEnabled())
            .putBoolean("progress_notifications", progressNotifications)
            .commit()
    }

    companion object {
        private fun file(context: Context) = context.getSharedPreferences("installer_settings", Context.MODE_PRIVATE)

        fun read(context: Context): InstallerPreferences {
            val prefs = file(context)
            val options = runCatching {
                prefs.getString("default_options", null)?.let {
                    InstallOptions.parse(RequestDocuments.parseObject(it, "installation defaults"), "installation defaults")
                } ?: InstallOptions()
            }.getOrDefault(InstallOptions())
            val interaction = runCatching { prefs.getString("default_interaction", null) }.getOrNull()
                ?.takeIf(InstallerContract::isInteraction) ?: InstallerContract.INTERACTION_DIALOG
            val authorizers = AuthorizerPreferences.decode(
                runCatching { prefs.getString("authorizer_order", null) }.getOrNull(),
                runCatching { prefs.getString("authorizer_enabled", null) }.getOrNull(),
            )
            return InstallerPreferences(options, interaction, authorizers,
                runCatching { prefs.getBoolean("progress_notifications", true) }.getOrDefault(true))
        }

        fun resolveAuthorizer(context: Context, requested: String?,
            states: Map<Authorizer, AuthorizerState> = AuthorizerStates.states(context)): Authorizer {
            val preference = read(context).authorizers
            return AuthorizerResolver.resolve(requested, states, preference.order, preference.enabled)
        }

        fun optionsDocument(value: InstallOptions) = JsonObject().apply {
            addProperty(InstallerContract.FIELD_AUTHORIZER, value.authorizer)
            addProperty(InstallerContract.FIELD_ALLOW_DOWNGRADE, value.allowDowngrade)
            addProperty(InstallerContract.FIELD_ALLOW_TEST_ONLY, value.allowTestOnly)
            addProperty(InstallerContract.FIELD_BYPASS_LOW_TARGET_SDK, value.bypassLowTargetSdk)
            value.installer?.let { addProperty(InstallerContract.FIELD_INSTALLER, it) }
            addProperty(InstallerContract.FIELD_USER, value.user)
            addProperty(InstallerContract.FIELD_DELETE_SOURCE, value.deleteSource)
            addProperty(InstallerContract.FIELD_CONTINUE_ON_ERROR, value.continueOnError)
            addProperty(InstallerContract.FIELD_TIMEOUT_MILLIS, value.timeoutMillis)
        }
    }
}
