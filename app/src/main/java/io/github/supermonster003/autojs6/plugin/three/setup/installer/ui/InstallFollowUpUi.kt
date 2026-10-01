package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.content.Context
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.R
import org.autojs.plugin.installer.api.InstallerContract as C

/** Optional post-install outcomes never relabel a successfully installed item as a failure. */
internal object InstallFollowUpUi {
    fun summary(context: Context, result: JsonObject?): List<String> = buildList {
        if (result?.get(C.FIELD_OK)?.asBoolean != true) return@buildList
        result.getAsJsonObject(C.FIELD_DEXOPT)?.let { optimization ->
            val status = when (optimization.get(C.FIELD_DEXOPT_STATUS)?.asString) {
                C.DEXOPT_STATUS_ACCEPTED -> R.string.advanced_dexopt_accepted
                C.DEXOPT_STATUS_FAILED -> R.string.advanced_dexopt_failed
                C.DEXOPT_STATUS_CANCELLED -> R.string.advanced_dexopt_cancelled
                C.DEXOPT_STATUS_TIMEOUT -> R.string.advanced_dexopt_timeout
                C.DEXOPT_STATUS_UNAVAILABLE -> R.string.advanced_dexopt_unavailable
                else -> R.string.advanced_dexopt_unknown
            }
            add(context.getString(R.string.advanced_dexopt_result, optimization.get(C.FIELD_FILTER)?.asString.orEmpty(), context.getString(status)))
            add(context.getString(R.string.install_optimizing_note))
        }
        result.get(C.FIELD_UPDATE_OWNER)?.let { owner ->
            add(if (owner.isJsonNull) context.getString(R.string.advanced_update_owner_none)
                else context.getString(R.string.advanced_update_owner_result, owner.asString))
        }
    }
}
