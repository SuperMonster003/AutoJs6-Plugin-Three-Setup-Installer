package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallRequest
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallSession
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.SourceEntry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.NoneInstallEngine
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.DeviceUsers
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.UserActionLauncher
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PlannedApk
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.io.File
import java.util.UUID

/** Exercises real notification routing without a package install, user preference change or grant. */
@RunWith(AndroidJUnit4::class)
class NotificationInstallDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun initialConfirmationUsesNotificationAndOnlyItsCurrentPromptCanApprove() {
        assumeTrue("Notifications must already be enabled", InstallNotifications.available(context))
        val request = InstallRequest("notification-test-${UUID.randomUUID()}", listOf(SourceEntry(0, 0, "preview.apk", 1)),
            InstallerContract.INTERACTION_NOTIFICATION, InstallOptions())
        // A direct record intentionally has no registry/persistence ticket. This presentation
        // test cannot modify the user's history, run an installation or start a source service.
        val record = InstallPresentation.Record(context.applicationContext, request, InstallPresentation.Callbacks(cancel = {}), false)
        val file = File(context.cacheDir, "unopened-notification-preview.apk")
        val prepared = PreparedPackage("apk", "preview.apk", 1, "example.notification.preview", "1", 1,
            "Notification preview", 24, 35, listOf(PlannedApk("base.apk", file, 1, null)),
            emptyList(), null, emptyList(), emptyList(), true, displayApk = null)
        val choice = AtomicReference<InstallPresentation.Choice>()
        val failure = AtomicReference<Throwable>()
        val finished = CountDownLatch(1)
        val worker = Thread {
            try {
                choice.set(record.confirm(0, prepared, InstallSession.Target(Authorizer.NONE, DeviceUsers(context).currentId, NoneInstallEngine(context)),
                    InstallOptions(), SystemClock.elapsedRealtime() + 10_000, record::checkNotificationAvailable))
            } catch (error: Throwable) { failure.set(error) }
            finally { finished.countDown() }
        }
        try {
            assertEquals(HomeActivity::class.java.name, record.activityIntent().component?.className)
            record.show()
            worker.start()
            val until = SystemClock.elapsedRealtime() + 5_000
            val manager = context.getSystemService(NotificationManager::class.java)
            while (manager.activeNotifications.none { it.tag == "installation.action:${record.token}" }) {
                if (SystemClock.elapsedRealtime() >= until) fail("The initial installation notification was not posted")
                SystemClock.sleep(20)
            }
            val notice = manager.activeNotifications.single { it.tag == "installation.action:${record.token}" }.notification
            assertNull(notice.contentIntent)
            assertEquals(2, notice.actions.size)
            assertFalse(record.acceptFromNotification("a-stale-prompt"))
            assertEquals(1L, finished.count)
            assertTrue(record.acceptFromNotification(requireNotNull(record.snapshot().prompt).token))
            assertTrue(finished.await(5, TimeUnit.SECONDS))
            assertNull(failure.get())
            assertEquals(setOf("base.apk"), choice.get().selectedApkNames)
        } finally {
            record.close()
            worker.join(5_000)
            assertFalse(worker.isAlive)
            InstallNotifications.remove(context, record.token)
        }
    }

    @Test fun systemConfirmationWaitsForNotificationTapAndCanBeCancelledThere() {
        assumeTrue("Notification permission and the installation channel must already be enabled", InstallNotifications.available(context))
        val launches = AtomicInteger()
        val oldLauncher = UserActionLauncher.delegate
        val returned = CountDownLatch(1)
        val failure = AtomicReference<InstallFailure>()
        UserActionLauncher.delegate = { _, _ -> launches.incrementAndGet(); true }
        val handle = UserActionBridge.open(context, Intent("test.NOTIFICATION_ONLY_CONFIRMATION"), false, {}, {
            failure.set(it)
            returned.countDown()
        })
        try {
            UserActionBridge.notifyOnly(context, handle.intent)
            instrumentation.waitForIdleSync()
            assertEquals(0, launches.get())
            assertFalse(UserActionBridge.isAttached(handle.token))
            val notice = context.getSystemService(NotificationManager::class.java).activeNotifications
                .single { it.tag == "installation.action:${handle.token}" }.notification
            assertNotNull(notice.contentIntent)
            if (Build.VERSION.SDK_INT >= 31) assertTrue(notice.contentIntent.isImmutable)
            val cancel = notice.actions.single().actionIntent
            cancel.send()
            assertTrue(returned.await(5, TimeUnit.SECONDS))
            assertEquals(InstallerErrorCodes.USER_CANCELLED, failure.get()?.code)
            assertEquals(0, launches.get())
        } finally {
            handle.close()
            UserActionLauncher.delegate = oldLauncher
        }
    }

    @Test fun closedSystemTicketCannotBeRepostedOrRecreateAnInstallation() {
        val handle = UserActionBridge.open(context, Intent("test.EXPIRED_NOTIFICATION"), false, {}, {})
        val intent = Intent(handle.intent)
        handle.close()
        val failure = assertThrows(InstallFailure::class.java) { UserActionBridge.notifyOnly(context, intent) }
        assertEquals(InstallerErrorCodes.CANCELLED, failure.code)
        assertNull(UserActionBridge.activityIntent(context, handle.token))
    }

    @Test fun unavailableNotificationsFailExplicitlyBeforeWaitingForConfirmation() {
        if (InstallNotifications.available(context)) {
            InstallNotifications.requireAvailable(context)
        } else {
            val failure = assertThrows(InstallFailure::class.java) { InstallNotifications.requireAvailable(context) }
            assertEquals(InstallerErrorCodes.NOTIFICATION_UNAVAILABLE, failure.code)
        }
    }

    @Test fun isolatedDisabledChannelIsReallyRejectedWithoutChangingTheInstallationChannel() {
        assumeTrue(Build.VERSION.SDK_INT >= 26)
        assumeTrue("The original installation channel must already be usable", InstallNotifications.available(context))
        val manager = context.getSystemService(NotificationManager::class.java)
        val original = requireNotNull(manager.getNotificationChannel(InstallNotifications.CHANNEL_ID))
        val enabled = manager.areNotificationsEnabled()
        val id = "notification-audit-${UUID.randomUUID()}"
        assertNull(manager.getNotificationChannel(id))
        try {
            manager.createNotificationChannel(NotificationChannel(id, "Notification audit fixture", NotificationManager.IMPORTANCE_NONE))
            assertEquals(NotificationManager.IMPORTANCE_NONE, manager.getNotificationChannel(id).importance)
            val failure = assertThrows(InstallFailure::class.java) { InstallNotifications.requireChannelAvailable(context, id) }
            assertEquals(InstallerErrorCodes.NOTIFICATION_UNAVAILABLE, failure.code)
            InstallNotifications.requireAvailable(context)
            assertEquals(original, manager.getNotificationChannel(InstallNotifications.CHANNEL_ID))
            assertEquals(enabled, manager.areNotificationsEnabled())
        } finally {
            manager.deleteNotificationChannel(id)
            assertNull(manager.getNotificationChannel(id))
            assertEquals(original, manager.getNotificationChannel(InstallNotifications.CHANNEL_ID))
            assertEquals(enabled, manager.areNotificationsEnabled())
        }
    }
}
