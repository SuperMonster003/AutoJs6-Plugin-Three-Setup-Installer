package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import com.google.gson.JsonParser
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import org.autojs.plugin.installer.api.InstallerContract as C
import org.junit.Assert.*
import org.junit.Test

class AdvancedInstallOptionsTest {
    @Test fun absentMetadataKeepsExistingDefaultsAndExplicitValuesAreStrict() {
        val defaults = InstallOptions.parse(JsonParser.parseString("{}").asJsonObject, "options")
        assertNull(defaults.installReason); assertNull(defaults.packageSource)
        assertFalse(defaults.grantAllRequestedPermissions); assertFalse(defaults.requestUpdateOwnership)
        assertEquals(C.DEXOPT_NONE, defaults.dexopt)
        val value = InstallOptions.parse(JsonParser.parseString("""{"grantAllRequestedPermissions":true,"requestUpdateOwnership":true,"dexopt":"speed-profile","installReason":"user","packageSource":"local-file"}""").asJsonObject, "options")
        assertTrue(value.grantAllRequestedPermissions && value.requestUpdateOwnership)
        assertEquals(C.DEXOPT_SPEED_PROFILE, value.dexopt)
        for (bad in listOf("""{"grantAllRequestedPermissions":"true"}""", """{"requestUpdateOwnership":1}""",
            """{"dexopt":"speed; reboot"}""", """{"installReason":"rollback"}""", """{"packageSource":3}""")) {
            assertEquals("INVALID_ARGUMENT", assertThrows(InstallFailure::class.java) {
                InstallOptions.parse(JsonParser.parseString(bad).asJsonObject, "options")
            }.code)
        }
    }

    @Test fun unsupportedPlatformsAreRefusedBeforeCreatingAnInstallationSession() {
        for ((sdk, options) in listOf(25 to InstallOptions(dexopt = C.DEXOPT_VERIFY),
            25 to InstallOptions(installReason = C.INSTALL_REASON_UNKNOWN),
            32 to InstallOptions(packageSource = C.PACKAGE_SOURCE_UNSPECIFIED),
            33 to InstallOptions(requestUpdateOwnership = true))) {
            assertEquals("INVALID_ARGUMENT", assertThrows(InstallFailure::class.java) {
                AdvancedInstallOptions.validate(options, Authorizer.ROOT, sdk)
            }.code)
        }
        AdvancedInstallOptions.validate(InstallOptions(), Authorizer.NONE, 24)
        AdvancedInstallOptions.validate(InstallOptions(requestUpdateOwnership = true,
            installReason = C.INSTALL_REASON_USER, packageSource = C.PACKAGE_SOURCE_LOCAL_FILE), Authorizer.NONE, 34)
    }

    @Test fun runtimeGrantAndCompilerRequireAnActualShellOrRootTransport() {
        for (authorizer in listOf(Authorizer.NONE, Authorizer.DHIZUKU)) {
            for (options in listOf(InstallOptions(grantAllRequestedPermissions = true), InstallOptions(dexopt = C.DEXOPT_SPEED))) {
                assertEquals("AUTHORIZER_REQUIRED", assertThrows(InstallFailure::class.java) {
                    AdvancedInstallOptions.validate(options, authorizer, 35)
                }.code)
            }
        }
        AdvancedInstallOptions.validate(InstallOptions(grantAllRequestedPermissions = true, dexopt = C.DEXOPT_SPEED), Authorizer.SHIZUKU, 24)
    }

    @Test fun publicNamesMapOnlyToTheirDocumentedFrameworkValues() {
        assertEquals(listOf(0, 2, 3, 4, 1), listOf("unknown", "device-restore", "device-setup", "user", "policy").map(AdvancedInstallOptions::reasonValue))
        assertEquals(listOf(0, 2, 3, 4, 1), listOf("unspecified", "store", "local-file", "downloaded-file", "other").map(AdvancedInstallOptions::sourceValue))
    }
}
