package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.content.Intent
import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonObject
import com.topjohnwu.superuser.Shell
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.PrivilegedClient
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.*
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PackageStaging
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class InstallSessionDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val authorizer = Authorizer.fromId(InstrumentationRegistry.getArguments().getString("engineAuthorizer"))
    private val listener = object : InstallEngine.Listener {
        override fun onUserAction(intent: Intent) = UserActionLauncher.launch(context, intent)
    }

    @Test fun realBatchPartialFailureVersionsAndUninstall() = withFixture { selected, files ->
        val v1 = fixture(files, 1)
        val invalid = File(files, "invalid.apk").apply { writeText("invalid package") }
        val v2 = fixture(files, 2)
        val result = batch(selected, listOf(v1, invalid, v2))
        val items = result.getAsJsonArray("results").map { it.asJsonObject }
        assertEquals(listOf(true, false, true), items.map { it["ok"].asBoolean })
        assertEquals(1L, items[2]["previousVersionCode"].asLong)
        assertEquals(2L, items[2]["versionCode"].asLong)
        assertFalse(items[0]["sourceDeleted"].asBoolean)
        assertTrue(v1.exists() && v2.exists())
        val uninstall = if (selected.privileged) PrivilegedUninstallEngine(context, selected) else NoneUninstallEngine(context)
        val answer = uninstall.uninstall(UninstallRequest(FIXTURE, false, "current", selected.id, "auto", 120_000), DeviceUsers(context).currentId, listener)
        assertEquals(FIXTURE, answer.packageName)
        assertFalse(installed())
    }

    @Test fun rootKeepDataSurvivesReinstallation() {
        assumeTrue(authorizer == Authorizer.ROOT)
        withFixture { selected, files ->
            val apk = fixture(files, 1)
            batch(selected, listOf(apk))
            val user = DeviceUsers(context).currentId
            val directory = "/data/user/$user/$FIXTURE/files"
            check(Shell.cmd("mkdir -p $directory && echo preserved > $directory/p2-marker").exec().isSuccess)
            val engine = PrivilegedUninstallEngine(context, selected)
            engine.uninstall(UninstallRequest(FIXTURE, true, "current", selected.id, "silent", 60_000), user, listener)
            assertFalse(installed())
            batch(selected, listOf(apk))
            assertEquals(listOf("preserved"), Shell.cmd("cat $directory/p2-marker").exec().out)
            engine.uninstall(UninstallRequest(FIXTURE, false, "current", selected.id, "silent", 60_000), user, listener)
            assertFalse(installed())
        }
    }

    @Test fun userListAndNonexistentTarget() {
        val selected = authorizer ?: Authorizer.NONE
        try {
            if (selected.privileged) check(AuthorizerStates.request(context, selected, 30_000))
            val manager = DeviceUsers(context)
            val users = manager.list(selected)
            assertTrue(users.any { it.id == manager.currentId && it.running })
            assertEquals(manager.currentId, manager.resolve("current", selected, 30_000))
            val error = assertThrows(InstallFailure::class.java) { manager.resolve("2147483647", selected, 30_000) }
            assertEquals(if (selected.privileged) "INVALID_ARGUMENT" else "AUTHORIZER_REQUIRED", error.code)
        } finally { release() }
    }

    @Test fun cancelledStalledSourceDoesNotCloseHostDescriptor() {
        val pipe = ParcelFileDescriptor.createPipe()
        val directory = PackageStaging.newDirectory(context, "..")
        val started = SystemClock.elapsedRealtime()
        try {
            val failure = assertThrows(InstallFailure::class.java) {
                PackageStaging.stageDescriptor(directory, pipe[0], "package.apk", -1, java.util.concurrent.atomic.AtomicBoolean()) {
                    if (SystemClock.elapsedRealtime() - started >= 200) throw InstallFailure("TIMEOUT", "stalled source")
                }
            }
            assertEquals("TIMEOUT", failure.code)
            assertTrue(SystemClock.elapsedRealtime() - started < 3_000)
            assertTrue(pipe[0].fileDescriptor.valid())
            assertTrue(directory.parentFile == PackageStaging.root(context))
        } finally {
            pipe.forEach { it.close() }
            PackageStaging.discard(directory)
        }
    }

    @Test fun closedDescriptorCannotBeAcquiredAndSameIdsHaveSeparateDirectories() {
        val pipe = ParcelFileDescriptor.createPipe()
        pipe[0].close()
        try {
            assertEquals("INVALID_ARGUMENT", assertThrows(InstallFailure::class.java) {
                DescriptorInstallEnvironment.acquire(context, listOf(pipe[0]))
            }.code)
        } finally { pipe[1].close() }
        val first = PackageStaging.newDirectory(context, "duplicate")
        val second = PackageStaging.newDirectory(context, "duplicate")
        try { assertNotEquals(first, second) } finally { PackageStaging.discard(first); PackageStaging.discard(second) }
    }

    @Test fun cancellingSessionDuringPipeReadCleansBeforeCallbackAndWorkerReuse() {
        val pipe = ParcelFileDescriptor.createPipe()
        val before = PackageStaging.root(context).listFiles().orEmpty().map { it.name }.toSet()
        val environment = DescriptorInstallEnvironment.acquire(context, listOf(pipe[0]))
        val request = InstallRequest("cancel-pipe", listOf(SourceEntry(0, 0, "package.apk", -1)), "auto",
            InstallOptions(authorizer = "none", timeoutMillis = 10_000))
        val preparing = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        var failed: InstallFailure? = null
        var cleanAtCallback = false
        val session = InstallSession(request, environment, object : InstallSession.Listener {
            override fun onStage(stage: String, detail: JsonObject) { if (stage == "preparing") preparing.countDown() }
            override fun onCompleted(result: JsonObject) { finished.countDown() }
            override fun onFailed(failure: InstallFailure) {
                failed = failure
                cleanAtCallback = before == PackageStaging.root(context).listFiles().orEmpty().map { it.name }.toSet()
                finished.countDown()
            }
        }, SystemClock::elapsedRealtime)
        try {
            session.start(executor)
            assertTrue(preparing.await(3, TimeUnit.SECONDS))
            val readDeadline = SystemClock.elapsedRealtime() + 3_000
            fun hasStagedTarget() = PackageStaging.root(context).listFiles().orEmpty()
                .filter { it.name !in before }.any { File(it, "item-0/source-0/package.apk").exists() }
            while (!hasStagedTarget() && SystemClock.elapsedRealtime() < readDeadline) SystemClock.sleep(10)
            assertTrue("The worker never reached source reading", hasStagedTarget())
            assertTrue(session.cancel())
            assertTrue(finished.await(3, TimeUnit.SECONDS))
            assertEquals("CANCELLED", failed?.code)
            assertTrue(cleanAtCallback)
            assertTrue(pipe[0].fileDescriptor.valid())
            assertFalse(executor.submit<Boolean> { Thread.currentThread().isInterrupted }.get(3, TimeUnit.SECONDS))
        } finally {
            session.close()
            pipe.forEach { it.close() }
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS))
        }
    }

    private fun batch(selected: Authorizer, files: List<File>): JsonObject {
        val before = PackageStaging.root(context).listFiles().orEmpty().map { it.name }.toSet()
        val descriptors = files.map { ParcelFileDescriptor.open(it, ParcelFileDescriptor.MODE_READ_ONLY) }
        val environment = try { DescriptorInstallEnvironment.acquire(context, descriptors) } finally { descriptors.forEach { it.close() } }
        val request = InstallRequest("device-batch", files.mapIndexed { index, file -> SourceEntry(index, index, "same.apk", file.length()) },
            "auto", InstallOptions(authorizer = selected.id, timeoutMillis = 120_000, deleteSource = true))
        val finished = CountDownLatch(1)
        var completed: JsonObject? = null
        var failed: InstallFailure? = null
        val executor = Executors.newSingleThreadExecutor()
        val session = InstallSession(request, environment, object : InstallSession.Listener {
            override fun onCompleted(result: JsonObject) { completed = result; finished.countDown() }
            override fun onFailed(failure: InstallFailure) { failed = failure; finished.countDown() }
        }, SystemClock::elapsedRealtime)
        try {
            session.start(executor)
            assertTrue("Batch did not complete", finished.await(150, TimeUnit.SECONDS))
            failed?.let { throw it }
            assertEquals(before, PackageStaging.root(context).listFiles().orEmpty().map { it.name }.toSet())
            return requireNotNull(completed)
        } finally {
            session.close()
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))
        }
    }

    private fun withFixture(test: (Authorizer, File) -> Unit) {
        assumeTrue("Opt in with engineAuthorizer=none|shizuku|root", authorizer != null)
        val selected = requireNotNull(authorizer)
        check(!installed(includeData = true)) { "Fixture or retained data already exists; refusing to modify it" }
        val directory = File(context.cacheDir, "batch-fixture-${System.nanoTime()}").apply { check(mkdir()) }
        try {
            if (selected.privileged) check(AuthorizerStates.request(context, selected, 30_000))
            test(selected, directory)
        } finally {
            if (installed(includeData = true)) {
                val answer = instrumentation.uiAutomation.executeShellCommand("pm uninstall $FIXTURE").use {
                    ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().use { reader -> reader.readText() }
                }
                assertTrue(answer, answer.contains("Success"))
            }
            directory.deleteRecursively()
            release()
        }
    }

    private fun fixture(directory: File, version: Int) = File(directory, "fixture-v$version.apk").also { file ->
        instrumentation.context.assets.open(file.name).use { input -> file.outputStream().use(input::copyTo) }
    }

    @Suppress("DEPRECATION")
    private fun installed(includeData: Boolean = false) = try {
        context.packageManager.getPackageInfo(FIXTURE, if (includeData) PackageManager.MATCH_UNINSTALLED_PACKAGES else 0)
        true
    } catch (_: PackageManager.NameNotFoundException) { false }

    private fun release() {
        // RootService shutdown is asynchronous. Subsequent tests may bind again only after death.
        val stopped = CountDownLatch(1)
        val remote = if (authorizer?.privileged == true) runCatching { PrivilegedClient.get(context).acquire(requireNotNull(authorizer)) }.getOrNull() else null
        remote?.asBinder()?.let { runCatching { it.linkToDeath({ stopped.countDown() }, 0) }.onFailure { stopped.countDown() } }
        PrivilegedClient.get(context).releaseAll()
        if (remote != null) assertTrue(stopped.await(10, TimeUnit.SECONDS))
        if (authorizer == Authorizer.ROOT) Shell.getCachedShell()?.close()
    }

    companion object { private const val FIXTURE = "io.github.supermonster003.autojs6.installer.spike.fixture" }
}
