package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import android.content.Intent
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.ArchiveOpener
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PlannedApk
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.Closeable
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit

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

    @Test fun `dialog selection replaces APKs options target user and the actual engine`() {
        val fixture = Fixture(request(items = listOf(0, 0), interaction = "dialog"))
        val selectedCalls = mutableListOf<InstallEngine.Request>()
        val selectedEngine = object : InstallEngine {
            override fun install(request: InstallEngine.Request, listener: InstallEngine.Listener, checkCancelled: () -> Unit): InstallEngine.Result {
                checkCancelled()
                selectedCalls += request
                return InstallEngine.Result("example.package", emptyList(), "dialog")
            }
        }
        val selectedOptions = fixture.session.request.options.copy(authorizer = "shizuku", user = "7", deleteSource = false, allowDowngrade = true)
        val selectedTarget = InstallSession.Target(Authorizer.SHIZUKU, 7, selectedEngine)
        var selectedPackage: PreparedPackage? = null
        fixture.expectedUserId = 7
        fixture.confirm = { fail("The configuration callback must not invoke the obsolete confirmation a second time") }
        fixture.configuration = { index, prepared, target, original ->
            assertEquals(0, index)
            assertEquals(42, target.userId)
            assertEquals(2, prepared.apks.size)
            assertSame(fixture.session.request, original)
            selectedPackage = ArchiveOpener.select(prepared, setOf(requireNotNull(prepared.baseApk).name))
            InstallSession.Selection(requireNotNull(selectedPackage), selectedTarget, selectedOptions)
        }
        fixture.run()
        assertTrue(fixture.calls.isEmpty())
        val submitted = selectedCalls.single()
        assertSame(selectedPackage, submitted.prepared)
        assertEquals(1, submitted.prepared.apks.size)
        assertEquals(selectedOptions, submitted.options)
        assertEquals(7, submitted.userId)
        assertEquals("dialog", submitted.interaction)
        assertEquals(1_100L, submitted.deadlineMillis)
        assertEquals(listOf(selectedTarget, selectedTarget), fixture.versionTargets)
        assertEquals(listOf(0 to selectedOptions), fixture.cleanups)
        assertEquals("shizuku", requireNotNull(fixture.completed)["authorizer"].asString)
        assertEquals("root", fixture.session.request.options.authorizer)
        assertTrue(fixture.session.request.options.deleteSource)
    }

    @Test fun `source deletion runs between confirmed installation and the item result`() {
        val fixture = Fixture(request(items = listOf(0)))
        fixture.install = {
            fixture.events += "platform-success"
            InstallEngine.Result("example.package", listOf("platform note"), "silent")
        }
        fixture.cleanup = { _, options ->
            assertTrue(options.deleteSource)
            assertEquals("platform-success", fixture.events[fixture.events.lastIndex - 1])
            InstallSession.SourceCleanup(deleted = true, notes = listOf("source removed"))
        }
        fixture.run()
        assertEquals(listOf("platform-success", "cleanup:0", "item:0", "discard:0", "close", "completed"), fixture.events)
        val result = requireNotNull(fixture.completed)
        assertTrue(result["ok"].asBoolean)
        assertTrue(result["sourceDeleted"].asBoolean)
        assertEquals(listOf("platform note", "source removed"), result.getAsJsonArray("notes").map { it.asString })
    }

    @Test fun `preparation confirmation and installation failures never invoke source deletion`() {
        for (phase in listOf("preparation", "confirmation", "installation")) {
            val fixture = Fixture(request(items = listOf(0), interaction = "dialog"))
            val fail = { throw InstallFailure(InstallerErrorCodes.USER_CANCELLED, "Stopped during $phase") }
            when (phase) {
                "preparation" -> fixture.afterPrepare = fail
                "confirmation" -> fixture.confirm = fail
                else -> fixture.install = fail
            }
            fixture.run()
            assertTrue("$phase must retain its original source", fixture.cleanups.isEmpty())
            assertEquals("USER_CANCELLED", fixture.failure?.code)
            assertEquals(listOf(0), fixture.discarded)
        }
    }

    @Test fun `source deletion failure preserves success and lets later items install`() {
        val fixture = Fixture(request())
        fixture.cleanup = { index, _ ->
            if (index == 0) throw SecurityException("Provider refused deletion")
            InstallSession.SourceCleanup(deleted = true)
        }
        fixture.run()
        assertEquals(listOf(0, 1, 2), fixture.cleanups.map { it.first })
        assertEquals(listOf(true, true, true), fixture.results().map { it["ok"].asBoolean })
        assertEquals(listOf(false, true, true), fixture.results().map { it["sourceDeleted"].asBoolean })
        assertTrue(fixture.results()[0].getAsJsonArray("notes").any { it.asString.contains("cleanup") })
        assertNull(fixture.failure)
    }

    @Test fun `late cancellation keeps the installed item cleanup and never deletes unstarted sources`() {
        val fixture = Fixture(request())
        fixture.install = { fixture.session.cancel(); success() }
        fixture.cleanup = { _, _ -> InstallSession.SourceCleanup(deleted = true) }
        fixture.run()
        assertEquals(listOf(0), fixture.cleanups.map { it.first })
        assertTrue(fixture.results().first()["ok"].asBoolean)
        assertTrue(fixture.results().first()["sourceDeleted"].asBoolean)
        assertEquals(listOf(false, false), fixture.results().drop(1).map { it["ok"].asBoolean })
        assertFalse(Thread.currentThread().isInterrupted)
    }

    @Test fun `item result observer exceptions cannot undo success or stop the remaining batch`() {
        val fixture = Fixture(request())
        fixture.itemResult = { _, _ -> throw IllegalStateException("UI was destroyed") }
        fixture.run()
        assertEquals(listOf(0, 1, 2), fixture.itemResults.map { it.first })
        assertEquals(3, fixture.calls.size)
        assertTrue(fixture.results().all { it["ok"].asBoolean })
        assertEquals(1, fixture.terminalCallbacks)
        assertNull(fixture.failure)
    }

    @Test fun `item observer cannot mutate the confirmed results returned to the host`() {
        val fixture = Fixture(request())
        fixture.itemResult = { _, result ->
            result.addProperty("ok", false)
            result.remove("packageName")
            result.getAsJsonArray("notes").add("observer mutation")
            throw IllegalStateException("Observer failed after mutating its copy")
        }
        fixture.run()
        assertTrue(fixture.results().all { it["ok"].asBoolean })
        assertTrue(fixture.results().all { it["packageName"].asString == "example.package" })
        assertTrue(fixture.results().all { it.getAsJsonArray("notes").size() == 0 })
    }

    @Test fun `global installation capacity rejects a fifth caller and double close releases only one slot`() {
        val leases = mutableListOf<Closeable>()
        try {
            repeat(4) { leases += InstallSlots.acquire() }
            assertEquals("INVALID_ARGUMENT", assertThrows(InstallFailure::class.java) { InstallSlots.acquire() }.code)
            leases.first().close()
            leases.first().close()
            leases += InstallSlots.acquire()
            assertEquals("INVALID_ARGUMENT", assertThrows(InstallFailure::class.java) { InstallSlots.acquire() }.code)
        } finally { leases.forEach { it.close() } }
        val restored = mutableListOf<Closeable>()
        try { repeat(4) { restored += InstallSlots.acquire() } } finally { restored.forEach { it.close() } }
    }

    @Test fun `simultaneous host external and retry callers still share four installation slots`() {
        val start = CountDownLatch(1)
        val attempted = CountDownLatch(12)
        val release = CountDownLatch(1)
        val acquired = ConcurrentLinkedQueue<Closeable>()
        val rejected = ConcurrentLinkedQueue<Throwable>()
        val workers = Executors.newFixedThreadPool(12)
        try {
            repeat(12) {
                workers.execute {
                    start.await()
                    var lease: Closeable? = null
                    try { InstallSlots.acquire().also { lease = it; acquired.add(it) } }
                    catch (failure: Throwable) { rejected.add(failure) }
                    finally { attempted.countDown() }
                    try { release.await() } finally { lease?.close() }
                }
            }
            start.countDown()
            assertTrue(attempted.await(5, TimeUnit.SECONDS))
            assertEquals(4, acquired.size)
            assertEquals(8, rejected.size)
            assertTrue(rejected.all { it is InstallFailure && it.code == InstallerErrorCodes.INVALID_ARGUMENT })
        } finally {
            start.countDown()
            release.countDown()
            workers.shutdown()
            if (!workers.awaitTermination(5, TimeUnit.SECONDS)) workers.shutdownNow()
            acquired.forEach { it.close() }
        }
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
        var expectedUserId = 42
        val calls = mutableListOf<InstallEngine.Request>()
        val prepared = mutableListOf<List<Int>>()
        val discarded = mutableListOf<Int>()
        val events = mutableListOf<String>()
        val versionTargets = mutableListOf<InstallSession.Target>()
        val cleanups = mutableListOf<Pair<Int, InstallOptions>>()
        val itemResults = mutableListOf<Pair<Int, JsonObject>>()
        var afterPrepare: () -> Unit = {}
        var confirm: () -> Unit = {}
        var install: () -> InstallEngine.Result = { success() }
        var configuration: ((Int, PreparedPackage, InstallSession.Target, InstallRequest) -> InstallSession.Selection)? = null
        var cleanup: (Int, InstallOptions) -> InstallSession.SourceCleanup = { _, _ -> InstallSession.SourceCleanup() }
        var itemResult: (Int, JsonObject) -> Unit = { _, _ -> }
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
            assertEquals(expectedUserId, target.userId)
            versionTargets += target
            if (++versions % 2 == 0 && failPostMetadata) throw IllegalStateException("metadata unavailable")
            return InstallSession.Version("$versions.0", versions.toLong())
        }
        override fun confirm(prepared: PreparedPackage, target: InstallSession.Target, deadlineMillis: Long, checkActive: () -> Unit) = confirm()
        override fun configure(index: Int, prepared: PreparedPackage, target: InstallSession.Target, request: InstallRequest,
            deadlineMillis: Long, checkActive: () -> Unit): InstallSession.Selection =
            configuration?.invoke(index, prepared, target, request)
                ?: super<InstallSession.Environment>.configure(index, prepared, target, request, deadlineMillis, checkActive)
        override fun onInstalled(index: Int, options: InstallOptions): InstallSession.SourceCleanup {
            cleanups += index to options
            events += "cleanup:$index"
            return cleanup(index, options)
        }
        override fun onUserAction(intent: Intent) = Unit
        override fun discardItem(index: Int) { discarded += index; events += "discard:$index" }
        override fun close() { closes++; events += "close"; if (failCleanup) throw IllegalStateException("cleanup") }
        override fun onProgress(progress: Float, detail: JsonObject) { if (failProgress) throw IllegalStateException("host died") }
        override fun onItemResult(index: Int, result: JsonObject) {
            itemResults += index to result.deepCopy()
            events += "item:$index"
            itemResult(index, result)
        }
        override fun onCompleted(result: JsonObject) { terminalCallbacks++; completed = result; events += "completed" }
        override fun onFailed(failure: InstallFailure) { terminalCallbacks++; this.failure = failure; events += "failed" }
    }
}
