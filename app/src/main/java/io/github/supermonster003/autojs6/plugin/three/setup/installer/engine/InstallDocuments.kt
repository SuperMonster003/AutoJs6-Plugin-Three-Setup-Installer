package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerState
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes

/**
 * Maps `PackageInstaller.STATUS_*` answers to contract failures (protocol document, "Error
 * Codes"). The status constants are repeated here so the mapping stays testable on the JVM.
 */
internal object InstallStatusMapper {

    const val STATUS_PENDING_USER_ACTION = -1
    const val STATUS_SUCCESS = 0
    const val STATUS_FAILURE = 1
    const val STATUS_FAILURE_BLOCKED = 2
    const val STATUS_FAILURE_ABORTED = 3
    const val STATUS_FAILURE_INVALID = 4
    const val STATUS_FAILURE_CONFLICT = 5
    const val STATUS_FAILURE_STORAGE = 6
    const val STATUS_FAILURE_INCOMPATIBLE = 7
    const val STATUS_FAILURE_TIMEOUT = 8

    private val SIGNATURE_CONFLICTS = listOf("INSTALL_FAILED_UPDATE_INCOMPATIBLE", "INSTALL_FAILED_SHARED_USER_INCOMPATIBLE")

    fun toFailure(status: Int, message: String?, packageName: String?, uninstall: Boolean = false): InstallFailure {
        val code = when (status) {
            STATUS_FAILURE_ABORTED -> InstallerErrorCodes.USER_CANCELLED
            STATUS_FAILURE_BLOCKED -> InstallerErrorCodes.BLOCKED_BY_POLICY
            STATUS_FAILURE_CONFLICT -> if (SIGNATURE_CONFLICTS.any { message?.contains(it) == true }) InstallerErrorCodes.SIGNATURE_MISMATCH else fallback(uninstall)
            STATUS_FAILURE_STORAGE -> InstallerErrorCodes.INSUFFICIENT_STORAGE
            STATUS_FAILURE_INCOMPATIBLE -> InstallerErrorCodes.INCOMPATIBLE_DEVICE
            STATUS_FAILURE_INVALID -> when {
                uninstall -> InstallerErrorCodes.UNINSTALL_FAILED
                message?.contains("INSTALL_FAILED_VERSION_DOWNGRADE") == true -> InstallerErrorCodes.INSTALL_FAILED
                else -> InstallerErrorCodes.INVALID_PACKAGE
            }
            STATUS_FAILURE_TIMEOUT -> InstallerErrorCodes.TIMEOUT
            else -> fallback(uninstall)
        }
        val text = when {
            !message.isNullOrBlank() -> message
            uninstall -> "Uninstallation failed with status $status"
            else -> "Installation failed with status $status"
        }
        return InstallFailure(code, text, status = status, systemMessage = message?.takeIf { it.isNotBlank() }, packageName = packageName)
    }

    private fun fallback(uninstall: Boolean) = if (uninstall) InstallerErrorCodes.UNINSTALL_FAILED else InstallerErrorCodes.INSTALL_FAILED
}

/** Builders of the result, detail and status documents the plugin sends (protocol document, "JSON Documents"). */
internal object InstallDocuments {

    /** Keep confirmed item outcomes even when optional platform metadata exceeds the wire budget. */
    fun boundedResult(result: JsonObject): JsonObject {
        if (result.toString().toByteArray(Charsets.UTF_8).size <= InstallerContract.MAX_JSON_BYTES) return result
        val bounded = result.deepCopy()
        val items = bounded.getAsJsonArray(InstallerContract.FIELD_RESULTS)?.map { it.asJsonObject } ?: listOf(bounded)
        fun boundPackage(document: JsonObject) {
            if (document.get(InstallerContract.FIELD_PACKAGE_NAME)?.asString?.length?.let { it > InstallerContract.MAX_INSTALLER_PACKAGE_LENGTH } == true) {
                document.remove(InstallerContract.FIELD_PACKAGE_NAME)
            }
        }
        items.forEach { item ->
            item.remove(InstallerContract.FIELD_VERSION_NAME)
            item.remove(InstallerContract.FIELD_NOTES)
            boundPackage(item)
            if (item.get(InstallerContract.FIELD_OK)?.asBoolean == true) {
                item.add(InstallerContract.FIELD_NOTES, JsonArray().apply { add("Optional result details were omitted to fit the response limit") })
            }
            item.getAsJsonObject(InstallerContract.FIELD_ERROR)?.let { error ->
                error.remove(InstallerContract.FIELD_ERROR_SYSTEM_MESSAGE)
                error.addProperty(InstallerContract.FIELD_ERROR_MESSAGE, "Error details were omitted to fit the response limit")
                boundPackage(error)
            }
        }
        return bounded
    }

    fun installResult(
        packageName: String?,
        versionName: String?,
        versionCode: Long?,
        previousVersionCode: Long?,
        authorizer: String?,
        interaction: String?,
        durationMillis: Long,
        sourceDeleted: Boolean,
        notes: List<String>,
    ): JsonObject = JsonObject().apply {
        addProperty(InstallerContract.FIELD_OK, true)
        packageName?.let { addProperty(InstallerContract.FIELD_PACKAGE_NAME, it) }
        versionName?.let { addProperty(InstallerContract.FIELD_VERSION_NAME, it) }
        versionCode?.let { addProperty(InstallerContract.FIELD_VERSION_CODE, it) }
        previousVersionCode?.let { addProperty(InstallerContract.FIELD_PREVIOUS_VERSION_CODE, it) }
        authorizer?.let { addProperty(InstallerContract.FIELD_AUTHORIZER, it) }
        interaction?.let { addProperty(InstallerContract.FIELD_INTERACTION, it) }
        addProperty(InstallerContract.FIELD_DURATION_MILLIS, durationMillis)
        addProperty(InstallerContract.FIELD_SOURCE_DELETED, sourceDeleted)
        add(InstallerContract.FIELD_NOTES, JsonArray().apply { notes.forEach { add(it) } })
    }

    fun failedItem(failure: InstallFailure, packageName: String?, authorizer: String?, interaction: String?, durationMillis: Long?): JsonObject = JsonObject().apply {
        addProperty(InstallerContract.FIELD_OK, false)
        (failure.packageName ?: packageName)?.let { addProperty(InstallerContract.FIELD_PACKAGE_NAME, it) }
        authorizer?.let { addProperty(InstallerContract.FIELD_AUTHORIZER, it) }
        interaction?.let { addProperty(InstallerContract.FIELD_INTERACTION, it) }
        durationMillis?.let { addProperty(InstallerContract.FIELD_DURATION_MILLIS, it) }
        add(InstallerContract.FIELD_ERROR, failure.withPackage(packageName).toJson())
    }

    fun batch(results: List<JsonObject>): JsonObject = JsonObject().apply {
        add(InstallerContract.FIELD_RESULTS, JsonArray().apply { results.forEach { add(it) } })
    }

    fun stageDetail(index: Int, packageName: String?): JsonObject = JsonObject().apply {
        addProperty(InstallerContract.FIELD_INDEX, index)
        packageName?.let { addProperty(InstallerContract.FIELD_PACKAGE_NAME, it) }
    }

    fun progressDetail(index: Int, bytesWritten: Long, totalBytes: Long): JsonObject = JsonObject().apply {
        addProperty(InstallerContract.FIELD_INDEX, index)
        addProperty(InstallerContract.FIELD_BYTES_WRITTEN, bytesWritten)
        addProperty(InstallerContract.FIELD_TOTAL_BYTES, totalBytes)
    }

    fun sessionStatus(state: String, index: Int, progress: Float): JsonObject = JsonObject().apply {
        addProperty(InstallerContract.FIELD_STATE, state)
        addProperty(InstallerContract.FIELD_STAGE, state)
        addProperty(InstallerContract.FIELD_INDEX, index)
        addProperty(InstallerContract.FIELD_PROGRESS, progress)
    }

    fun authorizerState(state: AuthorizerState): JsonObject = JsonObject().apply {
        addProperty("name", state.authorizer.id)
        addProperty(InstallerContract.FIELD_AVAILABLE, state.available)
        addProperty(InstallerContract.FIELD_RUNNING, state.running)
        addProperty(InstallerContract.FIELD_GRANTED, state.granted)
        state.reason?.let { addProperty(InstallerContract.FIELD_REASON, it) }
    }

    fun granted(granted: Boolean, authorizer: String): JsonObject = JsonObject().apply {
        addProperty(InstallerContract.FIELD_GRANTED, granted)
        addProperty(InstallerContract.FIELD_AUTHORIZER, authorizer)
    }

    fun uninstallResult(packageName: String, authorizer: String): JsonObject = JsonObject().apply {
        addProperty(InstallerContract.FIELD_OK, true)
        addProperty(InstallerContract.FIELD_PACKAGE_NAME, packageName)
        addProperty(InstallerContract.FIELD_AUTHORIZER, authorizer)
    }

    fun users(users: List<DeviceUser>): JsonObject = JsonObject().apply {
        add(InstallerContract.FIELD_USERS, JsonArray().apply {
            users.forEach { user ->
                add(JsonObject().apply {
                    addProperty(InstallerContract.FIELD_USER_ID, user.id)
                    user.name?.let { addProperty(InstallerContract.FIELD_USER_NAME, it) }
                    addProperty(InstallerContract.FIELD_USER_PRIMARY, user.primary)
                    addProperty(InstallerContract.FIELD_USER_RUNNING, user.running)
                })
            }
        })
    }

    fun defaultInstallerState(component: String?, isSelf: Boolean, method: String, requiresClear: Boolean): JsonObject = JsonObject().apply {
        if (component != null) addProperty(InstallerContract.FIELD_COMPONENT, component) else add(InstallerContract.FIELD_COMPONENT, com.google.gson.JsonNull.INSTANCE)
        addProperty(InstallerContract.FIELD_IS_SELF, isSelf)
        addProperty(InstallerContract.FIELD_METHOD, method)
        addProperty(InstallerContract.FIELD_REQUIRES_CLEAR, requiresClear)
    }
}

/** One device user as reported by `getUsers` (protocol "Users request and result"). */
internal data class DeviceUser(val id: Int, val name: String?, val primary: Boolean, val running: Boolean)
