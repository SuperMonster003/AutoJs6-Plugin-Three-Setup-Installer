package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import io.github.supermonster003.autojs6.plugin.three.setup.installer.R
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.RequestDocuments
import io.github.supermonster003.autojs6.plugin.three.setup.installer.profiles.*
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.SettingsChoice
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.SettingsUi
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.HostAppearanceActivity
import org.autojs.plugin.installer.api.InstallerContract as C

/** Three draft levels (field, profile, list), with exactly one explicit CAS at list Save. */
class InstallProfilesActivity : HostAppearanceActivity() {
    override val dialogTheme = false
    private lateinit var draft: Draft
    private lateinit var ui: SettingsUi
    private var prompt: Dialog? = null
    private var scroll: NestedScrollView? = null

    private class Draft : ViewModel() {
        var snapshot: InstallProfileSnapshot? = null
        var profiles = emptyList<InstallProfile>()
        var editor: InstallProfile? = null
        var resetRequested = false
        var notice: Int? = null
        var saving = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        draft = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                check(modelClass == Draft::class.java)
                return Draft() as T
            }
        })[Draft::class.java]
        if (draft.snapshot == null) load()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = back()
        })
        render(resetScroll = true)
    }

    override fun onAppearanceChanged() {
        prompt?.dismiss(); prompt = null
        if (::draft.isInitialized) render()
    }

    override fun onStop() {
        prompt?.dismiss(); prompt = null
        super.onStop()
    }

    private fun load() {
        val loaded = InstallProfilePreferences.read(this)
        draft.snapshot = loaded
        draft.profiles = loaded.profiles.toList()
        draft.editor = null
        draft.resetRequested = false
        draft.notice = null
    }

    private fun back() {
        if (draft.editor == null) finish()
        else { draft.editor = null; render(resetScroll = true) }
    }

    private fun render(resetScroll: Boolean = false) {
        if (isFinishing || isDestroyed) return
        val position = if (resetScroll) 0 else scroll?.scrollY ?: 0
        ui = SettingsUi(this, kit)
        val editor = draft.editor
        val page = ui.page(getString(if (editor == null) R.string.profile_title else R.string.profile_edit_title), ::back)
        scroll = page.content.parent as NestedScrollView
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPaddingRelative(kit.dp(12), kit.dp(8), kit.dp(12), kit.dp(8))
        }
        page.root.addView(actions, LinearLayout.LayoutParams(-1, -2))
        if (editor == null) list(page.content, actions) else edit(page.content, actions, editor)
        for (index in 0 until actions.childCount) {
            actions.getChildAt(index).layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        setContentView(page.root)
        scroll?.post { scroll?.scrollTo(0, position) }
    }

    private fun list(content: LinearLayout, actions: LinearLayout) {
        val snapshot = requireNotNull(draft.snapshot)
        content.addView(ui.caption(getString(R.string.profile_priority_note)))
        if (!snapshot.readable) {
            content.addView(ui.caption(getString(R.string.profile_unreadable)))
            content.addView(ui.row(R.string.profile_reset, getString(R.string.profile_reset_note), tag = "profiles-reset") {
                prompt = ui.confirmedChoice(R.string.profile_reset, listOf(SettingsChoice(getString(R.string.profile_reset), getString(R.string.profile_reset_note))), 0) {
                    draft.profiles = emptyList(); draft.resetRequested = true; draft.notice = R.string.profile_unsaved; render(); true
                }
            })
        } else {
            if (draft.profiles.isEmpty()) content.addView(ui.caption(getString(R.string.profile_empty)))
            draft.profiles.forEachIndexed { index, profile ->
                val summary = getString(R.string.profile_summary, getString(sourceLabel(profile.source)),
                    profile.packagePrefix.ifEmpty { getString(R.string.profile_any_package) }, profile.overrides.keys.size)
                content.addView(ui.row(profile.name, summary, R.drawable.ic_settings_tune, "profile-edit-${profile.id}") {
                    draft.editor = draft.profiles.firstOrNull { it.id == profile.id }?.copy()
                    render(resetScroll = true)
                })
                val controls = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPaddingRelative(kit.dp(64), 0, kit.dp(24), kit.dp(12))
                }
                controls.addView(kit.checkBox(getString(R.string.profile_enabled), profile.enabled, "profile-enabled-${profile.id}") { enabled ->
                    changeList { values -> values.map { if (it.id == profile.id) it.copy(enabled = enabled) else it } }
                })
                val moves = LinearLayout(this).apply { gravity = Gravity.END }
                moves.addView(kit.textButton(getString(R.string.settings_move_up), "profile-up-${profile.id}") { move(profile.id, -1) }.apply { isEnabled = index > 0 })
                moves.addView(kit.textButton(getString(R.string.settings_move_down), "profile-down-${profile.id}") { move(profile.id, 1) }.apply { isEnabled = index < draft.profiles.lastIndex })
                for (child in 0 until moves.childCount) {
                    moves.getChildAt(child).layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                }
                controls.addView(moves)
                controls.addView(kit.textButton(getString(R.string.profile_delete), "profile-delete-${profile.id}") {
                    changeList { values -> values.filterNot { it.id == profile.id } }
                })
                content.addView(controls)
                content.addView(ui.divider())
            }
        }
        draft.notice?.let { content.addView(ui.caption(getString(it))) }
        if (draft.notice == R.string.profile_conflict || !snapshot.readable) {
            content.addView(ui.row(R.string.profile_reload, getString(R.string.profile_reload_note), tag = "profiles-reload") {
                prompt = ui.confirmedChoice(R.string.profile_reload, listOf(SettingsChoice(getString(R.string.profile_reload), getString(R.string.profile_reload_note))), 0) {
                    load(); render(resetScroll = true); true
                }
            })
        }
        actions.addView(kit.textButton(getString(R.string.profile_add), "profiles-add") {
            if (draft.profiles.size < InstallProfileRules.MAX_PROFILES) {
                draft.editor = InstallProfile(name = getString(R.string.profile_new_name))
                render(resetScroll = true)
            }
        }.apply { isEnabled = snapshot.readable && draft.profiles.size < InstallProfileRules.MAX_PROFILES && !draft.saving })
        actions.addView(kit.textButton(getString(R.string.action_cancel), "profiles-cancel") { finish() })
        actions.addView(kit.textButton(getString(R.string.profile_save), "profiles-save", onClick = ::save).apply {
            isEnabled = !draft.saving && (snapshot.readable || draft.resetRequested) &&
                (draft.resetRequested || draft.profiles != snapshot.profiles)
        })
    }

    private fun changeList(transform: (List<InstallProfile>) -> List<InstallProfile>) {
        draft.profiles = transform(draft.profiles).toList()
        draft.notice = R.string.profile_unsaved
        render()
    }

    private fun move(id: String, delta: Int) = changeList { profiles ->
        val index = profiles.indexOfFirst { it.id == id }
        if (index < 0 || index + delta !in profiles.indices) profiles else profiles.toMutableList().apply {
            java.util.Collections.swap(this, index, index + delta)
        }
    }

    private fun save() {
        if (draft.saving || draft.editor != null) return
        draft.saving = true
        val result = try { InstallProfilePreferences.save(this, requireNotNull(draft.snapshot), draft.profiles.toList()) }
            finally { draft.saving = false }
        when (result) {
            InstallProfileSaveResult.SAVED -> { load(); draft.notice = R.string.profile_saved }
            InstallProfileSaveResult.CONFLICT -> draft.notice = R.string.profile_conflict
            InstallProfileSaveResult.INVALID -> draft.notice = R.string.profile_invalid
            InstallProfileSaveResult.FAILED -> draft.notice = R.string.profile_save_failed
        }
        render()
    }

    private fun edit(content: LinearLayout, actions: LinearLayout, profile: InstallProfile) {
        fun row(title: Int, summary: String, tag: String, click: () -> Unit) {
            content.addView(ui.row(title, summary, tag = tag, click = click)); content.addView(ui.divider())
        }
        content.addView(ui.caption(getString(R.string.profile_draft_note)))
        content.addView(ui.group(R.string.profile_conditions))
        row(R.string.profile_name, profile.name, "profile-name") {
            prompt = ui.input(R.string.profile_name, profile.name, getString(R.string.profile_name_note, InstallProfileRules.MAX_NAME_LENGTH),
                { value -> if (InstallProfileRules.validName(value.trim())) null else getString(R.string.profile_invalid_name) },
                maxLength = InstallProfileRules.MAX_NAME_LENGTH + 1) { value -> update { it.copy(name = value.trim()) }; true }
        }
        row(R.string.profile_source, getString(sourceLabel(profile.source)), "profile-source") {
            prompt = ui.confirmedChoice(R.string.profile_source, InstallProfileSources.ALL.map { SettingsChoice(getString(sourceLabel(it))) },
                InstallProfileSources.ALL.indexOf(profile.source)) { index -> update { it.copy(source = InstallProfileSources.ALL[index]) }; true }
        }
        row(R.string.profile_prefix, profile.packagePrefix.ifEmpty { getString(R.string.profile_any_package) }, "profile-prefix") {
            prompt = ui.input(R.string.profile_prefix, profile.packagePrefix, getString(R.string.profile_prefix_note),
                { value -> if (InstallProfileRules.validPrefix(value.trim())) null else getString(R.string.profile_invalid_prefix) },
                maxLength = InstallProfileRules.MAX_PREFIX_LENGTH + 1) { value -> update { it.copy(packagePrefix = value.trim()) }; true }
        }
        content.addView(ui.group(R.string.profile_overrides))
        content.addView(ui.caption(getString(R.string.profile_inherit_note)))
        field(content, profile, C.FIELD_AUTHORIZER, R.string.install_authorizer, authorizers)
        booleans.forEach { (key, title) -> field(content, profile, key, title, listOf(
            FieldChoice(R.string.settings_disabled, JsonPrimitive(false)), FieldChoice(R.string.settings_enabled, JsonPrimitive(true)))) }
        field(content, profile, C.FIELD_DEXOPT, R.string.advanced_dexopt,
            AdvancedInstallUi.dexopt.map { FieldChoice(it.second, JsonPrimitive(it.first)) })
        field(content, profile, C.FIELD_INSTALL_REASON, R.string.advanced_install_reason,
            AdvancedInstallUi.reasons.map { FieldChoice(if (it.first == null) R.string.profile_original_default else it.second,
                it.first?.let(::JsonPrimitive) ?: JsonNull.INSTANCE) })
        field(content, profile, C.FIELD_PACKAGE_SOURCE, R.string.advanced_package_source,
            AdvancedInstallUi.sources.map { FieldChoice(if (it.first == null) R.string.profile_original_default else it.second,
                it.first?.let(::JsonPrimitive) ?: JsonNull.INSTANCE) })
        row(R.string.settings_installer_package, customSummary(profile, C.FIELD_INSTALLER, R.string.profile_installer_default), "profile-option-installer") { installer(profile) }
        row(R.string.install_target_user, userSummary(profile), "profile-option-user") { targetUser(profile) }
        content.addView(ui.caption(getString(R.string.advanced_sdk_note)))
        actions.addView(kit.textButton(getString(R.string.action_cancel), "profile-edit-cancel", onClick = ::back))
        actions.addView(kit.textButton(getString(R.string.profile_finish_edit), "profile-edit-confirm") {
            val value = requireNotNull(draft.editor)
            if (runCatching { InstallProfileRules.validate(value) }.isSuccess) {
                val existing = draft.profiles.indexOfFirst { it.id == value.id }
                draft.profiles = if (existing < 0) draft.profiles + value else draft.profiles.toMutableList().apply { this[existing] = value }
                draft.editor = null; draft.notice = R.string.profile_unsaved; render(resetScroll = true)
            }
        }.apply { isEnabled = runCatching { InstallProfileRules.validate(profile) }.isSuccess })
    }

    private fun update(transform: (InstallProfile) -> InstallProfile) {
        draft.editor = draft.editor?.let(transform)
        render()
    }

    private data class FieldChoice(val label: Int, val value: JsonElement?)
    private fun field(content: LinearLayout, profile: InstallProfile, key: String, title: Int, values: List<FieldChoice>) {
        val choices = listOf(FieldChoice(R.string.profile_inherit, null)) + values
        val selected = choices.indexOfFirst { choice -> if (choice.value == null) !profile.overrides.contains(key) else
            profile.overrides.contains(key) && choice.value == profile.overrides[key] }.coerceAtLeast(0)
        content.addView(ui.row(title, getString(choices[selected].label), tag = "profile-option-$key") {
            prompt = ui.confirmedChoice(title, choices.map { SettingsChoice(getString(it.label)) }, selected) { index ->
                update { it.copy(overrides = it.overrides.with(key, choices[index].value)) }; true
            }
        })
        content.addView(ui.divider())
    }

    private fun customSummary(profile: InstallProfile, key: String, defaultLabel: Int) = when {
        !profile.overrides.contains(key) -> getString(R.string.profile_inherit)
        profile.overrides[key]?.isJsonNull == true -> getString(defaultLabel)
        else -> profile.overrides[key]?.asString.orEmpty()
    }

    private fun installer(profile: InstallProfile) {
        val current = profile.overrides[C.FIELD_INSTALLER]
        val selected = if (!profile.overrides.contains(C.FIELD_INSTALLER)) 0 else if (current?.isJsonNull == true) 1 else 2
        prompt = ui.confirmedChoice(R.string.settings_installer_package,
            listOf(R.string.profile_inherit, R.string.profile_installer_default, R.string.profile_custom).map { SettingsChoice(getString(it)) }, selected) { index ->
            when (index) {
                0 -> update { it.copy(overrides = it.overrides.with(C.FIELD_INSTALLER, null)) }
                1 -> update { it.copy(overrides = it.overrides.with(C.FIELD_INSTALLER, JsonNull.INSTANCE)) }
                else -> {
                    prompt?.dismiss()
                    prompt = ui.input(R.string.settings_installer_package, current?.takeUnless { it.isJsonNull }?.asString.orEmpty(),
                        getString(R.string.settings_installer_note), { value ->
                            if (value.isBlank() || runCatching { RequestDocuments.packageNameOf(value.trim(), "installer", true) }.isSuccess) null else getString(R.string.settings_invalid_package)
                        }, maxLength = C.MAX_INSTALLER_PACKAGE_LENGTH + 1) { value ->
                        update { it.copy(overrides = it.overrides.with(C.FIELD_INSTALLER,
                            value.trim().takeIf(String::isNotEmpty)?.let(::JsonPrimitive) ?: JsonNull.INSTANCE)) }; true
                    }
                }
            }
            true
        }
    }

    private fun userSummary(profile: InstallProfile): String = when {
        !profile.overrides.contains(C.FIELD_USER) -> getString(R.string.profile_inherit)
        profile.overrides[C.FIELD_USER]?.asString == C.USER_CURRENT -> getString(R.string.install_current_user)
        profile.overrides[C.FIELD_USER]?.asString == C.USER_ALL -> getString(R.string.confirm_all_users)
        else -> profile.overrides[C.FIELD_USER]?.asString.orEmpty()
    }

    private fun targetUser(profile: InstallProfile) {
        val current = profile.overrides[C.FIELD_USER]?.asString
        val selected = when { !profile.overrides.contains(C.FIELD_USER) -> 0; current == C.USER_CURRENT -> 1; current == C.USER_ALL -> 2; else -> 3 }
        prompt = ui.confirmedChoice(R.string.install_target_user,
            listOf(R.string.profile_inherit, R.string.install_current_user, R.string.confirm_all_users, R.string.install_user_id).map { SettingsChoice(getString(it)) }, selected) { index ->
            if (index < 3) update { it.copy(overrides = it.overrides.with(C.FIELD_USER,
                when (index) { 0 -> null; 1 -> JsonPrimitive(C.USER_CURRENT); else -> JsonPrimitive(C.USER_ALL) })) }
            else {
                prompt?.dismiss()
                prompt = ui.input(R.string.install_user_id, current?.toIntOrNull()?.toString().orEmpty(), getString(R.string.install_privileged_notice),
                    { value -> if (value.trim().toIntOrNull()?.let { it >= 0 } == true) null else getString(R.string.install_invalid_user) }, maxLength = 11) { value ->
                    update { it.copy(overrides = it.overrides.with(C.FIELD_USER, JsonPrimitive(value.trim().toInt().toString()))) }; true
                }
            }
            true
        }
    }

    private fun sourceLabel(source: String) = when (source) {
        C.SOURCE_HOST -> R.string.profile_source_host
        C.SOURCE_SCRIPT -> R.string.profile_source_script
        C.SOURCE_EXTERNAL -> R.string.profile_source_external
        else -> R.string.profile_source_any
    }

    companion object {
        private val authorizers = listOf(FieldChoice(R.string.install_authorizer_auto, JsonPrimitive(C.AUTHORIZER_AUTO)),
            FieldChoice(R.string.confirm_system, JsonPrimitive(C.AUTHORIZER_NONE)), FieldChoice(R.string.settings_shizuku, JsonPrimitive(C.AUTHORIZER_SHIZUKU)),
            FieldChoice(R.string.settings_root, JsonPrimitive(C.AUTHORIZER_ROOT)), FieldChoice(R.string.settings_dhizuku, JsonPrimitive(C.AUTHORIZER_DHIZUKU)))
        private val booleans = listOf(C.FIELD_ALLOW_DOWNGRADE to R.string.confirm_allow_downgrade, C.FIELD_ALLOW_TEST_ONLY to R.string.confirm_allow_test,
            C.FIELD_BYPASS_LOW_TARGET_SDK to R.string.confirm_bypass_target, C.FIELD_DELETE_SOURCE to R.string.install_delete_source,
            C.FIELD_GRANT_ALL_REQUESTED_PERMISSIONS to R.string.advanced_grant_permissions, C.FIELD_REQUEST_UPDATE_OWNERSHIP to R.string.advanced_update_ownership)
    }
}
