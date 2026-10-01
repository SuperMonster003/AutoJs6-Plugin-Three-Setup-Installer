package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import android.content.Intent
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import org.junit.Assert.*
import org.junit.Test

class UninstallEngineTest {
    private val listener = object : InstallEngine.Listener { override fun onUserAction(intent: Intent) = Unit }
    private val request = UninstallRequest("example.package", false, "current", "auto", "auto", 100)

    @Test fun `keepData and all users flags are combined only for privileged callers`() {
        val options = request.copy(keepData = true, user = "all")
        assertEquals(3, UninstallEngine.flags(options, Authorizer.ROOT, 10, 10))
        assertEquals(3, UninstallEngine.flags(options, Authorizer.SHIZUKU, 10, 10))
        for (option in listOf(options, request.copy(interaction = "silent"), request.copy(user = "0"))) {
            assertEquals("AUTHORIZER_REQUIRED", assertThrows(InstallFailure::class.java) { UninstallEngine.flags(option, Authorizer.NONE, 0, 0) }.code)
        }
    }

    @Test fun `user and explicit authorizer mismatch cannot reach platform`() {
        val engine = Fake()
        assertEquals("INVALID_ARGUMENT", assertThrows(InstallFailure::class.java) { engine.uninstall(request.copy(user = "5"), 0, listener) }.code)
        assertEquals("INVALID_ARGUMENT", assertThrows(InstallFailure::class.java) { engine.uninstall(request.copy(authorizer = "shizuku"), 0, listener) }.code)
        assertEquals(0, engine.calls)
    }

    @Test fun `uninstall failures retain platform policy and system message`() {
        for ((status, code) in listOf(1 to "UNINSTALL_FAILED", 2 to "BLOCKED_BY_POLICY", 3 to "USER_CANCELLED", 4 to "UNINSTALL_FAILED")) {
            val engine = Fake().apply { answer = InstallStatusBridge.Status(status, "original $status", "example.package", null) }
            val failure = assertThrows(InstallFailure::class.java) { engine.uninstall(request, 0, listener) }
            assertEquals(code, failure.code)
            assertEquals(status, failure.status)
            assertEquals("original $status", failure.systemMessage)
        }
    }

    @Test fun `cancelled or expired operations are never submitted`() {
        val engine = Fake()
        assertEquals("CANCELLED", assertThrows(InstallFailure::class.java) {
            engine.uninstall(request, 0, listener, { throw InstallFailure("CANCELLED", "stopped") })
        }.code)
        assertEquals("TIMEOUT", assertThrows(InstallFailure::class.java) {
            engine.uninstall(request, 0, listener, deadlineMillis = 1)
        }.code)
        assertEquals(0, engine.calls)
    }

    @Test fun `late cancellation cannot overwrite confirmed uninstall success`() {
        val engine = Fake()
        var cancelled = false
        engine.onPerform = { cancelled = true }
        val result = engine.uninstall(request, 0, listener, { if (cancelled) throw InstallFailure("CANCELLED", "late") })
        assertEquals("example.package", result.packageName)
        assertEquals("root", result.authorizer)
    }

    @Test fun `shared confirmation launcher failure retains the uninstall error code`() {
        val engine = Fake().apply { onPerform = { throw InstallFailure("INSTALL_FAILED", "Cannot show confirmation") } }
        val error = assertThrows(InstallFailure::class.java) { engine.uninstall(request, 0, listener) }
        assertEquals("UNINSTALL_FAILED", error.code)
        assertEquals("example.package", error.packageName)
    }

    private class Fake : UninstallEngine(Authorizer.ROOT, 0, { 10 }) {
        var calls = 0
        var answer = InstallStatusBridge.Status(0, null, "example.package", null)
        var onPerform: () -> Unit = {}
        override fun perform(request: UninstallRequest, flags: Int, userId: Int, deadline: Long, checkActive: () -> Unit,
            onUserAction: (Intent) -> Unit): InstallStatusBridge.Status { calls++; onPerform(); return answer }
    }
}
