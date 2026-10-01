package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstallNotificationIntentDeviceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun eachItemApprovalIsImmutableAndCannotReplaceAnotherPromptOrSession() {
        val first = InstallNotificationActionReceiver.approval(context, "Aa", "first-item")
        val next = InstallNotificationActionReceiver.approval(context, "Aa", "next-item")
        val other = InstallNotificationActionReceiver.approval(context, "BB", "first-item")
        val systemCancel = InstallNotificationActionReceiver.cancelSystem(context, "Aa")
        try {
            assertEquals(4, setOf(first, next, other, systemCancel).size)
            assertEquals(context.packageName, first.creatorPackage)
            if (Build.VERSION.SDK_INT >= 31) assertTrue(first.isImmutable)
            // There is no process-local owner for these tokens. Sending old actions must not
            // create a session, open an Activity or attempt to find its old package source.
            val before = InstallPresentation.snapshots(includeTerminal = true).map { it.token }
            first.send()
            next.send()
            systemCancel.send()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertEquals(before, InstallPresentation.snapshots(includeTerminal = true).map { it.token })
        } finally {
            listOf(first, next, other, systemCancel).forEach { it.cancel() }
        }
    }

    @Test fun differentTokensRemainIndependentEvenWhenTheirJavaHashesCollide() {
        val source = Intent(context, UserActionActivity::class.java).setData(Uri.parse("installer://confirmation/source"))
        assertEquals("Aa".hashCode(), "BB".hashCode())
        val first = InstallNotifications.activityPendingIntent(context, "Aa", "action", source)
        val second = InstallNotifications.activityPendingIntent(context, "BB", "action", source)
        try {
            assertNotEquals(first, second)
            assertEquals(context.packageName, first.creatorPackage)
            assertNull(source.categories)
            assertEquals("installer://confirmation/source", source.data.toString())
            if (Build.VERSION.SDK_INT >= 31) assertTrue(first.isImmutable)
        } finally {
            first.cancel()
            second.cancel()
        }
    }

    @Test fun progressAndResultCannotOverwriteAnOutstandingSystemConfirmation() {
        val source = Intent(context, UserActionActivity::class.java)
        val action = InstallNotifications.activityPendingIntent(context, "same-token", "action", source)
        val progress = InstallNotifications.activityPendingIntent(context, "same-token", "progress", source)
        val result = InstallNotifications.activityPendingIntent(context, "same-token", "result", source)
        try {
            assertNotEquals(action, progress)
            assertNotEquals(action, result)
            assertNotEquals(progress, result)
        } finally {
            action.cancel()
            progress.cancel()
            result.cancel()
        }
    }

    @Test fun notificationLinksCannotDelegateTheInstallersIdentityToAnotherPackage() {
        val external = Intent().setComponent(ComponentName("other.package", "other.package.Activity"))
        try {
            InstallNotifications.activityPendingIntent(context, "token", "action", external)
            fail("An external Activity must not receive a notification PendingIntent")
        } catch (_: IllegalArgumentException) {
            // Expected: only the plugin's own session/confirmation screens are valid targets.
        }
    }
}
