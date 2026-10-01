package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallStatusBridge
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallStatusBridge.Answer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallStatusBridge.Status
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallStatusReceiver
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.UserActionActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.UserActionBridge
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.UserActionState.Action
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue

/** Exercises the Activity/status boundary without installing a package or answering a system UI. */
@RunWith(AndroidJUnit4::class)
class UserActionDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun systemCancellationIsReadFromTheStatusBroadcastAfterTheActivityReturns() {
        val queue = pendingQueue()
        ticket(queue).use { ticket ->
            val status = ticket.await(5_000, SystemClock.elapsedRealtime() + 10_000, {}) { intent ->
                withAttached(intent) { action ->
                    assertEquals(Action.CONFIRM, action.start(true))
                    action.confirmationResult(Activity.RESULT_CANCELED, null)
                }
                queue.offer(Answer.Platform(Status(3, "User rejected confirmation", "example.fixture", null)))
            }
            assertEquals(3, status.status)
        }
    }

    @Test fun acceptedPromptKeepsWaitingForThePlatformAndEndsOnlyTheUserActionDeadline() {
        assertReturnedPromptWaitsForPlatform(Activity.RESULT_OK)
    }

    @Test fun legacyInstallerMayApproveButReturnDefaultCancelledBeforeDelayedPlatformSuccess() {
        assertReturnedPromptWaitsForPlatform(Activity.RESULT_CANCELED)
    }

    private fun assertReturnedPromptWaitsForPlatform(activityResult: Int) {
        val queue = pendingQueue()
        val success = Thread {
            SystemClock.sleep(250)
            queue.offer(Answer.Platform(Status(0, "installed", "example.fixture", null)))
        }
        try {
            ticket(queue).use { ticket ->
                val started = SystemClock.elapsedRealtime()
                val result = ticket.await(20, started + 5_000, {}) { intent ->
                    withAttached(intent) { action ->
                        assertEquals(Action.CONFIRM, action.start(true))
                        action.confirmationResult(activityResult, null)
                    }
                    success.start()
                }
                assertEquals(0, result.status)
                assertTrue(SystemClock.elapsedRealtime() - started >= 200)
            }
        } finally { if (success.isAlive) success.join(5_000) }
    }

    @Test fun terminalBroadcastWinsOverAnAlreadyQueuedOwnerCancellation() {
        val queue = pendingQueue()
        ticket(queue).use { ticket ->
            val result = ticket.await(5_000, SystemClock.elapsedRealtime() + 10_000, {}) { intent ->
                withAttached(intent) { action ->
                    action.start(true)
                    action.fail(InstallFailure(InstallerErrorCodes.USER_CANCELLED, "Confirmation owner closed"))
                }
                queue.offer(Answer.Platform(Status(0, "installed", "example.fixture", null)))
            }
            assertEquals(0, result.status)
        }
    }

    @Test fun queuedPlatformSuccessDoesNotNeedAnActivityResultOrStartANewPrompt() {
        val queue = pendingQueue().apply { offer(Answer.Platform(Status(0, null, "example.fixture", null))) }
        ticket(queue).use { ticket ->
            assertEquals(0, ticket.await(5_000, SystemClock.elapsedRealtime() + 10_000,
                { throw InstallFailure(InstallerErrorCodes.CANCELLED, "late cancellation") },
                { fail("A terminal operation must not display another prompt") }).status)
        }
    }

    @Test fun timeoutAndCancellationReleaseTheTicketEvenWhenNoActivityWasStarted() {
        for (cancel in listOf(false, true)) {
            var token: String? = null
            var cancelled = false
            ticket(pendingQueue()).use { ticket ->
                val error = assertThrows(InstallFailure::class.java) {
                    ticket.await(30, SystemClock.elapsedRealtime() + 5_000, {
                        if (cancelled) throw InstallFailure(InstallerErrorCodes.CANCELLED, "stopped")
                    }) { intent ->
                        token = intent.getStringExtra(UserActionBridge.EXTRA_TOKEN)
                        cancelled = cancel
                    }
                }
                assertEquals(if (cancel) InstallerErrorCodes.CANCELLED else InstallerErrorCodes.USER_ACTION_TIMEOUT, error.code)
            }
            instrumentation.runOnMainSync { assertNull(UserActionBridge.attach(requireNotNull(token), UserActionActivity())) }
        }
    }

    @Test fun permissionDetourAndRotationRetainOneConfirmationAndOneCancellation() {
        val errors = mutableListOf<InstallFailure>()
        UserActionBridge.open(context, Intent("test.CONFIRM"), true, { fail("No confirmation was accepted") }, errors::add).use { handle ->
            instrumentation.runOnMainSync {
                val first = UserActionActivity()
                val ticket = requireNotNull(UserActionBridge.attach(handle.token, first))
                assertEquals(Action.REQUEST_PERMISSION, ticket.start(false))
                UserActionBridge.detach(handle.token, first, changingConfigurations = true)
                val second = UserActionActivity()
                assertSame(ticket, UserActionBridge.attach(handle.token, second))
                assertEquals(Action.NONE, ticket.start(false))
                assertEquals(Action.CONFIRM, ticket.permissionResult(true))
                assertEquals(Action.NONE, ticket.permissionResult(true))
                UserActionBridge.detach(handle.token, second, changingConfigurations = false)
                ticket.confirmationResult(Activity.RESULT_OK, null)
            }
            assertEquals(listOf(InstallerErrorCodes.USER_CANCELLED), errors.map { it.code })
        }
    }

    @Test fun permissionDenialAndLegacyFailureDoNotMasqueradeAsSuccessfulInstallation() {
        val errors = mutableListOf<InstallFailure>()
        val denied = UserActionBridge.Ticket(Intent("test.CONFIRM"), true, { fail() }, errors::add)
        denied.start(false)
        assertEquals(Action.CANCELLED, denied.permissionResult(false))
        denied.confirmationResult(Activity.RESULT_OK, null)
        assertEquals(listOf(InstallerErrorCodes.USER_CANCELLED), errors.map { it.code })

        val failed = UserActionBridge.Ticket(Intent("test.CONFIRM"), false, { fail() }, errors::add)
        failed.start(true)
        failed.confirmationResult(Activity.RESULT_FIRST_USER, Intent().putExtra("android.intent.extra.INSTALL_RESULT", -3))
        assertEquals(InstallerErrorCodes.INSTALL_FAILED, errors.last().code)
        assertEquals("System confirmation result: -3", errors.last().systemMessage)
    }

    @Test fun forwardedConfirmationPreservesUriGrantsButCannotEscapeActivityResultOwnership() {
        val original = Intent("test.CONFIRM").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_FORWARD_RESULT or
            Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val ticket = UserActionBridge.Ticket(original, false, {}, {})
        val forResult = ticket.confirmationForResult()
        assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, forResult.flags)
        assertTrue(original.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test fun notificationFallbackCanOnlyReopenAnActiveUnansweredConfirmation() {
        val handle = UserActionBridge.open(context, Intent("test.CONFIRM"), false, {}, { fail(it.message) })
        try {
            assertNotNull(UserActionBridge.activityIntent(context, handle.token))
            assertFalse(UserActionBridge.isAttached(handle.token))
            instrumentation.runOnMainSync {
                val activity = UserActionActivity()
                val ticket = requireNotNull(UserActionBridge.attach(handle.token, activity))
                assertTrue(UserActionBridge.isAttached(handle.token))
                ticket.start(true)
                ticket.confirmationResult(Activity.RESULT_OK, null)
                assertNull(UserActionBridge.activityIntent(context, handle.token))
                UserActionBridge.detach(handle.token, activity, changingConfigurations = false)
                assertNull(UserActionBridge.attach(handle.token, UserActionActivity()))
            }
        } finally { handle.close() }
        assertNull(UserActionBridge.activityIntent(context, handle.token))
        assertFalse(UserActionBridge.isAttached(handle.token))
    }

    private fun pendingQueue() = LinkedBlockingQueue<Answer>().apply {
        offer(Answer.Platform(Status(-1, null, "example.fixture", Intent("test.CONFIRM"))))
    }

    private fun ticket(queue: LinkedBlockingQueue<Answer>): InstallStatusBridge.Ticket {
        val token = UUID.randomUUID().toString()
        val pending = PendingIntent.getBroadcast(context, 0,
            Intent(context, InstallStatusReceiver::class.java).setAction("${context.packageName}.TEST.$token"),
            if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
        return InstallStatusBridge.Ticket(context, token, pending, queue, requiresUnknownSourcesPermission = false)
    }

    private fun withAttached(intent: Intent, action: (UserActionBridge.Ticket) -> Unit) {
        assertEquals(UserActionActivity::class.java.name, intent.component?.className)
        val token = requireNotNull(intent.getStringExtra(UserActionBridge.EXTRA_TOKEN))
        instrumentation.runOnMainSync {
            val activity = UserActionActivity()
            val ticket = requireNotNull(UserActionBridge.attach(token, activity))
            try { action(ticket) } finally { UserActionBridge.detach(token, activity, changingConfigurations = true) }
        }
    }
}
