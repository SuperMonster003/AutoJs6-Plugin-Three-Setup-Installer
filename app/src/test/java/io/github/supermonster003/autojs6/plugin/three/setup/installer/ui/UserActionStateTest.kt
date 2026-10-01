package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.UserActionState.Action
import org.junit.Assert.*
import org.junit.Test

class UserActionStateTest {
    @Test fun `unknown source permission returns to the original confirmation only once`() {
        val state = UserActionState(requiresPermission = true)
        assertEquals(Action.REQUEST_PERMISSION, state.start(false))
        assertEquals(Action.NONE, state.start(false))
        assertEquals(Action.CONFIRM, state.permissionResult(true))
        assertEquals(Action.NONE, state.permissionResult(true))
        assertEquals(Action.NONE, state.start(true))
        assertEquals(Action.ACCEPTED, state.confirmationResult(true))
        assertTrue(state.accepted)
    }

    @Test fun `a recreation during either child activity cannot launch a second child`() {
        val state = UserActionState(requiresPermission = true)
        state.start(false)
        repeat(3) { assertEquals(Action.NONE, state.start(false)) }
        state.permissionResult(true)
        repeat(3) { assertEquals(Action.NONE, state.start(true)) }
        assertEquals(Action.ACCEPTED, state.confirmationResult(true))
    }

    @Test fun `denied permission finishes without showing confirmation`() {
        val state = UserActionState(requiresPermission = true)
        state.start(false)
        assertEquals(Action.CANCELLED, state.permissionResult(false))
        assertEquals(Action.NONE, state.start(true))
        assertEquals(Action.NONE, state.permissionResult(true))
        assertEquals(Action.NONE, state.confirmationResult(true))
        assertFalse(state.cancel())
    }

    @Test fun `an accepted prompt is not converted to cancellation on activity destruction`() {
        val state = UserActionState(requiresPermission = false)
        assertEquals(Action.CONFIRM, state.start(false))
        assertEquals(Action.ACCEPTED, state.confirmationResult(true))
        assertFalse(state.cancel())
        assertEquals(Action.NONE, state.confirmationResult(false))
        assertTrue(state.accepted)
    }

    @Test fun `legacy cancelled activity result waits for the platform status without cancelling`() {
        val state = UserActionState(requiresPermission = false)
        state.start(true)
        assertEquals(Action.RETURNED, state.confirmationDismissed())
        assertFalse(state.accepted)
        assertTrue(state.awaitingPlatformResult)
        assertFalse(state.cancel())
        assertEquals(Action.NONE, state.confirmationDismissed())
        assertEquals(Action.NONE, state.start(true))
    }

    @Test fun `system cancellation and deadline closure prevent any relaunch`() {
        for (cancelAtPrompt in listOf(false, true)) {
            val state = UserActionState(requiresPermission = false)
            state.start(true)
            if (cancelAtPrompt) assertEquals(Action.CANCELLED, state.confirmationResult(false)) else state.close()
            assertEquals(Action.NONE, state.start(true))
            assertEquals(Action.NONE, state.confirmationResult(true))
            assertFalse(state.cancel())
        }
    }

    @Test fun `already granted permission and privileged prompts skip settings`() {
        assertEquals(Action.CONFIRM, UserActionState(true).start(true))
        assertEquals(Action.CONFIRM, UserActionState(false).start(false))
    }

    @Test fun `cancellation before attachment cannot later open a child activity`() {
        val state = UserActionState(true)
        assertTrue(state.cancel())
        assertFalse(state.cancel())
        assertEquals(Action.NONE, state.start(false))
    }
}
