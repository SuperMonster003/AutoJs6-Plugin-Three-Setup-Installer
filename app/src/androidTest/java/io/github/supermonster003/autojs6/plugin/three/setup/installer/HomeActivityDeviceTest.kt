package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.KeyguardManager
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerState
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.queue.InstallQueue
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.HomeActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallChoices
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation
import org.autojs.plugin.installer.api.InstallerContract
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Supplies display snapshots only: no installation, history mutation or preference edits. */
@RunWith(AndroidJUnit4::class)
class HomeActivityDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val render = HomeActivity::class.java.getDeclaredMethod("renderTasks", List::class.java).apply { isAccessible = true }

    @Test fun progressUpdatesRetainTheTaskControlsFocusAndPressedState() {
        unlocked()
        val first = snapshot()
        val second = snapshot()
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                render.invoke(activity, listOf(first))
                val root = activity.window.decorView
                val row = requireNotNull(root.findViewWithTag<LinearLayout>("home-task-${first.token}"))
                val cancel = requireNotNull(root.findViewWithTag<View>("home-cancel-${first.token}"))
                val progress = requireNotNull(root.findViewWithTag<ProgressBar>("home-task-progress-${first.token}"))
                cancel.isFocusableInTouchMode = true
                assertTrue(cancel.requestFocus())
                cancel.isPressed = true
                repeat(10) { index ->
                    render.invoke(activity, listOf(first.copy(state = first.state.copy(progress = index / 10f))))
                    assertSame(row, root.findViewWithTag<View>("home-task-${first.token}"))
                    assertSame(cancel, root.findViewWithTag<View>("home-cancel-${first.token}"))
                    assertSame(progress, root.findViewWithTag<View>("home-task-progress-${first.token}"))
                    assertTrue(cancel.hasFocus())
                    assertTrue(cancel.isPressed)
                    assertEquals(index * 10, progress.progress)
                }
                // Adding or moving another row preserves the first task's widgets too.
                render.invoke(activity, listOf(second, first))
                val tasks = requireNotNull(root.findViewWithTag<LinearLayout>("home-tasks"))
                assertSame(row, tasks.getChildAt(1))
                render.invoke(activity, listOf(first, second))
                assertSame(row, tasks.getChildAt(0))
                render.invoke(activity, listOf(first))
                assertSame(row, tasks.getChildAt(0))
                assertEquals(1, tasks.childCount)
                assertNull(root.findViewWithTag<View>("home-task-${second.token}"))
            }
        }
    }

    @Test fun confirmationUsesThePluginPromptAndPageRebuildDropsOldWidgetReferences() {
        unlocked()
        val first = snapshot()
        val metadata = InstallPresentation.Metadata("Home view fixture", "example.home.fixture", "1", 1,
            null, 0, false, 1, 24, 35, "", null, "apk", emptyList(), emptyList())
        val prompt = InstallPresentation.Prompt(0, metadata,
            InstallChoices(InstallOptions(), listOf(InstallChoices.Apk("fixture.apk", true)), false), emptyList())
        val confirming = first.copy(state = first.state.copy(stage = InstallerContract.STAGE_CONFIRMING, prompt = prompt))
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.window.decorView
                assertNotNull(root.findViewWithTag<View>("home-root"))
                assertNotNull(root.findViewWithTag<View>("home-pick"))
                render.invoke(activity, listOf(first))
                val oldRow = requireNotNull(root.findViewWithTag<View>("home-task-${first.token}"))
                render.invoke(activity, listOf(confirming))
                val summary = requireNotNull(root.findViewWithTag<TextView>("home-task-stage-${first.token}"))
                assertEquals(activity.getString(R.string.home_queue_progress, 1, 1,
                    activity.getString(R.string.notification_action_required)), summary.text.toString())
                assertTrue(requireNotNull(root.findViewWithTag<ProgressBar>("home-task-progress-${first.token}")).isIndeterminate)
                HomeActivity::class.java.getDeclaredMethod("onAppearanceChanged").apply { isAccessible = true }.invoke(activity)
                assertNull(root.findViewWithTag<View>("home-task-${first.token}"))
                render.invoke(activity, listOf(first))
                val newRow = requireNotNull(root.findViewWithTag<View>("home-task-${first.token}"))
                assertNotSame(oldRow, newRow)
                assertSame(root.findViewWithTag<LinearLayout>("home-tasks"), newRow.parent)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val root = activity.window.decorView
                assertNotNull(root.findViewWithTag<View>("home-root"))
                assertNotNull(root.findViewWithTag<View>("home-more"))
                assertNotNull(root.findViewWithTag<View>("home-history"))
                assertNotNull(root.findViewWithTag<View>("home-pick"))
                assertNull(root.findViewWithTag<View>("home-task-${first.token}"))
            }
        }
    }

    @Test fun authorizerCardsRenderAvailabilityAndPermissionWithoutRequestingAccess() {
        unlocked()
        data class Case(val state: AuthorizerState, val running: Boolean, val buttonLabel: Int, val enabled: Boolean)
        val unavailableRoot = AuthorizerState(Authorizer.ROOT, available = false, running = true, granted = false)
        val cases = listOf(
            Case(unavailableRoot, false, R.string.home_authorize, false),
            Case(AuthorizerState(Authorizer.ROOT, true, true, false), true, R.string.home_authorize, true),
            Case(AuthorizerState(Authorizer.ROOT, true, true, true), true, R.string.home_authorized, false),
            Case(AuthorizerState(Authorizer.SHIZUKU, false, false, false), false, R.string.home_get_shizuku, true),
            Case(AuthorizerState(Authorizer.SHIZUKU, true, false, false), false, R.string.home_authorize, false),
            Case(AuthorizerState(Authorizer.SHIZUKU, true, true, false), true, R.string.home_authorize, true),
            Case(AuthorizerState(Authorizer.SHIZUKU, true, true, true), true, R.string.home_authorized, false),
        )
        val states = HomeActivity::class.java.getDeclaredField("states").apply { isAccessible = true }
        val renderAuthorizers = HomeActivity::class.java.getDeclaredMethod("renderAuthorizers").apply { isAccessible = true }
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val original = states.get(activity)
                try {
                    cases.forEach { case ->
                        states.set(activity, mapOf(case.state.authorizer to case.state))
                        renderAuthorizers.invoke(activity)
                        fun yes(value: Boolean) = activity.getString(if (value) R.string.home_yes else R.string.home_no)
                        val id = case.state.authorizer.id
                        val root = activity.window.decorView
                        val status = requireNotNull(root.findViewWithTag<TextView>("home-authorizer-state-$id"))
                        val button = requireNotNull(root.findViewWithTag<TextView>("home-authorize-$id"))
                        assertEquals(activity.getString(R.string.home_authorizer_state,
                            yes(case.state.available), yes(case.running), yes(case.state.granted)), status.text.toString())
                        assertEquals(activity.getString(case.buttonLabel), button.text.toString())
                        assertEquals(case.enabled, button.isEnabled)
                    }
                    // The display fix must not redefine the authorizer's public running value.
                    assertTrue(unavailableRoot.running)
                } finally {
                    states.set(activity, original)
                    renderAuthorizers.invoke(activity)
                }
            }
        }
    }

    private fun snapshot(): InstallQueue.Snapshot = InstallQueue.Snapshot(UUID.randomUUID().toString(),
        InstallerContract.SOURCE_HOME, System.currentTimeMillis(),
        InstallPresentation.Snapshot(1, InstallerContract.STAGE_WRITING, 0, 0f,
            listOf(InstallPresentation.Item("Home view fixture.apk", InstallerContract.STAGE_WRITING)),
            null, false, null, false, false))

    private fun unlocked() = assumeFalse("Unlock the device before Home UI tests",
        context.getSystemService(KeyguardManager::class.java).isKeyguardLocked)
}
