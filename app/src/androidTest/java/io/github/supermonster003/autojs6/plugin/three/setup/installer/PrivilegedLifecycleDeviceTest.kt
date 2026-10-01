package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.os.Binder
import android.os.DeadObjectException
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallEngine
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.PrivilegedInstallEngine
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.IPrivilegedInstaller
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.PrivilegedInstallerImpl
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.HiddenApiAccess
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PlannedApk
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Uses fake Binder endpoints and private cache files; never installs an application. */
@RunWith(AndroidJUnit4::class)
class PrivilegedLifecycleDeviceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun concurrentCleanupClosesTheFrameworkHandleOnlyOnce() {
        val calls = AtomicInteger()
        val close = recordCloser { calls.incrementAndGet() }
        val workers = Executors.newFixedThreadPool(2)
        val start = CountDownLatch(1)
        try {
            val jobs = (1..2).map { workers.submit { start.await(); close() } }
            start.countDown()
            jobs.forEach { it.get(5, TimeUnit.SECONDS) }
            close()
            assertEquals(1, calls.get())
        } finally { workers.shutdownNow() }
    }

    @Test fun cleanupDoesNotRetryAnAmbiguousFrameworkCloseFailure() {
        val calls = AtomicInteger()
        val close = recordCloser {
            calls.incrementAndGet()
            throw RemoteException("Close processed but reply lost")
        }
        assertThrows(InvocationTargetException::class.java) { close() }
        close()
        assertEquals(1, calls.get())
    }

    @Test fun uidCacheUsesBinderIdentityAndRefreshesAfterRebinding() = withSource { directory, request ->
        val shell = FakeService(directory, 2000)
        val root = FakeService(directory, 0)
        var current = shell.proxy()
        val engine = PrivilegedInstallEngine(context, Authorizer.SHIZUKU) { current }
        engine.install(request, listener)
        current = shell.proxy() // A new AIDL wrapper for the same Binder must use the cached UID.
        engine.install(request, listener)
        assertEquals(1, shell.uidReads)
        assertEquals(listOf("com.android.shell", "com.android.shell"), shell.installers)
        current = root.proxy()
        engine.install(request, listener)
        assertEquals(1, root.uidReads)
        assertEquals(listOf(context.packageName), root.installers)
    }

    @Test fun failedUidLookupIsNotCached() = withSource { directory, request ->
        val service = FakeService(directory, 2000).apply { failUidOnce = true }
        val engine = PrivilegedInstallEngine(context, Authorizer.SHIZUKU) { service.proxy() }
        assertThrows(InstallFailure::class.java) { engine.install(request, listener) }
        assertTrue(service.installers.isEmpty())
        engine.install(request, listener)
        assertEquals(2, service.uidReads)
        assertEquals(listOf("com.android.shell"), service.installers)
    }

    @Test fun explicitInstallerSkipsTheUidRoundTrip() = withSource { directory, request ->
        val service = FakeService(directory, 2000).apply { failUidOnce = true }
        val engine = PrivilegedInstallEngine(context, Authorizer.SHIZUKU) { service.proxy() }
        engine.install(request.copy(options = request.options.copy(installer = "example.custom")), listener)
        assertEquals(0, service.uidReads)
        assertEquals(listOf("example.custom"), service.installers)
    }

    @Test fun anAmbiguousCreateReplyIsNeverReplayed() = withSource { directory, request ->
        val service = FakeService(directory, 2000).apply { dieAfterCreate = true }
        var acquisitions = 0
        val engine = PrivilegedInstallEngine(context, Authorizer.SHIZUKU) { acquisitions++; service.proxy() }
        val failure = assertThrows(InstallFailure::class.java) { engine.install(request, listener) }
        assertEquals(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, failure.code)
        assertEquals(1, acquisitions)
        assertEquals(1, service.installers.size)
        assertEquals(0, service.commits)
    }

    @Test fun anAmbiguousCommitReplyIsNeverReplayed() = withSource { directory, request ->
        val service = FakeService(directory, 2000).apply { dieAfterCommit = true }
        var acquisitions = 0
        val engine = PrivilegedInstallEngine(context, Authorizer.SHIZUKU) { acquisitions++; service.proxy() }
        val failure = assertThrows(InstallFailure::class.java) { engine.install(request, listener) }
        assertEquals(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, failure.code)
        assertEquals(1, acquisitions)
        assertEquals(1, service.installers.size)
        assertEquals(1, service.commits)
    }

    private fun recordCloser(close: () -> Unit): () -> Unit {
        HiddenApiAccess.initialize()
        val frameworkInterface = Class.forName("android.content.pm.IPackageInstallerSession")
        val binder = Binder()
        val remote = Proxy.newProxyInstance(frameworkInterface.classLoader, arrayOf(frameworkInterface)) { _, method, _ ->
            when (method.name) {
                "close" -> { close(); null }
                "asBinder" -> binder
                else -> error("Unexpected framework operation: ${method.name}")
            }
        }
        val session = PackageInstaller.Session::class.java.getConstructor(frameworkInterface).newInstance(remote)
        val type = Class.forName(PrivilegedInstallerImpl::class.java.name + "\$Record")
        val record = type.getDeclaredConstructor(PackageInstaller.Session::class.java).apply { isAccessible = true }.newInstance(session)
        val method = type.getDeclaredMethod("closeSession").apply { isAccessible = true }
        return { method.invoke(record); Unit }
    }

    private fun withSource(block: (File, InstallEngine.Request) -> Unit) {
        val directory = File(context.cacheDir, "identity-test-${System.nanoTime()}").apply { check(mkdirs()) }
        try {
            val source = File(directory, "source.apk").apply { writeBytes(byteArrayOf(1, 2, 3)) }
            val prepared = PreparedPackage("apk", source.name, source.length(), "example.fixture", "1", 1L,
                "Fixture", 24, 28, listOf(PlannedApk("base.apk", source, source.length(), null)),
                emptyList(), null, emptyList(), emptyList(), true)
            block(directory, InstallEngine.Request(prepared, InstallOptions(authorizer = "shizuku"), 0))
        } finally { directory.deleteRecursively() }
    }

    private inner class FakeService(private val directory: File, private val uid: Int) {
        private val binder = Binder()
        var uidReads = 0
        var failUidOnce = false
        var dieAfterCreate = false
        var dieAfterCommit = false
        var commits = 0
        val installers = mutableListOf<String>()

        fun proxy(): IPrivilegedInstaller = Proxy.newProxyInstance(
            IPrivilegedInstaller::class.java.classLoader, arrayOf(IPrivilegedInstaller::class.java),
        ) { _, method, args ->
            when (method.name) {
                "asBinder" -> binder
                "getUid" -> {
                    uidReads++
                    if (failUidOnce) { failUidOnce = false; throw RemoteException("Temporary UID lookup failure") }
                    uid
                }
                "createSession" -> {
                    installers += args!![1] as String
                    if (dieAfterCreate) throw DeadObjectException()
                    installers.size
                }
                "openWrite" -> ParcelFileDescriptor.open(File(directory, "destination.apk"),
                    ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE or ParcelFileDescriptor.MODE_WRITE_ONLY)
                "commit" -> {
                    commits++
                    if (dieAfterCommit) throw DeadObjectException()
                    (args!![1] as IntentSender).sendIntent(context, 0, Intent().putExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_SUCCESS), null, null)
                    null
                }
                "release", "abandon" -> null
                "getSessionRecoveryInfo" -> null
                else -> error("Unexpected private operation: ${method.name}")
            }
        } as IPrivilegedInstaller
    }

    private val listener = object : InstallEngine.Listener {
        override fun onUserAction(intent: Intent) = error("Unexpected confirmation")
    }
}
