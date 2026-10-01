package io.github.supermonster003.autojs6.plugin.three.setup.installer.auth

import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

class RootAuthorizerTest {
    @Test fun `state distinguishes missing su unknown grant and refusal without opening any shell`() {
        val access = FakeAccess()
        val authorizer = RootAuthorizer(access)
        access.binary = false
        assertFalse(authorizer.state().available)
        assertTrue(authorizer.state().running)
        assertFalse(authorizer.request(1_000))
        access.binary = true
        assertTrue(authorizer.state().available)
        assertFalse(authorizer.state().granted)
        access.permission = false
        val refused = authorizer.state()
        assertTrue(refused.reason!!.contains("denied"))
        assertEquals(InstallerErrorCodes.AUTHORIZER_DENIED, assertThrows(InstallFailure::class.java) {
            AuthorizerResolver.resolve("root", mapOf(Authorizer.ROOT to refused))
        }.code)
        access.binary = false
        assertEquals(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, assertThrows(InstallFailure::class.java) {
            AuthorizerResolver.resolve("root", mapOf(Authorizer.ROOT to authorizer.state()))
        }.code)
        access.permission = true
        assertTrue(authorizer.state().usable)
        assertEquals(0, access.requestCount.get())
    }

    @Test fun `a short caller does not cancel another callers root prompt`() = fixture { f ->
        val patient = f.workers.submit<Boolean> { f.authorizer.request(5_000) }
        val prompt = f.access.next()
        assertEquals(InstallerErrorCodes.TIMEOUT, assertThrows(InstallFailure::class.java) { f.authorizer.request(20) }.code)
        assertEquals(1, f.access.requestCount.get())
        assertFalse(prompt.isCancelled)
        f.access.permission = true
        prompt.complete(true)
        assertTrue(patient.get(3, TimeUnit.SECONDS))
        assertTrue(f.authorizer.state().granted)
    }

    @Test fun `last timeout cancels the prompt and the next call can retry`() = fixture { f ->
        assertEquals(InstallerErrorCodes.TIMEOUT, assertThrows(InstallFailure::class.java) { f.authorizer.request(20) }.code)
        assertTrue(f.access.next().isCancelled)
        val retry = f.workers.submit<Boolean> { f.authorizer.request(5_000) }
        val prompt = f.access.next()
        f.access.permission = false
        prompt.complete(false)
        assertFalse(retry.get(3, TimeUnit.SECONDS))
        assertTrue(f.authorizer.state().reason!!.contains("denied"))
    }

    @Test fun `interruption only cancels the root request after the last waiter leaves`() = fixture { f ->
        val patient = f.workers.submit<Boolean> { f.authorizer.request(5_000) }
        val prompt = f.access.next()
        val interrupted = CountDownLatch(1)
        val caller = Thread {
            try { f.authorizer.request(5_000) }
            catch (_: InterruptedException) { interrupted.countDown() }
        }.apply { start() }
        caller.interrupt()
        assertTrue(interrupted.await(3, TimeUnit.SECONDS))
        caller.join(3_000)
        assertFalse(prompt.isCancelled)
        prompt.complete(true)
        assertTrue(patient.get(3, TimeUnit.SECONDS))
    }

    @Test fun `root timeout is capped at ten seconds and preserves smaller caller budgets`() {
        for ((requested, expected) in listOf(60_000L to 10_000L, 57L to 57L)) {
            var observed = 0L
            val stalled = object : CompletableFuture<Boolean>() {
                override fun get(timeout: Long, unit: TimeUnit): Boolean {
                    observed = unit.toMillis(timeout)
                    throw TimeoutException()
                }
            }
            val access = object : RootAccess {
                override fun hasSuBinary() = true
                override fun granted(): Boolean? = null
                override fun request(): Future<Boolean> = stalled
            }
            assertEquals(InstallerErrorCodes.TIMEOUT, assertThrows(InstallFailure::class.java) {
                RootAuthorizer(access).request(requested)
            }.code)
            assertEquals(expected, observed)
            assertTrue(stalled.isCancelled)
        }
    }

    @Test fun `shell startup failure is unavailable rather than a permission refusal`() = fixture { f ->
        val call = f.workers.submit<Boolean> { f.authorizer.request(5_000) }
        f.access.next().completeExceptionally(IllegalStateException("cannot start su"))
        val wrapped = assertThrows(java.util.concurrent.ExecutionException::class.java) { call.get(3, TimeUnit.SECONDS) }
        assertEquals(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, (wrapped.cause as InstallFailure).code)
        assertTrue(wrapped.cause!!.message!!.contains("cannot start su"))
    }

    private fun fixture(block: (Fixture) -> Unit) {
        val f = Fixture()
        try { block(f) } finally {
            f.workers.shutdownNow()
            assertTrue(f.workers.awaitTermination(3, TimeUnit.SECONDS))
        }
    }
    private class Fixture {
        val access = FakeAccess()
        val authorizer = RootAuthorizer(access)
        val workers = Executors.newFixedThreadPool(3)
    }
    private class FakeAccess : RootAccess {
        var binary = true
        @Volatile var permission: Boolean? = null
        val requestCount = AtomicInteger()
        val prompts = LinkedBlockingQueue<CompletableFuture<Boolean>>()
        override fun hasSuBinary() = binary
        override fun granted() = permission
        override fun request(): Future<Boolean> {
            requestCount.incrementAndGet()
            return CompletableFuture<Boolean>().also { prompts.offer(it) }
        }
        fun next(): CompletableFuture<Boolean> = requireNotNull(prompts.poll(3, TimeUnit.SECONDS)) { "Root prompt was not started" }
    }
}
