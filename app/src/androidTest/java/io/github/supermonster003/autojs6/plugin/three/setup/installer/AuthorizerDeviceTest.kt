package io.github.supermonster003.autojs6.plugin.three.setup.installer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.topjohnwu.superuser.Shell
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerResolver
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerState
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.PrivilegedClient
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import rikka.shizuku.Shizuku
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Authorization-only tests. They neither install packages nor change any default handler. */
@RunWith(AndroidJUnit4::class)
class AuthorizerDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun noneIsAlwaysGrantedAndStateQueriesDoNotCreateARootShell() {
        val shell = Shell.getCachedShell()
        repeat(3) {
            assertEquals(AuthorizerState(Authorizer.NONE, true, true, true), AuthorizerStates.state(context, Authorizer.NONE))
            assertTrue(AuthorizerStates.request(context, Authorizer.NONE, 1))
            AuthorizerStates.states(context)
        }
        assertSame(shell, Shell.getCachedShell())
    }

    @Test fun selectedPrivilegedAuthorizerStateRequestAndBindingRoundTrip() {
        val args = InstrumentationRegistry.getArguments()
        val selected = Authorizer.fromId(args.getString("authorizer") ?: args.getString("engineAuthorizer"))
        assumeTrue(selected?.privileged == true)
        val authorizer = requireNotNull(selected)
        val before = AuthorizerStates.state(context, authorizer)
        assertTrue(before.reason, before.available)
        assertTrue(before.reason, before.running)
        assertTrue(AuthorizerStates.request(context, authorizer, 30_000))
        val after = AuthorizerStates.state(context, authorizer)
        assertTrue(after.reason, after.usable)
        assertNull(after.reason)
        assertEquals(authorizer, AuthorizerResolver.resolve(authorizer.id, AuthorizerStates.states(context), enabled = emptySet()))
        if (authorizer == Authorizer.SHIZUKU) {
            assertFalse(Shizuku.isPreV11())
            args.getString("expectedShizukuUid")?.toInt()?.let { assertEquals(it, Shizuku.getUid()) }
        }

        val client = PrivilegedClient.get(context)
        val workers = Executors.newFixedThreadPool(2)
        var binder: android.os.IBinder? = null
        try {
            // An explicit permission request may overlap service launch. Both must finish and the
            // authorization probe must not close the shell that launches the RootService.
            val request = workers.submit<Boolean> { AuthorizerStates.request(context, authorizer, 30_000) }
            val binding = workers.submit<android.os.IBinder> { client.acquire(authorizer).asBinder() }
            assertTrue(request.get(35, TimeUnit.SECONDS))
            binder = binding.get(35, TimeUnit.SECONDS)
            assertTrue(binder.isBinderAlive)
            assertSame(binder, client.acquire(authorizer).asBinder())
            if (authorizer == Authorizer.ROOT) {
                // ServiceConnected can arrive before its launch task has returned. A final probe
                // passes through the same serial queue, so its completion is the cleanup barrier.
                assertTrue(AuthorizerStates.request(context, authorizer, 30_000))
                assertNull("No authorization/launch shell remains open", Shell.getCachedShell())
            }
        } finally {
            workers.shutdownNow()
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS))
            val stopped = CountDownLatch(1)
            binder?.let { runCatching { it.linkToDeath({ stopped.countDown() }, 0) }.onFailure { stopped.countDown() } }
            client.releaseAll()
            if (binder != null) assertTrue(stopped.await(10, TimeUnit.SECONDS))
        }
    }
}
