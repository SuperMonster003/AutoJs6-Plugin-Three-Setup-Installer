package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import org.autojs.plugin.installer.api.InstallerContract as C
import org.junit.Assert.*
import org.junit.Test

class InstallationNoticeRegistryTest {
    @Test fun notificationConfirmationOwnsForegroundAndCanCancelBeforeAnyWrite() {
        val state = InstallationNoticeRegistry<String>()
        val change = state.update("notice", "Package", C.STAGE_PREPARING, 0f, "owner", mandatory = true)
        assertFalse(change.beganWriting)
        assertTrue(change.beganForeground)
        assertTrue(state.writers().single().mandatory)
        assertEquals(listOf("owner"), state.cancel("notice"))
        assertFalse(state.update("notice", "Package", C.STAGE_CONFIRMING, 0f, "same-owner").beganForeground)
        assertTrue(state.writers().single().mandatory)
        assertTrue(state.cancel("notice").isEmpty())
        state.finish("notice")
        assertTrue(state.writers().isEmpty())
        assertFalse(state.update("notice", "Old action", C.STAGE_CONFIRMING, 0f, "late", mandatory = true).accepted)
    }

    @Test fun preparingDoesNotStartForegroundAndBatchKeepsTheServiceAfterItsFirstWrite() {
        val state = InstallationNoticeRegistry<String>()
        assertFalse(state.update("one", "App", C.STAGE_PREPARING, 0f, "first").beganWriting)
        assertTrue(state.writers().isEmpty())
        assertTrue(state.update("one", "App", C.STAGE_WRITING, 0f, "first").beganWriting)
        assertFalse(state.update("one", "App", C.STAGE_WRITING, 0.5f, "first").beganWriting)
        state.update("one", "Next app", C.STAGE_PREPARING, 0f, "second")
        assertEquals("Next app", state.writers().single().name)
        assertFalse(state.update("one", "Next app", C.STAGE_WRITING, 0.2f, "second").beganWriting)
    }

    @Test fun completingOneSessionLeavesOtherWritersAndRejectsItsLateCallbacks() {
        val state = InstallationNoticeRegistry<String>()
        state.update("one", "One", C.STAGE_WRITING, 0.3f, "one")
        state.update("two", "Two", C.STAGE_WRITING, 0.4f, "two")
        assertEquals("one", state.finish("one")?.payload)
        assertEquals(listOf("two"), state.writers().map { it.token })
        assertFalse(state.update("one", "One", C.STAGE_WRITING, 0.9f, "late").accepted)
        assertNull(state.finish("one"))
        state.finish("two")
        assertTrue(state.writers().isEmpty())
    }

    @Test fun notificationCancellationTargetsTheExistingSessionExactlyOnce() {
        val state = InstallationNoticeRegistry<String>()
        state.update("one", "One", C.STAGE_WRITING, 0.3f, "one")
        state.update("two", "Two", C.STAGE_WRITING, 0.4f, "two")
        assertEquals(listOf("one"), state.cancel("one"))
        assertTrue(state.cancel("one").isEmpty())
        state.update("one", "One", C.STAGE_WRITING, 0.5f, "replacement")
        assertTrue(state.cancel("one").isEmpty())
        assertEquals(listOf("two"), state.cancel(null))
        assertTrue(state.cancel(null).isEmpty())
    }

    @Test fun progressIsFiniteAndCapacityReturnsAfterACompletedSession() {
        val state = InstallationNoticeRegistry<String>(maximum = 2)
        state.update("one", "One", C.STAGE_WRITING, Float.NaN, "one")
        state.update("two", "Two", C.STAGE_WRITING, 3f, "two")
        assertEquals(listOf(0f, 1f), state.writers().map { it.progress })
        assertFalse(state.update("three", "Three", C.STAGE_WRITING, 0f, "three").accepted)
        state.finish("one")
        assertTrue(state.update("three", "Three", C.STAGE_WRITING, Float.POSITIVE_INFINITY, "three").accepted)
        assertEquals(listOf(1f, 0f), state.writers().map { it.progress })
    }

    @Test fun finishingBeforeTheFirstProgressPreventsLateWorkFromStartingAService() {
        val state = InstallationNoticeRegistry<String>()
        assertNull(state.finish("already-complete"))
        assertFalse(state.update("already-complete", "App", C.STAGE_WRITING, 1f, "late").accepted)
        assertTrue(state.writers().isEmpty())
    }
}
