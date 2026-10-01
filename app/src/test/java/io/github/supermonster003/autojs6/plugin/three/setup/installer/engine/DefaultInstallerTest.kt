package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import org.junit.Assert.*
import org.junit.Test

class DefaultInstallerTest {
    @Test fun `ordinary state never starts a privileged operation or promises missing entry capability`() {
        val backend = Fake()
        val installer = DefaultInstaller(backend)
        assertFalse(installer.available)
        assertFalse(installer.state()["isSelf"].asBoolean)
        assertEquals(0, backend.writes)
        assertEquals("INVALID_ARGUMENT", assertThrows(InstallFailure::class.java) { installer.set(true, Authorizer.ROOT) }.code)
        assertEquals("AUTHORIZER_REQUIRED", assertThrows(InstallFailure::class.java) { installer.set(true, Authorizer.NONE) }.code)
        assertEquals(0, backend.writes)
    }
    @Test fun `legacy competitor returns requiresClear and is invalidated by a changed default`() {
        val backend = Fake().apply { state = state.copy(entryAvailable = true); result = -1 }
        val installer = DefaultInstaller(backend)
        assertTrue(installer.set(true, Authorizer.SHIZUKU)["requiresClear"].asBoolean)
        assertFalse(installer.state()["isSelf"].asBoolean)
        backend.state = backend.state.copy(fingerprint = "changed")
        assertFalse(installer.state()["requiresClear"].asBoolean)
        assertEquals(1, backend.writes)
    }
    @Test fun `partial preference write is not success and disabling works before an entry exists`() {
        val backend = Fake().apply { state = state.copy(entryAvailable = true); result = 3 }
        val installer = DefaultInstaller(backend)
        assertEquals("INTERNAL", assertThrows(InstallFailure::class.java) { installer.set(true, Authorizer.ROOT) }.code)
        backend.state = backend.state.copy(entryAvailable = false)
        backend.result = 0
        assertFalse(installer.set(false, Authorizer.ROOT)["requiresClear"].asBoolean)
        assertEquals(2, backend.writes)
    }
    @Test fun `a local persistent receipt does not turn an ordinary preference into live policy evidence`() {
        val backend = Fake().apply { state = state.copy(isSelf = true, entryAvailable = true, persistentConfigured = true) }
        val state = DefaultInstaller(backend).state()
        assertTrue(state["persistentConfigured"].asBoolean)
        assertEquals("preferred", state["method"].asString)
        assertEquals(0, backend.writes)
    }
    @Test fun `completed persistent write is acknowledged but a later read still uses observed state`() {
        val backend = Fake().apply {
            state = state.copy(isSelf = true, entryAvailable = true, persistentConfigured = true)
            result = 4
        }
        val installer = DefaultInstaller(backend)
        assertEquals("persistent", installer.setPersistent(true, Authorizer.DHIZUKU)["method"].asString)
        assertEquals("preferred", installer.state()["method"].asString)
        backend.state = backend.state.copy(persistentConfigured = false)
        backend.result = 0
        val cleared = installer.setPersistent(false, Authorizer.DHIZUKU)
        assertFalse(cleared["persistentConfigured"].asBoolean)
        assertTrue(cleared["isSelf"].asBoolean)
        assertEquals("preferred", cleared["method"].asString)
    }
    @Test fun `partial persistent update is not acknowledged as success`() {
        val backend = Fake().apply { state = state.copy(isSelf = true, entryAvailable = true); result = 3 }
        assertEquals("INTERNAL", assertThrows(InstallFailure::class.java) {
            DefaultInstaller(backend).setPersistent(true, Authorizer.DHIZUKU)
        }.code)
    }
    private class Fake : DefaultInstaller.Backend {
        var state = DefaultInstaller.State("other/Installer", false, "preferred", false, "other")
        var writes = 0
        var result = 0
        override fun read() = state
        override fun set(enable: Boolean, authorizer: Authorizer, checkActive: () -> Unit): Int { checkActive(); writes++; return result }
        override fun setPersistent(enable: Boolean, authorizer: Authorizer, checkActive: () -> Unit): Int { checkActive(); writes++; return result }
    }
}
