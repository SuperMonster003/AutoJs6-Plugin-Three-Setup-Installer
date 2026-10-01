package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
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

class InstallRecoveryTest {
    @get:Rule val temporary = TemporaryFolder()
    private var now = 1_800_000_000_000L
    private fun snapshot(token: String = UUID.randomUUID().toString(), revision: Long = 1, terminal: Boolean = false) =
        InstallRecoverySnapshot(token, revision, now, now + InstallRecoverySnapshot.RETENTION_MILLIS, terminal,
            if (terminal) InstallerContract.STAGE_COMPLETED else InstallerContract.STAGE_COMMITTING, 1, listOf(
                InstallRecoverySnapshot.Item("Confirmed fixture", "example.confirmed", "2.0", 2, 1, "current", false, true, false, null),
                InstallRecoverySnapshot.Item("Pending fixture", "example.pending", "1.0", 1, null, "10", false, null, null, null)))

    @Test fun codecPreservesConfirmedItemsButPendingDisplayBecomesCancelled() {
        val original = snapshot()
        val decoded = InstallRecoveryCodec.decode(InstallRecoveryCodec.encode(original))
        assertEquals(original, decoded)
        val shown = decoded.display()
        assertTrue(shown.recovered && shown.interrupted && shown.terminal)
        assertFalse(shown.canRetry)
        assertNull(shown.prompt)
        assertEquals(listOf(InstallerContract.STAGE_COMPLETED, InstallerContract.STAGE_CANCELLED), shown.items.map { it.stage })
        assertTrue(shown.items.first().result!![InstallerContract.FIELD_OK].asBoolean)
        assertFalse(shown.items.last().result!![InstallerContract.FIELD_OK].asBoolean)
    }

    @Test fun capturePersistsOnlyWhitelistedErrorIdentifiersAndDisplayFacts() {
        val result = JsonObject().apply {
            addProperty(InstallerContract.FIELD_OK, false)
            add("error", JsonObject().apply {
                addProperty("code", InstallerErrorCodes.INVALID_PACKAGE)
                addProperty("systemMessage", "content://private.provider/source INSTALL_FAILED_INVALID_APK /storage/emulated/0/private.apk")
            })
        }
        val state = InstallPresentation.Snapshot(1, InstallerContract.STAGE_FAILED, 0, 0f,
            listOf(InstallPresentation.Item("content://private.provider/source", result = result)), null, true, null, false, false)
        val encoded = InstallRecoveryCodec.encode(InstallRecoverySnapshot.capture(UUID.randomUUID().toString(), state,
            InstallOptions(), now, InstallRecoverySnapshot.RETENTION_MILLIS)).toString(Charsets.UTF_8)
        assertFalse(encoded.contains("content://"))
        assertFalse(encoded.contains("/storage/"))
        assertTrue(encoded.contains("INSTALL_FAILED_INVALID_APK"))
        assertFalse(encoded.contains("systemMessage"))
    }

    @Test fun codecRejectsUnknownSchemaFieldsTypesTokensAndTrailingData() {
        val encoded = InstallRecoveryCodec.encode(snapshot()).toString(Charsets.UTF_8)
        val bad = listOf(encoded.replace("\"format\":1", "\"format\":2"),
            encoded.replace("\"format\":1", "\"format\":true"),
            encoded.replace("\"format\":1", "\"format\":1,\"intent\":{}"),
            encoded.replace("\"index\":1", "\"index\":33"), encoded + "{}", "{", "null")
        bad.forEach { text -> assertThrows(Exception::class.java) { InstallRecoveryCodec.decode(text.toByteArray()) } }
        for (token in listOf("../outside", "", "A".repeat(36), UUID.randomUUID().toString().uppercase())) {
            assertThrows(IllegalArgumentException::class.java) { InstallRecoveryCodec.encode(snapshot(token)) }
        }
        assertThrows(Exception::class.java) { InstallRecoveryCodec.decode(byteArrayOf(0xc3.toByte(), 0x28)) }
        assertThrows(Exception::class.java) { InstallRecoveryCodec.decode(ByteArray(InstallRecoverySnapshot.MAX_BYTES + 1)) }
    }

    @Test fun maximumBatchOfBoundedFieldsFitsTheFileCeiling() {
        val item = snapshot().items.first().copy(label = "界".repeat(96), packageName = "a." + "x".repeat(253),
            versionName = "界".repeat(64), ok = false, failure = InstallRecoverySnapshot.Failure(InstallerErrorCodes.INVALID_PACKAGE,
                Int.MAX_VALUE, "INSTALL_FAILED_" + "X".repeat(72)))
        val value = snapshot().copy(items = List(InstallerContract.MAX_BATCH_SOURCES) { item })
        assertTrue(InstallRecoveryCodec.encode(value).size <= InstallRecoverySnapshot.MAX_BYTES)
        assertThrows(IllegalArgumentException::class.java) { InstallRecoveryCodec.encode(value.copy(items = value.items + item)) }
    }

    @Test fun atomicWriteFailureKeepsTheLastCompleteResult() {
        val directory = temporary.newFolder()
        var fail = false
        val store = InstallRecoveryStore(directory, { now }, beforeCommit = { if (fail) throw IOException("Injected failure") })
        val first = snapshot()
        assertTrue(store.write(first))
        fail = true
        assertThrows(IOException::class.java) { store.write(first.copy(revision = 2)) }
        assertEquals(first, InstallRecoveryStore(directory, { now }).read(first.token))
        assertFalse(File(directory, "${first.token}.json.new").exists())
    }

    @Test fun unfinishedAtomicReplacementRestoresTheBackupAndDropsPartialData() {
        val directory = temporary.newFolder()
        val value = snapshot()
        val store = InstallRecoveryStore(directory, { now })
        store.write(value)
        val file = File(directory, "${value.token}.json")
        assertTrue(file.renameTo(File(file.path + ".bak")))
        file.writeText("{truncated")
        File(file.path + ".new").writeText("unfinished")
        assertEquals(value, InstallRecoveryStore(directory, { now }).read(value.token))
        assertFalse(File(file.path + ".bak").exists())
        assertFalse(File(file.path + ".new").exists())
    }

    @Test fun corruptOversizedExpiredAndFutureFilesAreDiscarded() {
        val directory = temporary.newFolder()
        val store = InstallRecoveryStore(directory, { now })
        val value = snapshot()
        File(directory, "${value.token}.json").writeText("{")
        assertNull(store.read(value.token))
        File(directory, "${value.token}.json").writeBytes(ByteArray(InstallRecoverySnapshot.MAX_BYTES + 1))
        assertNull(store.read(value.token))
        store.write(value)
        now = value.expiresAt
        assertNull(store.read(value.token))
        now = value.savedAt
        store.write(value)
        now -= InstallRecoverySnapshot.FUTURE_SKEW_MILLIS + 1
        assertNull(InstallRecoveryStore(directory, { now }).read(value.token))
    }

    @Test fun wallClockExpirySurvivesAProcessOrBootWithoutAnElapsedRealtimeOrigin() {
        val directory = temporary.newFolder()
        val value = snapshot()
        InstallRecoveryStore(directory, { now }).write(value)
        now += 1_000
        // A fresh store has no previous elapsedRealtime state at all.
        assertEquals(value, InstallRecoveryStore(directory, { now }).read(value.token))
        now = value.expiresAt + 1
        assertNull(InstallRecoveryStore(directory, { now }).read(value.token))
    }

    @Test fun limitsEvictOldestSnapshotsAndNeverTouchOtherFiles() {
        val directory = temporary.newFolder()
        val other = File(directory, "unrelated.txt").apply { writeText("keep") }
        val store = InstallRecoveryStore(directory, { now }, maximum = 3)
        val values = (0..4).map { now++; snapshot().also { store.write(it) } }
        assertNull(store.read(values[0].token))
        assertNull(store.read(values[1].token))
        values.drop(2).forEach { assertNotNull(store.read(it.token)) }
        assertEquals(3, directory.listFiles()!!.count { it.extension == "json" })
        store.clear()
        assertEquals("keep", other.readText())
    }

    @Test fun coalescedAndOutOfOrderWritesKeepOnlyTheNewestRevision() {
        val store = InstallRecoveryStore(temporary.newFolder(), { now })
        val queue = Queue()
        val writer = InstallRecoveryWriter({ store }, queue)
        val value = snapshot()
        val ticket = writer.begin(value.token)
        ticket.save(value)
        val latest = ticket.save(value.copy(revision = 2), true)!!
        queue.runAll()
        assertTrue(latest.await())
        assertFalse(ticket.save(value, true)!!.await())
        assertEquals(2L, store.read(value.token)!!.revision)
    }

    @Test fun closeDuringWriteRejectsCommitAndAllLaterWrites() {
        val directory = temporary.newFolder()
        val queue = Queue()
        lateinit var writer: InstallRecoveryWriter
        val value = snapshot()
        var closed = false
        val store = InstallRecoveryStore(directory, { now }, beforeCommit = { writer.remove(value.token) { closed = true } })
        writer = InstallRecoveryWriter({ store }, queue)
        val ticket = writer.begin(value.token)
        val saved = ticket.save(value, true)!!
        queue.runAll()
        assertFalse(saved.await())
        assertTrue(closed)
        assertNull(InstallRecoveryStore(directory, { now }).read(value.token))
        assertFalse(ticket.save(value.copy(revision = 3), true)!!.await())
    }

    @Test fun aDurableWriteSupersededDuringIOWaitsForItsReplacement() {
        val queue = Queue()
        val value = snapshot()
        lateinit var ticket: InstallRecoveryWriter.Ticket
        var replace = true
        val store = InstallRecoveryStore(temporary.newFolder(), { now }, beforeCommit = {
            if (replace) { replace = false; ticket.save(value.copy(revision = 2)) }
        })
        val writer = InstallRecoveryWriter({ store }, queue)
        ticket = writer.begin(value.token)
        val saved = ticket.save(value, true)!!
        queue.runAll()
        assertTrue(saved.await())
        assertEquals(2L, store.read(value.token)!!.revision)
    }

    @Test fun clearRejectsOldTicketsButAllowsNewGenerationWrites() {
        val store = InstallRecoveryStore(temporary.newFolder(), { now })
        val queue = Queue()
        val writer = InstallRecoveryWriter({ store }, queue)
        val old = snapshot()
        val ticket = writer.begin(old.token)
        val queued = ticket.save(old, true)!!
        writer.clear()
        val fresh = snapshot()
        writer.begin(fresh.token).save(fresh)
        queue.runAll()
        assertFalse(queued.await())
        assertFalse(ticket.save(old.copy(revision = 4), true)!!.await())
        assertNull(store.read(old.token))
        assertNotNull(store.read(fresh.token))
    }

    @Test fun retiringAtCapacityAllowsAReplacementBeforeDeletionFinishes() {
        val store = InstallRecoveryStore(temporary.newFolder(), { now })
        val queue = Queue()
        val writer = InstallRecoveryWriter({ store }, queue)
        val tickets = List(InstallRecoverySnapshot.MAX_ENTRIES) { writer.begin(UUID.randomUUID().toString()) }
        assertThrows(IllegalArgumentException::class.java) { writer.begin(UUID.randomUUID().toString()) }
        tickets.first().close()
        val replacement = writer.begin(UUID.randomUUID().toString())
        replacement.save(snapshot(replacement.token))
        assertFalse(tickets.first().save(snapshot(tickets.first().token), true)!!.await())
        queue.runAll()
        assertNotNull(store.read(replacement.token))
    }

    private class Queue : Executor {
        private val tasks = java.util.ArrayDeque<Runnable>()
        override fun execute(command: Runnable) { tasks.add(command) }
        fun runAll() { while (tasks.isNotEmpty()) tasks.removeFirst().run() }
    }
}
