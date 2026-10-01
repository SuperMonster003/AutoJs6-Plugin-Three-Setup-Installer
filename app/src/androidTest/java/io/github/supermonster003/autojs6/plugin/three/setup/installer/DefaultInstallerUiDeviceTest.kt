package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.KeyguardManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.util.AtomicFile
import android.view.Display
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.history.InstallHistoryCodec
import io.github.supermonster003.autojs6.plugin.three.setup.installer.history.InstallHistoryEntry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.history.InstallHistoryStore
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.InstallerPreferences
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.DefaultInstallerActivity
import org.autojs.plugin.installer.api.InstallerContract
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileNotFoundException
import java.io.StringReader
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Opt in with -e defaultUiAuthorizer shizuku|root on an unlocked device. Existing APK defaults,
 * last-chosen APK handlers, and ALL defaults owned by this plugin cause a skip, never a reset.
 *
 * -e defaultUiRestoreOnly true restores a pending durable journal without starting any Activity.
 * -e defaultUiHoldMillis 120000 pauses after locking for an independently operated real file
 * manager. On READY, touch the reported private continuePath after finishing that check. A timeout
 * fails the audit and restores the baseline. This test never launches a synthetic file-manager
 * intent and does not count the hold itself as file-manager evidence. During the hold, touch
 * dump-<runId> next to the plan to request a bounded tree-<runId>.json accessibility snapshot of
 * only this plugin, system DocumentsUI or the Android resolver; no second UiAutomation connects.
 * -e defaultUiAllowFilesClick true additionally enables click-<runId>.json requests during that
 * hold. Only DocumentsUI's fixture.apk, Download and the fixed test-directory naming pattern are
 * accepted; click-result-<runId>.json reports event acceptance, never inferred navigation success.
 * If an old DocumentsUI row has no clickable accessibility ancestor, the same guarded text node
 * receives a real SOURCE_TOUCHSCREEN/TOOL_TYPE_FINGER tap at its refreshed bounds' center.
 *
 * -e defaultUiCleanupHistoryOnly true -e defaultUiRunId <UUID> -e defaultUiHistoryToken <UUID>
 * removes only already-cancelled, external fixture entries created inside that completed run.
 * The same-run journal must already be restored. No Activity or privileged service is opened.
 *
 * -e defaultUiClearApprovedInstallerX true is a separate, device/package/record-shape-pinned
 * maintenance action explicitly approved for QV770340J7. It is never implied by a normal audit.
 * -e defaultUiVerifyInstallerXPartialClearOnly true only verifies the recorded partial-clear audit.
 * -e defaultUiPreserveApprovedInstallerXLastChosen true allows a Root UI audit on that same device
 * to preserve exactly its two remaining no-scheme last-chosen entries. Normal audits still skip.
 * -e defaultUiPreserveUnrelatedLastChosen true separately allows a chooser-only baseline with
 * proved third-party no-scheme APK last-chosen entries. It never clears them or permits existing
 * plugin records, actual defaults, or unproved entries. Full baseline restoration is still required.
 */
@RunWith(AndroidJUnit4::class)
class DefaultInstallerUiDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val args = InstrumentationRegistry.getArguments()
    private val target = ComponentName(context.packageName, "${context.packageName}.ui.ExternalInstallActivity")

    @Test fun explicitAuthorizerLocksAndUnlocksThroughTheRealDefaultInstallerPage() {
        check(listOf("defaultUiClearApprovedInstallerX", "defaultUiVerifyInstallerXPartialClearOnly",
            "defaultUiCleanupHistoryOnly", "defaultUiRestoreOnly").count { args.getString(it) == "true" } <= 1) {
            "Maintenance, read-only verification, history cleanup and restoration must be separate explicit modes"
        }
        if (args.getString("defaultUiVerifyInstallerXPartialClearOnly") == "true") {
            val preserved = DefaultInstallerUiRecovery.preserveApprovedInstallerXHistory(instrumentation, DefaultInstallerUiRecovery.snapshot(instrumentation))
            evidence("INSTALLER_X_PARTIAL_CLEAR_VERIFIED preservedLastChosen=2 preservedGenericWildcardLastChosen=${preserved.proof.get("preservedGenericWildcardRecordCount").asInt} actualDefault=false otherDefaultsUnchanged=true pluginPreferencesUnchanged=true domainLinksUnchanged=true noWrites=true")
            return
        }
        if (args.getString("defaultUiClearApprovedInstallerX") == "true") {
            check(args.getString("defaultUiRestoreOnly") != "true" && args.getString("defaultUiCleanupHistoryOnly") != "true" &&
                args.getString("defaultUiAuthorizer") == null) { "The approved InstallerX clear must run as a separate explicit mode" }
            val cleared = DefaultInstallerUiRecovery.clearApprovedInstallerX(instrumentation)
            evidence("APPROVED_INSTALLER_X_CLEARED removed=3 otherDefaultsUnchanged=true pluginPreferencesUnchanged=true domainLinksUnchanged=true shellIdentityDropped=true noActivity=true audit=${cleared.internalPath} readableCopy=${cleared.readableCopy}")
            return
        }
        if (args.getString("defaultUiCleanupHistoryOnly") == "true") {
            check(args.getString("defaultUiRestoreOnly") != "true") { "Cleanup and restoration are separate explicit modes" }
            cleanupCancelledFixtureHistory()
            return
        }
        if (args.getString("defaultUiRestoreOnly") == "true") {
            val restored = DefaultInstallerUiRecovery.restore(instrumentation)
            evidence("RESTORED runId=${restored.runId} noActivity=true plan=${restored.internalPath}")
            return
        }
        val requested = args.getString("defaultUiAuthorizer")
        assumeTrue("Explicit -e defaultUiAuthorizer shizuku|root is required", requested in listOf("shizuku", "root"))
        val authorizer = Authorizer.entries.single { it.id == requested }
        val holdMillis = args.getString("defaultUiHoldMillis")?.let {
            checkNotNull(it.toLongOrNull()) { "defaultUiHoldMillis must be a whole number" }
        } ?: 0L
        check(holdMillis == 0L || holdMillis in 1_000L..180_000L) { "defaultUiHoldMillis must be 0 or 1000..180000" }
        val allowFilesClick = args.getString("defaultUiAllowFilesClick") == "true"
        check(!allowFilesClick || holdMillis > 0L) { "defaultUiAllowFilesClick requires an explicit hold interval" }
        // Scope the shell snapshot to owner-user device runs. An unqualified preferred-xml dump
        // must not be treated as evidence about a secondary user's preferred activities.
        assumeTrue("This isolated default-installer audit requires owner user 0", Process.myUid() / 100_000 == 0)
        assumeTrue("Unlock the device before the default-installer UI audit",
            !context.getSystemService(KeyguardManager::class.java).isKeyguardLocked &&
                context.getSystemService(PowerManager::class.java).isInteractive)

        val baseline = DefaultInstallerUiRecovery.snapshot(instrumentation)
        check(listOf("defaultUiPreserveApprovedInstallerXLastChosen", "defaultUiPreserveUnrelatedLastChosen")
            .count { args.getString(it) == "true" } <= 1) { "Select only one explicit last-chosen preservation mode" }
        val preservedHistory = if (args.getString("defaultUiPreserveApprovedInstallerXLastChosen") == "true") {
            check(authorizer == Authorizer.ROOT) { "The audited InstallerX last-chosen exception is only for this explicit Root UI run" }
            DefaultInstallerUiRecovery.preserveApprovedInstallerXHistory(instrumentation, baseline).also {
                evidence("PRESERVING_APPROVED_INSTALLER_X_HISTORY partialClearRecorded=true preservedLastChosen=2 preservedGenericWildcardLastChosen=${it.proof.get("preservedGenericWildcardRecordCount").asInt} actualDefault=false")
            }
        } else if (args.getString("defaultUiPreserveUnrelatedLastChosen") == "true") {
            DefaultInstallerUiRecovery.preserveUnrelatedLastChosen(context, baseline).also {
                evidence("PRESERVING_UNRELATED_LAST_CHOSEN records=${it.proof.get("preservedRecordCount").asInt} actualDefault=false noHistoryCleared=true")
            }
        } else null
        DefaultInstallerUiRecovery.unsafeReason(context, baseline, preservedHistory)?.let { reason ->
            evidence("SKIPPED authorizer=$requested reason=$reason defaultsUnchanged=true")
            assumeTrue(reason, false)
        }
        assumeTrue("The plugin must not already resolve as the default before this isolated audit",
            baseline.resolved.none { it == target.flattenToString() })
        val preferences = InstallerPreferences.read(context)
        assumeTrue("The file-manager probe must already use dialog interaction; never silently install its fixture",
            holdMillis == 0L || preferences.interaction == InstallerContract.INTERACTION_DIALOG)
        assumeTrue("The requested authorizer must already be enabled; the test does not modify preferences",
            authorizer in preferences.authorizers.enabled)
        assumeTrue("Start the requested authorizer before this audit",
            waitUntil(15_000) { AuthorizerStates.state(context, authorizer).let { it.available && it.running } })
        val plan = DefaultInstallerUiRecovery.begin(context, requested!!, baseline, preservedHistory)
        evidence("PENDING runId=${plan.runId} authorizer=$requested plan=${plan.internalPath} readableCopy=${plan.readableCopy}")

        var scenario: ActivityScenario<DefaultInstallerActivity>? = null
        var primaryFailure: Throwable? = null
        try {
            var page = launchPage().also { scenario = it }
            awaitPage(page, locked = false)

            // Selecting an authorizer is still a draft. Cancel must not write a system default.
            clickPageAction(page, "default-set")
            chooseAuthorizer(page, authorizer)
            assertEquals("Choosing authorization wrote defaults before confirmation", baseline.document(),
                DefaultInstallerUiRecovery.snapshot(instrumentation).document())
            clickDialogAction(page, R.string.action_cancel)
            awaitPage(page, locked = false)
            assertEquals("Cancelling authorization changed a default or plugin preference", baseline.document(),
                DefaultInstallerUiRecovery.snapshot(instrumentation).document())
            evidence("CANCEL authorizer=$requested systemDefaultsUnchanged=true pluginPreferencesUnchanged=true")

            clickPageAction(page, "default-set")
            chooseAuthorizer(page, authorizer)
            assertEquals(baseline.document(), DefaultInstallerUiRecovery.snapshot(instrumentation).document())
            clickDialogAction(page, R.string.settings_confirm)
            await("The confirmed page did not become the APK handler in all four public queries", 45_000) {
                publicResolution().all { it == target.flattenToString() }
            }
            awaitPage(page, locked = true)
            assertLockedPage(page)
            val locked = DefaultInstallerUiRecovery.snapshot(instrumentation)
            assertEquals(List(4) { target.flattenToString() }, locked.resolved)
            DefaultInstallerUiRecovery.assertUnrelatedUnchanged(context, baseline, locked)
            evidence("LOCKED authorizer=$requested uiConfirmation=true publicResolution=4/4 otherDefaultsUnchanged=true pluginPreferencesUnchanged=true")

            if (holdMillis > 0L) {
                val auditFolder = File(plan.internalPath).parentFile!!
                val dumpRequest = File(auditFolder, "dump-${plan.runId}")
                val treeFile = File(auditFolder, "tree-${plan.runId}.json")
                val clickRequest = File(auditFolder, "click-${plan.runId}.json")
                val clickResult = File(auditFolder, "click-result-${plan.runId}.json")
                evidence("READY runId=${plan.runId} authorizer=$requested holdMillis=$holdMillis continuePath=${plan.continuePath} dumpRequest=${dumpRequest.absolutePath} treePath=${treeFile.absolutePath} filesClicksEnabled=$allowFilesClick clickRequest=${clickRequest.absolutePath} clickResult=${clickResult.absolutePath} fileManagerEvidence=external")
                await("No continuation marker arrived after the independently operated file-manager check", holdMillis) {
                    if (allowFilesClick && clickRequest.isFile) {
                        performHeldFilesClick(clickRequest, clickResult, plan.runId)
                        check(clickRequest.delete()) { "Could not remove the test-owned Files click request" }
                    }
                    if (dumpRequest.isFile) {
                        writeHeldUiTree(treeFile)
                        check(dumpRequest.delete()) { "Could not remove the test-owned tree request" }
                    }
                    File(plan.continuePath).isFile
                }
                assertEquals("The lock changed during the independently operated file-manager check",
                    List(4) { target.flattenToString() }, publicResolution())
                // The independent operator cancels its owned install confirmation before signalling.
                // Re-enter this actual page so its onResume refresh is exercised after external use.
                page.close()
                page = launchPage().also { scenario = it }
                awaitPage(page, locked = true)
                evidence("CONTINUED runId=${plan.runId} externalFileManagerCheckFinished=true")
            }

            clickPageAction(page, "default-clear")
            chooseAuthorizer(page, authorizer)
            assertEquals("Selecting authorization removed the lock before confirmation",
                List(4) { target.flattenToString() }, publicResolution())
            clickDialogAction(page, R.string.settings_confirm)
            await("The page did not restore the original four APK resolutions after unlocking", 45_000) {
                publicResolution() == baseline.resolved
            }
            awaitPage(page, locked = false)
            assertEquals("Unlock changed an unrelated default or plugin preference", baseline.document(),
                DefaultInstallerUiRecovery.snapshot(instrumentation).document())
            evidence("SUCCESS authorizer=$requested uiLock=true uiUnlock=true publicResolution=4/4 baselineRestored=true")
        } catch (failure: Throwable) {
            primaryFailure = failure
            evidence("FAILED authorizer=$requested publicResolution=${publicResolution()} reason=${failure.message}")
            throw failure
        } finally {
            var cleanupFailure: Throwable? = null
            fun cleanup(action: () -> Unit) {
                try { action() } catch (failure: Throwable) {
                    if (primaryFailure != null) primaryFailure!!.addSuppressed(failure)
                    else if (cleanupFailure == null) cleanupFailure = failure
                    else cleanupFailure!!.addSuppressed(failure)
                }
            }
            cleanup { scenario?.close() }
            cleanup {
                DefaultInstallerUiRecovery.restore(instrumentation)
                evidence("CLEANUP runId=${plan.runId} allPreferredAndLastChosenUnchanged=true pluginPreferencesUnchanged=true publicResolutionRestored=true")
            }
            if (primaryFailure == null) cleanupFailure?.let { throw it }
        }
    }

    private fun cleanupCancelledFixtureHistory() {
        val runId = checkNotNull(args.getString("defaultUiRunId")) { "defaultUiRunId is required for history cleanup" }
        val token = checkNotNull(args.getString("defaultUiHistoryToken")) { "defaultUiHistoryToken is required for history cleanup" }
        check(InstallHistoryEntry.validToken(token)) { "The history token must be a canonical UUID" }
        val run = DefaultInstallerUiRecovery.completedRun(context, runId)
        val stateBefore = DefaultInstallerUiRecovery.snapshot(instrumentation).document()
        try {
            var persisted: List<InstallHistoryEntry>? = null
            val found = waitUntil(5_000) {
                persisted = persistedHistory()
                persisted?.any { it.token == token } == true
            }
            val diskBefore = checkNotNull(persisted) { "History did not settle before cleanup; no record was deleted" }
            if (!found) {
                evidence("HISTORY_ABSENT runId=$runId token=$token removed=0 waitedMillis=5000 deletionSkipped=true")
                return
            }
            // The external confirmation's teardown can post its terminal history update after
            // default restoration finishes. Creation must still fall inside the audited run;
            // allow only one extra second for cancellation/IO completion, never for a new session.
            val terminalDeadline = Math.addExact(run.restoredAt, 1_000L)
            fun checkedOwned(entries: List<InstallHistoryEntry>): List<InstallHistoryEntry> = entries.filter { it.token == token }.also { owned ->
                check(owned.isNotEmpty()) { "The selected history token disappeared before its identity was verified" }
                owned.forEach { entry ->
                    check(entry.packageName == "io.github.supermonster003.autojs6.installer.spike.fixture" &&
                        entry.origin == InstallerContract.SOURCE_EXTERNAL && entry.result == InstallerContract.STAGE_CANCELLED &&
                        entry.startedAt in run.startedAt..run.restoredAt &&
                        entry.finishedAt?.let { it in entry.startedAt..terminalDeadline } == true &&
                        entry.updatedAt in entry.startedAt..terminalDeadline) {
                        "Refusing history cleanup: an entry is not an already-cancelled external fixture from the completed run"
                    }
                    InstallHistoryEntry.validate(entry)
                }
            }
            checkedOwned(diskBefore)
            // Loading the production store turns unfinished history into cancelled records on
            // cold start. Refuse that side effect while cleaning an unrelated owned fixture.
            check(diskBefore.all { it.terminal }) { "Unfinished unrelated history exists; no record was deleted" }
            val store = InstallHistoryStore.get(context)
            await("Installation history did not finish loading", 5_000) { store.loaded }
            val before = store.list()
            check(before.associateBy { it.id } == diskBefore.associateBy { it.id }) {
                "History changed before cleanup; no record was deleted"
            }
            val owned = checkedOwned(before)
            val unrelated = before.filterNot { it.token == token }.associateBy { it.id }
            val completed = CountDownLatch(owned.size)
            val successful = AtomicBoolean(true)
            owned.forEach { entry ->
                // remove() retires this exact ID on an active ticket before queuing disk IO, so a
                // late save cannot resurrect a deleted entry from the audited cancelled session.
                store.remove(entry.id) { written ->
                    if (!written) successful.set(false)
                    completed.countDown()
                }
            }
            check(completed.await(5, TimeUnit.SECONDS) && successful.get()) { "Owned history deletion did not finish durably" }
            val after = store.list()
            check(after.none { it.token == token }) { "An owned fixture history entry remains" }
            check(after.associateBy { it.id } == unrelated) { "An unrelated history entry changed during cleanup" }
            val diskAfter = checkNotNull(persistedHistory()) { "History is still being written after the deletion callback" }
            check(diskAfter.associateBy { it.id } == unrelated) { "The durable history differs from the expected unrelated entries" }
            evidence("HISTORY_CLEANUP runId=$runId token=$token removed=${owned.size} callbacksSuccessful=true durable=true otherHistoryUnchanged=true")
        } finally {
            check(stateBefore == DefaultInstallerUiRecovery.snapshot(instrumentation).document()) {
                "A default activity or plugin preference changed during history cleanup"
            }
            evidence("HISTORY_STATE_CHECK runId=$runId token=$token defaultsUnchanged=true pluginPreferencesUnchanged=true noActivity=true")
        }
    }

    /** Read without invoking the production loader's crash recovery or deleting its sidecars. */
    private fun persistedHistory(): List<InstallHistoryEntry>? {
        val file = File(context.noBackupFilesDir, "installation-history/history.json")
        val pending = File(file.path + ".new")
        val backup = File(file.path + ".bak")
        if (pending.exists() || backup.exists()) return null
        if (!file.exists()) return emptyList()
        check(file.isFile && file.length() in 1L..InstallHistoryCodec.MAX_BYTES.toLong()) { "History has an invalid size" }
        val bytes = try { file.readBytes() } catch (_: FileNotFoundException) { return null }
        if (pending.exists() || backup.exists()) return null
        return InstallHistoryCodec.decode(bytes)
    }

    private fun launchPage() = ActivityScenario.launch(DefaultInstallerActivity::class.java).also { scenario ->
        scenario.onActivity { it.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        instrumentation.waitForIdleSync()
    }

    private fun awaitPage(scenario: ActivityScenario<DefaultInstallerActivity>, locked: Boolean) {
        await("The default-installer page did not finish refreshing (locked=$locked)", 20_000) {
            var ready = false
            scenario.onActivity { activity ->
                val decor = activity.window.decorView
                val set = decor.findViewWithTag<View>("default-set")
                val clear = decor.findViewWithTag<View>("default-clear")
                ready = decor.hasWindowFocus() && set != null && clear != null && set.isEnabled == !locked && clear.isEnabled == locked
            }
            ready
        }
    }

    private fun assertLockedPage(scenario: ActivityScenario<DefaultInstallerActivity>) = scenario.onActivity { activity ->
        val decor = activity.window.decorView
        val componentRow = decor.findViewWithTag<View>("default-current")
        assertTrue("The current-handler row does not identify the actual external entry",
            descendants(componentRow).filterIsInstance<TextView>().any { it.text.toString() == target.flattenToString() })
        val stateRow = decor.findViewWithTag<View>("default-state")
        assertTrue("The state card does not show the plugin as selected",
            descendants(stateRow).filterIsInstance<TextView>().any { it.text.toString() == activity.getString(R.string.default_installer_self) })
    }

    /** Use the real production control's accessibility action after bringing it fully into view. */
    private fun clickPageAction(scenario: ActivityScenario<DefaultInstallerActivity>, tag: String) {
        await("The visible page action $tag was not clickable", 15_000) {
            var clicked = false
            scenario.onActivity { activity ->
                val decor = activity.window.decorView
                val button = decor.findViewWithTag<View>(tag)
                if (decor.hasWindowFocus() && button != null && button.isEnabled && button.isShown) {
                    if (!fullyVisible(button)) button.requestRectangleOnScreen(Rect(0, 0, button.width, button.height), true)
                    else clicked = button.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, null)
                }
            }
            clicked
        }
    }

    private fun chooseAuthorizer(scenario: ActivityScenario<DefaultInstallerActivity>, authorizer: Authorizer) {
        await("The real authorization chooser did not offer ${authorizer.id}", 15_000) {
            var label = ""
            scenario.onActivity { label = it.getString(if (authorizer == Authorizer.SHIZUKU) R.string.settings_shizuku else R.string.settings_root) }
            onVisibleNode({ node -> node.isCheckable && node.text?.toString()?.substringBefore('\n') == label }) {
                it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
        await("The authorization chooser did not retain ${authorizer.id} as its selected draft", 5_000) {
            var label = ""
            scenario.onActivity { label = it.getString(if (authorizer == Authorizer.SHIZUKU) R.string.settings_shizuku else R.string.settings_root) }
            onVisibleNode({ node -> node.isCheckable && node.text?.toString()?.substringBefore('\n') == label }) { it.isChecked }
        }
    }

    private fun clickDialogAction(scenario: ActivityScenario<DefaultInstallerActivity>, resource: Int) {
        await("The authorization dialog action was not visible", 10_000) {
            var label = ""
            scenario.onActivity { label = it.getString(resource) }
            onVisibleNode({ it.isClickable && it.text?.toString() == label }) {
                it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun onVisibleNode(predicate: (AccessibilityNodeInfo) -> Boolean, action: (AccessibilityNodeInfo) -> Boolean): Boolean {
        val root = instrumentation.uiAutomation.rootInActiveWindow ?: return false
        val nodes = arrayListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo) {
            nodes += node
            for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
        }
        visit(root)
        try {
            val matching = nodes.filter { it.packageName?.toString() == context.packageName && it.isVisibleToUser && it.isEnabled && predicate(it) }
            check(matching.size <= 1) { "The authorization dialog control is ambiguous" }
            return matching.singleOrNull()?.let(action) ?: false
        } finally { nodes.forEach { it.recycle() } }
    }

    private fun fullyVisible(view: View): Boolean {
        val bounds = Rect()
        return view.width > 0 && view.height > 0 && view.getGlobalVisibleRect(bounds) && bounds.width() >= view.width && bounds.height() >= view.height
    }

    /** Bounded, explicitly enabled bridge for navigating the real DocumentsUI during a held audit. */
    @Suppress("DEPRECATION")
    private fun performHeldFilesClick(request: File, resultFile: File, runId: String) {
        val result = JsonObject().apply {
            addProperty("runId", runId)
            addProperty("requestedAtMillis", request.lastModified())
            addProperty("performed", false)
            add("downAccepted", JsonNull.INSTANCE)
            add("upAccepted", JsonNull.INSTANCE)
        }
        val nodes = arrayListOf<AccessibilityNodeInfo>()
        val parents = arrayListOf<AccessibilityNodeInfo>()
        try {
            val bytes = ByteArray(2049)
            val length = request.inputStream().use { input ->
                var length = 0
                while (length < bytes.size) {
                    val read = input.read(bytes, length, bytes.size - length)
                    if (read < 0) break
                    length += read
                }
                length
            }
            check(length in 1..2048) { "The click request must contain at most 2048 bytes" }
            val values = linkedMapOf<String, String>()
            JsonReader(StringReader(String(bytes, 0, length, Charsets.UTF_8))).use { reader ->
                reader.strictness = Strictness.STRICT
                reader.beginObject()
                while (reader.hasNext()) {
                    val key = reader.nextName()
                    check(key in setOf("packageName", "text") && key !in values && reader.peek() == JsonToken.STRING) {
                        "Only one string packageName and one string text are accepted"
                    }
                    values[key] = reader.nextString()
                }
                reader.endObject()
                check(reader.peek() == JsonToken.END_DOCUMENT && values.keys == setOf("packageName", "text")) {
                    "The click request must contain exactly packageName and text"
                }
            }
            val packageName = values.getValue("packageName")
            val text = values.getValue("text")
            check(packageName in setOf("com.android.documentsui", "com.google.android.documentsui")) {
                "Only the two system DocumentsUI packages are allowed"
            }
            check(text == "fixture.apk" || text == "Download" || Regex("ThreeSetupDefaultProbe-[a-f0-9]{12}").matches(text)) {
                "The requested text is outside the fixture navigation allowlist"
            }
            result.addProperty("packageName", packageName)
            result.addProperty("text", text)
            val root = checkNotNull(instrumentation.uiAutomation.rootInActiveWindow) { "There is no active accessibility window" }
            nodes += root
            check(root.packageName?.toString() == packageName) { "The active window is not the requested DocumentsUI package" }
            fun visitChildren(node: AccessibilityNodeInfo, depth: Int) {
                check(depth <= 40 && nodes.size <= 1024) { "The DocumentsUI tree exceeds the bounded audit limit" }
                for (index in 0 until node.childCount) {
                    val child = node.getChild(index) ?: continue
                    nodes += child
                    visitChildren(child, depth + 1)
                }
            }
            visitChildren(root, 0)
            val matches = nodes.filter { it.packageName?.toString() == packageName && it.isVisibleToUser && it.isEnabled && it.text?.toString() == text }
            result.addProperty("matches", matches.size)
            check(matches.size == 1) { "The requested visible, enabled DocumentsUI text is absent or ambiguous" }
            val textNode = matches.single()
            check(textNode.refresh() && textNode.packageName?.toString() == packageName &&
                textNode.isVisibleToUser && textNode.isEnabled && textNode.text?.toString() == text) { "The DocumentsUI text node became stale" }
            var target: AccessibilityNodeInfo? = textNode
            var depth = 0
            while (target != null) {
                check(target.packageName?.toString() == packageName) { "A clickable ancestor crosses the DocumentsUI package boundary" }
                if (target.isVisibleToUser && target.isEnabled && target.isClickable) break
                if (depth == 5) { target = null; break }
                target = target.parent?.also { parents += it }
                depth++
            }
            val clickTarget = target
            val performed = if (clickTarget != null) {
                check(clickTarget.refresh() && clickTarget.packageName?.toString() == packageName &&
                    clickTarget.isVisibleToUser && clickTarget.isEnabled && clickTarget.isClickable) { "The DocumentsUI click target became stale" }
                val bounds = Rect().also(clickTarget::getBoundsInScreen)
                result.addProperty("method", "accessibility")
                result.addProperty("ancestorDepth", depth)
                result.addProperty("resourceId", clickTarget.viewIdResourceName)
                result.addProperty("class", clickTarget.className?.toString())
                result.add("bounds", JsonArray().apply { add(bounds.left); add(bounds.top); add(bounds.right); add(bounds.bottom) })
                clickTarget.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            } else {
                result.addProperty("method", "finger")
                result.addProperty("reason", "No clickable same-package ancestor within five levels")
                fingerClickHeldDocumentsNode(textNode, packageName, text, root.windowId, result)
            }
            result.addProperty("performed", performed)
            result.addProperty("status", if (performed) "action-dispatched" else "action-rejected")
        } catch (failure: Throwable) {
            result.addProperty("status", "request-rejected")
            result.addProperty("reason", failure.message?.take(512))
        } finally {
            parents.forEach { it.recycle() }
            nodes.forEach { it.recycle() }
            result.addProperty("completedAtMillis", System.currentTimeMillis())
            val bytes = GsonBuilder().setPrettyPrinting().create().toJson(result).toByteArray(Charsets.UTF_8)
            val atomic = AtomicFile(resultFile)
            val output = atomic.startWrite()
            try { output.write(bytes); output.fd.sync(); atomic.finishWrite(output) }
            catch (failure: Throwable) { atomic.failWrite(output); throw failure }
        }
    }

    @Suppress("DEPRECATION")
    private fun fingerClickHeldDocumentsNode(node: AccessibilityNodeInfo, packageName: String, text: String,
        expectedWindowId: Int, result: JsonObject): Boolean {
        check(context.getSystemService(PowerManager::class.java).isInteractive &&
            !context.getSystemService(KeyguardManager::class.java).isKeyguardLocked) { "The DocumentsUI device is not interactive and unlocked" }
        instrumentation.uiAutomation.waitForIdle(80, 2_000)
        val root = checkNotNull(instrumentation.uiAutomation.rootInActiveWindow) { "DocumentsUI lost its active window" }
        val windowBounds = Rect()
        try {
            check(root.packageName?.toString() == packageName && root.windowId == expectedWindowId) { "DocumentsUI lost its original active window" }
            root.getBoundsInScreen(windowBounds)
        } finally { root.recycle() }
        check(node.refresh() && node.packageName?.toString() == packageName && node.windowId == expectedWindowId &&
            node.isVisibleToUser && node.isEnabled && node.text?.toString() == text) { "The guarded DocumentsUI text moved to a different node/window" }
        val bounds = Rect().also(node::getBoundsInScreen)
        check(!bounds.isEmpty && windowBounds.contains(bounds)) { "The DocumentsUI text is not wholly inside its active window" }
        val displayId = if (Build.VERSION.SDK_INT >= 30) node.window?.let { window ->
            try { window.displayId } finally { window.recycle() }
        } ?: Display.DEFAULT_DISPLAY else Display.DEFAULT_DISPLAY
        if (Build.VERSION.SDK_INT < 34) check(displayId == Display.DEFAULT_DISPLAY) { "The public touch factory cannot target a secondary display on this API" }
        result.addProperty("resourceId", node.viewIdResourceName)
        result.addProperty("class", node.className?.toString())
        result.addProperty("displayId", displayId)
        result.addProperty("source", InputDevice.SOURCE_TOUCHSCREEN)
        result.addProperty("toolType", MotionEvent.TOOL_TYPE_FINGER)
        result.add("bounds", JsonArray().apply { add(bounds.left); add(bounds.top); add(bounds.right); add(bounds.bottom) })
        val properties = arrayOf(MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_FINGER })
        val coordinates = arrayOf(MotionEvent.PointerCoords().apply {
            x = bounds.exactCenterX(); y = bounds.exactCenterY(); pressure = 1f; size = 1f
        })
        val downTime = SystemClock.uptimeMillis()
        fun inject(action: Int): Boolean {
            coordinates[0].pressure = if (action == MotionEvent.ACTION_DOWN) 1f else 0f
            val event = if (Build.VERSION.SDK_INT >= 34) {
                requireNotNull(MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, 1, properties, coordinates,
                    0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, displayId, 0, MotionEvent.CLASSIFICATION_NONE))
            } else {
                MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, 1, properties, coordinates,
                    0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0)
            }
            return try { instrumentation.uiAutomation.injectInputEvent(event, true) } finally { event.recycle() }
        }
        var completed = false
        try {
            val down = inject(MotionEvent.ACTION_DOWN)
            result.addProperty("downAccepted", down)
            if (!down) return false
            SystemClock.sleep(80)
            val up = inject(MotionEvent.ACTION_UP)
            result.addProperty("upAccepted", up)
            completed = up
            return up
        } finally {
            if (!completed) runCatching { inject(MotionEvent.ACTION_CANCEL) }
                .onSuccess { result.addProperty("cancelAccepted", it) }
                .onFailure { result.addProperty("cancelFailure", it.message?.take(256)) }
        }
    }

    /** The external driver requests this tree instead of attaching a competing UiAutomation. */
    @Suppress("DEPRECATION")
    private fun writeHeldUiTree(file: File) {
        val allowed = setOf(context.packageName, "com.android.documentsui", "com.google.android.documentsui", "android", "com.android.intentresolver")
        val document = JsonObject().apply { addProperty("capturedAtMillis", System.currentTimeMillis()) }
        val nodes = JsonArray()
        var truncated = false
        val root = instrumentation.uiAutomation.rootInActiveWindow
        if (root == null) document.addProperty("status", "no-active-window")
        else try {
            val activePackage = root.packageName?.toString()
            document.addProperty("activePackage", activePackage)
            document.addProperty("windowId", root.windowId)
            if (activePackage !in allowed) document.addProperty("status", "package-not-allowed")
            else {
                document.addProperty("status", "captured")
                fun visit(node: AccessibilityNodeInfo, parent: Int, depth: Int) {
                    if (nodes.size() >= 512 || depth > 32) { truncated = true; return }
                    if (node.packageName?.toString() !in allowed) return
                    val index = nodes.size()
                    val bounds = Rect().also(node::getBoundsInScreen)
                    nodes.add(JsonObject().apply {
                        addProperty("index", index); addProperty("parent", parent)
                        addProperty("package", node.packageName?.toString())
                        addProperty("text", node.text?.toString()?.take(512))
                        addProperty("description", node.contentDescription?.toString()?.take(512))
                        addProperty("resourceId", node.viewIdResourceName)
                        addProperty("class", node.className?.toString())
                        addProperty("clickable", node.isClickable); addProperty("scrollable", node.isScrollable)
                        addProperty("enabled", node.isEnabled); addProperty("visible", node.isVisibleToUser)
                        addProperty("checked", node.isChecked)
                        add("bounds", JsonArray().apply { add(bounds.left); add(bounds.top); add(bounds.right); add(bounds.bottom) })
                    })
                    for (indexInParent in 0 until node.childCount) {
                        val child = node.getChild(indexInParent) ?: continue
                        try { visit(child, index, depth + 1) } finally { child.recycle() }
                    }
                }
                visit(root, -1, 0)
            }
        } finally { root.recycle() }
        document.add("nodes", nodes)
        document.addProperty("truncated", truncated)
        val bytes = GsonBuilder().setPrettyPrinting().create().toJson(document).toByteArray(Charsets.UTF_8)
        val atomic = AtomicFile(file)
        val output = atomic.startWrite()
        try { output.write(bytes); output.fd.sync(); atomic.finishWrite(output) }
        catch (failure: Throwable) { atomic.failWrite(output); throw failure }
    }

    private fun descendants(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (index in 0 until view.childCount) yieldAll(descendants(view.getChildAt(index)))
    }

    @Suppress("DEPRECATION")
    private fun publicResolution() = DefaultInstallerUiRecovery.probes.map { intent ->
        context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo
            ?.let { ComponentName(it.packageName, it.name).flattenToString() }
    }

    private fun await(message: String, timeoutMillis: Long, condition: () -> Boolean) {
        assertTrue(message, waitUntil(timeoutMillis, condition))
    }

    private fun waitUntil(timeoutMillis: Long, condition: () -> Boolean): Boolean {
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return true
            SystemClock.sleep(60)
        }
        return condition()
    }

    private fun evidence(message: String) = instrumentation.sendStatus(0, Bundle().apply {
        putString("default-installer-ui", "${Build.MODEL} API=${Build.VERSION.SDK_INT} $message")
    })
}
