package io.github.supermonster003.autojs6.plugin.three.setup.installer.profiles

import android.content.Intent
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallEngine
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallRequest
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallSession
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.SourceEntry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PlannedApk
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import org.autojs.plugin.installer.api.InstallerContract as C
import org.autojs.plugin.installer.api.InstallerErrorCodes as E
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.concurrent.Executor

/** Real session orchestration and profile planning, with observable platform/UI boundaries. */
class InstallProfileSessionTest {
    @Test fun packageProfileIsAppliedAfterPreparationAndBeforeUnavailableDefaultRootIsResolved() {
        val fixture = Fixture(request(), snapshot(profile("Local", "org.example.safe", """{"authorizer":"none"}""")),
            listOf("org.example.safe.app"))
        fixture.rootAvailable = false
        fixture.run()

        assertNull(fixture.failure)
        assertTrue(fixture.singleResult()[C.FIELD_OK].asBoolean)
        assertEquals(listOf("none"), fixture.resolutions.map { it.options.authorizer })
        assertEquals(listOf("initial", "prepare:0", "policy:org.example.safe.app", "profile:0", "resolve:none:current"),
            fixture.events.take(5))
        assertEquals(listOf(0), fixture.configured)
        assertEquals(C.INTERACTION_DIALOG, fixture.calls.single().request.interaction)
        assertEquals(Authorizer.NONE, fixture.calls.single().authorizer)
        assertTrue(fixture.optionsEvents.any { it.index == 0 && it.profileName == "Local" && it.interaction == C.INTERACTION_DIALOG })
        assertEquals(1, fixture.closes)
    }

    @Test fun unmatchedItemCanFailWithoutPreventingALaterMatchingItemInTheSameBatch() {
        val fixture = Fixture(request(count = 2), snapshot(profile("Allowed later", "org.example.safe.", """{"authorizer":"none"}""")),
            listOf("org.example.other", "org.example.safe.second"))
        fixture.rootAvailable = false
        fixture.run()

        assertNull(fixture.failure)
        val results = fixture.batchResults()
        assertEquals(listOf(false, true), results.map { it[C.FIELD_OK].asBoolean })
        assertEquals(E.AUTHORIZER_REQUIRED, results[0].getAsJsonObject(C.FIELD_ERROR)[C.FIELD_ERROR_CODE].asString)
        assertFalse(results[0].has(C.FIELD_AUTHORIZER))
        assertFalse(results[0].has(C.FIELD_SOURCE_DELETE_REQUESTED))
        assertEquals("none", results[1][C.FIELD_AUTHORIZER].asString)
        assertEquals(listOf("root", "none"), fixture.resolutions.map { it.options.authorizer })
        assertEquals(listOf(0, 1), fixture.discarded)
        assertEquals("org.example.safe.second", fixture.calls.single().request.prepared.packageName)
    }

    @Test fun absentOrFalseRemoteMarkerPreservesLegacyResolutionAndIgnoresEvenUnreadableProfiles() {
        for (marker in listOf("", ",\"applySourceProfiles\":false")) {
            val request = InstallRequest.parse("""{
                "id":"legacy","sources":[{"displayName":"source.apk","size":1}],
                "options":{"authorizer":"root"}$marker
            }""", 1)
            val fixture = Fixture(request, snapshot(profile("Would select NONE", "", """{"authorizer":"none"}""")).copy(readable = false),
                listOf("org.example.safe"))
            fixture.rootAvailable = false
            fixture.run()

            assertFalse(request.applySourceProfiles)
            assertEquals(E.AUTHORIZER_REQUIRED, fixture.failure?.code)
            assertEquals(listOf("initial", "resolve:root:current", "close", "failed"), fixture.events)
            assertTrue(fixture.preparedIndices.isEmpty())
            assertTrue(fixture.calls.isEmpty())
        }
    }

    @Test fun eachBatchItemResolvesItsOwnAuthorizerUserAndEngineAfterItsOwnMatch() {
        val fixture = Fixture(request(count = 2), snapshot(
            profile("Root personal", "org.example.first", """{"authorizer":"root","user":"10"}"""),
            profile("Shizuku work", "org.example.second", """{"authorizer":"shizuku","user":"11"}""")),
            listOf("org.example.first", "org.example.second"))
        fixture.run()

        assertNull(fixture.failure)
        assertEquals(listOf(Authorizer.ROOT, Authorizer.SHIZUKU), fixture.calls.map { it.authorizer })
        assertEquals(listOf(10, 11), fixture.calls.map { it.request.userId })
        assertEquals(listOf("root", "shizuku"), fixture.batchResults().map { it[C.FIELD_AUTHORIZER].asString })
        assertEquals(listOf(10, 10, 11, 11), fixture.versionTargets.map { it.userId })
        assertTrue(fixture.configured.isEmpty())
        assertEquals(listOf(0, 1), fixture.profileIndices)
    }

    @Test fun matchingUsesPreparedManifestIdentityInsteadOfTheSourceDisplayName() {
        val input = request(displayNames = listOf("org.example.filename.apk"))
        val fixture = Fixture(input, snapshot(
            profile("Misleading filename", "org.example.filename", """{"authorizer":"root"}"""),
            profile("Manifest identity", "org.example.manifest", """{"authorizer":"none"}""")),
            listOf("org.example.manifest"))
        fixture.rootAvailable = false
        fixture.run()

        assertNull(fixture.failure)
        assertEquals("org.example.filename.apk", fixture.calls.single().request.prepared.displayName)
        assertEquals("org.example.manifest", fixture.singleResult()[C.FIELD_PACKAGE_NAME].asString)
        assertEquals(listOf("none"), fixture.resolutions.map { it.options.authorizer })
        assertTrue(fixture.optionsEvents.all { it.profileName == "Manifest identity" })
    }

    @Test fun parsedExplicitFalseCurrentNoneAndNullRemainAuthoritativeInsideTheSession() {
        val input = InstallRequest.parse("""{
            "id":"explicit","applySourceProfiles":true,"sourceOrigin":"script",
            "sources":[{"displayName":"source.apk","size":1}],
            "options":{"authorizer":"none","user":"current","deleteSource":false,"dexopt":"none",
                "installer":null,"installReason":null,"packageSource":null,"grantAllRequestedPermissions":false}
        }""", 1)
        val fixture = Fixture(input, snapshot(profile("Aggressive defaults", "", """{
            "authorizer":"root","user":"10","deleteSource":true,"dexopt":"speed",
            "installer":"org.example.store","installReason":"policy","packageSource":"store",
            "grantAllRequestedPermissions":true,"requestUpdateOwnership":true
        }""")), listOf("org.example.explicit"))
        fixture.rootAvailable = false
        fixture.run()

        assertNull(fixture.failure)
        assertEquals(input.options.copy(requestUpdateOwnership = true), fixture.calls.single().request.options)
        assertEquals(Authorizer.NONE, fixture.calls.single().authorizer)
        assertFalse(fixture.singleResult()[C.FIELD_SOURCE_DELETE_REQUESTED].asBoolean)
        assertTrue(input.explicitOptions.containsAll(setOf(C.FIELD_DELETE_SOURCE, C.FIELD_INSTALLER, C.FIELD_DEXOPT, C.FIELD_USER)))
    }

    @Test fun finalDialogChoicesAreUsedWithoutApplyingTheProfileAgain() {
        val fixture = Fixture(request(), snapshot(profile("Dialog defaults", "", """{"authorizer":"none","deleteSource":true}""")),
            listOf("org.example.dialog"))
        fixture.configuration = { _, prepared, _, selected ->
            assertEquals("none", selected.options.authorizer)
            assertTrue(selected.options.deleteSource)
            val final = selected.options.copy(authorizer = "shizuku", user = "12", deleteSource = false)
            InstallSession.Selection(prepared, fixture.target(Authorizer.SHIZUKU, 12), final)
        }
        fixture.run()

        assertNull(fixture.failure)
        assertEquals(listOf(0), fixture.profileIndices)
        assertEquals(listOf("none"), fixture.resolutions.map { it.options.authorizer })
        val call = fixture.calls.single()
        assertEquals(Authorizer.SHIZUKU, call.authorizer)
        assertEquals(12, call.request.userId)
        assertFalse(call.request.options.deleteSource)
        assertEquals(call.request.options, fixture.cleanups.single().second)
        assertEquals(call.request.options, fixture.optionsEvents.last().options)
        assertFalse(fixture.singleResult()[C.FIELD_SOURCE_DELETE_REQUESTED].asBoolean)
    }

    @Test fun laterItemsKeepTheProcessingStartProfilesAndExplicitMaskDespiteExternalMutation() {
        val entries = mutableListOf(profile("Original", "", """{"authorizer":"none","deleteSource":true}"""))
        val explicit = mutableSetOf(C.FIELD_DELETE_SOURCE)
        val fixture = Fixture(request(count = 2, explicit = explicit), InstallProfileSnapshot(profiles = entries),
            listOf("org.example.first", "org.example.second"))
        fixture.rootAvailable = false
        fixture.engineAction = { index, installed ->
            if (index == 0) {
                entries.clear()
                entries += profile("Changed elsewhere", "", """{"authorizer":"root","deleteSource":true}""")
                explicit.clear()
                fixture.snapshot = InstallProfileSnapshot(profiles = entries)
            }
            success(installed)
        }
        fixture.run()

        assertNull(fixture.failure)
        assertEquals(listOf(Authorizer.NONE, Authorizer.NONE), fixture.calls.map { it.authorizer })
        assertTrue(fixture.calls.none { it.request.options.deleteSource })
        assertTrue(fixture.optionsEvents.all { it.profileName == "Original" })
        assertEquals(listOf(false, false), fixture.batchResults().map { it[C.FIELD_SOURCE_DELETE_REQUESTED].asBoolean })
    }

    @Test fun deferredResolutionAndLaterItemsShareTheOriginalAbsoluteDeadline() {
        val fixture = Fixture(request(count = 2, options = InstallOptions(authorizer = "root", timeoutMillis = 100)),
            snapshot(profile("Deferred", "", """{"authorizer":"root"}""")),
            listOf("org.example.first", "org.example.second"))
        fixture.engineAction = { index, installed ->
            if (index == 0) fixture.now = 170L
            success(installed)
        }
        fixture.beforeEngineCheck = { index -> if (index == 1) fixture.now = 201L }
        fixture.run()

        assertNull(fixture.failure)
        assertEquals(listOf(200L, 200L), fixture.resolveDeadlines)
        assertEquals(listOf(100L, 30L), fixture.resolveBudgets)
        assertEquals(listOf(200L, 200L), fixture.calls.map { it.request.deadlineMillis })
        val results = fixture.batchResults()
        assertEquals(listOf(true, false), results.map { it[C.FIELD_OK].asBoolean })
        assertEquals(E.TIMEOUT, results[1].getAsJsonObject(C.FIELD_ERROR)[C.FIELD_ERROR_CODE].asString)
        assertFalse(results[1].has(C.FIELD_SOURCE_DELETE_REQUESTED))
        assertEquals(listOf(0), fixture.cleanups.map { it.first })
    }

    @Test fun sourceDeletionRequestIsReportedOnlyForSuccessfulOptedInItemsAndIncludesExplicitFalse() {
        for (apply in listOf(false, true)) for (delete in listOf(false, true)) for (succeed in listOf(false, true)) {
            val fixture = Fixture(request(options = InstallOptions(authorizer = "root", deleteSource = delete), apply = apply),
                InstallProfileSnapshot(), listOf("org.example.result"))
            fixture.engineAction = { _, installed ->
                if (!succeed) throw InstallFailure(E.INSTALL_FAILED, "Fixture install failure")
                success(installed)
            }
            fixture.run()

            val result = fixture.itemResults.single().second
            assertEquals(succeed, result[C.FIELD_OK].asBoolean)
            assertEquals(apply && succeed, result.has(C.FIELD_SOURCE_DELETE_REQUESTED))
            if (apply && succeed) assertEquals(delete, result[C.FIELD_SOURCE_DELETE_REQUESTED].asBoolean)
            if (succeed) assertFalse(result[C.FIELD_SOURCE_DELETED].asBoolean)
            assertEquals(if (succeed) 1 else 0, fixture.cleanups.size)
            assertEquals(1, fixture.calls.size)
        }
    }

    @Test fun lateCleanupFailureKeepsConfirmedSuccessAndItsEffectiveDeletionRequest() {
        val fixture = Fixture(request(), snapshot(profile("Delete if possible", "", """{"deleteSource":true}""")),
            listOf("org.example.installed"))
        fixture.cleanup = { _, _ -> throw IllegalStateException("Fixture source is no longer writable") }
        fixture.run()

        assertNull(fixture.failure)
        val result = fixture.singleResult()
        assertTrue(result[C.FIELD_OK].asBoolean)
        assertTrue(result[C.FIELD_SOURCE_DELETE_REQUESTED].asBoolean)
        assertFalse(result[C.FIELD_SOURCE_DELETED].asBoolean)
        assertTrue(result.getAsJsonArray(C.FIELD_NOTES).any { it.asString.contains("cleanup") })
        assertEquals(1, fixture.calls.size)
        assertEquals(1, fixture.cleanups.size)
    }

    @Test fun profileAuthorizationCannotBypassPackagePolicyBeforeConfigurationOrWriting() {
        val fixture = Fixture(request(), snapshot(profile("No special privileges", "", """{"authorizer":"none"}""")),
            listOf("org.example.blocked"))
        fixture.policy = { throw InstallFailure(E.BLOCKED_BY_POLICY, "Fixture package rule") }
        fixture.run()

        assertEquals(E.BLOCKED_BY_POLICY, fixture.failure?.code)
        assertTrue(fixture.resolutions.isEmpty())
        assertTrue(fixture.configured.isEmpty())
        assertTrue(fixture.calls.isEmpty())
        assertTrue(fixture.cleanups.isEmpty())
        assertFalse(fixture.itemResults.single().second.has(C.FIELD_SOURCE_DELETE_REQUESTED))
        assertEquals(listOf(0), fixture.discarded)
    }

    private fun profile(name: String, prefix: String, options: String) = InstallProfile(name = name, packagePrefix = prefix,
        overrides = InstallProfileOverrides.parse(JsonParser.parseString(options).asJsonObject))

    private fun snapshot(vararg profiles: InstallProfile) = InstallProfileSnapshot(profiles = profiles.toList())

    private fun request(count: Int = 1, options: InstallOptions = InstallOptions(authorizer = "root"),
        apply: Boolean = true, explicit: Set<String> = emptySet(), displayNames: List<String> = List(count) { "source$it.apk" }) =
        InstallRequest("profiles", displayNames.mapIndexed { i, name -> SourceEntry(i, i, name, 1) }, C.INTERACTION_AUTO,
            options, origin = C.SOURCE_HOST, applySourceProfiles = apply, explicitOptions = explicit)

    private fun success(request: InstallEngine.Request) = InstallEngine.Result(request.prepared.packageName, emptyList(), request.interaction)

    private data class EngineCall(val authorizer: Authorizer, val request: InstallEngine.Request)
    private data class OptionsEvent(val index: Int, val options: InstallOptions, val interaction: String, val profileName: String?)

    private inner class Fixture(private val request: InstallRequest, var snapshot: InstallProfileSnapshot,
        private val packageNames: List<String>) : InstallSession.Environment, InstallSession.Listener {
        var now = 100L
        var rootAvailable = true
        var closes = 0
        var completed: JsonObject? = null
        var failure: InstallFailure? = null
        var configuration: ((Int, PreparedPackage, InstallSession.Target, InstallRequest) -> InstallSession.Selection)? = null
        var engineAction: (Int, InstallEngine.Request) -> InstallEngine.Result = { _, request -> success(request) }
        var beforeEngineCheck: (Int) -> Unit = {}
        var policy: () -> Unit = {}
        var cleanup: (Int, InstallOptions) -> InstallSession.SourceCleanup = { _, _ -> InstallSession.SourceCleanup() }
        val events = mutableListOf<String>()
        val calls = mutableListOf<EngineCall>()
        val resolutions = mutableListOf<InstallRequest>()
        val resolveDeadlines = mutableListOf<Long>()
        val resolveBudgets = mutableListOf<Long>()
        val preparedIndices = mutableListOf<Int>()
        val profileIndices = mutableListOf<Int>()
        val configured = mutableListOf<Int>()
        val discarded = mutableListOf<Int>()
        val versionTargets = mutableListOf<InstallSession.Target>()
        val cleanups = mutableListOf<Pair<Int, InstallOptions>>()
        val itemResults = mutableListOf<Pair<Int, JsonObject>>()
        val optionsEvents = mutableListOf<OptionsEvent>()
        private var plan: InstallProfilePlan? = null
        private val session = InstallSession(request, this, this) { now }

        fun run() = session.start(Executor { it.run() })
        fun singleResult() = requireNotNull(completed)
        fun batchResults() = requireNotNull(completed).getAsJsonArray(C.FIELD_RESULTS).map { it.asJsonObject }

        override fun initialTarget(request: InstallRequest, deadlineMillis: Long, checkActive: () -> Unit): InstallSession.Target? {
            events += "initial"
            val selected = InstallProfilePlan(snapshot, request).also { plan = it }
            return if (selected.deferIdentity) null else resolve(request, deadlineMillis, checkActive)
        }

        override fun itemRequest(index: Int, prepared: PreparedPackage, request: InstallRequest): InstallRequest {
            events += "profile:$index"
            profileIndices += index
            return requireNotNull(plan).forPackage(prepared.packageName)
        }

        override fun resolve(request: InstallRequest, deadlineMillis: Long, checkActive: () -> Unit): InstallSession.Target {
            checkActive()
            events += "resolve:${request.options.authorizer}:${request.options.user}"
            resolutions += request
            resolveDeadlines += deadlineMillis
            resolveBudgets += deadlineMillis - now
            val authorizer = Authorizer.fromId(request.options.authorizer) ?: Authorizer.ROOT
            if (authorizer == Authorizer.ROOT && !rootAvailable) throw InstallFailure(E.AUTHORIZER_REQUIRED, "Fixture root is unavailable")
            val userId = if (request.options.user == C.USER_CURRENT) 0 else request.options.user.toInt()
            return target(authorizer, userId)
        }

        fun target(authorizer: Authorizer, userId: Int) = InstallSession.Target(authorizer, userId, object : InstallEngine {
            override fun install(request: InstallEngine.Request, listener: InstallEngine.Listener, checkCancelled: () -> Unit): InstallEngine.Result {
                val index = calls.size
                calls += EngineCall(authorizer, request)
                events += "install:$index:${authorizer.id}:${request.userId}"
                beforeEngineCheck(index)
                checkCancelled()
                return engineAction(index, request)
            }
        })

        override fun interaction(request: InstallRequest, target: InstallSession.Target) =
            if (request.interaction == C.INTERACTION_AUTO && !target.authorizer.privileged) C.INTERACTION_DIALOG else request.interaction

        override fun prepare(index: Int, sources: List<SourceEntry>, checkActive: () -> Unit): PreparedPackage {
            checkActive()
            events += "prepare:$index"
            preparedIndices += index
            return PreparedPackage("apk", sources.first().displayName, 1, packageNames[index], "1", 1, "Fixture", 24, 28,
                listOf(PlannedApk("base.apk", File("fixture-$index.apk"), 1, null)), emptyList(), null, emptyList(), emptyList(), true)
        }

        override fun checkInstallPolicy(prepared: PreparedPackage) { events += "policy:${prepared.packageName}"; policy() }
        override fun installedVersion(packageName: String, target: InstallSession.Target): InstallSession.Version? { versionTargets += target; return null }
        override fun confirm(prepared: PreparedPackage, target: InstallSession.Target, deadlineMillis: Long, checkActive: () -> Unit) = Unit
        override fun configure(index: Int, prepared: PreparedPackage, target: InstallSession.Target, request: InstallRequest,
            deadlineMillis: Long, checkActive: () -> Unit): InstallSession.Selection {
            configured += index
            events += "configure:$index"
            return configuration?.invoke(index, prepared, target, request) ?: InstallSession.Selection(prepared, target, request.options)
        }

        override fun onInstalled(index: Int, options: InstallOptions): InstallSession.SourceCleanup {
            cleanups += index to options
            return cleanup(index, options)
        }
        override fun onUserAction(intent: Intent) = Unit
        override fun discardItem(index: Int) { discarded += index }
        override fun close() { closes++; events += "close" }
        override fun onOptionsResolved(index: Int, options: InstallOptions, interaction: String, profileName: String?) {
            optionsEvents += OptionsEvent(index, options, interaction, profileName)
        }
        override fun onItemResult(index: Int, result: JsonObject) { itemResults += index to result.deepCopy() }
        override fun onCompleted(result: JsonObject) { completed = result; events += "completed" }
        override fun onFailed(failure: InstallFailure) { this.failure = failure; events += "failed" }
    }
}
