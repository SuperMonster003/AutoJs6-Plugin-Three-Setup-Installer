package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.util.AtomicFile
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.DhizukuFramework
import io.github.supermonster003.autojs6.plugin.three.setup.installer.profiles.InstallProfile
import io.github.supermonster003.autojs6.plugin.three.setup.installer.profiles.InstallProfileCodec
import io.github.supermonster003.autojs6.plugin.three.setup.installer.profiles.InstallProfileOverrides
import io.github.supermonster003.autojs6.plugin.three.setup.installer.profiles.InstallProfilePreferences
import io.github.supermonster003.autojs6.plugin.three.setup.installer.profiles.InstallProfileSnapshot
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.InstallerPreferences
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.ExternalSources
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.ExternalInstaller
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation
import org.autojs.plugin.installer.api.InstallerContract as C
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.UUID

/** Fixed package only. Mutating runs require a dedicated API 31 emulator and explicit opt-in. */
@RunWith(AndroidJUnit4::class)
class InstallProfileRuntimeDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun externalManifestPrefixOverridesAnUnavailableLocalRootDefaultWithDhizuku() = installing { settings, source, ownership, history ->
        val profile = profile("Runtime manifest fixture", """{"authorizer":"dhizuku","deleteSource":false}""")
        settings.seed(profile)
        assertEquals("root", InstallerPreferences.read(context).options.authorizer)
        assertFalse(source.file.name.startsWith(profile.packagePrefix))
        ownership.installationStarted()
        val record = requireNotNull(InstallPresentation.find(ExternalInstaller.start(context,
            ExternalSources.fromUris(listOf(source.uri), 0), options = null)))
        history.track(record)
        try {
            assertEquals("root", record.request.options.authorizer)
            assertTrue(record.request.explicitOptions.isEmpty())
            awaitInstalled(record, profile.name)
            val item = record.snapshot().items.single()
            assertEquals("dhizuku", item.options?.authorizer)
            assertEquals(C.INTERACTION_AUTO, item.effectiveInteraction)
            assertFalse(item.confirmedOptions)
            source.assertPreserved()
            assertEquals(0, providerDeletes(source.uri))
            evidence("defaultRootUnusable=true optionsWereNull=true manifestPrefixMatched=true authorizer=dhizuku installedVersion=1 sourceRetained=true")
        } finally { record.close() }
    }

    @Test fun explicitDhizukuAndFalseDeletionOverrideTheMatchingRootAndDeleteProfile() = installing { settings, source, ownership, history ->
        val profile = profile("Conflicting defaults fixture", """{"authorizer":"root","deleteSource":true}""")
        settings.seed(profile)
        val explicit = InstallOptions(authorizer = C.AUTHORIZER_DHIZUKU, deleteSource = false, timeoutMillis = 90_000)
        ownership.installationStarted()
        val record = requireNotNull(InstallPresentation.find(ExternalInstaller.start(context,
            ExternalSources.fromUris(listOf(source.uri), 0), options = explicit)))
        history.track(record)
        try {
            assertEquals(InstallProfileOverrides.ALLOWED_KEYS, record.request.explicitOptions)
            awaitInstalled(record, profile.name)
            val effective = requireNotNull(record.snapshot().items.single().options)
            assertEquals(C.AUTHORIZER_DHIZUKU, effective.authorizer)
            assertFalse(effective.deleteSource)
            assertEquals(explicit.copy(timeoutMillis = effective.timeoutMillis), effective)
            source.assertPreserved()
            assertEquals(0, providerDeletes(source.uri))
            evidence("explicitOptions=true matchingProfileRootDeleteIgnored=true authorizer=dhizuku installedVersion=1 sourceRetained=true providerDeleteAttempts=0")
        } finally { record.close() }
    }

    @Test fun duplicateContentUrisAndCanonicalFileAliasesAreRetainedBeforeAnyDeleteAttempt() {
        optIn()
        OwnedSource().use { source ->
            val beforeSessions = context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet()
            val duplicateContent = ExternalSources.fromUris(listOf(source.uri, source.uri), 0)
            val firstContent = duplicateContent.deleteInstalled(context, 0, InstallOptions(deleteSource = true))
            val laterContent = duplicateContent.deleteInstalled(context, 1, InstallOptions(deleteSource = false))
            assertFalse(firstContent.deleted)
            assertTrue(firstContent.notes.single().contains("shared"))
            assertFalse(laterContent.deleted)
            assertEquals(0, providerDeletes(source.uri))
            source.assertPreserved()
            duplicateContent.openItem(context, 1, android.os.CancellationSignal()) {}.descriptor.use {
                assertEquals(source.file.length(), it.statSize)
            }

            val aliasDirectory = File(source.directory, "alias").apply { check(mkdir()) }
            try {
                val alias = File(aliasDirectory, "../fixture.apk")
                assertEquals(source.file.canonicalPath, alias.canonicalPath)
                val duplicateFile = ExternalSources.fromUris(listOf(Uri.fromFile(source.file), Uri.fromFile(alias)), 0)
                val firstFile = duplicateFile.deleteInstalled(context, 0, InstallOptions(deleteSource = true))
                val laterFile = duplicateFile.deleteInstalled(context, 1, InstallOptions(deleteSource = false))
                assertFalse(firstFile.deleted)
                assertTrue(firstFile.notes.single().contains("shared"))
                assertFalse(laterFile.deleted)
                source.assertPreserved()
                duplicateFile.openItem(context, 1, android.os.CancellationSignal()) {}.descriptor.use {
                    assertEquals(source.file.length(), it.statSize)
                }
            } finally { check(aliasDirectory.delete()) }
            assertEquals(beforeSessions, context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet())
            evidence("cleanupSeamOnly=true repeatedContentUriRetained=true canonicalFileAliasRetained=true secondSourceReadable=true providerDeleteAttempts=0 sessionsUnchanged=true")
        }
    }

    private fun installing(action: (SettingsLease, OwnedSource, FixturePackageOwnership, FixtureHistoryOwnership) -> Unit) {
        optIn()
        val root = AuthorizerStates.state(context, Authorizer.ROOT)
        check(!root.usable) { "This fixture requires an unusable Root default" }
        check(AuthorizerStates.state(context, Authorizer.DHIZUKU).usable) { "Dhizuku must already be granted; this fixture does not request permissions" }
        val framework = DhizukuFramework(context)
        val ownerSessions = framework.checked { framework.installer.mySessions.map { it.sessionId }.toSet() }
        val appSessions = context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet()
        val recoveryDirectory = File(context.noBackupFilesDir, "dhizuku-install-sessions")
        check(recoveryDirectory.listFiles().orEmpty().isEmpty()) { "Pending Dhizuku journals must be settled before this fixture" }
        try {
            SettingsLease().use { settings ->
                FixturePackageOwnership(setOf(FIXTURE)).use { ownership ->
                    OwnedSource().use { source ->
                        FixtureHistoryOwnership(context, FIXTURE).use { history -> action(settings, source, ownership, history) }
                    }
                }
            }
        } finally {
            await(10_000, "Fixture platform sessions did not settle") {
                framework.checked { framework.installer.mySessions.map { it.sessionId }.toSet() } == ownerSessions &&
                    context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet() == appSessions
            }
            check(recoveryDirectory.listFiles().orEmpty().isEmpty()) { "Fixture Dhizuku recovery state remains" }
            val finalRoot = AuthorizerStates.state(context, Authorizer.ROOT)
            assertEquals(listOf(root.available, root.running, root.granted), listOf(finalRoot.available, finalRoot.running, finalRoot.granted))
            assertEquals(framework.identity, DhizukuFramework.checkReady(context))
            evidence("sessionsRestored=true dhizukuOwnerAndPermissionRetained=true rootStateUnchanged=true")
        }
    }

    private fun optIn() {
        assumeTrue("Opt in with profileRuntimeFixtures=true", InstrumentationRegistry.getArguments().getString("profileRuntimeFixtures") == "true")
        check(Build.VERSION.SDK_INT == 31 && Build.HARDWARE in setOf("ranchu", "goldfish") && Process.myUid() / 100000 == 0) {
            "Runtime profile fixtures are restricted to the dedicated API 31 emulator in user 0"
        }
        check(InstallPresentation.snapshots().none { !it.state.terminal }) { "An unrelated installation is active" }
    }

    private fun profile(name: String, options: String) = InstallProfile(name = name, source = C.SOURCE_EXTERNAL,
        packagePrefix = FIXTURE, overrides = InstallProfileOverrides.parse(JsonParser.parseString(options).asJsonObject))

    @Suppress("DEPRECATION")
    private fun awaitInstalled(record: InstallPresentation.Record, profileName: String) {
        await(95_000, "Profile fixture did not settle") {
            val current = record.snapshot()
            check(current.prompt == null && current.safetyReview == null) { "The fixed privileged fixture unexpectedly requires a dialog decision" }
            current.terminal
        }
        val state = record.snapshot()
        assertNull("${state.failure?.toJson()}", state.failure)
        val item = state.items.single()
        val result = requireNotNull(item.result)
        assertTrue(result.toString(), result[C.FIELD_OK].asBoolean)
        assertEquals(FIXTURE, result[C.FIELD_PACKAGE_NAME].asString)
        assertEquals(C.AUTHORIZER_DHIZUKU, result[C.FIELD_AUTHORIZER].asString)
        assertEquals(profileName, item.profileName)
        assertTrue(record.request.applySourceProfiles)
        assertFalse(result[C.FIELD_SOURCE_DELETE_REQUESTED].asBoolean)
        assertFalse(result[C.FIELD_SOURCE_DELETED].asBoolean)
        val info = context.packageManager.getPackageInfo(FIXTURE, PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or
            PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS)
        assertEquals(1L, info.longVersionCode)
        assertTrue(info.activities.isNullOrEmpty() && info.services.isNullOrEmpty() && info.receivers.isNullOrEmpty() && info.providers.isNullOrEmpty())
    }

    private fun providerDeletes(uri: Uri): Int {
        val result = requireNotNull(context.contentResolver.call(uri, "fixtureDeletionState", uri.toString(), null))
        check(result.getString("fixtureUri") == uri.toString() && result.containsKey("deleteAttempts"))
        return result.getInt("deleteAttempts").also { check(it >= 0) }
    }

    private fun await(timeout: Long, message: String, predicate: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeout
        while (!predicate() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(25)
        check(predicate()) { message }
    }

    private fun evidence(value: String) = instrumentation.sendStatus(0, Bundle().apply {
        putString("profile-runtime", "${Build.MODEL} API=${Build.VERSION.SDK_INT} $value")
    })

    private inner class OwnedSource : Closeable {
        val directory = File(context.cacheDir, "p2-source-fixtures-${UUID.randomUUID()}")
        val file = File(directory, "fixture.apk")
        val uri: Uri = Uri.parse("content://${context.packageName}.source-fixtures/${directory.name}/fixture.apk")
        init {
            check(!directory.exists() && directory.mkdir() && directory.canonicalFile.parentFile == context.cacheDir.canonicalFile)
            val bytes = instrumentation.context.assets.open("advanced-fixtures/v1.apk").use { it.readBytes() }
            check(sha256(bytes) == SHA256)
            file.writeBytes(bytes)
            assertPreserved()
        }
        fun assertPreserved() { check(file.isFile && sha256(file.readBytes()) == SHA256) { "Fixed source changed before fixture cleanup" } }
        override fun close() {
            assertPreserved()
            check(directory.canonicalFile.parentFile == context.cacheDir.canonicalFile)
            check(directory.listFiles().orEmpty().toSet() == setOf(file)) { "Unexpected files in the fixture directory; refusing cleanup" }
            check(file.delete() && directory.delete())
        }
    }

    /** Compare full maps before every write/restore; retain a typed durable record if ownership is lost. */
    private inner class SettingsLease : Closeable {
        private val runId = UUID.randomUUID().toString()
        private val folder = File(context.noBackupFilesDir, "p9-profile-runtime-fixtures")
        private val lockHandle: RandomAccessFile
        private val lock: java.nio.channels.FileLock
        private val journal: AtomicFile
        private val files: List<PreferencesFile>
        private val originalProfiles: InstallProfileSnapshot
        private val originalSettings: InstallerPreferences
        private val pending = mutableMapOf<String, Map<String, Any>>()

        init {
            check(folder.isDirectory || folder.mkdirs())
            lockHandle = RandomAccessFile(File(folder, "exclusive.lock"), "rw")
            try {
                lock = requireNotNull(lockHandle.channel.tryLock()) { "Another runtime preferences fixture is active" }
                try {
                    folder.listFiles().orEmpty().filter { it.name.endsWith(".json") || it.name.endsWith(".bak") || it.name.endsWith(".new") }.forEach {
                        check(it.name.endsWith(".json") && JsonParser.parseString(it.readText()).asJsonObject["status"].asString == "restored") {
                            "A prior runtime preferences journal needs explicit recovery"
                        }
                    }
                    originalProfiles = InstallProfilePreferences.read(context)
                    check(originalProfiles.readable) { "Do not replace unreadable pre-existing profiles" }
                    originalSettings = InstallerPreferences.read(context)
                    files = listOf(PreferencesFile(PROFILE_FILE), PreferencesFile(SETTINGS_FILE))
                    journal = AtomicFile(File(folder, "$runId.json"))
                    writeJournal("pending")
                } catch (failure: Throwable) { lock.release(); throw failure }
            } catch (failure: Throwable) { lockHandle.close(); throw failure }
        }

        fun seed(profile: InstallProfile) {
            val profileFile = files.single { it.name == PROFILE_FILE }
            val encoded = InstallProfileCodec.encode(InstallProfileSnapshot(UUID.randomUUID().toString(), listOf(profile)))
            mutate(profileFile, profileFile.owned + ("document" to encoded)) {
                profileFile.preferences.edit().putString("document", encoded).commit()
            }
            assertEquals(listOf(profile), InstallProfilePreferences.read(context).profiles)
            val defaults = originalSettings.copy(options = InstallOptions(authorizer = C.AUTHORIZER_ROOT, timeoutMillis = 90_000),
                interaction = C.INTERACTION_AUTO, progressNotifications = false)
            val settingsFile = files.single { it.name == SETTINGS_FILE }
            val expected = settingsFile.owned + mapOf("default_options" to InstallerPreferences.optionsDocument(defaults.options).toString(),
                "default_interaction" to defaults.interaction, "authorizer_order" to defaults.authorizers.encodeOrder(),
                "authorizer_enabled" to defaults.authorizers.encodeEnabled(), "progress_notifications" to defaults.progressNotifications)
            mutate(settingsFile, expected) { defaults.save(context) }
            assertEquals(defaults, InstallerPreferences.read(context))
        }

        private fun mutate(file: PreferencesFile, intended: Map<String, Any>, action: () -> Boolean) {
            check(file.current() == file.owned) { "Preferences changed outside this fixture" }
            pending[file.name] = intended
            writeJournal("pending")
            check(file.current() == file.owned)
            check(action()) { "Fixture preference write was not durable" }
            check(file.current() == intended) { "Fixture preference write did not match its journal" }
            file.owned = intended
            pending.remove(file.name)
            writeJournal("pending")
        }

        override fun close() {
            try {
                // Check all documents before restoring either, so an unrelated editor is never silently overwritten.
                files.forEach { check(it.current() == it.owned || it.current() == pending[it.name]) {
                    "Preferences changed outside the fixture; retain the journal for explicit recovery"
                } }
                files.forEach { file ->
                    check(file.current() == file.owned || file.current() == pending[file.name])
                    pending[file.name] = file.original
                    writeJournal("restoring")
                    if (!file.existed) check(context.deleteSharedPreferences(file.name))
                    else check(file.preferences.edit().clear().apply { file.original.forEach { (key, value) -> putValue(key, value) } }.commit())
                    check(file.current() == file.original)
                    check(file.xml.exists() == file.existed)
                    file.owned = file.original
                    pending.remove(file.name)
                    writeJournal("restoring")
                }
                assertEquals(originalProfiles, InstallProfilePreferences.read(context))
                assertEquals(originalSettings, InstallerPreferences.read(context))
                writeJournal("restored")
                evidence("fullProfileAndInstallerPreferenceMapsRestored=true originalFilePresenceRestored=true journal=${journal.baseFile.name}")
            } finally { lock.release(); lockHandle.close() }
        }

        private fun writeJournal(status: String) {
            val value = JsonObject().apply {
                addProperty("schema", 1); addProperty("runId", runId); addProperty("uid", Process.myUid()); addProperty("pid", Process.myPid())
                addProperty("fingerprint", Build.FINGERPRINT); addProperty("status", status)
                add("files", JsonArray().apply { files.forEach { file -> add(JsonObject().apply {
                    addProperty("name", file.name); addProperty("originallyExisted", file.existed)
                    addProperty("originalXmlBase64", file.originalXml?.let { Base64.encodeToString(it, Base64.NO_WRAP) })
                    add("original", typedMap(file.original)); add("owned", typedMap(file.owned))
                    pending[file.name]?.let { add("intended", typedMap(it)) }
                }) } })
            }
            val stream = journal.startWrite()
            try { stream.write(value.toString().toByteArray(Charsets.UTF_8)); stream.fd.sync(); journal.finishWrite(stream) }
            catch (failure: Throwable) { journal.failWrite(stream); throw failure }
        }

        private inner class PreferencesFile(val name: String) {
            val preferences: SharedPreferences get() = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val xml = File(context.applicationInfo.dataDir, "shared_prefs/$name.xml")
            val existed = xml.exists()
            val originalXml = if (existed) xml.readBytes() else null
            val original = current()
            var owned = original
            init { check(!File(xml.path + ".bak").exists() && !File(xml.path + ".new").exists()) { "Preferences have unfinished persistence" } }
            fun current(): Map<String, Any> = preferences.all.mapValues { (_, value) ->
                when (value) {
                    is String, is Boolean, is Int, is Long, is Float -> value
                    is Set<*> -> value.map { check(it is String); it }.toSet()
                    else -> error("Unsupported existing preference value")
                }
            }
        }
    }

    private fun SharedPreferences.Editor.putValue(key: String, value: Any) {
        when (value) {
            is String -> putString(key, value)
            is Boolean -> putBoolean(key, value)
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Float -> putFloat(key, value)
            is Set<*> -> putStringSet(key, value.map { check(it is String); it }.toSet())
            else -> error("Unsupported preference value")
        }
    }

    private fun typedMap(values: Map<String, Any>) = JsonObject().apply { values.toSortedMap().forEach { (key, value) ->
        add(key, JsonObject().apply {
            addProperty("type", when (value) { is String -> "string"; is Boolean -> "boolean"; is Int -> "int"; is Long -> "long"; is Float -> "float"; is Set<*> -> "strings"; else -> error("Invalid preference type") })
            if (value is Set<*>) add("value", JsonArray().apply { value.map { check(it is String); it }.sorted().forEach(::add) })
            else addProperty("value", value.toString())
        })
    } }

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    companion object {
        private const val FIXTURE = "io.github.supermonster003.autojs6.installer.advanced.fixture"
        private const val SHA256 = "bae693d55c5dd912ef22e2e87e5a3a6fb83a40dac15318fbca8a230e00b577a0"
        private const val PROFILE_FILE = "installation_profiles"
        private const val SETTINGS_FILE = "installer_settings"
    }
}
