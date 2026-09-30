package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.topjohnwu.superuser.Shell
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.IPrivilegedInstaller
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.PrivilegedOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.PrivilegedServiceBinding
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.HiddenApiAccess
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.PackageManagerHidden
import io.github.supermonster003.autojs6.plugin.three.setup.installer.spike.SpikeInstallerActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.spike.SpikeStatusReceiver
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Opt-in only: -e privilegedAuthorizer shizuku|root. Ordinary CI does not acquire privileges. */
@RunWith(AndroidJUnit4::class)
class PrivilegedInstallerDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val authorizer = InstrumentationRegistry.getArguments().getString("privilegedAuthorizer")

    @Test fun installUpdateDowngradeUninstallAndRebind() {
        assumeTrue(authorizer in listOf("shizuku", "root"))
        // Never replace or uninstall an application that existed before this test.
        check(runCatching { context.packageManager.getPackageInfo(FIXTURE, 0) }.isFailure) { "Fixture already exists; refusing to modify it" }
        withService { service ->
            evidence("identity", "uid=${service.uid}, users=${service.users.getParcelableArrayList<Bundle>("users")?.map { it.getInt("id") }}")
            try {
                install(service, 1)
                install(service, 2)
                val downgrade = install(service, 1, requireSuccess = false)
                assertTrue(downgrade.getIntExtra(PackageInstaller.EXTRA_STATUS, -99) != PackageInstaller.STATUS_SUCCESS)
                uninstall(service)
                assertTrue(runCatching { context.packageManager.getPackageInfo(FIXTURE, 0) }.isFailure)
            } finally {
                if (runCatching { context.packageManager.getPackageInfo(FIXTURE, 0) }.isSuccess) uninstall(service)
            }
        }
        withService { service -> assertTrue(service.uid == 0 || service.uid == 2000) }
        evidence("rebind", "passed")
    }

    @Test fun rejectsInvalidStreamsAndAbandonsSessions() {
        assumeTrue(authorizer in listOf("shizuku", "root"))
        withService { service ->
            val ids = (1..4).map { service.createSession(Bundle(), caller(service), 0) }
            try {
                assertThrows(IllegalStateException::class.java) { service.createSession(Bundle(), caller(service), 0) }
                assertThrows(IllegalArgumentException::class.java) { service.openWrite(ids[0], "../base.apk", 1) }
                ParcelFileDescriptor.AutoCloseOutputStream(service.openWrite(ids[0], "base.apk", 16)).use { it.write(byteArrayOf(1)) }
                withResult(awaitResult = false) { sender ->
                    assertThrows(IllegalStateException::class.java) { service.commit(ids[0], sender) }
                }
                val heldWriters = (0..4).map { service.openWrite(ids[1], "split$it.apk", 16) }
                try {
                    // One writer is queued behind the four workers; abandon must release both kinds.
                    service.abandon(ids[1])
                } finally {
                    heldWriters.forEach { it.close() }
                }
            } finally {
                ids.forEach(service::abandon)
            }
            val next = service.createSession(Bundle(), caller(service), 0)
            service.abandon(next)
        }
        evidence("invalid-input-and-cleanup", "passed")
    }

    @Test fun preferredAndPersistentActivityPermissions() {
        assumeTrue(authorizer in listOf("shizuku", "root"))
        // The main-process wrapper separately exercises forwarding; UserService uses direct Binder.
        prepareShizuku()
        HiddenApiAccess.initialize()
        val packages = PackageManagerHidden(::ShizukuBinderWrapper)
        val component = ComponentName(context, SpikeInstallerActivity::class.java)
        val intents = PackageManagerHidden.INSTALL_ACTIONS.flatMap { action ->
            listOf("content", "file").map { PackageManagerHidden.intent(action, it) }
        }
        val competitors = intents.flatMap { packages.query(it, 0) }.map { it.activityInfo.packageName }.distinct()
        val existingApkPreferences = competitors.flatMap(packages::preferredActivities).any { it.hasDataType(PackageManagerHidden.APK_MIME) }
        assumeTrue("Existing APK preferences are preserved; use a clean device for this probe", !existingApkPreferences)
        withService { service ->
            try {
                val previous = ComponentName(context.packageName, "${context.packageName}.spike.SpikeOtherInstallerActivity")
                intents.forEach { intent ->
                    packages.addPreferred(PackageManagerHidden.filter(intent.action!!, intent.scheme!!), packages.query(intent, 0), previous, 0)
                    assertEquals(previous, packages.resolve(intent, 0))
                }
                assertEquals(4, service.setDefaultInstaller(component, true))
                intents.forEach { assertEquals(component, packages.resolve(it, 0)) }
                evidence("preferred", "4/4 action+scheme combinations, uid=${service.uid}")
            } finally {
                service.setDefaultInstaller(component, false)
            }
        }
        val filter = PackageManagerHidden.filter(Intent.ACTION_VIEW, "three-setup-spike")
        try {
            packages.persistentPreferred(filter, component, 0)
            evidence("persistent", "allowed, Shizuku uid=${Shizuku.getUid()}")
            packages.clearPersistentPreferred(context.packageName, 0)
        } catch (denied: SecurityException) {
            evidence("persistent", "SecurityException, Shizuku uid=${Shizuku.getUid()}: ${denied.message}")
        }
    }

    private fun install(service: IPrivilegedInstaller, version: Int, requireSuccess: Boolean = true): Intent {
        val bytes = instrumentation.context.assets.open("fixture-v$version.apk").use { it.readBytes() }
        val started = SystemClock.elapsedRealtime()
        val id = service.createSession(Bundle().apply { putLong(PrivilegedOptions.SIZE, bytes.size.toLong()) }, caller(service), 0)
        try {
            ParcelFileDescriptor.AutoCloseOutputStream(service.openWrite(id, "base.apk", bytes.size.toLong())).use { it.write(bytes) }
            val result = withResult { sender -> service.commit(id, sender) }!!
            evidence("install-v$version", "${SystemClock.elapsedRealtime() - started}ms status=${result.getIntExtra(PackageInstaller.EXTRA_STATUS, -99)} ${result.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)}")
            if (requireSuccess) {
                assertEquals(result.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE), PackageInstaller.STATUS_SUCCESS, result.getIntExtra(PackageInstaller.EXTRA_STATUS, -99))
                @Suppress("DEPRECATION")
                assertEquals(version, context.packageManager.getPackageInfo(FIXTURE, 0).versionCode)
            }
            return result
        } finally {
            service.abandon(id)
        }
    }

    private fun uninstall(service: IPrivilegedInstaller) {
        val started = SystemClock.elapsedRealtime()
        val result = withResult { sender -> service.uninstall(FIXTURE, 0, 0, sender) }!!
        evidence("uninstall", "${SystemClock.elapsedRealtime() - started}ms status=${result.getIntExtra(PackageInstaller.EXTRA_STATUS, -99)}")
        assertEquals(result.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE), PackageInstaller.STATUS_SUCCESS, result.getIntExtra(PackageInstaller.EXTRA_STATUS, -99))
    }

    private fun withResult(awaitResult: Boolean = true, block: (android.content.IntentSender) -> Unit): Intent? {
        val action = "${context.packageName}.spike.${UUID.randomUUID()}"
        val queue = LinkedBlockingQueue<Intent>()
        SpikeStatusReceiver.results[action] = queue
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
        val pending = PendingIntent.getBroadcast(context, 0, Intent(context, SpikeStatusReceiver::class.java).setAction(action), flags)
        try {
            block(pending.intentSender)
            if (!awaitResult) return null
            return queue.poll(60, TimeUnit.SECONDS).also { check(it != null) { "No package installer result" } }
        } finally {
            pending.cancel()
            SpikeStatusReceiver.results.remove(action)
        }
    }

    private fun withService(block: (IPrivilegedInstaller) -> Unit) {
        if (authorizer == "shizuku") prepareShizuku() else {
            Shell.setDefaultBuilder(Shell.Builder.create().setTimeout(30))
            check(Shell.getShell().isRoot) { "Root was not granted" }
        }
        val connected = CountDownLatch(1)
        val service = AtomicReference<IPrivilegedInstaller>()
        val binding = PrivilegedServiceBinding(context, authorizer == "root", {
            service.set(it)
            connected.countDown()
        }, { connected.countDown() })
        try {
            instrumentation.runOnMainSync { binding.bind() }
            check(connected.await(30, TimeUnit.SECONDS)) { "Privileged service bind timed out" }
            block(checkNotNull(service.get()) { "Privileged service disconnected" })
        } finally {
            val stopped = CountDownLatch(1)
            val binder = service.get()?.asBinder()
            if (binder != null) runCatching { binder.linkToDeath({ stopped.countDown() }, 0) }.onFailure { stopped.countDown() }
            instrumentation.runOnMainSync { binding.close() }
            if (binder != null) check(stopped.await(10, TimeUnit.SECONDS)) { "Privileged process did not stop after unbind" }
            if (authorizer == "root") Shell.getCachedShell()?.close()
        }
    }

    private fun prepareShizuku() {
        val received = CountDownLatch(1)
        val listener = Shizuku.OnBinderReceivedListener { received.countDown() }
        Shizuku.addBinderReceivedListenerSticky(listener)
        try { check(received.await(15, TimeUnit.SECONDS)) { "Start Shizuku before running the spike" } }
        finally { Shizuku.removeBinderReceivedListener(listener) }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            val permission = CountDownLatch(1)
            val result = Shizuku.OnRequestPermissionResultListener { _, _ -> permission.countDown() }
            Shizuku.addRequestPermissionResultListener(result)
            try {
                instrumentation.runOnMainSync { Shizuku.requestPermission(123) }
                check(permission.await(60, TimeUnit.SECONDS)) { "Shizuku permission request timed out" }
                check(Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) { "Shizuku permission denied" }
            } finally { Shizuku.removeRequestPermissionResultListener(result) }
        }
    }

    private fun caller(service: IPrivilegedInstaller) = if (service.uid == 2000) "com.android.shell" else context.packageName
    private fun evidence(step: String, result: String) {
        instrumentation.sendStatus(0, Bundle().apply { putString("spike", "${Build.MODEL} API=${Build.VERSION.SDK_INT} $authorizer $step: $result") })
    }

    companion object { private const val FIXTURE = "io.github.supermonster003.autojs6.installer.spike.fixture" }
}
