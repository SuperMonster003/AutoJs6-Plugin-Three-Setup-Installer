package io.github.supermonster003.autojs6.plugin.three.setup.installer.spike

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import android.util.AtomicFile
import android.view.WindowManager
import android.widget.TextView
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.PrivilegedClient
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.DescriptorInstallEnvironment
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallRequest
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallSession
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.PrivilegedInstallEngine
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.SourceEntry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.history.InstallHistoryEntry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.history.InstallHistoryStore
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.IPrivilegedInstaller
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PackageStaging
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.StagingDirectories
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallForegroundService
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallNotifications
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallRecoveryPersistence
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallRecoverySnapshot
import org.autojs.plugin.installer.api.InstallerContract as C
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * DUMP-protected, debug-only probe of an actual privileged write interrupted by main-process death.
 * The desktop driver copies the fixed, component-free fixture into a unique private directory,
 * proves its absence for every device user, and kills only the recorded main PID after READY.
 * This is deliberately not evidence of the host UID dying. No package is ever committed.
 */
class InstallProcessDeathProbeActivity : Activity() {
    @SuppressLint("SetTextI18n") // This shell-only debug probe has no production UI.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(TextView(this).apply { text = "Installer process-death probe (test fixture only)" })
        dispatch(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        dispatch(intent)
    }

    private fun dispatch(input: Intent) {
        val caseId = input.getStringExtra("caseId")?.takeIf { CASE.matches(it) } ?: run { finish(); return }
        val mode = input.getStringExtra("mode") ?: "recover"
        val authorizer = input.getStringExtra("authorizer")
        val expectedPid = input.getIntExtra("expectedPid", -1)
        // An OEM may restore the just-killed control Activity in a fresh process. The old
        // self-death command is idempotent: never kill that new process or poison recovery.
        if (mode == "die" && expectedPid > 0 && expectedPid != Process.myPid()) { finish(); return }
        Thread({
            val directory = File(filesDir.canonicalFile, "p6-process-death/$caseId")
            try {
                check(directory.isDirectory && directory.canonicalFile == directory.absoluteFile) { "Missing private probe directory" }
                when (mode) {
                    "start" -> startProbe(caseId, directory, requireNotNull(authorizer))
                    "recover" -> recover(directory)
                    "cleanup" -> cleanup(directory)
                    "watch-host" -> watchHost(caseId, directory)
                    "cleanup-host" -> cleanupHost(directory)
                    "die" -> die(directory, expectedPid)
                    else -> error("Unknown probe mode")
                }
            } catch (failure: Exception) {
                if (directory.isDirectory) atomicWrite(File(directory, "error.json"), JsonObject().apply {
                    addProperty("caseId", caseId)
                    addProperty("mode", mode)
                    addProperty("pid", Process.myPid())
                    addProperty("error", failure.javaClass.simpleName + ": " + failure.message)
                })
                runOnUiThread { finish() }
            }
        }, "p6-process-death-control").start()
    }

    private fun startProbe(caseId: String, directory: File, authorizerId: String) {
        check(active.get() == null && InstallPresentation.snapshots().isEmpty()) { "An installation is already active" }
        val existingHistory = InstallHistoryStore.get(this)
        waitUntil(10_000) { existingHistory.loaded }
        check(existingHistory.list().size < InstallHistoryEntry.MAX_ENTRIES) { "The probe must not evict existing history" }
        val authorizer = when (authorizerId) {
            C.AUTHORIZER_SHIZUKU -> Authorizer.SHIZUKU
            C.AUTHORIZER_ROOT -> Authorizer.ROOT
            else -> error("A privileged authorizer is required")
        }
        val source = File(directory, "fixture.apk")
        check(source.isFile && source.canonicalFile == source.absoluteFile && source.length() in 1..(1024 * 1024))
        check(!File(directory, "evidence.json").exists()) { "A recorded probe must not be replayed" }
        check(!installed()) { "The test fixture already exists" }
        val sourceHash = sha256(source)
        val stagingBefore = PackageStaging.root(this).list().orEmpty().toSet()
        val evidence = JsonObject().apply {
            addProperty("caseId", caseId)
            addProperty("kind", "plugin-main-death")
            addProperty("fixturePackage", FIXTURE)
            addProperty("sourceSha256", sourceHash)
            addProperty("sourceBytes", source.length())
            addProperty("authorizer", authorizerId)
            addProperty("seedPid", Process.myPid())
            addProperty("pluginUid", Process.myUid())
            addProperty("createdAt", System.currentTimeMillis())
            addProperty("phase", "starting")
            addProperty("createCount", 0)
            addProperty("commitCount", 0)
        }
        val evidenceFile = File(directory, "evidence.json")
        fun save() = atomicWrite(evidenceFile, evidence)
        save()
        if (authorizer == Authorizer.ROOT) {
            val authorizationStarted = SystemClock.elapsedRealtime()
            check(AuthorizerStates.request(this, authorizer, 30_000)) { "Root authorization was not granted" }
            evidence.addProperty("requestAuthorizerElapsedMs", SystemClock.elapsedRealtime() - authorizationStarted)
            save()
        }
        val request = InstallRequest("p6-process-death-$caseId", listOf(SourceEntry(0, 0, "fixture.apk", source.length())),
            C.INTERACTION_SILENT, InstallOptions(authorizer = authorizerId, timeoutMillis = 180_000), origin = C.SOURCE_HOME)
        val control = Active(caseId)
        check(active.compareAndSet(null, control))
        val record = InstallPresentation.create(this, request, InstallPresentation.Callbacks(cancel = { control.session?.cancel() }))
        control.record = record
        evidence.addProperty("token", record.token)
        save()
        val held = AtomicBoolean()
        val descriptor = ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY)
        var delegate: DescriptorInstallEnvironment? = null
        try {
            delegate = DescriptorInstallEnvironment.acquire(this, listOf(descriptor), preparedListener = { index, prepared ->
                validateFixture(prepared)
                record.onPrepared(index, prepared)
                val newDirectories = PackageStaging.root(this).list().orEmpty().toSet() - stagingBefore
                check(newDirectories.size == 1) { "Cannot identify the probe's unique staging directory" }
                evidence.addProperty("stagingDirectory", newDirectories.single())
                evidence.addProperty("preparedBytes", prepared.totalBytes)
                save()
            })
            val production = requireNotNull(delegate)
            val environment = object : InstallSession.Environment by production {
                override fun resolve(request: InstallRequest, deadlineMillis: Long, checkActive: () -> Unit): InstallSession.Target {
                    val target = production.resolve(request, deadlineMillis, checkActive)
                    check(target.authorizer == authorizer)
                    val engine = PrivilegedInstallEngine(this@InstallProcessDeathProbeActivity, authorizer) { timeout ->
                        val service = PrivilegedClient.get(this@InstallProcessDeathProbeActivity).acquire(authorizer, timeout)
                        val identity = service.processIdentity
                        check(identity.getInt("ownerUid") == Process.myUid())
                        evidence.addProperty("privilegedPid", identity.getInt("pid"))
                        evidence.addProperty("privilegedUid", identity.getInt("uid"))
                        save()
                        object : IPrivilegedInstaller by service {
                            override fun createSession(params: Bundle, installerPackageName: String, userId: Int): Int {
                                val id = service.createSession(params, installerPackageName, userId)
                                evidence.addProperty("platformSessionId", id)
                                evidence.addProperty("createCount", evidence["createCount"].asInt + 1)
                                save()
                                return id
                            }
                            override fun commit(sessionId: Int, sender: android.content.IntentSender) {
                                evidence.addProperty("commitCount", evidence["commitCount"].asInt + 1)
                                save()
                                error("A death probe must never commit a package")
                            }
                        }
                    }
                    return target.copy(engine = engine)
                }
            }
            val session = InstallSession(request, environment, object : InstallSession.Listener {
                override fun onStage(stage: String, detail: JsonObject) {
                    record.onStage(stage, detail)
                    InstallNotifications.update(applicationContext, record.token, "Process death test fixture", stage,
                        control.session?.status()?.progress ?: 0f, record.activityIntent()) { control.session?.cancel() }
                }
                override fun onProgress(progress: Float, detail: JsonObject) {
                    record.onProgress(progress, detail)
                    InstallNotifications.update(applicationContext, record.token, "Process death test fixture", C.STAGE_WRITING,
                        progress, record.activityIntent()) { control.session?.cancel() }
                    if (progress <= 0 || !held.compareAndSet(false, true)) return
                    check(evidence["createCount"].asInt == 1 && evidence["commitCount"].asInt == 0)
                    val snapshot = readSnapshot(record.token)
                    check(snapshot != null && !snapshot.terminal && snapshot.stage == C.STAGE_WRITING)
                    waitUntil(10_000) {
                        val history = InstallHistoryStore.get(applicationContext)
                        val persisted = File(noBackupFilesDir, "installation-history/history.json")
                        history.loaded && history.list().any { it.token == record.token && !it.terminal } &&
                            persisted.isFile && persisted.readText().contains(record.token)
                    }
                    waitUntil(10_000) { foregroundRunning() }
                    evidence.addProperty("bytesWritten", detail[C.FIELD_BYTES_WRITTEN].asLong)
                    evidence.addProperty("foregroundObserved", true)
                    evidence.addProperty("recoverySaved", true)
                    evidence.addProperty("historySaved", true)
                    evidence.addProperty("phase", "ready-for-main-death")
                    save()
                    // Hold a real opened session and stream after at least one accepted byte write.
                    // A missing desktop driver eventually cancels; it must never fall through to commit.
                    control.cancelled.await(120, TimeUnit.SECONDS)
                    throw InstallFailure(InstallerErrorCodes.CANCELLED, "The process-death probe was cancelled")
                }
                override fun onCompleted(result: JsonObject) = error("The process-death probe must not complete an installation")
                override fun onFailed(failure: InstallFailure) {
                    record.onFailed(failure)
                    InstallNotifications.remove(applicationContext, record.token)
                    evidence.addProperty("phase", "cancelled-before-main-death")
                    evidence.addProperty("errorCode", failure.code)
                    save()
                    control.finished.countDown()
                }
            }, SystemClock::elapsedRealtime)
            control.session = session
            session.start(control.executor)
        } catch (failure: Exception) {
            delegate?.close()
            record.close()
            active.compareAndSet(control, null)
            control.executor.shutdownNow()
            throw failure
        } finally { descriptor.close() }
    }

    private fun die(directory: File, expectedPid: Int) {
        val evidence = readEvidence(directory)
        check(expectedPid == Process.myPid() && evidence["seedPid"].asInt == expectedPid)
        check(evidence["phase"].asString == "ready-for-main-death" && evidence["createCount"].asInt == 1 &&
            evidence["commitCount"].asInt == 0 && evidence["bytesWritten"].asLong > 0)
        val control = requireNotNull(active.get()?.takeIf { it.caseId == directory.name })
        check(control.session?.status()?.stage == C.STAGE_WRITING)
        // Some OEMs deny run-as -> app signals despite matching UIDs. The DUMP-protected
        // driver can request only this already-validated case's own current process to die.
        Process.killProcess(Process.myPid())
    }

    private fun recover(directory: File) {
        val evidence = readEvidence(directory)
        check(evidence["phase"].asString == "ready-for-main-death") { "The real writer did not reach the guarded death point" }
        check(evidence["seedPid"].asInt != Process.myPid()) { "The main process must actually die" }
        val token = evidence["token"].asString
        check(InstallRecoverySnapshot.validToken(token))
        check(InstallPresentation.find(token) == null && InstallPresentation.snapshots().isEmpty()) { "A worker was replayed" }
        check(!installed() && sha256(File(directory, "fixture.apk")) == evidence["sourceSha256"].asString)
        val snapshot = readSnapshot(token)
        check(snapshot != null && !snapshot.terminal && snapshot.items.none { it.ok == true })
        val display = snapshot.display()
        check(display.recovered && display.interrupted && display.terminal && display.stage == C.STAGE_CANCELLED && !display.canRetry && display.prompt == null)
        val history = InstallHistoryStore.get(this)
        waitUntil(10_000) { history.loaded && history.list().any { it.token == token } }
        val restored = history.list().single { it.token == token }
        check(restored.result == C.STAGE_CANCELLED && restored.interrupted && restored.errorCode == InstallerErrorCodes.CANCELLED)
        val started = CountDownLatch(1)
        val startFailure = AtomicReference<Exception?>()
        runOnUiThread {
            try {
                check(startService(Intent(this, InstallForegroundService::class.java)) == ComponentName(this, InstallForegroundService::class.java))
            } catch (failure: Exception) { startFailure.set(failure) }
            finally { started.countDown() }
        }
        check(started.await(5, TimeUnit.SECONDS))
        startFailure.get()?.let { throw it }
        SystemClock.sleep(1500)
        check(!foregroundServicePresent()) { "An empty restarted foreground service did not stop" }
        check(InstallPresentation.find(token) == null && InstallPresentation.snapshots().isEmpty() && !installed())
        evidence.addProperty("recoverPid", Process.myPid())
        evidence.addProperty("recoveredHistory", restored.result)
        evidence.addProperty("recoveredInterrupted", restored.interrupted)
        evidence.addProperty("recoveryReadOnly", true)
        evidence.addProperty("foregroundRestartRequested", true)
        evidence.addProperty("foregroundRestartStoppedWithoutWriter", true)
        evidence.addProperty("sourcePreserved", true)
        evidence.addProperty("fixtureAbsent", true)
        evidence.addProperty("phase", "recovered-without-replay")
        atomicWrite(File(directory, "evidence.json"), evidence)
        runOnUiThread { finish() }
    }

    /** Watches a session accepted by the real HostCallerGuard; never opens a test-identity session. */
    private fun watchHost(caseId: String, directory: File) {
        check(caseId.length <= 32) { "The source name must fit the production staging-name limit" }
        check(!File(directory, "evidence.json").exists())
        val requestId = "p6-host-death-$caseId"
        var record: InstallPresentation.Record? = null
        waitUntil(30_000) {
            record = InstallPresentation.snapshots().mapNotNull { InstallPresentation.find(it.token) }.singleOrNull { it.request.id == requestId }
            record != null
        }
        val owned = requireNotNull(record)
        check(owned.request.origin == C.SOURCE_HOST && owned.request.sources.single().displayName == "host-death-$caseId.apk")
        check(owned.snapshot().stage == C.STAGE_PREPARING && !owned.snapshot().terminal)
        val sourceName = PackageStaging.safeName(owned.request.sources.single().displayName)
        var staging: File? = null
        waitUntil(15_000) {
            staging = PackageStaging.root(this).canonicalFile.listFiles().orEmpty().singleOrNull { candidate ->
                File(candidate, "item-0/source-0/$sourceName").let { it.isFile && it.length() in (256L * 1024)..(512L * 1024) }
            }
            staging != null
        }
        val stagingDirectory = requireNotNull(staging)
        check(stagingDirectory.canonicalFile == stagingDirectory.absoluteFile)
        val stagedFile = File(stagingDirectory, "item-0/source-0/$sourceName")
        val evidence = JsonObject().apply {
            addProperty("caseId", caseId)
            addProperty("kind", "official-host-caller-death")
            addProperty("requestId", requestId)
            addProperty("token", owned.token)
            addProperty("pluginUid", Process.myUid())
            addProperty("pluginPid", Process.myPid())
            addProperty("sourceOrigin", owned.request.origin)
            addProperty("stagingDirectory", stagingDirectory.name)
            // The last 256 KiB may still be in BufferedOutputStream. Record actual disk bytes,
            // not the larger count the independent host pipe has already accepted.
            addProperty("stagedBytes", stagedFile.length())
            addProperty("stagedSha256", sha256(stagedFile))
            addProperty("platformSessionCreated", false)
            addProperty("phase", "ready-for-host-death")
        }
        atomicWrite(File(directory, "evidence.json"), evidence)
        waitUntil(90_000) { owned.snapshot().terminal }
        val final = owned.snapshot()
        check(final.failure?.code == InstallerErrorCodes.CANCELLED && final.stage == C.STAGE_CANCELLED) {
            "Actual host death was not reported as cancellation: ${final.failure?.code}/${final.stage}"
        }
        check(!stagingDirectory.exists()) { "The owned host source staging was not removed" }
        check(owned.snapshot().items.none { it.result?.get(C.FIELD_OK)?.asBoolean == true })
        evidence.addProperty("phase", "host-death-cancelled")
        evidence.addProperty("errorCode", final.failure?.code)
        evidence.addProperty("stagingRemoved", true)
        atomicWrite(File(directory, "evidence.json"), evidence)
        runOnUiThread { finish() }
    }

    private fun cleanupHost(directory: File) {
        val evidence = JsonParser.parseString(AtomicFile(File(directory, "evidence.json")).openRead().bufferedReader().use { it.readText() }).asJsonObject
        check(evidence["caseId"].asString == directory.name && evidence["kind"].asString == "official-host-caller-death" && evidence["pluginUid"].asInt == Process.myUid())
        val token = evidence["token"].asString
        check(InstallRecoverySnapshot.validToken(token))
        val record = InstallPresentation.find(token)
        check(record == null || record.request.id == evidence["requestId"].asString)
        if (record != null && !record.snapshot().terminal) {
            record.cancel()
            waitUntil(15_000) { record.snapshot().terminal }
        }
        record?.close()
        InstallNotifications.remove(this, token)
        val removed = CountDownLatch(1)
        InstallRecoveryPersistence.remove(this, token) { removed.countDown() }
        check(removed.await(10, TimeUnit.SECONDS))
        val history = InstallHistoryStore.get(this)
        waitUntil(10_000) { history.loaded }
        val historyRemoved = CountDownLatch(1)
        var success = false
        history.remove(InstallHistoryEntry.id(token, 0)) { successful -> success = successful; historyRemoved.countDown() }
        check(historyRemoved.await(10, TimeUnit.SECONDS) && success)
        evidence.addProperty("phaseBeforeCleanup", evidence["phase"].asString)
        evidence.addProperty("phase", "cleaned")
        evidence.addProperty("ownedHistoryRemoved", true)
        atomicWrite(File(directory, "evidence.json"), evidence)
        runOnUiThread { finish() }
    }

    private fun cleanup(directory: File) {
        val evidence = readEvidence(directory)
        val control = active.get()?.takeIf { it.caseId == evidence["caseId"].asString }
        control?.let {
            it.cancelled.countDown()
            it.session?.cancel()
            check(it.finished.await(15, TimeUnit.SECONDS)) { "The owned writer is still running" }
            it.session?.close()
            it.executor.shutdownNow()
            check(it.executor.awaitTermination(10, TimeUnit.SECONDS))
            it.record?.close()
            active.compareAndSet(it, null)
        }
        val token = evidence.get("token")?.asString
        if (token != null) {
            check(InstallRecoverySnapshot.validToken(token))
            check(InstallPresentation.snapshots().none { it.token != token }) { "Another installation is active" }
            InstallPresentation.find(token)?.close()
            InstallNotifications.remove(this, token)
            val recoveryRemoved = CountDownLatch(1)
            InstallRecoveryPersistence.remove(this, token) { recoveryRemoved.countDown() }
            check(recoveryRemoved.await(10, TimeUnit.SECONDS))
            val history = InstallHistoryStore.get(this)
            waitUntil(10_000) { history.loaded }
            val historyRemoved = CountDownLatch(1)
            var removed = false
            history.remove(InstallHistoryEntry.id(token, 0)) { success -> removed = success; historyRemoved.countDown() }
            check(historyRemoved.await(10, TimeUnit.SECONDS) && removed)
            check(history.list().none { it.token == token })
            evidence.addProperty("ownedHistoryRemoved", true)
        }
        evidence.get("stagingDirectory")?.asString?.let { name ->
            check(name.matches(Regex("[A-Za-z0-9._-]{1,128}")))
            val parent = PackageStaging.root(this).canonicalFile
            val staging = File(parent, name)
            check(staging.parentFile == parent && staging.absoluteFile == staging.canonicalFile)
            if (staging.exists()) {
                // Production policy is 24 hours. Accelerate only this proven-owned orphan's age,
                // explicitly recording that this verifies eventual cleanup rather than instant cleanup.
                check(staging.setLastModified(System.currentTimeMillis() - StagingDirectories.STALE_AFTER_MILLIS - 60_000))
                evidence.addProperty("ownedOrphanAgeAccelerated", true)
                PackageStaging.cleanStale(this)
            }
            check(!staging.exists()) { "The expired owned staging directory was not reclaimed" }
            evidence.addProperty("stagingRemoved", true)
        }
        check(!installed()) { "Unexpected fixture installation; refuse automatic package removal" }
        check(sha256(File(directory, "fixture.apk")) == evidence["sourceSha256"].asString)
        PrivilegedClient.get(this).releaseAll()
        evidence.addProperty("phaseBeforeCleanup", evidence["phase"].asString)
        evidence.addProperty("phase", "cleaned")
        evidence.addProperty("cleanupPid", Process.myPid())
        atomicWrite(File(directory, "evidence.json"), evidence)
        runOnUiThread { finish() }
    }

    private fun validateFixture(prepared: PreparedPackage) {
        prepared.failure()?.let { throw it }
        check(prepared.packageName == FIXTURE && prepared.versionCode == 1L && prepared.apks.size == 1)
        val apk = prepared.apks.single()
        check(apk.manifest?.requestedPermissions?.isEmpty() == true)
        @Suppress("DEPRECATION")
        val info = requireNotNull(packageManager.getPackageArchiveInfo(apk.file.path, PackageManager.GET_ACTIVITIES or
            PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS or PackageManager.GET_PERMISSIONS))
        check(requireNotNull(info.applicationInfo).flags and ApplicationInfo.FLAG_HAS_CODE == 0)
        check(info.activities.isNullOrEmpty() && info.services.isNullOrEmpty() && info.receivers.isNullOrEmpty() && info.providers.isNullOrEmpty())
    }

    @Suppress("DEPRECATION")
    private fun installed(): Boolean = try { packageManager.getPackageInfo(FIXTURE, PackageManager.MATCH_UNINSTALLED_PACKAGES); true }
        catch (_: PackageManager.NameNotFoundException) { false }

    @Suppress("DEPRECATION")
    private fun foregroundRunning(): Boolean = getSystemService(ActivityManager::class.java).getRunningServices(100)
        .any { it.service == ComponentName(this, InstallForegroundService::class.java) && it.foreground && it.pid == Process.myPid() }

    @Suppress("DEPRECATION")
    private fun foregroundServicePresent(): Boolean = getSystemService(ActivityManager::class.java).getRunningServices(100)
        .any { it.service == ComponentName(this, InstallForegroundService::class.java) && it.pid == Process.myPid() }

    private fun readSnapshot(token: String): InstallRecoverySnapshot? {
        val done = CountDownLatch(1)
        val result = AtomicReference<InstallRecoverySnapshot?>()
        InstallRecoveryPersistence.load(this, token) { result.set(it); done.countDown() }
        check(done.await(10, TimeUnit.SECONDS))
        return result.get()
    }

    private fun readEvidence(directory: File): JsonObject = JsonParser.parseString(AtomicFile(File(directory, "evidence.json"))
        .openRead().bufferedReader().use { it.readText() }).asJsonObject.also {
        check(it["caseId"].asString == directory.name && it["kind"].asString == "plugin-main-death" && it["fixturePackage"].asString == FIXTURE)
        check(it["pluginUid"].asInt == Process.myUid())
    }

    private fun atomicWrite(file: File, value: JsonObject) {
        val atomic = AtomicFile(file)
        val output = atomic.startWrite()
        try { output.write(value.toString().toByteArray(Charsets.UTF_8)); output.fd.sync(); atomic.finishWrite(output) }
        catch (failure: Exception) { atomic.failWrite(output); throw failure }
    }

    private fun sha256(file: File): String = MessageDigest.getInstance("SHA-256").let { digest ->
        file.inputStream().use { input -> val bytes = ByteArray(64 * 1024); while (true) { val read = input.read(bytes); if (read < 0) break; digest.update(bytes, 0, read) } }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun waitUntil(timeout: Long, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeout
        while (!condition()) { check(SystemClock.elapsedRealtime() < deadline) { "Probe state did not settle" }; SystemClock.sleep(50) }
    }

    private class Active(val caseId: String) {
        var session: InstallSession? = null
        var record: InstallPresentation.Record? = null
        val cancelled = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
    }

    private companion object {
        val CASE = Regex("[a-z0-9-]{1,64}")
        const val FIXTURE = "io.github.supermonster003.autojs6.installer.spike.fixture"
        val active = AtomicReference<Active?>()
    }
}
