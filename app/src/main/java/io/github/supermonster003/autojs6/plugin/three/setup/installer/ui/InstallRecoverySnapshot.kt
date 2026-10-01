package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes

/** Display facts only. This type cannot describe a source, permission grant, Intent or worker. */
internal data class InstallRecoverySnapshot(
    val token: String,
    val revision: Long,
    val savedAt: Long,
    val expiresAt: Long,
    val terminal: Boolean,
    val stage: String,
    val index: Int,
    val items: List<Item>,
    val failure: Failure? = null,
    val canDeleteSource: Boolean = false,
) {
    data class Failure(val code: String, val status: Int? = null, val platformCode: String? = null) {
        fun json(): JsonObject = JsonObject().apply {
            addProperty(InstallerContract.FIELD_ERROR_CODE, code)
            status?.let { addProperty(InstallerContract.FIELD_ERROR_STATUS, it) }
            platformCode?.let { addProperty(InstallerContract.FIELD_ERROR_SYSTEM_MESSAGE, it) }
        }
    }
    data class Item(val label: String, val packageName: String?, val versionName: String?, val versionCode: Long?,
        val previousVersionCode: Long?, val user: String, val deleteRequested: Boolean, val ok: Boolean?,
        val sourceDeleted: Boolean?, val failure: Failure?, val followUpPending: Boolean = false)

    fun display(): InstallPresentation.Snapshot {
        val interrupted = !terminal
        val restoredItems = items.map { item ->
            val error = item.failure ?: if (item.ok == null) Failure(InstallerErrorCodes.CANCELLED) else null
            val result = JsonObject().apply {
                addProperty(InstallerContract.FIELD_OK, item.ok == true)
                item.packageName?.let { addProperty(InstallerContract.FIELD_PACKAGE_NAME, it) }
                item.versionName?.let { addProperty(InstallerContract.FIELD_VERSION_NAME, it) }
                item.versionCode?.let { addProperty(InstallerContract.FIELD_VERSION_CODE, it) }
                item.previousVersionCode?.let { addProperty(InstallerContract.FIELD_PREVIOUS_VERSION_CODE, it) }
                item.sourceDeleted?.let { addProperty(InstallerContract.FIELD_SOURCE_DELETED, it) }
                error?.let { add(InstallerContract.FIELD_ERROR, it.json()) }
            }
            InstallPresentation.Item(item.label,
                stage = when (item.ok) { true -> InstallerContract.STAGE_COMPLETED; false -> InstallerContract.STAGE_FAILED; null -> InstallerContract.STAGE_CANCELLED },
                result = result, options = InstallOptions(user = item.user, deleteSource = item.deleteRequested), followUpPending = item.followUpPending)
        }
        return InstallPresentation.Snapshot(revision,
            if (interrupted) InstallerContract.STAGE_CANCELLED else stage, index, 0f, restoredItems, null, true,
            failure?.let { InstallFailure(it.code, it.code, it.status, it.platformCode) }, false, canDeleteSource,
            recovered = true, interrupted = interrupted)
    }

    companion object {
        const val RETENTION_MILLIS = 600_000L
        const val MAX_TTL_MILLIS = InstallerContract.MAX_SESSION_TIMEOUT_MILLIS + RETENTION_MILLIS
        const val MAX_BYTES = 65_536
        const val MAX_ENTRIES = 128
        const val FUTURE_SKEW_MILLIS = 300_000L
        private val TOKEN = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        private val PACKAGE = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")
        private val PLATFORM = Regex("(?:INSTALL|DELETE)_FAILED_[A-Z0-9_]{1,72}")
        fun validToken(token: String) = TOKEN.matches(token)

        /** Persist no arbitrary error text: only a recognized platform error identifier survives. */
        private fun failure(code: String?, status: Int?, message: String?): Failure = Failure(
            code?.takeIf(InstallerErrorCodes::isKnown) ?: InstallerErrorCodes.INTERNAL,
            status, message?.let { PLATFORM.find(it)?.value })

        private fun text(value: String?, limit: Int): String? = value?.let { source ->
            val cleaned = source.replace(Regex("[A-Za-z][A-Za-z0-9+.-]*://\\S+"), "...")
                .replace(Regex("(?:^|\\s)(?:/[A-Za-z0-9._~%+-]+){2,}[^\\s]*"), " ...")
                .replace(Regex("[\\p{Cntrl}\\\\]"), " ").trim()
            if (cleaned.length <= limit) cleaned else cleaned.take(limit - 3).trimEnd { Character.isHighSurrogate(it) } + "..."
        }

        fun capture(token: String, state: InstallPresentation.Snapshot, defaultOptions: InstallOptions,
            now: Long, remainingMillis: Long): InstallRecoverySnapshot {
            val mapped = state.items.map { item ->
                val result = item.result
                val error = result?.getAsJsonObject(InstallerContract.FIELD_ERROR)
                val ok = result?.get(InstallerContract.FIELD_OK)?.asBoolean
                val options = item.options ?: defaultOptions
                Item(text(item.metadata?.label ?: item.displayName, 96).orEmpty(),
                    (result?.get(InstallerContract.FIELD_PACKAGE_NAME)?.asString ?: item.metadata?.packageName)
                        ?.takeIf { it.length <= 255 && PACKAGE.matches(it) },
                    text(result?.get(InstallerContract.FIELD_VERSION_NAME)?.asString ?: item.metadata?.versionName, 64),
                    result?.get(InstallerContract.FIELD_VERSION_CODE)?.asLong ?: item.metadata?.versionCode,
                    result?.get(InstallerContract.FIELD_PREVIOUS_VERSION_CODE)?.asLong ?: item.metadata?.previousVersion?.code,
                    options.user, options.deleteSource, ok, result?.get(InstallerContract.FIELD_SOURCE_DELETED)?.asBoolean,
                    if (ok == false) failure(error?.get(InstallerContract.FIELD_ERROR_CODE)?.asString,
                        error?.get(InstallerContract.FIELD_ERROR_STATUS)?.asInt,
                        error?.get(InstallerContract.FIELD_ERROR_SYSTEM_MESSAGE)?.asString) else null, item.followUpPending)
            }
            return InstallRecoverySnapshot(token, state.revision, now,
                now + remainingMillis.coerceIn(1, if (state.terminal) RETENTION_MILLIS else MAX_TTL_MILLIS),
                state.terminal, state.stage, state.index, mapped,
                state.failure?.let { failure(it.code, it.status, it.systemMessage) }, state.canDeleteSource)
        }

        fun validate(value: InstallRecoverySnapshot) {
            require(validToken(value.token) && value.revision >= 0)
            require(value.savedAt > 0 && value.savedAt <= Long.MAX_VALUE - MAX_TTL_MILLIS)
            require(value.expiresAt > value.savedAt && value.expiresAt - value.savedAt <=
                if (value.terminal) RETENTION_MILLIS else MAX_TTL_MILLIS)
            require(value.stage in InstallerContract.STAGES && value.items.size in 1..InstallerContract.MAX_BATCH_SOURCES)
            require(value.terminal == (value.stage in InstallerContract.TERMINAL_STAGES))
            if (value.stage == InstallerContract.STAGE_COMPLETED) require(value.items.all { it.ok == true })
            require(value.index in value.items.indices)
            fun checkFailure(error: Failure?) {
                if (error == null) return
                require(InstallerErrorCodes.isKnown(error.code))
                require(error.platformCode == null || PLATFORM.matches(error.platformCode))
            }
            value.items.forEach { item ->
                require(!item.followUpPending || item.ok == true)
                require(item.label.length <= 96 && text(item.label, 96) == item.label)
                require(item.packageName == null || item.packageName.length <= 255 && PACKAGE.matches(item.packageName))
                require(item.versionName == null || item.versionName.length <= 64 && text(item.versionName, 64) == item.versionName)
                require(item.user == InstallerContract.USER_CURRENT || item.user == InstallerContract.USER_ALL || item.user.toIntOrNull()?.let { it >= 0 } == true)
                require((item.ok == false) == (item.failure != null))
                checkFailure(item.failure)
            }
            checkFailure(value.failure)
        }

        fun validAt(value: InstallRecoverySnapshot, now: Long): Boolean = now >= value.savedAt - FUTURE_SKEW_MILLIS && now < value.expiresAt
    }
}
