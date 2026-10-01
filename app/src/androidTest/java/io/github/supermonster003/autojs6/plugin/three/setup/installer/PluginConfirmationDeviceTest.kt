package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.KeyguardManager
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonParser
import io.github.supermonster003.autojs6.plugin.three.setup.installer.binder.CallerGuard
import io.github.supermonster003.autojs6.plugin.three.setup.installer.binder.InstallerBinder
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallDialogActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation
import org.autojs.plugin.installer.api.IInstallerSessionCallback
import org.autojs.plugin.installer.api.InstallerContract
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PluginConfirmationDeviceTest {
    @Test fun decliningExplicitDialogNeverAllocatesAPackageInstallerSession() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assumeFalse("Unlock the device to exercise visible confirmation", context.getSystemService(KeyguardManager::class.java).isKeyguardLocked)
        val before = context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet()
        val ownership = FixturePackageOwnership(setOf(FixtureInstallUi.PACKAGE_NAME))
        val file = File(context.cacheDir, "dialog-fixture-${UUID.randomUUID()}.apk")
        instrumentation.context.assets.open("fixture-v1.apk").use { input -> file.outputStream().use(input::copyTo) }
        val done = CountDownLatch(1)
        var error: Bundle? = null
        var presentation: InstallPresentation.Record? = null
        val callback = object : IInstallerSessionCallback.Stub() {
            override fun onStage(id: String?, stage: String?, detail: Bundle?) = Unit
            override fun onProgress(id: String?, progress: Float, detail: Bundle?) = Unit
            override fun onCompleted(id: String?, result: Bundle?) { done.countDown() }
            override fun onFailed(id: String?, failure: Bundle?) { error = failure; done.countDown() }
        }
        try {
            InstallerBinder(context, object : CallerGuard { override fun enforceHost() = android.os.Process.myUid() }).use { router ->
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { source ->
                    val id = "decline-${UUID.randomUUID()}"
                    val request = Bundle().apply {
                        putInt(InstallerContract.KEY_CONTRACT_VERSION, InstallerContract.CONTRACT_VERSION)
                        putLong(InstallerContract.KEY_HOST_VERSION_CODE, ThreeSetupInstallerPlugin.REQUIRED_HOST_VERSION)
                        putString(InstallerContract.KEY_HOST_SESSION_ID, id)
                        putString(InstallerContract.KEY_REQUEST_JSON, """{"id":"$id","sources":[{"displayName":"fixture.apk","size":${file.length()}}],"interaction":"dialog","options":{"authorizer":"none","timeoutMillis":15000}}""")
                    }
                    val session = requireNotNull(router.openSession(arrayOf(source), request, callback))
                    try {
                        val expires = SystemClock.elapsedRealtime() + 10_000
                        var clicked = false
                        while (!clicked && SystemClock.elapsedRealtime() < expires) {
                            if (presentation == null) presentation = FixtureInstallUi.resumedRecord { it.request.id == id }
                            clicked = FixtureInstallUi.clickInstall(InstallDialogActivity.TAG_CANCEL, sessionId = id, packageName = FixtureInstallUi.PACKAGE_NAME)
                            if (!clicked) SystemClock.sleep(50)
                        }
                        assertTrue("The plugin confirmation could not be declined", clicked)
                        assertTrue(done.await(5, TimeUnit.SECONDS))
                        assertEquals("USER_CANCELLED", JsonParser.parseString(requireNotNull(error).getString(InstallerContract.KEY_ERROR_JSON)).asJsonObject["code"].asString)
                        assertEquals(before, context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet())
                    } finally { session.close() }
                }
            }
        } finally { presentation?.close(); file.delete(); ownership.close() }
    }
}
