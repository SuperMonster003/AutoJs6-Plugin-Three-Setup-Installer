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
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class DhizukuAuthorizerTest {
    @Test fun absentStoppedAndAlreadyGrantedStatesNeverStartPermissionUi() {
        val access = FakeAccess()
        val authorizer = DhizukuAuthorizer(access)
        for (state in listOf(
            AuthorizerState(Authorizer.DHIZUKU, false, false, false, "not installed or unsupported API"),
            AuthorizerState(Authorizer.DHIZUKU, true, false, false, "no active owner"),
        )) {
            access.current = state
            assertEquals(state, authorizer.state())
            assertFalse(authorizer.request(500))
            val availableAlternative = AuthorizerState(Authorizer.NONE, true, true, true)
            assertEquals(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, assertThrows(InstallFailure::class.java) {
                AuthorizerResolver.resolve("dhizuku", mapOf(Authorizer.DHIZUKU to state, Authorizer.NONE to availableAlternative))
            }.code)
        }
        access.current = ready(granted = true)
        assertTrue(authorizer.request(500))
        assertEquals(0, access.requests.get())
    }

    @Test fun concurrentWaitersShareOnePromptAndOneTimeoutDoesNotRetireTheOtherWaiter() {
        val access = FakeAccess()
        val authorizer = DhizukuAuthorizer(access)
        val worker = Executors.newSingleThreadExecutor()
        try {
            val patient = worker.submit<Boolean> { authorizer.request(5_000) }
            val prompt = access.next()
            assertEquals(InstallerErrorCodes.TIMEOUT, assertThrows(InstallFailure::class.java) { authorizer.request(30) }.code)
            assertEquals(1, access.requests.get())
            assertEquals(0, prompt.closes.get())
            assertFalse(patient.isDone)
            access.current = ready(granted = true)
            prompt.result(true)
            assertTrue(patient.get(3, TimeUnit.SECONDS))
            assertTrue(prompt.closed.await(3, TimeUnit.SECONDS))
            assertEquals(1, prompt.closes.get())
        } finally {
            worker.shutdownNow()
            assertTrue(worker.awaitTermination(3, TimeUnit.SECONDS))
        }
    }

    @Test fun lastTimeoutRetiresTheRegistrationAndLateCallbacksCannotAuthorizeANewRequest() {
        val access = FakeAccess()
        val authorizer = DhizukuAuthorizer(access)
        val worker = Executors.newSingleThreadExecutor()
        try {
            assertEquals(InstallerErrorCodes.TIMEOUT, assertThrows(InstallFailure::class.java) { authorizer.request(30) }.code)
            val old = access.next()
            assertEquals(1, old.closes.get())
            val next = worker.submit<Boolean> { authorizer.request(5_000) }
            val fresh = access.next()
            old.result(true)
            assertThrows(TimeoutException::class.java) { next.get(40, TimeUnit.MILLISECONDS) }
            assertEquals(0, fresh.closes.get())
            access.current = ready(granted = true)
            fresh.result(true)
            assertTrue(next.get(3, TimeUnit.SECONDS))
            assertEquals(1, fresh.closes.get())
            assertEquals(2, access.requests.get())
        } finally {
            worker.shutdownNow()
            assertTrue(worker.awaitTermination(3, TimeUnit.SECONDS))
        }
    }

    @Test fun synchronousCompletionStillClosesItsRegistrationExactlyOnce() {
        val access = FakeAccess().apply {
            immediate = { prompt -> current = ready(granted = true); prompt.result(true) }
        }
        assertTrue(DhizukuAuthorizer(access).request(500))
        assertEquals(1, access.next().closes.get())
    }

    @Test fun firstDenialCannotBeOverwrittenByADuplicateGrantCallback() {
        val access = FakeAccess().apply {
            immediate = { prompt ->
                prompt.result(false)
                current = ready(granted = true)
                prompt.result(true)
            }
        }
        assertFalse(DhizukuAuthorizer(access).request(500))
        assertEquals(1, access.next().closes.get())
    }

    @Test fun aGrantCallbackDoesNotAuthorizeAStoppedOrStillDeniedOwner() {
        for (after in listOf(ready(granted = false), AuthorizerState(Authorizer.DHIZUKU, true, false, true))) {
            val access = FakeAccess().apply { immediate = { prompt -> current = after; prompt.result(true) } }
            assertFalse(DhizukuAuthorizer(access).request(500))
            assertEquals(1, access.next().closes.get())
        }
    }

    @Test fun anInterruptedLastWaiterUnregistersWithoutAuthorizingOrLeakingAPrompt() {
        val access = FakeAccess()
        val authorizer = DhizukuAuthorizer(access)
        val failure = AtomicReference<Throwable>()
        val caller = Thread {
            try { authorizer.request(5_000) } catch (error: Throwable) { failure.set(error) }
        }
        caller.start()
        try {
            val prompt = access.next()
            caller.interrupt()
            caller.join(3_000)
            assertFalse(caller.isAlive)
            assertTrue(failure.get() is InterruptedException)
            assertEquals(1, prompt.closes.get())
            prompt.result(true)
            assertFalse(authorizer.state().usable)
        } finally {
            caller.interrupt()
            caller.join(3_000)
        }
    }

    @Test fun startupFailureRetiresTheRequestAndAllowsAFreshPrompt() {
        val access = FakeAccess()
        val authorizer = DhizukuAuthorizer(access)
        val unavailable = IllegalStateException("owner binder unavailable")
        access.startFailure = unavailable
        assertSame(unavailable, assertThrows(IllegalStateException::class.java) { authorizer.request(500) })
        access.startFailure = null
        access.immediate = { it.result(false) }
        assertFalse(authorizer.request(500))
        assertEquals(2, access.requests.get())
        assertEquals(1, access.next().closes.get())
    }

    @Test fun invalidTimeoutsDoNotReadStateOrRequestPermission() {
        val access = FakeAccess()
        val authorizer = DhizukuAuthorizer(access)
        for (timeout in listOf(0L, -1L)) assertThrows(IllegalArgumentException::class.java) { authorizer.request(timeout) }
        assertEquals(0, access.reads.get())
        assertEquals(0, access.requests.get())
    }

    private class Prompt(val result: (Boolean) -> Unit) {
        val closes = AtomicInteger()
        val closed = CountDownLatch(1)
        fun close() { closes.incrementAndGet(); closed.countDown() }
    }

    private class FakeAccess : DhizukuAccess {
        @Volatile var current = ready(granted = false)
        var immediate: ((Prompt) -> Unit)? = null
        var startFailure: RuntimeException? = null
        val reads = AtomicInteger()
        val requests = AtomicInteger()
        private val prompts = LinkedBlockingQueue<Prompt>()
        override fun state(): AuthorizerState { reads.incrementAndGet(); return current }
        override fun requestPermission(result: (Boolean) -> Unit): Closeable {
            requests.incrementAndGet()
            startFailure?.let { throw it }
            val prompt = Prompt(result)
            prompts.add(prompt)
            immediate?.invoke(prompt)
            return Closeable { prompt.close() }
        }
        fun next(): Prompt = requireNotNull(prompts.poll(3, TimeUnit.SECONDS)) { "No permission registration appeared" }
    }

    private companion object {
        fun ready(granted: Boolean) = AuthorizerState(Authorizer.DHIZUKU, true, true, granted)
    }
}
