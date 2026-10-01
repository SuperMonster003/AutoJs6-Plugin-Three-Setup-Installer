package io.github.supermonster003.autojs6.plugin.three.setup.installer.auth

import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import org.autojs.plugin.installer.api.InstallerErrorCodes

/**
 * Chooses the authorizer of one request (roadmap D8 / D14). An explicit request never falls back:
 * an unusable authorizer fails with `AUTHORIZER_UNAVAILABLE` or `AUTHORIZER_DENIED`. `auto` walks
 * [order] and takes the first usable authorizer, skipping the ones the settings disabled; when no
 * privileged authorizer is usable it ends at `none` without prompting anyone.
 *
 * Pure Kotlin: the states are supplied by the caller, so the JVM tests cover every branch.
 */
internal object AuthorizerResolver {

    fun resolve(
        requested: String?,
        states: Map<Authorizer, AuthorizerState>,
        order: List<Authorizer> = Authorizer.DEFAULT_ORDER,
        enabled: Set<Authorizer> = Authorizer.entries.toSet(),
    ): Authorizer {
        if (!Authorizer.isAuto(requested)) {
            val explicit = Authorizer.fromId(requested)
                ?: throw InstallFailure(InstallerErrorCodes.INVALID_ARGUMENT, "Unknown authorizer: $requested")
            val state = states[explicit] ?: throw InstallFailure(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, "${explicit.id} is not available")
            return when {
                state.usable -> explicit
                !state.available || !state.running -> throw InstallFailure(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, unavailableMessage(state))
                else -> throw InstallFailure(InstallerErrorCodes.AUTHORIZER_DENIED, "${explicit.id} is not granted to the plugin")
            }
        }
        val candidates = (order + Authorizer.NONE).distinct().filter { it in enabled }
        return candidates.firstOrNull { states[it]?.usable == true }
            ?: throw InstallFailure(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, "No enabled authorizer is usable")
    }

    private fun unavailableMessage(state: AuthorizerState): String = when {
        state.reason != null -> "${state.authorizer.id} is not available: ${state.reason}"
        !state.available -> "${state.authorizer.id} is not installed on this device"
        else -> "${state.authorizer.id} is not running"
    }
}
