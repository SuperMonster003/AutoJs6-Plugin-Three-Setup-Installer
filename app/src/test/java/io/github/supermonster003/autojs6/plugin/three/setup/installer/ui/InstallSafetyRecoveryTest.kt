package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.policy.*
import org.autojs.plugin.installer.api.InstallerContract as C
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class InstallSafetyRecoveryTest {
    private fun state(knownSuccess: Boolean, pending: Boolean) = InstallPresentation.Snapshot(1, C.STAGE_OPTIMIZING, 0, 1f,
        listOf(InstallPresentation.Item("Fixture", result = if (knownSuccess) JsonObject().apply {
            addProperty(C.FIELD_OK, true); addProperty(C.FIELD_PACKAGE_NAME, "example.fixture")
        } else null, followUpPending = pending)), null, false, null, false, false)

    @Test fun aCheckedSigningReviewNeverBecomesARestoredApproval() {
        val binding = InstallSafetyBinding(0, "example.fixture", null,
            listOf(ApkSafetyDigest("base.apk", 1, "a".repeat(64))), 0, "current", "root", "policy-revision",
            setOf("source"), InstalledSigningFact(true, true, signers = setOf("other")))
        val review = InstallPresentation.SafetyReview(binding).also { it.acknowledged = true }
        review.decision.answer(DialogSafetyApproval.fromDialog("owner", review.token, binding))
        val snapshot = InstallRecoverySnapshot.capture(UUID.randomUUID().toString(),
            state(false, false).copy(stage = C.STAGE_CONFIRMING, safetyReview = review), InstallOptions(), 1_000, 10_000)
        val bytes = InstallRecoveryCodec.encode(snapshot)
        val json = bytes.toString(Charsets.UTF_8)
        assertFalse(json.contains(review.token))
        assertFalse(json.contains("policy-revision"))
        val restored = InstallRecoveryCodec.decode(bytes).display()
        assertNull(restored.safetyReview)
        assertNull(restored.prompt)
        assertTrue(restored.items.single().result!![C.FIELD_OK].asBoolean == false)
        assertFalse(restored.canRetry)
    }
}
