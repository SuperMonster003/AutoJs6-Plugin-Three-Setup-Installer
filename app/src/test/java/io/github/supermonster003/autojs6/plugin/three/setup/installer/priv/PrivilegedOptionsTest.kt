package io.github.supermonster003.autojs6.plugin.three.setup.installer.priv

import org.junit.Assert.assertThrows
import org.junit.Test

class PrivilegedOptionsTest {
    @Test fun `rejects path traversal and ambiguous split names`() {
        listOf("../base.apk", "/base.apk", "a/b.apk", "a\\b.apk", ".apk", "a.apk\u0000", "a.apk ").forEach { name ->
            assertThrows(name, IllegalArgumentException::class.java) { PrivilegedOptions.validateName(name) }
        }
        listOf("base.apk", "split_config.arm64_v8a.apk", "feature-1.apk").forEach(PrivilegedOptions::validateName)
    }

    @Test fun `only allows supported install flags and guards low target bypass by SDK`() {
        PrivilegedOptions.validateFlags(PrivilegedOptions.INSTALL_REPLACE_EXISTING or PrivilegedOptions.INSTALL_ALLOW_TEST, 24)
        PrivilegedOptions.validateFlags(PrivilegedOptions.INSTALL_REQUEST_DOWNGRADE or PrivilegedOptions.INSTALL_ALLOW_DOWNGRADE, 28)
        PrivilegedOptions.validateFlags(PrivilegedOptions.INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK, 34)
        PrivilegedOptions.validateFlags(PrivilegedOptions.INSTALL_GRANT_ALL_REQUESTED_PERMISSIONS, 24)
        PrivilegedOptions.validateFlags(PrivilegedOptions.INSTALL_GRANT_ALL_REQUESTED_PERMISSIONS, 35)
        assertThrows(IllegalArgumentException::class.java) { PrivilegedOptions.validateFlags(0x200, 35) }
        assertThrows(IllegalArgumentException::class.java) { PrivilegedOptions.validateFlags(PrivilegedOptions.INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK, 33) }
    }

    @Test fun `explicit metadata is rejected before an unsupported platform can ignore it`() {
        PrivilegedOptions.validateMetadata(24, false, null, null)
        PrivilegedOptions.validateMetadata(26, false, 0, null)
        PrivilegedOptions.validateMetadata(33, false, 4, 0)
        PrivilegedOptions.validateMetadata(34, true, 1, 4)
        assertThrows(IllegalArgumentException::class.java) { PrivilegedOptions.validateMetadata(25, false, 0, null) }
        assertThrows(IllegalArgumentException::class.java) { PrivilegedOptions.validateMetadata(32, false, null, 0) }
        assertThrows(IllegalArgumentException::class.java) { PrivilegedOptions.validateMetadata(33, true, null, null) }
        assertThrows(IllegalArgumentException::class.java) { PrivilegedOptions.validateMetadata(34, false, 5, null) }
        assertThrows(IllegalArgumentException::class.java) { PrivilegedOptions.validateMetadata(34, false, null, -1) }
    }

    @Test fun `bounds package names and rejects command fragments`() {
        PrivilegedOptions.validatePackage("com.android.shell")
        listOf("", "single", "a..b", "a.b;id", "a.b\n", "a." + "b".repeat(254)).forEach { name ->
            assertThrows(IllegalArgumentException::class.java) { PrivilegedOptions.validatePackage(name) }
        }
    }
}
