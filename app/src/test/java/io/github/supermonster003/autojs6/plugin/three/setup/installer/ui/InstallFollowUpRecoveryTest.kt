package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import org.autojs.plugin.installer.api.InstallerContract as C
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class InstallFollowUpRecoveryTest {
    private fun state(knownSuccess: Boolean, pending: Boolean) = InstallPresentation.Snapshot(1, C.STAGE_OPTIMIZING, 0, 1f,
        listOf(InstallPresentation.Item("Fixture", result = if (knownSuccess) JsonObject().apply {
            addProperty(C.FIELD_OK, true); addProperty(C.FIELD_PACKAGE_NAME, "example.fixture")
        } else null, followUpPending = pending)), null, false, null, false, false)

    @Test fun deathDuringFollowUpPreservesConfirmedSuccessWithoutOfferingAnyReplay() {
        val snapshot = InstallRecoverySnapshot.capture(UUID.randomUUID().toString(), state(true, true), InstallOptions(), 1_000, 10_000)
        val restored = InstallRecoveryCodec.decode(InstallRecoveryCodec.encode(snapshot)).display()
        assertTrue(restored.recovered && restored.interrupted && restored.terminal)
        assertTrue(restored.items.single().result!![C.FIELD_OK].asBoolean)
        assertTrue(restored.items.single().followUpPending)
        assertFalse(restored.canRetry)
        assertNull(restored.prompt)
    }

    @Test fun anUnconfirmedInstallCannotForgeTheFollowUpMarker() {
        val snapshot = InstallRecoverySnapshot.capture(UUID.randomUUID().toString(), state(false, true), InstallOptions(), 1_000, 10_000)
        assertThrows(IllegalArgumentException::class.java) { InstallRecoveryCodec.encode(snapshot) }
    }

    @Test fun oldSnapshotsWithoutTheOptionalMarkerKeepTheirOriginalMeaning() {
        val snapshot = InstallRecoverySnapshot.capture(UUID.randomUUID().toString(), state(true, false), InstallOptions(), 1_000, 10_000)
        val bytes = InstallRecoveryCodec.encode(snapshot)
        assertFalse(bytes.toString(Charsets.UTF_8).contains("followUpPending"))
        assertFalse(InstallRecoveryCodec.decode(bytes).display().items.single().followUpPending)
    }

}
