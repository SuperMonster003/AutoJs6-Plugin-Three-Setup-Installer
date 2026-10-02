package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import io.github.supermonster003.autojs6.plugin.three.setup.installer.R
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.RequestDocuments
import io.github.supermonster003.autojs6.plugin.three.setup.installer.policy.InstallSafetyPolicy
import io.github.supermonster003.autojs6.plugin.three.setup.installer.policy.InstallSafetyPreferences
import io.github.supermonster003.autojs6.plugin.three.setup.installer.profiles.InstallProfilePreferences
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.*
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.AppearancePreferences
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.HostAppearanceReader
import org.autojs.plugin.installer.api.InstallerContract

/** Every picker edits a local draft; only the explicit confirmation persists a value. */
class SettingsActivity : SettingsPageActivity() {
    override val pageTitle = R.string.settings_title
    private var updates: AppUpdateController? = null
    private var displayedProfileRevision: String? = null

    override fun buildPage(content: LinearLayout) {
        val appearancePreference = AppearancePreferences.read(this)
        val defaults = InstallerPreferences.read(this)
        fun row(title: Int, summary: CharSequence?, icon: Int = R.drawable.ic_settings_tune, tag: String, click: () -> Unit) {
            content.addView(settingsUi.row(title, summary, icon, tag, click))
            content.addView(settingsUi.divider())
        }
        content.addView(settingsUi.group(R.string.settings_appearance))
        row(R.string.app_settings_language, getString(languageLabels[AppearancePreferences.LANGUAGES.indexOf(appearancePreference.language).coerceAtLeast(0)]),
            R.drawable.ic_settings_language, "settings-language") {
            choose(R.string.app_settings_language, languageLabels, AppearancePreferences.LANGUAGES.indexOf(appearancePreference.language)) {
                saveAppearance(appearancePreference.copy(language = AppearancePreferences.LANGUAGES[it]))
            }
        }
        row(R.string.app_settings_dark_mode, getString(modeLabels[AppearancePreferences.DARK_MODES.indexOf(appearancePreference.darkMode).coerceAtLeast(0)]),
            R.drawable.ic_settings_night, "settings-night") {
            choose(R.string.app_settings_dark_mode, modeLabels, AppearancePreferences.DARK_MODES.indexOf(appearancePreference.darkMode)) {
                saveAppearance(appearancePreference.copy(darkMode = AppearancePreferences.DARK_MODES[it]))
            }
        }
        row(R.string.app_settings_theme_color, appearancePreference.color?.let(ThemeColorValue::hex)
            ?: getString(R.string.app_settings_follow_autojs6), R.drawable.ic_settings_theme, "settings-color") {
            prompt = ThemeColorChooser.show(this, appearancePreference.color,
                HostAppearanceReader.cached()?.primary ?: AppearancePreferences.DEFAULT_COLOR,
                ThemeColorChooser.Palette(kit.palette.accent, kit.palette.surface, kit.palette.text, kit.palette.muted, kit.palette.outline),
                ThemeColorChooser.Labels(getString(R.string.app_settings_theme_color), getString(R.string.app_settings_follow_autojs6),
                    getString(R.string.theme_picker_presets), getString(R.string.theme_picker_custom), getString(R.string.theme_picker_input),
                    getString(R.string.theme_picker_invalid), getString(R.string.theme_picker_preview))) {
                saveAppearance(appearancePreference.copy(color = it))
            }
        }
        row(R.string.launcher_icon_title, getString(iconLabels[LauncherIcons.current(this).ordinal]),
            R.drawable.ic_settings_launcher, "settings-icon") {
            prompt = settingsUi.confirmedChoice(R.string.launcher_icon_title, iconLabels.mapIndexed { index, label ->
                SettingsChoice(getString(label), when (LauncherIconMode.entries[index]) {
                    LauncherIconMode.AUTO -> getString(R.string.launcher_icon_auto_note)
                    LauncherIconMode.TRANSPARENT -> getString(R.string.launcher_icon_transparent_note)
                    else -> null
                })
            }, LauncherIcons.current(this).ordinal) { index ->
                runCatching { LauncherIcons.select(this, LauncherIconMode.entries[index]) }.fold(
                    onSuccess = { notify(R.string.launcher_icon_applied_note); renderPage(); true },
                    onFailure = { notify(R.string.launcher_icon_failed); false },
                )
            }
        }
        content.addView(settingsUi.group(R.string.settings_installation))
        row(R.string.settings_authorizer_order, defaults.authorizers.order.filter { it in defaults.authorizers.enabled }
            .joinToString(" / ") { authorizerLabel(it) }, R.drawable.ic_shield, "settings-authorizers") { authorizers(defaults) }
        row(R.string.settings_interaction, getString(interactionLabels[interactions.indexOf(defaults.interaction)]), tag = "settings-interaction") {
            prompt = settingsUi.confirmedChoice(R.string.settings_interaction, interactionLabels.mapIndexed { index, label ->
                SettingsChoice(getString(label), if (index == 0) getString(R.string.settings_interaction_auto_note) else null)
            }, interactions.indexOf(defaults.interaction)) {
                save(defaults.copy(interaction = interactions[it]))
            }
        }
        fun flag(title: Int, value: Boolean, tag: String, update: (Boolean) -> InstallerPreferences) {
            row(title, getString(if (value) R.string.settings_enabled else R.string.settings_disabled), tag = tag) {
                choose(title, listOf(R.string.settings_disabled, R.string.settings_enabled), if (value) 1 else 0) { save(update(it == 1)) }
            }
        }
        flag(R.string.confirm_allow_downgrade, defaults.options.allowDowngrade, "settings-downgrade") { defaults.copy(options = defaults.options.copy(allowDowngrade = it)) }
        flag(R.string.confirm_allow_test, defaults.options.allowTestOnly, "settings-test") { defaults.copy(options = defaults.options.copy(allowTestOnly = it)) }
        flag(R.string.confirm_bypass_target, defaults.options.bypassLowTargetSdk, "settings-low-target") { defaults.copy(options = defaults.options.copy(bypassLowTargetSdk = it)) }
        row(R.string.settings_installer_package, defaults.options.installer ?: getString(R.string.settings_installer_self), tag = "settings-installer") {
            prompt = settingsUi.input(R.string.settings_installer_package, defaults.options.installer.orEmpty(),
                getString(R.string.settings_installer_note), { value ->
                    if (value.trim().isEmpty() || runCatching { RequestDocuments.packageNameOf(value.trim(), "installer", true) }.isSuccess) null
                    else getString(R.string.settings_invalid_package)
                }) { value -> save(defaults.copy(options = defaults.options.copy(installer = value.trim().takeIf(String::isNotEmpty)))) }
        }
        row(R.string.install_target_user, userLabel(defaults.options.user), tag = "settings-user") {
            val selected = when (defaults.options.user) { InstallerContract.USER_CURRENT -> 0; InstallerContract.USER_ALL -> 1; else -> 2 }
            prompt = settingsUi.confirmedChoice(R.string.install_target_user,
                listOf(R.string.install_current_user, R.string.confirm_all_users, R.string.install_user_id).map { SettingsChoice(getString(it)) }, selected) { index ->
                if (index < 2) save(defaults.copy(options = defaults.options.copy(user = if (index == 0) InstallerContract.USER_CURRENT else InstallerContract.USER_ALL)))
                else {
                    // Close the selector before replacing its tracked dialog with the ID draft.
                    prompt?.dismiss()
                    prompt = settingsUi.input(R.string.install_user_id, defaults.options.user.toIntOrNull()?.toString().orEmpty(),
                        getString(R.string.install_privileged_notice), { value ->
                            if (value.trim().toIntOrNull()?.let { it >= 0 } == true) null else getString(R.string.install_invalid_user)
                        }) { value -> save(defaults.copy(options = defaults.options.copy(user = value.trim().toInt().toString()))) }
                    true
                }
            }
        }
        flag(R.string.install_delete_source, defaults.options.deleteSource, "settings-delete-source") { defaults.copy(options = defaults.options.copy(deleteSource = it)) }
        flag(R.string.advanced_grant_permissions, defaults.options.grantAllRequestedPermissions, "settings-grant-permissions") {
            defaults.copy(options = defaults.options.copy(grantAllRequestedPermissions = it))
        }
        flag(R.string.advanced_update_ownership, defaults.options.requestUpdateOwnership, "settings-update-ownership") {
            defaults.copy(options = defaults.options.copy(requestUpdateOwnership = it))
        }
        row(R.string.advanced_dexopt, getString(AdvancedInstallUi.dexopt.first { it.first == defaults.options.dexopt }.second), tag = "settings-dexopt") {
            val choices = AdvancedInstallUi.dexopt
            choose(R.string.advanced_dexopt, choices.map { it.second }, choices.indexOfFirst { it.first == defaults.options.dexopt }) {
                save(defaults.copy(options = defaults.options.copy(dexopt = choices[it].first)))
            }
        }
        row(R.string.advanced_install_reason, getString(AdvancedInstallUi.reasons.first { it.first == defaults.options.installReason }.second), tag = "settings-install-reason") {
            val choices = AdvancedInstallUi.reasons
            choose(R.string.advanced_install_reason, choices.map { it.second }, choices.indexOfFirst { it.first == defaults.options.installReason }) {
                save(defaults.copy(options = defaults.options.copy(installReason = choices[it].first)))
            }
        }
        row(R.string.advanced_package_source, getString(AdvancedInstallUi.sources.first { it.first == defaults.options.packageSource }.second), tag = "settings-package-source") {
            val choices = AdvancedInstallUi.sources
            choose(R.string.advanced_package_source, choices.map { it.second }, choices.indexOfFirst { it.first == defaults.options.packageSource }) {
                save(defaults.copy(options = defaults.options.copy(packageSource = choices[it].first)))
            }
        }
        content.addView(settingsUi.caption(getString(R.string.advanced_sdk_note)))
        content.addView(settingsUi.caption(getString(R.string.settings_defaults_note)))
        val profiles = InstallProfilePreferences.read(this)
        displayedProfileRevision = profiles.revision
        row(R.string.profile_title, if (profiles.readable) getString(R.string.profile_count, profiles.profiles.count { it.enabled }, profiles.profiles.size)
            else getString(R.string.profile_unreadable), tag = "settings-profiles") { open(InstallProfilesActivity::class.java) }
        content.addView(settingsUi.group(R.string.policy_settings_title))
        val policy = InstallSafetyPreferences.read(this)
        row(R.string.policy_packages_title, if (policy.readable) getString(R.string.policy_rule_count, policy.packages.size)
            else getString(R.string.policy_unreadable), R.drawable.ic_shield, "settings-package-blacklist") { editPolicy(false) }
        row(R.string.policy_shared_users_title, if (policy.readable) getString(R.string.policy_rule_count, policy.sharedUsers.size)
            else getString(R.string.policy_unreadable), R.drawable.ic_shield, "settings-shared-user-blacklist") { editPolicy(true) }
        content.addView(settingsUi.caption(getString(R.string.policy_settings_note)))
        content.addView(settingsUi.group(R.string.settings_notifications))
        flag(R.string.settings_progress_notifications, defaults.progressNotifications, "settings-notifications") { defaults.copy(progressNotifications = it) }
        content.addView(settingsUi.caption(getString(R.string.settings_notifications_note)))
        content.addView(settingsUi.group(R.string.ui_about))
        row(R.string.default_installer_title, getString(R.string.default_installer_summary), R.drawable.ic_shield, "settings-default") { open(DefaultInstallerActivity::class.java) }
        row(R.string.ui_about, getString(R.string.app_name), R.drawable.ic_info, "settings-about") { open(AboutActivity::class.java) }
        row(R.string.release_history_title, null, R.drawable.ic_article, "settings-history") { open(ReleaseHistoryActivity::class.java) }
        row(R.string.app_update_check, getString(R.string.settings_update_manual_note), R.drawable.ic_download, "settings-update") {
            updates?.close()
            updates = AppUpdateController(this, settingsUi).also { it.check() }
        }
        row(R.string.app_update_manage_ignored, null, R.drawable.ic_download, "settings-ignored") {
            updates?.close()
            updates = AppUpdateController(this, settingsUi).also { it.manageIgnored() }
        }
    }

    override fun onResume() {
        super.onResume(); LauncherIcons.prepare(this)
        if (displayedProfileRevision != InstallProfilePreferences.read(this).revision) renderPage()
    }
    override fun onPageAppearanceChanged() { updates?.close(); updates = null }
    override fun onStop() { updates?.close(); updates = null; super.onStop() }

    private fun choose(title: Int, labels: List<Int>, current: Int, save: (Int) -> Boolean) {
        prompt = settingsUi.confirmedChoice(title, labels.map { SettingsChoice(getString(it)) }, current, save)
    }
    private fun save(value: InstallerPreferences): Boolean = runCatching { value.save(this) }.getOrDefault(false).also {
        if (it) renderPage() else notify(R.string.settings_error)
    }
    private fun editPolicy(sharedUser: Boolean) {
        val current = InstallSafetyPreferences.read(this)
        fun save(packages: Set<String>, sharedUsers: Set<String>): Boolean = runCatching {
            InstallSafetyPreferences.save(this, current, packages, sharedUsers)
        }.getOrDefault(false).also { if (it) renderPage() else notify(R.string.settings_error) }
        if (!current.readable) {
            prompt = settingsUi.confirmedChoice(R.string.policy_settings_title,
                listOf(SettingsChoice(getString(R.string.policy_reset), getString(R.string.policy_unreadable))), 0) {
                save(emptySet(), emptySet())
            }
            return
        }
        val selected = if (sharedUser) current.sharedUsers else current.packages
        val otherCount = if (sharedUser) current.packages.size else current.sharedUsers.size
        prompt = settingsUi.input(if (sharedUser) R.string.policy_shared_users_title else R.string.policy_packages_title,
            selected.sorted().joinToString("\n"), getString(R.string.policy_editor_note, InstallSafetyPolicy.MAX_RULES),
            validate = { value ->
                val rules = runCatching { InstallSafetyPolicy.parseLines(value) }.getOrNull()
                if (rules == null || rules.size + otherCount > InstallSafetyPolicy.MAX_RULES) getString(R.string.policy_invalid_rules) else null
            }, multiline = true, maxLength = InstallSafetyPolicy.MAX_EDITOR_LENGTH + 1) { value ->
                val rules = InstallSafetyPolicy.parseLines(value)
                if (sharedUser) save(current.packages, rules) else save(rules, current.sharedUsers)
            }
    }
    private fun saveAppearance(value: AppearancePreferences): Boolean = runCatching { value.save(this) }.getOrDefault(false).also {
        if (it) recreate() else notify(R.string.settings_error)
    }
    private fun userLabel(user: String) = when (user) {
        InstallerContract.USER_CURRENT -> getString(R.string.install_current_user)
        InstallerContract.USER_ALL -> getString(R.string.confirm_all_users)
        else -> user
    }
    private fun authorizerLabel(value: Authorizer) = when (value) {
        Authorizer.SHIZUKU -> getString(R.string.settings_shizuku)
        Authorizer.ROOT -> getString(R.string.settings_root)
        Authorizer.NONE -> getString(R.string.confirm_system)
        Authorizer.DHIZUKU -> getString(R.string.settings_dhizuku)
    }
    private fun open(type: Class<*>) = startActivity(Intent(this, type))

    private fun authorizers(preferences: InstallerPreferences) {
        val order = preferences.authorizers.order.toMutableList()
        val enabled = preferences.authorizers.enabled.toMutableSet()
        prompt = settingsUi.dialog(getString(R.string.settings_authorizer_order)) { layout, dialog ->
            layout.content.addView(kit.text(getString(R.string.settings_authorizers_note), 14f, kit.palette.muted))
            val rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            layout.content.addView(rows, LinearLayout.LayoutParams(-1, -2))
            val confirm = kit.textButton(getString(R.string.settings_confirm), "settings-confirm") {
                if (enabled.isNotEmpty() && save(preferences.copy(authorizers = AuthorizerPreferences(order.toList(), enabled.toSet())))) dialog.dismiss()
            }
            fun render() {
                rows.removeAllViews()
                order.forEachIndexed { index, authorizer ->
                    val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                    row.addView(kit.checkBox(authorizerLabel(authorizer), authorizer in enabled, "authorizer-${authorizer.id}") {
                        if (it) enabled += authorizer else enabled -= authorizer
                        confirm.isEnabled = enabled.isNotEmpty()
                    }, LinearLayout.LayoutParams(-1, -2))
                    val moves = LinearLayout(this).apply { gravity = android.view.Gravity.END }
                    moves.addView(kit.textButton(getString(R.string.settings_move_up), "authorizer-up-${authorizer.id}") {
                        java.util.Collections.swap(order, index, index - 1); render()
                    }.apply { isEnabled = index > 0 })
                    moves.addView(kit.textButton(getString(R.string.settings_move_down), "authorizer-down-${authorizer.id}") {
                        java.util.Collections.swap(order, index, index + 1); render()
                    }.apply { isEnabled = index < order.lastIndex })
                    row.addView(moves, LinearLayout.LayoutParams(-1, -2))
                    rows.addView(row, LinearLayout.LayoutParams(-1, -2))
                }
            }
            render()
            layout.actions.addView(kit.textButton(getString(R.string.action_cancel), "settings-cancel") { dialog.cancel() })
            layout.actions.addView(confirm)
        }
    }

    companion object {
        internal val languageLabels = listOf(R.string.app_settings_follow_autojs6, R.string.app_settings_follow_system,
            R.string.app_language_zh_hans, R.string.app_language_zh_hant_hk, R.string.app_language_zh_hant_tw, R.string.app_language_en,
            R.string.app_language_fr, R.string.app_language_es, R.string.app_language_ja, R.string.app_language_ko, R.string.app_language_ru, R.string.app_language_ar)
        internal val modeLabels = listOf(R.string.app_settings_follow_autojs6, R.string.app_settings_follow_system,
            R.string.app_settings_always_light, R.string.app_settings_always_dark)
        internal val iconLabels = listOf(R.string.launcher_icon_light, R.string.launcher_icon_dark, R.string.launcher_icon_auto, R.string.launcher_icon_transparent)
        private val interactions = listOf(InstallerContract.INTERACTION_AUTO, InstallerContract.INTERACTION_DIALOG, InstallerContract.INTERACTION_SILENT, InstallerContract.INTERACTION_NOTIFICATION)
        private val interactionLabels = listOf(R.string.install_authorizer_auto, R.string.settings_interaction_dialog, R.string.settings_interaction_silent, R.string.settings_interaction_notification)
    }
}

/** Permission-protected host entry forwards to the same standalone settings screen. */
class InstallerSettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, SettingsActivity::class.java))
        finish()
    }
}
