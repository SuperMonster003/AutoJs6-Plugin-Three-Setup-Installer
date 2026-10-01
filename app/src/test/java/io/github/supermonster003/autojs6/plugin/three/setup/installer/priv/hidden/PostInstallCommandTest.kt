package io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

class PostInstallCommandTest {
    private val pkg = "org.autojs.fixture.dexopt"
    private val job = "01234567-89ab-cdef-0123-456789abcdef"
    private fun jobLine(value: String = job) = "Job running. To cancel it, run 'pm art cancel $value' in a separate shell.\n"

    @Test fun commandVocabularyCannotSelectGlobalOrDestructiveWork() {
        assertEquals(listOf("/system/bin/cmd", "package", "compile", "-m", "speed", pkg), PostInstallCommand.arguments(pkg, "speed", 24))
        assertEquals(listOf("/system/bin/cmd", "package", "compile", "-v", "--primary-dex", "-m", "verify", pkg), PostInstallCommand.arguments(pkg, "verify", 34))
        PostInstallCommand.arguments(pkg, "verify", 26)
        for (filter in listOf("verify", "verify-at-runtime", "interpret-only", "speed;id", "-a", "none")) {
            assertThrows(IllegalArgumentException::class.java) { PostInstallCommand.arguments(pkg, filter, 24) }
        }
        assertThrows(IllegalArgumentException::class.java) { PostInstallCommand.arguments("$pkg;id", "speed", 34) }
        assertThrows(IllegalArgumentException::class.java) { PostInstallCommand.cancelArguments("$job --all") }
    }

    @Test fun artFailureAndCancellationAreNotSuccessEvenWithExitZero() {
        for ((status, expected) in listOf("PERFORMED" to "accepted", "SKIPPED" to "accepted", "FAILED" to "failed", "CANCELLED" to "cancelled")) {
            val process = FakeProcess(jobLine() + "Final Status: $status\n", 0)
            val result = PostInstallCommand({ process }).run(pkg, "speed", 34, 1000) { false }
            assertEquals(status, expected, result.status)
            assertEquals(0, result.exitCode)
            assertTrue(process.closed())
        }
    }

    @Test fun legacySuccessDoesNotAssertThatCompilationWasPerformed() {
        val accepted = PostInstallCommand({ FakeProcess("Success\n", 0) }).run(pkg, "speed-profile", 24, 1000) { false }
        assertEquals("accepted", accepted.status)
        assertTrue(accepted.message.contains("skipped"))
        assertEquals("unknown", PostInstallCommand({ FakeProcess("Success\n", 0) }).run(pkg, "speed", 34, 1000) { false }.status)
        assertEquals("unavailable", PostInstallCommand({ FakeProcess("Error: Unknown option: -v\n", 1) }).run(pkg, "speed", 34, 1000) { false }.status)
        assertEquals("failed", PostInstallCommand({ FakeProcess("Failure\n", 1) }).run(pkg, "speed", 24, 1000) { false }.status)
    }

    @Test fun timeoutOnlyCancelsTheOneJobObservedOnItsOwnPipe() {
        var now = 0L
        val commands = arrayListOf<List<String>>()
        val compiling = FakeProcess(jobLine(), null)
        val cancelling = FakeProcess("Job cancelled\n", 0)
        val command = PostInstallCommand({ args -> commands += args; if (commands.size == 1) compiling else cancelling }, { now }, { now += it })
        assertEquals("timeout", command.run(pkg, "speed", 34, 50) { false }.status)
        assertEquals(2, commands.size)
        assertEquals(PostInstallCommand.cancelArguments(job), commands[1])
        assertTrue(compiling.closed()); assertTrue(cancelling.closed())
    }

    @Test fun ambiguousOrAbsentJobDoesNotCancelAnotherFrameworkJob() {
        for (text in listOf("", jobLine() + jobLine(), jobLine("../other"))) {
            var now = 0L
            var launches = 0
            val process = FakeProcess(text, null)
            val command = PostInstallCommand({ launches++; process }, { now }, { now += it })
            assertEquals("timeout", command.run(pkg, "speed", 34, 30) { false }.status)
            assertEquals(1, launches)
            assertTrue(process.closed())
        }
    }

    @Test fun cancellationAndExpiredBudgetDoNotStartAProcess() {
        val command = PostInstallCommand({ error("No process should start") })
        assertEquals("cancelled", command.run(pkg, "speed", 34, 1000) { true }.status)
        assertEquals("timeout", command.run(pkg, "speed", 34, 0) { false }.status)
        assertEquals("unavailable", PostInstallCommand({ throw IOException("No command entry") }).run(pkg, "speed", 34, 1000) { false }.status)
    }

    @Test fun outputIsBoundedButTheFinalStatusIsReadAfterTheProcessExits() {
        val process = FakeProcess("detail\n".repeat(20_000) + "Final Status: SKIPPED\n", 0)
        assertEquals("accepted", PostInstallCommand({ process }).run(pkg, "speed", 34, 5000) { false }.status)
        val ambiguous = FakeProcess("Final Status: PERFORMED\nFinal Status: FAILED\n", 0)
        assertEquals("unknown", PostInstallCommand({ ambiguous }).run(pkg, "speed", 34, 1000) { false }.status)
    }

    private class TrackedInput(text: String) : ByteArrayInputStream(text.toByteArray()) {
        var wasClosed = false
        override fun close() { wasClosed = true; super.close() }
    }
    private class TrackedOutput : ByteArrayOutputStream() {
        var wasClosed = false
        override fun close() { wasClosed = true; super.close() }
    }
    private class FakeProcess(text: String, private var code: Int?) : Process() {
        private val input = TrackedInput(text)
        private val error = TrackedInput("")
        private val output = TrackedOutput()
        var destroyed = false
        override fun getInputStream() = input
        override fun getErrorStream() = error
        override fun getOutputStream() = output
        override fun waitFor(): Int = exitValue()
        override fun exitValue(): Int = code ?: throw IllegalThreadStateException()
        override fun destroy() { destroyed = true; code = code ?: 143 }
        fun closed() = destroyed && input.wasClosed && error.wasClosed && output.wasClosed
    }
}
