package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import io.github.supermonster003.autojs6.plugin.three.setup.installer.BuildConfig
import io.github.supermonster003.autojs6.plugin.three.setup.installer.R
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.AppUpdateController
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.ReleaseInfoCodec
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.SettingsPageActivity

class AboutActivity : SettingsPageActivity() {
    override val pageTitle = R.string.ui_about
    private var updates: AppUpdateController? = null

    override fun buildPage(content: LinearLayout) {
        content.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPaddingRelative(kit.dp(24), kit.dp(24), kit.dp(24), kit.dp(24))
            addView(ImageView(this@AboutActivity).apply {
                tag = "about-icon"
                setImageResource(R.mipmap.ic_launcher)
                background = kit.roundedFill(Color.TRANSPARENT, 24).apply { setStroke(kit.dp(1), kit.palette.outline) }
                clipToOutline = true
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(kit.dp(88), kit.dp(88)))
            addView(kit.text(getString(R.string.app_name), 24f, medium = true).apply {
                gravity = Gravity.CENTER
                setPaddingRelative(0, kit.dp(16), 0, kit.dp(8))
            }, LinearLayout.LayoutParams(-1, -2))
            addView(kit.text(getString(R.string.plugin_description), 14f, kit.palette.muted).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, -2))
        })
        content.addView(settingsUi.row(R.string.about_version, getString(R.string.about_version_value, BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE, getString(R.string.plugin_version_date)), R.drawable.ic_info, "about-version"))
        content.addView(settingsUi.row(R.string.about_developer, getString(R.string.plugin_author), R.drawable.ic_person, "about-author"))
        fun link(title: Int, icon: Int, tag: String, click: () -> Unit) {
            content.addView(settingsUi.row(title, null, icon, tag, click))
            content.addView(settingsUi.divider())
        }
        link(R.string.settings_source, R.drawable.ic_code, "about-source") { AppUpdateController.openPage(this, ReleaseInfoCodec.SOURCE) }
        link(R.string.about_developer_page, R.drawable.ic_person, "about-developer") { AppUpdateController.openPage(this, AppUpdateController.DEVELOPER_PAGE) }
        link(R.string.settings_license, R.drawable.ic_description, "about-license") { document("license") }
        link(R.string.settings_notices, R.drawable.ic_description, "about-notices") { document("notices") }
        link(R.string.release_history_title, R.drawable.ic_article, "about-history") { document("history") }
        link(R.string.app_update_check, R.drawable.ic_download, "about-update") {
            updates?.close()
            updates = AppUpdateController(this, settingsUi).also { it.check() }
        }
    }

    override fun onPageAppearanceChanged() { updates?.close(); updates = null }
    override fun onStop() { updates?.close(); updates = null; super.onStop() }
    private fun document(name: String) = startActivity(Intent(this, ReleaseHistoryActivity::class.java).putExtra("document", name))
}
