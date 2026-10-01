package io.github.supermonster003.autojs6.plugin.three.setup.installer.history

import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallRequest
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes

internal object InstallHistoryCapture {
    fun capture(token: String, request: InstallRequest, state: InstallPresentation.Snapshot,
        startedAt: Long, now: Long): List<InstallHistoryEntry> = state.items.mapIndexed { index, item ->
        val result = item.result
        val ok = result?.get(InstallerContract.FIELD_OK)?.asBoolean
        val error = result?.getAsJsonObject(InstallerContract.FIELD_ERROR)
        val errorCode = error?.get(InstallerContract.FIELD_ERROR_CODE)?.asString
        val stage = when {
            ok == true -> InstallerContract.STAGE_COMPLETED
            errorCode in setOf(InstallerErrorCodes.CANCELLED, InstallerErrorCodes.USER_CANCELLED) -> InstallerContract.STAGE_CANCELLED
            ok == false -> InstallerContract.STAGE_FAILED
            state.terminal -> InstallerContract.STAGE_CANCELLED
            else -> item.stage
        }
        val terminal = stage in InstallerContract.TERMINAL_STAGES
        InstallHistoryEntry(InstallHistoryEntry.id(token, index), token, index,
            InstallHistoryEntry.text(item.metadata?.label ?: item.displayName, 256).orEmpty(),
            InstallHistoryEntry.packageName(result?.get(InstallerContract.FIELD_PACKAGE_NAME)?.asString ?: item.metadata?.packageName),
            InstallHistoryEntry.text(result?.get(InstallerContract.FIELD_VERSION_NAME)?.asString ?: item.metadata?.versionName, 128),
            result?.get(InstallerContract.FIELD_VERSION_CODE)?.asLong ?: item.metadata?.versionCode,
            InstallHistoryEntry.text(item.metadata?.previousVersion?.name, 128),
            result?.get(InstallerContract.FIELD_PREVIOUS_VERSION_CODE)?.asLong ?: item.metadata?.previousVersion?.code,
            stage, startedAt, now.coerceAtLeast(startedAt), if (terminal) now.coerceAtLeast(startedAt) else null, request.origin,
            result?.get(InstallerContract.FIELD_AUTHORIZER)?.asString ?: (item.options ?: request.options).authorizer,
            if (stage == InstallerContract.STAGE_CANCELLED) errorCode ?: InstallerErrorCodes.CANCELLED else errorCode,
            InstallHistoryEntry.text(error?.get(InstallerContract.FIELD_ERROR_SYSTEM_MESSAGE)?.asString, 1024),
            result?.get(InstallerContract.FIELD_DURATION_MILLIS)?.asLong?.coerceAtLeast(0))
    }
}
