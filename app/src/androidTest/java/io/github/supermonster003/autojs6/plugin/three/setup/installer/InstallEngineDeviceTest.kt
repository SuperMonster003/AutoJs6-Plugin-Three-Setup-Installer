package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.topjohnwu.superuser.Shell
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.PrivilegedClient
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.CancellablePipeOutputStream
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallEngine
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.NoneInstallEngine
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.PrivilegedInstallEngine
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.UserActionLauncher
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.IPrivilegedInstaller
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PlannedApk
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class InstallEngineDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val authorizer = Authorizer.fromId(InstrumentationRegistry.getArguments().getString("engineAuthorizer"))

    @Test fun realInstallUpdateAndDowngradeResult() {
        assumeTrue("Opt in with engineAuthorizer=none|shizuku|root", authorizer != null)
        val selected = requireNotNull(authorizer)
        check(!fixtureInstalled()) { "The fixture already exists; refusing to modify it" }
        val client = PrivilegedClient.get(context)
        var service: IPrivilegedInstaller? = null
        val staging = File(context.cacheDir, "engine-fixture-${System.nanoTime()}").apply { check(mkdirs()) }
        try {
            if (selected.privileged) {
                check(AuthorizerStates.request(context, selected, 30_000)) { "Grant the requested authorizer before this test" }
                service = client.acquire(selected)
            }
            var releases = 0
            var abandons = 0
            var uidReads = 0
            val observed = service?.let { remote ->
                object : IPrivilegedInstaller by remote {
                    override fun getUid(): Int { uidReads++; return remote.uid }
                    override fun release(sessionId: Int) { remote.release(sessionId); releases++ }
                    override fun abandon(sessionId: Int) { abandons++; remote.abandon(sessionId) }
                }
            }
            val engine = if (selected.privileged) {
                PrivilegedInstallEngine(context, selected) { requireNotNull(observed) }
            } else NoneInstallEngine(context)
            val listener = object : InstallEngine.Listener {
                override fun onUserAction(intent: Intent) = UserActionLauncher.launch(context, intent)
            }
            for (version in listOf(1, 2)) {
                val started = SystemClock.elapsedRealtime()
                val result = engine.install(request(staging, version, selected), listener)
                assertEquals(FIXTURE, result.packageName)
                @Suppress("DEPRECATION")
                assertEquals(version, context.packageManager.getPackageInfo(FIXTURE, 0).versionCode)
                evidence("${selected.id} v$version: ${SystemClock.elapsedRealtime() - started}ms")
            }
            val failure = assertThrows(InstallFailure::class.java) { engine.install(request(staging, 1, selected), listener) }
            assertEquals(InstallerErrorCodes.INSTALL_FAILED, failure.code)
            assertEquals(4, failure.status)
            assertTrue(failure.systemMessage.orEmpty().contains("INSTALL_FAILED_VERSION_DOWNGRADE"))
            if (selected.privileged) {
                assertEquals("Each terminal result must release ownership", 3, releases)
                assertEquals("Terminal results must not abort platform sessions", 0, abandons)
                assertEquals("The engine must cache the UID of the reused Binder", 1, uidReads)
            }
        } finally {
            cleanupFixture()
            staging.deleteRecursively()
            val stopped = CountDownLatch(1)
            service?.asBinder()?.let { binder ->
                runCatching { binder.linkToDeath({ stopped.countDown() }, 0) }.onFailure { stopped.countDown() }
            }
            client.releaseAll()
            if (service != null) assertTrue("Service did not stop", stopped.await(10, TimeUnit.SECONDS))
            if (selected == Authorizer.ROOT) Shell.getCachedShell()?.close()
        }
    }

    @Test fun cancellingSystemConfirmationAbandonsTheCommittedSession() {
        assumeTrue(authorizer == Authorizer.NONE)
        check(!fixtureInstalled()) { "The fixture already exists; refusing to modify it" }
        val installer = context.packageManager.packageInstaller
        val before = installer.mySessions.map { it.sessionId }.toSet()
        val staging = File(context.cacheDir, "engine-cancel-${System.nanoTime()}").apply { check(mkdirs()) }
        var confirmed = false
        try {
            val failure = assertThrows(InstallFailure::class.java) {
                NoneInstallEngine(context).install(request(staging, 1, Authorizer.NONE), object : InstallEngine.Listener {
                    override fun onUserAction(intent: Intent) {
                        confirmed = true
                        throw InstallFailure(InstallerErrorCodes.CANCELLED, "Confirmation cancelled by test")
                    }
                })
            }
            assertTrue("No system confirmation was received", confirmed)
            assertEquals(InstallerErrorCodes.CANCELLED, failure.code)
            // abandonSession schedules removal in system_server; its Binder return is not removal.
            val removalDeadline = SystemClock.elapsedRealtime() + 5_000
            var remaining = installer.mySessions.map { it.sessionId }.toSet()
            while (remaining != before && SystemClock.elapsedRealtime() < removalDeadline) {
                SystemClock.sleep(25)
                remaining = installer.mySessions.map { it.sessionId }.toSet()
            }
            assertEquals(before, remaining)
            assertFalse(fixtureInstalled())
        } finally {
            cleanupFixture()
            staging.deleteRecursively()
        }
    }

    @Test fun fullPrivilegedPipeCanBeCancelledWithoutWaitingForTheReader() {
        val pipe = ParcelFileDescriptor.createPipe()
        val started = SystemClock.elapsedRealtime()
        try {
            val failure = assertThrows(InstallFailure::class.java) {
                CancellablePipeOutputStream(pipe[1]) {
                    if (SystemClock.elapsedRealtime() - started >= 300) throw InstallFailure(InstallerErrorCodes.CANCELLED, "stopped")
                }.use { it.write(ByteArray(2 * 1024 * 1024)) }
            }
            assertEquals(InstallerErrorCodes.CANCELLED, failure.code)
            assertTrue(SystemClock.elapsedRealtime() - started < 3_000)
        } finally {
            pipe.forEach { runCatching { it.close() } }
        }
    }

    @Test fun closedPrivilegedReaderReportsDestinationFailure() {
        val pipe = ParcelFileDescriptor.createPipe()
        pipe[0].close()
        val failure = assertThrows(InstallFailure::class.java) {
            CancellablePipeOutputStream(pipe[1]) {}.use { it.write(byteArrayOf(1)) }
        }
        assertEquals(InstallerErrorCodes.INSTALL_FAILED, failure.code)
        assertTrue(failure.message.orEmpty().contains("stopped reading"))
        assertTrue(failure.systemMessage.orEmpty().contains("EPIPE"))
    }

    private fun request(staging: File, version: Int, authorizer: Authorizer): InstallEngine.Request {
        val file = File(staging, "fixture-v$version.apk")
        instrumentation.context.assets.open(file.name).use { input -> file.outputStream().use(input::copyTo) }
        val prepared = PreparedPackage("apk", file.name, file.length(), FIXTURE, "$version.0", version.toLong(),
            "Fixture", 24, 28, listOf(PlannedApk("base.apk", file, file.length(), null)),
            emptyList(), null, emptyList(), emptyList(), true)
        return InstallEngine.Request(prepared, InstallOptions(authorizer = authorizer.id, timeoutMillis = 120_000), 0)
    }

    private fun fixtureInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(FIXTURE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) { false }

    private fun cleanupFixture() {
        if (!fixtureInstalled()) return
        instrumentation.uiAutomation.executeShellCommand("pm uninstall $FIXTURE").use { descriptor ->
            val answer = ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
            assertTrue(answer, answer.contains("Success"))
        }
    }

    private fun evidence(result: String) {
        instrumentation.sendStatus(0, android.os.Bundle().apply {
            putString("engine", "${Build.MODEL} API=${Build.VERSION.SDK_INT} $result")
        })
    }

    companion object { private const val FIXTURE = "io.github.supermonster003.autojs6.installer.spike.fixture" }
}
