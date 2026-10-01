package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.Process
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.*
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.DhizukuFramework
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.ArchiveOpener
import io.github.supermonster003.autojs6.plugin.three.setup.installer.spike.DhizukuSessionDeathProbeService
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.autojs.plugin.packagearchive.PackageDeviceSpec
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class DhizukuSessionRecoveryDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val fixture = FixtureInstallUi.PACKAGE_NAME

    @Test fun cancellationAfterARealWriteAbandonsOnlyTheOwnedSession() {
        ready()
        FixturePackageOwnership(setOf(fixture)).use {
            val framework = DhizukuFramework(context)
            val baseline = sessions(framework)
            val journal = DhizukuSessionJournal(context)
            check(journal.records().isEmpty()) { "An earlier recovery record must be reviewed first" }
            val folder = File(context.cacheDir, "p8-dhizuku-cancel-${UUID.randomUUID()}").apply { check(mkdir()) }
            try {
                val file = File(folder, "fixture.apk")
                instrumentation.context.assets.open("fixture-v1.apk").use { input -> file.outputStream().use(input::copyTo) }
                val prepared = ArchiveOpener.open(file, file.name, PackageDeviceSpec.from(context), File(folder, "prepared").apply { check(mkdir()) }, true)
                prepared.failure()?.let { throw it }
                assertEquals(fixture, prepared.packageName)
                var written = false
                val failure = assertThrows(InstallFailure::class.java) {
                    DhizukuInstallEngine(context).install(InstallEngine.Request(prepared,
                        InstallOptions(authorizer = Authorizer.DHIZUKU.id), 0, InstallerContract.INTERACTION_SILENT),
                        object : InstallEngine.Listener {
                            override fun onProgress(bytesWritten: Long, totalBytes: Long) { if (bytesWritten > 0) written = true }
                            override fun onUserAction(intent: Intent) = fail("No commit or confirmation was expected")
                        }) { if (written) throw InstallFailure(InstallerErrorCodes.CANCELLED, "Owned fixture cancellation") }
                }
                assertTrue(written)
                assertEquals(InstallerErrorCodes.CANCELLED, failure.code)
                settle { sessions(framework) == baseline }
                assertTrue(journal.records().isEmpty())
                evidence("cancelAfterWrite=true committed=false exactSessionAbandoned=true journalEmpty=true")
            } finally {
                journal.recover(framework)
                check(folder.deleteRecursively())
            }
        }
    }

    @Test fun recoveryDoesNotAbandonALiveRegisteredSession() {
        ready()
        FixturePackageOwnership(setOf(fixture)).use {
            val framework = DhizukuFramework(context)
            val baseline = sessions(framework)
            val journal = DhizukuSessionJournal(context)
            check(journal.records().isEmpty())
            val lease = journal.create(framework, parameters(8192), fixture, 8192)
            try {
                val report = journal.recover(framework)
                assertEquals(1, report.active)
                assertEquals(0, report.abandoned)
                assertNotNull(framework.checked { framework.installer.getSessionInfo(lease.record.id) })
            } finally { try { lease.abandon(framework) } finally { lease.close() } }
            settle { sessions(framework) == baseline }
            assertTrue(journal.records().isEmpty())
            evidence("liveSessionPreserved=true cleanupExact=true journalEmpty=true")
        }
    }

    @Test fun realCreatorProcessDeathRecoversTheKnownIdAndPreservesAnUnjournaledOwnerSession() {
        ready()
        FixturePackageOwnership(setOf(fixture)).use {
            val framework = DhizukuFramework(context)
            val baseline = sessions(framework)
            val journal = DhizukuSessionJournal(context)
            check(journal.records().isEmpty())
            val token = UUID.randomUUID().toString()
            val directory = DhizukuSessionDeathProbeService.directory(context, token)
            check(!directory.exists())
            val control = framework.checked { framework.installer.createSession(parameters(8192).apply {
                setOriginatingUri(Uri.parse("android-app://${context.packageName}/dhizuku-control/$token"))
            }) }
            var remote: IBinder? = null
            var pid: Int? = null
            var bound = false
            val connected = CountDownLatch(1)
            val died = CountDownLatch(1)
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) { remote = service; connected.countDown() }
                override fun onServiceDisconnected(name: ComponentName) = Unit
            }
            try {
                bound = context.bindService(Intent(context, DhizukuSessionDeathProbeService::class.java)
                    .putExtra("case", token).putExtra("confirmDhizukuFixture", true), connection, Context.BIND_AUTO_CREATE)
                check(bound && connected.await(10, TimeUnit.SECONDS))
                val binder = requireNotNull(remote)
                val request = Parcel.obtain(); val reply = Parcel.obtain()
                try {
                    request.writeInterfaceToken(DhizukuSessionDeathProbeService.DESCRIPTOR)
                    check(binder.transact(IBinder.FIRST_CALL_TRANSACTION, request, reply, 0))
                    reply.readException(); pid = reply.readInt(); assertEquals(token, reply.readString())
                } finally { request.recycle(); reply.recycle() }
                val creatorPid = requireNotNull(pid)
                check(creatorPid > 0 && creatorPid != Process.myPid())
                binder.linkToDeath({ died.countDown() }, 0)
                var status: JsonObject? = null
                settle {
                    status = runCatching { JsonParser.parseString(File(directory, "status.json").readText()).asJsonObject }.getOrNull()
                    status != null
                }
                val captured = requireNotNull(status)
                assertTrue(captured.toString(), captured.get("ready").asBoolean)
                assertEquals(creatorPid, captured.get("pid").asInt)
                assertEquals(4096, captured.get("bytesWritten").asInt)
                val ownedId = captured.get("session").asInt
                val saved = journal.records().single()
                assertEquals(ownedId, saved.id)
                assertEquals(creatorPid, saved.creatorPid)
                assertEquals(captured.get("sessionToken").asString, saved.token)
                // A second process must not reclaim the still-live creator's journal.
                assertEquals(1, journal.recover(framework).active)
                assertTrue(binder.isBinderAlive)
                context.unbindService(connection); bound = false
                Process.killProcess(creatorPid)
                assertTrue(died.await(10, TimeUnit.SECONDS))
                settle { !binder.isBinderAlive }
                assertNotNull("The owner server outlives the killed creator", framework.checked { framework.installer.getSessionInfo(ownedId) })
                val report = journal.recover(framework)
                assertEquals(1, report.abandoned)
                assertEquals(0, report.retained)
                assertNotNull("An owner's unrelated ID must survive recovery", framework.checked { framework.installer.getSessionInfo(control) })
                settle { sessions(framework) == baseline + control }
                assertTrue(journal.records().isEmpty())
                evidence("creatorPid=$creatorPid realProcessDeath=true writtenBytes=4096 committed=false recoveredId=$ownedId unrelatedId=$control preserved=true journalEmpty=true")
            } finally {
                if (bound) runCatching { context.unbindService(connection) }
                pid?.takeIf { it > 0 && it != Process.myPid() }?.let { candidate ->
                    if (remote?.isBinderAlive == true) { Process.killProcess(candidate); died.await(10, TimeUnit.SECONDS) }
                }
                journal.recover(framework)
                framework.checked { framework.installer.abandonSession(control) }
                settle { sessions(framework) == baseline }
                check(!directory.exists() || directory.deleteRecursively())
            }
        }
    }

    private fun ready() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("confirmDhizukuFixture") == "true")
        assumeTrue(Build.VERSION.SDK_INT >= 28)
        check(Process.myUid() / 100000 == 0 && AuthorizerStates.state(context, Authorizer.DHIZUKU).usable)
    }
    private fun parameters(size: Long) = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
        setAppPackageName(fixture); setSize(size)
    }
    private fun sessions(framework: DhizukuFramework) = framework.checked { framework.installer.mySessions.map { it.sessionId }.toSet() }
    private fun settle(condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (!condition() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(50)
        check(condition()) { "Owned Dhizuku recovery did not settle" }
    }
    private fun evidence(message: String) = instrumentation.sendStatus(0, Bundle().apply { putString("dhizuku-recovery", message) })
}
