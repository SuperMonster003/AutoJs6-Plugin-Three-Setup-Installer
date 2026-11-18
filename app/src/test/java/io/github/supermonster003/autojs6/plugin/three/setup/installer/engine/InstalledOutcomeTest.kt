package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import io.github.supermonster003.autojs6.plugin.three.setup.installer.policy.InstalledSigningFact
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstalledOutcomeTest {
    private val signers = setOf("cert")
    private val absent = InstalledSigningFact(known = true, found = false)
    private val old = InstalledSigningFact(true, true, true, signers, 6641, "6.8.1", 1_000)
    private val new = InstalledSigningFact(true, true, true, signers, 6642, "6.8.1", 2_000)

    @Test fun `a new install or a changed update with the expected identity is confirmed`() {
        assertTrue(InstalledOutcome.confirms(6642, signers, absent, new))
        assertTrue(InstalledOutcome.confirms(6642, signers, old, new))
        // A reinstall of the same version only differs in its update time.
        assertTrue(InstalledOutcome.confirms(6642, signers, new, new.copy(lastUpdateTime = 3_000)))
        assertTrue(InstalledOutcome.confirms(6642, signers, old.copy(installedForUser = false), new))
    }

    @Test fun `an unchanged package is never confirmed`() {
        assertFalse(InstalledOutcome.confirms(6642, signers, new, new))
        assertFalse(InstalledOutcome.confirms(6641, signers, old, old))
    }

    @Test fun `another version or another signer is never confirmed`() {
        assertFalse(InstalledOutcome.confirms(6643, signers, old, new))
        assertFalse(InstalledOutcome.confirms(6642, setOf("other"), old, new))
        assertFalse(InstalledOutcome.confirms(6642, signers, old, new.copy(signers = signers + "extra")))
        assertFalse(InstalledOutcome.confirms(6642, signers, old, new.copy(installedForUser = false)))
        assertFalse(InstalledOutcome.confirms(6642, signers, old, absent))
    }

    @Test fun `unknown facts are never confirmed`() {
        assertFalse(InstalledOutcome.confirms(null, signers, absent, new))
        assertFalse(InstalledOutcome.confirms(6642, emptySet(), absent, new))
        assertFalse(InstalledOutcome.confirms(6642, signers, null, new))
        assertFalse(InstalledOutcome.confirms(6642, signers, absent, null))
        assertFalse(InstalledOutcome.confirms(6642, signers, InstalledSigningFact(known = false, found = false), new))
        assertFalse(InstalledOutcome.confirms(6642, signers, absent, new.copy(known = false)))
        assertFalse(InstalledOutcome.confirms(6642, signers, old.copy(lastUpdateTime = null), new))
        assertFalse(InstalledOutcome.confirms(6642, signers, old, new.copy(lastUpdateTime = null)))
    }

    @Test fun `the note keeps the reported code and the system message`() {
        val aborted = InstallStatusMapper.toFailure(InstallStatusMapper.STATUS_FAILURE_ABORTED,
            "INSTALL_FAILED_ABORTED: User rejected permissions", "example.package")
        assertEquals("The package installer reported USER_CANCELLED (INSTALL_FAILED_ABORTED: User rejected permissions), " +
            "but the package is installed with the expected version and signing certificate", InstalledOutcome.note(aborted))
        assertEquals("The package installer reported CANCELLED, but the package is installed with the expected version and signing certificate",
            InstalledOutcome.note(InstallFailure(InstallerErrorCodes.CANCELLED, "Installation interrupted")))
    }
}
