package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstallKindTest {
    @Test fun versionCodesDecideWhatAnApprovedInstallationDoes() {
        assertEquals(InstallKind.NEW, InstallKind.of(installedKnown = true, installedCode = null, archiveCode = 7))
        assertEquals(InstallKind.UPDATE, InstallKind.of(installedKnown = true, installedCode = 6, archiveCode = 7))
        assertEquals(InstallKind.REINSTALL, InstallKind.of(installedKnown = true, installedCode = 7, archiveCode = 7))
        assertEquals(InstallKind.DOWNGRADE, InstallKind.of(installedKnown = true, installedCode = 8, archiveCode = 7))
    }

    @Test fun unknownFactsNeverClaimAKind() {
        // Another user's installed state may be unreadable; an absent record must not read as "new".
        assertNull(InstallKind.of(installedKnown = false, installedCode = null, archiveCode = 7))
        assertNull(InstallKind.of(installedKnown = false, installedCode = 6, archiveCode = 7))
        assertNull(InstallKind.of(installedKnown = true, installedCode = 6, archiveCode = null))
        // A package without a readable version is still new when nothing is installed.
        assertEquals(InstallKind.NEW, InstallKind.of(installedKnown = true, installedCode = null, archiveCode = null))
    }
}
