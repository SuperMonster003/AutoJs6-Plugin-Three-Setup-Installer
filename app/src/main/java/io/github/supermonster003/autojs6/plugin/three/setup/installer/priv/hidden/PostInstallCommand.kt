package io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden

import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.PrivilegedOptions
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Fixed argv only. No shell parsing, global compilation, profile deletion, or mutation retry. */
internal class PostInstallCommand(
    private val start: (List<String>) -> Process = { ProcessBuilder(it).redirectErrorStream(true).start() },
    private val clock: () -> Long = { TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) },
    private val sleep: (Long) -> Unit = Thread::sleep,
) {
    data class Result(val status: String, val message: String, val exitCode: Int? = null)

    fun run(packageName: String, filter: String, sdk: Int, timeoutMillis: Long, cancelled: () -> Boolean): Result {
        val command = arguments(packageName, filter, sdk)
        require(timeoutMillis in 0..PrivilegedOptions.MAX_POST_INSTALL_TIMEOUT)
        if (cancelled() || Thread.currentThread().isInterrupted) return Result("cancelled", "Compilation was cancelled before it started")
        if (timeoutMillis == 0L) return Result("timeout", "No time remained to start compilation")
        val deadline = clock() + timeoutMillis
        val output = Output()
        var process: Process? = null
        var stopped = false
        try {
            process = start(command)
            while (true) {
                drain(process, output)
                if (cancelled() || Thread.currentThread().isInterrupted) {
                    stopped = true
                    stop(process, output, sdk)
                    return Result("cancelled", "Compilation was cancelled; its final framework outcome may be unverified")
                }
                val exitCode = exitCode(process)
                if (exitCode != null) {
                    while (process.inputStream.available() > 0) {
                        if (clock() >= deadline) return Result("unknown", "The final compilation output could not be completely observed", exitCode)
                        drain(process, output)
                    }
                    output.finish()
                    return output.result(exitCode, sdk)
                }
                if (clock() >= deadline) {
                    stopped = true
                    stop(process, output, sdk)
                    return Result("timeout", "Compilation timed out; its final framework outcome may be unverified")
                }
                sleep(25)
            }
        } catch (_: InterruptedException) {
            if (process != null && !stopped) { stopped = true; stop(process, output, sdk) }
            Thread.currentThread().interrupt()
            return Result("cancelled", "Compilation was interrupted; its final framework outcome may be unverified")
        } catch (failure: IOException) {
            return Result(if (process == null) "unavailable" else "unknown", "Compilation command could not be ${if (process == null) "started" else "observed"}: ${failure.message.orEmpty().take(200)}")
        } catch (failure: SecurityException) {
            return Result("unavailable", "Compilation was denied by the platform: ${failure.message.orEmpty().take(200)}")
        } finally {
            if (process != null) {
                try { if (!stopped && exitCode(process) == null) stop(process, output, sdk) }
                finally { close(process) }
            }
        }
    }

    private fun drain(process: Process, output: Output) {
        val input = process.inputStream
        val buffer = ByteArray(1024)
        // available()+bounded reads avoid an extra reader thread stuck after a Binder shell dies.
        repeat(16) {
            val available = input.available()
            if (available <= 0) return
            val count = input.read(buffer, 0, minOf(available, buffer.size))
            if (count <= 0) return
            output.add(buffer, count)
        }
    }

    private fun stop(process: Process, output: Output, sdk: Int) {
        val interrupted = Thread.interrupted()
        try {
            runCatching { drain(process, output) }
            // Only the unique UUID printed to this command's private output can name a job.
            // A missing/ambiguous result never falls back to cancelling global dexopt work.
            val job = if (sdk >= 34) output.jobId() else null
            if (job != null) runCatching {
                val cancellation = start(cancelArguments(job))
                try {
                    val deadline = clock() + 1_000
                    val reply = Output()
                    while (exitCode(cancellation) == null && clock() < deadline) {
                        drain(cancellation, reply)
                        sleep(10)
                    }
                } finally { close(cancellation) }
            }
        } finally {
            runCatching { process.destroy() }
            if (interrupted) Thread.currentThread().interrupt()
        }
    }

    private fun close(process: Process) {
        runCatching { process.destroy() }
        runCatching { process.outputStream.close() }
        runCatching { process.inputStream.close() }
        runCatching { process.errorStream.close() }
    }

    private fun exitCode(process: Process): Int? = try { process.exitValue() } catch (_: IllegalThreadStateException) { null }

    internal class Output {
        private val bytes = ByteArrayOutputStream()
        private val line = ByteArrayOutputStream()
        private val statuses = arrayListOf<String>()
        private val jobs = arrayListOf<String>()
        private var overlong = false
        private var malformed = false
        private var statusOverflow = false

        fun add(value: ByteArray, count: Int = value.size) {
            val remaining = 64 * 1024 - bytes.size()
            if (remaining > 0) bytes.write(value, 0, minOf(count, remaining))
            for (index in 0 until count) {
                val current = value[index].toInt() and 0xff
                if (current == 10) finishLine()
                else if (line.size() < 4096 && !overlong) line.write(current)
                else { overlong = true; malformed = true }
            }
        }
        fun finish() { if (line.size() > 0 || overlong) finishLine() }
        private fun finishLine() {
            if (!overlong) {
                val value = line.toString(Charsets.UTF_8.name()).removeSuffix("\r")
                FINAL.matchEntire(value)?.let {
                    if (statuses.size < 2) statuses += it.groupValues[1] else statusOverflow = true
                }
                JOB.matchEntire(value)?.let {
                    if (jobs.size < 2) jobs += it.groupValues[1]
                }
            }
            line.reset(); overlong = false
        }
        fun jobId(): String? = jobs.singleOrNull()
        fun result(exitCode: Int, sdk: Int): Result {
            val text = bytes.toString(Charsets.UTF_8.name()).trim()
            val lower = text.lowercase(Locale.ROOT)
            if (listOf("unknown command", "unknown option", "not a valid compilation filter", "permission denial", "securityexception", "need root or shell").any(lower::contains)) {
                return Result("unavailable", "The platform or authorization does not support this compilation request", exitCode)
            }
            if (sdk >= 34 && !malformed && !statusOverflow && statuses.size == 1) {
                val status = statuses.single()
                return when (status) {
                    "PERFORMED", "SKIPPED" -> if (exitCode == 0) Result("accepted", "ART reported $status", exitCode)
                        else Result("unknown", "ART reported $status but the command exited with $exitCode", exitCode)
                    "FAILED" -> Result("failed", "ART reported FAILED", exitCode)
                    "CANCELLED" -> Result("cancelled", "ART reported CANCELLED", exitCode)
                    else -> Result("unknown", "ART returned an unrecognized compilation status", exitCode)
                }
            }
            if (exitCode != 0) return Result("failed", "Compilation command exited with $exitCode", exitCode)
            if (sdk < 34 && !malformed && text == "Success") return Result("accepted", "The platform reported Success; this can include skipped compilation", exitCode)
            return Result("unknown", "The compilation command returned no unambiguous final result", exitCode)
        }
    }

    companion object {
        private val UUID = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        private val FINAL = Regex("Final Status: ([A-Z_]+)")
        private val JOB = Regex("Job running\\. To cancel it, run 'pm art cancel (${UUID.pattern})' in a separate shell\\.")
        fun arguments(packageName: String, filter: String, sdk: Int): List<String> {
            PrivilegedOptions.validatePackage(packageName)
            PrivilegedOptions.validateDexopt(filter, sdk)
            require(filter != "none")
            return listOf("/system/bin/cmd", "package", "compile") +
                (if (sdk >= 34) listOf("-v", "--primary-dex") else emptyList()) + listOf("-m", filter, packageName)
        }
        fun cancelArguments(jobId: String): List<String> {
            require(UUID.matches(jobId))
            return listOf("/system/bin/cmd", "package", "art", "cancel", jobId)
        }
    }
}
