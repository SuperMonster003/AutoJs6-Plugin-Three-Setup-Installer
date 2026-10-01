package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.KeyguardManager
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallRequest
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.SourceEntry
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.history.InstallHistoryEntry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.history.InstallHistoryFile
import io.github.supermonster003.autojs6.plugin.three.setup.installer.history.InstallHistoryStore
import io.github.supermonster003.autojs6.plugin.three.setup.installer.queue.InstallQueue
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.InstallerPreferences
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.ExternalSources
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.Closeable
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Exercises the real local batch controller with unreadable sources only; no package is installed. */
@RunWith(AndroidJUnit4::class)
class HistoryQueueDeviceTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @Test fun localQueueContinuesUnreadableSourcesAndRecordsOneHistoryRowPerItem() {
        ready()
        val sources = ExternalSources.fromUris(List(3) { missingUri() }, 0)
        var token: String? = null
        val observed = AtomicInteger()
        val mainOnly = AtomicBoolean(true)
        val observer = InstallQueue.observe {
            observed.incrementAndGet()
            if (Looper.myLooper() != Looper.getMainLooper()) mainOnly.set(false)
        }
        try {
            token = InstallQueue.start(context, sources, InstallerContract.SOURCE_HOME,
                InstallOptions(authorizer = InstallerContract.AUTHORIZER_NONE, continueOnError = true, timeoutMillis = 15_000))
            val record = requireNotNull(InstallPresentation.find(token))
            waitUntil { record.snapshot().terminal }
            val state = record.snapshot()
            assertEquals(3, state.items.size)
            assertTrue(state.items.all { it.result?.get("ok")?.asBoolean == false })
            assertTrue(state.items.all { it.result?.getAsJsonObject("error")?.get("code")?.asString == InstallerErrorCodes.SOURCE_UNREADABLE })
            assertFalse(InstallQueue.snapshots().any { it.token == token })
            assertTrue(InstallQueue.snapshots(includeTerminal = true).any { it.token == token })
            val history = history(token, 3)
            assertTrue(history.all { it.origin == InstallerContract.SOURCE_HOME && it.result == InstallerContract.STAGE_FAILED })
            waitUntil { observed.get() > 0 }
            assertTrue(mainOnly.get())
        } finally {
            observer.close()
            token?.let { cleanup(it, 3) }
        }
    }

    @Test fun cancellationStopsTheOpenSourceAndMarksAllRemainingItemsCancelled() {
        ready()
        val directory = File(context.cacheDir, "p2-source-fixtures-${UUID.randomUUID()}").apply { check(mkdir()) }
        val fixture = File(directory, "fixture.apk").apply { writeText("This source is cancelled before inspection") }
        val blocked = Uri.parse("content://${context.packageName}.source-fixtures/${directory.name}/fixture.apk?waitForCancel=query")
        val sources = ExternalSources.fromUris(listOf(blocked, missingUri(), missingUri()), 0)
        var token: String? = null
        try {
            token = InstallQueue.start(context, sources, InstallerContract.SOURCE_EXTERNAL,
                InstallOptions(authorizer = InstallerContract.AUTHORIZER_NONE, continueOnError = true, timeoutMillis = 15_000))
            val record = requireNotNull(InstallPresentation.find(token))
            waitUntil {
                context.contentResolver.call(blocked, "fixtureCancellationState", blocked.toString(), null)?.getBoolean("waiting") == true
            }
            val active = InstallQueue.snapshots().single { it.token == token }
            assertEquals(InstallerContract.SOURCE_EXTERNAL, active.origin)
            assertEquals(0, active.state.index)
            assertEquals(3, active.state.items.size)
            assertTrue(InstallQueue.cancel(token))
            waitUntil { record.snapshot().terminal }
            val history = history(token, 3)
            assertTrue(history.all { it.result == InstallerContract.STAGE_CANCELLED && it.errorCode == InstallerErrorCodes.CANCELLED })
            assertFalse(InstallQueue.cancel(token))
        } finally {
            token?.let { cleanup(it, 3) }
            fixture.delete()
            directory.delete()
        }
    }

    @Test fun shareSourcesIgnoreCallerOriginAndOptionsWhileRetainingTheValidatedGrantSet() {
        val uri = missingUri()
        val input = Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(InstallerContract.FIELD_SOURCE_ORIGIN, InstallerContract.SOURCE_HOST)
            .putExtra(InstallerContract.FIELD_OPTIONS, "{\"authorizer\":\"root\",\"deleteSource\":true}")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        val sources = ExternalSources.fromIntent(input)
        assertEquals(listOf(uri), sources.uris)
        assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, sources.grantIntent.flags)
        assertEquals(uri, sources.grantIntent.clipData!!.getItemAt(0).uri)
        assertNull(sources.grantIntent.extras)
        assertNull(sources.grantIntent.component)
        assertNull(sources.grantIntent.action)
    }

    @Test fun closingTheDisplayCannotDiscardALateConfirmedWorkerOutcome() {
        val request = InstallRequest("fixture", listOf(SourceEntry(0, 0, "fixture.apk", 1)),
            InstallerContract.INTERACTION_SILENT, InstallOptions(), origin = InstallerContract.SOURCE_HOST)
        val record = InstallPresentation.create(context, request, InstallPresentation.Callbacks(cancel = {}))
        try {
            record.close()
            assertNull(InstallPresentation.find(record.token))
            val confirmed = JsonObject().apply {
                addProperty("ok", true); addProperty("packageName", "example.history.fixture"); addProperty("authorizer", "none")
            }
            record.onItemResult(0, confirmed)
            record.onCompleted(confirmed)
            val saved = history(record.token, 1).single()
            assertEquals(InstallerContract.STAGE_COMPLETED, saved.result)
            assertEquals("example.history.fixture", saved.packageName)
            assertNull(saved.errorCode)
        } finally { cleanup(record.token, 1) }
    }

    @Test fun privateAndroidHistoryKeepsNewestTwoHundredAndClearSurvivesReloadAndLateWrites() {
        val cache = context.cacheDir.canonicalFile
        val directory = File(cache, "p5-history-fs-${UUID.randomUUID()}").canonicalFile
        assertEquals(cache, directory.parentFile)
        val io = Executors.newSingleThreadExecutor { Thread(it, "history-filesystem-device-test") }
        val main = Handler(Looper.getMainLooper())
        val callbacks = Executor { command -> check(main.post(command)) }
        val callbacksOnMain = AtomicBoolean(true)
        val observed = CountDownLatch(1)
        var observer: Closeable? = null
        try {
            assertTrue(io.submit<Boolean> { directory.mkdir() }.get(10, TimeUnit.SECONDS))
            val target = File(directory, "history.json")
            val disk = InstallHistoryFile(target)
            val store = InstallHistoryStore(disk, io, callbacks)
            observer = store.observe {
                if (Looper.myLooper() != Looper.getMainLooper()) callbacksOnMain.set(false)
                observed.countDown()
            }
            val all = mutableListOf<InstallHistoryEntry>()
            val tickets = mutableListOf<InstallHistoryStore.Ticket>()
            val started = System.currentTimeMillis() - 1_000
            repeat(7) { group ->
                val token = UUID.randomUUID().toString()
                val batch = List(InstallerContract.MAX_BATCH_SOURCES) { index ->
                    val ordinal = group * InstallerContract.MAX_BATCH_SOURCES + index
                    val time = started + ordinal
                    InstallHistoryEntry(InstallHistoryEntry.id(token, index), token, index,
                        "Filesystem fixture $ordinal", "example.history.fixture", "2", 2, "1", 1,
                        InstallerContract.STAGE_COMPLETED, time, time, time,
                        InstallerContract.SOURCE_HOME, InstallerContract.AUTHORIZER_NONE)
                }
                all += batch
                tickets += store.begin(token).also { it.save(batch, revision = 0) }
            }
            assertEquals(224, all.size)
            // The read is queued after all seven writes; the bounded Future also acts as a barrier.
            val persisted = io.submit<List<InstallHistoryEntry>> { disk.read() }.get(15, TimeUnit.SECONDS)
            val expected = all.takeLast(InstallHistoryEntry.MAX_ENTRIES).reversed()
            assertEquals(200, persisted.size)
            assertEquals(expected, persisted)
            assertEquals(expected, store.list())
            assertTrue(observed.await(10, TimeUnit.SECONDS))
            val reloaded = InstallHistoryStore(InstallHistoryFile(target), io, callbacks)
            assertEquals(expected, io.submit<List<InstallHistoryEntry>> { reloaded.list() }.get(10, TimeUnit.SECONDS))
            assertTrue(io.submit<Boolean> {
                target.isFile && !File(target.path + ".new").exists() && !File(target.path + ".bak").exists()
            }.get(10, TimeUnit.SECONDS))

            val cleared = CountDownLatch(1)
            val clearSaved = AtomicBoolean()
            store.clear { saved ->
                if (Looper.myLooper() != Looper.getMainLooper()) callbacksOnMain.set(false)
                clearSaved.set(saved)
                cleared.countDown()
            }
            assertTrue("Private history clear callback timed out", cleared.await(10, TimeUnit.SECONDS))
            assertTrue(clearSaved.get())
            assertTrue(store.list().isEmpty())
            // This ticket was live before clear and its rows were inside the retained 200.
            tickets.last().save(all.takeLast(InstallerContract.MAX_BATCH_SOURCES), revision = 1)
            assertTrue(io.submit<List<InstallHistoryEntry>> { disk.read() }.get(10, TimeUnit.SECONDS).isEmpty())
            assertTrue(store.list().isEmpty())
            val afterClear = InstallHistoryStore(InstallHistoryFile(target), io, callbacks)
            assertTrue(io.submit<List<InstallHistoryEntry>> { afterClear.list() }.get(10, TimeUnit.SECONDS).isEmpty())
            assertTrue(callbacksOnMain.get())
        } finally {
            observer?.close()
            io.shutdownNow()
            assertTrue("Private history IO worker did not stop", io.awaitTermination(10, TimeUnit.SECONDS))
            // This directory is uniquely owned by this test, never the process-wide history store.
            assertEquals(cache, directory.canonicalFile.parentFile)
            assertTrue(directory.name.startsWith("p5-history-fs-"))
            if (directory.exists()) assertTrue(directory.deleteRecursively())
        }
    }

    private fun ready() {
        assumeFalse("Unlock the device before queue UI tests", context.getSystemService(KeyguardManager::class.java).isKeyguardLocked)
        assumeTrue("The none authorizer must be enabled for source-only tests", Authorizer.NONE in InstallerPreferences.read(context).authorizers.enabled)
    }

    private fun missingUri(): Uri = Uri.parse("content://${context.packageName}.source-fixtures/p2-source-fixtures-${UUID.randomUUID()}/fixture.apk")
    private fun history(token: String, count: Int): List<InstallHistoryEntry> {
        val store = InstallHistoryStore.get(context)
        waitUntil { store.list().count { it.token == token && it.terminal } == count }
        return store.list().filter { it.token == token }
    }

    private fun cleanup(token: String, count: Int) {
        InstallPresentation.find(token)?.close()
        val store = InstallHistoryStore.get(context)
        val done = CountDownLatch(count)
        repeat(count) { index -> store.remove(InstallHistoryEntry.id(token, index)) { done.countDown() } }
        assertTrue("Fixture history cleanup timed out", done.await(10, TimeUnit.SECONDS))
        instrumentation.waitForIdleSync()
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return
            SystemClock.sleep(25)
        }
        fail("The installation queue did not reach the expected state")
    }
}
