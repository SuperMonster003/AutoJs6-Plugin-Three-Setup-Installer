package io.github.supermonster003.autojs6.plugin.three.setup.installer.source

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class StagingDirectoriesTest {
    @get:Rule val temporary = TemporaryFolder()
    private val now = 1_800_000_000_000L

    @Test fun `first standalone source open deletes only directories older than 24 hours`() {
        val root = temporary.newFolder("staging")
        val old = child(root, "old", now - StagingDirectories.STALE_AFTER_MILLIS - 1)
        val boundary = child(root, "boundary", now - StagingDirectories.STALE_AFTER_MILLIS)
        val recent = child(root, "recent", now - 1)
        val future = child(root, "future", now + StagingDirectories.STALE_AFTER_MILLIS)
        val other = File(root, "unowned-file").apply { writeText("preserve"); check(setLastModified(1)) }
        val directories = StagingDirectories { now }
        val fresh = directories.create(root, "../../untrusted-id")
        try {
            assertFalse(old.exists())
            assertTrue(listOf(boundary, recent, future, other, fresh).all { it.exists() })
            assertEquals(root.canonicalFile, fresh.parentFile)
            assertFalse(fresh.name.contains('/'))
        } finally { directories.discard(fresh) }
    }

    @Test fun `later cleanup preserves live directories even after wall clock advances`() {
        var clock = now
        val root = temporary.newFolder("staging")
        val directories = StagingDirectories { clock }
        val active = directories.create(root, "live")
        val payload = File(active, "source.apk").apply { writeText("owned bytes") }
        check(active.setLastModified(now))
        val stale = child(root, "crashed-process", now)
        clock += StagingDirectories.STALE_AFTER_MILLIS + 1
        directories.cleanStale(root)
        assertTrue(payload.isFile)
        assertFalse(stale.exists())
        directories.discard(active)
        directories.discard(active)
        assertFalse(active.exists())
    }

    @Test fun `same untrusted ID and concurrent cleanup never share or delete live directories`() {
        val root = temporary.newFolder("staging")
        val directories = StagingDirectories { now }
        val workers = Executors.newFixedThreadPool(4)
        val start = CountDownLatch(1)
        try {
            val jobs = (0 until 12).map {
                workers.submit<File> {
                    start.await()
                    directories.create(root, "duplicate").also { directory ->
                        File(directory, "live.apk").writeText("source")
                        check(directory.setLastModified(1))
                        directories.cleanStale(root)
                    }
                }
            }
            start.countDown()
            val owned = jobs.map { it.get(5, TimeUnit.SECONDS) }
            assertEquals(12, owned.toSet().size)
            assertTrue(owned.all { File(it, "live.apk").isFile })
            owned.forEach(directories::discard)
            assertTrue(root.listFiles().orEmpty().isEmpty())
        } finally { start.countDown(); workers.shutdownNow() }
    }

    @Test fun `discard does not accept a directory merely because its parent is named staging`() {
        val root = temporary.newFolder("staging")
        val unowned = child(root, "foreign", now)
        StagingDirectories { now }.discard(unowned)
        assertEquals("source", File(unowned, "source.apk").readText())
    }

    private fun child(root: File, name: String, modified: Long): File = File(root, name).apply {
        check(mkdir())
        File(this, "source.apk").writeText("source")
        check(setLastModified(modified))
    }
}
