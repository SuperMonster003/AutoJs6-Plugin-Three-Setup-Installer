package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.os.Bundle
import android.widget.LinearLayout
import io.github.supermonster003.autojs6.plugin.three.setup.installer.R
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.DefaultInstallerController
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.DefaultInstallerState
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.SettingsPageActivity
import org.autojs.plugin.installer.api.InstallerContract

class DefaultInstallerActivity : SettingsPageActivity() {
    override val pageTitle = R.string.default_installer_title
    private var state: DefaultInstallerState? = null
    private var busy = false
    private var controller: DefaultInstallerController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = DefaultInstallerController(this, { state = it; renderPage() }, ::notify, { busy = it; renderPage() })
    }

    override fun onResume() { super.onResume(); controller?.refresh() }
    override fun onPageAppearanceChanged() { controller?.dismissDialogs() }
    override fun onDestroy() { controller?.close(); controller = null; super.onDestroy() }

    override fun buildPage(content: LinearLayout) {
        content.addView(settingsUi.caption(getString(R.string.default_installer_summary)))
        content.addView(settingsUi.row(R.string.default_installer_current, state?.component ?: getString(R.string.default_installer_none),
            R.drawable.ic_settings_launcher, "default-current"))
        content.addView(settingsUi.row(R.string.default_installer_state,
            getString(when { state == null -> R.string.default_installer_loading; state?.isSelf == true -> R.string.default_installer_self; else -> R.string.default_installer_other }),
            R.drawable.ic_shield, "default-state"))
        content.addView(settingsUi.row(R.string.default_installer_method,
            getString(if (state?.persistentConfigured == true) R.string.default_installer_persistent else when (state?.method) {
                InstallerContract.DEFAULT_METHOD_PREFERRED -> R.string.default_installer_preferred; else -> R.string.default_installer_resolved }),
            R.drawable.ic_info, "default-method"))
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPaddingRelative(kit.dp(24), kit.dp(12), kit.dp(24), kit.dp(12))
        }
        actions.addView(kit.textButton(getString(R.string.default_installer_set), "default-set") { controller?.setDefault(true) }.apply { isEnabled = !busy && state != null && state?.isSelf != true })
        actions.addView(kit.textButton(getString(R.string.default_installer_clear), "default-clear") { controller?.setDefault(false) }
            .apply { isEnabled = !busy && state?.isSelf == true && state?.method == InstallerContract.DEFAULT_METHOD_PREFERRED })
        actions.addView(kit.textButton(getString(R.string.default_installer_open_settings), "default-system-settings") { controller?.openSystemSettings() })
        actions.addView(kit.textButton(getString(R.string.default_installer_set_persistent), "default-persistent-set") { controller?.setDefault(true, persistent = true) }
            .apply { isEnabled = !busy && state != null })
        actions.addView(kit.textButton(getString(R.string.default_installer_clear_persistent), "default-persistent-clear") { controller?.setDefault(false, persistent = true) }
            .apply { isEnabled = !busy && state != null })
        content.addView(actions)
        if (busy) content.addView(kit.progressBar().apply { isIndeterminate = true })
        content.addView(settingsUi.caption(getString(if (state?.requiresClear == true) R.string.default_installer_requires_clear else R.string.default_installer_guide)))
        content.addView(settingsUi.caption(getString(R.string.default_installer_oem_note)))
        content.addView(settingsUi.caption(getString(R.string.default_installer_persistent_note)))
    }
}
