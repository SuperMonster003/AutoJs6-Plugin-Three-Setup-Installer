package io.github.supermonster003.autojs6.plugin.three.setup.installer.profiles

import android.content.Intent
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.*
import io.github.supermonster003.autojs6.plugin.three.setup.installer.policy.DialogSafetyApproval
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.ArchiveOpener
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PlannedApk
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.installRetryInteraction
import org.autojs.plugin.installer.api.InstallerContract as C
import org.autojs.plugin.installer.api.InstallerErrorCodes as E
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.concurrent.Executor

class InstallProfileRetryTest {
    @Test fun confirmedAutoRetryRevisitsConfigurationBeforeAnyPreviouslyDeselectedSplitCanBeWritten() {
        val base = InstallRequest("first", listOf(SourceEntry(0, 0, "fixture.apks", 2)), C.INTERACTION_AUTO,
            InstallOptions(), applySourceProfiles = true, explicitOptions = emptySet())
        val profile = InstallProfile(name = "Use system confirmation", overrides = InstallProfileOverrides.empty()
            .with(C.FIELD_AUTHORIZER, JsonPrimitive(C.AUTHORIZER_NONE)))
        val events = mutableListOf<String>()
        val first = Attempt(base, profile, failInstallation = true, events = events)
        first.run()
        assertEquals(E.INSUFFICIENT_STORAGE, first.failure?.code)
        assertEquals(C.AUTHORIZER_ROOT, first.confirmedOptions!!.authorizer)
        assertEquals(listOf("base.apk"), first.writtenApks)
        assertEquals(listOf("configure:none", "review:dialog", "validate", "install:dialog"), events)

        events.clear()
        val retry = base.copy(id = "retry", options = requireNotNull(first.confirmedOptions),
            interaction = installRetryInteraction(base.interaction, confirmedOptions = true),
            explicitOptions = InstallProfileOverrides.ALLOWED_KEYS)
        // The original profile still selects NONE, but the last confirmed options must win.
        // Prepare returns the complete archive again; only configure can remove optional.apk.
        val second = Attempt(retry, profile, failInstallation = false, events = events)
        second.run()
        assertNull(second.failure)
        assertTrue(second.completed!!.get(C.FIELD_OK).asBoolean)
        assertEquals(listOf("base.apk"), second.writtenApks)
        assertEquals(listOf("configure:root", "review:dialog", "validate", "install:dialog"), events)
    }

    @Test fun onlyConfirmedAutoRetriesAcquireAnExplicitDialog() {
        for (interaction in listOf(C.INTERACTION_AUTO, C.INTERACTION_DIALOG, C.INTERACTION_SILENT, C.INTERACTION_NOTIFICATION)) {
            assertEquals(interaction, installRetryInteraction(interaction, confirmedOptions = false))
            assertEquals(if (interaction == C.INTERACTION_AUTO) C.INTERACTION_DIALOG else interaction,
                installRetryInteraction(interaction, confirmedOptions = true))
        }
    }

    private class Attempt(private val request: InstallRequest, profile: InstallProfile,
        private val failInstallation: Boolean, private val events: MutableList<String>) : InstallSession.Environment, InstallSession.Listener {
        private val plan = InstallProfilePlan(InstallProfileSnapshot(profiles = listOf(profile)), request)
        var confirmedOptions: InstallOptions? = null
        var writtenApks = emptyList<String>()
        var completed: JsonObject? = null
        var failure: InstallFailure? = null

        fun run() = InstallSession(request, this, this) { 100L }.start(Executor { it.run() })
        override fun initialTarget(request: InstallRequest, deadlineMillis: Long, checkActive: () -> Unit): InstallSession.Target? = null
        override fun itemRequest(index: Int, prepared: PreparedPackage, request: InstallRequest) = plan.forPackage(prepared.packageName)
        override fun resolve(request: InstallRequest, deadlineMillis: Long, checkActive: () -> Unit): InstallSession.Target =
            target(requireNotNull(Authorizer.fromId(request.options.authorizer)))
        override fun interaction(request: InstallRequest, target: InstallSession.Target) =
            if (request.interaction == C.INTERACTION_AUTO && !target.authorizer.privileged) C.INTERACTION_DIALOG else request.interaction
        override fun prepare(index: Int, sources: List<SourceEntry>, checkActive: () -> Unit) = PreparedPackage(
            C.FORMAT_APKS, "fixture.apks", 2, "org.example.fixture", "1", 1, "Fixture", 24, 28,
            listOf(PlannedApk("base.apk", File("base.apk"), 1, null), PlannedApk("optional.apk", File("optional.apk"), 1, "optional")),
            emptyList(), null, emptyList(), emptyList(), true)
        override fun confirm(prepared: PreparedPackage, target: InstallSession.Target, deadlineMillis: Long, checkActive: () -> Unit) = Unit
        override fun configure(index: Int, prepared: PreparedPackage, target: InstallSession.Target, request: InstallRequest,
            deadlineMillis: Long, checkActive: () -> Unit): InstallSession.Selection {
            events += "configure:${target.authorizer.id}"
            val options = request.options.copy(authorizer = C.AUTHORIZER_ROOT)
            confirmedOptions = options
            return InstallSession.Selection(ArchiveOpener.select(prepared, setOf("base.apk")), this.target(Authorizer.ROOT), options)
        }
        override fun reviewSafety(index: Int, prepared: PreparedPackage, target: InstallSession.Target, options: InstallOptions,
            interaction: String, deadlineMillis: Long, onReviewRequired: () -> Unit, checkActive: () -> Unit): DialogSafetyApproval? {
            events += "review:$interaction"
            return null
        }
        override fun validateSafety(index: Int, prepared: PreparedPackage, target: InstallSession.Target, options: InstallOptions,
            approval: DialogSafetyApproval?, checkActive: () -> Unit) { events += "validate" }
        override fun installedVersion(packageName: String, target: InstallSession.Target): InstallSession.Version? = null
        override fun onUserAction(intent: Intent) = Unit
        override fun discardItem(index: Int) = Unit
        override fun close() = Unit
        override fun onCompleted(result: JsonObject) { completed = result }
        override fun onFailed(failure: InstallFailure) { this.failure = failure }

        private fun target(authorizer: Authorizer) = InstallSession.Target(authorizer, 0, object : InstallEngine {
            override fun install(request: InstallEngine.Request, listener: InstallEngine.Listener, checkCancelled: () -> Unit): InstallEngine.Result {
                events += "install:${request.interaction}"
                writtenApks = request.prepared.apks.map { it.name }
                if (failInstallation) throw InstallFailure(E.INSUFFICIENT_STORAGE, "Temporary fixture failure")
                return InstallEngine.Result(request.prepared.packageName, emptyList(), request.interaction)
            }
        })
    }
}
