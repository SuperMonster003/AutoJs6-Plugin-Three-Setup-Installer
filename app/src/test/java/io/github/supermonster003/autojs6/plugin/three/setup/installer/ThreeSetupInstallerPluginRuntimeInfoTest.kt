package io.github.supermonster003.autojs6.plugin.three.setup.installer

import org.autojs.plugin.common.api.PluginActions
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ThreeSetupInstallerPluginRuntimeInfoTest {

    @Test
    fun `runtime fields are assembled without losing the plugin identity`() {
        val info = ThreeSetupInstallerPluginRuntimeInfo(
            name = "3-Setup Installer",
            description = "Installs, updates and uninstalls Android apps, with silent installation through Shizuku or Root",
            instruction = "# 3-Setup Installer",
            versionName = "1.0.0",
            versionCode = 1L,
            versionDate = "Sep 30, 2026",
        )

        assertEquals("3-Setup Installer", info.name)
        assertEquals("Installs, updates and uninstalls Android apps, with silent installation through Shizuku or Root", info.description)
        assertEquals("# 3-Setup Installer", info.instruction)
        assertEquals("SuperMonster003", info.author)
        assertEquals("three-setup-installer", info.id)
        assertEquals("installer", info.engine)
        assertEquals("default", info.variant)
        assertEquals("1.0.0", info.versionName)
        assertEquals(1L, info.versionCode)
        assertEquals("Sep 30, 2026", info.versionDate)
        assertArrayEquals(emptyArray<String>(), info.supportedAbis)
        assertEquals(5299L, info.requiresHostVersion)
        assertEquals(ThreeSetupInstallerPlugin.REQUIRED_HOST_VERSION, info.requiresHostVersion)
    }

    @Test
    fun `identity constants follow the host discovery contract`() {
        assertEquals("io.github.supermonster003.autojs6.plugin.three.setup.installer", ThreeSetupInstallerPlugin.PACKAGE_NAME)
        assertEquals("org.autojs.autojs6", ThreeSetupInstallerPlugin.HOST_PACKAGE_NAME)
        assertEquals("moe.shizuku.privileged.api", ThreeSetupInstallerPlugin.SHIZUKU_PACKAGE_NAME)
        assertEquals("three-setup-installer", ThreeSetupInstallerPlugin.ID)
        assertEquals("installer", ThreeSetupInstallerPlugin.ENGINE)
        assertEquals("default", ThreeSetupInstallerPlugin.VARIANT)
        assertEquals("SuperMonster003", ThreeSetupInstallerPlugin.AUTHOR)
        assertEquals("org.autojs.plugin.INSTALLER", ThreeSetupInstallerPlugin.SERVICE_ACTION)
        assertEquals("installer", ThreeSetupInstallerPlugin.SERVICE_CATEGORY)
        assertEquals("org.autojs.plugin.INFO", ThreeSetupInstallerPlugin.INFO_ACTION)
        assertEquals(PluginActions.INFO, ThreeSetupInstallerPlugin.INFO_ACTION)
        assertEquals("org.autojs.plugin.installer.api.IInstallerPlugin", ThreeSetupInstallerPlugin.SERVICE_DESCRIPTOR)
    }

    @Test
    fun `identity constants match the values the documentation and build publish`() {
        val root = findProjectRoot()
        val common = Files.readString(root.resolve(".readme/common.json"))
        assertTrue(common.contains("\"plugin_application_id\": \"${ThreeSetupInstallerPlugin.PACKAGE_NAME}\""))
        assertTrue(common.contains("\"plugin_id\": \"${ThreeSetupInstallerPlugin.ID}\""))
        assertTrue(common.contains("\"plugin_engine\": \"${ThreeSetupInstallerPlugin.ENGINE}\""))
        assertTrue(common.contains("\"plugin_variant\": \"${ThreeSetupInstallerPlugin.VARIANT}\""))
        assertTrue(common.contains("\"plugin_service_action\": \"${ThreeSetupInstallerPlugin.SERVICE_ACTION}\""))
        assertTrue(common.contains("\"plugin_service_category\": \"${ThreeSetupInstallerPlugin.SERVICE_CATEGORY}\""))
        assertTrue(common.contains("\"plugin_aidl_interface\": \"${ThreeSetupInstallerPlugin.SERVICE_DESCRIPTOR}\""))
        assertTrue(common.contains("\"required_host_version_code\": \"${ThreeSetupInstallerPlugin.REQUIRED_HOST_VERSION}\""))

        val build = Files.readString(root.resolve("app/build.gradle.kts"))
        // app/build.gradle.kts binds the application id once and reuses it for namespace and applicationId.
        assertTrue(build.contains("val globalApplicationId = \"${ThreeSetupInstallerPlugin.PACKAGE_NAME}\""))
        assertTrue(build.contains("applicationId = globalApplicationId"))
        assertTrue(build.contains("\"plugin_id\", \"${ThreeSetupInstallerPlugin.ID}\""))
        assertTrue(build.contains("\"plugin_engine\", \"${ThreeSetupInstallerPlugin.ENGINE}\""))
        assertTrue(build.contains("\"plugin_variant\", \"${ThreeSetupInstallerPlugin.VARIANT}\""))
        assertTrue(build.contains("\"plugin_author\", \"${ThreeSetupInstallerPlugin.AUTHOR}\""))

        val settings = Files.readString(root.resolve("settings.gradle.kts"))
        assertTrue(settings.contains("rootProject.name = \"autojs6-plugin-three-setup-installer\""))
    }

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { path ->
        path.parent
    }.first { path -> Files.isDirectory(path.resolve("app/src/main")) }
}
