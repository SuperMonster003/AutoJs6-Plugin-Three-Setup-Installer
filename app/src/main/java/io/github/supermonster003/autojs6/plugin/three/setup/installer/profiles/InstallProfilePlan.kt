package io.github.supermonster003.autojs6.plugin.three.setup.installer.profiles

import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallRequest
import java.util.Collections

/** One processing-start snapshot. A later item or editor save cannot alter its matching inputs. */
internal class InstallProfilePlan(snapshot: InstallProfileSnapshot, request: InstallRequest) {
    private val original = request.copy(sources = Collections.unmodifiableList(request.sources.toList()),
        explicitOptions = Collections.unmodifiableSet(request.explicitOptions.toSet()), matchedProfileName = null)
    private val profiles = if (request.applySourceProfiles) snapshot.frozen() else InstallProfileSnapshot()
    init { require(profiles.readable) { "Installation profiles are unreadable" } }
    val deferIdentity: Boolean = profiles.hasCandidates(original.origin)

    fun forPackage(packageName: String?): InstallRequest {
        val profile = packageName?.let { profiles.match(original.origin, it) } ?: return original
        return original.copy(options = profile.overrides.apply(original.options, original.explicitOptions), matchedProfileName = profile.name)
    }
}
