package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import org.autojs.plugin.installer.api.InstallerContract
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class InstallationNoticePublicationsTest {
    @Test fun removalWaitsForAnAlreadyPublishingResultAndThenCancelsIt() {
        val entries = registry()
        val publications = InstallationNoticePublications(entries)
        val notice = AtomicReference<String?>()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val removalStarted = CountDownLatch(1)
        val removalFinished = CountDownLatch(1)
        val error = AtomicReference<Throwable?>()
        val completed = AtomicBoolean()
        val publisher = worker(error) {
            completed.set(publications.complete("session") { entry ->
                entered.countDown()
                check(release.await(5, TimeUnit.SECONDS))
                notice.set("result:${entry.payload}")
            })
        }
        assertTrue(entered.await(5, TimeUnit.SECONDS))
        val remover = worker(error) {
            removalStarted.countDown()
            publications.remove("session") { notice.set(null) }
            removalFinished.countDown()
        }
        try {
            assertTrue(removalStarted.await(5, TimeUnit.SECONDS))
            assertFalse("Removal must not return before the in-flight publication is cancelled", removalFinished.await(100, TimeUnit.MILLISECONDS))
        } finally {
            release.countDown()
            join(error, publisher, remover)
        }
        assertTrue(completed.get())
        assertNull(notice.get())
        assertTrue(entries.writers().isEmpty())
    }

    @Test fun removalBeforeCompletionSuppressesBothResultAndLateActionPublication() {
        val entries = registry()
        val publications = InstallationNoticePublications(entries)
        val calls = mutableListOf<String>()
        publications.remove("session") { calls += "cancel" }
        assertFalse(publications.complete("session") { calls += "result" })
        assertFalse(publications.action("session") { calls += "action"; true })
        assertEquals(listOf("cancel"), calls)
        assertFalse(entries.update("session", "App", InstallerContract.STAGE_WRITING, 1f, "late").accepted)
    }

    @Test fun removalAlsoCancelsAnActionWhosePublicationWasAlreadyInFlight() {
        val entries = InstallationNoticeRegistry<String>()
        val publications = InstallationNoticePublications(entries)
        val notice = AtomicReference<String?>()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val removalFinished = CountDownLatch(1)
        val error = AtomicReference<Throwable?>()
        val publisher = worker(error) {
            assertTrue(publications.action("confirmation") {
                entered.countDown()
                check(release.await(5, TimeUnit.SECONDS))
                notice.set("confirmation")
                true
            })
        }
        assertTrue(entered.await(5, TimeUnit.SECONDS))
        val remover = worker(error) {
            publications.remove("confirmation") { notice.set(null) }
            removalFinished.countDown()
        }
        try {
            assertFalse(removalFinished.await(100, TimeUnit.MILLISECONDS))
        } finally {
            release.countDown()
            join(error, publisher, remover)
        }
        assertNull(notice.get())
        assertFalse(publications.action("confirmation") { notice.set("stale"); true })
    }

    @Test fun completionIsSynchronousAndCannotPublishTwice() {
        val entries = registry()
        val publications = InstallationNoticePublications(entries)
        var notice: String? = null
        assertTrue(publications.complete("session") { notice = "complete" })
        assertEquals("complete", notice)
        assertFalse(publications.complete("session") { notice = "duplicate" })
        assertFalse(publications.action("session") { notice = "late action"; true })
        publications.remove("session") { notice = null }
        assertNull(notice)
    }

    @Test fun platformPublicationFailureReleasesTheLockAndStillReclaimsTheSession() {
        val entries = registry()
        val publications = InstallationNoticePublications(entries)
        assertThrows(IllegalStateException::class.java) {
            publications.complete("session") { throw IllegalStateException("Notification service unavailable") }
        }
        assertTrue(entries.writers().isEmpty())
        var cancelled = false
        publications.remove("session") { cancelled = true }
        assertTrue(cancelled)
        assertTrue(publications.action("another confirmation") { true })
    }

    @Test fun closedActionTokensReuseTheRegistrysBoundedRetention() {
        val entries = InstallationNoticeRegistry<String>()
        val publications = InstallationNoticePublications(entries)
        repeat(256) { index -> publications.remove("closed-$index") {} }
        assertFalse(entries.isFinished("closed-0"))
        assertTrue(entries.isFinished("closed-128"))
        assertTrue(entries.isFinished("closed-255"))
        assertFalse(publications.action("closed-255") { fail("Closed token was republished"); true })
    }

    private fun registry() = InstallationNoticeRegistry<String>().apply {
        update("session", "App", InstallerContract.STAGE_WRITING, 0.5f, "payload")
    }

    private fun worker(error: AtomicReference<Throwable?>, run: () -> Unit) = Thread {
        try { run() } catch (failure: Throwable) { error.compareAndSet(null, failure) }
    }.apply { isDaemon = true; start() }

    private fun join(error: AtomicReference<Throwable?>, vararg workers: Thread) {
        workers.forEach { worker -> worker.join(5_000); assertFalse("Publication thread did not finish", worker.isAlive) }
        error.get()?.let { throw AssertionError("Publication worker failed", it) }
    }
}
