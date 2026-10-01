package io.github.supermonster003.autojs6.plugin.three.setup.installer.binder

import io.github.supermonster003.autojs6.plugin.three.setup.installer.ThreeSetupInstallerPlugin
import org.junit.Assert.*
import org.junit.Test

class CallerPolicyTest {
    private val host = ThreeSetupInstallerPlugin.HOST_PACKAGE_NAME
    private val version = ThreeSetupInstallerPlugin.REQUIRED_HOST_VERSION
    @Test fun `only official uid with current version and identical nonempty signers passes`() {
        assertTrue(CallerPolicy.allowed(10001, setOf(host), 10001, version, setOf("a", "b"), setOf("b", "a")))
        assertFalse(CallerPolicy.allowed(10002, setOf(host), 10001, version, setOf("a"), setOf("a")))
        assertFalse(CallerPolicy.allowed(10001, setOf("impostor"), 10001, version, setOf("a"), setOf("a")))
        assertFalse(CallerPolicy.allowed(10001, setOf(host), null, version, setOf("a"), setOf("a")))
        assertFalse(CallerPolicy.allowed(10001, setOf(host), 10001, version - 1, setOf("a"), setOf("a")))
        assertFalse(CallerPolicy.allowed(10001, setOf(host), 10001, version, setOf("a"), setOf("b")))
        assertFalse(CallerPolicy.allowed(10001, setOf(host), 10001, version, emptySet(), emptySet()))
    }
    @Test fun `session methods reject a different otherwise accepted host uid`() {
        var caller = 1
        val guard = object : CallerGuard { override fun enforceHost() = caller }
        guard.enforceOwner(1)
        caller = 2
        assertThrows(SecurityException::class.java) { guard.enforceOwner(1) }
    }
}
