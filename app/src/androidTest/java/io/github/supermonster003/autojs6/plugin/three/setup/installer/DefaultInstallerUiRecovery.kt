package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.Process
import android.util.AtomicFile
import android.util.Xml
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.PackageManagerHidden
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.io.StringReader
import java.io.StringWriter
import java.security.MessageDigest
import java.util.UUID

/**
 * The default-UI audit never changes preferences and only starts without existing APK defaults or
 * any preferred activity owned by this plugin. Persist this fact before opening the UI: even a
 * native renderer/process crash can then be recovered without opening a window or a privileged
 * service. Last-chosen entries (always=false), omitted by some public framework implementations,
 * are protected by an additional read-only shell snapshot.
 */
internal object DefaultInstallerUiRecovery {
    private const val RELATIVE_PATH = "p5-default-installer/restore-plan.json"
    private const val MAX_BYTES = 8 * 1024 * 1024
    private const val COMPONENT_SUFFIX = ".ui.ExternalInstallActivity"
    private const val APPROVED_INSTALLER_X = "com.rosan.installer.x.revived"
    private const val APPROVED_INSTALLER_X_COMPONENT = "$APPROVED_INSTALLER_X/com.rosan.installer.ui.activity.InstallerActivity"
    private const val PARTIAL_CLEAR_AUDIT_ID = "9808cd47-4af6-4eca-a2ac-43f0a9a00d93"
    private const val PARTIAL_CLEAR_AUDIT_PATH = "p5-default-installer/approved-installerx-clear-$PARTIAL_CLEAR_AUDIT_ID.json"
    private const val APPROVED_PLUGIN_HISTORY_KIND = "preserve-qv710af65f-plugin-history-2026-10-02"

    val probes: List<Intent> get() = PackageManagerHidden.INSTALL_ACTIONS.flatMap { action ->
        listOf("content", "file").map { PackageManagerHidden.intent(action, it) }
    }

    data class Plan(val runId: String, val internalPath: String, val readableCopy: String?, val continuePath: String)
    data class CompletedRun(val runId: String, val startedAt: Long, val restoredAt: Long)
    data class ApprovedClear(val internalPath: String, val readableCopy: String?)
    class PreservedLastChosen internal constructor(
        val proof: JsonObject,
        internal val publicEntries: Set<String>,
        internal val shellEntries: Set<String>,
    )
    data class Preferred(val component: ComponentName, val filter: IntentFilter, val canonical: String, val always: Boolean? = null)
    data class Snapshot(
        val publicEntries: List<Preferred>,
        val shellEntries: List<Preferred>,
        val preferences: JsonObject,
        val resolved: List<String?>,
    ) {
        fun document() = JsonObject().apply {
            add("publicPreferred", strings(publicEntries.map { it.canonical }.sorted()))
            add("shellPreferred", strings(shellEntries.map { it.canonical }.sorted()))
            add("preferences", preferences.deepCopy())
            add("resolved", JsonArray().apply { resolved.forEach { if (it == null) add(JsonNull.INSTANCE) else add(it) } })
        }
    }

    @Suppress("DEPRECATION")
    fun snapshot(instrumentation: Instrumentation): Snapshot {
        val context = instrumentation.targetContext
        val filters = arrayListOf<IntentFilter>()
        val activities = arrayListOf<ComponentName>()
        context.packageManager.getPreferredActivities(filters, activities, null)
        check(filters.size == activities.size) { "The public preferred-activity snapshot is incomplete" }
        val publicEntries = filters.mapIndexed { index, filter ->
            val encoded = StringWriter().also { writer ->
                Xml.newSerializer().apply {
                    setOutput(writer); startTag(null, "filter"); filter.writeToXml(this); endTag(null, "filter"); flush()
                }
            }.toString()
            val canonical = JsonObject().apply {
                addProperty("component", activities[index].flattenToString())
                add("filter", parseXml(encoded).document())
            }.toString()
            Preferred(activities[index], filter, canonical)
        }
        val xml = ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("dumpsys package preferred-xml --full")
        ).use { boundedRead(it).toString(Charsets.UTF_8) }
        val document = parseXml(xml)
        check(document.name == "preferred-activities") { "Cannot inspect all preferred and last-chosen activities: ${document.name}" }
        val shellEntries = document.children.map { item ->
            check(item.name == "item") { "Unexpected preferred-activity XML element" }
            val component = checkNotNull(ComponentName.unflattenFromString(checkNotNull(item.attributes["name"])))
            val filter = item.children.single { it.name == "filter" }.toFilter()
            val always = when (item.attributes["always"]) {
                "true" -> true
                "false" -> false
                else -> error("Cannot safely inspect a preferred activity without its always flag")
            }
            Preferred(component, filter, item.document().toString(), always)
        }
        val resolved = probes.map { intent -> context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.let { ComponentName(it.packageName, it.name).flattenToString() } }
        return Snapshot(publicEntries, shellEntries, preferenceSnapshot(context), resolved)
    }

    fun unsafeReason(context: Context, snapshot: Snapshot, preserved: PreservedLastChosen? = null): String? {
        val entries = snapshot.publicEntries + snapshot.shellEntries
        if (snapshot.publicEntries.any { it.component.packageName == context.packageName && it.canonical !in preserved?.publicEntries.orEmpty() } ||
            snapshot.shellEntries.any { it.component.packageName == context.packageName &&
                (it.always != false || it.canonical !in preserved?.shellEntries.orEmpty()) }) {
            return "Existing plugin preferred/last-chosen activities must remain untouched; use a clean device"
        }
        val protectedEntries = if (preserved == null) entries else {
            // Only exact records proved to be last-chosen by the complete shell snapshot may
            // survive this opt-in. Actual defaults and any unproved public entry stay protected.
            snapshot.publicEntries.filterNot { it.canonical in preserved.publicEntries } +
                snapshot.shellEntries.filterNot { it.always == false && it.canonical in preserved.shellEntries }
        }
        val existing = protectedEntries.firstOrNull { entry ->
            entry.filter.hasDataType(PackageManagerHidden.APK_MIME) || probes.any { matches(entry.filter, it) }
        }
        return existing?.let {
            "Existing APK preferred/last-chosen activity ${it.component.flattenToShortString()} (always=${it.always}) is preserved"
        }
    }

    fun begin(context: Context, authorizer: String, snapshot: Snapshot, preserved: PreservedLastChosen? = null): Plan {
        check(unsafeReason(context, snapshot, preserved) == null) { "The default-installer audit may not replace existing preferences" }
        if (preserved != null) {
            check(preserved.proof.get("validatedState") == snapshot.document()) {
                "The preserved-history proof must describe this exact baseline"
            }
            if (preserved.proof.has("sourceAuditId")) check(authorizer == "root") {
                "The approved InstallerX exception is restricted to Root"
            }
        }
        if (hasPlan(context)) check(read(context).get("status").asString == "restored") {
            "A pending default-installer audit exists; run with -e defaultUiRestoreOnly true before starting another audit"
        }
        val runId = UUID.randomUUID().toString()
        val document = JsonObject().apply {
            addProperty("format", 1)
            addProperty("packageName", context.packageName)
            addProperty("uid", Process.myUid())
            addProperty("sdk", Build.VERSION.SDK_INT)
            addProperty("fingerprint", Build.FINGERPRINT)
            addProperty("runId", runId)
            addProperty("authorizer", authorizer)
            addProperty("status", "pending")
            addProperty("createdAtMillis", System.currentTimeMillis())
            add("baseline", snapshot.document())
            preserved?.let { add(if (it.proof.has("sourceAuditId")) "preservedInstallerXHistory" else "preservedLastChosen", it.proof.deepCopy()) }
        }
        write(context, document)
        check(read(context) == document) { "The recovery plan was not persisted before changing defaults" }
        return location(context, runId)
    }

    /** Read-only ownership proof for removing a cancelled fixture history after the UI audit. */
    fun completedRun(context: Context, expectedRunId: String): CompletedRun {
        check(UUID.fromString(expectedRunId).toString() == expectedRunId) { "The cleanup run ID must be a canonical UUID" }
        val document = read(context)
        check(document.get("runId").asString == expectedRunId) { "The cleanup run ID does not match the saved journal" }
        check(document.get("status").asString == "restored") { "Restore the default-installer audit before cleaning its history" }
        val startedAt = document.get("createdAtMillis").asLong
        val restoredAt = document.get("restoredAtMillis").asLong
        check(startedAt > 0L && restoredAt >= startedAt) { "The journal has no valid completed test interval" }
        return CompletedRun(expectedRunId, startedAt, restoredAt)
    }

    /**
     * Read-only, one-device exception after the recorded partial clear. The two remaining
     * always=false filters have no scheme and are not equal to our four action/scheme filters.
     * They remain part of the baseline and must survive every lock/unlock and recovery check.
     */
    fun preserveApprovedInstallerXHistory(instrumentation: Instrumentation, current: Snapshot): PreservedLastChosen {
        val context = instrumentation.targetContext
        check(Build.VERSION.SDK_INT >= 33 && Process.myUid() / 100_000 == 0 &&
            readShell(instrumentation, "getprop ro.serialno").trim() == "QV770340J7") {
            "The partial-clear history exception is restricted to QV770340J7, API 33+ owner user 0"
        }
        val bytes = readPartialClearAudit(context)
        val audit = JsonParser.parseString(bytes.toString(Charsets.UTF_8)).asJsonObject
        check(audit.get("format").asInt == 1 && audit.get("kind").asString == "approved-installerx-last-chosen-cleanup" &&
            audit.get("auditId").asString == PARTIAL_CLEAR_AUDIT_ID && audit.get("serial").asString == "QV770340J7" &&
            audit.get("packageName").asString == context.packageName && audit.get("uid").asInt == Process.myUid() &&
            audit.get("sdk").asInt == Build.VERSION.SDK_INT && audit.get("fingerprint").asString == Build.FINGERPRINT &&
            audit.get("targetPackage").asString == APPROVED_INSTALLER_X && audit.get("approvedRecordCount").asInt == 3 &&
            audit.get("status").asString == "failed") { "The specified private partial-clear audit does not match this approved device/run" }
        val before = audit.getAsJsonObject("before")
        val after = audit.getAsJsonObject("after")
        val actual = current.document()
        val expected = approvedInstallerXEntries()
        val expectedShell = expected.map { it.document().toString() }
        val expectedPublic = expected.map { entry -> JsonObject().apply {
            addProperty("component", APPROVED_INSTALLER_X_COMPONENT)
            add("filter", filterDocument(entry.children.single().toFilter()))
        }.toString() }
        for ((key, approvedRecords) in listOf("shellPreferred" to expectedShell, "publicPreferred" to expectedPublic)) {
            check(serializedPackageEntries(before, key, APPROVED_INSTALLER_X) == approvedRecords.sorted()) {
                "The recorded pre-clear baseline is not exactly the three approved InstallerX APK records"
            }
            val remaining = approvedRecords.take(2).sorted()
            check(serializedPackageEntries(after, key, APPROVED_INSTALLER_X) == remaining &&
                serializedPackageEntries(actual, key, APPROVED_INSTALLER_X) == remaining) {
                "The historical audit/current state do not contain exactly the two approved no-scheme last-chosen records"
            }
            val originalOther = serializedPackageEntries(before, key, APPROVED_INSTALLER_X, exclude = true)
            check(originalOther == serializedPackageEntries(after, key, APPROVED_INSTALLER_X, exclude = true) &&
                originalOther == serializedPackageEntries(actual, key, APPROVED_INSTALLER_X, exclude = true)) {
                "Another preferred/last-chosen item changed since the partial-clear audit"
            }
            check(serializedPackageEntries(actual, key, context.packageName).isEmpty()) { "Plugin defaults already exist" }
        }
        check(before.get("preferences") == after.get("preferences") && before.get("preferences") == actual.get("preferences")) {
            "Plugin preferences changed since the partial-clear audit"
        }
        val appLinks = installerXAppLinks(instrumentation)
        check(audit.get("appLinksBefore").asString == audit.get("appLinksAfter").asString &&
            audit.get("appLinksBefore").asString == appLinks) { "InstallerX domain-link settings changed since the partial-clear audit" }
        val resolver = ComponentName("android", "com.android.internal.app.ResolverActivity").flattenToString()
        check(current.resolved == List(4) { resolver } && after.get("resolved") == actual.get("resolved")) {
            "APK intents no longer resolve to the system chooser; never exempt a real current default"
        }
        // The full before/after/current equality checks above already bind all non-InstallerX
        // records to the fixed approved audit. hasDataType(APK_MIME) also matches wildcards, so
        // distinguish a generic last-chosen handler from an APK-specific preference explicitly.
        val genericShell = current.shellEntries.filter { it.always == false && genericApkWildcard(it.filter) }
        val genericPublic = current.publicEntries.filter { entry ->
            genericApkWildcard(entry.filter) && current.shellEntries.filter { shell ->
                shell.component == entry.component && filterDocument(shell.filter) == filterDocument(entry.filter)
            }.let { matches -> matches.size == 1 && matches.single().always == false && matches.single() in genericShell }
        }
        val proof = JsonObject().apply {
            addProperty("kind", "preserve-two-approved-installerx-last-chosen-records")
            addProperty("sourceAuditId", PARTIAL_CLEAR_AUDIT_ID)
            addProperty("sourceAuditSha256", sha256(bytes))
            addProperty("preservedRecordCount", 2)
            addProperty("preservedGenericWildcardRecordCount", genericShell.size)
            add("preservedGenericWildcardRecords", strings(genericShell.map { it.canonical }.sorted()))
            addProperty("partialClearRecorded", true)
            addProperty("appLinks", appLinks)
            add("validatedState", actual)
        }
        return PreservedLastChosen(proof,
            (expectedPublic.take(2) + genericPublic.map { it.canonical }).toSet(),
            (expectedShell.take(2) + genericShell.map { it.canonical }).toSet())
    }

    /**
     * An explicit opt-in for a chooser-only baseline. These third-party last-chosen filters have
     * no scheme, so none equals the four action/scheme filters written by the production engine.
     * No history is cleared or reconstructed. Every original entry still has to survive byte for
     * byte in the canonical before/after snapshots, including during crash recovery.
     */
    fun preserveUnrelatedLastChosen(context: Context, current: Snapshot): PreservedLastChosen {
        check(Process.myUid() / 100_000 == 0) { "Last-chosen preservation requires owner user 0" }
        check((current.publicEntries + current.shellEntries).none { it.component.packageName == context.packageName }) {
            "Existing plugin preferred/last-chosen activities remain protected"
        }
        val resolver = ComponentName("android", "com.android.internal.app.ResolverActivity").flattenToString()
        check(current.resolved == List(4) { resolver }) { "All four APK probes must resolve to the system chooser" }
        fun relevant(entry: Preferred) = entry.filter.hasDataType(PackageManagerHidden.APK_MIME) ||
            probes.any { matches(entry.filter, it) }
        val shell = current.shellEntries.filter(::relevant)
        check(shell.isNotEmpty()) { "There are no APK last-chosen records to preserve" }
        shell.forEach { entry ->
            val filter = entry.filter
            check(entry.always == false && filter.countActions() == 1 &&
                filter.getAction(0) in PackageManagerHidden.INSTALL_ACTIONS &&
                filter.countCategories() == 1 && filter.hasCategory(Intent.CATEGORY_DEFAULT) &&
                filter.countDataTypes() == 1 && filter.hasDataType(PackageManagerHidden.APK_MIME) &&
                filter.countDataSchemes() == 0 && filter.countDataAuthorities() == 0 &&
                filter.countDataPaths() == 0 && filter.countDataSchemeSpecificParts() == 0 && filter.priority == 0) {
                "Only third-party, no-scheme APK last-chosen records can be preserved"
            }
        }
        val public = current.publicEntries.filter(::relevant)
        public.forEach { entry ->
            check(shell.count { it.component == entry.component && filterDocument(it.filter) == filterDocument(entry.filter) } == 1) {
                "Each public record requires exactly one matching shell always=false proof"
            }
        }
        val proof = JsonObject().apply {
            addProperty("kind", "preserve-unrelated-no-scheme-apk-last-chosen")
            addProperty("preservedRecordCount", shell.size)
            add("validatedState", current.document())
        }
        return PreservedLastChosen(proof, public.map { it.canonical }.toSet(), shell.map { it.canonical }.toSet())
    }

    /** The maintainer approved this device's existing four plugin records on 2026-10-02. */
    fun preserveApprovedPluginHistory(instrumentation: Instrumentation, current: Snapshot): PreservedLastChosen {
        approvedPluginDevice(instrumentation)
        val context = instrumentation.targetContext
        val own = baselinePluginRecords(context, current.document(), allowApproved = true)
        check(own.first.size == 4 && own.second.size == 4) { "The four approved plugin history records must still exist" }
        val unrelated = preserveUnrelatedLastChosen(context, current.copy(
            publicEntries = current.publicEntries.filterNot { it.component.packageName == context.packageName },
            shellEntries = current.shellEntries.filterNot { it.component.packageName == context.packageName },
        ))
        val proof = JsonObject().apply {
            addProperty("kind", APPROVED_PLUGIN_HISTORY_KIND)
            addProperty("serial", "QV710AF65F")
            addProperty("approvedPluginRecords", 4)
            addProperty("preservedThirdPartyRecords", unrelated.shellEntries.size)
            add("validatedState", current.document())
        }
        return PreservedLastChosen(proof, unrelated.publicEntries + own.first, unrelated.shellEntries + own.second)
    }

    private fun approvedPluginDevice(instrumentation: Instrumentation) {
        check(Build.VERSION.SDK_INT == 31 && Process.myUid() / 100_000 == 0 &&
            readShell(instrumentation, "getprop ro.serialno").trim() == "QV710AF65F") {
            "The approved plugin history scope is only QV710AF65F / API 31 / owner user 0"
        }
    }

    /** Exact canonical records, not a blanket exception for all preferences owned by the plugin. */
    private fun baselinePluginRecords(context: Context, baseline: JsonObject, allowApproved: Boolean): Pair<Set<String>, Set<String>> {
        val public = serializedPackageEntries(baseline, "publicPreferred", context.packageName)
        val shell = serializedPackageEntries(baseline, "shellPreferred", context.packageName)
        if (public.isEmpty() && shell.isEmpty()) return emptySet<String>() to emptySet()
        check(allowApproved) { "The baseline has unapproved plugin preferred/last-chosen records" }
        val component = ComponentName(context.packageName, context.packageName + COMPONENT_SUFFIX)
        val expected = listOf(PackageManagerHidden.APK_MIME, "application/vnd.apkm", "application/xapk-package-archive", "application/octet-stream").map { type ->
            Element("item", mapOf("name" to component.flattenToShortString(), "match" to "600000", "always" to "false", "set" to "0"),
                listOf(Element("filter", emptyMap(), listOf(
                    Element("action", mapOf("name" to Intent.ACTION_VIEW), emptyList()),
                    Element("cat", mapOf("name" to Intent.CATEGORY_DEFAULT), emptyList()),
                    Element("staticType", mapOf("name" to type), emptyList()),
                ))))
        }
        val expectedPublic = expected.map { entry -> JsonObject().apply {
            addProperty("component", component.flattenToString())
            add("filter", filterDocument(entry.children.single().toFilter()))
        }.toString() }.sorted()
        check(public == expectedPublic && shell == expected.map { it.document().toString() }.sorted()) {
            "Plugin history differs from the four approved VIEW/no-scheme/always=false records"
        }
        return public.toSet() to shell.toSet()
    }

    private fun genericApkWildcard(filter: IntentFilter): Boolean {
        val types = (0 until filter.countDataTypes()).map(filter::getDataType)
        // IntentFilter internally shortens application/* and */* to application and *.
        // Do not use hasDataType alone: it performs wildcard matching rather than exact membership.
        return types.isNotEmpty() && PackageManagerHidden.APK_MIME !in types &&
            types.all { it in setOf("*", "*/*", "application", "application/*") } &&
            filter.hasDataType(PackageManagerHidden.APK_MIME)
    }

    private fun serializedPackageEntries(document: JsonObject, key: String, packageName: String, exclude: Boolean = false): List<String> =
        document.getAsJsonArray(key).map { it.asString }.filter { encoded ->
            val entry = JsonParser.parseString(encoded).asJsonObject
            val component = checkNotNull(ComponentName.unflattenFromString(if (key == "shellPreferred")
                entry.getAsJsonObject("attributes").get("name").asString else entry.get("component").asString))
            (component.packageName == packageName) != exclude
        }.sorted()

    private fun readPartialClearAudit(context: Context): ByteArray {
        val file = File(context.filesDir, PARTIAL_CLEAR_AUDIT_PATH)
        check(file.isFile && !File(file.path + ".bak").exists() && !File(file.path + ".new").exists()) {
            "The specified private partial-clear audit is unavailable or has an unfinished write"
        }
        // No AtomicFile recovery here: this inspection must not rewrite the original failed audit.
        return file.inputStream().use(::boundedRead)
    }

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    /**
     * One separately approved maintenance action for QV770340J7, never part of normal UI setup or
     * recovery. It permanently removes only the three current last-chosen APK records and does
     * not recreate an older always=true record. There is no configurable package or Root fallback.
     */
    @Suppress("DEPRECATION")
    fun clearApprovedInstallerX(instrumentation: Instrumentation): ApprovedClear {
        val context = instrumentation.targetContext
        check(Build.VERSION.SDK_INT >= 33 && Process.myUid() / 100_000 == 0) {
            "The approved InstallerX cleanup is restricted to API 33+ owner user 0"
        }
        val serial = readShell(instrumentation, "getprop ro.serialno").trim()
        check(serial == "QV770340J7") { "This InstallerX cleanup was approved only for QV770340J7" }
        check(context.packageManager.getPackageInfo(APPROVED_INSTALLER_X, 0).packageName == APPROVED_INSTALLER_X)
        if (hasPlan(context)) check(read(context).get("status").asString == "restored") {
            "A default-installer audit is still pending; restore it before the separately approved cleanup"
        }
        val before = snapshot(instrumentation)
        check((before.publicEntries + before.shellEntries).none { it.component.packageName == context.packageName }) {
            "Plugin defaults exist; refusing to mix an active audit with the approved cleanup"
        }
        val expected = approvedInstallerXEntries()
        val targetShell = before.shellEntries.filter { it.component.packageName == APPROVED_INSTALLER_X }
        check(targetShell.size == 3 && targetShell.all { it.always == false } &&
            targetShell.map { it.canonical }.sorted() == expected.map { it.document().toString() }.sorted()) {
            "InstallerX records no longer exactly match the three approved last-chosen APK filters"
        }
        val expectedFilters = expected.map { it.children.single().toFilter() }
        val targetPublic = before.publicEntries.filter { it.component.packageName == APPROVED_INSTALLER_X }
        check(targetPublic.size <= 3 && targetPublic.all { entry ->
            entry.component.flattenToString() == APPROVED_INSTALLER_X_COMPONENT &&
                expectedFilters.any { expectedFilter -> filterDocument(entry.filter) == filterDocument(expectedFilter) }
        } && targetPublic.map { it.canonical }.distinct().size == targetPublic.size) {
            "A public InstallerX preferred activity differs from the three approved APK records"
        }
        val linksBefore = installerXAppLinks(instrumentation)
        val automation = instrumentation.uiAutomation
        val permissionBefore = context.checkSelfPermission(android.Manifest.permission.SET_PREFERRED_APPLICATIONS)
        check(permissionBefore == PackageManager.PERMISSION_DENIED) {
            "SET_PREFERRED_APPLICATIONS is already granted/delegated; refusing to replace an active permission identity"
        }
        val auditId = UUID.randomUUID().toString()
        val relativePath = "p5-default-installer/approved-installerx-clear-$auditId.json"
        val audit = JsonObject().apply {
            addProperty("format", 1); addProperty("kind", "approved-installerx-last-chosen-cleanup")
            addProperty("auditId", auditId); addProperty("serial", serial)
            addProperty("packageName", context.packageName); addProperty("uid", Process.myUid())
            addProperty("sdk", Build.VERSION.SDK_INT); addProperty("fingerprint", Build.FINGERPRINT)
            addProperty("targetPackage", APPROVED_INSTALLER_X); addProperty("approvedRecordCount", 3)
            addProperty("status", "pending"); addProperty("createdAtMillis", System.currentTimeMillis())
            add("before", before.document()); addProperty("appLinksBefore", linksBefore)
        }
        writeDocument(context, relativePath, audit)
        try {
            // Recheck immediately before changing identity, so a stale on-disk preflight cannot
            // authorize newly added defaults. All ordinary probes still skip existing defaults.
            check(before.document() == snapshot(instrumentation).document()) { "Default preferences changed after the approved preflight" }
            try {
                automation.adoptShellPermissionIdentity(android.Manifest.permission.SET_PREFERRED_APPLICATIONS)
                context.packageManager.clearPackagePreferredActivities(APPROVED_INSTALLER_X)
            } finally {
                automation.dropShellPermissionIdentity()
            }
            val after = snapshot(instrumentation)
            val linksAfter = installerXAppLinks(instrumentation)
            audit.add("after", after.document()); audit.addProperty("appLinksAfter", linksAfter)
            check((after.publicEntries + after.shellEntries).none { it.component.packageName == APPROVED_INSTALLER_X }) {
                "The public clear call did not remove the approved InstallerX records; no stronger fallback was attempted"
            }
            fun unrelated(entries: List<Preferred>) = entries.filterNot { it.component.packageName == APPROVED_INSTALLER_X }
                .map { it.canonical }.sorted()
            check(unrelated(before.publicEntries) == unrelated(after.publicEntries)) { "An unrelated public default changed" }
            check(unrelated(before.shellEntries) == unrelated(after.shellEntries)) { "An unrelated preferred/last-chosen record changed" }
            check(before.preferences == after.preferences) { "A plugin preference changed" }
            check(linksBefore == linksAfter) { "InstallerX domain-link settings changed" }
            check(context.checkSelfPermission(android.Manifest.permission.SET_PREFERRED_APPLICATIONS) == permissionBefore) {
                "The adopted preferred-application permission did not return to its original state"
            }
            audit.addProperty("status", "cleared")
            audit.addProperty("removedRecords", 3)
            audit.addProperty("otherDefaultsUnchanged", true)
            audit.addProperty("pluginPreferencesUnchanged", true)
            audit.addProperty("domainLinksUnchanged", true)
            audit.addProperty("shellIdentityDropped", true)
        } catch (failure: Throwable) {
            audit.addProperty("status", "failed")
            audit.addProperty("failureType", failure.javaClass.name)
            audit.addProperty("failure", failure.message?.take(1024))
            runCatching { snapshot(instrumentation).document() }.onSuccess { audit.add("after", it) }
            runCatching { installerXAppLinks(instrumentation) }.onSuccess { audit.addProperty("appLinksAfter", it) }
            throw failure
        } finally {
            audit.addProperty("finishedAtMillis", System.currentTimeMillis())
            writeDocument(context, relativePath, audit)
        }
        return ApprovedClear(File(context.filesDir, relativePath).absolutePath,
            context.getExternalFilesDir(null)?.let { File(it, relativePath).absolutePath })
    }

    private fun approvedInstallerXEntries(): List<Element> = listOf(
        "<action name=\"android.intent.action.INSTALL_PACKAGE\"/>",
        "<action name=\"android.intent.action.VIEW\"/>",
        "<action name=\"android.intent.action.MAIN\"/><action name=\"android.intent.action.VIEW\"/><action name=\"android.intent.action.INSTALL_PACKAGE\"/>",
    ).mapIndexed { index, actions ->
        val schemes = if (index == 2) "<scheme name=\"content\"/><scheme name=\"file\"/>" else ""
        parseXml("<item name=\"$APPROVED_INSTALLER_X_COMPONENT\" match=\"600000\" always=\"false\" set=\"0\"><filter>$actions" +
            "<cat name=\"android.intent.category.DEFAULT\"/><staticType name=\"application/vnd.android.package-archive\"/>$schemes</filter></item>")
    }

    private fun filterDocument(filter: IntentFilter): JsonObject = StringWriter().let { writer ->
        Xml.newSerializer().apply {
            setOutput(writer); startTag(null, "filter"); filter.writeToXml(this); endTag(null, "filter"); flush()
        }
        parseXml(writer.toString()).document()
    }

    private fun installerXAppLinks(instrumentation: Instrumentation): String {
        val marker = "DEFAULT_UI_APP_LINKS_EXIT=0"
        // executeShellCommand(String) uses Runtime.exec, so shell syntax must go through an
        // explicit shell stdin. This fixed script only queries app links and reports its exit code.
        val pipes = instrumentation.uiAutomation.executeShellCommandRw("sh")
        val output = try {
            check(pipes.size == 2)
            ParcelFileDescriptor.AutoCloseOutputStream(pipes[1]).use {
                it.write(("cmd package get-app-links --user 0 com.rosan.installer.x.revived 2>&1\n" +
                    "printf '\\nDEFAULT_UI_APP_LINKS_EXIT=%s\\n' \"\$?\"\nexit\n").toByteArray(Charsets.UTF_8))
            }
            ParcelFileDescriptor.AutoCloseInputStream(pipes[0]).use { boundedRead(it).toString(Charsets.UTF_8) }.trimEnd()
        } finally { pipes.forEach { runCatching { it.close() } } }
        check(output.endsWith("\n$marker") || output == marker) { "The read-only InstallerX app-link query failed" }
        return output.removeSuffix(marker).trimEnd()
    }

    private fun readShell(instrumentation: Instrumentation, command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
            .use { boundedRead(it).toString(Charsets.UTF_8) }

    fun assertUnrelatedUnchanged(context: Context, before: Snapshot, after: Snapshot, preserved: PreservedLastChosen? = null) {
        val own = baselinePluginRecords(context, before.document(), preserved?.proof?.get("kind")?.asString == APPROVED_PLUGIN_HISTORY_KIND)
        check(before.preferences == after.preferences) { "The default-installer UI changed plugin preferences" }
        check(before.publicEntries.map { it.canonical }.sorted() == after.publicEntries
            .filterNot { it.component.packageName == context.packageName && it.canonical !in own.first }.map { it.canonical }.sorted()) {
            "An unrelated public preferred activity changed"
        }
        check(before.shellEntries.map { it.canonical }.sorted() == after.shellEntries
            .filterNot { it.component.packageName == context.packageName && it.canonical !in own.second }.map { it.canonical }.sorted()) {
            "An unrelated preferred/last-chosen activity changed"
        }
        assertOwnedEntries(context, after, own)
    }

    /** Idempotent after success. A prior successful audit never rewrites a subsequent user's state. */
    @Suppress("DEPRECATION")
    fun restore(instrumentation: Instrumentation): Plan {
        val context = instrumentation.targetContext
        val document = read(context)
        val runId = document.get("runId").asString
        if (document.get("status").asString == "restored") return location(context, runId)
        check(document.get("status").asString == "pending")
        val before = document.getAsJsonObject("baseline")
        var approvedPluginHistory = false
        document.getAsJsonObject("preservedLastChosen")?.let { proof ->
            approvedPluginHistory = proof.get("kind").asString == APPROVED_PLUGIN_HISTORY_KIND
            if (approvedPluginHistory) {
                approvedPluginDevice(instrumentation)
                check(proof.get("serial").asString == "QV710AF65F" && proof.get("approvedPluginRecords").asInt == 4)
            }
            check((approvedPluginHistory || proof.get("kind").asString == "preserve-unrelated-no-scheme-apk-last-chosen") &&
                proof.get("validatedState") == before) { "The preserved last-chosen proof no longer matches the recovery baseline" }
        }
        // Validate exact approved records again before considering a clear, including recovery
        // without the original test arguments. All other pre-existing plugin entries stay protected.
        val own = baselinePluginRecords(context, before, approvedPluginHistory)
        val current = snapshot(instrumentation)
        assertOwnedEntries(context, current, own)
        check(before.get("publicPreferred") == strings(current.publicEntries
            .filterNot { it.component.packageName == context.packageName && it.canonical !in own.first }.map { it.canonical }.sorted()) &&
            before.get("shellPreferred") == strings(current.shellEntries
                .filterNot { it.component.packageName == context.packageName && it.canonical !in own.second }.map { it.canonical }.sorted()) &&
            before.get("preferences") == current.preferences) {
            "The original records or plugin preferences changed; refusing cleanup of an uncertain baseline"
        }
        if (current.publicEntries.any { it.component.packageName == context.packageName && it.canonical !in own.first } ||
            current.shellEntries.any { it.component.packageName == context.packageName && it.canonical !in own.second }) {
            // Public clear of our own package requires no grant/service and leaves all other
            // packages untouched. Never clear somebody else's package or restore broad defaults.
            context.packageManager.clearPackagePreferredActivities(context.packageName)
        }
        val after = snapshot(instrumentation).document()
        check(before.get("publicPreferred") == after.get("publicPreferred")) { "Public preferred activities did not return to the recorded baseline" }
        check(before.get("shellPreferred") == after.get("shellPreferred")) { "Preferred/last-chosen activities did not return to the recorded baseline" }
        check(before.get("preferences") == after.get("preferences")) { "Plugin preferences differ from the recorded baseline; no user values were overwritten" }
        check(before.get("resolved") == after.get("resolved")) { "APK resolution did not return to the recorded baseline" }
        document.getAsJsonObject("preservedInstallerXHistory")?.let { proof ->
            check(proof.get("sourceAuditId").asString == PARTIAL_CLEAR_AUDIT_ID &&
                proof.get("sourceAuditSha256").asString == sha256(readPartialClearAudit(context))) { "The preserved-history audit changed during the UI test" }
            check(proof.get("appLinks").asString == installerXAppLinks(instrumentation)) { "InstallerX domain-link settings changed during the UI test" }
        }
        document.addProperty("status", "restored")
        document.addProperty("restoredAtMillis", System.currentTimeMillis())
        write(context, document)
        val plan = location(context, runId)
        val signal = File(plan.continuePath)
        check(!signal.exists() || signal.delete()) { "The test-owned continuation marker could not be removed" }
        return plan
    }

    internal fun assertOwnedEntries(context: Context, snapshot: Snapshot, preserved: Pair<Set<String>, Set<String>> = emptySet<String>() to emptySet()) {
        val target = ComponentName(context.packageName, context.packageName + COMPONENT_SUFFIX)
        listOf(snapshot.publicEntries.filterNot { it.canonical in preserved.first },
            snapshot.shellEntries.filterNot { it.canonical in preserved.second }).forEach { records ->
            val owned = records.filter { it.component.packageName == context.packageName }
            check(owned.size <= 4) { "At most four new production filters may be cleared" }
            owned.forEach { entry ->
                val filter = entry.filter
                check(entry.component == target && entry.always != false && filter.countActions() == 1 &&
                    filter.getAction(0) in PackageManagerHidden.INSTALL_ACTIONS &&
                    filter.countCategories() == 1 && filter.hasCategory(Intent.CATEGORY_DEFAULT) &&
                    filter.countDataTypes() == 1 && filter.getDataType(0) == PackageManagerHidden.APK_MIME &&
                    filter.countDataSchemes() == 1 && filter.getDataScheme(0) in listOf("content", "file") &&
                    filter.countDataAuthorities() == 0 && filter.countDataPaths() == 0 &&
                    filter.countDataSchemeSpecificParts() == 0 && filter.priority == 0) {
                    "An unowned plugin preferred/last-chosen item appeared; refusing to clear it"
                }
            }
            check(owned.map { it.filter.getAction(0) to it.filter.getDataScheme(0) }.distinct().size == owned.size) {
                "Only one of each of the four new production filters may be cleared"
            }
        }
    }

    private fun matches(filter: IntentFilter, intent: Intent) = filter.match(intent.action, intent.type, intent.scheme,
        intent.data, intent.categories, "DefaultInstallerUiAudit") >= 0

    private fun preferenceSnapshot(context: Context): JsonObject = JsonObject().apply {
        val folder = File(context.applicationInfo.dataDir, "shared_prefs")
        val names = (listOf("app-appearance", "installer_settings", "installer_updates") +
            folder.listFiles().orEmpty().filter { it.isFile && (it.name.endsWith(".xml") || it.name.endsWith(".xml.bak")) }
                .map { it.name.removeSuffix(".bak").removeSuffix(".xml") }).distinct().sorted()
        names.forEach { name ->
            val values = context.getSharedPreferences(name, Context.MODE_PRIVATE).all
            add(name, JsonObject().apply {
                values.toSortedMap().forEach { (key, value) ->
                    add(key, JsonObject().apply {
                        when (value) {
                            is String -> { addProperty("type", "string"); addProperty("value", value) }
                            is Boolean -> { addProperty("type", "boolean"); addProperty("value", value) }
                            is Int -> { addProperty("type", "int"); addProperty("value", value.toString()) }
                            is Long -> { addProperty("type", "long"); addProperty("value", value.toString()) }
                            is Float -> { addProperty("type", "float-bits"); addProperty("value", value.toRawBits().toString()) }
                            is Set<*> -> {
                                check(value.all { it is String }); addProperty("type", "string-set")
                                add("value", strings(value.filterIsInstance<String>().sorted()))
                            }
                            else -> error("Unsupported preference type in $name/$key")
                        }
                    })
                }
            })
        }
    }

    private fun hasPlan(context: Context) = File(context.filesDir, RELATIVE_PATH).let { it.exists() || File(it.path + ".bak").exists() }

    private fun read(context: Context): JsonObject {
        check(hasPlan(context)) { "No saved default-installer recovery plan exists; no baseline may be inferred" }
        val bytes = AtomicFile(File(context.filesDir, RELATIVE_PATH)).openRead().use(::boundedRead)
        return JsonParser.parseString(bytes.toString(Charsets.UTF_8)).asJsonObject.also {
            check(it.get("format").asInt == 1 && it.get("packageName").asString == context.packageName)
            check(it.get("uid").asInt == Process.myUid() && it.get("sdk").asInt == Build.VERSION.SDK_INT)
            check(it.get("fingerprint").asString == Build.FINGERPRINT)
            check(it.get("authorizer").asString in listOf("shizuku", "root"))
            check(UUID.fromString(it.get("runId").asString).toString() == it.get("runId").asString)
        }
    }

    private fun write(context: Context, document: JsonObject) = writeDocument(context, RELATIVE_PATH, document)

    private fun writeDocument(context: Context, relativePath: String, document: JsonObject) {
        val bytes = GsonBuilder().setPrettyPrinting().create().toJson(document).toByteArray(Charsets.UTF_8)
        check(bytes.size <= MAX_BYTES)
        fun writeFile(file: File) {
            check(file.parentFile!!.isDirectory || file.parentFile!!.mkdirs())
            val atomic = AtomicFile(file)
            val stream = atomic.startWrite()
            try { stream.write(bytes); stream.fd.sync(); atomic.finishWrite(stream) }
            catch (failure: Throwable) { atomic.failWrite(stream); throw failure }
        }
        writeFile(File(context.filesDir, relativePath))
        context.getExternalFilesDir(null)?.let { writeFile(File(it, relativePath)) }
    }

    private fun location(context: Context, runId: String) = Plan(runId, File(context.filesDir, RELATIVE_PATH).absolutePath,
        context.getExternalFilesDir(null)?.let { File(it, RELATIVE_PATH).absolutePath },
        File(context.filesDir, "p5-default-installer/continue-$runId").absolutePath)

    private fun boundedRead(input: InputStream): ByteArray = java.io.ByteArrayOutputStream().use { output ->
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            check(output.size() + count <= MAX_BYTES) { "The preferred-activity snapshot exceeds the audit limit" }
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    }

    private fun strings(values: List<String>) = JsonArray().apply { values.forEach(::add) }

    /** Sort XML attributes/children; framework dump order must not cause false restoration failures. */
    private data class Element(val name: String, val attributes: Map<String, String>, val children: List<Element>) {
        fun document(): JsonObject = JsonObject().apply {
            addProperty("name", name)
            add("attributes", JsonObject().apply { attributes.toSortedMap().forEach { (key, value) -> addProperty(key, value) } })
            add("children", JsonArray().apply { children.map { it.document() }.sortedBy { it.toString() }.forEach(::add) })
        }

        fun toFilter(): IntentFilter {
            val writer = StringWriter()
            val serializer = Xml.newSerializer().apply { setOutput(writer) }
            fun emit(element: Element) {
                serializer.startTag(null, element.name)
                element.attributes.forEach { (key, value) -> serializer.attribute(null, key, value) }
                element.children.forEach(::emit)
                serializer.endTag(null, element.name)
            }
            emit(this); serializer.flush()
            val parser = Xml.newPullParser().apply { setInput(StringReader(writer.toString())); nextTag() }
            return IntentFilter().apply { readFromXml(parser) }
        }
    }

    private fun parseXml(xml: String): Element {
        val parser = Xml.newPullParser().apply { setInput(StringReader(xml)) }
        check(parser.nextTag() == XmlPullParser.START_TAG) { "The preferred-activity dump is not XML" }
        fun readElement(): Element {
            val name = parser.name
            val attributes = (0 until parser.attributeCount).associate { parser.getAttributeName(it) to parser.getAttributeValue(it) }
            val children = arrayListOf<Element>()
            while (true) when (parser.next()) {
                XmlPullParser.START_TAG -> children += readElement()
                XmlPullParser.END_TAG -> return Element(name, attributes, children)
                XmlPullParser.TEXT -> check(parser.text.isBlank()) { "Unexpected text in preferred-activity XML" }
                XmlPullParser.END_DOCUMENT -> error("Truncated preferred-activity XML")
            }
        }
        return readElement()
    }
}
