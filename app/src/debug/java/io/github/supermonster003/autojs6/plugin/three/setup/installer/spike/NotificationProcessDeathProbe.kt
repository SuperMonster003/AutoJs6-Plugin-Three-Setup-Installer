package io.github.supermonster003.autojs6.plugin.three.setup.installer.spike

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import android.util.AtomicFile
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.supermonster003.autojs6.plugin.three.setup.installer.history.InstallHistoryCodec
import io.github.supermonster003.autojs6.plugin.three.setup.installer.history.InstallHistoryEntry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.history.InstallHistoryStore
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PackageStaging
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallNotifications
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallRecoveryPersistence
import org.autojs.plugin.installer.api.InstallerContract as C
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Desktop-owned, fixed-fixture audit through the existing DUMP-protected debug control Activity. */
internal object NotificationProcessDeathProbe {
    private const val FIXTURE = "io.github.supermonster003.autojs6.installer.spike.fixture"
    private const val SHA256 = "fec583a389978fdc298e9d85979b95feceb93e6fb3fd3f581511c826401e8b69"

    fun run(context: Context, nonce: String, directory: File, mode: String) {
        check(UUID.fromString(nonce).toString() == nonce)
        check(Process.myUid() / 100000 == 0)
        val historyFile = File(context.noBackupFilesDir, "installation-history/history.json")
        val baselineFile = File(directory, "notification-history-before.json")
        val journal = File(directory, "notification.json")
        val preferences = context.getSharedPreferences("installer_settings", Context.MODE_PRIVATE)
        val preferenceFile = File(context.applicationInfo.dataDir, "shared_prefs/installer_settings.xml")
        val source = Uri.parse("content://${context.packageName}.test.notification-source/$nonce/fixture.apk")
        check(!installed(context)) { "An existing fixture must not be touched" }
        val sessions = context.packageManager.packageInstaller.mySessions.map { it.sessionId }.sorted()
        if (mode == "notification-prepare") {
            check(!journal.exists() && !baselineFile.exists()) { "A recorded probe must not be replayed" }
            check(preferences.all.isEmpty() && InstallPresentation.snapshots(true).isEmpty() && sessions.isEmpty())
            check(InstallNotifications.available(context))
            val baseline = readHistory(historyFile)
            check(baseline.size < InstallHistoryEntry.MAX_ENTRIES && baseline.all { it.terminal })
            val bytes = if (historyFile.exists()) historyFile.readBytes() else InstallHistoryCodec.encode(emptyList())
            baselineFile.outputStream().use { it.write(bytes); it.fd.sync() }
            val saved = JsonObject().apply {
                addProperty("kind", "notification-source-process-death")
                addProperty("nonce", nonce); addProperty("packageName", context.packageName)
                addProperty("uid", Process.myUid()); addProperty("sdk", Build.VERSION.SDK_INT)
                addProperty("fingerprint", Build.FINGERPRINT); addProperty("seedPid", Process.myPid())
                addProperty("seedStartTicks", processStartTicks())
                addProperty("createdAt", System.currentTimeMillis()); addProperty("phase", "prepared")
                addProperty("sourceUri", source.toString()); addProperty("sourceSha256", SHA256)
                addProperty("historySha256", digest(bytes)); addProperty("historyExisted", historyFile.exists())
                addProperty("preferenceExisted", preferenceFile.exists())
                add("stagingBefore", JsonArray().apply { PackageStaging.root(context).list().orEmpty().sorted().forEach { add(it) } })
            }
            write(journal, saved)
            check(preferences.edit().putString("default_interaction", C.INTERACTION_NOTIFICATION).commit())
            return
        }
        val saved = JsonParser.parseString(AtomicFile(journal).openRead().bufferedReader().use { it.readText() }).asJsonObject
        check(saved["kind"].asString == "notification-source-process-death" && saved["nonce"].asString == nonce &&
            saved["packageName"].asString == context.packageName && saved["uid"].asInt == Process.myUid() &&
            saved["sdk"].asInt == Build.VERSION.SDK_INT && saved["fingerprint"].asString == Build.FINGERPRINT &&
            saved["sourceUri"].asString == source.toString() && saved["sourceSha256"].asString == SHA256)
        val baselineBytes = baselineFile.readBytes()
        check(digest(baselineBytes) == saved["historySha256"].asString)
        val baseline = InstallHistoryCodec.decode(baselineBytes).associateBy { it.id }
        check(preferences.all == mapOf("default_interaction" to C.INTERACTION_NOTIFICATION) && sessions.isEmpty())
        if (mode == "notification-observe") {
            check(saved["phase"].asString == "prepared" && saved["seedPid"].asInt == Process.myPid())
            var record: InstallPresentation.Record? = null
            await {
                val snapshots = InstallPresentation.snapshots(true)
                check(snapshots.size <= 1)
                record = snapshots.singleOrNull()?.let { InstallPresentation.find(it.token) }
                record?.snapshot()?.prompt?.metadata?.packageName == FIXTURE
            }
            val owner = requireNotNull(record)
            val metadata = requireNotNull(owner.snapshot().prompt).metadata
            check(owner.notificationMode && !owner.snapshot().terminal && metadata.versionCode == 1L &&
                owner.request.origin == C.SOURCE_EXTERNAL && owner.request.items.size == 1 &&
                owner.request.sources.single().displayName == "fixture.apk")
            check(context.checkUriPermission(source, Process.myPid(), Process.myUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION) == PackageManager.PERMISSION_GRANTED)
            val descriptor = requireNotNull(context.contentResolver.openFileDescriptor(source, "r"))
            val bytes = ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
            check(digest(bytes) == SHA256)
            await { runCatching { readHistory(historyFile).any { it.token == owner.token && it.packageName == FIXTURE } }.getOrDefault(false) }
            val current = readHistory(historyFile)
            check(current.filterNot { it.token == owner.token }.associateBy { it.id } == baseline)
            check(current.count { it.token == owner.token } == 1)
            val before = saved.getAsJsonArray("stagingBefore").map { it.asString }.toSet()
            val created = PackageStaging.root(context).list().orEmpty().toSet() - before
            check(created.size <= 1)
            saved.add("ownedStaging", JsonArray().apply { created.forEach { add(it) } })
            saved.addProperty("token", owner.token); saved.addProperty("phase", "ready")
            saved.addProperty("sourceReopened", true); saved.addProperty("platformSessionCreated", false)
            write(journal, saved)
            return
        }
        val abort = mode == "notification-abort"
        check((mode == "notification-cleanup" || abort) && saved["phase"].asString == "ready")
        val token = saved["token"].asString
        check(UUID.fromString(token).toString() == token)
        if (abort) {
            InstallPresentation.find(token)?.let { owner ->
                check(owner.notificationMode && owner.snapshot().items.single().metadata?.packageName == FIXTURE)
                owner.cancel()
                await { owner.snapshot().terminal }
                owner.close()
            }
            await { context.checkUriPermission(source, Process.myPid(), Process.myUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION) == PackageManager.PERMISSION_DENIED }
        } else check(saved["seedPid"].asInt != Process.myPid()) { "The creator process did not die" }
        check(InstallPresentation.snapshots(true).isEmpty()) { "A request was recreated or another task is active" }
        check(context.checkUriPermission(source, Process.myPid(), Process.myUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION) == PackageManager.PERMISSION_DENIED)
        val current = readHistory(historyFile)
        check(current.filterNot { it.token == token }.associateBy { it.id } == baseline)
        val owned = current.single { it.token == token }
        check(owned.id == InstallHistoryEntry.id(token, 0) && owned.origin == C.SOURCE_EXTERNAL &&
            owned.packageName == FIXTURE && owned.startedAt >= saved["createdAt"].asLong)
        val history = InstallHistoryStore.get(context)
        await { history.loaded }
        check(history.list().filterNot { it.token == token }.associateBy { it.id } == baseline)
        val interrupted = history.list().single { it.token == token }
        check(interrupted.terminal && (abort || interrupted.interrupted && interrupted.result == C.STAGE_CANCELLED))
        val done = CountDownLatch(1)
        var removed = false
        history.remove(owned.id) { success -> removed = success; done.countDown() }
        check(done.await(10, TimeUnit.SECONDS) && removed)
        check(readHistory(historyFile).associateBy { it.id } == baseline)
        val recoveryDone = CountDownLatch(1)
        InstallNotifications.remove(context, token)
        InstallRecoveryPersistence.remove(context, token) { recoveryDone.countDown() }
        check(recoveryDone.await(10, TimeUnit.SECONDS))
        saved.getAsJsonArray("ownedStaging").map { it.asString }.forEach { name ->
            check(name.matches(Regex("[A-Za-z0-9._-]{1,128}")))
            val root = PackageStaging.root(context).canonicalFile
            val child = File(root, name)
            check(child.canonicalFile == child.absoluteFile && child.parentFile == root)
            check(!child.exists() || child.deleteRecursively())
        }
        check(preferences.all == mapOf("default_interaction" to C.INTERACTION_NOTIFICATION))
        check(preferences.edit().remove("default_interaction").commit())
        if (!saved["preferenceExisted"].asBoolean) {
            check(!File(preferenceFile.path + ".bak").exists())
            check(!preferenceFile.exists() || preferenceFile.delete())
        }
        if (!saved["historyExisted"].asBoolean && baseline.isEmpty()) check(!historyFile.exists() || historyFile.delete())
        saved.addProperty("phase", "cleaned"); saved.addProperty("cleanupPid", Process.myPid())
        saved.addProperty("aborted", abort)
        saved.addProperty("interruptedWithoutReplay", !abort && interrupted.interrupted); saved.addProperty("otherHistoryUnchanged", true)
        write(journal, saved)
    }

    /** Same-process death only, through the pre-existing DUMP-protected Activity. No installation. */
    fun verifyDeathPoint(context: Context, nonce: String, directory: File, expectedPid: Int) {
        check(UUID.fromString(nonce).toString() == nonce && expectedPid == Process.myPid())
        val saved = JsonParser.parseString(AtomicFile(File(directory, "notification.json")).openRead().bufferedReader().use { it.readText() }).asJsonObject
        check(saved["kind"].asString == "notification-source-process-death" && saved["nonce"].asString == nonce &&
            saved["uid"].asInt == Process.myUid() && saved["seedPid"].asInt == expectedPid && saved["phase"].asString == "ready" &&
            saved["seedStartTicks"].asString == processStartTicks() &&
            saved["packageName"].asString == context.packageName && saved["fingerprint"].asString == Build.FINGERPRINT)
        val token = saved["token"].asString
        val owner = requireNotNull(InstallPresentation.find(token))
        check(InstallPresentation.snapshots(true).map { it.token } == listOf(token) && owner.notificationMode && !owner.snapshot().terminal &&
            owner.snapshot().prompt?.metadata?.packageName == FIXTURE && context.packageManager.packageInstaller.mySessions.isEmpty())
        val baselineFile = File(directory, "notification-history-before.json")
        check(digest(baselineFile.readBytes()) == saved["historySha256"].asString)
        val baseline = InstallHistoryCodec.decode(baselineFile.readBytes()).associateBy { it.id }
        val current = readHistory(File(context.noBackupFilesDir, "installation-history/history.json"))
        check(current.filterNot { it.token == token }.associateBy { it.id } == baseline && current.count { it.token == token } == 1)
    }

    fun recordDeathControlRemoved(directory: File) {
        val file = File(directory, "notification.json")
        val saved = JsonParser.parseString(AtomicFile(file).openRead().bufferedReader().use { it.readText() }).asJsonObject
        saved.addProperty("deathControlActivityDestroyed", true)
        saved.addProperty("deathControlTaskRemoved", true)
        write(file, saved)
    }

    private fun processStartTicks() = File("/proc/self/stat").readText().substringAfterLast(") ").trim().split(Regex("\\s+"))[19]

    private fun readHistory(file: File): List<InstallHistoryEntry> {
        check(!File(file.path + ".new").exists() && !File(file.path + ".bak").exists())
        return if (file.exists()) InstallHistoryCodec.decode(file.readBytes()) else emptyList()
    }
    @Suppress("DEPRECATION")
    private fun installed(context: Context) = try { context.packageManager.getPackageInfo(FIXTURE, PackageManager.MATCH_UNINSTALLED_PACKAGES); true }
        catch (_: PackageManager.NameNotFoundException) { false }
    private fun write(file: File, value: JsonObject) {
        val atomic = AtomicFile(file); val output = atomic.startWrite()
        try { output.write(value.toString().toByteArray()); output.fd.sync(); atomic.finishWrite(output) }
        catch (failure: Exception) { atomic.failWrite(output); throw failure }
    }
    private fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun await(condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 10_000
        while (!condition()) { check(SystemClock.elapsedRealtime() < deadline) { "Notification death probe did not settle" }; SystemClock.sleep(50) }
    }
}
