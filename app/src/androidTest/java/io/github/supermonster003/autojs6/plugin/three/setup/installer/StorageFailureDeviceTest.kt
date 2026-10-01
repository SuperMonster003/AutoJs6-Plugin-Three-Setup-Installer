package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.os.DeadObjectException
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.CancellablePipeOutputStream
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.StorageErrors
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.privilegedInstallerCall
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.PrivilegedOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.PrivilegedWriteState
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.ArchiveOpener
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.StagingDirectories
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Bounded fault injection: no disk filling, package install, preference changes or network. */
@RunWith(AndroidJUnit4::class)
class StorageFailureDeviceTest {
    @Test fun realErrnosSurviveNestedIoAndFutureFailuresWithoutMatchingSourceText() {
        for (errno in listOf(OsConstants.ENOSPC, OsConstants.EDQUOT)) {
            val original = IOException("Cannot write the APK", ErrnoException("write", errno))
            val nested = java.util.concurrent.ExecutionException(original)
            val failure = InstallFailure.from(nested, "example.fixture")
            assertEquals(InstallerErrorCodes.INSUFFICIENT_STORAGE, failure.code)
            assertEquals("example.fixture", failure.packageName)
            assertSame(nested, failure.cause)
            assertEquals(InstallerErrorCodes.INSUFFICIENT_STORAGE, ArchiveOpener.extractionFailure(original, null).code)
        }
        assertFalse(StorageErrors.isInsufficientStorage(IOException("ENOSPC (No space left on device)")))
        val denied = IOException("Cannot read", ErrnoException("open", OsConstants.EACCES))
        assertEquals(InstallerErrorCodes.SOURCE_UNREADABLE, InstallFailure.from(denied).code)
    }

    @Test fun pipeFailureReadsTheOriginalStorageErrorBeforeTheWriterFutureHasCompleted() {
        val pipe = ParcelFileDescriptor.createPipe()
        val state = PrivilegedWriteState()
        val readerClosed = CountDownLatch(1)
        val finishClose = CountDownLatch(1)
        val worker = Executors.newSingleThreadExecutor()
        val queries = AtomicInteger()
        try {
            val work = worker.submit {
                assertThrows(IOException::class.java) {
                    state.write(closeInput = {
                        pipe[0].close()
                        readerClosed.countDown()
                        check(finishClose.await(5, TimeUnit.SECONDS))
                    }, onFailure = {}) { throw IOException("Destination full", ErrnoException("write", OsConstants.ENOSPC)) }
                }
            }
            assertTrue(readerClosed.await(5, TimeUnit.SECONDS))
            assertFalse(work.isDone)
            CancellablePipeOutputStream(pipe[1], checkActive = {}, checkRemoteWrite = {
                queries.incrementAndGet()
                val original = assertThrows(IllegalStateException::class.java) { state.checkFailure() }
                assertTrue(StorageErrors.isInsufficientStorage(original))
                // Real Binder Parcel exception encoding preserves the exact private marker.
                val reply = Parcel.obtain()
                try {
                    reply.writeException(IllegalStateException(PrivilegedOptions.ERROR_INSUFFICIENT_STORAGE, original))
                    reply.setDataPosition(0)
                    privilegedInstallerCall { reply.readException() }
                } finally { reply.recycle() }
            }).use { output ->
                val failure = assertThrows(InstallFailure::class.java) { output.write(byteArrayOf(1, 2, 3)) }
                assertEquals(InstallerErrorCodes.INSUFFICIENT_STORAGE, failure.code)
            }
            assertEquals(1, queries.get())
            finishClose.countDown()
            work.get(5, TimeUnit.SECONDS)
        } finally { finishClose.countDown(); pipe.forEach { runCatching { it.close() } }; worker.shutdownNow() }
    }

    @Test fun cancellationRacingWithBrokenPipeTakesPrecedenceOverRemoteStorageLookup() {
        val pipe = ParcelFileDescriptor.createPipe()
        val checks = AtomicInteger()
        val queries = AtomicInteger()
        pipe[0].close()
        try {
            CancellablePipeOutputStream(pipe[1], checkActive = {
                if (checks.incrementAndGet() >= 2) throw InstallFailure(InstallerErrorCodes.CANCELLED, "Cancelled")
            }, checkRemoteWrite = { queries.incrementAndGet(); error("No remote lookup after cancellation") }).use { output ->
                assertEquals(InstallerErrorCodes.CANCELLED,
                    assertThrows(InstallFailure::class.java) { output.write(byteArrayOf(1)) }.code)
            }
            assertEquals(0, queries.get())
        } finally { runCatching { pipe[1].close() } }
    }

    @Test fun failedDiagnosticQueriesRemainInstallFailuresAndLateCancellationStillWins() {
        for (cancelDuringQuery in listOf(false, true)) {
            val pipe = ParcelFileDescriptor.createPipe()
            pipe[0].close()
            var cancelled = false
            try {
                CancellablePipeOutputStream(pipe[1], checkActive = {
                    if (cancelled) throw InstallFailure(InstallerErrorCodes.CANCELLED, "Cancelled during query")
                }, checkRemoteWrite = {
                    if (cancelDuringQuery) {
                        cancelled = true
                        throw InstallFailure(InstallerErrorCodes.INSUFFICIENT_STORAGE, "Late storage result")
                    }
                    throw IllegalStateException("Writer validation failed")
                }).use { output ->
                    val code = if (cancelDuringQuery) InstallerErrorCodes.CANCELLED else InstallerErrorCodes.INSTALL_FAILED
                    assertEquals(code, assertThrows(InstallFailure::class.java) { output.write(byteArrayOf(1)) }.code)
                }
            } finally { runCatching { pipe[1].close() } }
        }
    }

    @Test fun binderDeathFromBrokenPipeDiagnosticsRetainsAuthorizerFailureUnlessCancelled() {
        // Actual pipe EPIPE with an injected Binder transport exception; this does not kill a
        // privileged process or claim coverage of the separate process-death device matrix.
        for (cancelDuringQuery in listOf(false, true)) {
            val pipe = ParcelFileDescriptor.createPipe()
            pipe[0].close()
            val death = DeadObjectException()
            val queries = AtomicInteger()
            var cancelled = false
            try {
                CancellablePipeOutputStream(pipe[1], checkActive = {
                    if (cancelled) throw InstallFailure(InstallerErrorCodes.CANCELLED, "Cancelled during Binder death")
                }, checkRemoteWrite = {
                    queries.incrementAndGet()
                    cancelled = cancelDuringQuery
                    throw death
                }).use { output ->
                    val failure = assertThrows(InstallFailure::class.java) { output.write(byteArrayOf(1)) }
                    assertEquals(if (cancelDuringQuery) InstallerErrorCodes.CANCELLED else InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, failure.code)
                    if (!cancelDuringQuery) {
                        assertSame(death, failure.cause)
                        assertTrue(failure.retryable)
                    }
                }
                assertEquals(1, queries.get())
            } finally { runCatching { pipe[1].close() } }
        }
    }

    @Test fun staleCleanupNeverFollowsDirectorySymlinksOrRemovesLiveSources() {
        val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val base = File(cache, "p6-staging-${UUID.randomUUID()}").apply { check(mkdir()) }
        val root = File(base, "staging").apply { check(mkdir()) }
        val outside = File(base, "outside").apply { check(mkdir()) }
        val preserved = File(outside, "source.apk").apply { writeText("preserve") }
        val stale = File(root, "crashed").apply { check(mkdir()) }
        val now = System.currentTimeMillis()
        Os.symlink(outside.absolutePath, File(stale, "linked-directory").absolutePath)
        check(stale.setLastModified(now - StagingDirectories.STALE_AFTER_MILLIS - 1_000))
        val directories = StagingDirectories { now }
        var live: File? = null
        try {
            live = directories.create(root, "live")
            assertFalse(stale.exists())
            assertEquals("preserve", preserved.readText())
            check(live.setLastModified(now - 2 * StagingDirectories.STALE_AFTER_MILLIS))
            directories.cleanStale(root)
            assertTrue(live.isDirectory)
        } finally { directories.discard(live); base.deleteRecursively() }
    }
}
