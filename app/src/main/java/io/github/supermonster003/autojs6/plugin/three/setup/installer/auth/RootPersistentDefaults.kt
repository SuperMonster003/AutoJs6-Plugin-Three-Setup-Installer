package io.github.supermonster003.autojs6.plugin.three.setup.installer.auth

import android.content.Context
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.system.Os
import android.util.AtomicFile
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import org.autojs.plugin.installer.api.InstallerErrorCodes
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** One bounded system-UID subprocess per request; the shared RootService never changes UID. */
internal object RootPersistentDefaults {
    private const val TIMEOUT = 20_000L
    private const val CLEANUP_GRACE = 3_000L

    @Synchronized fun set(context: Context, enable: Boolean, checkActive: () -> Unit): Int =
        execute(context, if (enable) RootPersistentDefaultProtocol.Operation.SET else RootPersistentDefaultProtocol.Operation.CLEAR, checkActive).let {
            if (enable) {
                check(it.persistentMatches == 4 && it.resolvedMatches == 4 && it.ownedFilters >= 4)
                4
            } else {
                check(it.persistentMatches == 0 && it.ownedFilters == 0)
                0
            }
        }

    /** Explicit privileged audit only; ordinary UI status reads must not start a Root shell. */
    @Synchronized fun read(context: Context, checkActive: () -> Unit): RootPersistentDefaultProtocol.State =
        execute(context, RootPersistentDefaultProtocol.Operation.READ, checkActive)

    private fun execute(context: Context, operation: RootPersistentDefaultProtocol.Operation, checkActive: () -> Unit): RootPersistentDefaultProtocol.State {
        check(Looper.myLooper() != Looper.getMainLooper()) { "Root persistent-default requests must run on a worker" }
        require(context.packageName == RootPersistentDefaultProtocol.PACKAGE)
        if (Process.myUid() / 100_000 != 0) throw InstallFailure(InstallerErrorCodes.INVALID_ARGUMENT, "Root persistent defaults currently require primary user 0")
        checkActive()
        val application = context.applicationContext
        val token = UUID.randomUUID().toString().replace("-", "")
        val root = File(application.noBackupFilesDir.canonicalFile, "root-persistent-default")
        check(root.canonicalFile == root && (root.isDirectory || root.mkdir()))
        val directory = File(root, token)
        check(directory.mkdir() && directory.canonicalFile == directory && Os.lstat(directory.path).st_uid == Process.myUid())
        val control = File(directory, "control")
        val output = File(directory, "output")
        val guard = File(root, "guard")
        check(control.createNewFile() && output.createNewFile())
        if (!guard.exists()) check(guard.createNewFile())
        check(guard.isFile && guard.canonicalFile == guard && Os.lstat(guard.path).st_uid == Process.myUid())
        val metadata = JsonObject().apply {
            addProperty("token", token); addProperty("operation", operation.value)
            addProperty("pending", true); addProperty("commitSent", false)
        }
        fun save() {
            val file = AtomicFile(File(directory, "state.json"))
            val stream = file.startWrite()
            try { stream.write(metadata.toString().toByteArray()); file.finishWrite(stream) }
            catch (error: Throwable) { file.failWrite(stream); throw error }
        }
        fun signal(value: String) = FileOutputStream(control, true).use {
            it.write("$value:$token\n".toByteArray()); it.fd.sync()
        }
        save()
        val cancelled = AtomicBoolean()
        var launcher: Future<Unit>? = null
        var identity: RootPersistentDefaultProtocol.Identity? = null
        var commitSent = false
        var completed = false
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT
        var consumed = 0
        fun packets(): List<JsonObject> {
            check(output.length() <= RootPersistentDefaultProtocol.MAX_OUTPUT) { "System bridge output exceeds its bound" }
            val text = output.readText()
            val completeLines = text.substringBeforeLast('\n', "").lineSequence().filter(String::isNotEmpty).toList()
            check(completeLines.size <= 8 && completeLines.size >= consumed)
            return completeLines.drop(consumed).map { RootPersistentDefaultProtocol.packet(it, token) }.also { consumed = completeLines.size }
        }
        fun done(packet: JsonObject): RootPersistentDefaultProtocol.State {
            check(identity != null) { "No verified system-UID handshake" }
            val success = packet["success"]?.asBoolean == true
            check(!success || commitSent) { "System bridge completed without an explicit commit" }
            val verified = if (success) RootPersistentDefaultProtocol.state(packet).also { value ->
                when (operation) {
                    RootPersistentDefaultProtocol.Operation.SET -> check(value.persistentMatches == 4 && value.resolvedMatches == 4 && value.ownedFilters >= 4)
                    RootPersistentDefaultProtocol.Operation.CLEAR -> check(value.persistentMatches == 0 && value.ownedFilters == 0)
                    RootPersistentDefaultProtocol.Operation.READ -> Unit
                }
            } else null
            metadata.add("result", packet.deepCopy())
            completed = success || packet["settled"]?.asBoolean == true
            metadata.addProperty("pending", !completed)
            save()
            if (!success) throw InstallFailure(packet["code"]?.asString?.takeIf {
                it in setOf(InstallerErrorCodes.CANCELLED, InstallerErrorCodes.TIMEOUT, InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, InstallerErrorCodes.INTERNAL)
            } ?: InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, packet["message"]?.asString?.take(500) ?: "Root/system persistent-default request failed")
            return requireNotNull(verified)
        }
        try {
            val command = RootPersistentDefaultProtocol.launch(application.applicationInfo.sourceDir, directory.path, operation, token)
            launcher = RootShellAccess.submit(application, { shell ->
                if (!shell.isRoot) throw InstallFailure(InstallerErrorCodes.AUTHORIZER_DENIED, "Root authorization was denied")
                if (cancelled.get() || Thread.currentThread().isInterrupted) throw InstallFailure(InstallerErrorCodes.CANCELLED, "Root persistent-default request cancelled before launch")
                val out = ArrayList<String>()
                val errors = ArrayList<String>()
                val result = shell.newJob().add(command).to(out, errors).exec()
                check(result.isSuccess && out.any { it.startsWith("ROOT_SYSTEM_LAUNCHED:") }) { "Could not launch the isolated Root/system bridge" }
            })
            while (true) {
                checkActive()
                if (SystemClock.elapsedRealtime() >= deadline) throw InstallFailure(InstallerErrorCodes.TIMEOUT, "Root/system persistent-default request timed out")
                if (launcher.isDone) launcher.get()
                for (packet in packets()) when (packet.get("type").asString) {
                    "hello" -> {
                        check(identity == null)
                        identity = RootPersistentDefaultProtocol.identity(packet)
                        metadata.add("identity", packet.deepCopy()); save()
                    }
                    "prepared" -> {
                        check(identity != null && !commitSent)
                        checkActive()
                        metadata.add("prepared", packet.deepCopy()); metadata.addProperty("commitSent", true); save()
                        commitSent = true
                        signal("commit") // A write/fsync failure can still have delivered the token.
                    }
                    "done" -> return done(packet)
                    else -> error("Unexpected system bridge packet")
                }
                Thread.sleep(25)
            }
        } catch (failure: Exception) {
            cancelled.set(true)
            val interrupted = Thread.interrupted() || failure is InterruptedException
            try {
                runCatching { signal("cancel") }
                launcher?.cancel(true)
                // The cancellation callback may have fired before this loop consumed hello.
                // Learn identity only; never send commit from the failure/cleanup path.
                for (packet in runCatching { packets() }.getOrDefault(emptyList())) {
                    if (packet.get("type")?.asString == "hello" && identity == null) {
                        runCatching { RootPersistentDefaultProtocol.identity(packet) }.onSuccess { identity = it }
                    } else if (packet.get("type")?.asString == "done" && identity != null) {
                        if (commitSent && packet["success"]?.asBoolean == true) return done(packet)
                        runCatching { done(packet) }
                    }
                }
                val cleanupDeadline = SystemClock.elapsedRealtime() + CLEANUP_GRACE
                while (identity != null && !completed && SystemClock.elapsedRealtime() < cleanupDeadline) {
                    for (packet in runCatching { packets() }.getOrDefault(emptyList())) {
                        if (packet.get("type")?.asString == "done") {
                            // A verified commit may have completed just before cancellation arrived.
                            if (commitSent && packet["success"]?.asBoolean == true) return done(packet)
                            runCatching { done(packet) }
                        }
                    }
                    if (!completed) SystemClock.sleep(25)
                }
                if (!completed && identity != null) {
                    val owned = requireNotNull(identity)
                    runCatching {
                        RootShellAccess.submit(application, { shell ->
                            check(shell.isRoot)
                            shell.newJob().add(RootPersistentDefaultProtocol.terminate(token, owned)).exec()
                            Unit
                        }).get(3, TimeUnit.SECONDS)
                    }.onFailure { failure.addSuppressed(it) }
                    metadata.addProperty("recoveryRequired", commitSent)
                }
                metadata.addProperty("pending", commitSent && !completed)
                metadata.addProperty("failure", failure.message.orEmpty().take(500))
                save()
            } finally { if (interrupted) Thread.currentThread().interrupt() }
            val actual = if (failure is ExecutionException) failure.cause ?: failure else failure
            throw if (actual is InstallFailure) actual else if (actual is InterruptedException) {
                InstallFailure(InstallerErrorCodes.CANCELLED, "Root persistent-default request cancelled", cause = actual)
            } else InstallFailure(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, "Root/system persistent defaults are unavailable: ${actual.message.orEmpty().take(300)}", cause = actual)
        } finally {
            // Keep uncertain, committed operations for diagnosis; never replay a policy mutation.
            if (!commitSent || completed) {
                listOf("control", "output", "state.json", "state.json.bak").forEach { name -> File(directory, name).let { if (it.exists()) it.delete() } }
                directory.delete()
            }
        }
    }
}

/** Closed command/protocol vocabulary, kept Android-free for JVM boundary tests. */
internal object RootPersistentDefaultProtocol {
    const val PACKAGE = "io.github.supermonster003.autojs6.plugin.three.setup.installer"
    const val ENTRY = "$PACKAGE.priv.RootSystemDefaultMain"
    const val MAX_OUTPUT = 64 * 1024L
    val keys = setOf("android.intent.action.VIEW/content", "android.intent.action.VIEW/file",
        "android.intent.action.INSTALL_PACKAGE/content", "android.intent.action.INSTALL_PACKAGE/file")
    enum class Operation(val value: String) { SET("set"), CLEAR("clear"), READ("read") }
    data class Identity(val pid: Int, val startTicks: Long)
    data class State(val persistentMatches: Int, val resolvedMatches: Int, val ownedFilters: Int)

    fun quote(value: String): String {
        require(value.isNotEmpty() && value.none { it < ' ' || it == '\u007f' })
        return "'" + value.replace("'", "'\"'\"'") + "'"
    }
    fun token(value: String) = require(value.matches(Regex("[0-9a-f]{32}"))) { "Invalid operation token" }
    fun launch(apk: String, directory: String, operation: Operation, id: String): String {
        token(id)
        require(apk.startsWith('/') && apk.endsWith(".apk"))
        val allowed = listOf("/data/user/0/$PACKAGE/no_backup/root-persistent-default/", "/data/data/$PACKAGE/no_backup/root-persistent-default/")
        require(allowed.any { directory == it + id })
        val guard = directory.substringBeforeLast('/') + "/guard"
        val child = "CLASSPATH=${quote(apk)} exec /system/bin/app_process /system/bin --nice-name=three_setup_default_$id $ENTRY ${operation.value} $id"
        return "( trap '' HUP; exec su 1000 -c ${quote(child)} 0<${quote("$directory/control")} 1>${quote("$directory/output")} 2<>${quote(guard)} ) & echo ROOT_SYSTEM_LAUNCHED:\$!"
    }
    fun terminate(id: String, identity: Identity): String {
        token(id); require(identity.pid > 1 && identity.startTicks > 0)
        val process = "/proc/${identity.pid}"
        return "if [ -r '$process/stat' ] && grep -q '^Uid:[[:space:]]*1000[[:space:]]' '$process/status' && " +
            "[ \"\$(tr '\\000' '\\n' < '$process/cmdline' | head -n 1)\" = 'three_setup_default_$id' ]; then " +
            "set -- \$(cat '$process/stat'); if [ \"\${22}\" = '${identity.startTicks}' ]; then kill -TERM ${identity.pid}; fi; fi"
    }
    fun packet(line: String, id: String): JsonObject {
        token(id); require(line.length <= MAX_OUTPUT)
        val value = JsonParser.parseString(line).asJsonObject
        require(value["token"]?.asString == id && value["type"]?.asString in setOf("hello", "prepared", "done"))
        return value
    }
    fun identity(packet: JsonObject): Identity {
        require(packet["type"].asString == "hello" && number(packet, "uid") == 1000L && number(packet, "gid") == 1000L)
        val pid = number(packet, "pid"); val ticks = number(packet, "startTicks")
        require(pid in 2L..Int.MAX_VALUE.toLong() && ticks > 0)
        return Identity(pid.toInt(), ticks)
    }
    fun state(packet: JsonObject): State {
        val persistent = number(packet, "persistentMatches")
        val resolved = number(packet, "resolvedMatches")
        val owned = number(packet, "ownedFilters")
        // A clear deliberately does not resolve ordinary defaults, which may still point here.
        require(persistent in 0..4 && resolved in -1..4 && owned in 0..100_000)
        return State(persistent.toInt(), resolved.toInt(), owned.toInt())
    }
    private fun number(packet: JsonObject, name: String): Long {
        val value = requireNotNull(packet[name]?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive)
        require(value.isNumber && value.asString.matches(Regex("-?(0|[1-9][0-9]{0,18})")))
        return requireNotNull(value.asString.toLongOrNull())
    }
    fun changes(operation: Operation, ownedKeys: List<String>?, competing: Boolean): Boolean {
        if (operation == Operation.READ) return false
        require(ownedKeys != null && ownedKeys.all { it in keys }) { "The existing plugin policy contains unknown filters" }
        if (operation == Operation.CLEAR) return ownedKeys.isNotEmpty()
        require(!competing) { "Another package has persistent APK policy" }
        require(ownedKeys.isEmpty() || ownedKeys.toSet() == keys) { "A partial plugin policy already exists and will be preserved" }
        return ownedKeys.isEmpty()
    }
    fun canRollbackNew(previousOwned: Int, currentKeys: List<String>?, unrelatedUnchanged: Boolean): Boolean =
        previousOwned == 0 && unrelatedUnchanged && currentKeys != null && currentKeys.all { it in keys } &&
            currentKeys.distinct().size == currentKeys.size
}
