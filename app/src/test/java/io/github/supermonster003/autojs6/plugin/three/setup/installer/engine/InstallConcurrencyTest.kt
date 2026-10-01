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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class InstallConcurrencyTest {
    @Test fun `one package is serial across authorizers and users while another package runs`() = workers { pool ->
        val versions = ConcurrentHashMap<String, Long>()
        val releaseFirst = CountDownLatch(1)
        val first = Job(listOf("example.serial"), versions, Authorizer.ROOT, 0).apply {
            installing = { waitFor(releaseFirst) }
        }
        val second = Job(listOf("example.serial"), versions, Authorizer.SHIZUKU, 10)
        val unrelated = Job(listOf("example.unrelated"), versions)
        try {
            first.session.start(pool)
            waitFor(first.entered)
            second.session.start(pool)
            waitFor(second.prepared)
            unrelated.session.start(pool)
            waitFor(unrelated.done)
            assertTrue(requireNotNull(unrelated.result)["ok"].asBoolean)
            assertFalse("The second user must not sample a stale installed version", second.versionRead.await(150, TimeUnit.MILLISECONDS))
            assertEquals(0, second.engineCalls.get())
            releaseFirst.countDown()
            waitFor(first.done)
            waitFor(second.done)
            val result = requireNotNull(second.result)
            assertEquals(1L, result["previousVersionCode"].asLong)
            assertEquals(2L, result["versionCode"].asLong)
            assertEquals("shizuku", result["authorizer"].asString)
            assertEquals(10, second.submittedUser)
        } finally { releaseFirst.countDown(); first.session.cancel(); second.session.cancel() }
    }

    @Test fun `a waiting cancellation releases only the waiter and its pooled thread can be reused`() = workers(2) { pool ->
        val versions = ConcurrentHashMap<String, Long>()
        val releaseFirst = CountDownLatch(1)
        val first = Job(listOf("example.cancel"), versions).apply { installing = { waitFor(releaseFirst) } }
        val waiting = Job(listOf("example.cancel"), versions)
        val next = Job(listOf("example.cancel"), versions)
        try {
            first.session.start(pool)
            waitFor(first.entered)
            waiting.session.start(pool)
            waitFor(waiting.prepared)
            assertTrue(waiting.session.cancel())
            waitFor(waiting.done)
            assertEquals(InstallerErrorCodes.CANCELLED, waiting.failure?.code)
            assertEquals(0, waiting.engineCalls.get())
            assertEquals(1, waiting.discards.get())
            next.session.start(pool)
            waitFor(next.prepared)
            assertFalse(next.entered.await(150, TimeUnit.MILLISECONDS))
            releaseFirst.countDown()
            waitFor(first.done)
            waitFor(next.done)
            assertNull(next.failure)
            assertEquals(1L, requireNotNull(next.result)["previousVersionCode"].asLong)
        } finally { releaseFirst.countDown(); first.session.cancel(); waiting.session.cancel(); next.session.cancel() }
    }

    @Test fun `waiting uses the original deadline and never starts an expired platform session`() = workers { pool ->
        val versions = ConcurrentHashMap<String, Long>()
        val now = AtomicLong(100)
        val releaseFirst = CountDownLatch(1)
        val first = Job(listOf("example.timeout"), versions).apply { installing = { waitFor(releaseFirst) } }
        val waiting = Job(listOf("example.timeout"), versions, clock = now::get, timeout = 100)
        try {
            first.session.start(pool)
            waitFor(first.entered)
            waiting.session.start(pool)
            waitFor(waiting.prepared)
            now.set(200)
            waitFor(waiting.done)
            assertEquals(InstallerErrorCodes.TIMEOUT, waiting.failure?.code)
            assertEquals(0, waiting.engineCalls.get())
            assertEquals(1, waiting.discards.get())
            assertEquals(1L, first.done.count)
            releaseFirst.countDown()
            waitFor(first.done)
            val retry = Job(listOf("example.timeout"), versions)
            retry.session.start(pool)
            waitFor(retry.done)
            assertNull(retry.failure)
        } finally { releaseFirst.countDown(); first.session.cancel(); waiting.session.cancel() }
    }

    @Test fun `a failed owner releases after engine cleanup before the next version sample`() = workers { pool ->
        val versions = ConcurrentHashMap<String, Long>()
        val releaseFirst = CountDownLatch(1)
        val cleaned = CountDownLatch(1)
        val first = Job(listOf("example.failed"), versions).apply {
            installing = {
                try { waitFor(releaseFirst); throw InstallFailure(InstallerErrorCodes.INSTALL_FAILED, "platform refused") }
                finally { cleaned.countDown() }
            }
        }
        val second = Job(listOf("example.failed"), versions).apply {
            beforeVersion = { check(cleaned.count == 0L) { "Previous platform handle is still owned" } }
        }
        try {
            first.session.start(pool)
            waitFor(first.entered)
            second.session.start(pool)
            waitFor(second.prepared)
            assertFalse(second.versionRead.await(150, TimeUnit.MILLISECONDS))
            releaseFirst.countDown()
            waitFor(first.done)
            waitFor(second.done)
            assertEquals(InstallerErrorCodes.INSTALL_FAILED, first.failure?.code)
            assertNull(second.failure)
            assertEquals(1L, requireNotNull(second.result)["versionCode"].asLong)
        } finally { releaseFirst.countDown(); first.session.cancel(); second.session.cancel() }
    }

    @Test fun `reversed multi package batches release each item without deadlock`() = workers(2) { pool ->
        val versions = ConcurrentHashMap<String, Long>()
        val firstItems = CountDownLatch(2)
        val active = ConcurrentHashMap<String, AtomicInteger>()
        fun job(names: List<String>) = Job(names, versions).apply {
            installing = { request ->
                val name = requireNotNull(request.prepared.packageName)
                val count = active.computeIfAbsent(name) { AtomicInteger() }
                check(count.incrementAndGet() == 1) { "Concurrent writer for $name" }
                try {
                    if (engineCalls.get() == 1) { firstItems.countDown(); waitFor(firstItems) }
                } finally { count.decrementAndGet() }
            }
        }
        val first = job(listOf("example.batch.a", "example.batch.b"))
        val second = job(listOf("example.batch.b", "example.batch.a"))
        try {
            first.session.start(pool)
            second.session.start(pool)
            waitFor(first.done)
            waitFor(second.done)
            for (item in listOf(first, second)) {
                assertNull(item.failure)
                assertTrue(requireNotNull(item.result).getAsJsonArray("results").all { it.asJsonObject["ok"].asBoolean })
                assertEquals(2, item.discards.get())
            }
            assertEquals(mapOf("example.batch.a" to 2L, "example.batch.b" to 2L), versions)
        } finally { firstItems.countDown(); firstItems.countDown(); first.session.cancel(); second.session.cancel() }
    }

    @Test fun `a plugin confirmation holds no package lock and versions refresh after confirmation`() = workers(2) { pool ->
        val versions = ConcurrentHashMap<String, Long>()
        val confirming = CountDownLatch(1)
        val confirm = CountDownLatch(1)
        val dialog = Job(listOf("example.confirm"), versions, interaction = "dialog").apply {
            confirmation = { confirming.countDown(); waitFor(confirm) }
        }
        val automatic = Job(listOf("example.confirm"), versions)
        try {
            dialog.session.start(pool)
            waitFor(confirming)
            automatic.session.start(pool)
            waitFor(automatic.done)
            assertNull(automatic.failure)
            confirm.countDown()
            waitFor(dialog.done)
            assertEquals(1L, requireNotNull(dialog.result)["previousVersionCode"].asLong)
            assertEquals(2L, requireNotNull(dialog.result)["versionCode"].asLong)
        } finally { confirm.countDown(); dialog.session.cancel(); automatic.session.cancel() }
    }

    private fun workers(size: Int = 3, test: (java.util.concurrent.ExecutorService) -> Unit) {
        val pool = Executors.newFixedThreadPool(size)
        try { test(pool) } finally {
            pool.shutdownNow()
            assertTrue("Workers did not exit", pool.awaitTermination(5, TimeUnit.SECONDS))
        }
    }

    private class Job(
        private val names: List<String>,
        private val versions: ConcurrentHashMap<String, Long>,
        private val authorizer: Authorizer = Authorizer.ROOT,
        private val user: Int = 0,
        clock: () -> Long = { TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) },
        timeout: Long = 10_000,
        interaction: String = "auto",
    ) : InstallSession.Environment, InstallSession.Listener {
        val prepared = CountDownLatch(1)
        val entered = CountDownLatch(1)
        val versionRead = CountDownLatch(1)
        val done = CountDownLatch(1)
        val engineCalls = AtomicInteger()
        val discards = AtomicInteger()
        var submittedUser = -1
        var result: JsonObject? = null
        var failure: InstallFailure? = null
        var installing: (InstallEngine.Request) -> Unit = {}
        var beforeVersion: () -> Unit = {}
        var confirmation: () -> Unit = {}
        val session = InstallSession(InstallRequest("concurrency", names.mapIndexed { index, _ -> SourceEntry(index, index, "$index.apk", 1) },
            interaction, InstallOptions(authorizer = authorizer.id, user = user.toString(), timeoutMillis = timeout)), this, this, clock)
        private val engine = object : InstallEngine {
            override fun install(request: InstallEngine.Request, listener: InstallEngine.Listener, checkCancelled: () -> Unit): InstallEngine.Result {
                checkCancelled()
                submittedUser = request.userId
                engineCalls.incrementAndGet()
                entered.countDown()
                installing(request)
                checkCancelled()
                val name = requireNotNull(request.prepared.packageName)
                versions.compute(name) { _, value -> (value ?: 0L) + 1L }
                return InstallEngine.Result(name, emptyList(), "silent")
            }
        }
        override fun resolve(request: InstallRequest, deadlineMillis: Long, checkActive: () -> Unit) = InstallSession.Target(authorizer, user, engine)
        override fun prepare(index: Int, sources: List<SourceEntry>, checkActive: () -> Unit): PreparedPackage {
            prepared.countDown()
            return PreparedPackage("apk", "$index.apk", 1, names[index], "1", 1, null, 24, 28,
                listOf(PlannedApk("base.apk", File("$index.apk"), 1, null)), emptyList(), null, emptyList(), emptyList(), true)
        }
        override fun installedVersion(packageName: String, target: InstallSession.Target): InstallSession.Version? {
            beforeVersion()
            versionRead.countDown()
            return versions[packageName]?.let { InstallSession.Version(it.toString(), it) }
        }
        override fun confirm(prepared: PreparedPackage, target: InstallSession.Target, deadlineMillis: Long, checkActive: () -> Unit) = confirmation()
        override fun onUserAction(intent: Intent) = Unit
        override fun discardItem(index: Int) { discards.incrementAndGet() }
        override fun close() = Unit
        override fun onCompleted(result: JsonObject) { this.result = result; done.countDown() }
        override fun onFailed(failure: InstallFailure) { this.failure = failure; done.countDown() }
    }

    companion object {
        private fun waitFor(latch: CountDownLatch) { check(latch.await(5, TimeUnit.SECONDS)) { "Timed out waiting for a test checkpoint" } }
    }
}
