package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.supermonster003.autojs6.plugin.three.setup.installer.R
import io.github.supermonster003.autojs6.plugin.three.setup.installer.installedapps.InstalledApp
import io.github.supermonster003.autojs6.plugin.three.setup.installer.installedapps.InstalledAppsFilter
import io.github.supermonster003.autojs6.plugin.three.setup.installer.installedapps.InstalledAppsQuery
import io.github.supermonster003.autojs6.plugin.three.setup.installer.installedapps.InstalledAppsRepository
import io.github.supermonster003.autojs6.plugin.three.setup.installer.installedapps.InstalledAppsSort
import io.github.supermonster003.autojs6.plugin.three.setup.installer.installedapps.InstalledAppsUninstaller
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.appearance.HostAppearanceActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.kit.InstallerColorPolicy
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.io.Closeable
import java.lang.ref.WeakReference
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.FutureTask
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** A current-user application browser. Expanding a row never starts an uninstall. */
class InstalledAppsActivity : HostAppearanceActivity() {
    override val dialogTheme = false
    private lateinit var repository: InstalledAppsRepository
    private lateinit var uninstaller: InstalledAppsUninstaller
    private val worker = ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, ArrayBlockingQueue(1))
    private val generation = AtomicInteger()
    private val main = Handler(Looper.getMainLooper())
    private var pending: FutureTask<Unit>? = null
    private var catalog: List<InstalledApp>? = null
    private var visibleApps = emptyList<InstalledApp>()
    private var filter = InstalledAppsFilter()
    private var expandedPackage: String? = null
    private var loading = true
    private var loadFailed = false
    private var pageStarted = false
    private var result: Pair<Int, String>? = null
    private var resultErrorCode: String? = null
    private var sortDialog: Dialog? = null
    private var listState: Parcelable? = null
    private lateinit var list: ListView
    private lateinit var adapter: AppsAdapter
    private lateinit var status: TextView
    private lateinit var operationStatus: TextView
    private lateinit var retry: View
    private lateinit var progress: View
    private lateinit var search: EditText

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = InstalledAppsRepository(applicationContext)
        uninstaller = InstalledAppsUninstaller(applicationContext)
        filter = InstalledAppsFilter(savedInstanceState?.getString(STATE_QUERY).orEmpty(),
            InstalledAppsSort.restore(savedInstanceState?.getString(STATE_SORT)),
            savedInstanceState?.getBoolean(STATE_SYSTEM) ?: false)
        expandedPackage = savedInstanceState?.getString(STATE_EXPANDED)
        listState = savedInstanceState?.getParcelable(STATE_LIST)
        savedInstanceState?.getString(STATE_RESULT_LABEL)?.let { label ->
            result = (if (savedInstanceState.getBoolean(STATE_UNINSTALLING)) R.string.apps_uninstall_interrupted
                else savedInstanceState.getInt(STATE_RESULT)) to label
            resultErrorCode = savedInstanceState.getString(STATE_ERROR)
        }
        render()
    }

    override fun onResume() {
        super.onResume()
        refresh(reload = true)
    }

    override fun onStart() {
        super.onStart()
        pageStarted = true
    }

    override fun onStop() {
        pageStarted = false
        cancelListWork()
        if (::adapter.isInitialized) adapter.cancelIcons()
        super.onStop()
    }

    override fun onAppearanceChanged() {
        if (!::repository.isInitialized) return
        listState = list.onSaveInstanceState()
        val focused = search.hasFocus()
        render()
        if (focused) { search.requestFocus(); search.setSelection(search.length()) }
        refresh(reload = true)
    }

    private fun render() {
        sortDialog?.dismiss()
        if (::adapter.isInitialized) adapter.cancelIcons()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = resources.configuration.layoutDirection
            setBackgroundColor(kit.palette.background)
        }
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPaddingRelative(kit.dp(12), kit.dp(4), kit.dp(24), kit.dp(4))
        }
        heading.addView(kit.textButton(getString(R.string.apps_back)) { finish() })
        heading.addView(kit.text(getString(R.string.apps_title), 20f, medium = true).apply {
            ViewCompat.setAccessibilityHeading(this, true)
            setPaddingRelative(kit.dp(8), kit.dp(8), 0, kit.dp(8))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(heading)

        // Filters are part of the scrollable list so they remain reachable with the IME, large
        // fonts, landscape and narrow multi-window viewports without squeezing rows to zero height.
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPaddingRelative(kit.dp(24), kit.dp(8), kit.dp(24), kit.dp(12))
        }
        search = EditText(this).apply {
            tag = TAG_SEARCH
            hint = getString(R.string.apps_search)
            contentDescription = getString(R.string.apps_search)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            isSingleLine = true
            textSize = 16f
            minHeight = kit.dp(56)
            setTextColor(kit.palette.text)
            setHintTextColor(kit.palette.muted)
            backgroundTintList = ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_focused), intArrayOf()),
                intArrayOf(kit.palette.disabledText, kit.palette.accent, kit.palette.outline))
            highlightColor = InstallerColorPolicy.withAlpha(kit.palette.accent, 0x55)
            if (Build.VERSION.SDK_INT >= 29) textCursorDrawable = textCursorDrawable?.mutate()?.apply { setTint(kit.palette.accent) }
            setText(filter.query)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(value: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(value: CharSequence?, start: Int, before: Int, count: Int) {
                    filter = filter.copy(query = value?.toString().orEmpty())
                    refresh(reload = false)
                }
                override fun afterTextChanged(value: Editable?) = Unit
            })
        }
        controls.addView(search, fillWidth())
        controls.addView(kit.textButton(getString(R.string.apps_sort, getString(sortLabel(filter.sort))), TAG_SORT) {
            showSortDialog()
        }, fillWidth())
        controls.addView(kit.switch(getString(R.string.apps_show_system), filter.showSystem, TAG_SYSTEM) { checked ->
            filter = filter.copy(showSystem = checked)
            refresh(reload = false)
        }, fillWidth())
        progress = kit.progressBar().apply { isIndeterminate = true }
        controls.addView(progress, fillWidth())
        status = kit.text(null, color = kit.palette.muted).apply {
            tag = TAG_STATUS
            setPaddingRelative(0, kit.dp(12), 0, kit.dp(4))
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        controls.addView(status, fillWidth())
        retry = kit.textButton(getString(R.string.install_retry)) { refresh(reload = true) }
        controls.addView(retry, fillWidth())
        operationStatus = kit.text(null, color = kit.palette.muted).apply {
            tag = TAG_OPERATION
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            setPaddingRelative(0, kit.dp(8), 0, kit.dp(4))
        }
        controls.addView(operationStatus, fillWidth())

        adapter = AppsAdapter()
        list = ListView(this).apply {
            tag = TAG_LIST
            divider = null
            dividerHeight = 0
            clipToPadding = false
            setItemsCanFocus(true)
            addHeaderView(controls, null, false)
            adapter = this@InstalledAppsActivity.adapter
            setRecyclerListener { recycled -> (recycled.tag as? AppRow)?.cancelIcon() }
        }
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        setContentView(root)
        ViewCompat.requestApplyInsets(root)
        adapter.notifyDataSetChanged()
        showStatus()
    }

    private fun refresh(reload: Boolean) {
        if (!pageStarted || isFinishing || isDestroyed) return
        cancelListWork()
        val expected = generation.get()
        val selectedFilter = filter
        val locale = resources.configuration.locales[0]
        val previous = catalog.takeUnless { reload }
        loading = true
        loadFailed = false
        showStatus()
        val owner = WeakReference(this)
        val source = repository
        val gate = generation
        val handler = main
        val task = FutureTask<Unit> {
            val checkActive = {
                if (Thread.currentThread().isInterrupted || gate.get() != expected) throw InterruptedException()
            }
            try {
                val loaded = previous ?: source.load(checkActive)
                val selected = InstalledAppsQuery.apply(loaded, selectedFilter, locale, checkActive)
                checkActive()
                handler.post {
                    owner.get()?.takeIf { !it.isDestroyed && !it.isFinishing && gate.get() == expected }?.let { activity ->
                        activity.catalog = loaded
                        activity.visibleApps = selected
                        activity.loading = false
                        activity.adapter.notifyDataSetChanged()
                        activity.listState?.let(activity.list::onRestoreInstanceState)
                        activity.listState = null
                        activity.showStatus()
                    }
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (_: Exception) {
                handler.post {
                    owner.get()?.takeIf { !it.isDestroyed && !it.isFinishing && gate.get() == expected }?.let { activity ->
                        activity.loading = false
                        activity.loadFailed = true
                        activity.showStatus()
                    }
                }
            }
        }
        pending = task
        worker.execute(task)
    }

    private fun cancelListWork() {
        generation.incrementAndGet()
        pending?.let { it.cancel(true); worker.remove(it) }
        pending = null
    }

    private fun showStatus() {
        if (!::status.isInitialized) return
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        retry.visibility = if (loadFailed) View.VISIBLE else View.GONE
        status.text = when {
            loading -> getString(R.string.apps_loading)
            loadFailed -> getString(R.string.apps_load_failed)
            visibleApps.isEmpty() -> getString(R.string.apps_empty)
            else -> getString(R.string.apps_count, visibleApps.size)
        }
        operationStatus.visibility = if (result == null) View.GONE else View.VISIBLE
        operationStatus.text = result?.let { (message, label) ->
            val text = getString(message, label)
            resultErrorCode?.let { "$text\n${getString(R.string.install_error_code, it)}" } ?: text
        }
        operationStatus.setTextColor(if (resultErrorCode == null) kit.palette.muted else kit.palette.danger)
    }

    private fun showSortDialog() {
        sortDialog?.dismiss()
        var choice = filter.sort
        val layout = kit.dialog(getString(R.string.apps_sort_title))
        val buttons = mutableListOf<Pair<InstalledAppsSort, RadioButton>>()
        InstalledAppsSort.entries.forEach { value ->
            val radio = RadioButton(this).apply {
                tag = "$TAG_SORT_OPTION${value.name}"
                text = getString(sortLabel(value))
                textSize = 16f
                setTextColor(kit.palette.text)
                buttonTintList = kit.choiceTint()
                isChecked = value == choice
                minHeight = kit.dp(56)
                setPaddingRelative(kit.dp(4), kit.dp(8), kit.dp(4), kit.dp(8))
                background = RippleDrawable(ColorStateList.valueOf(kit.palette.ripple), null, kit.roundedFill(Color.WHITE, 8))
                setOnClickListener {
                    choice = value
                    buttons.forEach { (candidate, view) -> view.isChecked = candidate == choice }
                }
            }
            buttons += value to radio
            layout.content.addView(radio, fillWidth())
        }
        val dialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(layout.root)
            setCancelable(true)
            setCanceledOnTouchOutside(true)
        }
        layout.root.setOnClickListener { dialog.cancel() }
        layout.surface.isClickable = true
        layout.actions.addView(kit.textButton(getString(R.string.action_cancel)) { dialog.cancel() })
        layout.actions.addView(kit.textButton(getString(R.string.apps_confirm), TAG_SORT_CONFIRM) {
            filter = filter.copy(sort = choice)
            dialog.dismiss()
            list.findViewWithTag<TextView>(TAG_SORT)?.text = getString(R.string.apps_sort, getString(sortLabel(choice)))
            refresh(reload = false)
        })
        sortDialog = dialog
        dialog.setOnDismissListener { if (sortDialog === dialog) sortDialog = null }
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        ViewCompat.requestApplyInsets(layout.root)
    }

    private fun uninstall(app: InstalledApp) {
        if (uninstaller.start(app.packageName) { failure ->
            resultErrorCode = failure?.code?.takeUnless { it == InstallerErrorCodes.USER_CANCELLED || it == InstallerErrorCodes.CANCELLED }
            result = when (failure?.code) {
                null -> R.string.apps_uninstall_success
                InstallerErrorCodes.USER_CANCELLED, InstallerErrorCodes.CANCELLED -> R.string.apps_uninstall_cancelled
                else -> R.string.apps_uninstall_failed
            } to app.label
            if (pageStarted) adapter.notifyDataSetChanged()
            showStatus()
            refresh(reload = true)
        }) {
            result = R.string.apps_uninstalling to app.label
            resultErrorCode = null
            adapter.notifyDataSetChanged()
            showStatus()
        }
    }

    private fun open(app: InstalledApp) {
        val intent = runCatching { packageManager.getLaunchIntentForPackage(app.packageName) }.getOrNull()
        if (intent == null) actionUnavailable(app, R.string.install_open_unavailable)
        else launch(intent, app)
    }

    private fun info(app: InstalledApp) = launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", app.packageName, null)), app)

    private fun launch(intent: Intent, app: InstalledApp) {
        try { startActivity(intent) }
        catch (_: Exception) { actionUnavailable(app, R.string.apps_action_failed) }
    }

    private fun actionUnavailable(app: InstalledApp, message: Int) {
        result = message to app.label
        resultErrorCode = null
        showStatus()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_QUERY, filter.query)
        outState.putString(STATE_SORT, filter.sort.name)
        outState.putBoolean(STATE_SYSTEM, filter.showSystem)
        outState.putString(STATE_EXPANDED, expandedPackage)
        outState.putParcelable(STATE_LIST, list.onSaveInstanceState())
        result?.let { (message, label) ->
            outState.putInt(STATE_RESULT, message)
            outState.putString(STATE_RESULT_LABEL, label)
            outState.putString(STATE_ERROR, resultErrorCode)
            outState.putBoolean(STATE_UNINSTALLING, uninstaller.isBusy)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        sortDialog?.dismiss()
        cancelListWork()
        worker.shutdownNow()
        main.removeCallbacksAndMessages(null)
        if (::adapter.isInitialized) adapter.cancelIcons()
        if (::repository.isInitialized) repository.close()
        if (::uninstaller.isInitialized) uninstaller.close()
        super.onDestroy()
    }

    private inner class AppsAdapter : BaseAdapter() {
        private val rows = mutableSetOf<AppRow>()
        override fun getCount() = visibleApps.size
        override fun getItem(position: Int) = visibleApps[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = (convertView?.tag as? AppRow) ?: AppRow().also { rows += it }
            row.bind(getItem(position))
            return row.root
        }
        fun cancelIcons() = rows.forEach(AppRow::cancelIcon)
    }

    private inner class AppRow {
        val root = LinearLayout(this@InstalledAppsActivity).apply { orientation = LinearLayout.VERTICAL; tag = this@AppRow }
        private val header = LinearLayout(this@InstalledAppsActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = kit.dp(72)
            setPaddingRelative(kit.dp(24), kit.dp(12), kit.dp(24), kit.dp(12))
            background = RippleDrawable(ColorStateList.valueOf(kit.palette.ripple), null, ColorDrawable(Color.WHITE))
            isFocusable = true
            accessibilityDelegate = object : View.AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.className = Button::class.java.name
                }
            }
        }
        private val icon = ImageView(this@InstalledAppsActivity).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        private val chevron = ImageView(this@InstalledAppsActivity).apply {
            setImageResource(R.drawable.ic_settings_chevron)
            imageTintList = ColorStateList.valueOf(kit.palette.muted)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        private val label = kit.text(null, 16f)
        private val packageLabel = kit.text(null, color = kit.palette.muted)
        private val version = kit.text(null, color = kit.palette.muted)
        private val actions = LinearLayout(this@InstalledAppsActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPaddingRelative(kit.dp(64), 0, kit.dp(24), kit.dp(8))
        }
        private var app: InstalledApp? = null
        private var iconRequest: Closeable? = null
        private val remove = kit.textButton(getString(R.string.action_uninstall), danger = true) { app?.let(::uninstall) }

        init {
            header.addView(icon, LinearLayout.LayoutParams(kit.dp(40), kit.dp(40)).apply { marginEnd = kit.dp(16) })
            header.addView(LinearLayout(this@InstalledAppsActivity).apply {
                orientation = LinearLayout.VERTICAL
                listOf(label, packageLabel, version).forEach { text ->
                    text.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    addView(text, fillWidth())
                }
                packageLabel.setPaddingRelative(0, kit.dp(4), 0, 0)
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            header.addView(chevron, LinearLayout.LayoutParams(kit.dp(24), kit.dp(24)).apply { marginStart = kit.dp(16) })
            header.setOnClickListener {
                app?.let { value ->
                    expandedPackage = value.packageName.takeUnless { it == expandedPackage }
                    adapter.notifyDataSetChanged()
                }
            }
            actions.addView(remove, fillWidth())
            actions.addView(kit.textButton(getString(R.string.install_open)) { app?.let(::open) }, fillWidth())
            actions.addView(kit.textButton(getString(R.string.apps_details)) { app?.let(::info) }, fillWidth())
            root.addView(header, fillWidth())
            root.addView(actions, fillWidth())
            root.addView(View(this@InstalledAppsActivity).apply { setBackgroundColor(kit.palette.divider) },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, kit.dp(1)).apply { marginStart = kit.dp(64) })
        }

        fun bind(value: InstalledApp) {
            cancelIcon()
            app = value
            header.tag = "$TAG_ROW${value.packageName}"
            label.text = value.label
            packageLabel.text = value.packageName
            version.text = getString(R.string.install_version, value.versionName?.takeIf { it.isNotBlank() }
                ?: getString(R.string.install_unknown), value.versionCode)
            val expanded = expandedPackage == value.packageName
            chevron.rotation = if (expanded) {
                if (resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) -90f else 90f
            } else 0f
            header.contentDescription = "${label.text}\n${packageLabel.text}\n${version.text}"
            ViewCompat.setStateDescription(header, getString(if (expanded) R.string.apps_actions_collapse else R.string.apps_actions_expand))
            actions.visibility = if (expanded) View.VISIBLE else View.GONE
            remove.isEnabled = !uninstaller.isBusy
            val cached = repository.cachedIcon(value)
            icon.setImageBitmap(cached)
            if (cached == null && pageStarted) iconRequest = repository.requestIcon(value) { loaded ->
                if (app == value) icon.setImageBitmap(loaded)
            }
        }

        fun cancelIcon() { iconRequest?.close(); iconRequest = null }
    }

    private fun fillWidth() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    private fun sortLabel(sort: InstalledAppsSort) = when (sort) {
        InstalledAppsSort.NAME -> R.string.apps_sort_name
        InstalledAppsSort.INSTALLED -> R.string.apps_sort_installed
        InstalledAppsSort.UPDATED -> R.string.apps_sort_updated
    }

    companion object {
        const val TAG_SEARCH = "apps_search"
        const val TAG_SORT = "apps_sort"
        const val TAG_SORT_OPTION = "apps_sort_option_"
        const val TAG_SORT_CONFIRM = "apps_sort_confirm"
        const val TAG_SYSTEM = "apps_system"
        const val TAG_STATUS = "apps_status"
        const val TAG_OPERATION = "apps_operation"
        const val TAG_LIST = "apps_list"
        const val TAG_ROW = "apps_row_"
        private const val STATE_QUERY = "apps.query"
        private const val STATE_SORT = "apps.sort"
        private const val STATE_SYSTEM = "apps.system"
        private const val STATE_EXPANDED = "apps.expanded"
        private const val STATE_LIST = "apps.list"
        private const val STATE_RESULT = "apps.result"
        private const val STATE_RESULT_LABEL = "apps.resultLabel"
        private const val STATE_ERROR = "apps.error"
        private const val STATE_UNINSTALLING = "apps.uninstalling"
    }
}
