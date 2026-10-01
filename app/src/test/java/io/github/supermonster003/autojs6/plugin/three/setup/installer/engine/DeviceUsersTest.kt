package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import org.junit.Assert.*
import org.junit.Test

class DeviceUsersTest {
    private val users = listOf(DeviceUser(0, "Owner", true, true), DeviceUser(10, "Work", false, false))

    @Test fun `current and all resolve to the calling app user while numeric ids must exist`() {
        for (authorizer in listOf(Authorizer.ROOT, Authorizer.SHIZUKU)) {
            assertEquals(10, DeviceUsers.resolve("current", authorizer, 10, users))
            assertEquals(10, DeviceUsers.resolve("all", authorizer, 10, users))
            assertEquals(0, DeviceUsers.resolve("0", authorizer, 10, users))
            assertEquals("INVALID_ARGUMENT", assertThrows(InstallFailure::class.java) { DeviceUsers.resolve("99", authorizer, 0, users) }.code)
        }
    }

    @Test fun `none cannot select any explicit user even its own`() {
        assertEquals(10, DeviceUsers.resolve("current", Authorizer.NONE, 10, users))
        for (user in listOf("0", "10", "all")) {
            assertEquals("AUTHORIZER_REQUIRED", assertThrows(InstallFailure::class.java) { DeviceUsers.resolve(user, Authorizer.NONE, 10, users) }.code)
        }
    }
}
