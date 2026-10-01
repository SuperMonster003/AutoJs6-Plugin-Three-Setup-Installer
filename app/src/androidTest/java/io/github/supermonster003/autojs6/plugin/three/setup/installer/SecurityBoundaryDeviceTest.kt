package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.Process
import android.system.Os
import android.system.OsConstants
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.HiddenApiAccess
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.ExternalSources
import org.autojs.plugin.installer.api.IInstallerCallback
import org.autojs.plugin.installer.api.IInstallerPlugin
import org.autojs.plugin.installer.api.IInstallerSessionCallback
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Production boundaries only: no authorizer requests, package mutations or preference changes. */
@RunWith(AndroidJUnit4::class)
class SecurityBoundaryDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun productionInstallerRejectsNonHostUidBeforeEveryOperationAndCallback() {
        val hostUid = runCatching { context.packageManager.getApplicationInfo(ThreeSetupInstallerPlugin.HOST_PACKAGE_NAME, 0).uid }.getOrNull()
        assertNotEquals("The probe must run as a non-host caller", hostUid, Process.myUid())
        val ready = CountDownLatch(1)
        var binder: IBinder? = null
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) { binder = service; ready.countDown() }
            override fun onServiceDisconnected(name: ComponentName?) = Unit
        }
        assertTrue(context.bindService(Intent(context, ThreeSetupInstallerPluginService::class.java), connection, Context.BIND_AUTO_CREATE))
        try {
            assertTrue("The production service must bind", ready.await(10, TimeUnit.SECONDS))
            val installer = IInstallerPlugin.Stub.asInterface(requireNotNull(binder))
            assertEquals(ThreeSetupInstallerPlugin.ID, installer.info.id)
            assertFalse(installer.capabilities.isEmpty)
            val callbacks = AtomicInteger()
            val callback = object : IInstallerCallback.Stub() {
                override fun onResult(result: Bundle?) { callbacks.incrementAndGet() }
                override fun onError(error: Bundle?) { callbacks.incrementAndGet() }
            }
            val sessionCallback = object : IInstallerSessionCallback.Stub() {
                override fun onStage(id: String?, stage: String?, detail: Bundle?) { callbacks.incrementAndGet() }
                override fun onProgress(id: String?, progress: Float, detail: Bundle?) { callbacks.incrementAndGet() }
                override fun onCompleted(id: String?, result: Bundle?) { callbacks.incrementAndGet() }
                override fun onFailed(id: String?, error: Bundle?) { callbacks.incrementAndGet() }
            }
            // Invalid/empty payloads make the test harmless even if a future guard regresses.
            // The required synchronous SecurityException must precede any input validation.
            val operations: Map<String, () -> Unit> = linkedMapOf(
                "getAuthorizerState" to { installer.getAuthorizerState("none"); Unit },
                "getDefaultInstallerState" to { installer.getDefaultInstallerState(); Unit },
                "requestAuthorizer" to { installer.requestAuthorizer("none", callback) },
                "inspect" to { installer.inspect(null, null, callback) },
                "getUsers" to { installer.getUsers(null, callback) },
                "setDefaultInstaller" to { installer.setDefaultInstaller(false, null, callback) },
                "uninstall" to { installer.uninstall(null, callback) },
                "openSession" to { installer.openSession(emptyArray(), null, sessionCallback); Unit },
            )
            operations.forEach { (name, operation) ->
                assertThrows(name, SecurityException::class.java) { operation() }
            }
            assertEquals("Rejected calls must not enter the asynchronous work queue", 0, callbacks.get())
        } finally { context.unbindService(connection) }
    }

    @Suppress("DEPRECATION")
    @Test fun installedMergedComponentsKeepTheExplicitExportAndPermissionBoundary() {
        val info = context.packageManager.getPackageInfo(context.packageName,
            PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or
                PackageManager.GET_PROVIDERS or PackageManager.MATCH_DISABLED_COMPONENTS)
        val exported = linkedMapOf<String, String?>()
        info.activities.orEmpty().filter { it.exported }.forEach { exported[it.name] = it.permission }
        info.services.orEmpty().filter { it.exported }.forEach { exported[it.name] = it.permission }
        info.receivers.orEmpty().filter { it.exported }.forEach { exported[it.name] = it.permission }
        info.providers.orEmpty().filter { it.exported }.forEach {
            assertEquals("An exported provider must guard reads and writes equally", it.readPermission, it.writePermission)
            exported[it.name] = it.readPermission
        }
        val prefix = context.packageName
        val pluginPermission = "org.autojs.permission.PLUGIN"
        val expected = linkedMapOf<String, String?>(
            "$prefix.WakeActivity" to pluginPermission,
            "$prefix.ThreeSetupInstallerPluginInfoService" to pluginPermission,
            "$prefix.ThreeSetupInstallerPluginService" to pluginPermission,
            "$prefix.ui.InstallerSettingsActivity" to pluginPermission,
            "$prefix.ui.ExternalInstallActivity" to null,
            "$prefix.launcher.AdaptiveLightIconAlias" to null,
            "$prefix.launcher.AdaptiveDarkIconAlias" to null,
            "$prefix.launcher.AdaptiveAutoIconAlias" to null,
            "$prefix.launcher.TransparentIconAlias" to null,
            "rikka.shizuku.ShizukuProvider" to "android.permission.INTERACT_ACROSS_USERS_FULL",
            "androidx.profileinstaller.ProfileInstallReceiver" to "android.permission.DUMP",
        )
        if (BuildConfig.DEBUG) {
            expected["$prefix.spike.InstallRecoveryProbeActivity"] = "android.permission.DUMP"
            expected["$prefix.spike.InstallProcessDeathProbeActivity"] = "android.permission.DUMP"
            expected["$prefix.spike.SpikeInstallerActivity"] = null
            expected["$prefix.spike.SpikeOtherInstallerActivity"] = null
        }
        assertEquals("Dependency manifest merging must not add unreviewed exported components", expected, exported)
        val application = requireNotNull(info.applicationInfo)
        assertEquals(0, application.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
        assertEquals(BuildConfig.DEBUG, application.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
        info.services.orEmpty().single { it.name == "$prefix.priv.RootInstallerService" }.let { assertFalse(it.exported) }
        assertFalse(info.services.orEmpty().any { it.name == "$prefix.priv.ShizukuUserService" })
    }

    @Test fun providerCannotReturnWritableDescriptorAndRejectedSourceClosesItsHandle() {
        assertTrue("The hostile provider is compiled only in the debug APK", BuildConfig.DEBUG)
        val directory = File(context.cacheDir, "p2-source-fixtures-${UUID.randomUUID()}").apply { check(mkdir()) }
        val file = File(directory, "fixture.apk").apply { writeBytes(byteArrayOf(80, 75, 3, 4, 21, 34, 55, 89)) }
        val original = file.readBytes()
        val uri = Uri.parse("content://${context.packageName}.source-fixtures/${directory.name}/fixture.apk?writable=1")
        try {
            // Establish the adversarial precondition through the actual ContentResolver.
            context.contentResolver.openFileDescriptor(uri, "r")!!.use {
                assertEquals(OsConstants.O_RDWR, HiddenApiAccess.accessMode(it.fileDescriptor))
                assertFalse("The ownership probe must see the live fixture descriptor", descriptorsPointingAt(file).isEmpty())
            }
            assertTrue(descriptorsPointingAt(file).isEmpty())
            val failure = assertThrows(InstallFailure::class.java) {
                ExternalSources.fromIntent(Intent(Intent.ACTION_VIEW, uri)).open(context, {}).use { }
            }
            assertEquals(InstallerErrorCodes.INVALID_ARGUMENT, failure.code)
            assertTrue("A rejected provider handle must be closed: ${descriptorsPointingAt(file)}", descriptorsPointingAt(file).isEmpty())
            assertArrayEquals(original, file.readBytes())
        } finally { directory.deleteRecursively() }
    }

    @Test fun externalSourceChecksPreserveReadOnlyFileAndPipeSemantics() {
        assertTrue("The source provider is compiled only in the debug APK", BuildConfig.DEBUG)
        val directory = File(context.cacheDir, "p2-source-fixtures-${UUID.randomUUID()}").apply { check(mkdir()) }
        val bytes = ByteArray(1024) { (it % 251).toByte() }
        val file = File(directory, "fixture.apk").apply { writeBytes(bytes) }
        val content = Uri.parse("content://${context.packageName}.source-fixtures/${directory.name}/fixture.apk")
        try {
            for (uri in listOf(Uri.fromFile(file), content, content.buildUpon().appendQueryParameter("pipe", "1").build())) {
                ExternalSources.fromIntent(Intent(Intent.ACTION_VIEW, uri)).open(context, {}).use { opened ->
                    val descriptor = opened.descriptors.single()
                    assertEquals(OsConstants.O_RDONLY, HiddenApiAccess.accessMode(descriptor.fileDescriptor))
                    ParcelFileDescriptor.AutoCloseInputStream(ParcelFileDescriptor.dup(descriptor.fileDescriptor)).use {
                        assertArrayEquals(bytes, it.readBytes())
                    }
                }
                assertArrayEquals(bytes, file.readBytes())
            }
            assertTrue(descriptorsPointingAt(file).isEmpty())
        } finally { directory.deleteRecursively() }
    }

    private fun descriptorsPointingAt(file: File): List<String> {
        val source = Os.stat(file.absolutePath)
        val descriptors = requireNotNull(File("/proc/self/fd").list()) { "Descriptor ownership inspection is unavailable" }
        return descriptors.filter { descriptor ->
            // Inode identity also works when /data/user/0 and /data/data spell the same file.
            runCatching { Os.stat("/proc/self/fd/$descriptor").let { it.st_dev == source.st_dev && it.st_ino == source.st_ino } }
                .getOrDefault(false)
        }
    }
}
