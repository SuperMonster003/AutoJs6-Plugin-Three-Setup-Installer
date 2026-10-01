package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.Activity
import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Intent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.spike.UserActionChildActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.UserActionActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.UserActionBridge
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

/** Exercises real child Activities, without installing packages or opening permission settings. */
@RunWith(AndroidJUnit4::class)
class UserActionTaskDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val manager = context.getSystemService(ActivityManager::class.java)

    @Test fun workerCancellationRemovesOwnedTaskAndItsNestedChildrenButLeavesAnotherTaskAlive() {
        assumeUnlocked()
        val independentId = UUID.randomUUID().toString()
        val childId = UUID.randomUUID().toString()
        val failures = ConcurrentLinkedQueue<InstallFailure>()
        val handle = UserActionBridge.open(context, childIntent(childId, descendant = true), false, {}, failures::add)
        try {
            context.startActivity(childIntent(independentId).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK))
            val independent = awaitValue("The independent fixture task was not resumed") { resumedChild(independentId) }
            context.startActivity(handle.intent)
            val stack = awaitValue("The confirmation did not create both child Activities") {
                val owner = owner(handle.token) ?: return@awaitValue null
                val children = children(childId)
                val nestedResumed = children.any { !it.intent.getBooleanExtra(UserActionChildActivity.EXTRA_OPEN_DESCENDANT, false) && it in resumed() }
                if (children.size == 2 && nestedResumed) Stack(owner, children, owner.taskId) else null
            }
            instrumentation.runOnMainSync {
                assertTrue(stack.owner.isTaskRoot)
                assertNotEquals(independent.taskId, stack.taskId)
                assertTrue(stack.children.all { it.taskId == stack.taskId })
            }

            // This is the same worker-side cleanup used when the session is cancelled or expires.
            handle.close()
            awaitValue("Cancellation left a confirmation child or its task alive") {
                if (stack.owner.isDestroyed && stack.children.all { it.isDestroyed } && stack.taskId !in taskIds()) Unit else null
            }
            instrumentation.runOnMainSync {
                assertFalse(independent.isDestroyed)
                assertTrue(independent.taskId in taskIds())
            }
            assertTrue(failures.isEmpty())
        } finally {
            handle.close()
            removeFixtureTasks(setOf(independentId, childId), handle.token)
        }
    }

    @Test fun cancellingANonRootConfirmationNeverRemovesItsCallersTask() {
        assumeUnlocked()
        val callerId = UUID.randomUUID().toString()
        val childId = UUID.randomUUID().toString()
        val handle = UserActionBridge.open(context, childIntent(childId), false, {}, {})
        try {
            context.startActivity(childIntent(callerId).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK))
            val caller = awaitValue("The caller fixture task was not resumed") { resumedChild(callerId) }
            instrumentation.runOnMainSync {
                // Exercise the defensive branch for an owner embedded in another Activity's task.
                caller.startActivity(Intent(handle.intent).setFlags(0))
            }
            val stack = awaitValue("The embedded confirmation did not open its child") {
                val owner = owner(handle.token) ?: return@awaitValue null
                val children = children(childId)
                if (children.size == 1 && children.single() in resumed()) Stack(owner, children, owner.taskId) else null
            }
            instrumentation.runOnMainSync {
                assertFalse(stack.owner.isTaskRoot)
                assertTrue(caller.isTaskRoot)
                assertEquals(caller.taskId, stack.taskId)
            }
            handle.close()
            awaitValue("Cancellation did not return to the caller Activity") {
                if (stack.owner.isDestroyed && stack.children.single().isDestroyed && caller in resumed()) Unit else null
            }
            instrumentation.runOnMainSync {
                assertFalse(caller.isDestroyed)
                assertTrue(caller.taskId in taskIds())
            }
        } finally {
            handle.close()
            removeFixtureTasks(setOf(callerId, childId), handle.token)
        }
    }

    private data class Stack(val owner: UserActionActivity, val children: List<UserActionChildActivity>, val taskId: Int)

    private fun childIntent(id: String, descendant: Boolean = false): Intent = Intent(context, UserActionChildActivity::class.java)
        .putExtra(UserActionChildActivity.EXTRA_FIXTURE_ID, id)
        .putExtra(UserActionChildActivity.EXTRA_OPEN_DESCENDANT, descendant)

    private fun owner(token: String): UserActionActivity? = activities().filterIsInstance<UserActionActivity>()
        .singleOrNull { it.intent.getStringExtra(UserActionBridge.EXTRA_TOKEN) == token }

    private fun children(id: String): List<UserActionChildActivity> = activities().filterIsInstance<UserActionChildActivity>()
        .filter { it.intent.getStringExtra(UserActionChildActivity.EXTRA_FIXTURE_ID) == id }

    private fun resumedChild(id: String): UserActionChildActivity? = resumed().filterIsInstance<UserActionChildActivity>()
        .singleOrNull { it.intent.getStringExtra(UserActionChildActivity.EXTRA_FIXTURE_ID) == id }

    private fun resumed(): Collection<Activity> = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)

    private fun activities(): List<Activity> = listOf(Stage.CREATED, Stage.STARTED, Stage.RESUMED, Stage.PAUSED, Stage.STOPPED)
        .flatMap { ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(it) }.distinct()

    @Suppress("DEPRECATION")
    private fun taskIds(): Set<Int> = manager.appTasks.mapNotNull { runCatching { it.taskInfo?.id }.getOrNull() }.toSet()

    private fun <T> awaitValue(message: String, query: () -> T?): T {
        val deadline = SystemClock.elapsedRealtime() + 8_000
        while (SystemClock.elapsedRealtime() < deadline) {
            var value: T? = null
            instrumentation.runOnMainSync { value = query() }
            value?.let { return it }
            SystemClock.sleep(25)
        }
        throw AssertionError(message)
    }

    private fun assumeUnlocked() {
        assumeFalse("Unlock the device to exercise child Activity cleanup", context.getSystemService(KeyguardManager::class.java).isKeyguardLocked)
    }

    private fun removeFixtureTasks(fixtureIds: Set<String>, token: String) {
        instrumentation.runOnMainSync {
            manager.appTasks.forEach { task ->
                val base = runCatching { task.taskInfo?.baseIntent }.getOrNull() ?: return@forEach
                val owned = when (base.component?.className) {
                    UserActionActivity::class.java.name -> base.getStringExtra(UserActionBridge.EXTRA_TOKEN) == token
                    UserActionChildActivity::class.java.name -> base.getStringExtra(UserActionChildActivity.EXTRA_FIXTURE_ID) in fixtureIds
                    else -> false
                }
                if (owned) task.finishAndRemoveTask()
            }
        }
    }
}
