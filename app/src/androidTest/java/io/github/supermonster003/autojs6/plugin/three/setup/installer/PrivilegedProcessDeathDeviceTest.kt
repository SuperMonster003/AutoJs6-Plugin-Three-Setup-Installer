package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Bundle
import android.os.DeadObjectException
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.PrivilegedClient
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.RootShellAccess
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.SharedBindingCache
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.bindingHandshake
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallEngine
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.PrivilegedInstallEngine
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.IPrivilegedInstaller
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.PrivilegedOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PlannedApk
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import io.github.supermonster003.autojs6.plugin.three.setup.installer.spike.SpikeStatusReceiver
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import rikka.shizuku.Shizuku
import java.io.Closeable
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

/**
 * Opt-in SIGKILL of only this test's attached plugin UserService/RootService. The Shizuku server
 * is never killed or restarted. Each PID comes from the real Binder and is checked again in /proc.
 */
@RunWith(AndroidJUnit4::class)
class PrivilegedProcessDeathDeviceTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @Test fun actualDeathDuringWritingDoesNotCommitAndASeparateRequestCanRebind() = withRun { run ->
        val remote = run.acquire()
        var killed = false
        val failure = assertThrows(InstallFailure::class.java) {
            run.engine(remote).install(run.request, object : InstallEngine.Listener {
                override fun onProgress(bytesWritten: Long, totalBytes: Long) {
                    if (bytesWritten > 0 && !killed) { killed = true; run.killOwnedProcess(remote) }
                }
                override fun onUserAction(intent: Intent) = error("No confirmation is authorized")
            })
        }
        assertTrue(killed)
        assertEquals(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, failure.code)
        assertEquals(0, run.commits)
        assertEquals(1, run.created.size)
        assertFalse(run.fixtureInstalled())
        val firstSession = run.created.single()
        if (Build.VERSION.SDK_INT >= 33) {
            await("Production recovery left its known orphan session behind", 5_000) {
                context.packageManager.packageInstaller.getSessionInfo(firstSession) == null
            }
        }
        val orphan = context.packageManager.packageInstaller.getSessionInfo(firstSession) != null
        evidence("writing-death authorizer=${run.authorizer.id} session=$firstSession code=${failure.code} " +
            "commits=0 platformSessionStillPresent=$orphan productionRecoveryExpected=${Build.VERSION.SDK_INT >= 33}")
        // Cleanup can only target an id returned by this test's real createSession call.
        run.cleanOwnedSession(firstSession)
        val rebound = run.acquire()
        assertNotSame(remote.asBinder(), rebound.asBinder())
        assertEquals(FixtureInstallUi.PACKAGE_NAME, run.engine(rebound).install(run.request, noUserAction).packageName)
        assertTrue(run.fixtureInstalled())
        assertEquals(2, run.created.size)
        assertEquals(1, run.commits)
        run.assertSourcePreserved()
        evidence("SUCCESS writingDeath=true reboundInstall=true authorizer=${run.authorizer.id} firstSession=$firstSession " +
            "newSession=${run.created.last()} failedOperationReplayed=false sourcePreserved=true")
    }

    @Test fun actualDeathAfterCommitFailsPromptlyWithoutReplayingTheCommittedOperation() = withRun { run ->
        val remote = run.acquire()
        val action = "${context.packageName}.spike.p6-death.${UUID.randomUUID()}"
        val answers = LinkedBlockingQueue<Intent>()
        SpikeStatusReceiver.results[action] = answers
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
        val pending = PendingIntent.getBroadcast(context, 0,
            Intent(context, SpikeStatusReceiver::class.java).setAction(action), flags)
        try {
            val spy = object : IPrivilegedInstaller by remote {
                override fun commit(sessionId: Int, sender: IntentSender) {
                    // Fault injection withholds the real status from the engine's ticket, while
                    // the platform actually installs the guarded fixture. No status is fabricated.
                    remote.commit(sessionId, pending.intentSender)
                    val result = requireNotNull(answers.poll(45, TimeUnit.SECONDS)) { "No real commit result" }
                    assertEquals(PackageInstaller.STATUS_SUCCESS, result.getIntExtra(PackageInstaller.EXTRA_STATUS, -99))
                    assertTrue(run.fixtureInstalled())
                    run.killOwnedProcess(remote)
                }
            }
            val started = SystemClock.elapsedRealtime()
            val failure = assertThrows(InstallFailure::class.java) { run.engine(spy).install(run.request, noUserAction) }
            val elapsed = SystemClock.elapsedRealtime() - started
            assertEquals(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, failure.code)
            assertTrue("Binder death was mistaken for a result timeout: ${elapsed}ms", elapsed < 30_000)
            assertEquals(1, run.created.size)
            assertEquals(1, run.commits)
            assertTrue(run.fixtureInstalled())
            run.assertSourcePreserved()
            evidence("SUCCESS afterCommitDeath=true realStatusWithheld=true actualInstalledVersionCode=1 " +
                "authorizer=${run.authorizer.id} code=${failure.code} creates=1 commits=1 elapsedMillis=$elapsed " +
                "operationReplayed=false")
        } finally { pending.cancel(); SpikeStatusReceiver.results.remove(action) }
    }

    @Test fun readOnlyHandshakeRetriesAtMostOnceAcrossTwoActualServiceDeaths() = withRun { run ->
        val killedPids = mutableListOf<Int>()
        var acquired = 0
        val failure = assertThrows(DeadObjectException::class.java) {
            bindingHandshake<IPrivilegedInstaller>(30_000, SystemClock::elapsedRealtime,
                acquire = { remaining -> acquired++; run.client.acquire(run.authorizer, remaining).also(run.remotes::add) },
                verify = { remote ->
                    killedPids += run.killOwnedProcess(remote)
                    remote.uid // A real transaction on the dead remote Binder, not an injected exception.
                }, invalidate = { PrivilegedClient.invalidate(run.authorizer) },
                disconnected = { it is DeadObjectException || it is SharedBindingCache.ConnectionDied })
        }
        assertEquals(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, InstallFailure.from(failure).code)
        assertEquals(2, acquired)
        assertEquals(2, killedPids.distinct().size)
        assertTrue(run.created.isEmpty())
        assertEquals(0, run.commits)
        evidence("SUCCESS handshakeActualDeaths=2 authorizer=${run.authorizer.id} pids=$killedPids " +
            "connectionAttempts=2 code=AUTHORIZER_UNAVAILABLE platformCreates=0")
    }

    @Test fun recoveryRejectsMismatchedIdentityWithoutAbandoningTheLiveSession() = withRun { run ->
        assumeTrue("AOSP exposes the actual installer UID from API 33", Build.VERSION.SDK_INT >= 33)
        val remote = run.acquire()
        val params = Bundle().apply {
            putLong(PrivilegedOptions.SIZE, run.request.prepared.totalBytes)
            putString(PrivilegedOptions.PACKAGE_NAME, FixtureInstallUi.PACKAGE_NAME)
        }
        val id = remote.createSession(params, if (remote.uid == 2000) "com.android.shell" else context.packageName, Process.myUid() / 100000)
        check(run.created.add(id))
        val identity = requireNotNull(remote.getSessionRecoveryInfo(id)) { "The owned platform session has no safe recovery identity" }
        try {
            assertEquals(Process.myUid(), identity.getInt("ownerUid"))
            val variants = listOf<Bundle.() -> Unit>(
                { putInt("ownerUid", getInt("ownerUid") + 1) },
                { putInt("installerUid", getInt("installerUid") + 1) },
                { putInt("sessionId", getInt("sessionId") + 1) },
                { putInt("userId", getInt("userId") + 1) },
                { putLong("createdMillis", getLong("createdMillis") + 1) },
                { putLong("size", getLong("size") + 1) },
                { putString("installer", "example.other.installer") },
                { putString("packageName", "example.other.application") },
            )
            variants.forEach { mutate ->
                val wrong = Bundle(identity).apply(mutate)
                assertThrows(SecurityException::class.java) { remote.abandonRecoveredSession(id, wrong) }
                assertNotNull("Rejected recovery modified its target", context.packageManager.packageInstaller.getSessionInfo(id))
            }
            assertThrows(IllegalArgumentException::class.java) {
                remote.abandonRecoveredSession(id, Bundle(identity).apply { putString("unexpected", "field") })
            }
            assertThrows(IllegalArgumentException::class.java) {
                remote.abandonRecoveredSession(id, Bundle(identity).apply { putString("size", "wrong-type") })
            }
            assertNotNull(context.packageManager.packageInstaller.getSessionInfo(id))
            assertEquals(0, run.commits)
            evidence("SUCCESS recoveryIdentityGuard=true authorizer=${run.authorizer.id} session=$id rejectedVariants=10 commits=0")
        } finally { remote.abandon(id) }
    }

    @Test fun stoppingOnlyTheExplicitTemporaryEmulatorShizukuServerCancelsItsLiveWrite() {
        val args = InstrumentationRegistry.getArguments()
        val pidText = args.getString("temporaryShizukuServerPid")
        assumeTrue("Only a driver-owned temporary emulator server may be stopped", pidText != null)
        check(Build.VERSION.SDK_INT == 24 && shell("getprop ro.kernel.qemu").trim() == "1") {
            "The server-stop probe is restricted to the designated API 24 emulator"
        }
        val pid = requireNotNull(pidText?.toIntOrNull()).also { require(it > 1 && it != Process.myPid()) }
        val name = shell("cat /proc/$pid/cmdline").substringBefore('\u0000')
        check(name == "shizuku_server") { "The selected PID is not a Shizuku server" }
        val start = shell("cat /proc/$pid/stat").substringAfterLast(") ").split(' ').getOrNull(19)
        check(start != null && start.matches(Regex("[0-9]+")))
        check(Regex("^Uid:\\s+2000\\s", RegexOption.MULTILINE).containsMatchIn(shell("cat /proc/$pid/status")))
        withRun(temporaryServerStop = true) { run ->
            check(run.authorizer == Authorizer.SHIZUKU && Shizuku.getUid() == 2000)
            val remote = run.acquire()
            check(remote.processIdentity.getInt("pid", -1) != pid)
            var stopped = false
            val failure = assertThrows(InstallFailure::class.java) {
                run.engine(remote).install(run.request, object : InstallEngine.Listener {
                    override fun onProgress(bytesWritten: Long, totalBytes: Long) {
                        if (bytesWritten <= 0 || stopped) return
                        check(shell("cat /proc/$pid/cmdline").substringBefore('\u0000') == name)
                        check(shell("cat /proc/$pid/stat").substringAfterLast(") ").split(' ').getOrNull(19) == start)
                        check(Regex("^Uid:\\s+2000\\s", RegexOption.MULTILINE).containsMatchIn(shell("cat /proc/$pid/status")))
                        check(Shizuku.pingBinder())
                        shell("kill -9 $pid")
                        stopped = true
                        await("Temporary Shizuku server is still reachable", 5_000) { !Shizuku.pingBinder() }
                        instrumentation.waitForIdleSync()
                        // If server death leaked its private service, stop at this checkpoint
                        // without allowing even the owned fixture to be committed by stale work.
                        await("The private service survived loss of its authorization server", 5_000) { !remote.asBinder().isBinderAlive }
                    }
                    override fun onUserAction(intent: Intent) = error("No confirmation is authorized")
                })
            }
            assertTrue(stopped)
            assertEquals(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, failure.code)
            assertEquals(0, run.commits)
            assertEquals(1, run.created.size)
            assertFalse(run.fixtureInstalled())
            assertFalse(AuthorizerStates.state(context, Authorizer.SHIZUKU).running)
            val id = run.created.single()
            await("Stopping the server left the owned session behind", 5_000) { context.packageManager.packageInstaller.getSessionInfo(id) == null }
            evidence("SUCCESS temporaryServerStopped=true serverPid=$pid serverStartTicks=$start " +
                "code=${failure.code} platformSession=$id privateServiceDead=true commits=0 operationReplayed=false")
        }
        assertFalse(Shizuku.pingBinder())
    }

    private fun withRun(temporaryServerStop: Boolean = false, block: (Run) -> Unit) {
        val option = InstrumentationRegistry.getArguments().getString("peerDeathAuthorizer")
        assumeTrue("Opt in with peerDeathAuthorizer=shizuku|root", option != null)
        val authorizer = requireNotNull(Authorizer.fromId(option)).also { require(it.privileged) }
        check(InstallPresentation.snapshots().isEmpty()) { "Finish active installer work first" }
        FixturePackageOwnership(setOf(FixtureInstallUi.PACKAGE_NAME)).use { ownership ->
            if (authorizer == Authorizer.SHIZUKU) {
                await("Previously granted Shizuku must be available", 5_000) { AuthorizerStates.state(context, authorizer).usable }
            } else check(AuthorizerStates.request(context, authorizer, 10_000)) { "Root was not authorized" }
            Run(authorizer).use { run ->
                ownership.installationStarted()
                block(run)
            }
        }
        val servers = if (temporaryServerStop) "temporaryServerStopped=true preExistingServersUntouched=true" else "shizukuServerUntouched=true"
        evidence("CLEANUP fixtureAbsentAcrossUsers=true ownSourcesRemoved=true ownServiceOnly=true $servers")
    }

    private inner class Run(val authorizer: Authorizer) : Closeable {
        val client = PrivilegedClient.get(context)
        val remotes = mutableListOf<IPrivilegedInstaller>()
        val created = linkedSetOf<Int>()
        var commits = 0
        private val directory = File(context.cacheDir, "p6-peer-death-${UUID.randomUUID()}").apply { check(mkdir()) }
        private val source = File(directory, "fixture-v1.apk")
        val request: InstallEngine.Request

        init {
            instrumentation.context.assets.open("fixture-v1.apk").use { input -> source.outputStream().use(input::copyTo) }
            assertSourcePreserved()
            request = InstallEngine.Request(PreparedPackage("apk", source.name, source.length(), FixtureInstallUi.PACKAGE_NAME,
                "1.0", 1, "Installer spike fixture", 24, 28,
                listOf(PlannedApk("base.apk", source, source.length(), null, sha256 = FIXTURE_SHA256)),
                emptyList(), null, emptyList(), emptyList(), true),
                InstallOptions(authorizer = authorizer.id, timeoutMillis = 90_000), Process.myUid() / 100000,
                InstallerContract.INTERACTION_SILENT)
        }

        fun acquire() = client.acquire(authorizer).also(remotes::add)

        fun engine(remote: IPrivilegedInstaller): PrivilegedInstallEngine {
            val trace = object : IPrivilegedInstaller by remote {
                override fun createSession(params: Bundle, installerPackageName: String, userId: Int): Int =
                    remote.createSession(params, installerPackageName, userId).also { check(created.add(it)) }
                override fun commit(sessionId: Int, sender: IntentSender) { remote.commit(sessionId, sender); commits++ }
            }
            return PrivilegedInstallEngine(context, authorizer,
                acquireRecovery = { timeout -> client.acquire(authorizer, timeout).also(remotes::add) }) { trace }
        }

        fun killOwnedProcess(remote: IPrivilegedInstaller): Int {
            check(remote in remotes || remotes.any { it.asBinder() === remote.asBinder() })
            val identity = remote.processIdentity
            val pid = identity.getInt("pid", -1)
            val uid = identity.getInt("uid", -1)
            check(identity.getInt("ownerUid", -1) == Process.myUid())
            check(pid > 1 && pid != Process.myPid())
            check(uid == if (authorizer == Authorizer.ROOT) 0 else 2000)
            val name = processName(pid)
            check(name.startsWith(context.packageName + ":") && name.length <= 180 && name.matches(Regex("[a-zA-Z0-9._:-]+"))) {
                "The real Binder's PID does not name this plugin's process: $name"
            }
            val start = privilegedCommand("cat /proc/$pid/stat").substringAfterLast(") ").split(' ').getOrNull(19)
            check(start != null && start.matches(Regex("[0-9]+")))
            val dead = CountDownLatch(1)
            remote.asBinder().linkToDeath({ dead.countDown() }, 0)
            val status = privilegedCommand("cat /proc/$pid/status")
            val actualUid = Regex("^Uid:\\s+(\\d+)", RegexOption.MULTILINE).find(status)?.groupValues?.get(1)?.toInt()
            check(actualUid == uid)
            check(processName(pid) == name)
            check(privilegedCommand("cat /proc/$pid/stat").substringAfterLast(") ").split(' ').getOrNull(19) == start)
            // Re-read identity on the same still-live Binder immediately before targeting its PID.
            val confirmed = remote.processIdentity
            check(confirmed.getInt("pid", -1) == pid && confirmed.getInt("uid", -1) == uid &&
                confirmed.getInt("ownerUid", -1) == Process.myUid())
            val reply = privilegedCommand("kill -9 $pid")
            check(reply.isBlank()) { "Guarded process kill failed: $reply" }
            check(dead.await(10, TimeUnit.SECONDS)) { "The attached Binder did not die" }
            assertFalse(remote.asBinder().isBinderAlive)
            evidence("PROCESS_KILLED authorizer=${authorizer.id} pid=$pid uid=$uid ownerUid=${Process.myUid()} name=$name startTicks=$start")
            return pid
        }

        private fun processName(pid: Int): String = if (authorizer == Authorizer.ROOT) {
            // Keep /proc's binary separators out of libsu's line-oriented stdout API.
            privilegedCommand("tr '\\000' '\\n' < /proc/$pid/cmdline").lineSequence().firstOrNull().orEmpty()
        } else privilegedCommand("cat /proc/$pid/cmdline").substringBefore('\u0000')

        fun cleanOwnedSession(id: Int) {
            check(id in created) { "Only ids returned by this run may be cleaned" }
            val info = context.packageManager.packageInstaller.getSessionInfo(id) ?: return
            check(info.sessionId == id)
            check(info.installerPackageName == if (authorizer == Authorizer.SHIZUKU) "com.android.shell" else context.packageName)
            val response = privilegedCommand("pm install-abandon $id")
            check(response.trim() == "Success") { "Cannot abandon this run's session: $response" }
            await("Owned platform session remains after cleanup", 5_000) { context.packageManager.packageInstaller.getSessionInfo(id) == null }
        }

        private fun privilegedCommand(script: String): String = if (authorizer == Authorizer.ROOT) {
            RootShellAccess.submit(context, { shell ->
                check(shell.isRoot)
                val output = mutableListOf<String>()
                val errors = mutableListOf<String>()
                val result = shell.newJob().add(script).to(output, errors).exec()
                check(result.isSuccess) { "Guarded root command failed: ${errors.joinToString(" ")}" }
                output.joinToString("\n")
            }).get(15, TimeUnit.SECONDS)
        } else {
            // The instrumentation shell shares uid 2000 with this Shizuku UserService. It cannot
            // kill root processes, and the command verifies name, uid and birth time before SIGKILL.
            shell(script)
        }

        fun fixtureInstalled() = runCatching { context.packageManager.getPackageInfo(FixtureInstallUi.PACKAGE_NAME, 0) }.isSuccess

        @Suppress("DEPRECATION")
        fun assertSourcePreserved() {
            assertEquals(FIXTURE_SHA256, digest(source))
            val info = requireNotNull(context.packageManager.getPackageArchiveInfo(source.absolutePath, 0))
            assertEquals(FixtureInstallUi.PACKAGE_NAME, info.packageName)
            assertEquals(1, info.versionCode)
            assertEquals(0, requireNotNull(info.applicationInfo).flags and ApplicationInfo.FLAG_HAS_CODE)
            ZipFile(source).use { zip -> check(zip.entries().asSequence().none { it.name.endsWith(".dex", true) || it.name.endsWith(".so", true) }) }
        }

        override fun close() {
            var failure: Throwable? = null
            fun cleanup(block: () -> Unit) { try { block() } catch (problem: Throwable) {
                if (failure == null) failure = problem else failure!!.addSuppressed(problem)
            } }
            created.forEach { cleanup { cleanOwnedSession(it) } }
            cleanup { assertSourcePreserved() }
            cleanup { check(directory.deleteRecursively() || !directory.exists()) }
            val stopped = CountDownLatch(remotes.count { it.asBinder().isBinderAlive })
            remotes.filter { it.asBinder().isBinderAlive }.forEach { remote ->
                runCatching { remote.asBinder().linkToDeath({ stopped.countDown() }, 0) }.onFailure { stopped.countDown() }
            }
            cleanup {
                client.releaseAll()
                // The negative server-stop probe must clean its already attached private service
                // even when testing a broken production unbind path. This never targets a server.
                if (authorizer == Authorizer.SHIZUKU && !Shizuku.pingBinder()) {
                    remotes.filter { it.asBinder().isBinderAlive }.forEach { runCatching { it.destroy() } }
                }
                check(stopped.await(10, TimeUnit.SECONDS)) { "An owned service is still running" }
            }
            failure?.let { throw it }
        }
    }

    private fun shell(command: String): String = instrumentation.uiAutomation.executeShellCommand(command).use {
        ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().use { reader -> reader.readText() }
    }
    private fun digest(file: File): String = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it.toInt() and 0xff) }
    private fun await(message: String, timeout: Long, ready: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeout
        while (SystemClock.elapsedRealtime() < deadline) { if (ready()) return; SystemClock.sleep(25) }
        error(message)
    }
    private fun evidence(message: String) = instrumentation.sendStatus(0, Bundle().apply { putString("privileged-process-death", message) })
    private val noUserAction = object : InstallEngine.Listener {
        override fun onUserAction(intent: Intent) = error("No confirmation is authorized")
    }
    private companion object { const val FIXTURE_SHA256 = "fec583a389978fdc298e9d85979b95feceb93e6fb3fd3f581511c826401e8b69" }
}
