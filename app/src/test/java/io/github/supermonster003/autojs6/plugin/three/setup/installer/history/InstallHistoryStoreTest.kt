package io.github.supermonster003.autojs6.plugin.three.setup.installer.history

import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class InstallHistoryStoreTest {
    @get:Rule val temporary = TemporaryFolder()
    private val direct = Executor { it.run() }
    private var now = 1_800_000_000_000L
    private fun entry(token: String = UUID.randomUUID().toString(), index: Int = 0, terminal: Boolean = true,
        origin: String = InstallerContract.SOURCE_HOME) = InstallHistoryEntry(
        InstallHistoryEntry.id(token, index), token, index, "Example", "example.package", "2.0", 2, "1.0", 1,
        if (terminal) InstallerContract.STAGE_COMPLETED else InstallerContract.STAGE_PENDING,
        now, now + 20, if (terminal) now + 20 else null, origin, InstallerContract.AUTHORIZER_ROOT)
    private fun file() = File(temporary.newFolder(), "history.json")

    @Test fun codecPreservesAllOriginsMetadataTimesAndBoundedDiagnostics() {
        val entries = InstallHistoryEntry.ORIGINS.map { origin -> entry(origin = origin).copy(
            result = InstallerContract.STAGE_FAILED, errorCode = InstallerErrorCodes.INSTALL_FAILED,
            systemMessage = "INSTALL_FAILED_VERSION_DOWNGRADE: Version 1 is older than 2", durationMillis = 8) }
        assertEquals(entries, InstallHistoryCodec.decode(InstallHistoryCodec.encode(entries)))
        val text = InstallHistoryCodec.encode(entries).toString(Charsets.UTF_8)
        assertFalse(text.contains("sources"))
        assertFalse(text.contains("intent"))
    }

    @Test fun codecRejectsUnknownFieldsWrongTypesDuplicateItemsAndTrailingDocuments() {
        val value = InstallHistoryCodec.encode(listOf(entry())).toString(Charsets.UTF_8)
        for (json in listOf(value.replace("\"format\":1", "\"format\":2"), value.replace("\"format\":1", "\"format\":true"),
            value.replace("\"format\":1", "\"format\":1,\"request\":{}"), value.replace("\"itemIndex\":0", "\"itemIndex\":32"),
            value + "{}", "{}", "null")) {
            assertThrows(Exception::class.java) { InstallHistoryCodec.decode(json.toByteArray()) }
        }
        val item = entry()
        assertThrows(IllegalArgumentException::class.java) { InstallHistoryCodec.encode(listOf(item, item)) }
        assertThrows(Exception::class.java) { InstallHistoryCodec.decode(byteArrayOf(0xc3.toByte(), 0x28)) }
    }

    @Test fun metadataAndErrorTextDoNotRetainPrivateSourceUrisOrPaths() {
        val message = "INSTALL_FAILED_INVALID_APK: content://private.provider/package /storage/emulated/0/private.apk"
        val safe = InstallHistoryEntry.text(message, 1024)!!
        assertTrue(safe.contains("INSTALL_FAILED_INVALID_APK"))
        assertFalse(safe.contains("content://"))
        assertFalse(safe.contains("/storage/"))
        assertEquals(1024, InstallHistoryEntry.text("x".repeat(2048), 1024)!!.length)
    }

    @Test fun unicodePathsAreRedactedWithoutRemovingTheSurroundingDiagnostic() {
        for (path in listOf("/sdcard/私人/fixture.apk", "/私人/安装包.apk", "/sdcard/私人 文件/应用.apk",
            "/данные/файл.apk", "/sdcard/📦/应用.apk", "C:\\用户\\私人 文件\\应用.apk")) {
            val safe = InstallHistoryEntry.text("Cannot open '$path': INSTALL_FAILED_INVALID_APK", 1024)!!
            assertEquals("Cannot open '...': INSTALL_FAILED_INVALID_APK", safe)
            assertEquals(safe, InstallHistoryEntry.text(safe, 1024))
            val saved = entry().copy(result = InstallerContract.STAGE_FAILED,
                errorCode = InstallerErrorCodes.INVALID_PACKAGE, systemMessage = safe)
            assertEquals(listOf(saved), InstallHistoryCodec.decode(InstallHistoryCodec.encode(listOf(saved))))
        }
    }

    @Test fun restartMarksOnlyUnfinishedItemsCancelledAndPreservesConfirmedResults() {
        val target = file()
        val token = UUID.randomUUID().toString()
        val confirmed = entry(token, 0)
        val pending = entry(token, 1, terminal = false)
        InstallHistoryFile(target).write(listOf(confirmed, pending))
        now += 500
        val store = InstallHistoryStore(InstallHistoryFile(target), direct, clock = { now })
        val byIndex = store.list().associateBy { it.itemIndex }
        assertEquals(confirmed, byIndex[0])
        assertEquals(InstallerContract.STAGE_CANCELLED, byIndex[1]!!.result)
        assertEquals(InstallerErrorCodes.CANCELLED, byIndex[1]!!.errorCode)
        assertTrue(byIndex[1]!!.interrupted)
        assertEquals(store.list(), InstallHistoryFile(target).read())
    }

    @Test fun historyIsBoundedToTheNewestTwoHundredAcrossRestarts() {
        val target = file()
        val store = InstallHistoryStore(InstallHistoryFile(target), direct, clock = { now })
        val values = (0..200).map {
            now++
            entry().also { value -> assertTrue(store.begin(value.token).save(listOf(value), 0, finish = true).await()) }
        }
        assertEquals(200, store.list().size)
        assertFalse(store.list().any { it.id == values.first().id })
        assertEquals(values.last().id, store.list().first().id)
        assertEquals(store.list(), InstallHistoryStore(InstallHistoryFile(target), direct, clock = { now }).list())
    }

    @Test fun loadingAndWritingAreQueuedAndSnapshotsNeverPerformIo() {
        val target = file()
        InstallHistoryFile(target).write(listOf(entry()))
        val queued = Queue()
        val store = InstallHistoryStore(InstallHistoryFile(target), queued, clock = { now })
        var changes = 0
        val subscription = store.observe { changes++ }
        assertFalse(store.loaded)
        assertTrue(store.list().isEmpty())
        assertEquals(0, changes)
        queued.drain()
        assertTrue(store.loaded)
        assertEquals(1, store.list().size)
        assertEquals(1, changes)
        subscription.close()
        store.clear()
        queued.drain()
        assertEquals(1, changes)
    }

    @Test fun removingAnItemRetiresQueuedAndFutureWritesFromTheSameSession() {
        val queued = Queue()
        val target = file()
        val store = InstallHistoryStore(InstallHistoryFile(target), queued, clock = { now })
        val first = entry(terminal = false)
        val second = entry(first.token, index = 1, terminal = false)
        val ticket = store.begin(first.token)
        ticket.save(listOf(first, second), 0)
        store.remove(first.id)
        queued.drain()
        assertEquals(listOf(second.id), store.list().map { it.id })
        ticket.save(listOf(first.copy(result = "completed", finishedAt = first.updatedAt), second), 1)
        queued.drain()
        assertEquals(listOf(second.id), InstallHistoryFile(target).read().map { it.id })
    }

    @Test fun clearInvalidatesOldTicketsButAllowsNewSessionsAfterTheClear() {
        val queued = Queue()
        val target = file()
        val store = InstallHistoryStore(InstallHistoryFile(target), queued, clock = { now })
        val old = entry(terminal = false)
        val ticket = store.begin(old.token)
        ticket.save(listOf(old), 0)
        store.clear()
        assertFalse(ticket.save(listOf(old), 1).await())
        val current = entry()
        store.begin(current.token).save(listOf(current), 0, finish = true)
        queued.drain()
        assertEquals(listOf(current), InstallHistoryFile(target).read())
    }

    @Test fun deletionDuringAWriteCannotResurrectItsSnapshot() {
        val queued = Queue()
        val target = file()
        val value = entry()
        lateinit var store: InstallHistoryStore
        var deleted = false
        val file = InstallHistoryFile(target) {
            if (!deleted) { deleted = true; store.remove(value.id) }
        }
        store = InstallHistoryStore(file, queued, clock = { now })
        store.begin(value.token).save(listOf(value), 0)
        queued.drain()
        assertTrue(store.list().isEmpty())
        assertTrue(InstallHistoryFile(target).read().isEmpty())
    }

    @Test fun clearRacingAnInFlightWriteIsDurableAndLateCompletionCannotRecreateRows() {
        val io = Executors.newSingleThreadExecutor()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val cleared = CountDownLatch(1)
        var firstWrite = true
        val target = file()
        val disk = InstallHistoryFile(target) {
            if (firstWrite) {
                firstWrite = false
                entered.countDown()
                check(release.await(5, TimeUnit.SECONDS))
            }
        }
        try {
            val store = InstallHistoryStore(disk, io, clock = { now })
            val value = entry(terminal = false)
            val ticket = store.begin(value.token)
            val writing = ticket.save(listOf(value), 0)
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            store.clear { success -> assertTrue(success); cleared.countDown() }
            release.countDown()
            assertFalse(writing.await())
            assertTrue(cleared.await(5, TimeUnit.SECONDS))
            assertFalse(ticket.save(listOf(value.copy(result = "completed", finishedAt = value.updatedAt)), 1).await())
            assertTrue(InstallHistoryFile(target).read().isEmpty())
        } finally { release.countDown(); io.shutdownNow() }
    }

    @Test fun atomicFailureKeepsThePreviousFileAndDoesNotChangeTheTrueOutcome() {
        val target = file()
        val first = entry()
        InstallHistoryFile(target).write(listOf(first))
        val store = InstallHistoryStore(InstallHistoryFile(target) { throw IOException("Injected disk failure") }, direct, clock = { now })
        val next = entry()
        assertFalse(store.begin(next.token).save(listOf(next), 0, finish = true).await())
        assertEquals(InstallerContract.STAGE_COMPLETED, store.list().first { it.id == next.id }.result)
        assertEquals(listOf(first), InstallHistoryFile(target).read())
    }

    @Test fun interruptedAtomicReplacementRestoresTheBackup() {
        val target = file()
        val first = entry()
        InstallHistoryFile(target).write(listOf(first))
        assertTrue(target.renameTo(File(target.path + ".bak")))
        target.writeText("{truncated")
        File(target.path + ".new").writeText("unfinished")
        assertEquals(listOf(first), InstallHistoryFile(target).read())
        assertFalse(File(target.path + ".new").exists())
        assertFalse(File(target.path + ".bak").exists())
    }

    private class Queue : Executor {
        private val pending = ArrayDeque<Runnable>()
        override fun execute(command: Runnable) { pending.addLast(command) }
        fun drain() { while (pending.isNotEmpty()) pending.removeFirst().run() }
    }
}
