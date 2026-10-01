package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.history.InstallHistoryCodec
import io.github.supermonster003.autojs6.plugin.three.setup.installer.history.InstallHistoryEntry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.history.InstallHistoryStore
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation
import org.autojs.plugin.installer.api.InstallerContract
import java.io.Closeable
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Owns one newly created external fixture token, never a package-wide or whole-history deletion. */
internal class FixtureHistoryOwnership(private val context: Context) : Closeable {
    private val baseline: Map<String, InstallHistoryEntry>
    private val history: InstallHistoryStore
    private val startedAt = System.currentTimeMillis()
    private var record: InstallPresentation.Record? = null

    init {
        // Read before loading the production store, whose restart recovery cancels unfinished entries.
        val disk = settledDisk()
        check(disk.all { it.terminal }) { "Unfinished history exists; refusing an external fixture audit" }
        check(disk.size < InstallHistoryEntry.MAX_ENTRIES) { "The external fixture would evict an existing history entry" }
        baseline = disk.associateBy { it.id }
        history = InstallHistoryStore.get(context)
        await("History did not load") { history.loaded }
        check(history.list().associateBy { it.id } == baseline) { "History changed while the fixture audit was starting" }
    }

    fun track(value: InstallPresentation.Record) {
        check(record == null && value.request.origin == InstallerContract.SOURCE_EXTERNAL && value.request.items.size == 1)
        check(baseline.values.none { it.token == value.token }) { "The external fixture token predates this audit" }
        check(value.request.sources.single().displayName == "fixture.apk") { "The selected record is not the fixed fixture source" }
        record = value
    }

    override fun close() {
        val owned = record
        if (owned != null) {
            owned.close()
            await("The owned external fixture did not settle before history cleanup") { owned.snapshot().terminal }
            val ids = owned.request.items.indices.map { InstallHistoryEntry.id(owned.token, it) }.toSet()
            val before = history.list()
            check(before.filterNot { it.id in ids }.associateBy { it.id } == baseline) {
                "Unrelated history changed; no fixture history was deleted"
            }
            before.filter { it.id in ids }.forEach { entry ->
                InstallHistoryEntry.validate(entry)
                check(entry.token == owned.token && entry.origin == InstallerContract.SOURCE_EXTERNAL &&
                    entry.startedAt >= startedAt && entry.packageName in setOf(null, FixtureInstallUi.PACKAGE_NAME)) {
                    "The selected history entry does not belong to the fixed external fixture"
                }
            }
            // Retire exact IDs even before a queued terminal save reaches disk.
            val completed = CountDownLatch(ids.size)
            val successful = AtomicBoolean(true)
            ids.forEach { id -> history.remove(id) { written ->
                if (!written) successful.set(false)
                completed.countDown()
            } }
            check(completed.await(5, TimeUnit.SECONDS) && successful.get()) { "Fixture history cleanup was not saved durably" }
            check(history.list().associateBy { it.id } == baseline) { "History cache did not return to its original entries" }
            check(settledDisk().associateBy { it.id } == baseline) { "Durable history did not return to its original entries" }
            InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
                putString("fixture-history", "token=${owned.token} removedIds=${ids.size} otherHistoryUnchanged=true durable=true")
            })
        } else {
            check(history.list().associateBy { it.id } == baseline && settledDisk().associateBy { it.id } == baseline) {
                "History changed without an owned fixture token; no entry was deleted"
            }
        }
    }

    private fun settledDisk(): List<InstallHistoryEntry> {
        var result: List<InstallHistoryEntry>? = null
        await("History persistence did not settle") { result = persisted(); result != null }
        return requireNotNull(result)
    }

    private fun persisted(): List<InstallHistoryEntry>? {
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

    private fun await(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 5_000
        while (!condition() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(25)
        check(condition()) { message }
    }
}
