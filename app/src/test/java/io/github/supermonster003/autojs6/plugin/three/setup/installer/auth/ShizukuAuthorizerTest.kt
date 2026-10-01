package io.github.supermonster003.autojs6.plugin.three.setup.installer.auth

import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.*
import org.junit.Test
import java.io.Closeable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ShizukuAuthorizerTest {
    @Test fun `state distinguishes absent stopped incompatible denied and granted without requesting`() = fixture { f ->
        f.access.installed = false
        assertFalse(f.authorizer.state().available)
        f.access.installed = true
        f.access.running = false
        assertEquals(AuthorizerState(Authorizer.SHIZUKU, true, false, false, "Shizuku is not running"), f.authorizer.state())
        f.access.running = true
        f.access.preV11 = true
        val incompatible = f.authorizer.state()
        assertFalse(incompatible.available)
        assertTrue(incompatible.running)
        assertTrue(incompatible.reason!!.contains("API 11"))
        assertFalse(f.authorizer.request(1_000))
        val failure = assertThrows(InstallFailure::class.java) {
            AuthorizerResolver.resolve("shizuku", mapOf(Authorizer.SHIZUKU to incompatible))
        }
        assertEquals(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, failure.code)
        assertTrue(failure.message!!.contains("API 11"))
        f.access.preV11 = false
        assertFalse(f.authorizer.state().granted)
        f.access.granted = true
        assertTrue(f.authorizer.state().usable)
        assertTrue(f.authorizer.request(1_000))
        assertEquals(0, f.access.requestCount.get())
    }

    @Test fun `a permanent denial does not show another permission dialog`() = fixture { f ->
        f.access.rationale = true
        assertFalse(f.authorizer.request(1_000))
        assertEquals(0, f.access.requestCount.get())
    }

    @Test fun `one waiter timing out preserves the other permission request and ignores unrelated replies`() = fixture { f ->
        val patient = f.workers.submit<Boolean> { f.authorizer.request(5_000) }
        val prompt = f.access.next()
        assertEquals(InstallerErrorCodes.TIMEOUT, assertThrows(InstallFailure::class.java) { f.authorizer.request(20) }.code)
        assertEquals(1, f.access.requestCount.get())
        assertEquals(1L, prompt.closed.count)
        prompt.result(prompt.code + 1, true)
        assertFalse(patient.isDone)
        f.access.granted = true
        prompt.result(prompt.code, true)
        assertTrue(patient.get(3, TimeUnit.SECONDS))
        assertTrue(prompt.closed.await(3, TimeUnit.SECONDS))
    }

    @Test fun `last timeout unregisters and a stale permission result cannot complete a later request`() = fixture { f ->
        assertEquals(InstallerErrorCodes.TIMEOUT, assertThrows(InstallFailure::class.java) { f.authorizer.request(20) }.code)
        val old = f.access.next()
        assertTrue(old.closed.await(3, TimeUnit.SECONDS))
        val next = f.workers.submit<Boolean> { f.authorizer.request(5_000) }
        val fresh = f.access.next()
        assertNotEquals(old.code, fresh.code)
        old.result(old.code, true)
        fresh.result(old.code, true)
        assertFalse(next.isDone)
        f.access.granted = true
        fresh.result(fresh.code, true)
        assertTrue(next.get(3, TimeUnit.SECONDS))
    }

    @Test fun `last interrupted waiter removes the permission result listener`() = fixture { f ->
        val interrupted = CountDownLatch(1)
        val caller = Thread {
            try { f.authorizer.request(5_000) }
            catch (_: InterruptedException) { interrupted.countDown() }
        }.apply { start() }
        val prompt = f.access.next()
        caller.interrupt()
        assertTrue(interrupted.await(3, TimeUnit.SECONDS))
        assertTrue(prompt.closed.await(3, TimeUnit.SECONDS))
        caller.join(3_000)
        assertFalse(caller.isAlive)
    }

    @Test fun `binder death releases pending permission callers and invalidates the cached service`() = fixture { f ->
        val call = f.workers.submit<Boolean> { f.authorizer.request(5_000) }
        val prompt = f.access.next()
        f.access.running = false
        f.access.changed!!()
        assertFalse(call.get(3, TimeUnit.SECONDS))
        assertTrue(prompt.closed.await(3, TimeUnit.SECONDS))
        assertEquals(1, f.invalidations.get())
        assertFalse(f.authorizer.state().running)
        f.access.running = true
        f.access.changed!!()
        assertEquals(1, f.invalidations.get())
        assertTrue(f.authorizer.state().running)
    }

    @Test fun `duplicate received callbacks preserve an active request and its binding`() = fixture { f ->
        val call = f.workers.submit<Boolean> { f.authorizer.request(5_000) }
        val prompt = f.access.next()
        repeat(3) { f.access.changed!!() }
        assertEquals(0, f.invalidations.get())
        assertEquals(1L, prompt.closed.count)
        assertFalse(call.isDone)
        f.access.granted = true
        prompt.result(prompt.code, true)
        assertTrue(call.get(3, TimeUnit.SECONDS))
    }

    @Test fun `late initial received callback preserves work started with the first server`() {
        val access = FakeAccess().apply { identity = null }
        val invalidations = AtomicInteger()
        ShizukuAuthorizer(access) { invalidations.incrementAndGet() }.use { authorizer ->
            access.identity = Any()
            access.granted = true
            assertTrue(authorizer.request(1_000))
            access.changed!!()
            assertEquals(0, invalidations.get())
            access.identity = Any()
            access.changed!!()
            assertEquals(1, invalidations.get())
        }
    }

    @Test fun `a replacement binder releases a pending request even if no dead callback arrived`() = fixture { f ->
        val call = f.workers.submit<Boolean> { f.authorizer.request(5_000) }
        val prompt = f.access.next()
        f.access.identity = Any()
        f.access.changed!!()
        assertFalse(call.get(3, TimeUnit.SECONDS))
        assertTrue(prompt.closed.await(3, TimeUnit.SECONDS))
        assertEquals(1, f.invalidations.get())
    }

    @Test fun `synchronous permission completion also closes its listener`() = fixture { f ->
        f.access.immediate = true
        assertTrue(f.authorizer.request(1_000))
        assertTrue(f.access.next().closed.await(3, TimeUnit.SECONDS))
    }

    @Test fun `closing releases a pending caller and removes the binder observation`() = fixture { f ->
        val call = f.workers.submit<Boolean> { f.authorizer.request(5_000) }
        val prompt = f.access.next()
        f.authorizer.close()
        assertFalse(call.get(3, TimeUnit.SECONDS))
        assertTrue(prompt.closed.await(3, TimeUnit.SECONDS))
        assertNull(f.access.changed)
    }

    private fun fixture(block: (Fixture) -> Unit) {
        val f = Fixture()
        try { block(f) } finally {
            f.authorizer.close()
            f.workers.shutdownNow()
            assertTrue(f.workers.awaitTermination(3, TimeUnit.SECONDS))
        }
    }

    private class Fixture {
        val access = FakeAccess()
        val invalidations = AtomicInteger()
        val authorizer = ShizukuAuthorizer(access) { invalidations.incrementAndGet() }
        val workers = Executors.newFixedThreadPool(3)
    }

    private class Prompt(val code: Int, val result: (Int, Boolean) -> Unit) {
        val closed = CountDownLatch(1)
    }

    private class FakeAccess : ShizukuAccess {
        var installed = true
        @Volatile var running = true
        var preV11 = false
        @Volatile var granted = false
        var rationale = false
        var immediate = false
        var identity: Any? = Any()
        var changed: (() -> Unit)? = null
        val requestCount = AtomicInteger()
        val prompts = LinkedBlockingQueue<Prompt>()
        override fun isInstalled() = installed
        override fun isRunning() = running
        override fun isPreV11() = preV11
        override fun isGranted() = granted
        override fun shouldShowRationale() = rationale
        override fun binderIdentity(): Any? = if (running) identity else null
        override fun observeBinder(changed: () -> Unit): Closeable {
            this.changed = changed
            return Closeable { this.changed = null }
        }
        override fun requestPermission(code: Int, result: (Int, Boolean) -> Unit): Closeable {
            requestCount.incrementAndGet()
            val prompt = Prompt(code, result)
            prompts.offer(prompt)
            if (immediate) { granted = true; result(code, true) }
            return Closeable { prompt.closed.countDown() }
        }
        fun next(): Prompt = requireNotNull(prompts.poll(3, TimeUnit.SECONDS)) { "Permission request was not registered" }
    }
}
