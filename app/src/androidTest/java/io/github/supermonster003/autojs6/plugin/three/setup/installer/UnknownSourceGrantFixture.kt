package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.annotation.TargetApi
import android.app.ActivityManager
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.UserActionActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.UserActionBridge
import org.junit.Assume.assumeTrue
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArrayList

/** Changes only this plugin's REQUEST_INSTALL_PACKAGES modes; allowance is granted only by UI. */
@TargetApi(26)
internal class UnknownSourcePermissionState(private val context: Context) : Closeable {
    data class Modes(val packageMode: String, val uidMode: String)
    private val userId = Process.myUid() / 100000
    private val packageName = context.packageName
    val original = read()
    private val originallyAllowed = context.packageManager.canRequestPackageInstalls()
    private var touched = false
    private val restoreMode = InstrumentationRegistry.getArguments().getString("unknownSourcePermissionRestore")
    private val restoreByDriver = restoreMode == "driver"

    init {
        check(packageName == ThreeSetupInstallerPlugin.PACKAGE_NAME)
        check(restoreMode == null || restoreMode == "driver") { "Unsupported unknown-source permission restoration owner" }
        check(context.packageManager.getPackagesForUid(Process.myUid())?.toSet() == setOf(packageName)) {
            "Refusing to change an app-op for a UID shared with another package"
        }
        val owner = if (restoreByDriver) "driver precondition" else "saved permission"
        evidence("$owner modes: user=$userId package=${original.packageMode} uid=${original.uidMode} allowed=$originallyAllowed")
    }

    fun denyForTest() {
        touched = true
        if (restoreByDriver) {
            check(read() == Modes("deny", "default") && !context.packageManager.canRequestPackageInstalls()) {
                "The external driver must establish the denied precondition before starting instrumentation"
            }
            return
        }
        // A UID override would otherwise supersede the Settings page's per-package toggle.
        if (original.uidMode != "default") setMode(uid = true, mode = "default")
        setMode(uid = false, mode = "deny")
        check(read() == Modes("deny", "default")) { "Could not establish the exact denied test precondition" }
        check(!context.packageManager.canRequestPackageInstalls()) { "The denied test precondition is not effective" }
    }

    fun assertGrantedBySettings() {
        check(context.packageManager.canRequestPackageInstalls()) { "The Settings switch did not grant installation permission" }
        check(read() == Modes("allow", "default")) { "Settings did not grant this plugin's package-level app-op" }
    }

    override fun close() {
        if (!touched) return
        if (restoreByDriver) {
            evidence("permission restoration delegated to the external driver after instrumentation exits")
            touched = false
            return
        }
        var failure: Throwable? = null
        fun restore(action: () -> Unit) {
            try { action() } catch (problem: Throwable) {
                if (failure == null) failure = problem else failure!!.addSuppressed(problem)
            }
        }
        restore { setMode(uid = false, mode = original.packageMode) }
        restore { setMode(uid = true, mode = original.uidMode) }
        restore { check(read() == original) { "REQUEST_INSTALL_PACKAGES modes were not restored exactly: expected=$original actual=${read()}" } }
        restore { check(context.packageManager.canRequestPackageInstalls() == originallyAllowed) { "Effective unknown-source permission was not restored" } }
        failure?.let { throw it }
        touched = false
        evidence("restored permission modes: user=$userId package=${original.packageMode} uid=${original.uidMode} allowed=$originallyAllowed")
    }

    private fun setMode(uid: Boolean, mode: String) {
        check(mode in MODES)
        val reply = shell("cmd appops set --user $userId ${if (uid) "--uid " else ""}$packageName REQUEST_INSTALL_PACKAGES $mode")
        check(reply.isBlank()) { "Unexpected app-op update response: $reply" }
    }

    private fun read(): Modes {
        val reply = shell("cmd appops get --user $userId $packageName REQUEST_INSTALL_PACKAGES")
        val pattern = Regex("^(Uid mode: )?REQUEST_INSTALL_PACKAGES: (allow|ignore|deny|default|foreground)(?:[; ].*)?$", RegexOption.MULTILINE)
        val matches = pattern.findAll(reply.trim()).toList()
        check(matches.isNotEmpty() || reply.trim().startsWith("No operations.")) { "Cannot safely read installation app-ops: $reply" }
        val packageModes = matches.filter { it.groupValues[1].isEmpty() }.map { it.groupValues[2] }
        val uidModes = matches.filter { it.groupValues[1].isNotEmpty() }.map { it.groupValues[2] }
        check(packageModes.size <= 1 && uidModes.size <= 1) { "Ambiguous installation app-op modes: $reply" }
        // REQUEST_INSTALL_PACKAGES has MODE_DEFAULT as its framework default on API 26+.
        // Access/rejection timestamps can change during this test; only permission modes are restored.
        return Modes(packageModes.singleOrNull() ?: "default", uidModes.singleOrNull() ?: "default")
    }

    companion object {
        private val MODES = setOf("allow", "ignore", "deny", "default", "foreground")
        private fun shell(command: String): String = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).use {
            ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().use { reader -> reader.readText() }
        }
        fun evidence(message: String) = InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
            putString("unknown-source-grant", message)
        })
    }
}

/** Observe starts without blocking or returning a synthetic Activity result. */
@TargetApi(26)
internal class UnknownSourceGrantTrace(private val context: Context) : Instrumentation.ActivityMonitor(), Closeable {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val installer = context.packageManager.packageInstaller
    val createdSessions = CopyOnWriteArrayList<Int>()
    val finishedSessions = CopyOnWriteArrayList<Pair<Int, Boolean>>()
    val bridgeTokens = CopyOnWriteArrayList<String>()
    val settingsPackages = CopyOnWriteArrayList<String>()
    val confirmationSessions = CopyOnWriteArrayList<Int>()
    private val callback = object : PackageInstaller.SessionCallback() {
        override fun onCreated(sessionId: Int) { createdSessions += sessionId }
        override fun onBadgingChanged(sessionId: Int) = Unit
        override fun onActiveChanged(sessionId: Int, active: Boolean) = Unit
        override fun onProgressChanged(sessionId: Int, progress: Float) = Unit
        override fun onFinished(sessionId: Int, success: Boolean) { finishedSessions += sessionId to success }
    }

    init {
        check(installer.mySessions.isEmpty()) { "Finish other plugin installation sessions before this opt-in test" }
        installer.registerSessionCallback(callback, Handler(Looper.getMainLooper()))
        instrumentation.addMonitor(this)
    }

    override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
        if (intent.component?.className == UserActionActivity::class.java.name) {
            intent.getStringExtra(UserActionBridge.EXTRA_TOKEN)?.let(bridgeTokens::add)
        }
        if (intent.action == Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES) settingsPackages += intent.data?.toString().orEmpty()
        if (intent.hasExtra(PackageInstaller.EXTRA_SESSION_ID)) {
            confirmationSessions += intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        }
        return null
    }

    fun originalSession(): PackageInstaller.SessionInfo {
        check(createdSessions.size == 1) { "Expected exactly one platform session, found $createdSessions" }
        return requireNotNull(installer.getSessionInfo(createdSessions.single())).also {
            check(it.appPackageName == FixtureInstallUi.PACKAGE_NAME) { "The platform session is not the fixed fixture" }
            check(it.installerPackageName == context.packageName) { "The platform session is not owned by this plugin" }
        }
    }

    fun settleOwnedFixtureSessions() {
        val deadline = SystemClock.elapsedRealtime() + 10_000
        while (SystemClock.elapsedRealtime() < deadline) {
            val owned = installer.mySessions.filter {
                it.appPackageName == FixtureInstallUi.PACKAGE_NAME && it.installerPackageName == context.packageName
            }
            owned.forEach { info -> runCatching { installer.abandonSession(info.sessionId) } }
            // Await the platform finish callback before package-ownership cleanup, so a late
            // successful commit cannot install the fixture after the cleanup's package query.
            if (owned.isEmpty() && createdSessions.all { id -> finishedSessions.any { it.first == id } }) return
            SystemClock.sleep(50)
        }
        error("The owned fixture session did not settle before test cleanup: created=$createdSessions finished=$finishedSessions")
    }

    override fun close() {
        installer.unregisterSessionCallback(callback)
        instrumentation.removeMonitor(this)
    }
}

/** Only known standard Settings controls inside the task rooted at this test's live bridge. */
internal class OwnedUnknownSourceUi(
    private val context: Context,
    private val bridgeToken: String,
    private val fixtureLabel: String = FixtureInstallUi.LABEL,
) {
    init { require(fixtureLabel in setOf(FixtureInstallUi.LABEL, "3-Setup Core Fixture")) }
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val manager = context.getSystemService(ActivityManager::class.java)
    private var scanLookupReported = false
    private var safeScanLookupReported = false

    fun settingsTask(): ActivityManager.AppTask? = task()?.takeIf {
        val top = it.info()?.topActivity
        top != null && top.packageName == SETTINGS && top.className in SETTINGS_ACTIVITIES
    }

    fun hasSettingsChild(): Boolean = task()?.info()?.topActivity?.let {
        it.className != UserActionActivity::class.java.name
    } == true

    fun additionalOemPermissionComponent(): String? = task()?.info()?.topActivity?.takeIf {
        it.packageName == "com.miui.securitycenter" &&
            it.className == "com.miui.permcenter.privacymanager.SpecialPermissionInterceptActivity"
    }?.flattenToShortString()

    fun clickAllowFromThisSource(): Boolean {
        if (settingsTask() == null) return false
        val root = instrumentation.uiAutomation.rootInActiveWindow ?: return false
        if (root.packageName?.toString() != SETTINGS) return false
        val switches = SWITCH_IDS.flatMap(root::findAccessibilityNodeInfosByViewId)
            .distinct().filter { it.isVisibleToUser && it.isCheckable && it.className?.toString() == "android.widget.Switch" }
        check(switches.size <= 1) { "Ambiguous Settings switches; refusing to click any of them" }
        val control = switches.singleOrNull() ?: return false
        check(!control.isChecked) { "The unknown-source switch was already enabled before this test's UI action" }
        check(control.isEnabled) { "The unknown-source switch is disabled by device policy" }
        var clickable: AccessibilityNodeInfo? = control
        repeat(3) {
            val current = clickable ?: return@repeat
            if (current.isEnabled && current.isVisibleToUser && current.isClickable && current.packageName?.toString() == SETTINGS) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            clickable = current.parent
        }
        return false
    }

    fun requireRecognizedSettings() {
        val top = task()?.info()?.topActivity
        assumeTrue("Unsupported Settings component for the opt-in grant driver: $top", top != null && top.packageName == SETTINGS && top.className in SETTINGS_ACTIVITIES)
    }

    /** Called once before an unsupported switch skips; never records node text or descriptions. */
    fun reportSettingsCheckables() {
        if (settingsTask() == null) return
        val root = instrumentation.uiAutomation.rootInActiveWindow ?: return
        if (root.packageName?.toString() != SETTINGS) return
        val pending = java.util.ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        pending.addLast(root to 0)
        val fields = mutableListOf<String>()
        var requested = 1
        var visited = 0
        while (pending.isNotEmpty()) {
            val (node, depth) = pending.removeLast()
            visited++
            if (node.isCheckable && node.packageName?.toString() == SETTINGS) {
                fields += "resourceId=${node.viewIdResourceName} className=${node.className} " +
                    "enabled=${node.isEnabled} checked=${node.isChecked} visible=${node.isVisibleToUser}"
            }
            if (depth == 32) continue
            for (index in 0 until node.childCount) {
                if (requested >= 512) break
                requested++
                node.getChild(index)?.let { pending.addLast(it to depth + 1) }
            }
        }
        UnknownSourcePermissionState.evidence("Settings checkable fields: nodes=$visited " + fields.joinToString(" | ").ifEmpty { "none" })
    }

    fun canApproveFixture(): Boolean {
        val owner = task()?.info()?.topActivity?.packageName
        return owner in INSTALLERS && instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString() == owner
    }

    fun canReturnFromSettings(): Boolean = settingsTask() != null &&
        instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString() == SETTINGS

    /** Explicit opt-in may scan this code-free fixture; no warning or harmful-app block is bypassed. */
    fun handleFixedFixtureScan(acceptScan: Boolean, verifyOriginalSession: () -> Unit): Boolean {
        val root = instrumentation.uiAutomation.rootInActiveWindow ?: return false
        if (root.packageName?.toString() != "com.android.vending") return false
        val actionText = if (acceptScan) "Scan app" else "Don't install app"
        val expected = setOf(fixtureLabel, "App scan recommended", actionText)
        val matched = mutableListOf<AccessibilityNodeInfo>()
        val pending = java.util.ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        pending.addLast(root to 0)
        var requested = 1
        var visited = 0
        var complete = true
        while (pending.isNotEmpty()) {
            val (node, depth) = pending.removeLast()
            visited++
            if (node.isVisibleToUser && node.packageName?.toString() == "com.android.vending" && node.text?.toString() in expected) {
                matched += node
            }
            if (depth == 32) {
                if (node.childCount > 0) complete = false
                continue
            }
            for (index in 0 until node.childCount) {
                if (requested >= 512) { complete = false; break }
                requested++
                val child = node.getChild(index)
                if (child == null) complete = false else pending.addLast(child to depth + 1)
            }
        }
        fun exact(text: String) = matched.filter { it.text?.toString() == text }
        if (!scanLookupReported) {
            scanLookupReported = true
            UnknownSourcePermissionState.evidence("scan lookup: fixtureLabelHit=${exact(fixtureLabel).isNotEmpty()} " +
                "scanTitleHit=${exact("App scan recommended").isNotEmpty()} action=$actionText actionHit=${exact(actionText).isNotEmpty()} nodes=$visited")
        }
        // A truncated tree cannot establish that the requested action is unique.
        if (!complete) return false
        if (exact(fixtureLabel).isEmpty() || exact("App scan recommended").isEmpty()) return false
        val actions = exact(actionText).distinct()
        check(actions.size <= 1) { "Ambiguous action on the fixed fixture's scan prompt" }
        var candidate = actions.singleOrNull() ?: return false
        repeat(5) {
            if (candidate.packageName?.toString() != "com.android.vending") return false
            if (candidate.isEnabled && candidate.isVisibleToUser && candidate.isClickable) {
                verifyOriginalSession()
                return candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            candidate = candidate.parent ?: return false
        }
        return false
    }

    /** Observed API 31 clean-result page only; cached scans may reach it without a new scan prompt. */
    fun acceptFixedFixtureSafeScan(verifyOriginalSession: () -> Unit): Boolean {
        if (InstrumentationRegistry.getArguments().getString("allowPlayProtectScan") != "true") return false
        val root = instrumentation.uiAutomation.rootInActiveWindow ?: return false
        if (root.packageName?.toString() != "com.android.vending") return false
        val title = "This app looks safe"
        val body = "You can continue to install it"
        val action = "Install"
        val expected = setOf(fixtureLabel, title, body, action)
        val matched = mutableListOf<AccessibilityNodeInfo>()
        val pending = java.util.ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        pending.addLast(root to 0)
        var requested = 1
        var visited = 0
        var complete = true
        while (pending.isNotEmpty()) {
            val (node, depth) = pending.removeLast()
            visited++
            if (node.isVisibleToUser && node.packageName?.toString() == "com.android.vending" && node.text?.toString() in expected) {
                matched += node
            }
            if (depth == 32) {
                if (node.childCount > 0) complete = false
                continue
            }
            for (index in 0 until node.childCount) {
                if (requested >= 512) { complete = false; break }
                requested++
                val child = node.getChild(index)
                if (child == null) complete = false else pending.addLast(child to depth + 1)
            }
        }
        fun exact(text: String) = matched.filter { it.text?.toString() == text }
        if (!safeScanLookupReported && exact(title).isNotEmpty()) {
            safeScanLookupReported = true
            UnknownSourcePermissionState.evidence("safe scan lookup: fixtureLabelHit=${exact(fixtureLabel).isNotEmpty()} " +
                "safeTitleHit=true safeBodyHit=${exact(body).isNotEmpty()} installHit=${exact(action).isNotEmpty()} nodes=$visited complete=$complete")
        }
        if (!complete || exact(fixtureLabel).isEmpty() || exact(title).isEmpty() || exact(body).isEmpty()) return false
        val actions = exact(action).distinct()
        check(actions.size <= 1) { "Ambiguous installation action on the fixed fixture's clean scan result" }
        var candidate = actions.singleOrNull() ?: return false
        repeat(5) {
            if (candidate.packageName?.toString() != "com.android.vending") return false
            if (candidate.isEnabled && candidate.isVisibleToUser && candidate.isClickable) {
                verifyOriginalSession()
                return candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            candidate = candidate.parent ?: return false
        }
        return false
    }

    @Suppress("DEPRECATION")
    fun diagnostic(): String {
        val owned = runCatching {
            manager.appTasks.mapNotNull { it.info() }.firstOrNull {
                it.baseIntent.component?.className == UserActionActivity::class.java.name &&
                    it.baseIntent.getStringExtra(UserActionBridge.EXTRA_TOKEN) == bridgeToken
            }
        }.getOrNull()
        val activePackage = runCatching { instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString() }.getOrNull()
        return "bridge=$bridgeToken bridgeAttached=${UserActionBridge.isAttached(bridgeToken)} " +
            "ownedTaskId=${owned?.id} ownedTaskTop=${owned?.topActivity?.flattenToShortString()} activeWindowPackage=$activePackage"
    }

    fun closeOwnedTask() {
        manager.appTasks.firstOrNull { candidate ->
            val info = candidate.info() ?: return@firstOrNull false
            info.baseIntent.component?.className == UserActionActivity::class.java.name &&
                info.baseIntent.getStringExtra(UserActionBridge.EXTRA_TOKEN) == bridgeToken
        }?.finishAndRemoveTask()
    }

    private fun task(): ActivityManager.AppTask? = manager.appTasks.firstOrNull {
        val info = it.info() ?: return@firstOrNull false
        info.baseActivity?.className == UserActionActivity::class.java.name &&
            info.baseIntent.getStringExtra(UserActionBridge.EXTRA_TOKEN) == bridgeToken && UserActionBridge.isAttached(bridgeToken)
    }

    private fun ActivityManager.AppTask.info(): ActivityManager.RecentTaskInfo? = runCatching { taskInfo }.getOrNull()

    companion object {
        private const val SETTINGS = "com.android.settings"
        private val SETTINGS_ACTIVITIES = setOf("com.android.settings.Settings\$ManageExternalSourcesActivity", "com.android.settings.Settings\$ManageAppExternalSourcesActivity")
        private val SWITCH_IDS = listOf("android:id/switch_widget", "com.android.settings:id/switch_widget", "com.android.settings:id/switch_main",
            "com.android.settings:id/switchWidget") // Observed standard Switch on HyperOS API 35.
        private val INSTALLERS = setOf("com.android.packageinstaller", "com.google.android.packageinstaller")
    }
}
