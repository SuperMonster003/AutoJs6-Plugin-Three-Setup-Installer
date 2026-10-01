package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.supermonster003.autojs6.plugin.three.setup.installer.R
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerState
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.history.InstallHistoryEntry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.history.InstallHistoryStore
import io.github.supermonster003.autojs6.plugin.three.setup.installer.queue.InstallQueue
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.DefaultInstallerController
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.DefaultInstallerState
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.SettingsUi
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.ExternalSources
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.HostAppearanceActivity
import org.autojs.plugin.installer.api.InstallerContract
import java.io.Closeable
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** The launcher observes existing operations; opening or recreating Home never replays an install. */
class HomeActivity : HostAppearanceActivity() {
    override val dialogTheme = false
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "installer-home-authorizers") }
    private var authorizerWork: Future<*>? = null
    private var generation = 0
    private var requesting: Authorizer? = null
    private var states: Map<Authorizer, AuthorizerState> = emptyMap()
    private var defaultState: DefaultInstallerState? = null
    private var defaultBusy = false
    private lateinit var defaults: DefaultInstallerController
    private lateinit var history: InstallHistoryStore
    private lateinit var ui: SettingsUi
    private lateinit var authorizationCard: LinearLayout
    private lateinit var defaultCard: LinearLayout
    private lateinit var tasks: LinearLayout
    private lateinit var records: LinearLayout
    private var taskObserver: Closeable? = null
    private var historyObserver: Closeable? = null
    private var prompt: Dialog? = null
    private var observing = false
    private var updateQueued = false
    private var shownHistory: List<InstallHistoryEntry>? = null
    private data class TaskWidgets(val row: LinearLayout, val title: TextView, val stage: TextView, val progress: ProgressBar)
    private val taskWidgets = linkedMapOf<String, TaskWidgets>()
    private var emptyTasks: View? = null
    private val redrawTasks = Runnable {
        updateQueued = false
        if (observing && !isDestroyed && !isFinishing) renderTasks()
    }

    private val picker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode != RESULT_OK || data == null) return@registerForActivityResult
        runCatching {
            val selected = buildList {
                data.clipData?.let { clip ->
                    require(clip.itemCount <= InstallerContract.MAX_BATCH_SOURCES)
                    repeat(clip.itemCount) { add(clip.getItemAt(it).uri ?: error("missing source URI")) }
                } ?: data.data?.let(::add)
            }
            val sources = ExternalSources.fromUris(selected, data.flags)
            InstallQueue.start(this, sources, origin = InstallerContract.SOURCE_HOME)
        }.onFailure { notify(R.string.home_pick_error) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        history = InstallHistoryStore.get(applicationContext)
        defaults = DefaultInstallerController(this,
            onState = { defaultState = it; renderDefault() },
            onFailure = ::notify,
            onBusy = { defaultBusy = it; renderDefault() },
        )
        LauncherIcons.prepare(this)
        renderPage()
    }

    override fun onStart() {
        super.onStart()
        observing = true
        taskObserver = InstallQueue.observe(::scheduleTasks)
        historyObserver = history.observe { if (observing) renderHistory() }
        renderTasks()
        renderHistory()
    }

    override fun onResume() {
        super.onResume()
        refreshAuthorizers()
        defaults.refresh()
    }

    override fun onStop() {
        observing = false
        taskObserver?.close(); taskObserver = null
        historyObserver?.close(); historyObserver = null
        main.removeCallbacks(redrawTasks); updateQueued = false
        prompt?.dismiss(); prompt = null
        defaults.dismissDialogs()
        super.onStop()
    }

    override fun onDestroy() {
        generation++
        authorizerWork?.cancel(true)
        worker.shutdownNow()
        defaults.close()
        super.onDestroy()
    }

    override fun onAppearanceChanged() {
        prompt?.dismiss(); prompt = null
        defaults.dismissDialogs()
        renderPage()
    }

    private fun renderPage() {
        taskWidgets.clear()
        emptyTasks = null
        ui = SettingsUi(this, kit)
        val page = ui.page(getString(R.string.app_name))
        page.root.tag = "home-root"
        page.toolbar.addView(kit.textButton(getString(R.string.home_more), "home-more") { showMore(page.toolbar) },
            LinearLayout.LayoutParams(-2, -2).apply { marginEnd = kit.dp(12) })
        authorizationCard = card(page.content)
        defaultCard = card(page.content)
        page.content.addView(ui.group(R.string.home_active))
        tasks = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; tag = "home-tasks" }
        page.content.addView(tasks)
        page.content.addView(ui.group(R.string.home_history))
        records = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; tag = "home-history" }
        page.content.addView(records)
        val footer = LinearLayout(this).apply {
            gravity = Gravity.END
            setPaddingRelative(kit.dp(24), kit.dp(8), kit.dp(24), kit.dp(16))
        }
        footer.addView(FloatingActionButton(this).apply {
            tag = "home-pick"
            setImageResource(android.R.drawable.ic_input_add)
            backgroundTintList = ColorStateList.valueOf(kit.palette.primary)
            imageTintList = ColorStateList.valueOf(kit.palette.onPrimary)
            rippleColor = kit.palette.ripple
            contentDescription = getString(R.string.home_pick)
            setOnClickListener { choosePackages() }
            ViewCompat.setTooltipText(this, contentDescription)
        }, LinearLayout.LayoutParams(kit.dp(56), kit.dp(56)))
        page.root.addView(footer, LinearLayout.LayoutParams(-1, -2))
        setContentView(page.root)
        shownHistory = null
        renderAuthorizers(); renderDefault(); renderTasks(); renderHistory()
    }

    private fun card(parent: LinearLayout): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = kit.roundedFill(kit.palette.surface)
        setPaddingRelative(kit.dp(16), kit.dp(12), kit.dp(16), kit.dp(12))
        parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply {
            marginStart = kit.dp(24); marginEnd = kit.dp(24); topMargin = kit.dp(12)
        })
    }

    private fun title(parent: LinearLayout, value: CharSequence) {
        parent.addView(kit.text(value, 16f, medium = true).apply { ViewCompat.setAccessibilityHeading(this, true) })
    }

    private fun renderAuthorizers() {
        if (!::authorizationCard.isInitialized) return
        authorizationCard.removeAllViews()
        title(authorizationCard, getString(R.string.home_authorizers))
        listOf(Authorizer.SHIZUKU, Authorizer.ROOT, Authorizer.DHIZUKU).forEach { method ->
            val state = states[method]
            val name = getString(when (method) { Authorizer.SHIZUKU -> R.string.settings_shizuku; Authorizer.DHIZUKU -> R.string.settings_dhizuku; else -> R.string.settings_root })
            // Root has no separate daemon: the shared API reports running even without su.
            // Mask that sentinel only in Home's display; authorization still uses the raw state.
            val displayedRunning = state?.let { it.running && (method != Authorizer.ROOT || it.available) } == true
            authorizationCard.addView(kit.text(name, 16f).apply { setPaddingRelative(0, kit.dp(12), 0, kit.dp(4)) })
            authorizationCard.addView(kit.text(if (state == null) getString(R.string.home_loading) else getString(
                R.string.home_authorizer_state, yes(state.available), yes(displayedRunning), yes(state.granted)), color = kit.palette.muted)
                .apply { tag = "home-authorizer-state-${method.id}" })
            val download = method == Authorizer.SHIZUKU && state?.available == false
            val label = if (download) R.string.home_get_shizuku else if (state?.usable == true) R.string.home_authorized else R.string.home_authorize
            authorizationCard.addView(kit.textButton(getString(label), "home-authorize-${method.id}") {
                if (download) runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))) }
                    .onFailure { notify(R.string.home_authorize_failed) }
                else requestAuthorization(method)
            }.apply { isEnabled = requesting == null && (download || state?.let { it.available && it.running && !it.granted } == true) })
        }
    }

    private fun refreshAuthorizers() {
        if (requesting != null) return
        val expected = ++generation
        authorizerWork?.cancel(true)
        authorizerWork = worker.submit {
            val result = runCatching { AuthorizerStates.states(applicationContext) }
            main.post {
                if (!isDestroyed && !isFinishing && expected == generation) {
                    states = result.getOrDefault(emptyMap())
                    renderAuthorizers()
                }
            }
        }
    }

    private fun requestAuthorization(method: Authorizer) {
        if (requesting != null) return
        requesting = method
        val expected = ++generation
        renderAuthorizers()
        authorizerWork?.cancel(true)
        authorizerWork = worker.submit {
            val granted = runCatching { AuthorizerStates.request(applicationContext, method, 60_000) }.getOrDefault(false)
            main.post {
                if (!isDestroyed && !isFinishing && expected == generation) {
                    requesting = null
                    if (!granted) notify(R.string.home_authorize_failed)
                    refreshAuthorizers(); defaults.refresh()
                }
            }
        }
    }

    private fun renderDefault() {
        if (!::defaultCard.isInitialized) return
        defaultCard.removeAllViews()
        title(defaultCard, getString(R.string.default_installer_title))
        val state = defaultState
        defaultCard.addView(kit.text(when {
            state == null -> getString(R.string.home_loading)
            state.isSelf -> getString(R.string.default_installer_self)
            else -> state.component ?: getString(R.string.default_installer_none)
        }, color = kit.palette.muted))
        if (state?.requiresClear == true) defaultCard.addView(kit.text(getString(R.string.default_installer_requires_clear), color = kit.palette.muted))
        defaultCard.addView(kit.textButton(getString(if (state?.isSelf == true) R.string.default_installer_clear else R.string.default_installer_set), "home-default-toggle") {
            defaults.setDefault(defaultState?.isSelf != true)
        }.apply { isEnabled = state != null && !defaultBusy })
        defaultCard.addView(kit.textButton(getString(R.string.default_installer_state), "home-default-details") {
            startActivity(Intent(this, DefaultInstallerActivity::class.java))
        })
    }

    private fun scheduleTasks() {
        if (!observing || updateQueued) return
        updateQueued = true
        main.postDelayed(redrawTasks, 150)
    }

    private fun renderTasks(active: List<InstallQueue.Snapshot> = InstallQueue.snapshots()) {
        if (!::tasks.isInitialized) return
        val tokens = active.map { it.token }.toSet()
        taskWidgets.keys.filter { it !in tokens }.forEach { token ->
            taskWidgets.remove(token)?.let { tasks.removeView(it.row) }
        }
        if (active.isEmpty()) {
            if (emptyTasks == null) emptyTasks = ui.caption(getString(R.string.home_empty_active)).also(tasks::addView)
            return
        }
        emptyTasks?.let(tasks::removeView)
        emptyTasks = null
        active.forEachIndexed { position, task ->
            val state = task.state
            val index = state.index.coerceIn(0, state.items.lastIndex)
            val item = state.items[index]
            val widgets = taskWidgets.getOrPut(task.token) { createTaskWidgets(task.token) }
            // Byte-progress updates retain the controls, pressed state and accessibility focus.
            // Only additions, removals or a different task order move a row in the hierarchy.
            if (tasks.getChildAt(position) !== widgets.row) {
                tasks.removeView(widgets.row)
                tasks.addView(widgets.row, position)
            }
            val label = item.metadata?.label ?: item.displayName
            if (widgets.title.text.toString() != label) widgets.title.text = label
            val phase = if (state.prompt != null) getString(R.string.notification_action_required) else stage(state.stage)
            val summary = getString(R.string.home_queue_progress, index + 1, state.items.size, phase)
            if (widgets.stage.text.toString() != summary) widgets.stage.text = summary
            val indeterminate = state.stage != InstallerContract.STAGE_WRITING
            if (widgets.progress.isIndeterminate != indeterminate) {
                // Material progress indicators require hiding before switching to indeterminate.
                val visibility = widgets.progress.visibility
                widgets.progress.visibility = View.INVISIBLE
                widgets.progress.isIndeterminate = indeterminate
                widgets.progress.visibility = visibility
            }
            if (!indeterminate) {
                val percentage = (state.progress.coerceIn(0f, 1f) * 100).toInt()
                if (widgets.progress.progress != percentage) widgets.progress.progress = percentage
            }
        }
    }

    private fun createTaskWidgets(token: String): TaskWidgets {
        val row = card(tasks).apply { tag = "home-task-$token" }
        val label = kit.text("", 16f, medium = true).apply {
            tag = "home-task-title-$token"
            ViewCompat.setAccessibilityHeading(this, true)
        }
        val stage = kit.text("", color = kit.palette.muted).apply { tag = "home-task-stage-$token" }
        val progress = kit.progressBar().apply { tag = "home-task-progress-$token" }
        row.addView(label)
        row.addView(stage)
        row.addView(progress, LinearLayout.LayoutParams(-1, kit.dp(8)).apply { topMargin = kit.dp(8); bottomMargin = kit.dp(8) })
        row.addView(kit.textButton(getString(R.string.install_open), "home-open-$token") {
            if (!InstallQueue.open(this, token)) notify(R.string.home_task_unavailable)
        })
        row.addView(kit.textButton(getString(R.string.home_cancel_queue), "home-cancel-$token", danger = true) { InstallQueue.cancel(token) })
        return TaskWidgets(row, label, stage, progress)
    }

    private fun renderHistory() {
        if (!::records.isInitialized) return
        val values = history.list().filter { it.terminal }
        if (shownHistory == values) return
        shownHistory = values
        records.removeAllViews()
        if (values.isEmpty()) records.addView(ui.caption(getString(R.string.home_empty_history)))
        values.forEach { entry ->
            val row = card(records)
            row.tag = "home-history-${entry.id}"
            title(row, entry.label.ifBlank { entry.packageName ?: getString(R.string.install_unknown) })
            row.addView(kit.text(listOfNotNull(entry.packageName, versions(entry), stage(entry.result)).joinToString("\n"), color = kit.palette.muted))
            row.addView(kit.text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.finishedAt ?: entry.updatedAt)), color = kit.palette.muted))
            row.isClickable = true; row.isFocusable = true
            row.contentDescription = listOfNotNull(entry.label, entry.packageName, stage(entry.result)).joinToString(", ")
            ViewCompat.setScreenReaderFocusable(row, true)
            row.setOnClickListener { historyDetails(entry) }
        }
    }

    private fun historyDetails(entry: InstallHistoryEntry) {
        prompt?.dismiss()
        prompt = ui.dialog(entry.label) { layout, dialog ->
            val origin = when (entry.origin) {
                InstallerContract.SOURCE_SCRIPT -> R.string.home_origin_script
                InstallerContract.SOURCE_EXTERNAL -> R.string.home_origin_external
                InstallerContract.SOURCE_HOME -> R.string.home_origin_home
                else -> R.string.home_origin_host
            }
            listOfNotNull(entry.packageName, versions(entry), stage(entry.result),
                getString(R.string.home_origin, getString(origin)),
                getString(R.string.confirm_system).takeIf { entry.authorizer == InstallerContract.AUTHORIZER_NONE } ?: entry.authorizer,
                getString(R.string.home_history_time, DateFormat.getDateTimeInstance().format(Date(entry.finishedAt ?: entry.updatedAt))),
                getString(R.string.install_interrupted_explanation).takeIf { entry.interrupted },
                entry.errorCode?.let { getString(R.string.install_error_code, it) }, entry.systemMessage,
            ).forEach { layout.content.addView(kit.text(it).apply { setTextIsSelectable(true); setPadding(0, kit.dp(4), 0, kit.dp(4)) }) }
            layout.actions.addView(kit.textButton(getString(R.string.install_done)) { dialog.dismiss() })
            layout.actions.addView(kit.textButton(getString(R.string.home_history_delete), danger = true) {
                dialog.dismiss()
                confirm(R.string.home_history_delete, R.string.home_history_delete_confirm) { history.remove(entry.id, ::historyWritten) }
            })
        }
    }

    private fun showMore(anchor: View) {
        val menu = PopupMenu(this, anchor, Gravity.END)
        menu.menu.add(0, 1, 0, R.string.apps_title)
        menu.menu.add(0, 2, 1, R.string.settings_title)
        menu.menu.add(0, 3, 2, R.string.home_history_clear).isEnabled = history.list().isNotEmpty()
        menu.setOnMenuItemClickListener {
            when (it.itemId) {
                1 -> startActivity(Intent(this, InstalledAppsActivity::class.java))
                2 -> startActivity(Intent(this, SettingsActivity::class.java))
                3 -> confirm(R.string.home_history_clear, R.string.home_history_clear_confirm) { history.clear(::historyWritten) }
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
        menu.show()
    }

    private fun confirm(title: Int, message: Int, action: () -> Unit) {
        prompt?.dismiss()
        prompt = ui.dialog(getString(title)) { layout, dialog ->
            layout.content.addView(kit.text(getString(message)))
            layout.actions.addView(kit.textButton(getString(R.string.action_cancel)) { dialog.cancel() })
            layout.actions.addView(kit.textButton(getString(R.string.settings_confirm), danger = true) { action(); dialog.dismiss() })
        }
    }

    private fun historyWritten(saved: Boolean) { if (!saved && !isDestroyed && !isFinishing) notify(R.string.home_history_write_failed) }
    private fun yes(value: Boolean) = getString(if (value) R.string.home_yes else R.string.home_no)
    private fun notify(message: Int) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    private fun versions(entry: InstallHistoryEntry): String? {
        val current = entry.versionName ?: entry.versionCode?.toString() ?: return null
        val previous = entry.previousVersionName ?: entry.previousVersionCode?.toString() ?: return current
        return getString(R.string.home_version_change, previous, current)
    }
    private fun stage(value: String): String = getString(when (value) {
        InstallerContract.STAGE_PREPARING -> R.string.install_preparing
        InstallerContract.STAGE_CONFIRMING -> R.string.install_waiting_system
        InstallerContract.STAGE_WRITING -> R.string.install_writing
        InstallerContract.STAGE_COMMITTING -> R.string.install_committing
        InstallerContract.STAGE_COMPLETED -> R.string.install_success
        InstallerContract.STAGE_FAILED -> R.string.install_failed
        InstallerContract.STAGE_CANCELLED -> R.string.install_cancelled
        else -> R.string.install_pending
    })

    private fun choosePackages() {
        runCatching { picker.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            putExtra(Intent.EXTRA_MIME_TYPES, PackageEntryTypes.MIME_TYPES)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }) }.onFailure { notify(R.string.home_pick_error) }
    }
}
