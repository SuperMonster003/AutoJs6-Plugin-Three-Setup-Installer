package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.Activity
import android.content.Intent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallStatusMapper
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.UninstallDialogActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.UninstallDialogBridge
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the result bridge without removing packages or launching system UI. */
@RunWith(AndroidJUnit4::class)
class UninstallDialogDeviceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun activityResultsPreserveSuccessCancellationAndLegacyFailure() {
        for ((result, status) in listOf(Activity.RESULT_OK to 0, Activity.RESULT_CANCELED to 3, Activity.RESULT_FIRST_USER to 1)) {
            val answer = UninstallDialogBridge.await(context, "example.package", SystemClock.elapsedRealtime() + 5_000, {}) { intent ->
                assertEquals(UninstallDialogActivity::class.java.name, intent.component?.className)
                val token = requireNotNull(intent.getStringExtra(UninstallDialogBridge.EXTRA_TOKEN))
                UninstallDialogBridge.complete(token, result, Intent().putExtra("android.intent.extra.INSTALL_RESULT", -3))
                // Activity destruction can follow the result before the worker resumes.
                UninstallDialogBridge.cancelIfPending(token)
            }
            assertEquals(status, answer.status)
            assertEquals("System uninstall result: -3", answer.message)
            if (status != 0) assertEquals(if (status == 3) "USER_CANCELLED" else "UNINSTALL_FAILED",
                InstallStatusMapper.toFailure(status, answer.message, answer.packageName, uninstall = true).code)
        }
    }

    @Test fun cancellationReleasesThePendingTicketAndDoesNotWaitForAnActivityResult() {
        var cancelled = false
        var token = ""
        val error = assertThrows(InstallFailure::class.java) {
            UninstallDialogBridge.await(context, "example.package", SystemClock.elapsedRealtime() + 5_000, {
                if (cancelled) throw InstallFailure("CANCELLED", "stopped")
            }) { intent ->
                token = requireNotNull(intent.getStringExtra(UninstallDialogBridge.EXTRA_TOKEN))
                cancelled = true
            }
        }
        assertEquals("CANCELLED", error.code)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            assertNull(UninstallDialogBridge.attach(token, UninstallDialogActivity()))
        }
        UninstallDialogBridge.complete(token, Activity.RESULT_OK, null)
    }

    @Test fun queuedSuccessSurvivesCancellationReportedAfterTheSystemResult() {
        var cancelled = false
        val answer = UninstallDialogBridge.await(context, "example.package", SystemClock.elapsedRealtime() + 5_000, {
            if (cancelled) throw InstallFailure("CANCELLED", "late cancellation")
        }) { intent ->
            val token = requireNotNull(intent.getStringExtra(UninstallDialogBridge.EXTRA_TOKEN))
            UninstallDialogBridge.complete(token, Activity.RESULT_OK, null)
            cancelled = true
            UninstallDialogBridge.cancelIfPending(token)
        }
        assertEquals(0, answer.status)
    }

    @Test fun recreatedOwnerCannotLaunchASecondUninstallAndOldOwnerCannotCancelIt() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val answer = UninstallDialogBridge.await(context, "example.package", SystemClock.elapsedRealtime() + 5_000, {}) { intent ->
            val token = requireNotNull(intent.getStringExtra(UninstallDialogBridge.EXTRA_TOKEN))
            instrumentation.runOnMainSync {
                val first = UninstallDialogActivity()
                val ticket = requireNotNull(UninstallDialogBridge.attach(token, first))
                assertTrue(ticket.claimLaunch())
                UninstallDialogBridge.detach(token, first, changingConfigurations = true)
                val replacement = UninstallDialogActivity()
                assertSame(ticket, UninstallDialogBridge.attach(token, replacement))
                assertFalse(ticket.claimLaunch())
                UninstallDialogBridge.detach(token, first, changingConfigurations = false)
                assertFalse(ticket.isAnswered)
                UninstallDialogBridge.complete(token, Activity.RESULT_OK, null)
                UninstallDialogBridge.detach(token, replacement, changingConfigurations = false)
            }
        }
        assertEquals(0, answer.status)
    }
}
