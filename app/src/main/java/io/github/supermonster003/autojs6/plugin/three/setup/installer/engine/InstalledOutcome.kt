package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import io.github.supermonster003.autojs6.plugin.three.setup.installer.policy.InstalledSigningFact

/**
 * Decides whether the device proves that a committed item was installed although its platform
 * session ended without success. Some system installers (ColorOS) install the APK through a
 * session of their own and then reject the caller's session ("User rejected permissions"), and
 * a caller that updates itself is killed by the very installation it asked for. The result must
 * follow the package's actual state, never a guess: the package must now be installed for the
 * target user with exactly the prepared versionCode and signing certificates, and it must have
 * changed (newly present, or a new lastUpdateTime) since it was sampled before the session opened.
 */
internal object InstalledOutcome {

    fun confirms(versionCode: Long?, sourceSigners: Set<String>, before: InstalledSigningFact?, after: InstalledSigningFact?): Boolean {
        if (versionCode == null || sourceSigners.isEmpty()) return false
        if (before == null || !before.known || after == null || !after.known) return false
        if (!after.found || !after.installedForUser || after.versionCode != versionCode || after.signers != sourceSigners) return false
        if (!before.found || !before.installedForUser) return true
        val previous = before.lastUpdateTime ?: return false
        val current = after.lastUpdateTime ?: return false
        return current != previous
    }

    /** The note added to a result whose platform answer was contradicted by the device. */
    fun note(reported: InstallFailure): String {
        val detail = reported.systemMessage?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
        return "The package installer reported ${reported.code}$detail, but the package is installed with the expected version and signing certificate"
    }
}
