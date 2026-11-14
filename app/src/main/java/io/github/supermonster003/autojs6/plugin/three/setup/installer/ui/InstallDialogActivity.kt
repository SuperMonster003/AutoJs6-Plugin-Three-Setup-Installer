package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.SpannableString
import android.text.TextWatcher
import android.text.format.Formatter
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.JsonObject
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton
import io.github.supermonster003.autojs6.plugin.three.setup.installer.R
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.DeviceUsers
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.policy.SignatureRisk
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.InstallerPreferences
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.SettingsChoice
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.SettingsUi
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.HostAppearanceActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.kit.InstallerDialogLayout
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.kit.InstallerColorPolicy
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

/** Confirmation, progress and results all attach to the same process-local presentation token. */
class InstallDialogActivity : HostAppearanceActivity() {
    private lateinit var saved: InstallDialogSavedState
    private var record: InstallPresentation.Record? = null
    private var layout: InstallerDialogLayout? = null
    private var renderedRevision = Long.MIN_VALUE
    private var progress: ProgressBar? = null
    private var progressText: TextView? = null
    private var information: Dialog? = null
    private var recoveryState: InstallPresentation.Snapshot? = null
    private var recoveryGeneration = 0
    private var closing = false
    private var autoGeneration = 0
    private var autoPromptToken: String? = null
    private val views get() = InstallDialogViews(kit)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(false)
        saved = ViewModelProvider(this)[InstallDialogSavedState::class.java]
        val supplied = intent.getStringExtra(InstallPresentation.EXTRA_TOKEN)
        val data = intent.data
        val valid = supplied != null && InstallRecoverySnapshot.validToken(supplied) && data?.scheme == "three-setup-install" && data.host == "session" && data.lastPathSegment == supplied
        val restored = saved.token
        val token = if (valid && (restored == null || restored == supplied)) supplied else null
        saved.token = token
        if (saved.closing) {
            closing = true
            val owner = WeakReference(this)
            token?.let { InstallRecoveryPersistence.remove(applicationContext, it) { owner.get()?.takeUnless { it.isDestroyed }?.finish() } } ?: finish()
            return
        }
        record = token?.let(InstallPresentation::find)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = dismissPresentation()
        })
        val current = record
        if (current == null) {
            interrupted()
            token?.let { id ->
                // There is no executable owner for this token. Remove old process notifications
                // instead of leaving an ongoing progress/action notice for a read-only result.
                InstallNotifications.remove(applicationContext, id)
                val expected = ++recoveryGeneration
                val owner = WeakReference(this)
                InstallRecoveryPersistence.load(applicationContext, id) { snapshot ->
                    owner.get()?.takeIf { !it.closing && !it.isFinishing && !it.isDestroyed && it.recoveryGeneration == expected }?.let { activity ->
                        activity.recoveryState = snapshot?.display()
                        activity.recoveryState?.let(activity::present) ?: activity.interrupted()
                    }
                }
            }
        } else current.attach(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // documentLaunchMode=intoExisting routes one token to one task. Never swap a live task's
        // owner using an unexpected extra or replayed Intent.
        val token = intent.getStringExtra(InstallPresentation.EXTRA_TOKEN)
        if (token == saved.token && intent.data == this.intent.data) record?.let { present(it.snapshot()) }
    }

    override fun onResume() {
        super.onResume()
        InstallNotifications.resumeForeground(this)
        // Shizuku may have been started or granted while this dialog was in the background.
        record?.snapshot()?.prompt?.let(::refreshAutoAuthorizer)
    }

    override fun onAppearanceChanged() {
        if (closing) return
        information?.dismiss()
        val current = record
        if (current == null && recoveryState == null) interrupted() else redraw()
    }

    internal fun present(state: InstallPresentation.Snapshot) {
        if (closing || isFinishing || isDestroyed) return
        if (renderedRevision == state.revision) {
            updateProgress(state)
            return
        }
        renderedRevision = state.revision
        progress = null
        progressText = null
        val title = when {
            state.interrupted -> R.string.install_interrupted
            state.terminal && state.stage == InstallerContract.STAGE_COMPLETED -> R.string.install_success
            state.terminal && state.stage == InstallerContract.STAGE_CANCELLED -> R.string.install_cancelled
            state.terminal -> R.string.install_failed
            state.safetyReview != null -> R.string.policy_signature_review
            state.prompt != null -> R.string.confirm_install_title
            else -> R.string.install_in_progress
        }
        val dialog = kit.dialog(getString(title))
        layout = dialog
        if (state.recovered && state.interrupted) dialog.content.addView(kit.text(getString(R.string.install_interrupted_explanation)).apply { tag = "install_recovery_explanation" })
        if (state.items.size > 1) batch(dialog.content, state)
        val item = state.items.getOrNull(state.index)
        if (state.safetyReview != null && !state.terminal) signatureReview(dialog, state.safetyReview)
        else if (state.prompt != null) confirmation(dialog, state, state.prompt)
        else if (state.terminal) results(dialog, state)
        else {
            item?.metadata?.let { dialog.content.addView(views.header(it.icon, it.label, it.packageName)) }
                ?: item?.let { dialog.content.addView(kit.text(it.displayName, 16f, medium = true)) }
            progressText = views.note(stageText(state.stage), tag = "install_progress_text", top = 20).also { dialog.content.addView(it) }
            progress = kit.progressBar().also {
                dialog.content.addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, kit.dp(8)).apply {
                    topMargin = kit.dp(12); bottomMargin = kit.dp(4)
                })
                it.tag = "install_progress_bar"
            }
            dialog.content.addView(views.note(getString(if (state.stage == InstallerContract.STAGE_OPTIMIZING)
                R.string.install_optimizing_note else R.string.install_commit_notice)))
            dialog.content.addView(views.spaced(View(this), bottom = 8))
            dialog.actions.addView(kit.textButton(getString(if (state.items.size > 1) R.string.install_cancel_all else R.string.action_cancel), TAG_CANCEL) { record?.cancel() }.apply {
                id = android.R.id.button2
            })
        }
        setContentView(dialog.root)
        updateProgress(state)
        state.prompt?.takeIf { it.token != autoPromptToken }?.let { prompt ->
            autoPromptToken = prompt.token
            refreshAutoAuthorizer(prompt)
        }
    }

    /** Keeps the visible "Automatic (...)" label current; the worker still resolves it again on approval. */
    private fun refreshAutoAuthorizer(prompt: InstallPresentation.Prompt) {
        val expected = ++autoGeneration
        val owner = WeakReference(this)
        val application = applicationContext
        background.execute {
            val resolved = runCatching { InstallerPreferences.resolveAuthorizer(application, InstallerContract.AUTHORIZER_AUTO) }.getOrNull()
            main.post {
                owner.get()?.takeIf { !it.closing && !it.isFinishing && !it.isDestroyed && it.autoGeneration == expected }?.let { activity ->
                    if (resolved != null && prompt.autoAuthorizer != resolved && activity.record?.snapshot()?.prompt === prompt) {
                        prompt.autoAuthorizer = resolved
                        activity.redraw()
                    }
                }
            }
        }
    }

    private fun confirmation(dialog: InstallerDialogLayout, state: InstallPresentation.Snapshot, prompt: InstallPresentation.Prompt) {
        val metadata = prompt.metadata
        val choices = prompt.choices
        val content = dialog.content
        val draft = choices.snapshot()
        content.addView(views.header(metadata.icon, metadata.label, metadata.packageName, kindBadge(metadata)))
        content.addView(views.spaced(views.facts(packageFacts(metadata, state.items.getOrNull(prompt.index)?.profileName)), top = 16, bottom = 4))
        if (metadata.installedUserId != DeviceUsers(this).currentId) {
            content.addView(views.note(getString(R.string.install_metadata_user, metadata.installedUserId), top = 4))
        }
        authorizerRow(content, prompt, draft.options)
        if (metadata.splits.size > 1) splitSection(content, metadata, choices, draft.selectedApkNames)
        permissionSection(content, metadata, draft.selectedApkNames)
        val installButton = kit.textButton(getString(R.string.action_install), TAG_CONFIRM) { record?.accept() }.apply {
            id = android.R.id.button1
            isEnabled = choices.valid()
        }
        optionSection(content, state, prompt, draft.options, installButton)
        dialog.actions.addView(kit.textButton(getString(if (state.items.size > 1) R.string.install_cancel_all else R.string.action_cancel), TAG_CANCEL) {
            record?.cancel()
        }.apply { id = android.R.id.button2 })
        dialog.actions.addView(installButton)
    }

    private fun kindBadge(data: InstallPresentation.Metadata): View? = when (InstallKind.of(data.installedKnown, data.previousVersion?.code, data.versionCode)) {
        InstallKind.NEW -> views.badge(getString(R.string.install_kind_new), kit.palette.accent)
        InstallKind.UPDATE -> views.badge(getString(R.string.install_kind_update), kit.palette.accent)
        InstallKind.REINSTALL -> views.badge(getString(R.string.install_kind_reinstall), kit.palette.muted)
        InstallKind.DOWNGRADE -> views.badge(getString(R.string.install_kind_downgrade), kit.palette.danger)
        null -> null
    }?.apply { tag = TAG_KIND }

    private fun packageFacts(data: InstallPresentation.Metadata, profileName: String?): List<InstallDialogViews.Fact> = buildList {
        val unknown = getString(R.string.install_unknown)
        val archive = version(data.versionName, data.versionCode)
        val installed = data.previousVersion?.takeIf { it.code != data.versionCode || it.name != data.versionName }
        add(InstallDialogViews.Fact(getString(R.string.install_info_version),
            installed?.let { getString(R.string.install_info_version_change, version(it.name, it.code), archive) } ?: archive, tag = "install_info_version"))
        add(InstallDialogViews.Fact(getString(R.string.install_info_size), Formatter.formatFileSize(this@InstallDialogActivity, data.size)))
        add(InstallDialogViews.Fact(getString(R.string.install_info_sdk),
            getString(R.string.install_info_sdk_value, data.minSdk?.toString() ?: unknown, data.targetSdk?.toString() ?: unknown)))
        // A package that is not installed has nothing to compare with; its badge already says so.
        when (data.signature) {
            InstallMetadata.NOT_INSTALLED -> null
            InstallerContract.SIGNER_MATCH -> R.string.install_info_signature_match
            InstallerContract.SIGNER_MISMATCH -> R.string.install_info_signature_mismatch
            InstallMetadata.OTHER_USER -> R.string.install_info_signature_other_user
            else -> R.string.install_info_signature_unknown
        }?.let { add(InstallDialogViews.Fact(getString(R.string.install_info_signature), getString(it),
            danger = it == R.string.install_info_signature_mismatch, tag = "install_info_signature")) }
        data.sharedUserId?.let { add(InstallDialogViews.Fact(getString(R.string.install_info_shared_uid), it)) }
        profileName?.let { add(InstallDialogViews.Fact(getString(R.string.install_info_profile), it)) }
    }

    private fun authorizerName(id: String?): String = when (id) {
        InstallerContract.AUTHORIZER_NONE -> getString(R.string.confirm_system)
        InstallerContract.AUTHORIZER_SHIZUKU -> "Shizuku"
        InstallerContract.AUTHORIZER_ROOT -> "Root"
        InstallerContract.AUTHORIZER_DHIZUKU -> "Dhizuku"
        else -> getString(R.string.install_authorizer_auto)
    }

    /** `Automatic` names the method it selects, so the choice is never a guess. */
    private fun authorizerLabel(id: String, automatic: Authorizer?): String =
        if (Authorizer.isAuto(id) && automatic != null) getString(R.string.install_authorizer_auto_resolved, authorizerName(automatic.id)) else authorizerName(id)

    private fun authorizerRow(content: LinearLayout, prompt: InstallPresentation.Prompt, options: InstallOptions) {
        val choices = prompt.choices
        val labels = AUTHORIZERS.map { authorizerLabel(it, prompt.autoAuthorizer) }
        val current = AUTHORIZERS.indexOf(options.authorizer).coerceAtLeast(0)
        content.addView(views.row(getString(R.string.install_authorizer), labels[current], TAG_AUTHORIZER, InstallDialogViews.Trailing.OPEN) {
            select(R.string.install_authorizer, labels, current) { index ->
                val authorizer = AUTHORIZERS[index]
                val old = choices.snapshot().options
                choices.updateOptions(if (authorizer in UNPRIVILEGED) old.copy(authorizer = authorizer,
                    allowDowngrade = false, allowTestOnly = false, bypassLowTargetSdk = false, installer = null, user = InstallerContract.USER_CURRENT,
                    grantAllRequestedPermissions = false, dexopt = InstallerContract.DEXOPT_NONE)
                    else old.copy(authorizer = authorizer))
                if (authorizer in UNPRIVILEGED) choices.setUser(InstallerContract.USER_CURRENT)
            }
        })
    }

    private fun splitSection(content: LinearLayout, metadata: InstallPresentation.Metadata, choices: InstallChoices, selected: Set<String>) {
        val open = SECTION_SPLITS in saved.expanded
        content.addView(views.row(getString(R.string.install_splits),
            getString(R.string.install_splits_selected, metadata.splits.count { it.name in selected }, metadata.splits.size),
            TAG_SPLITS, if (open) InstallDialogViews.Trailing.COLLAPSE else InstallDialogViews.Trailing.EXPAND) { toggle(SECTION_SPLITS, TAG_SPLITS) })
        if (!open) return
        metadata.splits.forEach { split ->
            val description = when {
                split.base -> getString(R.string.install_base_required)
                !split.selectable -> getString(R.string.install_split_unavailable)
                else -> Formatter.formatFileSize(this, split.size)
            }
            content.addView(kit.checkBox(described(split.name, description), split.name in selected, "install_split_${split.name}") { checked ->
                choices.select(split.name, checked)
                redraw()
            }.apply { isEnabled = split.selectable && !split.base; minHeight = kit.dp(72) })
        }
    }

    /** One control per entry: the secondary line belongs to the label instead of trailing below it. */
    private fun described(label: String, description: String): CharSequence = SpannableString("$label\n$description").apply {
        setSpan(RelativeSizeSpan(14f / 16f), label.length + 1, length, 0)
        setSpan(ForegroundColorSpan(kit.palette.muted), label.length + 1, length, 0)
    }

    private fun permissionSection(content: LinearLayout, metadata: InstallPresentation.Metadata, selected: Set<String>) {
        val permissions = metadata.splits.filter { it.name in selected }.flatMap { it.requestedPermissions }.distinct().sorted()
        val open = SECTION_PERMISSIONS in saved.expanded
        content.addView(views.row(getString(R.string.policy_permissions, permissions.size), null, TAG_PERMISSIONS,
            if (open) InstallDialogViews.Trailing.COLLAPSE else InstallDialogViews.Trailing.EXPAND) { toggle(SECTION_PERMISSIONS, TAG_PERMISSIONS) })
        if (!open) return
        content.addView(views.note(getString(R.string.policy_permissions_note), top = 0))
        if (permissions.isEmpty()) content.addView(views.note(getString(R.string.policy_permissions_none), kit.palette.text, "install_permissions_list", top = 8))
        else content.addView(views.lines(permissions, "install_permissions_list"))
    }

    private fun optionSection(content: LinearLayout, state: InstallPresentation.Snapshot, prompt: InstallPresentation.Prompt,
        options: InstallOptions, installButton: View) {
        val choices = prompt.choices
        val privileged = options.authorizer !in UNPRIVILEGED
        // An invalid target user disables Install; keep its field in view so the reason stays visible.
        val open = SECTION_OPTIONS in saved.expanded || !choices.valid()
        content.addView(views.row(getString(R.string.install_more_options), if (open) null else optionSummary(state, options, choices.userInput()),
            TAG_MORE_OPTIONS, if (open) InstallDialogViews.Trailing.COLLAPSE else InstallDialogViews.Trailing.EXPAND) { toggle(SECTION_OPTIONS, TAG_MORE_OPTIONS) })
        if (!open) return
        fun flag(title: Int, checked: Boolean, tag: String, enabled: Boolean, update: (InstallOptions, Boolean) -> InstallOptions) {
            content.addView(kit.switch(getString(title), checked, tag) { value ->
                choices.updateOptions(update(choices.snapshot().options, value))
            }.apply { isEnabled = enabled })
        }
        fun choice(title: Int, entries: List<Pair<String?, Int>>, current: String?, tag: String, enabled: Boolean, update: (InstallOptions, String?) -> InstallOptions) {
            val index = entries.indexOfFirst { it.first == current }.coerceAtLeast(0)
            content.addView(views.row(getString(title), getString(entries[index].second), tag, InstallDialogViews.Trailing.OPEN, enabled) {
                select(title, entries.map { getString(it.second) }, index) { chosen ->
                    choices.updateOptions(update(choices.snapshot().options, entries[chosen].first))
                }
            })
        }
        flag(R.string.install_delete_source, state.canDeleteSource && options.deleteSource, "install_delete_source", state.canDeleteSource) { value, checked ->
            value.copy(deleteSource = checked)
        }
        if (!state.canDeleteSource) content.addView(views.note(getString(R.string.install_source_owned_by_sender), top = 0).apply {
            (layoutParams as LinearLayout.LayoutParams).bottomMargin = kit.dp(8)
        })
        flag(R.string.confirm_allow_downgrade, options.allowDowngrade, "install_allow_downgrade", privileged) { value, checked -> value.copy(allowDowngrade = checked) }
        flag(R.string.confirm_allow_test, options.allowTestOnly, "install_allow_test", privileged) { value, checked -> value.copy(allowTestOnly = checked) }
        flag(R.string.confirm_bypass_target, options.bypassLowTargetSdk, "install_bypass_target", privileged) { value, checked -> value.copy(bypassLowTargetSdk = checked) }
        flag(R.string.advanced_grant_permissions, options.grantAllRequestedPermissions, "install_grant_permissions", privileged) { value, checked ->
            value.copy(grantAllRequestedPermissions = checked)
        }
        flag(R.string.advanced_update_ownership, options.requestUpdateOwnership, "install_update_ownership", true) { value, checked ->
            value.copy(requestUpdateOwnership = checked)
        }
        choice(R.string.advanced_dexopt, AdvancedInstallUi.dexopt, options.dexopt, "install_dexopt", privileged) { value, chosen ->
            value.copy(dexopt = chosen ?: InstallerContract.DEXOPT_NONE)
        }
        choice(R.string.advanced_install_reason, AdvancedInstallUi.reasons, options.installReason, "install_install_reason", true) { value, chosen ->
            value.copy(installReason = chosen)
        }
        choice(R.string.advanced_package_source, AdvancedInstallUi.sources, options.packageSource, "install_package_source", true) { value, chosen ->
            value.copy(packageSource = chosen)
        }
        targetUser(content, prompt, privileged, installButton)
        content.addView(views.note(getString(R.string.install_privileged_notice), top = 16))
        content.addView(views.note(getString(R.string.advanced_sdk_note)))
        options.installer?.let { content.addView(views.note(getString(R.string.confirm_installer, it))) }
        content.addView(views.spaced(View(this), bottom = 8))
    }

    /** Names what is switched on while the section is folded; an open section shows the controls themselves. */
    private fun optionSummary(state: InstallPresentation.Snapshot, options: InstallOptions, user: String): String = buildList {
        if (state.canDeleteSource && options.deleteSource) add(R.string.install_delete_source)
        if (options.allowDowngrade) add(R.string.confirm_allow_downgrade)
        if (options.allowTestOnly) add(R.string.confirm_allow_test)
        if (options.bypassLowTargetSdk) add(R.string.confirm_bypass_target)
        if (options.grantAllRequestedPermissions) add(R.string.advanced_grant_permissions)
        if (options.requestUpdateOwnership) add(R.string.advanced_update_ownership)
        if (options.dexopt != InstallerContract.DEXOPT_NONE) add(R.string.advanced_dexopt)
        if (options.installReason != null) add(R.string.advanced_install_reason)
        if (options.packageSource != null) add(R.string.advanced_package_source)
        if (user != InstallerContract.USER_CURRENT) add(R.string.install_target_user)
    }.takeIf { it.isNotEmpty() }?.joinToString(", ") { getString(it) } ?: getString(R.string.install_more_options_none)

    private fun targetUser(content: LinearLayout, prompt: InstallPresentation.Prompt, privileged: Boolean, installButton: View) {
        val choices = prompt.choices
        content.addView(views.subheading(getString(R.string.install_target_user)))
        val current = choices.userInput()
        val knownUsers = prompt.users.filter { it.id != DeviceUsers(this).currentId }
        val values = buildList {
            add(InstallerContract.USER_CURRENT to getString(R.string.install_current_user))
            add(InstallerContract.USER_ALL to getString(R.string.confirm_all_users))
            knownUsers.forEach { add(it.id.toString() to getString(R.string.install_user_named, it.id, it.name.orEmpty())) }
            add(CUSTOM_USER to getString(R.string.install_user_id))
        }
        val custom = values.none { it.first == current }
        val targetInput = EditText(this).apply {
            tag = "install_user_input"
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = getString(R.string.install_user_id)
            setText(if (custom) current else "")
            setTextColor(kit.palette.text)
            setHintTextColor(kit.palette.muted)
            backgroundTintList = ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf(android.R.attr.state_focused), intArrayOf()),
                intArrayOf(kit.palette.disabledText, kit.palette.accent, kit.palette.outline))
            textSize = 16f
            minHeight = kit.dp(48)
            isEnabled = custom && privileged
            // The field only matters for a custom ID; the other choices need no empty input below them.
            visibility = if (custom) View.VISIBLE else View.GONE
            isSingleLine = true
            highlightColor = InstallerColorPolicy.withAlpha(kit.palette.accent, 0x55)
            if (Build.VERSION.SDK_INT >= 29) textCursorDrawable = textCursorDrawable?.mutate()?.apply { setTint(kit.palette.accent) }
            if (isEnabled && !choices.valid()) error = getString(R.string.install_invalid_user)
        }
        radioChoices(content, values, if (custom) CUSTOM_USER else current, "install_target_user", privileged) { user ->
            targetInput.isEnabled = user == CUSTOM_USER
            targetInput.visibility = if (user == CUSTOM_USER) View.VISIBLE else View.GONE
            choices.setUser(if (user == CUSTOM_USER) targetInput.text.toString() else user)
            installButton.isEnabled = choices.valid()
            targetInput.error = if (choices.valid()) null else getString(R.string.install_invalid_user)
        }
        targetInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!targetInput.isEnabled) return
                choices.setUser(s.toString())
                installButton.isEnabled = choices.valid()
                targetInput.error = if (choices.valid()) null else getString(R.string.install_invalid_user)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        content.addView(targetInput)
    }

    /** A confirmed selector above the dialog; nothing changes until the choice is confirmed. */
    private fun select(title: Int, labels: List<String>, current: Int, changed: (Int) -> Unit) {
        information?.dismiss()
        information = SettingsUi(this, kit).confirmedChoice(title, labels.map { SettingsChoice(it) }, current) { index ->
            if (index != current) {
                changed(index)
                redraw()
            }
            true
        }
    }

    private fun toggle(section: String, row: String) {
        val opening = section !in saved.expanded
        saved.expanded = if (opening) saved.expanded + section else saved.expanded - section
        redraw(if (opening) row else null)
    }

    private fun signatureReview(dialog: InstallerDialogLayout, review: InstallPresentation.SafetyReview) {
        val facts = review.binding
        dialog.content.addView(kit.text(facts.packageName, 16f, medium = true))
        dialog.content.addView(views.note(if (facts.requestedUser == InstallerContract.USER_ALL) getString(R.string.confirm_all_users)
            else getString(R.string.install_metadata_user, facts.userId), top = 2))
        dialog.content.addView(views.note(getString(if (facts.signatureRisk == SignatureRisk.MISMATCH)
            R.string.policy_signature_mismatch else R.string.policy_signature_unknown), kit.palette.danger, top = 16))
        dialog.content.addView(views.note(getString(R.string.policy_signature_scope)))
        dialog.content.addView(views.note(getString(R.string.policy_source_signers,
            facts.sourceSigners.takeIf { it.isNotEmpty() }?.sorted()?.joinToString("\n") ?: getString(R.string.install_unknown)), kit.palette.text, top = 16)
            .apply { setTextIsSelectable(true) })
        val installed = if (facts.installed.known && !facts.installed.found) getString(R.string.install_signature_not_installed)
            else facts.installed.signers.takeIf { it.isNotEmpty() }?.sorted()?.joinToString("\n") ?: getString(R.string.install_unknown)
        dialog.content.addView(views.note(getString(R.string.policy_installed_signers, installed), kit.palette.text).apply { setTextIsSelectable(true) })
        val accept = kit.textButton(getString(R.string.action_install), "install_signature_continue") {
            record?.acceptSafety(this, review.token)
        }.apply { id = android.R.id.button1; isEnabled = review.acknowledged }
        dialog.content.addView(views.spaced(kit.checkBox(getString(R.string.policy_signature_allow_once), review.acknowledged, "install_signature_acknowledge") { checked ->
            record?.acknowledgeSafety(this, review.token, checked)
            accept.isEnabled = review.acknowledged
        }, top = 8))
        dialog.actions.addView(kit.textButton(getString(R.string.action_cancel), TAG_CANCEL) { record?.cancel() }.apply { id = android.R.id.button2 })
        dialog.actions.addView(accept)
    }

    private fun batch(content: LinearLayout, state: InstallPresentation.Snapshot) {
        section(content, R.string.install_batch)
        state.items.forEachIndexed { index, item ->
            content.addView(views.note(getString(R.string.install_item_status, index + 1, item.metadata?.label ?: item.displayName, stageText(item.stage)),
                kit.palette.text, top = if (index == 0) 0 else 8))
            if (state.recovered) restoredIdentity(content, item)
            sourceDeletionNotice(content, state, item)
            if (state.terminal && item.result?.get(InstallerContract.FIELD_OK)?.asBoolean == false) {
                errorDetails(content, item)
                if (state.canRetry) content.addView(kit.textButton(getString(R.string.install_retry), "install_retry_$index") {
                    if (record?.retry(index) == true) dismissPresentation()
                    else Toast.makeText(this, R.string.install_retry_unavailable, Toast.LENGTH_LONG).show()
                })
            }
        }
    }

    private fun results(dialog: InstallerDialogLayout, state: InstallPresentation.Snapshot) {
        state.failure?.takeIf { failure -> state.items.none {
            it.result?.getAsJsonObject(InstallerContract.FIELD_ERROR)?.get(InstallerContract.FIELD_ERROR_CODE)?.asString == failure.code
        } }?.let { failure ->
            errorDetails(dialog.content, InstallPresentation.Item("", result = JsonObject().apply {
                addProperty(InstallerContract.FIELD_OK, false)
                add(InstallerContract.FIELD_ERROR, failure.toJson())
            }))
        }
        val single = state.items.singleOrNull()
        single?.metadata?.let { dialog.content.addView(views.header(it.icon, it.label, it.packageName)) }
        if (single != null && single.metadata == null) dialog.content.addView(kit.text(single.displayName, 16f, medium = true))
        if (single != null && state.recovered) restoredIdentity(dialog.content, single)
        if (single != null) {
            if (single.result?.get(InstallerContract.FIELD_OK)?.asBoolean == true) {
                // The title already reads "Installation succeeded"; the body states what was installed and how.
                resultFacts(single, state.recovered).takeIf { it.isNotEmpty() }?.let { dialog.content.addView(views.spaced(views.facts(it), top = 16, bottom = 4)) }
                sourceDeletionNotice(dialog.content, state, single)
                val packageName = single.result.get(InstallerContract.FIELD_PACKAGE_NAME)?.asString ?: single.metadata?.packageName
                val user = single.options?.user ?: record?.request?.options?.user
                val current = user == InstallerContract.USER_CURRENT || user == DeviceUsers(this).currentId.toString() || user == InstallerContract.USER_ALL
                val launch = if (!state.recovered && current && packageName != null) packageManager.getLaunchIntentForPackage(packageName) else null
                if (!state.recovered) dialog.actions.addView(kit.textButton(getString(R.string.install_open), TAG_OPEN) {
                    try { startActivity(launch); dismissPresentation() }
                    catch (_: Exception) { Toast.makeText(this, R.string.install_open_unavailable, Toast.LENGTH_LONG).show() }
                }.apply { isEnabled = launch != null })
                if (!state.recovered && launch == null) dialog.content.addView(views.note(if (current) getString(R.string.install_open_no_launch_entry)
                    else getString(R.string.install_open_in_profile, user ?: getString(R.string.install_unknown)), tag = if (current) null else TAG_OPEN_PROFILE))
                dialog.content.addView(views.spaced(View(this), bottom = 8))
            } else {
                errorDetails(dialog.content, single)
                if (state.canRetry) dialog.actions.addView(kit.textButton(getString(R.string.install_retry), "install_retry_0") {
                    if (record?.retry(0) == true) dismissPresentation()
                    else Toast.makeText(this, R.string.install_retry_unavailable, Toast.LENGTH_LONG).show()
                })
            }
        }
        dialog.actions.addView(kit.textButton(getString(R.string.install_done), TAG_DONE) { dismissPresentation() })
    }

    /** What the finished installation changed and which method carried it out. A restored result lists its own identity. */
    private fun resultFacts(item: InstallPresentation.Item, recovered: Boolean): List<InstallDialogViews.Fact> = buildList {
        val result = item.result ?: return@buildList
        val name = result.get(InstallerContract.FIELD_VERSION_NAME)?.asString ?: item.metadata?.versionName
        val code = result.get(InstallerContract.FIELD_VERSION_CODE)?.asLong ?: item.metadata?.versionCode
        if (!recovered && (name != null || code != null)) {
            val installed = version(name, code)
            val replaced = item.metadata?.previousVersion?.takeIf { it.code != code || it.name != name }
            add(InstallDialogViews.Fact(getString(R.string.install_info_version),
                replaced?.let { getString(R.string.install_info_version_change, version(it.name, it.code), installed) } ?: installed))
        }
        result.get(InstallerContract.FIELD_AUTHORIZER)?.takeIf { it.isJsonPrimitive }?.asString?.let { used ->
            val requested = item.options?.authorizer ?: record?.request?.options?.authorizer
            add(InstallDialogViews.Fact(getString(R.string.install_authorizer),
                if (Authorizer.isAuto(requested)) getString(R.string.install_authorizer_auto_resolved, authorizerName(used)) else authorizerName(used),
                tag = TAG_RESULT_AUTHORIZER))
        }
    }

    private fun sourceDeletionNotice(content: LinearLayout, state: InstallPresentation.Snapshot, item: InstallPresentation.Item) {
        val result = item.result
        if (item.followUpPending) {
            if (state.recovered || state.terminal) content.addView(views.note(getString(R.string.advanced_follow_up_unknown), top = 8))
            return
        }
        InstallFollowUpUi.summary(this, result).forEach { content.addView(views.note(it, tag = "install_follow_up", top = 8)) }
        if (state.canDeleteSource && item.options?.deleteSource == true && result?.get(InstallerContract.FIELD_OK)?.asBoolean == true &&
            result.get(InstallerContract.FIELD_SOURCE_DELETED)?.asBoolean != true) {
            content.addView(views.note(getString(R.string.install_source_not_deleted), tag = TAG_SOURCE_NOT_DELETED, top = 8))
        }
    }

    private fun restoredIdentity(content: LinearLayout, item: InstallPresentation.Item) {
        val result = item.result ?: return
        result.get(InstallerContract.FIELD_PACKAGE_NAME)?.asString?.let { content.addView(views.note(it, top = 2)) }
        val name = result.get(InstallerContract.FIELD_VERSION_NAME)?.asString
        val code = result.get(InstallerContract.FIELD_VERSION_CODE)?.asLong
        val previous = result.get(InstallerContract.FIELD_PREVIOUS_VERSION_CODE)?.asLong
        if (name != null || code != null) {
            val text = if (result.get(InstallerContract.FIELD_OK)?.asBoolean == true && previous != null)
                getString(R.string.install_version_change, version(null, previous), version(name, code))
            else getString(R.string.install_package_version, version(name, code))
            content.addView(views.note(text, top = 2))
        }
    }

    private fun errorDetails(content: LinearLayout, item: InstallPresentation.Item) {
        val error = item.result?.getAsJsonObject(InstallerContract.FIELD_ERROR)
        val code = error?.get(InstallerContract.FIELD_ERROR_CODE)?.asString ?: InstallerErrorCodes.INTERNAL
        val systemMessage = error?.get(InstallerContract.FIELD_ERROR_SYSTEM_MESSAGE)?.asString?.takeIf { it.isNotBlank() }
        val failureText = getString(R.string.install_error_code, code) + (systemMessage?.let { "\n$it" } ?: "")
        if (item.metadata?.format == InstallerContract.FORMAT_AAB) {
            content.addView(views.spaced(kit.text(getString(R.string.install_aab_unsupported), 16f, kit.palette.danger), top = 12))
            content.addView(kit.textButton(getString(R.string.install_aab_information), "install_aab_information") { aabInformation(item.metadata) })
        }
        content.addView(views.note(failureText, kit.palette.danger, TAG_ERROR).apply { setTextIsSelectable(true) })
        if (systemMessage == null && recoveryState == null) content.addView(views.note(getString(R.string.install_no_system_message), top = 8))
        content.addView(kit.textButton(getString(R.string.install_copy), TAG_COPY) {
            getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(getString(R.string.install_failed), failureText))
            Toast.makeText(this, R.string.install_copied, Toast.LENGTH_SHORT).show()
        })
    }

    private fun aabInformation(data: InstallPresentation.Metadata) {
        information?.dismiss()
        val message = getString(R.string.install_aab_explanation) + if (data.aabModules.isEmpty()) "" else "\n\n" +
            getString(R.string.install_aab_modules, data.aabModules.joinToString(", "))
        information = MaterialAlertDialogBuilder(this).setTitle(R.string.install_aab_information).setMessage(message)
            .setBackground(kit.roundedFill(kit.palette.surface, 24))
            .setPositiveButton(R.string.install_done, null).create().also { alert ->
                alert.setOnShowListener {
                    alert.getButton(AlertDialog.BUTTON_POSITIVE).apply { isAllCaps = false; setTextColor(kit.palette.primary) }
                    alert.window?.setLayout(minOf(kit.dp(560), resources.displayMetrics.widthPixels - kit.dp(48)), ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                alert.show()
            }
    }

    private fun updateProgress(state: InstallPresentation.Snapshot) {
        val writing = state.stage == InstallerContract.STAGE_WRITING
        progress?.apply { isIndeterminate = !writing; max = 100; progress = (state.progress * 100).toInt() }
        progressText?.text = if (writing) getString(R.string.install_progress_percent, stageText(state.stage), (state.progress * 100).toInt()) else stageText(state.stage)
    }

    private fun interrupted() {
        if (isFinishing || isDestroyed) return
        val dialog = kit.dialog(getString(R.string.install_interrupted))
        dialog.content.addView(kit.text(getString(R.string.install_interrupted_explanation)))
        dialog.actions.addView(kit.textButton(getString(R.string.install_done), TAG_DONE) { dismissPresentation() })
        setContentView(dialog.root)
    }

    private fun radioChoices(content: LinearLayout, choices: List<Pair<String, String>>, selected: String, prefix: String,
        enabled: Boolean = true, changed: (String) -> Unit) {
        val group = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        choices.forEach { (value, label) ->
            group.addView(MaterialRadioButton(this).apply {
                id = View.generateViewId()
                tag = "${prefix}_$value"
                text = label
                textSize = 16f
                setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
                    intArrayOf(kit.palette.disabledText, kit.palette.text)))
                buttonTintList = kit.choiceTint()
                minimumHeight = kit.dp(56)
                isEnabled = enabled
                isChecked = value == selected
                setOnClickListener { changed(value) }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        content.addView(group)
    }

    private fun section(content: LinearLayout, title: Int) {
        content.addView(kit.text(getString(title), 14f, kit.palette.muted, true).apply {
            setPaddingRelative(0, kit.dp(24), 0, kit.dp(8))
            androidx.core.view.ViewCompat.setAccessibilityHeading(this, true)
        })
    }

    private fun stageText(stage: String): String = getString(when (stage) {
        InstallerContract.STAGE_PREPARING -> R.string.install_preparing
        InstallerContract.STAGE_CONFIRMING -> R.string.install_waiting_system
        InstallerContract.STAGE_WRITING -> R.string.install_writing
        InstallerContract.STAGE_COMMITTING -> R.string.install_committing
        InstallerContract.STAGE_OPTIMIZING -> R.string.install_optimizing
        InstallerContract.STAGE_COMPLETED -> R.string.install_success
        InstallerContract.STAGE_FAILED -> R.string.install_failed
        InstallerContract.STAGE_CANCELLED -> R.string.install_cancelled
        else -> R.string.install_pending
    })

    private fun version(name: String?, code: Long?): String = when {
        !name.isNullOrBlank() && code != null -> getString(R.string.install_version, name, code)
        !name.isNullOrBlank() -> name
        code != null -> code.toString()
        else -> getString(R.string.install_unknown)
    }

    /** Rebuilds the page in place; [reveal] names a row whose section was just opened. */
    private fun redraw(reveal: String? = null) {
        val scrollY = layout?.scroll?.scrollY ?: 0
        renderedRevision = Long.MIN_VALUE
        (record?.snapshot() ?: recoveryState)?.let(::present)
        layout?.scroll?.post {
            val scroll = layout?.scroll ?: return@post
            scroll.scrollTo(0, scrollY)
            // Scroll no further than the opened section needs; a section taller than the viewport starts at its row.
            reveal?.let { scroll.findViewWithTag<View>(it) }?.let { row ->
                val content = row.parent as? ViewGroup ?: return@let
                val following = (content.indexOfChild(row) + 1 until content.childCount).map(content::getChildAt)
                val end = following.firstOrNull { it.tag in SECTION_ROWS }?.top ?: content.height
                if (end > scrollY + scroll.height) scroll.smoothScrollTo(0, minOf(row.top, end - scroll.height))
            }
        }
    }

    override fun onDestroy() {
        recoveryGeneration++
        information?.dismiss()
        record?.detach(this)
        if (isFinishing && !isChangingConfigurations) {
            if (record != null) record?.dismiss()
            else saved.token?.let { InstallNotifications.remove(applicationContext, it); InstallRecoveryPersistence.remove(applicationContext, it) }
        }
        super.onDestroy()
    }

    private fun dismissPresentation() {
        if (closing) return
        closing = true
        saved.closing = true
        recoveryGeneration++
        val current = record
        if (current != null) current.dismiss()
        else saved.token?.let { token ->
            InstallNotifications.remove(applicationContext, token)
            val owner = WeakReference(this)
            InstallRecoveryPersistence.remove(applicationContext, token) { owner.get()?.takeUnless { it.isDestroyed }?.finish() }
        } ?: finish()
    }

    companion object {
        const val TAG_CONFIRM = "install_confirm"
        const val TAG_CANCEL = "install_cancel"
        const val TAG_DONE = "install_done"
        const val TAG_OPEN = "install_open"
        const val TAG_ERROR = "install_error"
        const val TAG_COPY = "install_copy"
        const val TAG_SOURCE_NOT_DELETED = "install_source_not_deleted"
        const val TAG_OPEN_PROFILE = "install_open_profile"
        const val TAG_KIND = "install_kind"
        const val TAG_AUTHORIZER = "install_authorizer"
        const val TAG_SPLITS = "install_splits_expand"
        const val TAG_PERMISSIONS = "install_permissions_expand"
        const val TAG_MORE_OPTIONS = "install_more_options"
        const val TAG_RESULT_AUTHORIZER = "install_result_authorizer"
        private const val CUSTOM_USER = "custom"
        private const val SECTION_SPLITS = "splits"
        private const val SECTION_PERMISSIONS = "permissions"
        private const val SECTION_OPTIONS = "options"
        private val SECTION_ROWS = setOf(TAG_SPLITS, TAG_PERMISSIONS, TAG_MORE_OPTIONS)
        private val AUTHORIZERS = listOf(InstallerContract.AUTHORIZER_AUTO, InstallerContract.AUTHORIZER_NONE,
            InstallerContract.AUTHORIZER_SHIZUKU, InstallerContract.AUTHORIZER_ROOT, InstallerContract.AUTHORIZER_DHIZUKU)
        /** Authorizers that cannot honour the privileged options or another target user. */
        private val UNPRIVILEGED = listOf(InstallerContract.AUTHORIZER_NONE, InstallerContract.AUTHORIZER_DHIZUKU)
        private val main = Handler(Looper.getMainLooper())
        private val background = Executors.newSingleThreadExecutor { task -> Thread(task, "installer-dialog-state").apply { isDaemon = true } }
    }
}

class InstallDialogSavedState(private val saved: SavedStateHandle) : ViewModel() {
    var token: String?
        get() = saved[InstallPresentation.EXTRA_TOKEN]
        set(value) { saved[InstallPresentation.EXTRA_TOKEN] = value }
    var closing: Boolean
        get() = saved["presentationClosed"] ?: false
        set(value) { saved["presentationClosed"] = value }
    /** Folded sections the user opened; a recreated dialog keeps them open. */
    var expanded: Set<String>
        get() = saved.get<ArrayList<String>>("expandedSections").orEmpty().toSet()
        set(value) { saved["expandedSections"] = ArrayList(value) }
}
