package io.github.supermonster003.autojs6.plugin.three.setup.installer.priv

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.os.Process
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import android.util.Xml
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.RootPersistentDefaultProtocol
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.HiddenApiAccess
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.PackageManagerHidden
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.StringReader
import java.io.StringWriter
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

/** Fixed, non-exported app_process entry. Its caller must launch it as UID/GID 1000 already. */
object RootSystemDefaultMain {
    private const val PACKAGE = RootPersistentDefaultProtocol.PACKAGE
    private val COMPONENT = ComponentName(PACKAGE, "$PACKAGE.ui.ExternalInstallActivity")
    private const val DESCRIPTOR = "android.content.pm.IPackageManager"
    private const val USER = 0
    private const val MAX_XML = 4 * 1024 * 1024
    private val combinations = PackageManagerHidden.INSTALL_ACTIONS.flatMap { action -> listOf("content", "file").map { action to it } }

    @JvmStatic fun main(args: Array<String>) {
        var token = ""
        var success = false
        var transactionStarted = false
        var terminalSent = false
        val output = FileOutputStream(FileDescriptor.out)
        fun emit(type: String, body: JsonObject.() -> Unit = {}) {
            val packet = JsonObject().apply { addProperty("type", type); addProperty("token", token); body() }
            val bytes = (packet.toString() + "\n").toByteArray()
            check(bytes.size <= RootPersistentDefaultProtocol.MAX_OUTPUT)
            output.write(bytes); output.flush(); output.fd.sync()
        }
        try {
            check(args.size == 2)
            token = args[1].also(RootPersistentDefaultProtocol::token)
            val operation = RootPersistentDefaultProtocol.Operation.entries.single { it.value == args[0] }
            check(Process.myUid() == 1000 && Os.getuid() == 1000 && Os.getgid() == 1000) { "Root/system identity is unavailable" }
            val manager = PackageManagerHidden()
            val appUid = manager.packageUid(PACKAGE, USER)
            // Only pre-opened regular private files are supported. A su implementation that
            // proxies stdin/stdout as a pipe must fail before any policy mutation.
            for (descriptor in listOf(FileDescriptor.`in`, FileDescriptor.out, FileDescriptor.err)) {
                val stat = Os.fstat(descriptor)
                check(OsConstants.S_ISREG(stat.st_mode) && stat.st_uid == appUid) { "The Root manager did not preserve private file descriptors" }
            }
            val processStat = File("/proc/self/stat").readText().substringAfterLast(')').trim().split(Regex("\\s+"))
            emit("hello") {
                addProperty("uid", Process.myUid()); addProperty("gid", Os.getgid())
                addProperty("pid", Process.myPid()); addProperty("startTicks", processStat[19].toLong())
                addProperty("selinuxContext", File("/proc/self/attr/current").readText().trim().trimEnd('\u0000'))
            }
            // stderr is a caller-owned lock file, shared by all Root requests for this plugin.
            // This is an OS lock, so a prior orphan cannot race a new Root policy transaction.
            FileOutputStream(FileDescriptor.err).channel.use { channel ->
                val lock = channel.tryLock() ?: error("Another Root/system default operation is still active")
                lock.use {
                    val control = Control(token, FileInputStream(FileDescriptor.`in`))
                    val cancelled = Thread { control.shutdown.set(true) }
                    Runtime.getRuntime().addShutdownHook(cancelled)
                    try {
                        val transaction = Transaction(manager, operation, control)
                        transactionStarted = true
                        val result = transaction.execute { before, writes ->
                            emit("prepared") {
                                addProperty("willWrite", writes); addProperty("baselineOwnedFilters", before.owned.size)
                                addProperty("preferredHash", before.preferredHash); addProperty("otherPersistentHash", before.otherHash)
                            }
                            control.awaitCommit()
                        }
                        success = result.success
                        emit("done") {
                            addProperty("success", result.success); addProperty("settled", result.settled)
                            addProperty("persistentMatches", result.persistentMatches)
                            addProperty("resolvedMatches", result.resolvedMatches)
                            addProperty("ownedFilters", result.ownedFilters)
                            addProperty("code", result.code); addProperty("message", result.message.take(500))
                        }
                        terminalSent = true
                    } finally { runCatching { Runtime.getRuntime().removeShutdownHook(cancelled) } }
                }
            }
        } catch (failure: Throwable) {
            if (!terminalSent) {
                success = false
                runCatching { emit("done") {
                    addProperty("success", false); addProperty("settled", !transactionStarted)
                    addProperty("code", InstallerErrorCodes.AUTHORIZER_UNAVAILABLE)
                    addProperty("message", failure.message.orEmpty().take(500))
                } }
            }
        }
        exitProcess(if (success) 0 else 1)
    }

    private class Control(private val token: String, private val input: FileInputStream) {
        val shutdown = AtomicBoolean()
        private val deadline = SystemClock.elapsedRealtime() + 8_000
        private val data = StringBuilder()
        private var committed = false
        fun check() {
            while (input.available() > 0) {
                data.append(input.read().toChar())
                require(data.length <= 160) { "Invalid control stream" }
            }
            val complete = data.toString().substringBeforeLast('\n', "").lineSequence().filter(String::isNotEmpty)
            for (line in complete) when (line) {
                "commit:$token" -> committed = true
                "cancel:$token" -> shutdown.set(true)
                else -> error("Unknown control message")
            }
            if (shutdown.get()) throw Stopped(InstallerErrorCodes.CANCELLED, "Root persistent-default request cancelled")
            if (SystemClock.elapsedRealtime() >= deadline) throw Stopped(InstallerErrorCodes.TIMEOUT, "Root/system operation timed out")
        }
        fun awaitCommit() {
            while (!committed) { check(); if (!committed) Thread.sleep(10) }
            check()
        }
    }

    private class Stopped(val code: String, message: String) : RuntimeException(message)
    private data class Result(val success: Boolean, val settled: Boolean, val persistentMatches: Int = 0,
        val resolvedMatches: Int = -1, val ownedFilters: Int = 0, val code: String = "", val message: String = "")

    private class Transaction(private val manager: PackageManagerHidden, private val operation: RootPersistentDefaultProtocol.Operation, private val control: Control) {
        private val remote = HiddenApiAccess.service("package", DESCRIPTOR)
        fun execute(prepare: (Snapshot, Boolean) -> Unit): Result {
            var before: Snapshot? = null
            var attempted = false
            try {
                control.check()
                val initial = snapshot().also { before = it }
                val writes = RootPersistentDefaultProtocol.changes(operation, initial.ownedKeys(), initial.competing)
                if (operation == RootPersistentDefaultProtocol.Operation.SET) {
                    check(combinations.all { (action, scheme) -> manager.query(PackageManagerHidden.intent(action, scheme), USER).any {
                        it.activityInfo.enabled && it.activityInfo.exported && ComponentName(it.activityInfo.packageName, it.activityInfo.name) == COMPONENT
                    } }) { "The plugin APK entry is not available" }
                }
                prepare(initial, writes)
                control.check()
                check(initial.same(snapshot())) { "Persistent defaults changed before confirmation" }
                if (writes) {
                    attempted = true
                    when (operation) {
                        RootPersistentDefaultProtocol.Operation.SET -> combinations.forEach { (action, scheme) ->
                            control.check()
                            manager.persistentPreferred(PackageManagerHidden.filter(action, scheme), COMPONENT, USER)
                        }
                        RootPersistentDefaultProtocol.Operation.CLEAR -> manager.clearPersistentPreferred(PACKAGE, USER)
                        RootPersistentDefaultProtocol.Operation.READ -> error("Read cannot mutate policy")
                    }
                }
                if (operation != RootPersistentDefaultProtocol.Operation.CLEAR) control.check()
                val after = snapshot()
                check(after.preferredHash == initial.preferredHash && after.otherHash == initial.otherHash) { "An unrelated policy changed during the request" }
                val persistent = after.persistentMatches()
                val resolved = if (operation == RootPersistentDefaultProtocol.Operation.CLEAR) -1 else resolvedMatches()
                when (operation) {
                    RootPersistentDefaultProtocol.Operation.SET -> check(after.ownedKeys()?.toSet() == RootPersistentDefaultProtocol.keys && persistent == 4 && resolved == 4) { "Persistent/default resolution was not 4/4" }
                    RootPersistentDefaultProtocol.Operation.CLEAR -> check(after.owned.isEmpty() && persistent == 0) { "Persistent plugin policy was not cleared" }
                    RootPersistentDefaultProtocol.Operation.READ -> Unit
                }
                return Result(true, true, persistent, resolved, after.owned.size)
            } catch (failure: Throwable) {
                var settled = !attempted
                if (attempted && before != null) {
                    val initial = requireNotNull(before)
                    runCatching {
                        val current = snapshot()
                        if (current.same(initial)) settled = true
                        else if (operation == RootPersistentDefaultProtocol.Operation.SET &&
                            RootPersistentDefaultProtocol.canRollbackNew(initial.owned.size, current.ownedKeys(),
                                current.preferredHash == initial.preferredHash && current.otherHash == initial.otherHash)) {
                            if (current.owned.isNotEmpty()) manager.clearPersistentPreferred(PACKAGE, USER)
                            check(snapshot().same(initial)) { "Partial Root policy rollback could not be verified" }
                            settled = true
                        }
                    }
                }
                return Result(false, settled, code = (failure as? Stopped)?.code ?: InstallerErrorCodes.AUTHORIZER_UNAVAILABLE,
                    message = failure.message.orEmpty() + if (settled) "" else "; the policy result is unverified and its audit was retained")
            }
        }
        private fun resolvedMatches(): Int = combinations.count { (action, scheme) ->
            manager.resolve(PackageManagerHidden.intent(action, scheme), USER) == COMPONENT
        }
        private fun snapshot(): Snapshot {
            HiddenApiAccess.invoke(remote, HiddenApiAccess.method(DESCRIPTOR, "flushPackageRestrictionsAsUser", Integer.TYPE), USER)
            val file = File("/data/system/users/0/package-restrictions.xml")
            check(file.isFile && file.length() in 1L..MAX_XML.toLong() && !File(file.path + ".bak").exists()) { "Persistent policy storage cannot be inspected" }
            val root = parse(file.readText())
            check(root.name == "package-restrictions")
            fun section(name: String): List<Node> = root.children.filter { it.name == name }.also { check(it.size == 1) }.single().children.also {
                check(it.all { item -> item.name == "item" })
            }
            return Snapshot(section("preferred-activities").map { it.xml() }.sorted(), section("persistent-preferred-activities").map(::record))
        }
    }

    private data class Record(val component: ComponentName, val filter: IntentFilter, val xml: String, val knownKey: String?)
    private data class Snapshot(val preferred: List<String>, val persistent: List<Record>) {
        val owned = persistent.filter { it.component.packageName == PACKAGE }
        val preferredHash = hash(preferred)
        val otherHash = hash(persistent.filter { it.component.packageName != PACKAGE }.map { it.xml }.sorted())
        val competing = persistent.any { it.component.packageName != PACKAGE && overlapsApk(it.filter) }
        fun same(other: Snapshot) = preferred == other.preferred && persistent.map { it.xml }.sorted() == other.persistent.map { it.xml }.sorted()
        fun persistentMatches() = combinations.count { (action, scheme) -> owned.any { it.component == COMPONENT && matches(it.filter, action, scheme) } }
        fun ownedKeys(): List<String>? {
            val values = owned.map { entry ->
                entry.knownKey ?: return null
            }
            return values
        }
    }
    /** Read-only classification shared by policy ownership checks and the guarded Debug fixture. */
    internal fun knownPolicyKey(component: ComponentName, filter: IntentFilter): String? {
        if (component != COMPONENT || filter.countActions() != 1 || filter.countCategories() != 1 ||
            !filter.hasCategory(Intent.CATEGORY_DEFAULT) || filter.countDataTypes() != 1 || filter.getDataType(0) != PackageManagerHidden.APK_MIME ||
            filter.countDataSchemes() != 1 || filter.countDataAuthorities() != 0 || filter.countDataPaths() != 0 ||
            filter.countDataSchemeSpecificParts() != 0 || filter.priority != 0) return null
        return "${filter.getAction(0)}/${filter.getDataScheme(0)}".takeIf { it in RootPersistentDefaultProtocol.keys }
    }
    /** readFromXml may ignore OEM/future fields; only the fixed platform representation is owned. */
    internal fun knownPolicyKeyFromXml(component: ComponentName, xml: String): String? {
        val node = parse(xml, policySectionsOnly = false)
        require(node.name == "filter")
        return knownPolicyKey(component, readFilter(node), node)
    }
    private fun knownPolicyKey(component: ComponentName, filter: IntentFilter, original: Node): String? {
        val key = knownPolicyKey(component, filter) ?: return null
        fun serialized(value: IntentFilter): Node {
            val writer = StringWriter()
            Xml.newSerializer().apply {
                setOutput(writer); startTag(null, "filter")
                value.writeToXml(this)
                endTag(null, "filter"); flush()
            }
            return parse(writer.toString(), policySectionsOnly = false)
        }
        // Preserve unknown fields/children even when this platform silently drops them on read.
        if (original != serialized(filter)) return null
        val fixed = PackageManagerHidden.filter(key.substringBefore('/'), key.substringAfter('/'))
        return key.takeIf { original == serialized(fixed) }
    }
    private fun matches(filter: IntentFilter, action: String, scheme: String): Boolean {
        val intent = PackageManagerHidden.intent(action, scheme)
        return filter.match(action, intent.type, scheme, intent.data, setOf(Intent.CATEGORY_DEFAULT), "RootPersistentDefault") >= 0
    }
    /** A provider/path-specific policy must not be missed merely because our probe URI differs. */
    internal fun overlapsApk(filter: IntentFilter): Boolean =
        filter.hasDataType(PackageManagerHidden.APK_MIME) &&
            PackageManagerHidden.INSTALL_ACTIONS.any(filter::hasAction) &&
            (filter.countDataSchemes() == 0 || listOf("content", "file").any(filter::hasDataScheme))
    private fun record(node: Node): Record {
        val component = requireNotNull(ComponentName.unflattenFromString(node.attributes.getValue("name")))
        val filterNode = node.children.filter { it.name == "filter" }.also { check(it.size == 1) }.single()
        val filter = readFilter(filterNode)
        val known = if (node.attributes.keys == setOf("name") && node.children.size == 1) {
            knownPolicyKey(component, filter, filterNode)
        } else null
        return Record(component, filter, node.xml(), known)
    }
    private fun readFilter(node: Node): IntentFilter {
        val parser = Xml.newPullParser().apply { setInput(StringReader(node.xml())) }
        while (parser.next() != XmlPullParser.START_TAG) Unit
        return IntentFilter().apply { readFromXml(parser) }
    }
    private fun hash(values: List<String>) = MessageDigest.getInstance("SHA-256").digest(values.joinToString("\n").toByteArray())
        .joinToString("") { "%02x".format(it) }

    private data class Node(val name: String, val attributes: Map<String, String>, val children: List<Node>) {
        fun xml(): String {
            val writer = StringWriter()
            val serializer = Xml.newSerializer().apply { setOutput(writer) }
            fun write(node: Node) {
                serializer.startTag(null, node.name)
                node.attributes.toSortedMap().forEach { (name, value) -> serializer.attribute(null, name, value) }
                node.children.forEach(::write)
                serializer.endTag(null, node.name)
            }
            write(this); serializer.flush(); return writer.toString()
        }
    }
    private fun parse(xml: String, policySectionsOnly: Boolean = true): Node {
        check(xml.length <= MAX_XML && !xml.contains("<!DOCTYPE", ignoreCase = true))
        val parser = Xml.newPullParser().apply { setInput(StringReader(xml)) }
        var nodes = 0
        fun skipUnrelated() {
            val depth = parser.depth
            while (true) {
                when (parser.next()) {
                    XmlPullParser.END_DOCUMENT -> error("Truncated package restriction XML")
                    XmlPullParser.END_TAG -> if (parser.depth == depth) return
                }
                check(parser.depth <= 128) { "Unrelated package restriction nesting exceeds its bound" }
            }
        }
        fun read(depth: Int): Node {
            check(++nodes <= 100_000 && depth <= 32)
            val name = parser.name
            val attributes = (0 until parser.attributeCount).associate { parser.getAttributeName(it) to parser.getAttributeValue(it) }
            val children = arrayListOf<Node>()
            while (true) when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    if (policySectionsOnly && depth == 1 && parser.name !in setOf("preferred-activities", "persistent-preferred-activities")) {
                        // Suspended-app extras may contain legitimate text/bundles. They are not
                        // policy records and must neither be interpreted nor copied into audits.
                        skipUnrelated()
                    } else children += read(depth + 1)
                }
                XmlPullParser.END_TAG -> return Node(name, attributes, children)
                XmlPullParser.END_DOCUMENT -> error("Truncated preference XML")
                XmlPullParser.TEXT -> check(parser.text.isBlank())
            }
        }
        while (parser.next() != XmlPullParser.START_TAG) check(parser.eventType != XmlPullParser.END_DOCUMENT)
        return read(1)
    }
}
