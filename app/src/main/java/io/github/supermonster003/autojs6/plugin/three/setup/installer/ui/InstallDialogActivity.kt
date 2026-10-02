package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.format.Formatter
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
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
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.DeviceUsers
import io.github.supermonster003.autojs6.plugin.three.setup.installer.policy.SignatureRisk
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.HostAppearanceActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.kit.InstallerDialogLayout
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.kit.InstallerColorPolicy
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.lang.ref.WeakReference

/** Confirmation, progress and results all attach to the same process-local presentation token. */
class InstallDialogActivity : HostAppearanceActivity() {
    private lateinit var saved: InstallDialogSavedState
    private var record: InstallPresentation.Record? = null
    private var layout: InstallerDialogLayout? = null
    private var renderedRevision = Long.MIN_VALUE
    private var progress: ProgressBar? = null
    private var progressText: TextView? = null
    private var information: AlertDialog? = null
    private var recoveryState: InstallPresentation.Snapshot? = null
    private var recoveryGeneration = 0
    private var closing = false
    private var permissionsExpanded = false

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
            item?.metadata?.let { header(dialog.content, it) }
                ?: item?.let { dialog.content.addView(kit.text(it.displayName, 16f)) }
            progressText = kit.text(stageText(state.stage), color = kit.palette.muted).also { it.tag = "install_progress_text"; dialog.content.addView(it) }
            progress = kit.progressBar().also {
                dialog.content.addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, kit.dp(8)).apply {
                    topMargin = kit.dp(16); bottomMargin = kit.dp(16)
                })
                it.tag = "install_progress_bar"
            }
            dialog.content.addView(kit.text(getString(if (state.stage == InstallerContract.STAGE_OPTIMIZING)
                R.string.install_optimizing_note else R.string.install_commit_notice), color = kit.palette.muted))
            dialog.actions.addView(kit.textButton(getString(if (state.items.size > 1) R.string.install_cancel_all else R.string.action_cancel), TAG_CANCEL) { record?.cancel() }.apply {
                id = android.R.id.button2
            })
        }
        setContentView(dialog.root)
        updateProgress(state)
    }

    private fun confirmation(dialog: InstallerDialogLayout, state: InstallPresentation.Snapshot, prompt: InstallPresentation.Prompt) {
        val metadata = prompt.metadata
        val choices = prompt.choices
        header(dialog.content, metadata)
        metadata(dialog.content, metadata)
        state.items.getOrNull(prompt.index)?.profileName?.let {
            dialog.content.addView(kit.text(getString(R.string.profile_matched, it), 14f, kit.palette.muted))
        }
        if (metadata.splits.size > 1) {
            section(dialog.content, R.string.install_splits)
            val selected = choices.snapshot().selectedApkNames
            metadata.splits.forEach { split ->
                val description = when {
                    split.base -> getString(R.string.install_base_required)
                    !split.selectable -> getString(R.string.install_split_unavailable)
                    else -> Formatter.formatFileSize(this, split.size)
                }
                dialog.content.addView(kit.checkBox(split.name, split.name in selected, "install_split_${split.name}") { checked ->
                    choices.select(split.name, checked)
                    redraw()
                }.apply { isEnabled = split.selectable && !split.base })
                dialog.content.addView(kit.text(description, color = kit.palette.muted).apply { setPaddingRelative(kit.dp(40), 0, 0, kit.dp(8)) })
            }
        }
        permissionPreview(dialog.content, metadata, choices.snapshot().selectedApkNames)
        section(dialog.content, R.string.install_options)
        section(dialog.content, R.string.install_authorizer)
        val initial = choices.snapshot().options
        radioChoices(dialog.content, listOf(
            InstallerContract.AUTHORIZER_AUTO to getString(R.string.install_authorizer_auto),
            InstallerContract.AUTHORIZER_NONE to getString(R.string.confirm_system),
            InstallerContract.AUTHORIZER_SHIZUKU to "Shizuku",
            InstallerContract.AUTHORIZER_ROOT to "Root",
            InstallerContract.AUTHORIZER_DHIZUKU to "Dhizuku",
        ), initial.authorizer, "install_authorizer") { authorizer ->
            val old = choices.snapshot().options
            choices.updateOptions(if (authorizer in listOf(InstallerContract.AUTHORIZER_NONE, InstallerContract.AUTHORIZER_DHIZUKU)) old.copy(authorizer = authorizer,
                allowDowngrade = false, allowTestOnly = false, bypassLowTargetSdk = false, installer = null, user = InstallerContract.USER_CURRENT,
                grantAllRequestedPermissions = false, dexopt = InstallerContract.DEXOPT_NONE)
                else old.copy(authorizer = authorizer))
            if (authorizer in listOf(InstallerContract.AUTHORIZER_NONE, InstallerContract.AUTHORIZER_DHIZUKU)) choices.setUser(InstallerContract.USER_CURRENT)
            redraw()
        }
        val privileged = initial.authorizer !in listOf(InstallerContract.AUTHORIZER_NONE, InstallerContract.AUTHORIZER_DHIZUKU)
        dialog.content.addView(kit.switch(getString(R.string.confirm_allow_downgrade), initial.allowDowngrade, "install_allow_downgrade") {
            choices.updateOptions(choices.snapshot().options.copy(allowDowngrade = it))
        }.apply { isEnabled = privileged })
        dialog.content.addView(kit.switch(getString(R.string.confirm_allow_test), initial.allowTestOnly, "install_allow_test") {
            choices.updateOptions(choices.snapshot().options.copy(allowTestOnly = it))
        }.apply { isEnabled = privileged })
        dialog.content.addView(kit.switch(getString(R.string.confirm_bypass_target), initial.bypassLowTargetSdk, "install_bypass_target") {
            choices.updateOptions(choices.snapshot().options.copy(bypassLowTargetSdk = it))
        }.apply { isEnabled = privileged })
        dialog.content.addView(kit.switch(getString(R.string.install_delete_source), state.canDeleteSource && initial.deleteSource, "install_delete_source") {
            choices.updateOptions(choices.snapshot().options.copy(deleteSource = it))
        }.apply { isEnabled = state.canDeleteSource })
        dialog.content.addView(kit.switch(getString(R.string.advanced_grant_permissions), initial.grantAllRequestedPermissions, "install_grant_permissions") {
            choices.updateOptions(choices.snapshot().options.copy(grantAllRequestedPermissions = it))
        }.apply { isEnabled = privileged })
        dialog.content.addView(kit.switch(getString(R.string.advanced_update_ownership), initial.requestUpdateOwnership, "install_update_ownership") {
            choices.updateOptions(choices.snapshot().options.copy(requestUpdateOwnership = it))
        })
        advancedChoice(dialog.content, R.string.advanced_dexopt, AdvancedInstallUi.dexopt, initial.dexopt, "install_dexopt", privileged) {
            choices.updateOptions(choices.snapshot().options.copy(dexopt = it ?: InstallerContract.DEXOPT_NONE))
        }
        advancedChoice(dialog.content, R.string.advanced_install_reason, AdvancedInstallUi.reasons, initial.installReason, "install_install_reason") {
            choices.updateOptions(choices.snapshot().options.copy(installReason = it))
        }
        advancedChoice(dialog.content, R.string.advanced_package_source, AdvancedInstallUi.sources, initial.packageSource, "install_package_source") {
            choices.updateOptions(choices.snapshot().options.copy(packageSource = it))
        }
        dialog.content.addView(kit.text(getString(R.string.advanced_sdk_note), 14f, kit.palette.muted))
        if (!state.canDeleteSource) dialog.content.addView(kit.text(getString(R.string.install_source_owned_by_sender), color = kit.palette.muted))
        initial.installer?.let { dialog.content.addView(kit.text(getString(R.string.confirm_installer, it), color = kit.palette.muted)) }
        val installButton = kit.textButton(getString(R.string.action_install), TAG_CONFIRM) { record?.accept() }.apply {
            id = android.R.id.button1
            isEnabled = choices.valid()
        }
        section(dialog.content, R.string.install_target_user)
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
            isSingleLine = true
            highlightColor = InstallerColorPolicy.withAlpha(kit.palette.accent, 0x55)
            if (Build.VERSION.SDK_INT >= 29) textCursorDrawable = textCursorDrawable?.mutate()?.apply { setTint(kit.palette.accent) }
        }
        radioChoices(dialog.content, values, if (custom) CUSTOM_USER else current, "install_target_user", privileged) { user ->
            targetInput.isEnabled = user == CUSTOM_USER
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
        dialog.content.addView(targetInput)
        dialog.content.addView(kit.text(getString(R.string.install_privileged_notice), color = kit.palette.muted))
        dialog.actions.addView(kit.textButton(getString(if (state.items.size > 1) R.string.install_cancel_all else R.string.action_cancel), TAG_CANCEL) {
            record?.cancel()
        }.apply { id = android.R.id.button2 })
        dialog.actions.addView(installButton)
    }

    private fun header(content: LinearLayout, metadata: InstallPresentation.Metadata) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
        metadata.icon?.let { icon ->
            row.addView(ImageView(this).apply {
                setImageBitmap(icon)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(kit.dp(48), kit.dp(48)).apply { marginEnd = kit.dp(16) })
        }
        row.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(kit.text(metadata.label, 16f, medium = true))
            metadata.packageName?.let { addView(kit.text(it, color = kit.palette.muted).apply { setTextIsSelectable(true) }) }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        content.addView(row)
    }

    private fun metadata(content: LinearLayout, data: InstallPresentation.Metadata) {
        val new = version(data.versionName, data.versionCode)
        val versionText = when {
            !data.installedKnown -> getString(R.string.install_package_version, new)
            data.previousVersion == null -> getString(R.string.install_new_version, new)
            else -> getString(R.string.install_version_change, version(data.previousVersion.name, data.previousVersion.code), new)
        }
        content.addView(kit.text(versionText, color = kit.palette.muted))
        content.addView(kit.text(getString(R.string.install_package_size, Formatter.formatFileSize(this, data.size)), color = kit.palette.muted))
        content.addView(kit.text(getString(R.string.install_sdk_versions, data.minSdk?.toString() ?: getString(R.string.install_unknown),
            data.targetSdk?.toString() ?: getString(R.string.install_unknown)), color = kit.palette.muted))
        val signer = when (data.signature) {
            InstallerContract.SIGNER_MATCH -> R.string.install_signature_match
            InstallerContract.SIGNER_MISMATCH -> R.string.install_signature_mismatch
            InstallMetadata.NOT_INSTALLED -> R.string.install_signature_not_installed
            InstallMetadata.OTHER_USER -> R.string.install_signature_other_user
            else -> R.string.install_signature_unknown
        }
        content.addView(kit.text(getString(signer), color = if (signer == R.string.install_signature_mismatch) kit.palette.danger else kit.palette.muted))
        content.addView(kit.text(getString(R.string.install_metadata_user, data.installedUserId), color = kit.palette.muted))
        data.sharedUserId?.let { content.addView(kit.text(getString(R.string.policy_shared_uid, it), color = kit.palette.muted)) }
    }

    private fun permissionPreview(content: LinearLayout, metadata: InstallPresentation.Metadata, selected: Set<String>) {
        val permissions = metadata.splits.filter { it.name in selected }.flatMap { it.requestedPermissions }.distinct().sorted()
        content.addView(kit.textButton(getString(R.string.policy_permissions, permissions.size), "install_permissions_expand") {
            permissionsExpanded = !permissionsExpanded
            redraw()
        })
        if (permissionsExpanded) {
            content.addView(kit.text(getString(R.string.policy_permissions_note), 14f, kit.palette.muted))
            content.addView(kit.text(if (permissions.isEmpty()) getString(R.string.policy_permissions_none) else permissions.joinToString("\n"),
                14f, kit.palette.text).apply { tag = "install_permissions_list"; setTextIsSelectable(true) })
        }
    }

    private fun advancedChoice(content: LinearLayout, title: Int, options: List<Pair<String?, Int>>, current: String?,
        tag: String, enabled: Boolean = true, changed: (String?) -> Unit) {
        val selected = options.indexOfFirst { it.first == current }.coerceAtLeast(0)
        content.addView(kit.textButton(getString(title) + ": " + getString(options[selected].second), tag) {
            information?.dismiss()
            var draft = selected
            information = MaterialAlertDialogBuilder(this).setTitle(title)
                .setSingleChoiceItems(options.map { getString(it.second) }.toTypedArray(), selected) { _, index -> draft = index }
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.settings_confirm) { _, _ -> changed(options[draft].first); redraw() }
                .setBackground(kit.roundedFill(kit.palette.surface, 24)).create().also { it.show() }
        }.apply { isEnabled = enabled })
    }

    private fun signatureReview(dialog: InstallerDialogLayout, review: InstallPresentation.SafetyReview) {
        val facts = review.binding
        dialog.content.addView(kit.text(facts.packageName, 16f, medium = true))
        dialog.content.addView(kit.text(if (facts.requestedUser == InstallerContract.USER_ALL) getString(R.string.confirm_all_users)
            else getString(R.string.install_metadata_user, facts.userId), color = kit.palette.muted))
        dialog.content.addView(kit.text(getString(if (facts.signatureRisk == SignatureRisk.MISMATCH)
            R.string.policy_signature_mismatch else R.string.policy_signature_unknown), color = kit.palette.danger))
        dialog.content.addView(kit.text(getString(R.string.policy_signature_scope), 14f, kit.palette.muted))
        dialog.content.addView(kit.text(getString(R.string.policy_source_signers,
            facts.sourceSigners.takeIf { it.isNotEmpty() }?.sorted()?.joinToString("\n") ?: getString(R.string.install_unknown)), 14f).apply { setTextIsSelectable(true) })
        val installed = if (facts.installed.known && !facts.installed.found) getString(R.string.install_signature_not_installed)
            else facts.installed.signers.takeIf { it.isNotEmpty() }?.sorted()?.joinToString("\n") ?: getString(R.string.install_unknown)
        dialog.content.addView(kit.text(getString(R.string.policy_installed_signers, installed), 14f).apply { setTextIsSelectable(true) })
        val accept = kit.textButton(getString(R.string.action_install), "install_signature_continue") {
            record?.acceptSafety(this, review.token)
        }.apply { id = android.R.id.button1; isEnabled = review.acknowledged }
        dialog.content.addView(kit.checkBox(getString(R.string.policy_signature_allow_once), review.acknowledged, "install_signature_acknowledge") { checked ->
            record?.acknowledgeSafety(this, review.token, checked)
            accept.isEnabled = review.acknowledged
        })
        dialog.actions.addView(kit.textButton(getString(R.string.action_cancel), TAG_CANCEL) { record?.cancel() }.apply { id = android.R.id.button2 })
        dialog.actions.addView(accept)
    }

    private fun batch(content: LinearLayout, state: InstallPresentation.Snapshot) {
        section(content, R.string.install_batch)
        state.items.forEachIndexed { index, item ->
            content.addView(kit.text(getString(R.string.install_item_status, index + 1, item.metadata?.label ?: item.displayName, stageText(item.stage)), 14f))
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
        single?.metadata?.let { header(dialog.content, it) }
        if (single != null && single.metadata == null) dialog.content.addView(kit.text(single.displayName, 16f, medium = true))
        if (single != null && state.recovered) restoredIdentity(dialog.content, single)
        if (single != null) {
            if (single.result?.get(InstallerContract.FIELD_OK)?.asBoolean == true) {
                dialog.content.addView(kit.text(getString(R.string.install_success), 16f))
                sourceDeletionNotice(dialog.content, state, single)
                val packageName = single.result.get(InstallerContract.FIELD_PACKAGE_NAME)?.asString ?: single.metadata?.packageName
                val user = single.options?.user ?: record?.request?.options?.user
                val current = user == InstallerContract.USER_CURRENT || user == DeviceUsers(this).currentId.toString() || user == InstallerContract.USER_ALL
                val launch = if (!state.recovered && current && packageName != null) packageManager.getLaunchIntentForPackage(packageName) else null
                if (!state.recovered) dialog.actions.addView(kit.textButton(getString(R.string.install_open), TAG_OPEN) {
                    try { startActivity(launch); dismissPresentation() }
                    catch (_: Exception) { Toast.makeText(this, R.string.install_open_unavailable, Toast.LENGTH_LONG).show() }
                }.apply { isEnabled = launch != null })
                if (!state.recovered && launch == null) dialog.content.addView(kit.text(if (current) getString(R.string.install_open_unavailable)
                    else getString(R.string.install_open_in_profile, user ?: getString(R.string.install_unknown)), color = kit.palette.muted).apply {
                    if (!current) tag = TAG_OPEN_PROFILE
                })
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

    private fun sourceDeletionNotice(content: LinearLayout, state: InstallPresentation.Snapshot, item: InstallPresentation.Item) {
        val result = item.result
        if (item.followUpPending) {
            if (state.recovered || state.terminal) content.addView(kit.text(getString(R.string.advanced_follow_up_unknown), color = kit.palette.muted))
            return
        }
        InstallFollowUpUi.summary(this, result).forEach { content.addView(kit.text(it, color = kit.palette.muted).apply { tag = "install_follow_up" }) }
        if (state.canDeleteSource && item.options?.deleteSource == true && result?.get(InstallerContract.FIELD_OK)?.asBoolean == true &&
            result.get(InstallerContract.FIELD_SOURCE_DELETED)?.asBoolean != true) {
            content.addView(kit.text(getString(R.string.install_source_not_deleted), color = kit.palette.muted).apply { tag = TAG_SOURCE_NOT_DELETED })
        }
    }

    private fun restoredIdentity(content: LinearLayout, item: InstallPresentation.Item) {
        val result = item.result ?: return
        result.get(InstallerContract.FIELD_PACKAGE_NAME)?.asString?.let { content.addView(kit.text(it, color = kit.palette.muted)) }
        val name = result.get(InstallerContract.FIELD_VERSION_NAME)?.asString
        val code = result.get(InstallerContract.FIELD_VERSION_CODE)?.asLong
        val previous = result.get(InstallerContract.FIELD_PREVIOUS_VERSION_CODE)?.asLong
        if (name != null || code != null) {
            val text = if (result.get(InstallerContract.FIELD_OK)?.asBoolean == true && previous != null)
                getString(R.string.install_version_change, version(null, previous), version(name, code))
            else getString(R.string.install_package_version, version(name, code))
            content.addView(kit.text(text, color = kit.palette.muted))
        }
    }

    private fun errorDetails(content: LinearLayout, item: InstallPresentation.Item) {
        val error = item.result?.getAsJsonObject(InstallerContract.FIELD_ERROR)
        val code = error?.get(InstallerContract.FIELD_ERROR_CODE)?.asString ?: InstallerErrorCodes.INTERNAL
        val systemMessage = error?.get(InstallerContract.FIELD_ERROR_SYSTEM_MESSAGE)?.asString?.takeIf { it.isNotBlank() }
        val failureText = getString(R.string.install_error_code, code) + (systemMessage?.let { "\n$it" } ?: "")
        if (item.metadata?.format == InstallerContract.FORMAT_AAB) {
            content.addView(kit.text(getString(R.string.install_aab_unsupported), 16f, kit.palette.danger))
            content.addView(kit.textButton(getString(R.string.install_aab_information), "install_aab_information") { aabInformation(item.metadata) })
        }
        content.addView(kit.text(failureText, color = kit.palette.danger).apply { tag = TAG_ERROR; setTextIsSelectable(true) })
        if (systemMessage == null && recoveryState == null) content.addView(kit.text(getString(R.string.install_no_system_message), color = kit.palette.muted))
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

    private fun redraw() {
        val scrollY = layout?.scroll?.scrollY ?: 0
        renderedRevision = Long.MIN_VALUE
        (record?.snapshot() ?: recoveryState)?.let(::present)
        layout?.scroll?.post { layout?.scroll?.scrollTo(0, scrollY) }
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
        private const val CUSTOM_USER = "custom"
    }
}

class InstallDialogSavedState(private val saved: SavedStateHandle) : ViewModel() {
    var token: String?
        get() = saved[InstallPresentation.EXTRA_TOKEN]
        set(value) { saved[InstallPresentation.EXTRA_TOKEN] = value }
    var closing: Boolean
        get() = saved["presentationClosed"] ?: false
        set(value) { saved["presentationClosed"] = value }
}
