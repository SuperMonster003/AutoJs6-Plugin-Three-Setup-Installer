package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallEngine
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallStatusBridge
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.SessionInstallEngine
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PlannedApk
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.io.OutputStream

/** Simulates an OEM confirmation at the session boundary; no package is installed and no UI opens. */
@RunWith(AndroidJUnit4::class)
class InstallEngineInteractionDeviceTest {
    @Test fun autoPrivilegedInstallationAcceptsSystemConfirmationAndReportsDialog() {
        Harness(Authorizer.SHIZUKU, pendingAction = true).use { harness ->
            val result = harness.install("auto")
            assertEquals("dialog", result.interaction)
            assertEquals(listOf("The system required confirmation despite the privileged authorizer"), result.notes)
            assertEquals(1, harness.confirmations)
            assertTrue(harness.closed)
            assertFalse(harness.abandoned)
        }
    }

    @Test fun explicitSilentNeverLaunchesSystemConfirmation() {
        Harness(Authorizer.ROOT, pendingAction = true).use { harness ->
            val failure = assertThrows(InstallFailure::class.java) { harness.install("silent") }
            assertEquals(InstallerErrorCodes.AUTHORIZER_REQUIRED, failure.code)
            assertEquals(0, harness.confirmations)
            assertTrue(harness.abandoned && harness.closed)
        }
    }

    @Test fun autoWithoutSystemConfirmationStillReportsSilent() {
        Harness(Authorizer.ROOT, pendingAction = false).use { harness ->
            val result = harness.install("auto")
            assertEquals("silent", result.interaction)
            assertTrue(result.notes.isEmpty())
            assertEquals(0, harness.confirmations)
        }
    }

    @Test fun ordinaryInstallationReportsDialogWithoutAnOemFallbackNote() {
        Harness(Authorizer.NONE, pendingAction = true).use { harness ->
            val result = harness.install("auto")
            assertEquals("dialog", result.interaction)
            assertTrue(result.notes.isEmpty())
            assertEquals(1, harness.confirmations)
        }
    }

    @Test fun explicitDialogRemainsDialogWithoutAnOemFallbackNote() {
        Harness(Authorizer.SHIZUKU, pendingAction = true).use { harness ->
            val result = harness.install("dialog")
            assertEquals("dialog", result.interaction)
            assertTrue(result.notes.isEmpty())
        }
    }

    @Test fun cancellationFromAutoConfirmationStillAbandons() {
        Harness(Authorizer.SHIZUKU, pendingAction = true).use { harness ->
            val failure = assertThrows(InstallFailure::class.java) {
                harness.install("auto") { throw InstallFailure(InstallerErrorCodes.CANCELLED, "Stopped") }
            }
            assertEquals(InstallerErrorCodes.CANCELLED, failure.code)
            assertTrue(harness.abandoned && harness.closed)
        }
    }

    private class Harness(private val authorizer: Authorizer, private val pendingAction: Boolean) : Closeable {
        private val file = File.createTempFile("interaction-", ".apk", InstrumentationRegistry.getInstrumentation().targetContext.cacheDir)
            .apply { writeBytes(byteArrayOf(1, 2, 3)) }
        private val prompt = Intent("test.CONFIRM_INSTALLATION")
        var confirmations = 0
        var abandoned = false
        var closed = false

        private val engine = object : SessionInstallEngine(authorizer) {
            override fun openSession(request: InstallEngine.Request, parameters: Parameters, deadlineMillis: Long): Session = object : Session {
                override fun openWrite(apk: PlannedApk, checkActive: () -> Unit): OutputStream = ByteArrayOutputStream()
                override fun fsync(output: OutputStream) = Unit
                override fun commit() = Unit
                override fun await(deadlineMillis: Long, checkActive: () -> Unit, onUserAction: (Intent) -> Unit): InstallStatusBridge.Status {
                    if (pendingAction) onUserAction(prompt)
                    checkActive()
                    return InstallStatusBridge.Status(0, null, "example.fixture", null)
                }
                override fun abandon() { abandoned = true }
                override fun close() { closed = true }
            }
        }

        fun install(interaction: String, confirm: () -> Unit = {}): InstallEngine.Result {
            val prepared = PreparedPackage("apk", file.name, file.length(), "example.fixture", "1", 1L,
                "Fixture", 24, 28, listOf(PlannedApk("base.apk", file, file.length(), null)),
                emptyList(), null, emptyList(), emptyList(), true)
            return engine.install(InstallEngine.Request(prepared, InstallOptions(authorizer = authorizer.id), 0, interaction), object : InstallEngine.Listener {
                override fun onUserAction(intent: Intent) {
                    assertSame(prompt, intent)
                    confirmations++
                    confirm()
                }
            })
        }

        override fun close() { file.delete() }
    }
}
