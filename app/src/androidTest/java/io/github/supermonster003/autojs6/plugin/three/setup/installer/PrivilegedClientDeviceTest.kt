package io.github.supermonster003.autojs6.plugin.three.setup.installer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.PrivilegedClient
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PrivilegedClientDeviceTest {
    @Test fun immediateReacquisitionDoesNotSubscribeToARetiringShizukuService() {
        val authorizer = Authorizer.fromId(InstrumentationRegistry.getArguments().getString("engineAuthorizer"))
        assumeTrue(authorizer == Authorizer.SHIZUKU)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(AuthorizerStates.request(context, Authorizer.SHIZUKU, 30_000))
        val client = PrivilegedClient.get(context)
        val stopped = CountDownLatch(8)
        var previous: android.os.IBinder? = null
        try {
            repeat(8) {
                val current = client.acquire(Authorizer.SHIZUKU).asBinder()
                assertTrue(current.isBinderAlive)
                assertNotSame(previous, current)
                current.linkToDeath({ stopped.countDown() }, 0)
                previous = current
                // Deliberately do not wait for the previous process's death before acquiring again.
                client.releaseAll()
            }
            assertTrue("Every retired UserService must exit", stopped.await(15, TimeUnit.SECONDS))
        } finally {
            client.releaseAll()
        }
    }

    @Test fun fourConcurrentAcquisitionsShareOneServiceAndRebindAfterShutdown() {
        val authorizer = Authorizer.fromId(InstrumentationRegistry.getArguments().getString("engineAuthorizer"))
        assumeTrue(authorizer?.privileged == true)
        val selected = requireNotNull(authorizer)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(AuthorizerStates.request(context, selected, 30_000))
        val client = PrivilegedClient.get(context)
        val workers = Executors.newFixedThreadPool(4)
        val start = CountDownLatch(1)
        var last: android.os.IBinder? = null
        try {
            val futures = (0..3).map { workers.submit<android.os.IBinder> { start.await(); client.acquire(selected).asBinder() } }
            start.countDown()
            val binders = futures.map { it.get(35, TimeUnit.SECONDS) }
            last = binders.first()
            assertTrue(binders.all { it === last })
            val stopped = CountDownLatch(1)
            last.linkToDeath({ stopped.countDown() }, 0)
            client.releaseAll()
            assertTrue(stopped.await(10, TimeUnit.SECONDS))
            val previous = last
            last = client.acquire(selected).asBinder()
            assertNotSame(previous, last)
            assertTrue(last.isBinderAlive)
        } finally {
            workers.shutdownNow()
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS))
            val stopped = CountDownLatch(1)
            last?.let { runCatching { it.linkToDeath({ stopped.countDown() }, 0) }.onFailure { stopped.countDown() } }
            client.releaseAll()
            if (last != null) assertTrue(stopped.await(10, TimeUnit.SECONDS))
        }
    }
}
