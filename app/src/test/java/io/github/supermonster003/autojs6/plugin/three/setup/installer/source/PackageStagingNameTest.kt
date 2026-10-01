package io.github.supermonster003.autojs6.plugin.three.setup.installer.source

import org.junit.Assert.assertEquals
import org.junit.Test

class PackageStagingNameTest {

    @Test
    fun `display names are reduced to a safe stem and a short lowercase extension`() {
        assertEquals("app.apk", PackageStaging.safeName("app.apk"))
        assertEquals("Game_1.2.xapk", PackageStaging.safeName("Game 1.2.XAPK"))
        assertEquals("evil.apk", PackageStaging.safeName("../../evil.apk"))
        assertEquals("package.bin", PackageStaging.safeName("...."))
        assertEquals("noext.bin", PackageStaging.safeName("noext"))
        assertEquals(48 + 4, PackageStaging.safeName("a".repeat(200) + ".apk").length)
        assertEquals("x.bin", PackageStaging.safeName("x.toolongextension"))
    }
}
