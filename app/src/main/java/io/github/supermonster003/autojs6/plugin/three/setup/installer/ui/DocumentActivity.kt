package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.LinearLayout
import io.github.supermonster003.autojs6.plugin.three.setup.installer.R
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.DocumentText
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.SettingsPageActivity
import java.util.concurrent.Executors

/**
 * The bundled license and third-party notices (the About page). Bundled documents only: no WebView, active markup,
 * external images or network access. The release history has its own page ([ReleaseHistoryActivity]).
 */
class DocumentActivity : SettingsPageActivity() {
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "bundled-documents") }
    private var generation = 0
    private val license get() = intent.getStringExtra(EXTRA_DOCUMENT) == LICENSE
    override val pageTitle get() = if (license) R.string.settings_license else R.string.settings_notices

    override fun buildPage(content: LinearLayout) {
        val license = license
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
            val loaded = runCatching { read(if (license) "legal/LICENSE" else "legal/THIRD_PARTY_NOTICES.md") }.getOrNull()
            runOnUiThread {
                if (isFinishing || isDestroyed || expected != generation) return@runOnUiThread
                progress.visibility = View.GONE
                text.text = if (loaded == null) getString(R.string.release_history_error)
                else if (license) loaded else DocumentText.render(loaded, kit.palette)
            }
        }
    }

    override fun onDestroy() { generation++; worker.shutdownNow(); super.onDestroy() }

    companion object {
        private const val EXTRA_DOCUMENT = "document"
        const val LICENSE = "license"
        const val NOTICES = "notices"

        /** [LICENSE] or [NOTICES]; anything else shows the notices. */
        fun open(context: Context, document: String) =
            context.startActivity(Intent(context, DocumentActivity::class.java).putExtra(EXTRA_DOCUMENT, document))
    }
}
