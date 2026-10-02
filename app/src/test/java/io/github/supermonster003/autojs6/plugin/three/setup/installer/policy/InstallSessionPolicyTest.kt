package io.github.supermonster003.autojs6.plugin.three.setup.installer.policy

import android.content.Intent
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.*
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PlannedApk
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import org.autojs.plugin.installer.api.InstallerErrorCodes as E
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class InstallSessionPolicyTest {
    @Test fun blockingRulesRunBeforeEveryConfirmationOrEngine() {
        listOf("auto", "dialog", "silent", "notification").forEach { mode ->
            val fixture = Fixture(mode)
            fixture.rules = { throw InstallFailure(E.BLOCKED_BY_POLICY, "Fixture policy") }
            fixture.run()
            assertEquals(E.BLOCKED_BY_POLICY, fixture.failure?.code)
            assertEquals(0, fixture.confirmations)
            assertEquals(0, fixture.engineCalls)
            assertEquals(0, fixture.sourceDeletions)
            assertTrue(fixture.discarded)
        }
    }

    @Test fun changedFinalFactsRejectWritingAfterDialogConfiguration() {
        val fixture = Fixture("dialog")
        fixture.finalCheck = {
            assertEquals(1, fixture.confirmations)
            assertTrue(fixture.sampledVersion)
            throw InstallFailure(E.BLOCKED_BY_POLICY, "Facts changed")
        }
        fixture.run()
        assertEquals(E.BLOCKED_BY_POLICY, fixture.failure?.code)
        assertEquals(0, fixture.engineCalls)
        assertEquals(0, fixture.sourceDeletions)
    }

    @Test fun reviewDoesNotHoldThePackageLockButFinalValidationWaitsForIt() {
        val fixture = Fixture("dialog")
        val reviewed = CountDownLatch(1)
        val checked = CountDownLatch(1)
        fixture.review = { reviewed.countDown() }
        fixture.finalCheck = { checked.countDown() }
        val held = PackageInstallLocks.acquire("example.policy.fixture") {}
        val worker = Thread { fixture.run() }
        try {
            worker.start()
            assertTrue(reviewed.await(5, TimeUnit.SECONDS))
            assertEquals(1L, checked.count)
            held.close()
            worker.join(5_000)
            assertFalse(worker.isAlive)
            assertEquals(0L, checked.count)
            assertEquals(1, fixture.engineCalls)
            assertNull(fixture.failure)
        } finally { held.close(); worker.interrupt(); worker.join(5_000) }
    }

    private class Fixture(mode: String) : InstallSession.Environment, InstallSession.Listener {
        var rules: () -> Unit = {}
        var review: () -> Unit = {}
        var finalCheck: () -> Unit = {}
        var confirmations = 0
        var engineCalls = 0
        var sourceDeletions = 0
        var discarded = false
        var sampledVersion = false
        var failure: InstallFailure? = null
        private val request = InstallRequest("policy", listOf(SourceEntry(0, 0, "fixture.apk", 1)), mode,
            InstallOptions(authorizer = "root", deleteSource = true))
        private val engine = object : InstallEngine {
            override fun install(request: InstallEngine.Request, listener: InstallEngine.Listener, checkCancelled: () -> Unit): InstallEngine.Result {
                engineCalls++
                return InstallEngine.Result("example.policy.fixture", emptyList(), "silent")
            }
        }
        private val session = InstallSession(request, this, this) { 100L }
        fun run() = session.start(Executor { it.run() })
        override fun resolve(request: InstallRequest, deadlineMillis: Long, checkActive: () -> Unit) = InstallSession.Target(Authorizer.ROOT, 0, engine)
        override fun prepare(index: Int, sources: List<SourceEntry>, checkActive: () -> Unit) = PreparedPackage("apk", "fixture.apk", 1,
            "example.policy.fixture", "1", 1, "Fixture", 24, 28, listOf(PlannedApk("base.apk", File("fixture.apk"), 1, null)),
            emptyList(), null, emptyList(), emptyList(), true)
        override fun checkInstallPolicy(prepared: PreparedPackage) = rules()
        override fun confirm(prepared: PreparedPackage, target: InstallSession.Target, deadlineMillis: Long, checkActive: () -> Unit) { confirmations++ }
        override fun reviewSafety(index: Int, prepared: PreparedPackage, target: InstallSession.Target, options: InstallOptions,
            interaction: String, deadlineMillis: Long, onReviewRequired: () -> Unit, checkActive: () -> Unit): DialogSafetyApproval? { review(); return null }
        override fun validateSafety(index: Int, prepared: PreparedPackage, target: InstallSession.Target, options: InstallOptions,
            approval: DialogSafetyApproval?, checkActive: () -> Unit) = finalCheck()
        override fun installedVersion(packageName: String, target: InstallSession.Target): InstallSession.Version? { sampledVersion = true; return null }
        override fun onInstalled(index: Int, options: InstallOptions): InstallSession.SourceCleanup { sourceDeletions++; return InstallSession.SourceCleanup(true) }
        override fun onUserAction(intent: Intent) = Unit
        override fun discardItem(index: Int) { discarded = true }
        override fun close() = Unit
        override fun onCompleted(result: JsonObject) = Unit
        override fun onFailed(failure: InstallFailure) { this.failure = failure }
    }
}
