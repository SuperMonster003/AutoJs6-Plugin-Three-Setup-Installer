package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.KeyguardManager
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.android.material.materialswitch.MaterialSwitch
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.UninstallRequest
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.ConfirmationActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.PluginConfirmation
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Only confirms a request draft; this test never calls an uninstall engine. */
@RunWith(AndroidJUnit4::class)
class UninstallConfirmationDeviceTest {
    @Test fun selectedKeepDataSurvivesActivityRecreationAndIsReturnedToTheCaller() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assumeFalse("Unlock the device to exercise visible confirmation", context.getSystemService(KeyguardManager::class.java).isKeyguardLocked)
        val original = UninstallRequest("example.confirmation.fixture", false, "10", "root", "dialog", 20_000)
        val ticket = PluginConfirmation.Ticket(original, Authorizer.ROOT, 10, "3-Setup uninstall confirmation fixture")
        val result = AtomicReference<UninstallRequest?>()
        val error = AtomicReference<Throwable?>()
        val done = CountDownLatch(1)
        val worker = Thread {
            try { result.set(PluginConfirmation.await(context, ticket, SystemClock.elapsedRealtime() + 20_000, {})) }
            catch (failure: Throwable) { error.set(failure) }
            finally { done.countDown() }
        }
        var first: ConfirmationActivity? = null
        var recreated: ConfirmationActivity? = null
        try {
            worker.start()
            first = awaitAttached(ticket)
            assertNotNull("The uninstall confirmation was not shown", first)
            val originalToken = requireNotNull(first).intent.getStringExtra(PluginConfirmation.EXTRA_TOKEN)
            assertNotNull(originalToken)
            instrumentation.runOnMainSync {
                val view = requireNotNull(requireNotNull(first).window.decorView.findViewWithTag<MaterialSwitch>(ConfirmationActivity.TAG_KEEP_DATA))
                assertFalse(view.isChecked)
                view.isChecked = true
                assertTrue(ticket.keepData)
                first?.recreate()
            }
            // ActivityMonitor can return an earlier onResume observation of the old instance.
            // Wait for the retained ticket to attach a different, resumed owner after destruction.
            recreated = awaitAttached(ticket, previous = requireNotNull(first))
            assertNotNull("The confirmation did not reattach after recreation", recreated)
            assertNotSame(first, recreated)
            instrumentation.runOnMainSync {
                assertTrue(requireNotNull(first).isDestroyed)
                assertSame(recreated, ticket.activity.get())
                assertEquals(originalToken, requireNotNull(recreated).intent.getStringExtra(PluginConfirmation.EXTRA_TOKEN))
                val decor = requireNotNull(recreated).window.decorView
                assertTrue(requireNotNull(decor.findViewWithTag<MaterialSwitch>(ConfirmationActivity.TAG_KEEP_DATA)).isChecked)
                requireNotNull(decor.findViewById<android.view.View>(android.R.id.button1)).performClick()
            }
            assertTrue(done.await(5, TimeUnit.SECONDS))
            error.get()?.let { throw AssertionError("Uninstall confirmation failed", it) }
            assertEquals(original.copy(keepData = true), result.get())
            assertFalse(original.keepData)
        } finally {
            ticket.answer(false)
            if (worker.isAlive) { worker.interrupt(); worker.join(5_000) }
            instrumentation.runOnMainSync { first?.finish(); recreated?.finish(); ticket.activity.get()?.finish() }
        }
    }

    private fun awaitAttached(ticket: PluginConfirmation.Ticket, previous: ConfirmationActivity? = null): ConfirmationActivity? {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = SystemClock.elapsedRealtime() + 8_000
        while (SystemClock.elapsedRealtime() < deadline) {
            var attached: ConfirmationActivity? = null
            instrumentation.runOnMainSync {
                val owner = ticket.activity.get()
                if (owner != null && owner !== previous && !owner.isFinishing && !owner.isDestroyed &&
                    (previous == null || previous.isDestroyed) &&
                    owner in ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED) &&
                    owner.window.decorView.findViewWithTag<MaterialSwitch>(ConfirmationActivity.TAG_KEEP_DATA) != null) {
                    attached = owner
                }
            }
            attached?.let { return it }
            if (!ticket.isPending) return null
            SystemClock.sleep(25)
        }
        return null
    }
}
