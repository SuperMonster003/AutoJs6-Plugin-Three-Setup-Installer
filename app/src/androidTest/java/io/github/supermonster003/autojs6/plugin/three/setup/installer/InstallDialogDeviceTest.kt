package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.KeyguardManager
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.lifecycle.Lifecycle
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.DeviceUsers
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallDocuments
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallRequest
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallSession
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.NoneInstallEngine
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.SourceEntry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PlannedApk
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallDialogActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallRecoveryPersistence
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallRecoverySnapshot
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class InstallDialogDeviceTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @Test fun confirmationDraftSurvivesRecreationAndProgressNeedsAnAuthoritativeResult() {
        unlocked()
        val file = fixture()
        val record = InstallPresentation.create(context, request(), InstallPresentation.Callbacks(cancel = {}))
        val done = CountDownLatch(1)
        val choice = AtomicReference<InstallPresentation.Choice>()
        val failure = AtomicReference<Throwable>()
        var worker: Thread? = null
        try {
            ActivityScenario.launch<InstallDialogActivity>(record.activityIntent()).useOwnedWindow("InstallDialog") { scenario ->
                var originalTask = -1
                scenario.onActivity { originalTask = it.taskId }
                worker = Thread {
                    try { choice.set(record.confirm(0, prepared(file), target(), InstallOptions(), SystemClock.elapsedRealtime() + 20_000) {}) }
                    catch (error: Throwable) { failure.set(error) }
                    finally { done.countDown() }
                }.apply { start() }
                waitUntil { record.snapshot().prompt != null }
                waitForView(scenario, InstallDialogActivity.TAG_CONFIRM)
                scenario.onActivity { activity ->
                    val base = activity.window.decorView.findViewWithTag<CheckBox>("install_split_base.apk")
                    assertTrue(base.isChecked)
                    assertFalse(base.isEnabled)
                    activity.window.decorView.findViewWithTag<CheckBox>("install_split_feature.apk").performClick()
                    assertEquals(setOf("base.apk"), record.snapshot().prompt!!.choices.snapshot().selectedApkNames)
                }
                scenario.recreate()
                waitForView(scenario, InstallDialogActivity.TAG_CONFIRM)
                assertSinglePresentation(record.token, originalTask)
                assertEquals(1L, done.count)
                scenario.onActivity { activity ->
                    val base = activity.window.decorView.findViewWithTag<CheckBox>("install_split_base.apk")
                    assertTrue(base.isChecked)
                    assertFalse(base.isEnabled)
                    assertFalse(activity.window.decorView.findViewWithTag<CheckBox>("install_split_feature.apk").isChecked)
                    activity.window.decorView.findViewWithTag<View>(InstallDialogActivity.TAG_CONFIRM).performClick()
                }
                assertTrue(done.await(5, TimeUnit.SECONDS))
                assertNull(failure.get())
                assertEquals(setOf("base.apk"), choice.get().selectedApkNames)
                record.onStage(InstallerContract.STAGE_WRITING, InstallDocuments.stageDetail(0, "fixture.package"))
                record.onProgress(0.37f, InstallDocuments.progressDetail(0, 37, 100))
                waitForView(scenario, "install_progress_text")
                scenario.onActivity { activity ->
                    assertEquals(activity.getString(R.string.install_progress_percent, activity.getString(R.string.install_writing), 37),
                        activity.window.decorView.findViewWithTag<TextView>("install_progress_text").text.toString())
                }
                record.onStage(InstallerContract.STAGE_COMMITTING, InstallDocuments.stageDetail(0, "fixture.package"))
                instrumentation.waitForIdleSync()
                scenario.onActivity { activity -> assertTrue(activity.window.decorView.findViewWithTag<ProgressBar>("install_progress_bar").isIndeterminate) }
                record.onStage(InstallerContract.STAGE_COMPLETED, InstallDocuments.stageDetail(0, "fixture.package"))
                assertFalse(record.snapshot().terminal)
                record.onCompleted(success())
                waitForView(scenario, InstallDialogActivity.TAG_DONE)
                assertTrue(record.snapshot().terminal)
                assertSinglePresentation(record.token, originalTask)
                // Exercise the real completion action and require destruction before the test
                // harness adds its synthetic EmptyActivity transition while closing the scenario.
                scenario.onActivity { it.window.decorView.findViewWithTag<View>(InstallDialogActivity.TAG_DONE).performClick() }
                waitUntil { scenario.state == Lifecycle.State.DESTROYED }
                assertNull(InstallPresentation.find(record.token))
            }
        } finally { record.close(); worker?.interrupt(); worker?.join(5_000); file.delete() }
    }

    @Test fun decliningConfirmationNeverAllocatesAnInstallerSession() {
        unlocked()
        val before = context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet()
        val file = fixture()
        val cancelCallbacks = AtomicInteger()
        val record = InstallPresentation.create(context, request(), InstallPresentation.Callbacks(cancel = { cancelCallbacks.incrementAndGet() }))
        val done = CountDownLatch(1)
        val failure = AtomicReference<InstallFailure>()
        var worker: Thread? = null
        try {
            ActivityScenario.launch<InstallDialogActivity>(record.activityIntent()).useOwnedWindow("InstallDialog") { scenario ->
                worker = Thread {
                    try { record.confirm(0, prepared(file), target(), InstallOptions(), SystemClock.elapsedRealtime() + 20_000) {} }
                    catch (error: InstallFailure) { failure.set(error); record.onFailed(error) }
                    finally { done.countDown() }
                }.apply { start() }
                waitForView(scenario, InstallDialogActivity.TAG_CONFIRM)
                scenario.onActivity { it.window.decorView.findViewWithTag<View>(InstallDialogActivity.TAG_CANCEL).performClick() }
                record.cancel()
                assertTrue(done.await(5, TimeUnit.SECONDS))
                assertEquals(InstallerErrorCodes.USER_CANCELLED, failure.get().code)
                assertEquals(0, cancelCallbacks.get())
                assertEquals(before, context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet())
                waitForView(scenario, InstallDialogActivity.TAG_DONE)
            }
        } finally { record.close(); worker?.interrupt(); worker?.join(5_000); file.delete() }
    }

    @Test fun failureShowsCodeSystemMessageAndCopiesThem() {
        unlocked()
        val record = InstallPresentation.create(context, request(), InstallPresentation.Callbacks(cancel = {}))
        try {
            record.onFailed(InstallFailure(InstallerErrorCodes.INSTALL_FAILED, "private debug text", systemMessage = "INSTALL_FAILED_VERSION_DOWNGRADE"))
            ActivityScenario.launch<InstallDialogActivity>(record.activityIntent()).useOwnedWindow("InstallDialog") { scenario ->
                val deadline = SystemClock.elapsedRealtime() + 10_000
                waitUntil(deadline) {
                    var ready = false
                    scenario.onActivity { ready = it.window.decorView.readyForWindowInput() }
                    ready
                }
                var shown = ""
                scenario.onActivity { activity ->
                    check(activity.window.decorView.readyForWindowInput()) { "The copy window lost focus before the click" }
                    shown = activity.window.decorView.findViewWithTag<TextView>(InstallDialogActivity.TAG_ERROR).text.toString()
                    assertTrue(shown.contains("INSTALL_FAILED"))
                    assertTrue(shown.contains("INSTALL_FAILED_VERSION_DOWNGRADE"))
                    assertFalse(shown.contains("private debug text"))
                    activity.window.decorView.findViewWithTag<View>(InstallDialogActivity.TAG_COPY).performClick()
                }
                var copied: String? = null
                waitUntil(deadline) {
                    scenario.onActivity { activity ->
                        copied = if (activity.window.decorView.readyForWindowInput())
                            activity.getSystemService(ClipboardManager::class.java).primaryClip
                                ?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
                        else null
                    }
                    copied == shown
                }
                assertEquals(shown, copied)
            }
        } finally { record.close() }
    }

    @Test fun missingProcessRecordShowsInterruptionAndNeverRecreatesAnOperation() {
        unlocked()
        val token = UUID.randomUUID().toString()
        val intent = Intent(context, InstallDialogActivity::class.java).putExtra(InstallPresentation.EXTRA_TOKEN, token)
            .setData(Uri.parse("three-setup-install://session/$token"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        ActivityScenario.launch<InstallDialogActivity>(intent).useOwnedWindow("InstallDialog") { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity.window.decorView.findViewWithTag<View>(InstallDialogActivity.TAG_DONE))
                assertNull(activity.window.decorView.findViewWithTag<View>(InstallDialogActivity.TAG_CONFIRM))
            }
            scenario.recreate()
            assertNull(InstallPresentation.find(token))
        }
    }

    @Test fun batchRetryStartsOnlyTheSelectedFailedItemAndKeepsTheOldOutcome() {
        unlocked()
        val retries = mutableListOf<Int>()
        val record = InstallPresentation.create(context, request(batch = true), InstallPresentation.Callbacks(cancel = {}, retry = { retries += it; true }))
        try {
            val error = InstallDocuments.failedItem(InstallFailure(InstallerErrorCodes.SOURCE_UNREADABLE, "Missing"), null, null, null, 0)
            record.onItemResult(0, error)
            record.onCompleted(InstallDocuments.batch(listOf(error, success())))
            val oldOutcome = record.snapshot().items.map { it.result.toString() }
            ActivityScenario.launch<InstallDialogActivity>(record.activityIntent()).useOwnedWindow("InstallDialog") { scenario ->
                scenario.onActivity { it.window.decorView.findViewWithTag<View>("install_retry_0").performClick() }
                assertEquals(listOf(0), retries)
                assertEquals(oldOutcome, record.snapshot().items.map { it.result.toString() })
            }
        } finally { record.close() }
    }

    @Test fun cancellationAfterAnItemResultPreservesThatConfirmedInstallation() {
        val record = InstallPresentation.create(context, request(batch = true), InstallPresentation.Callbacks(cancel = {}))
        try {
            record.onItemResult(0, success())
            record.onFailed(InstallFailure(InstallerErrorCodes.CANCELLED, "Cancellation between items"))
            assertTrue(record.snapshot().items[0].result!!.get(InstallerContract.FIELD_OK).asBoolean)
            assertEquals(InstallerContract.STAGE_COMPLETED, record.snapshot().items[0].stage)
            assertEquals(InstallerContract.STAGE_CANCELLED, record.snapshot().items[1].stage)
        } finally { record.close() }
    }

    @Test fun batchCancellationMarksTheWorkerBeforeReleasingItsConfirmation() {
        unlocked()
        val file = fixture()
        val pendingAtCancellation = AtomicBoolean()
        lateinit var prompt: InstallPresentation.Prompt
        val record = InstallPresentation.create(context, request(batch = true), InstallPresentation.Callbacks(cancel = {
            pendingAtCancellation.set(!prompt.decision.await(0))
        }))
        val done = CountDownLatch(1)
        var worker: Thread? = null
        try {
            ActivityScenario.launch<InstallDialogActivity>(record.activityIntent()).useOwnedWindow("InstallDialog") { scenario ->
                worker = Thread {
                    try { record.confirm(0, prepared(file), target(), InstallOptions(), SystemClock.elapsedRealtime() + 20_000) {} }
                    catch (_: InstallFailure) { }
                    finally { done.countDown() }
                }.apply { start() }
                waitForView(scenario, InstallDialogActivity.TAG_CONFIRM)
                prompt = requireNotNull(record.snapshot().prompt)
                record.cancel()
                assertTrue(done.await(5, TimeUnit.SECONDS))
                assertTrue(pendingAtCancellation.get())
                record.onFailed(InstallFailure(InstallerErrorCodes.CANCELLED, "Batch cancelled"))
                waitForView(scenario, InstallDialogActivity.TAG_DONE)
                scenario.onActivity { it.window.decorView.findViewWithTag<View>(InstallDialogActivity.TAG_DONE).performClick() }
                waitUntil { scenario.state == Lifecycle.State.DESTROYED }
            }
        } finally { record.close(); worker?.interrupt(); worker?.join(5_000); file.delete() }
    }

    @Test fun cancellationAfterApprovalReachesTheWorkerExactlyOnceBeforeThePromptIsCleared() {
        unlocked()
        val file = fixture()
        val approved = CountDownLatch(1)
        val releaseWorker = CountDownLatch(1)
        val blockedOnce = AtomicBoolean()
        val cancelled = AtomicBoolean()
        val cancelCalls = AtomicInteger()
        val record = InstallPresentation.create(context, request(), InstallPresentation.Callbacks(cancel = {
            cancelCalls.incrementAndGet()
            cancelled.set(true)
        }))
        val done = CountDownLatch(1)
        val failure = AtomicReference<InstallFailure>()
        var worker: Thread? = null
        try {
            ActivityScenario.launch<InstallDialogActivity>(record.activityIntent()).useOwnedWindow("InstallDialog") { scenario ->
                worker = Thread {
                    try {
                        record.confirm(0, prepared(file), target(), InstallOptions(), SystemClock.elapsedRealtime() + 20_000) {
                            if (record.snapshot().prompt?.decision?.result() != null && blockedOnce.compareAndSet(false, true)) {
                                approved.countDown()
                                check(releaseWorker.await(8, TimeUnit.SECONDS)) { "The test did not release the approved worker" }
                            }
                            if (cancelled.get()) throw InstallFailure(InstallerErrorCodes.CANCELLED, "Execution cancelled after approval")
                        }
                    } catch (error: Throwable) { failure.set(InstallFailure.from(error)) }
                    finally { done.countDown() }
                }.apply { start() }
                waitForView(scenario, InstallDialogActivity.TAG_CONFIRM)
                scenario.onActivity { it.window.decorView.findViewWithTag<View>(InstallDialogActivity.TAG_CONFIRM).performClick() }
                assertTrue(approved.await(5, TimeUnit.SECONDS))
                assertNotNull(record.snapshot().prompt)
                val cancellations = List(2) { Thread { record.cancel() } }
                cancellations.forEach { it.start() }
                cancellations.forEach { it.join(2_000); assertFalse("Cancellation must not wait for the blocked worker", it.isAlive) }
                assertEquals(1, cancelCalls.get())
                releaseWorker.countDown()
                assertTrue(done.await(5, TimeUnit.SECONDS))
                assertEquals(InstallerErrorCodes.CANCELLED, failure.get()?.code)
                record.onFailed(requireNotNull(failure.get()))
                waitForView(scenario, InstallDialogActivity.TAG_DONE)
                scenario.onActivity { it.window.decorView.findViewWithTag<View>(InstallDialogActivity.TAG_DONE).performClick() }
                waitUntil { scenario.state == Lifecycle.State.DESTROYED }
            }
        } finally { releaseWorker.countDown(); record.close(); worker?.interrupt(); worker?.join(5_000); file.delete() }
    }

    /** Injects a display result only; the referenced system package is never installed or opened. */
    @Test fun confirmedOtherUserCannotOpenTheCurrentUsersPackageAndDeletionFailureStaysSuccessful() {
        unlocked()
        val launchablePackage = "com.android.settings"
        assumeTrue("A launchable current-user package is needed for this display regression",
            context.packageManager.getLaunchIntentForPackage(launchablePackage) != null)
        val file = fixture()
        val otherUser = if (DeviceUsers(context).currentId == 10) "11" else "10"
        val record = InstallPresentation.create(context, request(), InstallPresentation.Callbacks(cancel = {}), canDeleteSource = true)
        val done = CountDownLatch(1)
        val choice = AtomicReference<InstallPresentation.Choice>()
        val failure = AtomicReference<Throwable>()
        var worker: Thread? = null
        try {
            ActivityScenario.launch<InstallDialogActivity>(record.activityIntent()).useOwnedWindow("InstallDialog") { scenario ->
                worker = Thread {
                    try { choice.set(record.confirm(0, prepared(file), target(), InstallOptions(deleteSource = true), SystemClock.elapsedRealtime() + 20_000) {}) }
                    catch (error: Throwable) { failure.set(error) }
                    finally { done.countDown() }
                }.apply { start() }
                waitForView(scenario, InstallDialogActivity.TAG_CONFIRM)
                scenario.onActivity { activity ->
                    activity.window.decorView.findViewWithTag<View>("install_target_user_custom").performClick()
                    activity.window.decorView.findViewWithTag<EditText>("install_user_input").setText(otherUser)
                    activity.window.decorView.findViewWithTag<View>(InstallDialogActivity.TAG_CONFIRM).performClick()
                }
                assertTrue(done.await(5, TimeUnit.SECONDS))
                assertNull(failure.get())
                assertEquals(otherUser, choice.get().options.user)
                record.onCompleted(InstallDocuments.installResult(launchablePackage, "1", 1, null, "none", "dialog", 10, false, emptyList()))
                waitForView(scenario, InstallDialogActivity.TAG_DONE)
                scenario.onActivity { activity ->
                    assertFalse(activity.window.decorView.findViewWithTag<View>(InstallDialogActivity.TAG_OPEN).isEnabled)
                    assertEquals(activity.getString(R.string.install_open_in_profile, otherUser),
                        activity.window.decorView.findViewWithTag<TextView>(InstallDialogActivity.TAG_OPEN_PROFILE).text.toString())
                    assertNotNull(activity.window.decorView.findViewWithTag<View>(InstallDialogActivity.TAG_SOURCE_NOT_DELETED))
                    assertTrue(record.snapshot().items.single().result!!.get(InstallerContract.FIELD_OK).asBoolean)
                    activity.window.decorView.findViewWithTag<View>(InstallDialogActivity.TAG_DONE).performClick()
                }
                waitUntil { scenario.state == Lifecycle.State.DESTROYED }
            }
        } finally { record.close(); worker?.interrupt(); worker?.join(5_000); file.delete() }
    }

    @Test fun persistedPartialResultRestoresReadOnlyWithoutAnInstallationWorker() {
        unlocked()
        val before = context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet()
        val value = recoveryFixture()
        val writer = InstallRecoveryPersistence.writer(context)
        val ticket = writer.begin(value.token)
        assertTrue(ticket.save(value, true)!!.await())
        try {
            ActivityScenario.launch<InstallDialogActivity>(recoveryIntent(value.token)).useOwnedWindow("InstallDialog") { scenario ->
                waitForView(scenario, "install_recovery_explanation")
                scenario.onActivity { activity ->
                    val text = viewText(activity.window.decorView)
                    assertTrue(text.contains("Confirmed recovery fixture"))
                    assertTrue(text.contains("example.recovery.confirmed"))
                    assertTrue(text.contains(activity.getString(R.string.install_success)))
                    assertTrue(text.contains(activity.getString(R.string.install_cancelled)))
                    assertNull(activity.window.decorView.findViewWithTag<View>(InstallDialogActivity.TAG_CONFIRM))
                    assertNull(activity.window.decorView.findViewWithTag<View>(InstallDialogActivity.TAG_CANCEL))
                    assertNull(activity.window.decorView.findViewWithTag<View>(InstallDialogActivity.TAG_OPEN))
                    assertNull(activity.window.decorView.findViewWithTag<View>("install_retry_1"))
                }
                assertNull(InstallPresentation.find(value.token))
                scenario.recreate()
                waitForView(scenario, "install_recovery_explanation")
                scenario.onActivity { it.window.decorView.findViewWithTag<View>(InstallDialogActivity.TAG_DONE).performClick() }
                waitUntil { scenario.state == Lifecycle.State.DESTROYED }
                assertFalse(ticket.save(value.copy(revision = 100), true)!!.await())
                assertNull(readRecovery(value.token))
                assertEquals(before, context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet())
            }
        } finally { ticket.close() }
    }

    @Test fun finishingDuringDiskReadRejectsLateRestorationAcrossRecreation() {
        unlocked()
        val value = recoveryFixture()
        val writer = InstallRecoveryPersistence.writer(context)
        val ticket = writer.begin(value.token)
        assertTrue(ticket.save(value, true)!!.await())
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        writer.read(UUID.randomUUID().toString()) { entered.countDown(); release.await(8, TimeUnit.SECONDS) }
        assertTrue(entered.await(5, TimeUnit.SECONDS))
        try {
            ActivityScenario.launch<InstallDialogActivity>(recoveryIntent(value.token)).useOwnedWindow("InstallDialog") { scenario ->
                scenario.onActivity { it.window.decorView.findViewWithTag<View>(InstallDialogActivity.TAG_DONE).performClick() }
                scenario.recreate()
                release.countDown()
                waitUntil { scenario.state == Lifecycle.State.DESTROYED }
                assertNull(InstallPresentation.find(value.token))
                assertFalse(ticket.save(value.copy(revision = 50), true)!!.await())
                assertNull(readRecovery(value.token))
            }
        } finally { release.countDown(); ticket.close() }
    }

    @Test fun durableFailureRestoresItsSafeErrorCodeWithoutRetryOrOpen() {
        unlocked()
        val initial = recoveryFixture()
        val value = initial.copy(terminal = true, stage = InstallerContract.STAGE_FAILED, index = 0,
            items = listOf(initial.items.first().copy(ok = false, failure = InstallRecoverySnapshot.Failure(
                InstallerErrorCodes.SIGNATURE_MISMATCH, 5, "INSTALL_FAILED_UPDATE_INCOMPATIBLE"))))
        val ticket = InstallRecoveryPersistence.writer(context).begin(value.token)
        assertTrue(ticket.save(value, true)!!.await())
        try {
            ActivityScenario.launch<InstallDialogActivity>(recoveryIntent(value.token)).useOwnedWindow("InstallDialog") { scenario ->
                waitForView(scenario, InstallDialogActivity.TAG_ERROR)
                scenario.onActivity { activity ->
                    val message = activity.window.decorView.findViewWithTag<TextView>(InstallDialogActivity.TAG_ERROR).text.toString()
                    assertTrue(message.contains(InstallerErrorCodes.SIGNATURE_MISMATCH))
                    assertTrue(message.contains("INSTALL_FAILED_UPDATE_INCOMPATIBLE"))
                    assertNull(activity.window.decorView.findViewWithTag<View>("install_retry_0"))
                    assertNull(activity.window.decorView.findViewWithTag<View>(InstallDialogActivity.TAG_OPEN))
                    activity.window.decorView.findViewWithTag<View>(InstallDialogActivity.TAG_DONE).performClick()
                }
                waitUntil { scenario.state == Lifecycle.State.DESTROYED }
            }
        } finally { ticket.close() }
    }

    private fun recoveryFixture(): InstallRecoverySnapshot {
        val now = System.currentTimeMillis()
        return InstallRecoverySnapshot(UUID.randomUUID().toString(), 4, now, now + InstallRecoverySnapshot.RETENTION_MILLIS,
            false, InstallerContract.STAGE_COMMITTING, 1, listOf(
                InstallRecoverySnapshot.Item("Confirmed recovery fixture", "example.recovery.confirmed", "2.0", 2, 1, "current", false, true, false, null),
                InstallRecoverySnapshot.Item("Interrupted recovery fixture", "example.recovery.pending", "1.0", 1, null, "current", false, null, null, null)))
    }
    private fun recoveryIntent(token: String) = Intent(context, InstallDialogActivity::class.java)
        .putExtra(InstallPresentation.EXTRA_TOKEN, token).setData(Uri.parse("three-setup-install://session/$token"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    private fun viewText(view: View): String = when (view) {
        is TextView -> view.text.toString()
        is ViewGroup -> (0 until view.childCount).joinToString("\n") { viewText(view.getChildAt(it)) }
        else -> ""
    }
    private fun readRecovery(token: String): InstallRecoverySnapshot? {
        val done = CountDownLatch(1)
        val result = AtomicReference<InstallRecoverySnapshot>()
        InstallRecoveryPersistence.writer(context).read(token) { result.set(it); done.countDown() }
        assertTrue(done.await(5, TimeUnit.SECONDS))
        return result.get()
    }

    private fun request(batch: Boolean = false): InstallRequest = InstallRequest("ui-${UUID.randomUUID()}",
        listOf(SourceEntry(0, 0, "fixture.apk", -1)) + if (batch) listOf(SourceEntry(1, 1, "other.apk", -1)) else emptyList(),
        InstallerContract.INTERACTION_DIALOG, InstallOptions())

    private fun target() = InstallSession.Target(Authorizer.NONE, DeviceUsers(context).currentId, NoneInstallEngine(context))
    private fun success(): JsonObject = InstallDocuments.installResult("fixture.package", "1.0", 1, null, "none", "dialog", 10, false, emptyList())
    private fun fixture() = File(context.cacheDir, "install-ui-${UUID.randomUUID()}.apk").also { file ->
        instrumentation.context.assets.open("fixture-v1.apk").use { input -> file.outputStream().use(input::copyTo) }
    }
    private fun prepared(file: File) = PreparedPackage("apks", "fixture.apks", file.length(), "fixture.package", "1.0", 1, "Fixture", 24, 35,
        listOf(PlannedApk("base.apk", file, file.length(), null), PlannedApk("feature.apk", file, file.length(), "feature")),
        emptyList(), null, emptyList(), emptyList(), true)

    private fun unlocked() = assumeFalse("Unlock the device for installation UI tests", context.getSystemService(KeyguardManager::class.java).isKeyguardLocked)
    private fun waitForView(scenario: ActivityScenario<InstallDialogActivity>, tag: String) = waitUntil {
        var found = false
        scenario.onActivity { found = it.window.decorView.findViewWithTag<View>(tag) != null }
        found
    }
    private fun assertSinglePresentation(token: String, taskId: Int) {
        instrumentation.runOnMainSync {
            val activities = listOf(Stage.CREATED, Stage.STARTED, Stage.RESUMED, Stage.PAUSED, Stage.STOPPED)
                .flatMap { ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(it) }
                .filterIsInstance<InstallDialogActivity>()
                .filter { !it.isDestroyed && it.intent.getStringExtra(InstallPresentation.EXTRA_TOKEN) == token }.distinct()
            assertEquals("The presentation must have one Activity", 1, activities.size)
            assertEquals("Recreation must preserve its document task", taskId, activities.single().taskId)
        }
    }
    private fun waitUntil(deadline: Long = SystemClock.elapsedRealtime() + 10_000, condition: () -> Boolean) {
        while (!condition()) {
            if (SystemClock.elapsedRealtime() >= deadline) fail("Installation UI did not reach the expected state")
            SystemClock.sleep(25)
        }
    }
}
