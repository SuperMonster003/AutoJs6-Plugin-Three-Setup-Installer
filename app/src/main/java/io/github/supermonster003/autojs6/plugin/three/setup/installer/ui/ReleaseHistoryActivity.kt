package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import io.github.supermonster003.autojs6.plugin.three.setup.installer.R
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.DocumentText
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.ReleaseHistory
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.SettingsPageActivity
import java.util.concurrent.Executors

/** Bundled documents only. No WebView, active markup, external images or network access. */
class ReleaseHistoryActivity : SettingsPageActivity() {
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "bundled-documents") }
    private var generation = 0
    override val pageTitle get() = when (intent.getStringExtra("document")) {
        "license" -> R.string.settings_license
        "notices" -> R.string.settings_notices
        else -> R.string.release_history_title
    }

    override fun buildPage(content: LinearLayout) {
        val document = intent.getStringExtra("document")
        val expected = ++generation
        val progress = kit.progressBar().apply { isIndeterminate = true }
        content.addView(progress, LinearLayout.LayoutParams(-1, kit.dp(8)))
        val text = kit.text(getString(R.string.default_installer_loading)).apply {
            tag = "document"
            setPaddingRelative(kit.dp(24), kit.dp(16), kit.dp(24), kit.dp(24))
            textDirection = View.TEXT_DIRECTION_LOCALE
            setTextIsSelectable(true)
            setLineSpacing(0f, 1.25f)
        }
        content.addView(text, LinearLayout.LayoutParams(-1, -2))
        val locale = resources.configuration.locales[0]
        worker.execute {
            fun read(path: String): String = assets.open(path).use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 1024 * 1024)
                    output.write(buffer, 0, count)
                }
                output.toString(Charsets.UTF_8.name())
            }
            val loaded = runCatching { when (document) {
                "license" -> read("legal/LICENSE")
                "notices" -> read("legal/THIRD_PARTY_NOTICES.md")
                else -> ReleaseHistory.load(locale, ::read)
            } }.getOrNull()
            runOnUiThread {
                if (isFinishing || isDestroyed || expected != generation) return@runOnUiThread
                progress.visibility = View.GONE
                text.text = if (loaded == null) getString(R.string.release_history_error)
                else if (document == "license") loaded else DocumentText.render(loaded, kit.palette)
            }
        }
    }

    override fun onDestroy() { generation++; worker.shutdownNow(); super.onDestroy() }
}
