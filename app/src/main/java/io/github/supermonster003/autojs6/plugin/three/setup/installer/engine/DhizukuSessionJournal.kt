package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import android.content.Context
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.util.AtomicFile
import android.util.Log
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ThreeSetupInstallerPlugin
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.DhizukuFramework
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.DhizukuOwnerIdentity
import java.io.Closeable
import java.io.File
import java.util.UUID

/**
 * Only returned, durably recorded IDs can be recovered. A private nonce in originatingUri proves
 * the session alongside its owner/signature/user/package/size. No APK or source URI is recorded.
 * API 26/27 do not expose that nonce in SessionInfo; unmatched sessions are retained, never guessed.
 */
internal class DhizukuSessionJournal(context: Context) {
    private val directory = File(context.noBackupFilesDir, "dhizuku-install-sessions")

    data class Snapshot(val id: Int, val installerPackage: String?, val packageName: String?, val size: Long,
        val origin: String?, val userId: Int?, val createdMillis: Long?)

    data class Record(val token: String, val id: Int, val owner: DhizukuOwnerIdentity,
        val packageName: String, val size: Long, val createdMillis: Long?,
        val creatorPid: Int, val creatorStartTicks: String) {
        val origin: String get() = originatingUri(token)

        fun matches(currentOwner: DhizukuOwnerIdentity, actual: Snapshot): Boolean =
            owner == currentOwner && id == actual.id && owner.packageName == actual.installerPackage &&
                packageName == actual.packageName && size == actual.size && origin == actual.origin &&
                (actual.userId == null || owner.userId == actual.userId) &&
                (createdMillis == null || createdMillis == actual.createdMillis)
    }

    data class Recovery(val abandoned: Int, val absent: Int, val active: Int, val retained: Int)

    inner class Lease internal constructor(val record: Record) : Closeable {
        private var closed = false
        fun completed() = forgetQuietly(record)
        fun abandon(framework: DhizukuFramework) {
            framework.checked { framework.installer.abandonSession(record.id) }
            forgetQuietly(record)
        }
        override fun close() {
            synchronized(lock) { if (!closed) { closed = true; activeTokens.remove(record.token) } }
        }
    }

    /** Register activity before creation, then save the returned ID before any APK stream opens. */
    fun create(framework: DhizukuFramework, params: PackageInstaller.SessionParams,
        packageName: String, size: Long): Lease {
        require(packageName.isNotBlank() && size > 0)
        val token = UUID.randomUUID().toString()
        val creatorPid = Process.myPid()
        val startTicks = checkNotNull(processStartTicks(creatorPid)) { "Cannot identify the Dhizuku session creator" }
        synchronized(lock) {
            check((knownTokens() + activeTokens).size < MAX_RECORDS) { "Too many pending Dhizuku recovery records" }
            check(!file(token).exists())
            activeTokens.add(token)
        }
        params.setOriginatingUri(Uri.parse(originatingUri(token)))
        var id: Int? = null
        var record: Record? = null
        try {
            val returnedId = framework.checked { framework.installer.createSession(params) }
            id = returnedId
            val snapshot = snapshot(framework, returnedId) ?: error("The newly created Dhizuku session is missing")
            val owned = Record(token, returnedId, framework.identity, packageName, size, snapshot.createdMillis, creatorPid, startTicks)
            record = owned
            check(snapshot.installerPackage == framework.owner.packageName && snapshot.packageName == packageName &&
                (Build.VERSION.SDK_INT < 27 || snapshot.size == size))
            if (Build.VERSION.SDK_INT >= 28) check(owned.matches(framework.identity, snapshot)) {
                "The newly created Dhizuku session cannot prove its ownership"
            }
            synchronized(lock) { write(owned) }
            return Lease(owned)
        } catch (failure: Exception) {
            id?.let { ownedId ->
                runCatching { framework.checked { framework.installer.abandonSession(ownedId) } }
                    .onSuccess { record?.let(::forgetQuietly) }.onFailure(failure::addSuppressed)
            }
            synchronized(lock) { activeTokens.remove(token) }
            throw failure
        }
    }

    /** Never scans the owner's session list; query only IDs from the plugin's private journal. */
    fun recover(framework: DhizukuFramework): Recovery = synchronized(lock) {
        var abandoned = 0; var absent = 0; var active = 0; var retained = 0
        for (token in knownTokens()) {
            if (token in activeTokens) { active++; continue }
            val record = runCatching { read(token) }.getOrNull()
            if (record == null || record.owner != framework.identity) { retained++; continue }
            // Production operations share a process, but a separate same-UID process must not
            // reclaim another live process's handle. Ambiguous process identity is retained.
            if (record.creatorPid != Process.myPid() && creatorMayBeAlive(record)) { active++; continue }
            val actual = snapshot(framework, record.id)
            if (actual == null) {
                forgetQuietly(record); absent++; continue
            }
            if (!record.matches(framework.identity, actual)) { retained++; continue }
            framework.checked { framework.installer.abandonSession(record.id) }
            forgetQuietly(record)
            abandoned++
        }
        Recovery(abandoned, absent, active, retained)
    }

    internal fun records(): List<Record> = synchronized(lock) { knownTokens().mapNotNull { runCatching { read(it) }.getOrNull() } }

    private fun snapshot(framework: DhizukuFramework, id: Int): Snapshot? = framework.checked {
        framework.installer.getSessionInfo(id)?.let { info ->
            // API 26 can create a live lease, but cannot read back its size or origin proof.
            // The missing size remains unequal to every valid record during later recovery.
            Snapshot(info.sessionId, info.installerPackageName, info.appPackageName,
                if (Build.VERSION.SDK_INT >= 27) info.size else -1L,
                if (Build.VERSION.SDK_INT >= 28) info.originatingUri?.toString() else null,
                if (Build.VERSION.SDK_INT >= 29) {
                    if (info.user == UserHandle.getUserHandleForUid(framework.identity.uid)) framework.identity.userId else -1
                } else null,
                if (Build.VERSION.SDK_INT >= 30) info.createdMillis.takeIf { it > 0 } else null)
        }
    }

    private fun knownTokens(): Set<String> = directory.listFiles().orEmpty().mapNotNull { entry ->
        val name = entry.name.removeSuffix(".bak").removeSuffix(".new")
        name.takeIf { it.endsWith(".json") }?.removeSuffix(".json")?.takeIf(::validToken)
    }.toSet()

    private fun file(token: String): File {
        require(validToken(token))
        return File(directory, "$token.json").also {
            check(it.canonicalFile.parentFile == directory.canonicalFile) { "Recovery journal escaped its directory" }
        }
    }

    private fun read(token: String): Record {
        val bytes = AtomicFile(file(token)).openRead().use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                check(output.size() + count <= MAX_BYTES) { "Recovery record exceeds its limit" }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        return decode(bytes.toString(Charsets.UTF_8)).also { check(it.token == token) { "Recovery token does not match its file" } }
    }

    private fun write(record: Record) {
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create Dhizuku recovery directory" }
        val bytes = encode(record).toByteArray(Charsets.UTF_8)
        check(bytes.size <= MAX_BYTES)
        val atomic = AtomicFile(file(record.token))
        val stream = atomic.startWrite()
        try { stream.write(bytes); stream.fd.sync(); atomic.finishWrite(stream) }
        catch (failure: Exception) { atomic.failWrite(stream); throw failure }
    }

    private fun forgetQuietly(record: Record) = synchronized(lock) {
        runCatching {
            if (record.token in knownTokens()) {
                check(read(record.token) == record) { "Recovery record changed" }
                AtomicFile(file(record.token)).delete()
            }
        }.onFailure { Log.w(TAG, "Could not retire known Dhizuku session ${record.id}") }
        Unit
    }

    companion object {
        private const val TAG = "DhizukuSessionJournal"
        private const val MAX_BYTES = 16 * 1024
        private const val MAX_RECORDS = 64
        private val lock = Any()
        private val activeTokens = mutableSetOf<String>()
        private val keys = setOf("schema", "token", "id", "owner", "ownerPackage", "ownerUid", "user",
            "provider", "signingSha256", "package", "size", "createdMillis", "creatorPid", "creatorStartTicks")

        private fun processStartTicks(pid: Int): String? = runCatching {
            File("/proc/$pid/stat").readText().substringAfterLast(") ").split(' ')[19]
                .takeIf { it.matches(Regex("[0-9]{1,22}")) }
        }.getOrNull()

        private fun creatorMayBeAlive(record: Record): Boolean {
            processStartTicks(record.creatorPid)?.let { return it == record.creatorStartTicks }
            return try { Os.kill(record.creatorPid, 0); true } catch (failure: ErrnoException) {
                failure.errno != OsConstants.ESRCH
            }
        }

        private fun validToken(value: String) = runCatching { UUID.fromString(value).toString() == value }.getOrDefault(false)
        fun originatingUri(token: String): String {
            require(validToken(token))
            return "android-app://${ThreeSetupInstallerPlugin.PACKAGE_NAME}/dhizuku-session/$token"
        }

        internal fun encode(record: Record): String = JsonObject().apply {
            addProperty("schema", 1); addProperty("token", record.token); addProperty("id", record.id)
            addProperty("owner", record.owner.component); addProperty("ownerPackage", record.owner.packageName)
            addProperty("ownerUid", record.owner.uid); addProperty("user", record.owner.userId)
            addProperty("provider", record.owner.provider); addProperty("signingSha256", record.owner.signingSha256)
            addProperty("package", record.packageName); addProperty("size", record.size)
            addProperty("creatorPid", record.creatorPid); addProperty("creatorStartTicks", record.creatorStartTicks)
            add("createdMillis", record.createdMillis?.let { com.google.gson.JsonPrimitive(it) } ?: JsonNull.INSTANCE)
        }.toString()

        internal fun decode(json: String): Record {
            require(json.toByteArray(Charsets.UTF_8).size <= MAX_BYTES)
            val root = RequestDocuments.parseObject(json, "Dhizuku recovery record")
            require(root.keySet() == keys)
            fun number(key: String): Long = root.get(key).let {
                require(it.isJsonPrimitive && it.asJsonPrimitive.isNumber); it.asBigDecimal.longValueExact()
            }
            fun integer(key: String): Int = number(key).also { require(it in 0..Int.MAX_VALUE.toLong()) }.toInt()
            fun text(key: String): String = root.get(key).let {
                require(it.isJsonPrimitive && it.asJsonPrimitive.isString)
                it.asString.also { value -> require(value.isNotBlank() && value.length <= 512 && value.none(Char::isISOControl)) }
            }
            require(number("schema") == 1L)
            val token = text("token"); require(validToken(token))
            val owner = DhizukuOwnerIdentity(text("owner"), text("ownerPackage"), integer("ownerUid"), integer("user"), text("provider"), text("signingSha256"))
            require(owner.uid / 100000 == owner.userId && owner.component.startsWith(owner.packageName + "/") &&
                owner.provider.startsWith(owner.packageName + "/") && owner.signingSha256.matches(Regex("[0-9a-f]{64}")))
            val id = integer("id"); require(id > 0)
            val size = number("size"); require(size > 0)
            val created = if (root.get("createdMillis").isJsonNull) null else number("createdMillis").also { require(it > 0) }
            val pid = integer("creatorPid"); require(pid > 0)
            val ticks = text("creatorStartTicks"); require(ticks.matches(Regex("[0-9]{1,22}")))
            return Record(token, id, owner, text("package"), size, created, pid, ticks)
        }
    }
}
