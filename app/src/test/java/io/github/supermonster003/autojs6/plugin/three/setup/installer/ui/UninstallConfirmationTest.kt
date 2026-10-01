package io.github.supermonster003.autojs6.plugin.three.setup.installer.ui

import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.UninstallRequest
import org.junit.Assert.*
import org.junit.Test

class UninstallConfirmationTest {
    private val request = UninstallRequest("example.package", false, "10", "root", "dialog", 60_000)

    @Test fun `acceptance returns the selected keep data option without changing the caller request`() {
        val ticket = PluginConfirmation.Ticket(request, Authorizer.ROOT, 10)
        ticket.setKeepData(true)
        ticket.answer(true)
        assertEquals(request.copy(keepData = true), ticket.confirmedRequest())
        assertFalse(request.keepData)
    }

    @Test fun `an accepted request is frozen before the confirmation activity is destroyed`() {
        val ticket = PluginConfirmation.Ticket(request.copy(keepData = true), Authorizer.ROOT, 10)
        ticket.setKeepData(false)
        ticket.answer(true)
        ticket.setKeepData(true)
        ticket.answer(false)
        assertEquals(request, ticket.confirmedRequest())
    }

    @Test fun `declining never exposes an executable request even after later edits or approval`() {
        val ticket = PluginConfirmation.Ticket(request, Authorizer.ROOT, 10)
        ticket.setKeepData(true)
        ticket.answer(false)
        ticket.setKeepData(false)
        ticket.answer(true)
        assertEquals("USER_CANCELLED", assertThrows(InstallFailure::class.java) { ticket.confirmedRequest() }.code)
    }

    @Test fun `the unprivileged confirmation cannot enable keep data`() {
        val ordinary = request.copy(user = "current", authorizer = "none")
        val ticket = PluginConfirmation.Ticket(ordinary, Authorizer.NONE, 0)
        ticket.setKeepData(true)
        ticket.answer(true)
        assertEquals(ordinary, ticket.confirmedRequest())
    }
}
