package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonObject
import com.topjohnwu.superuser.Shell
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.PrivilegedClient
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.DescriptorInstallEnvironment
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.DeviceUsers
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallEngine
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallRequest
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallSession
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallSlots
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.PrivilegedInstallEngine
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.SourceEntry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.IPrivilegedInstaller
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PackageStaging
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.Closeable
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.ZipFile

/** Explicit opt-in; every successful result comes from the real privileged package installer. */
@RunWith(AndroidJUnit4::class)
class InstallConcurrencyDeviceTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @Test fun aSecondSamePackageSessionWaitsForRealSuccessReleaseAndVersionRefresh() = withFixture { run ->
        val first = run.job(1, holdWriting = true)
        val second = run.job(2, predecessor = first)
        first.start()
        first.awaitWriting()
        second.start()
        second.assertWaitingWithoutPlatformSession()
        assertEquals(1, first.created.size)
        assertEquals(0, first.releases.get())
        first.continueWriting.countDown()
        first.awaitSuccess(1)
        second.awaitSuccess(2)
        assertEquals(1L, requireNotNull(second.result)[InstallerContract.FIELD_PREVIOUS_VERSION_CODE].asLong)
        assertTrue("The second platform session started before the first platform handle was released",
            first.releasedAt.get() in 1 until second.createdAt.get())
        assertTrue("The second platform session started before the first item sources were discarded",
            first.discardedAt.get() in 1 until second.createdAt.get())
        assertEquals(1, first.releases.get())
        assertEquals(1, second.releases.get())
        assertEquals(0, first.abandons.get() + second.abandons.get())
        run.assertInstalledVersion(2)
        run.assertSourcesPreserved()
        evidence("SUCCESS serialized=true authorizer=${run.authorizer.id} firstSession=${first.created.single()} " +
            "secondSession=${second.created.single()} firstReleaseOrder=${first.releasedAt.get()} " +
            "secondCreateOrder=${second.createdAt.get()} previousVersionCode=1 installedVersionCode=2 sourcePreserved=true")
    }

    @Test fun cancellingTheSecondWaiterCreatesNoPlatformSessionAndTheFirstStillCompletes() = withFixture { run ->
        val first = run.job(1, holdWriting = true)
        val cancelled = run.job(2)
        first.start()
        first.awaitWriting()
        cancelled.start()
        cancelled.assertWaitingWithoutPlatformSession()
        assertTrue(cancelled.session.cancel())
        cancelled.awaitDone()
        assertEquals(InstallerErrorCodes.CANCELLED, cancelled.failure?.code)
        assertNull(cancelled.result)
        assertTrue(cancelled.created.isEmpty())
        assertEquals(0, cancelled.engineCalls.get())
        assertEquals(0, cancelled.releases.get() + cancelled.abandons.get())
        assertEquals(1, cancelled.discards.get())
        assertEquals(1L, first.done.count)
        first.continueWriting.countDown()
        first.awaitSuccess(1)
        run.assertInstalledVersion(1)
        // A real later update proves the cancelled waiter's permit and worker were released.
        val retry = run.job(2, predecessor = first)
        retry.start()
        retry.awaitSuccess(2)
        assertEquals(1L, requireNotNull(retry.result)[InstallerContract.FIELD_PREVIOUS_VERSION_CODE].asLong)
        run.assertInstalledVersion(2)
        run.assertSourcesPreserved()
        evidence("SUCCESS waitingCancellation=true authorizer=${run.authorizer.id} cancelledPlatformSessions=0 " +
            "firstSession=${first.created.single()} firstInstalledVersionCode=1 retrySession=${retry.created.single()} " +
            "retryPreviousVersionCode=1 installedVersionCode=2 sourcePreserved=true")
    }

    private fun withFixture(test: (Run) -> Unit) {
        val option = InstrumentationRegistry.getArguments().getString("concurrencyAuthorizer")
        assumeTrue("Opt in with concurrencyAuthorizer=shizuku|root", option != null)
        val selected = requireNotNull(Authorizer.fromId(option)).also { check(it.privileged) { "A privileged authorizer is required" } }
        check(InstallPresentation.snapshots().isEmpty()) { "Finish other plugin installations before this opt-in test" }
        // This guard examines every user with pm list packages -u before permitting any install.
        FixturePackageOwnership(setOf(FixtureInstallUi.PACKAGE_NAME)).use { ownership ->
            if (selected == Authorizer.SHIZUKU) {
                await("Previously granted Shizuku did not become usable", 5_000) { AuthorizerStates.state(context, selected).usable }
            } else {
                check(AuthorizerStates.request(context, selected, 10_000)) { "Root authorization was not granted" }
            }
            val run = Run(selected, PrivilegedClient.get(context).acquire(selected))
            var primary: Throwable? = null
            try {
                run.prepareSources()
                ownership.installationStarted()
                test(run)
            } catch (failure: Throwable) {
                primary = failure
                throw failure
            } finally {
                try { run.close() } catch (failure: Throwable) {
                    if (primary == null) throw failure else primary.addSuppressed(failure)
                }
            }
        }
        evidence("CLEANUP fixtureAbsentAcrossUsers=true ownedSourcesRemoved=true historyUnchanged=true workersSettled=true")
    }

    private inner class Run(val authorizer: Authorizer, val remote: IPrivilegedInstaller) : Closeable {
        val sequence = AtomicLong()
        val user = DeviceUsers(context).currentId
        val workers = Executors.newFixedThreadPool(2)
        private val folder = File(context.cacheDir, "p6-concurrency-${UUID.randomUUID()}").apply { check(mkdir()) }
        private val history = File(context.noBackupFilesDir, "installation-history/history.json")
        private val historyBefore = history.takeIf(File::isFile)?.let(::digest)
        private val stagingBefore = PackageStaging.root(context).listFiles().orEmpty().map { it.name }.toSet()
        private val jobs = mutableListOf<Job>()
        private val files = linkedMapOf<Int, File>()

        fun prepareSources() {
            (1..2).forEach { version ->
                val file = File(folder, "fixture-v$version.apk").also { files[version] = it }
                instrumentation.context.assets.open(file.name).use { input -> file.outputStream().use(input::copyTo) }
                assertFixture(file, version)
            }
        }

        fun job(version: Int, holdWriting: Boolean = false, predecessor: Job? = null): Job {
            val slot = InstallSlots.acquire()
            return try { Job(this, requireNotNull(files[version]), version, holdWriting, predecessor, slot).also(jobs::add) }
            catch (failure: Throwable) { slot.close(); throw failure }
        }

        fun assertInstalledVersion(expected: Long) {
            val installed = remote.getInstalledVersion(FixtureInstallUi.PACKAGE_NAME, user)
            assertTrue(installed.containsKey("versionCode"))
            assertEquals(expected, installed.getLong("versionCode"))
            @Suppress("DEPRECATION")
            val info = context.packageManager.getPackageInfo(FixtureInstallUi.PACKAGE_NAME, 0)
            @Suppress("DEPRECATION")
            assertEquals(expected, if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong())
        }

        fun assertSourcesPreserved() = files.forEach { (version, file) -> assertFixture(file, version) }

        override fun close() {
            var problem: Throwable? = null
            fun cleanup(action: () -> Unit) {
                try { action() } catch (failure: Throwable) {
                    if (problem == null) problem = failure else problem!!.addSuppressed(failure)
                }
            }
            // Interrupt the held progress callback before releasing its latch: cleanup must not
            // accidentally allow a test that failed its assertions to commit a new installation.
            jobs.forEach { it.session.close() }
            jobs.forEach { it.continueWriting.countDown() }
            workers.shutdownNow()
            cleanup { check(workers.awaitTermination(15, TimeUnit.SECONDS)) { "Owned installation workers did not settle" } }
            jobs.forEach { job ->
                cleanup { job.slot.close() }
                cleanup { job.environment.close() }
                job.created.filter { it !in job.settled }.forEach { id -> cleanup { remote.abandon(id) } }
            }
            cleanup { assertSourcesPreserved() }
            cleanup {
                assertEquals("Core installation tests must not create or alter user history", historyBefore, history.takeIf(File::isFile)?.let(::digest))
                val remaining = PackageStaging.root(context).listFiles().orEmpty().map { it.name }.toSet() - stagingBefore
                assertTrue("Owned staging remains: $remaining", remaining.isEmpty())
            }
            cleanup { check(folder.deleteRecursively() || !folder.exists()) { "Owned fixture sources remain" } }
            val death = CountDownLatch(1)
            cleanup {
                val binder = remote.asBinder()
                if (!binder.isBinderAlive) death.countDown()
                else try { binder.linkToDeath({ death.countDown() }, 0) } catch (failure: Exception) {
                    if (!binder.isBinderAlive) death.countDown() else throw failure
                }
            }
            cleanup { PrivilegedClient.get(context).releaseAll() }
            cleanup { check(death.await(10, TimeUnit.SECONDS) || !remote.asBinder().isBinderAlive) { "Privileged service did not stop" } }
            if (authorizer == Authorizer.ROOT) cleanup { Shell.getCachedShell()?.close() }
            problem?.let { throw it }
        }
    }

    private inner class Job(
        private val run: Run,
        private val source: File,
        private val version: Int,
        private val holdWriting: Boolean,
        private val predecessor: Job?,
        val slot: Closeable,
    ) : InstallSession.Listener {
        val prepared = CountDownLatch(1)
        val writing = CountDownLatch(1)
        val continueWriting = CountDownLatch(if (holdWriting) 1 else 0)
        val done = CountDownLatch(1)
        val created = CopyOnWriteArrayList<Int>()
        val settled = CopyOnWriteArrayList<Int>()
        val engineCalls = AtomicInteger()
        val releases = AtomicInteger()
        val abandons = AtomicInteger()
        val discards = AtomicInteger()
        val versionReads = AtomicInteger()
        val createdAt = AtomicLong()
        val releasedAt = AtomicLong()
        val discardedAt = AtomicLong()
        val confirmedSuccess = AtomicBoolean()
        private val pauseUsed = AtomicBoolean()
        @Volatile var result: JsonObject? = null
        @Volatile var failure: InstallFailure? = null
        private val delegate = ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY).use {
            DescriptorInstallEnvironment.acquire(context, listOf(it))
        }
        private val observed = object : IPrivilegedInstaller by run.remote {
            override fun createSession(params: Bundle, installerPackageName: String, userId: Int): Int {
                predecessor?.let {
                    check(it.confirmedSuccess.get() && it.releases.get() == 1 && it.discards.get() == 1) {
                        "The previous same-package installation has not succeeded and released its resources"
                    }
                }
                return run.remote.createSession(params, installerPackageName, userId).also {
                    created += it
                    createdAt.set(run.sequence.incrementAndGet())
                }
            }
            override fun release(sessionId: Int) {
                run.remote.release(sessionId)
                settled += sessionId
                releases.incrementAndGet()
                releasedAt.set(run.sequence.incrementAndGet())
            }
            override fun abandon(sessionId: Int) {
                run.remote.abandon(sessionId)
                settled += sessionId
                abandons.incrementAndGet()
            }
        }
        private val realEngine = PrivilegedInstallEngine(context, run.authorizer) { observed }
        private val engine = object : InstallEngine {
            override fun install(request: InstallEngine.Request, listener: InstallEngine.Listener, checkCancelled: () -> Unit): InstallEngine.Result {
                engineCalls.incrementAndGet()
                val installed = realEngine.install(request, object : InstallEngine.Listener by listener {
                    override fun onProgress(bytesWritten: Long, totalBytes: Long) {
                        listener.onProgress(bytesWritten, totalBytes)
                        if (bytesWritten <= 0 || !pauseUsed.compareAndSet(false, true)) return
                        check(created.size == 1) { "No real platform session exists at the write checkpoint" }
                        writing.countDown()
                        val deadline = SystemClock.elapsedRealtime() + 30_000
                        while (!continueWriting.await(100, TimeUnit.MILLISECONDS)) {
                            checkCancelled()
                            check(SystemClock.elapsedRealtime() < deadline) { "The controlled writing checkpoint timed out" }
                        }
                        checkCancelled()
                    }
                }, checkCancelled)
                confirmedSuccess.set(true)
                return installed
            }
        }
        val environment = object : InstallSession.Environment by delegate {
            override fun resolve(request: InstallRequest, deadlineMillis: Long, checkActive: () -> Unit): InstallSession.Target {
                val resolved = delegate.resolve(request, deadlineMillis, checkActive)
                check(resolved.authorizer == run.authorizer && resolved.userId == run.user)
                return resolved.copy(engine = engine)
            }
            override fun prepare(index: Int, sources: List<SourceEntry>, checkActive: () -> Unit): PreparedPackage =
                delegate.prepare(index, sources, checkActive).also {
                    check(it.packageName == FixtureInstallUi.PACKAGE_NAME && it.versionCode == version.toLong() && it.installable)
                    prepared.countDown()
                }
            override fun installedVersion(packageName: String, target: InstallSession.Target): InstallSession.Version? {
                versionReads.incrementAndGet()
                return delegate.installedVersion(packageName, target)
            }
            override fun discardItem(index: Int) {
                delegate.discardItem(index)
                discards.incrementAndGet()
                discardedAt.set(run.sequence.incrementAndGet())
            }
            override fun onUserAction(intent: Intent) = error("This opted-in privileged test must not open installation confirmation")
        }
        val session = InstallSession(InstallRequest("concurrency-${UUID.randomUUID()}", listOf(SourceEntry(0, 0, source.name, source.length())),
            InstallerContract.INTERACTION_SILENT, InstallOptions(authorizer = run.authorizer.id, timeoutMillis = 90_000, deleteSource = false)),
            environment, this, SystemClock::elapsedRealtime)

        fun start() = session.start(run.workers)
        fun awaitWriting() {
            await("The real platform writer never reached its byte checkpoint", 20_000) { writing.count == 0L || done.count == 0L }
            failure?.let { throw it }
            check(writing.count == 0L && done.count > 0) { "The real writer is not paused" }
        }
        fun assertWaitingWithoutPlatformSession() {
            await("The second source was not prepared", 15_000) { prepared.count == 0L || done.count == 0L }
            failure?.let { throw it }
            check(prepared.count == 0L && done.count > 0) { "The second session did not reach its wait" }
            // Observe the stable wait, rather than sampling only immediately after prepare.
            val until = SystemClock.elapsedRealtime() + 500
            while (SystemClock.elapsedRealtime() < until) {
                check(created.isEmpty() && engineCalls.get() == 0 && versionReads.get() == 0 && done.count > 0) {
                    "A concurrent same-package session allocated a platform handle or sampled the old version"
                }
                SystemClock.sleep(25)
            }
            assertEquals(InstallerContract.STAGE_PREPARING, session.status().stage)
        }
        fun awaitDone() { check(done.await(60, TimeUnit.SECONDS)) { "Real installation did not complete" } }
        fun awaitSuccess(expectedVersion: Long) {
            awaitDone()
            failure?.let { throw it }
            val answer = requireNotNull(result)
            assertTrue(answer.toString(), answer[InstallerContract.FIELD_OK].asBoolean)
            assertEquals(FixtureInstallUi.PACKAGE_NAME, answer[InstallerContract.FIELD_PACKAGE_NAME].asString)
            assertEquals(expectedVersion, answer[InstallerContract.FIELD_VERSION_CODE].asLong)
            assertEquals(run.authorizer.id, answer[InstallerContract.FIELD_AUTHORIZER].asString)
            assertFalse(answer[InstallerContract.FIELD_SOURCE_DELETED].asBoolean)
            assertTrue(confirmedSuccess.get())
        }
        override fun onCompleted(result: JsonObject) { this.result = result; slot.close(); done.countDown() }
        override fun onFailed(failure: InstallFailure) { this.failure = failure; slot.close(); done.countDown() }
    }

    @Suppress("DEPRECATION")
    private fun assertFixture(source: File, version: Int) {
        assertEquals("Only the pinned repository fixture is authorized", if (version == 1) V1_SHA256 else V2_SHA256, digest(source))
        val info = requireNotNull(context.packageManager.getPackageArchiveInfo(source.absolutePath, 0))
        assertEquals(FixtureInstallUi.PACKAGE_NAME, info.packageName)
        assertEquals(version.toLong(), if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong())
        assertEquals(0, requireNotNull(info.applicationInfo).flags and ApplicationInfo.FLAG_HAS_CODE)
        ZipFile(source).use { zip ->
            assertFalse("The fixture must not contain executable code", zip.entries().asSequence().any {
                it.name.endsWith(".dex", ignoreCase = true) || it.name.startsWith("lib/") && it.name.endsWith(".so", ignoreCase = true)
            })
        }
    }

    private fun digest(file: File): String = MessageDigest.getInstance("SHA-256").let { digest ->
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
        }
        digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
    private fun await(message: String, timeout: Long, ready: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeout
        while (SystemClock.elapsedRealtime() < deadline) { if (ready()) return; SystemClock.sleep(25) }
        error(message)
    }
    private fun evidence(message: String) = instrumentation.sendStatus(0, Bundle().apply { putString("install-concurrency", message) })

    private companion object {
        const val V1_SHA256 = "fec583a389978fdc298e9d85979b95feceb93e6fb3fd3f581511c826401e8b69"
        const val V2_SHA256 = "dc3716a9c54e63219ed621ef06f68c4088f54630e4b4c167aebfdda653a4bb38"
    }
}
