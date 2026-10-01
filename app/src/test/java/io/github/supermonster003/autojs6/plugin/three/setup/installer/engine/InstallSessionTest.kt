package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import android.content.Intent
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PlannedApk
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException

class InstallSessionTest {
    private val direct = Executor { it.run() }

    @Test fun `batch installs in first appearance order with one engine call per split set`() {
        val fixture = Fixture(request(items = listOf(7, 7, 2, 9)))
        fixture.run()
        assertEquals(listOf(listOf(0, 1), listOf(2), listOf(3)), fixture.prepared)
        assertEquals(listOf(2, 1, 1), fixture.calls.map { it.prepared.apks.size })
        assertEquals(listOf(42, 42, 42), fixture.calls.map { it.userId })
        assertEquals(listOf("auto", "auto", "auto"), fixture.calls.map { it.interaction })
        assertEquals(3, fixture.results().size)
        assertEquals(listOf(0, 1, 2), fixture.discarded)
        assertEquals(listOf("close", "completed"), fixture.events.takeLast(2))
        assertFalse(fixture.session.cancel())
        fixture.session.close()
        assertEquals(1, fixture.closes)
    }

    @Test fun `item failure continues by default and preserves ordered errors`() {
        val fixture = Fixture(request())
        fixture.install = { if (fixture.calls.size == 2) throw InstallFailure(InstallerErrorCodes.SIGNATURE_MISMATCH, "bad signer") else success() }
        fixture.run()
        val results = fixture.results()
        assertEquals(listOf(true, false, true), results.map { it["ok"].asBoolean })
        assertEquals("SIGNATURE_MISMATCH", results[1].getAsJsonObject("error")["code"].asString)
        assertEquals(1, fixture.terminalCallbacks)
    }

    @Test fun `fail fast does not prepare subsequent items and closes resources`() {
        val fixture = Fixture(request(continueOnError = false))
        fixture.install = { throw InstallFailure(InstallerErrorCodes.INSTALL_FAILED, "refused") }
        fixture.run()
        assertEquals(1, fixture.calls.size)
        assertEquals("INSTALL_FAILED", fixture.failure?.code)
        assertEquals(listOf("close", "failed"), fixture.events.takeLast(2))
        assertEquals("failed", fixture.session.status().stage)
    }

    @Test fun `one deadline includes preparation and is reused for every item`() {
        val fixture = Fixture(request(timeout = 50))
        fixture.install = { fixture.now += 30; success() }
        fixture.run()
        assertEquals(listOf(150L, 150L), fixture.calls.map { it.deadlineMillis })
        assertEquals(listOf(true, true, false), fixture.results().map { it["ok"].asBoolean })
        assertEquals("TIMEOUT", fixture.results().last().getAsJsonObject("error")["code"].asString)
        assertEquals(2, fixture.prepared.size)
    }

    @Test fun `expiration during preparation never reaches the engine`() {
        val fixture = Fixture(request(items = listOf(0), timeout = 50))
        fixture.afterPrepare = { fixture.now = 150 }
        fixture.run()
        assertEquals("TIMEOUT", fixture.failure?.code)
        assertTrue(fixture.calls.isEmpty())
        assertEquals(1, fixture.closes)
    }

    @Test fun `late cancel preserves the confirmed success and cancels every remaining item`() {
        val fixture = Fixture(request())
        fixture.install = { fixture.session.cancel(); success() }
        fixture.run()
        assertEquals(1, fixture.calls.size)
        assertEquals(listOf(true, false, false), fixture.results().map { it["ok"].asBoolean })
        assertEquals(listOf("CANCELLED", "CANCELLED"), fixture.results().drop(1).map { it.getAsJsonObject("error")["code"].asString })
        assertFalse(Thread.currentThread().isInterrupted)
    }

    @Test fun `engine actual interaction and installed versions are reported`() {
        val fixture = Fixture(request(items = listOf(0)))
        fixture.install = { InstallEngine.Result("example.package", listOf("system confirmation"), "dialog") }
        fixture.run()
        val result = requireNotNull(fixture.completed)
        assertEquals("dialog", result["interaction"].asString)
        assertEquals("system confirmation", result.getAsJsonArray("notes").single().asString)
        assertEquals(1L, result["previousVersionCode"].asLong)
        assertEquals(2L, result["versionCode"].asLong)
        assertFalse(result["sourceDeleted"].asBoolean)
    }

    @Test fun `metadata or cleanup failure after success cannot report failure`() {
        val fixture = Fixture(request(items = listOf(0)))
        fixture.failPostMetadata = true
        fixture.failCleanup = true
        fixture.run()
        assertTrue(requireNotNull(fixture.completed)["ok"].asBoolean)
        assertEquals(1, fixture.terminalCallbacks)
        assertNull(fixture.failure)
    }

    @Test fun `explicit dialog invokes confirmation before any installation`() {
        val fixture = Fixture(request(items = listOf(0), interaction = "dialog"))
        fixture.confirm = { throw InstallFailure(InstallerErrorCodes.USER_CANCELLED, "declined") }
        fixture.run()
        assertEquals("USER_CANCELLED", fixture.failure?.code)
        assertTrue(fixture.calls.isEmpty())
    }

    @Test fun `prestart cancellation closes owned resources without preparing`() {
        val fixture = Fixture(request())
        assertTrue(fixture.session.cancel())
        fixture.run()
        assertTrue(fixture.prepared.isEmpty())
        assertEquals("CANCELLED", fixture.failure?.code)
        assertEquals(1, fixture.closes)
    }

    @Test fun `close before start and rejected scheduling each finish exactly once`() {
        val closed = Fixture(request())
        closed.session.close()
        closed.session.close()
        assertThrows(IllegalStateException::class.java) { closed.run() }
        assertEquals(1, closed.closes)
        assertEquals(1, closed.terminalCallbacks)
        val rejected = Fixture(request())
        rejected.session.start { throw RejectedExecutionException() }
        assertEquals(1, rejected.closes)
        assertEquals("INTERNAL", rejected.failure?.code)
    }

    @Test fun `interrupt raised by an engine is cleared before the worker is reused`() {
        val first = Fixture(request(items = listOf(0)))
        first.install = { Thread.currentThread().interrupt(); throw InstallFailure(InstallerErrorCodes.CANCELLED, "interrupted") }
        first.run()
        assertFalse(Thread.currentThread().isInterrupted)
        val second = Fixture(request(items = listOf(0)))
        second.run()
        assertNotNull(second.completed)
    }

    @Test fun `lost progress callback stops further installs but still cleans sources`() {
        val fixture = Fixture(request())
        fixture.failProgress = true
        fixture.run()
        assertEquals(1, fixture.calls.size)
        assertTrue(fixture.results().all { !it["ok"].asBoolean })
        assertEquals(1, fixture.closes)
        assertEquals(1, fixture.terminalCallbacks)
    }

    @Test fun `cancelled current package name is not attributed to unstarted items`() {
        val fixture = Fixture(request())
        fixture.install = { throw InstallFailure("CANCELLED", "cancelled current item", packageName = "example.package") }
        fixture.run()
        assertEquals("example.package", fixture.results()[0]["packageName"].asString)
        assertTrue(fixture.results().drop(1).all { !it.has("packageName") && !it.getAsJsonObject("error").has("packageName") })
        assertEquals(2, fixture.session.status().index)
    }

    @Test fun `oversized optional metadata never turns a confirmed success into failure`() {
        val fixture = Fixture(request(items = listOf(0)))
        fixture.install = { InstallEngine.Result("example.package", listOf("x".repeat(65_537)), "silent") }
        fixture.run()
        assertNull(fixture.failure)
        assertTrue(requireNotNull(fixture.completed)["ok"].asBoolean)
        assertTrue(requireNotNull(fixture.completed).toString().toByteArray().size < 65_536)
        assertEquals("completed", fixture.session.status().stage)
        assertEquals(1, fixture.closes)
    }

    private fun request(items: List<Int> = listOf(0, 1, 2), continueOnError: Boolean = true, timeout: Long = 1_000, interaction: String = "auto") =
        InstallRequest("batch", items.mapIndexed { i, item -> SourceEntry(i, item, "same.apk", 1) }, interaction,
            InstallOptions(authorizer = "root", user = "42", deleteSource = true, continueOnError = continueOnError, timeoutMillis = timeout))

    private fun success() = InstallEngine.Result("example.package", emptyList(), "silent")

    private inner class Fixture(request: InstallRequest) : InstallSession.Environment, InstallSession.Listener {
        var now = 100L
        var closes = 0
        var terminalCallbacks = 0
        var completed: JsonObject? = null
        var failure: InstallFailure? = null
        var failCleanup = false
        var failPostMetadata = false
        var failProgress = false
        val calls = mutableListOf<InstallEngine.Request>()
        val prepared = mutableListOf<List<Int>>()
        val discarded = mutableListOf<Int>()
        val events = mutableListOf<String>()
        var afterPrepare: () -> Unit = {}
        var confirm: () -> Unit = {}
        var install: () -> InstallEngine.Result = { success() }
        val session = InstallSession(request, this, this) { now }
        private var versions = 0
        private val engine = object : InstallEngine {
            override fun install(request: InstallEngine.Request, listener: InstallEngine.Listener, checkCancelled: () -> Unit): InstallEngine.Result {
                calls += request
                listener.onStage("writing")
                listener.onProgress(1, 1)
                checkCancelled()
                return install()
            }
        }

        fun run() = session.start(direct)
        fun results() = requireNotNull(completed).getAsJsonArray("results").map { it.asJsonObject }
        override fun resolve(request: InstallRequest, deadlineMillis: Long, checkActive: () -> Unit) = InstallSession.Target(Authorizer.ROOT, 42, engine)
        override fun prepare(index: Int, sources: List<SourceEntry>, checkActive: () -> Unit): PreparedPackage {
            prepared += sources.map { it.descriptor }
            afterPrepare()
            return PreparedPackage("apk", "same.apk", 1, "example.package", "2.0", 2, null, 24, 28,
                sources.mapIndexed { i, _ -> PlannedApk("part$i.apk", File("part$i.apk"), 1, if (i == 0) null else "$i") },
                emptyList(), null, emptyList(), emptyList(), true)
        }
        override fun installedVersion(packageName: String, target: InstallSession.Target): InstallSession.Version {
            assertEquals(42, target.userId)
            if (++versions % 2 == 0 && failPostMetadata) throw IllegalStateException("metadata unavailable")
            return InstallSession.Version("$versions.0", versions.toLong())
        }
        override fun confirm(prepared: PreparedPackage, target: InstallSession.Target, deadlineMillis: Long, checkActive: () -> Unit) = confirm()
        override fun onUserAction(intent: Intent) = Unit
        override fun discardItem(index: Int) { discarded += index }
        override fun close() { closes++; events += "close"; if (failCleanup) throw IllegalStateException("cleanup") }
        override fun onProgress(progress: Float, detail: JsonObject) { if (failProgress) throw IllegalStateException("host died") }
        override fun onCompleted(result: JsonObject) { terminalCallbacks++; completed = result; events += "completed" }
        override fun onFailed(failure: InstallFailure) { terminalCallbacks++; this.failure = failure; events += "failed" }
    }
}
