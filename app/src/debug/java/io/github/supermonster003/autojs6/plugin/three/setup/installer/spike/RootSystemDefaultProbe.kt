package io.github.supermonster003.autojs6.plugin.three.setup.installer.spike

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.os.SystemClock
import android.system.Os
import android.util.AtomicFile
import android.util.Xml
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.HiddenApiAccess
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.PackageManagerHidden
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.io.StringReader
import java.io.StringWriter
import kotlin.system.exitProcess

/**
 * Q9 fixed API-24 spike, loaded from a Debug APK by an independent `su 1000 app_process`.
 * No manifest entry, installation, setuid call or shared RootService state is involved.
 * Only run/restore/inspect and a 32-digit run token are accepted; all targets are fixed.
 */
object RootSystemDefaultProbe {
    private const val PACKAGE = "io.github.supermonster003.autojs6.plugin.three.setup.installer"
    private val COMPONENT = ComponentName(PACKAGE, "$PACKAGE.ui.ExternalInstallActivity")
    private const val DESCRIPTOR = "android.content.pm.IPackageManager"
    private const val USER = 0
    private const val MAX_XML = 4 * 1024 * 1024
    private val ACTIONS = listOf(Intent.ACTION_VIEW, "android.intent.action.INSTALL_PACKAGE")
    private val SCHEMES = listOf("content", "file")

    @JvmStatic fun main(args: Array<String>) {
        var passed = false
        try {
            check(args.size == 2 && args[0] in setOf("run", "restore", "inspect") &&
                args[1].matches(Regex("[0-9a-f]{32}"))) { "Expected fixed operation and run token" }
            check(Process.myUid() == 1000 && Os.getuid() == 1000 && Os.getgid() == 1000) {
                "The probe must already run as system UID/GID 1000"
            }
            check(Build.VERSION.SDK_INT == 24 && Build.SUPPORTED_ABIS.first() == "x86") {
                "This audited spike is restricted to API 24 / x86"
            }
            val directory = File("/data/local/tmp/three-setup-root-system-${args[1]}/audit")
            check(directory.isDirectory && directory.canonicalPath == directory.absolutePath &&
                Os.lstat(directory.path).st_uid == 1000) { "The system-owned audit directory is missing or indirect" }
            passed = Audit(directory, args[1]).execute(args[0])
        } catch (failure: Throwable) {
            println(JSONObject().put("success", false).put("uid", Process.myUid()).put("error", cause(failure)))
        }
        exitProcess(if (passed) 0 else 1)
    }

    private class Audit(private val directory: File, private val token: String) {
        private val packageManager = PackageManagerHidden()
        private val remote = HiddenApiAccess.service("package", DESCRIPTOR)
        private val binder = Class.forName("android.os.ServiceManager").getMethod("getService", String::class.java)
            .invoke(null, "package") as IBinder
        private val journalFile = File(directory, "journal.json")
        private val deadline = SystemClock.elapsedRealtime() + 30_000
        private var snapshotIndex = 0

        fun execute(mode: String): Boolean {
            val identity = JSONObject().put("token", token).put("uid", Process.myUid()).put("gid", Os.getgid())
                .put("sdk", Build.VERSION.SDK_INT).put("abi", Build.SUPPORTED_ABIS.first())
                .put("selinuxContext", File("/proc/self/attr/current").readText().trim().trimEnd('\u0000'))
            val installed = requireNotNull(packageManager.packageInfo(PACKAGE, USER)) { "The fixed target is not installed" }
            identity.put("installedVersionCode", installed.versionCode)
            val state = if (mode == "restore") {
                readJournal().also { check(it.getString("token") == token && it.getString("package") == PACKAGE) }
            } else {
                check(!journalFile.exists() && !File(journalFile.path + ".bak").exists()) { "An earlier journal must be restored first" }
                val before = snapshot()
                assertSafeBaseline(before)
                for ((action, scheme) in combinations()) {
                    check(packageManager.query(PackageManagerHidden.intent(action, scheme), USER).any {
                        ComponentName(it.activityInfo.packageName, it.activityInfo.name) == COMPONENT
                    }) { "The fixed target does not match $action / $scheme" }
                }
                JSONObject().put("token", token).put("package", PACKAGE).put("userId", USER)
                    .put("identity", identity).put("baseline", before.json()).put("phase", "prepared")
                    .put("mutationAttempted", false).also { saveJournal(it) }
            }
            if (mode == "inspect") {
                state.put("phase", "inspected").put("restored", true)
                saveJournal(state)
                println(identity.put("success", true).put("inspectionOnly", true).put("baseline", state.getJSONObject("baseline")))
                return true
            }

            var failure: Throwable? = null
            var verified = false
            try {
                if (mode == "run") {
                    val before = Snapshot.fromJson(state.getJSONObject("baseline"))
                    check(snapshot().json().toString() == before.json().toString()) { "Baseline changed before mutation" }
                    assertSafeBaseline(before)
                    state.put("phase", "adding").put("mutationAttempted", true)
                    saveJournal(state) // Durable proof exists before the first possible write.
                    for ((action, scheme) in combinations()) {
                        checkTime()
                        packageManager.persistentPreferred(PackageManagerHidden.filter(action, scheme), COMPONENT, USER)
                    }
                    val locked = snapshot()
                    assertOwnedCurrent(before, locked, requireAll = true)
                    check(locked.resolved.all { it == COMPONENT.flattenToString() }) { "Persistent selection did not resolve 4/4" }
                    state.put("phase", "locked").put("locked", locked.json()).put("fourPersistentFiltersVerified", true)
                    saveJournal(state)
                    verified = true
                }
            } catch (error: Throwable) {
                failure = error
                state.put("operationError", cause(error))
            } finally {
                try {
                    restore(state)
                } catch (cleanup: Throwable) {
                    state.put("cleanupError", cause(cleanup)).put("restored", false)
                    failure?.addSuppressed(cleanup) ?: run { failure = cleanup }
                    saveJournal(state)
                }
            }
            val success = failure == null && state.optBoolean("restored") && (verified || mode == "restore")
            println(identity.put("success", success).put("fourPersistentFiltersVerified", verified)
                .put("restored", state.optBoolean("restored")).put("operationError", state.optString("operationError"))
                .put("cleanupError", state.optString("cleanupError")))
            return success
        }

        private fun restore(state: JSONObject) {
            val before = Snapshot.fromJson(state.getJSONObject("baseline"))
            assertSafeBaseline(before)
            val current = snapshot()
            assertOwnedCurrent(before, current, requireAll = false)
            val owned = current.persistent.map(::record).filter { it.component.packageName == PACKAGE }
            if (owned.isNotEmpty()) {
                check(state.getBoolean("mutationAttempted")) { "No saved mutation ownership" }
                packageManager.clearPersistentPreferred(PACKAGE, USER)
            }
            val after = snapshot()
            check(before.json().toString() == after.json().toString()) { "Complete preference/resolution baseline was not restored" }
            state.put("after", after.json()).put("phase", "restored").put("restored", true)
            saveJournal(state)
        }

        private fun assertSafeBaseline(value: Snapshot) {
            for (entry in value.preferred + value.persistent) {
                val record = record(entry)
                check(record.component.packageName != PACKAGE && !matchesApk(record.filter)) {
                    "A plugin or matching APK preference predates this probe"
                }
            }
        }

        private fun assertOwnedCurrent(before: Snapshot, current: Snapshot, requireAll: Boolean) {
            check(before.preferred == current.preferred) { "Ordinary preferred/last-chosen records changed" }
            val records = current.persistent.map(::record)
            check(records.filter { it.component.packageName != PACKAGE }.map { it.xml }.sorted() == before.persistent) {
                "Unrelated persistent records changed; no cleanup is safe"
            }
            val own = records.filter { it.component.packageName == PACKAGE }
            val keys = own.map { item ->
                check(item.component == COMPONENT)
                val filter = item.filter
                check(filter.countActions() == 1 && filter.getAction(0) in ACTIONS &&
                    filter.countCategories() == 1 && filter.hasCategory(Intent.CATEGORY_DEFAULT) &&
                    filter.countDataTypes() == 1 && filter.getDataType(0) == PackageManagerHidden.APK_MIME &&
                    filter.countDataSchemes() == 1 && filter.getDataScheme(0) in SCHEMES &&
                    filter.countDataAuthorities() == 0 && filter.countDataPaths() == 0 &&
                    filter.countDataSchemeSpecificParts() == 0 && filter.priority == 0) { "Unexpected owned persistent filter" }
                filter.getAction(0) to filter.getDataScheme(0)
            }
            check(keys.size == keys.distinct().size && keys.toSet().all { it in combinations() })
            if (requireAll) check(keys.toSet() == combinations().toSet()) { "The four persistent records were not saved" }
        }

        private fun snapshot(): Snapshot {
            checkTime()
            // API 24 has no full persistent-preference query. Ask the framework to flush its own
            // current state, then read only the preference sections; never edit this system file.
            HiddenApiAccess.invoke(remote, HiddenApiAccess.method(DESCRIPTOR, "flushPackageRestrictionsAsUser", Integer.TYPE), USER)
            val restrictions = File("/data/system/users/0/package-restrictions.xml")
            check(restrictions.isFile && restrictions.length() in 1L..MAX_XML.toLong())
            check(!File(restrictions.path + ".bak").exists()) { "Package restrictions are still being saved" }
            val tree = parse(restrictions.readText())
            val sections = tree.children.filter { it.name == "persistent-preferred-activities" }
            check(sections.size == 1) { "Persistent-preference section is missing or ambiguous" }
            val persistent = sections.single().children.map { check(it.name == "item"); it.xml() }.sorted()
            val preferred = dumpPreferred()
            val resolved = combinations().map { (action, scheme) ->
                packageManager.resolve(PackageManagerHidden.intent(action, scheme), USER)?.flattenToString().orEmpty()
            }
            check(preferred == dumpPreferred()) { "Resolution changed ordinary preferences during the audit" }
            return Snapshot(preferred, persistent, resolved)
        }

        private fun dumpPreferred(): List<String> {
            val file = File(directory, "preferred-${++snapshotIndex}-${SystemClock.elapsedRealtimeNanos()}.xml")
            check(file.createNewFile())
            FileOutputStream(file).use { output ->
                binder.dump(output.fd, arrayOf("preferred-xml", "--full"))
                output.fd.sync()
            }
            check(file.length() in 1L..MAX_XML.toLong())
            val root = parse(file.readText())
            check(root.name == "preferred-activities")
            return root.children.map { check(it.name == "item"); it.xml() }.sorted()
        }

        private fun saveJournal(value: JSONObject) {
            val file = AtomicFile(journalFile)
            val output = file.startWrite()
            try { output.write(value.toString(2).toByteArray()); file.finishWrite(output) }
            catch (failure: Throwable) { file.failWrite(output); throw failure }
        }

        private fun readJournal(): JSONObject = AtomicFile(journalFile).openRead().use {
            check(journalFile.length() <= MAX_XML * 2L)
            JSONObject(it.bufferedReader().readText())
        }

        private fun checkTime() = check(SystemClock.elapsedRealtime() <= deadline) { "The fixed probe deadline expired" }
    }

    private data class Snapshot(val preferred: List<String>, val persistent: List<String>, val resolved: List<String>) {
        fun json(): JSONObject = JSONObject().put("preferred", JSONArray(preferred)).put("persistent", JSONArray(persistent)).put("resolved", JSONArray(resolved))
        companion object {
            fun fromJson(value: JSONObject): Snapshot {
                fun list(name: String) = value.getJSONArray(name).let { array -> (0 until array.length()).map(array::getString) }
                return Snapshot(list("preferred"), list("persistent"), list("resolved")).also { check(it.resolved.size == 4) }
            }
        }
    }

    private data class Record(val xml: String, val component: ComponentName, val filter: IntentFilter)
    private fun record(xml: String): Record {
        val node = parse(xml)
        check(node.name == "item" && node.children.count { it.name == "filter" } == 1)
        val component = requireNotNull(ComponentName.unflattenFromString(node.attributes.getValue("name")))
        val parser = Xml.newPullParser().apply { setInput(StringReader(node.children.single { it.name == "filter" }.xml())) }
        while (parser.next() != XmlPullParser.START_TAG) Unit
        val filter = IntentFilter().apply { readFromXml(parser) }
        return Record(xml, component, filter)
    }

    private fun matchesApk(filter: IntentFilter): Boolean = combinations().any { (action, scheme) ->
        val intent = PackageManagerHidden.intent(action, scheme)
        filter.match(action, intent.type, scheme, intent.data, setOf(Intent.CATEGORY_DEFAULT), "RootSystemDefaultProbe") >= 0
    }

    private fun combinations() = ACTIONS.flatMap { action -> SCHEMES.map { action to it } }

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
            write(this)
            serializer.flush()
            return writer.toString()
        }
    }

    private fun parse(xml: String): Node {
        check(xml.length <= MAX_XML && !xml.contains("<!DOCTYPE", ignoreCase = true))
        val parser = Xml.newPullParser().apply { setInput(StringReader(xml)) }
        var nodes = 0
        fun read(depth: Int): Node {
            check(++nodes <= 100_000 && depth <= 32)
            val name = parser.name
            val attributes = (0 until parser.attributeCount).associate { parser.getAttributeName(it) to parser.getAttributeValue(it) }
            val children = arrayListOf<Node>()
            while (true) {
                when (parser.next()) {
                    XmlPullParser.START_TAG -> children += read(depth + 1)
                    XmlPullParser.END_TAG -> return Node(name, attributes, children)
                    XmlPullParser.END_DOCUMENT -> error("Truncated preference XML")
                    XmlPullParser.TEXT -> check(parser.text.isBlank()) { "Unexpected preference XML text" }
                }
            }
        }
        while (parser.next() != XmlPullParser.START_TAG) check(parser.eventType != XmlPullParser.END_DOCUMENT)
        return read(1)
    }

    private fun cause(failure: Throwable): String = generateSequence(failure) { it.cause }.take(5)
        .joinToString(" <- ") { "${it.javaClass.name}: ${it.message.orEmpty().take(500)}" }
}
