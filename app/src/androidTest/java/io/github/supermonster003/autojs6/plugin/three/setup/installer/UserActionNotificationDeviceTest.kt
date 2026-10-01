package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.UserActionLauncher
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.UninstallDialogActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.UninstallDialogBridge
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.UserActionActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.UserActionBridge
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Tests the fallback without posting real notifications or opening a system confirmation. */
@RunWith(AndroidJUnit4::class)
class UserActionNotificationDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun silentlyBlockedActivityCanBeOpenedFromNotificationAndAttachmentDismissesIt() = withNotifications { notices ->
        UserActionLauncher.delegate = { _, _ -> true }
        UserActionBridge.open(context, Intent("test.CONFIRM"), false, {}, {}).use { handle ->
            UserActionLauncher.launch(context, handle.intent)
            assertTrue(UserActionLauncher.notifyIfUnattached(context, handle.intent))
            assertEquals(UserActionActivity::class.java.name, notices.active[handle.token]?.component?.className)
            instrumentation.runOnMainSync {
                val activity = UserActionActivity()
                assertNotNull(UserActionBridge.attach(handle.token, activity))
                assertFalse(notices.active.containsKey(handle.token))
                assertFalse(UserActionLauncher.notifyIfUnattached(context, handle.intent))
                UserActionBridge.detach(handle.token, activity, changingConfigurations = true)
            }
        }
        assertTrue(notices.active.isEmpty())
    }

    @Test fun terminalOrCancelledTicketCannotPublishAStaleDelayedNotification() = withNotifications { notices ->
        UserActionLauncher.delegate = { _, _ -> true }
        val handle = UserActionBridge.open(context, Intent("test.CONFIRM"), false, {}, {})
        UserActionLauncher.launch(context, handle.intent)
        handle.close()
        assertFalse(UserActionLauncher.notifyIfUnattached(context, handle.intent))
        assertEquals(0, notices.published)
        assertTrue(notices.active.isEmpty())
    }

    @Test fun explicitStartFailureFallsBackImmediatelyAndStillRejectsWhenNotificationsAreUnavailable() = withNotifications { notices ->
        UserActionLauncher.delegate = { _, _ -> false }
        UserActionBridge.open(context, Intent("test.CONFIRM"), false, {}, {}).use { handle ->
            UserActionLauncher.launch(context, handle.intent)
            assertTrue(notices.active.containsKey(handle.token))
        }
        notices.allowed = false
        UserActionBridge.open(context, Intent("test.CONFIRM"), false, {}, {}).use { handle ->
            assertEquals("INSTALL_FAILED", assertThrows(InstallFailure::class.java) {
                UserActionLauncher.launch(context, handle.intent)
            }.code)
        }
    }

    @Test fun systemUninstallNotificationTargetsItsExistingTicketAndIsRemovedAfterTheResult() = withNotifications { notices ->
        UserActionLauncher.delegate = { _, _ -> false }
        val status = UninstallDialogBridge.await(context, "example.package", SystemClock.elapsedRealtime() + 5_000, {}) { intent ->
            UserActionLauncher.launch(context, intent)
            val token = requireNotNull(intent.getStringExtra(UninstallDialogBridge.EXTRA_TOKEN))
            assertEquals(UninstallDialogActivity::class.java.name, notices.active[token]?.component?.className)
            UninstallDialogBridge.complete(token, Activity.RESULT_OK, null)
        }
        assertEquals(0, status.status)
        assertTrue(notices.active.isEmpty())
    }

    @Test fun terminalCleanupCannotRaceAheadOfANotificationPublicationAndLeaveItVisible() = withNotifications { notices ->
        val publishing = CountDownLatch(1)
        val release = CountDownLatch(1)
        notices.beforePublish = { publishing.countDown(); check(release.await(5, TimeUnit.SECONDS)) }
        val handle = UserActionBridge.open(context, Intent("test.CONFIRM"), false, {}, {})
        val poster = Thread { UserActionLauncher.notifyIfUnattached(context, handle.intent) }
        val closer = Thread { handle.close() }
        try {
            poster.start()
            assertTrue(publishing.await(5, TimeUnit.SECONDS))
            closer.start()
            val deadline = SystemClock.elapsedRealtime() + 5_000
            while (UserActionBridge.activityIntent(context, handle.token) != null && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(10)
            assertNull(UserActionBridge.activityIntent(context, handle.token))
            release.countDown()
            poster.join(5_000)
            closer.join(5_000)
            assertFalse(poster.isAlive || closer.isAlive)
            assertTrue(notices.active.isEmpty())
        } finally {
            release.countDown()
            if (poster.isAlive) poster.join(5_000)
            if (closer.isAlive) closer.join(5_000)
            handle.close()
        }
    }

    private fun withNotifications(test: (Notices) -> Unit) {
        val oldLauncher = UserActionLauncher.delegate
        val oldNotifications = UserActionLauncher.notifications
        val notices = Notices()
        UserActionLauncher.notifications = notices
        try { test(notices) } finally {
            UserActionLauncher.delegate = oldLauncher
            UserActionLauncher.notifications = oldNotifications
        }
    }

    private class Notices : UserActionLauncher.NotificationDelegate {
        val active = ConcurrentHashMap<String, Intent>()
        @Volatile var allowed = true
        @Volatile var published = 0
        var beforePublish: () -> Unit = {}
        override fun notifyAction(context: Context, token: String, intent: Intent): Boolean {
            if (!allowed) return false
            beforePublish()
            active[token] = Intent(intent)
            published++
            return true
        }
        override fun dismissAction(context: Context, token: String) { active.remove(token) }
    }
}
