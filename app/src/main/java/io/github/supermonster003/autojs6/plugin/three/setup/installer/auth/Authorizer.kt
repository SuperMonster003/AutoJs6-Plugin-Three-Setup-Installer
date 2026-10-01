package io.github.supermonster003.autojs6.plugin.three.setup.installer.auth

import org.autojs.plugin.installer.api.InstallerContract

/** The authorizers of contract version 1 (roadmap D2); `auto` is a request-side choice, never an authorizer. */
internal enum class Authorizer(val id: String, val privileged: Boolean) {
    NONE(InstallerContract.AUTHORIZER_NONE, privileged = false),
    SHIZUKU(InstallerContract.AUTHORIZER_SHIZUKU, privileged = true),
    ROOT(InstallerContract.AUTHORIZER_ROOT, privileged = true),
    DHIZUKU(InstallerContract.AUTHORIZER_DHIZUKU, privileged = true),
    ;

    companion object {
        /** The order `auto` tries authorizers in (roadmap D14); the settings page may reorder it in P5. */
        val DEFAULT_ORDER: List<Authorizer> = listOf(SHIZUKU, ROOT, DHIZUKU, NONE)

        fun fromId(id: String?): Authorizer? = entries.firstOrNull { it.id == id }

        fun isAuto(id: String?): Boolean = id == null || id == InstallerContract.AUTHORIZER_AUTO
    }
}

/** What an authorizer looks like right now, as reported through `getAuthorizerState` (protocol document). */
internal data class AuthorizerState(
    val authorizer: Authorizer,
    val available: Boolean,
    val running: Boolean,
    val granted: Boolean,
    val reason: String? = null,
) {
    val usable: Boolean get() = available && running && granted
}
