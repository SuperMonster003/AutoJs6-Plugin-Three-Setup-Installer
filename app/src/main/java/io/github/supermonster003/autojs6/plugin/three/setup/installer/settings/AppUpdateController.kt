package io.github.supermonster003.autojs6.plugin.three.setup.installer.settings

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.LinearLayout
import android.widget.Toast
import io.github.supermonster003.autojs6.plugin.three.setup.installer.BuildConfig
import io.github.supermonster003.autojs6.plugin.three.setup.installer.R
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.ReleaseHistoryActivity
import java.io.Closeable
import java.util.concurrent.Executors

/** No background checks. Every network operation starts at the user's Check for updates action. */
internal class AppUpdateController(private val activity: SettingsPageActivity, private val ui: SettingsUi) : Closeable {
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "manual-release-check") }
    private val preferences = activity.getSharedPreferences("installer_updates", Context.MODE_PRIVATE)
    private var cancellation: UpdateCancellation? = null
    private var prompt: Dialog? = null
    @Volatile private var closed = false
    private var generation = 0

    fun check() {
        val now = System.currentTimeMillis()
        val previous = preferences.getLong("last_attempt", 0L).takeIf { it > 0 }
        if (!UpdateSchedulePolicy.due(previous, now)) {
            val cached = preferences.getString("release", null)
            if (cached != null) {
                val cachedResult = if (cached == "none") UpdateResult.Success(null)
                else runCatching { UpdateResult.Success(ReleaseInfoCodec.decode(cached)) }.getOrNull()
                if (cachedResult != null) { result(cachedResult, cached = true); return }
            }
            prompt = ui.message(R.string.app_update_check, activity.getString(R.string.settings_update_limited))
            return
        }
        val expected = ++generation
        val token = UpdateCancellation().also { cancellation = it }
        prompt = ui.dialog(activity.getString(R.string.app_update_check)) { layout, dialog ->
            layout.content.addView(ui.kit.text(activity.getString(R.string.app_update_checking), 14f, ui.kit.palette.muted))
            layout.content.addView(ui.kit.progressBar().apply { isIndeterminate = true }, LinearLayout.LayoutParams(-1, ui.dp(8)).apply { topMargin = ui.dp(16) })
            layout.actions.addView(ui.kit.textButton(activity.getString(R.string.action_cancel), "update-cancel") { dialog.cancel() })
            dialog.setOnCancelListener { generation++; token.cancel() }
        }
        worker.execute {
            preferences.edit().putLong("last_attempt", now).remove("release").commit()
            val response = AppUpdateRepository().fetchLatest(token)
            if (!token.cancelled && response is UpdateResult.Success) {
                preferences.edit().putString("release", response.release?.let(ReleaseInfoCodec::encode) ?: "none").commit()
            }
            activity.runOnUiThread {
                if (closed || token.cancelled || generation != expected || activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                prompt?.dismiss()
                result(response, cached = false)
            }
        }
    }

    private fun result(value: UpdateResult, cached: Boolean) {
        val release = (value as? UpdateResult.Success)?.release
        val ignored = preferences.getStringSet("ignored", emptySet()).orEmpty()
        val newer = release != null && AppVersionPolicy.isNewer(release.tag, BuildConfig.VERSION_NAME)
        val hidden = release != null && UpdateSchedulePolicy.ignored(release.tag, ignored)
        val message = when {
            value is UpdateResult.Failure -> activity.getString(R.string.settings_update_failed)
            release == null -> activity.getString(R.string.settings_update_unpublished)
            hidden -> activity.getString(R.string.settings_update_ignored, release.tag)
            newer -> activity.getString(R.string.settings_update_available, release.tag)
            else -> activity.getString(R.string.settings_update_current)
        }
        prompt = ui.dialog(activity.getString(R.string.app_update_check)) { layout, dialog ->
            layout.content.addView(ui.kit.text(message))
            if (cached) layout.content.addView(ui.kit.text(activity.getString(R.string.settings_update_cached), 14f, ui.kit.palette.muted))
            if (newer && !hidden && release != null && release.notes.isNotBlank()) {
                layout.content.addView(ui.kit.text(release.notes, 14f, ui.kit.palette.muted))
            }
            if (newer && !hidden && release != null) {
                layout.actions.addView(ui.kit.textButton(activity.getString(R.string.settings_update_ignore), "update-ignore") {
                    if (preferences.edit().putStringSet("ignored", ignored + release.tag).commit()) dialog.dismiss()
                    else Toast.makeText(activity, R.string.settings_error, Toast.LENGTH_LONG).show()
                })
            }
            layout.actions.addView(ui.kit.textButton(activity.getString(R.string.release_history_title), "update-history") {
                dialog.dismiss(); activity.startActivity(Intent(activity, ReleaseHistoryActivity::class.java))
            })
            if (release != null) layout.actions.addView(ui.kit.textButton(activity.getString(R.string.settings_update_open_release), "update-open") {
                dialog.dismiss(); openPage(activity, release.url)
            })
            else layout.actions.addView(ui.kit.textButton(activity.getString(R.string.settings_confirm), "update-done") { dialog.dismiss() })
        }
    }

    fun manageIgnored() {
        val ignored = preferences.getStringSet("ignored", emptySet()).orEmpty().sorted()
        if (ignored.isEmpty()) {
            prompt = ui.message(R.string.app_update_manage_ignored, activity.getString(R.string.app_update_no_ignored)); return
        }
        val remove = mutableSetOf<String>()
        prompt = ui.dialog(activity.getString(R.string.app_update_manage_ignored)) { layout, dialog ->
            ignored.forEach { tag -> layout.content.addView(ui.kit.checkBox(tag, false) { if (it) remove += tag else remove -= tag }) }
            layout.actions.addView(ui.kit.textButton(activity.getString(R.string.action_cancel)) { dialog.cancel() })
            layout.actions.addView(ui.kit.textButton(activity.getString(R.string.app_update_stop_ignoring)) {
                if (preferences.edit().putStringSet("ignored", ignored.toSet() - remove).commit()) dialog.dismiss()
                else Toast.makeText(activity, R.string.settings_error, Toast.LENGTH_LONG).show()
            })
        }
    }

    override fun close() {
        closed = true
        generation++
        cancellation?.cancel()
        prompt?.dismiss()
        worker.shutdownNow()
    }

    companion object {
        const val DEVELOPER_PAGE = "https://github.com/SuperMonster003"
        fun openPage(context: Context, url: String) {
            val valid = url == ReleaseInfoCodec.SOURCE || url == DEVELOPER_PAGE || runCatching {
                val tag = Uri.parse(url).lastPathSegment ?: return@runCatching false
                AppVersionPolicy.parse(tag) != null && ReleaseInfoCodec.validUrl(url, tag)
            }.getOrDefault(false)
            if (!valid || runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)) }.isFailure) {
                Toast.makeText(context, R.string.settings_link_unavailable, Toast.LENGTH_LONG).show()
            }
        }
    }
}
