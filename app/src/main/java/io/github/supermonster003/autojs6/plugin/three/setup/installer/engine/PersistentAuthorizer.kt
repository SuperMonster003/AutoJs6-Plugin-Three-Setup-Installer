package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerResolver
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerState
import org.autojs.plugin.installer.api.InstallerErrorCodes

/** Auto considers only transports capable of this operation; an explicit identity never falls back. */
internal object PersistentAuthorizer {
    fun resolve(requested: String, states: Map<Authorizer, AuthorizerState>, order: List<Authorizer>,
        enabled: Set<Authorizer>, sdk: Int, currentUser: Int): Authorizer {
        if (!Authorizer.isAuto(requested)) {
            val selected = AuthorizerResolver.resolve(requested, states, order, enabled)
            if (selected in setOf(Authorizer.ROOT, Authorizer.DHIZUKU)) return selected
            throw InstallFailure(InstallerErrorCodes.AUTHORIZER_REQUIRED, "Persistent defaults require Dhizuku or a supported system-UID Root bridge")
        }
        return order.firstOrNull { it in enabled && states[it]?.usable == true &&
            ((it == Authorizer.ROOT && currentUser == 0) || (it == Authorizer.DHIZUKU && sdk in 26..33)) }
            ?: throw InstallFailure(InstallerErrorCodes.AUTHORIZER_REQUIRED, "No enabled authorizer supports persistent defaults on this device/user")
    }
}
