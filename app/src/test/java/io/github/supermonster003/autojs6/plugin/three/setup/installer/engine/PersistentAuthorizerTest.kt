package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerState
import org.junit.Assert.*
import org.junit.Test

class PersistentAuthorizerTest {
    private val enabled = Authorizer.entries.toSet()
    private fun states(vararg usable: Authorizer) = Authorizer.entries.associateWith {
        AuthorizerState(it, true, true, it in usable)
    }
    @Test fun autoSkipsShizukuAndRespectsOrderAndDisabledEntries() {
        val all = states(Authorizer.SHIZUKU, Authorizer.ROOT, Authorizer.DHIZUKU)
        assertEquals(Authorizer.ROOT, PersistentAuthorizer.resolve("auto", all, Authorizer.DEFAULT_ORDER, enabled, 31, 0))
        assertEquals(Authorizer.DHIZUKU, PersistentAuthorizer.resolve("auto", all, Authorizer.DEFAULT_ORDER, enabled - Authorizer.ROOT, 31, 0))
        assertThrows(InstallFailure::class.java) {
            PersistentAuthorizer.resolve("auto", states(Authorizer.SHIZUKU), Authorizer.DEFAULT_ORDER, enabled, 31, 0)
        }
    }
    @Test fun autoHonorsDeviceAndProfileLimitsWhileExplicitRequestsDoNotSwitchIdentities() {
        val all = states(Authorizer.SHIZUKU, Authorizer.ROOT, Authorizer.DHIZUKU)
        assertEquals(Authorizer.DHIZUKU, PersistentAuthorizer.resolve("auto", all, Authorizer.DEFAULT_ORDER, enabled, 31, 10))
        assertThrows(InstallFailure::class.java) { PersistentAuthorizer.resolve("auto", all, Authorizer.DEFAULT_ORDER, enabled, 34, 10) }
        assertThrows(InstallFailure::class.java) { PersistentAuthorizer.resolve("shizuku", all, Authorizer.DEFAULT_ORDER, enabled, 31, 0) }
        assertEquals(Authorizer.DHIZUKU, PersistentAuthorizer.resolve("dhizuku", all, Authorizer.DEFAULT_ORDER, enabled - Authorizer.DHIZUKU, 31, 0))
    }
}
