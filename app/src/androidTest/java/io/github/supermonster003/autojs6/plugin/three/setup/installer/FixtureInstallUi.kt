package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.os.ParcelFileDescriptor
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.ConfirmationActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallDialogActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation
import java.io.Closeable

/** UI actions are bound to a known request/token and, for approval, a prepared fixed fixture. */
internal object FixtureInstallUi {
    const val PACKAGE_NAME = "io.github.supermonster003.autojs6.installer.spike.fixture"
    const val LABEL = "3-Setup Spike Fixture"
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    fun clickInstall(tag: String, token: String? = null, sessionId: String? = null, packageName: String? = null): Boolean {
        require(token != null || sessionId != null)
        var clicked = false
        instrumentation.runOnMainSync {
            for (activity in ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<InstallDialogActivity>()) {
                val actualToken = activity.intent.getStringExtra(InstallPresentation.EXTRA_TOKEN) ?: continue
                val record = InstallPresentation.find(actualToken) ?: continue
                if (token != null && token != actualToken || sessionId != null && sessionId != record.request.id) continue
                if (packageName != null && record.snapshot().prompt?.metadata?.packageName != packageName) continue
                if (tag == InstallDialogActivity.TAG_CONFIRM && packageName == null) continue
                val button = activity.window.decorView.findViewWithTag<View>(tag)
                if (button != null && button.isEnabled && button.isShown) clicked = button.performClick()
            }
        }
        return clicked
    }
    fun hasInstallView(token: String, tag: String): Boolean {
        var found = false
        instrumentation.runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<InstallDialogActivity>().forEach {
                if (it.intent.getStringExtra(InstallPresentation.EXTRA_TOKEN) == token) found = it.window.decorView.findViewWithTag<View>(tag)?.isShown == true
            }
        }
        return found
    }
    fun resumedRecord(accept: (InstallPresentation.Record) -> Boolean): InstallPresentation.Record? {
        var found: InstallPresentation.Record? = null
        instrumentation.runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<InstallDialogActivity>().forEach {
                it.intent.getStringExtra(InstallPresentation.EXTRA_TOKEN)?.let(InstallPresentation::find)?.takeIf(accept)?.let { candidate -> found = candidate }
            }
        }
        return found
    }
    fun clickFixtureUninstall(packageName: String): Boolean {
        require(packageName == FixtureInstallUi.PACKAGE_NAME)
        var clicked = false
        instrumentation.runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<ConfirmationActivity>().forEach { activity ->
                fun containsPackage(view: View): Boolean = (view is TextView && view.text.toString() == packageName) ||
                    (view is ViewGroup && (0 until view.childCount).any { containsPackage(view.getChildAt(it)) })
                if (containsPackage(activity.window.decorView)) activity.window.decorView.findViewWithTag<View>(ConfirmationActivity.TAG_CONFIRM)?.let {
                    if (it.isEnabled && it.isShown) clicked = it.performClick()
                }
            }
        }
        return clicked
    }
    fun acceptSystemFixture(label: String): Boolean {
        require(label in setOf(FixtureInstallUi.LABEL, "3-Setup Core Fixture"))
        val root = instrumentation.uiAutomation.rootInActiveWindow ?: return false
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        try {
            val owner = root.packageName?.toString()
            if (owner !in setOf("com.android.packageinstaller", "com.google.android.packageinstaller")) return false
            val labels = root.findAccessibilityNodeInfosByText(label).also(nodes::addAll)
            if (labels.none { it.isVisibleToUser && it.text?.toString() == label }) return false
            // Standard positive-button IDs can also appear in warnings. Only the normal install,
            // update or OK labels are accepted; never "Install anyway" or a generic continuation.
            val normalLabels = mutableSetOf("Install", "Update", "OK", "安装", "更新", "确定")
            runCatching { instrumentation.targetContext.packageManager.getResourcesForApplication(requireNotNull(owner)) }.getOrNull()?.let { resources ->
                for (name in listOf("install", "update", "ok")) {
                    @Suppress("DiscouragedApi")
                    val id = resources.getIdentifier(name, "string", owner)
                    if (id != 0) runCatching { resources.getString(id) }.getOrNull()?.let(normalLabels::add)
                }
            }
            val candidates = (root.findAccessibilityNodeInfosByViewId("android:id/button1") +
                root.findAccessibilityNodeInfosByViewId("$owner:id/ok_button") +
                normalLabels.flatMap(root::findAccessibilityNodeInfosByText)).also(nodes::addAll)
            return candidates.firstOrNull { node ->
                node.isVisibleToUser && node.isEnabled && node.isClickable &&
                    normalLabels.any { it.equals(node.text?.toString()?.trim(), true) }
            }?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
        } finally {
            @Suppress("DEPRECATION")
            nodes.forEach { it.recycle() }
            @Suppress("DEPRECATION")
            root.recycle()
        }
    }
}

/** Refuses pre-existing packages/data across every enumerated user, and only cleans started tests. */
internal class FixturePackageOwnership(private val packages: Set<String>) : Closeable {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val users: List<String>
    private var started = false
    init {
        val fixedPackages = setOf(FixtureInstallUi.PACKAGE_NAME,
            "io.github.supermonster003.autojs6.installer.advanced.fixture",
            "io.github.supermonster003.autojs6.installer.advanced.shared.fixture")
        require(packages.isNotEmpty() && packages.all { it in fixedPackages })
        users = Regex("UserInfo\\{(\\d+):").findAll(shell("pm list users")).map { it.groupValues[1] }.toList()
        check(users.isNotEmpty()) { "Cannot enumerate users for fixture ownership" }
        check(users.none { user -> listed(user).any { it in packages } }) { "A fixture or retained fixture data already exists; refusing to modify it" }
    }
    fun installationStarted() { started = true }
    override fun close() {
        if (!started) return
        packages.forEach { packageName ->
            if (users.any { packageName in listed(it) }) {
                val reply = shell("pm uninstall $packageName")
                check(reply.contains("Success")) { "Owned fixture cleanup failed: $reply" }
                check(users.none { packageName in listed(it) }) { "Owned fixture remains installed" }
            }
        }
    }
    private fun listed(user: String): Set<String> {
        val lines = shell("pm list packages -u --user $user").lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
        check("package:android" in lines && lines.all { it.startsWith("package:") }) { "Cannot safely enumerate user $user packages" }
        return lines.map { it.removePrefix("package:") }.toSet()
    }
    private fun shell(command: String): String = instrumentation.uiAutomation.executeShellCommand(command).use {
        ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().use { reader -> reader.readText() }
    }
}
