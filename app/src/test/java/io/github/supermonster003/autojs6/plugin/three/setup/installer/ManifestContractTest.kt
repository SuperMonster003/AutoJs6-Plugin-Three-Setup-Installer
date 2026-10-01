package io.github.supermonster003.autojs6.plugin.three.setup.installer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Keeps `AndroidManifest.xml` and [ThreeSetupInstallerPlugin] from drifting apart: the host discovers the
 * plugin through the manifest, while the services and tests use the Kotlin constants.
 */
class ManifestContractTest {

    private val manifest: Element by lazy {
        val path = findProjectRoot().resolve("app/src/main/AndroidManifest.xml")
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        factory.newDocumentBuilder().parse(path.toFile()).documentElement
    }

    @Test
    fun `manifest declares only required permissions and queries each authorizer and package handlers`() {
        val permissions = manifest.children("uses-permission").map { it.androidAttribute("name") }
        assertEquals(
            listOf(
                PLUGIN_PERMISSION,
                "android.permission.REQUEST_INSTALL_PACKAGES",
                "android.permission.REQUEST_DELETE_PACKAGES",
                "android.permission.QUERY_ALL_PACKAGES",
                "android.permission.FOREGROUND_SERVICE",
                "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
                "android.permission.POST_NOTIFICATIONS",
                "moe.shizuku.manager.permission.API_V23",
                "com.rosan.dhizuku.permission.API",
                "android.permission.INTERNET",
            ),
            permissions,
        )

        val queries = manifest.child("queries")
        assertEquals(
            listOf(ThreeSetupInstallerPlugin.HOST_PACKAGE_NAME, ThreeSetupInstallerPlugin.SHIZUKU_PACKAGE_NAME, "com.rosan.dhizuku"),
            queries.children("package").map { it.androidAttribute("name") },
        )
        val intent = queries.children("intent").single()
        assertEquals("android.intent.action.VIEW", intent.child("action").androidAttribute("name"))
        assertEquals("application/vnd.android.package-archive", intent.child("data").androidAttribute("mimeType"))
    }

    @Test
    fun `min SDK overrides remain limited to APIs explicitly gated by the plugin`() {
        val sdk = manifest.child("uses-sdk")
        assertEquals(setOf("org.lsposed.hiddenapibypass", "com.rosan.dhizuku.api"),
            sdk.getAttributeNS("http://schemas.android.com/tools", "overrideLibrary").split(',').toSet())
        assertNull(sdk.androidAttributeOrNull("minSdkVersion"))
    }

    @Test
    fun `application metadata points at the wake activity and the author string`() {
        val application = manifest.child("application")
        assertEquals("false", application.androidAttribute("allowBackup"))
        assertEquals("@string/app_name", application.androidAttribute("label"))
        assertEquals("@mipmap/ic_launcher_system", application.androidAttribute("icon"))
        assertEquals("@mipmap/ic_launcher_system", application.androidAttribute("roundIcon"))
        assertEquals("@style/Theme.ThreeSetupInstaller", application.androidAttribute("theme"))
        assertEquals("@xml/data_extraction_rules", application.androidAttribute("dataExtractionRules"))
        assertEquals("@xml/locales_config", application.androidAttribute("localeConfig"))

        val metaData = application.children("meta-data").associate { it.androidAttribute("name") to it.androidAttribute("value") }
        assertEquals(".WakeActivity", metaData["org.autojs.plugin.WAKE_ACTIVITY"])
        assertEquals("@string/plugin_author", metaData["org.autojs.plugin.info.AUTHOR"])
        assertEquals("0", metaData["org.autojs.plugin.contract.NATIVE_PAGE_ALIGNMENT"])
    }

    @Test
    fun `the wake activity follows the activation contract and uninstall is internal`() {
        val activities = manifest.child("application").children("activity").associateBy { it.androidAttribute("name") }
        assertEquals(setOf(".WakeActivity", ".ui.UninstallDialogActivity", ".ui.ConfirmationActivity",
            ".ui.InstallDialogActivity", ".ui.UserActionActivity", ".ui.ExternalInstallActivity", ".ui.HomeActivity",
            ".ui.InstalledAppsActivity", ".ui.SettingsActivity", ".ui.DefaultInstallerActivity", ".ui.AboutActivity",
            ".ui.ReleaseHistoryActivity", ".ui.InstallerSettingsActivity"), activities.keys)
        listOf(".ui.InstallDialogActivity", ".ui.UserActionActivity").forEach { name ->
            assertEquals("false", activities.getValue(name).androidAttribute("exported"))
            assertEquals("true", activities.getValue(name).androidAttribute("excludeFromRecents"))
        }
        val install = activities.getValue(".ui.InstallDialogActivity")
        assertEquals("standard", install.androidAttribute("launchMode"))
        assertEquals("intoExisting", install.androidAttribute("documentLaunchMode"))
        val external = activities.getValue(".ui.ExternalInstallActivity")
        assertNull(external.androidAttributeOrNull("permission"))
        val actions = external.children("intent-filter").flatMap { it.children("action") }.map { it.androidAttribute("name") }.toSet()
        assertEquals(setOf("android.intent.action.VIEW", "android.intent.action.INSTALL_PACKAGE", "android.intent.action.SEND", "android.intent.action.SEND_MULTIPLE"), actions)
        val confirmation = activities.getValue(".ui.ConfirmationActivity")
        assertEquals("false", confirmation.androidAttribute("exported"))
        assertEquals("true", confirmation.androidAttribute("excludeFromRecents"))
        assertTrue(confirmation.children("intent-filter").isEmpty())
        val uninstall = activities.getValue(".ui.UninstallDialogActivity")
        assertEquals("false", uninstall.androidAttribute("exported"))
        assertEquals("true", uninstall.androidAttribute("excludeFromRecents"))
        assertTrue(uninstall.children("intent-filter").isEmpty())
        val wake = activities.getValue(".WakeActivity")
        assertEquals(".WakeActivity", wake.androidAttribute("name"))
        assertEquals("true", wake.androidAttribute("exported"))
        assertEquals("true", wake.androidAttribute("excludeFromRecents"))
        assertEquals("true", wake.androidAttribute("finishOnTaskLaunch"))
        assertEquals(PLUGIN_PERMISSION, wake.androidAttribute("permission"))
        assertEquals("@android:style/Theme.NoDisplay", wake.androidAttribute("theme"))
        val wakeFilter = wake.child("intent-filter")
        assertEquals(listOf("org.autojs.plugin.action.WAKE"), wakeFilter.children("action").map { it.androidAttribute("name") })
        assertEquals(listOf("android.intent.category.DEFAULT"), wakeFilter.children("category").map { it.androidAttribute("name") })
        val aliases = manifest.child("application").children("activity-alias")
        assertEquals(4, aliases.size)
        assertEquals(listOf(".launcher.AdaptiveAutoIconAlias"), aliases.filter { it.androidAttribute("enabled") == "true" }.map { it.androidAttribute("name") })
        aliases.forEach {
            assertEquals(".ui.HomeActivity", it.androidAttribute("targetActivity"))
            assertEquals("true", it.androidAttribute("exported"))
            assertEquals("android.intent.action.MAIN", it.child("intent-filter").child("action").androidAttribute("name"))
            assertEquals("android.intent.category.LAUNCHER", it.child("intent-filter").child("category").androidAttribute("name"))
        }
        listOf(".ui.HomeActivity", ".ui.SettingsActivity", ".ui.DefaultInstallerActivity", ".ui.AboutActivity", ".ui.ReleaseHistoryActivity", ".ui.InstalledAppsActivity").forEach {
            assertEquals("false", activities.getValue(it).androidAttribute("exported"))
        }
        assertEquals(PLUGIN_PERMISSION, activities.getValue(".ui.InstallerSettingsActivity").androidAttribute("permission"))
        val receivers = manifest.child("application").children("receiver")
        assertEquals(setOf(".engine.InstallStatusReceiver", ".ui.InstallNotifications\$CancelReceiver", ".ui.InstallNotificationActionReceiver", ".ui.LauncherIconUpdateReceiver"), receivers.map { it.androidAttribute("name") }.toSet())
        receivers.forEach {
            assertEquals("false", it.androidAttribute("exported"))
            if (it.androidAttribute("name") == ".ui.LauncherIconUpdateReceiver") {
                assertEquals("android.intent.action.MY_PACKAGE_REPLACED", it.child("intent-filter").child("action").androidAttribute("name"))
            } else assertTrue(it.children("intent-filter").isEmpty())
        }
    }

    @Test
    fun `info service and installer service match the identity constants`() {
        val services = manifest.child("application").children("service").associateBy { it.androidAttribute("name") }
        assertEquals(setOf(".ThreeSetupInstallerPluginInfoService", ".ThreeSetupInstallerPluginService", ".priv.RootInstallerService", ".ui.InstallForegroundService"), services.keys)
        assertEquals("false", services.getValue(".ui.InstallForegroundService").androidAttribute("exported"))
        assertEquals("dataSync", services.getValue(".ui.InstallForegroundService").androidAttribute("foregroundServiceType"))
        val root = services.getValue(".priv.RootInstallerService")
        assertEquals("false", root.androidAttribute("exported"))
        assertTrue(root.children("intent-filter").isEmpty())

        val info = services.getValue(".ThreeSetupInstallerPluginInfoService")
        assertDiscoveryContract(info, ThreeSetupInstallerPlugin.INFO_ACTION)
        assertNull(info.androidAttributeOrNull("process"))

        val installer = services.getValue(".ThreeSetupInstallerPluginService")
        assertDiscoveryContract(installer, ThreeSetupInstallerPlugin.SERVICE_ACTION)
        assertNull(installer.androidAttributeOrNull("process"))
    }

    @Test
    fun `the shizuku provider is the only provider and keeps its cross user guard`() {
        val provider = manifest.child("application").children("provider").single()
        assertEquals("rikka.shizuku.ShizukuProvider", provider.androidAttribute("name"))
        assertEquals("\${applicationId}.shizuku", provider.androidAttribute("authorities"))
        assertEquals("true", provider.androidAttribute("exported"))
        assertEquals("true", provider.androidAttribute("enabled"))
        assertEquals("false", provider.androidAttribute("multiprocess"))
        assertEquals("android.permission.INTERACT_ACROSS_USERS_FULL", provider.androidAttribute("permission"))
    }

    @Test
    fun `only discovery activation settings launcher and external package entry are exported`() {
        val expected = mapOf(
            ".WakeActivity" to PLUGIN_PERMISSION,
            ".ThreeSetupInstallerPluginInfoService" to PLUGIN_PERMISSION,
            ".ThreeSetupInstallerPluginService" to PLUGIN_PERMISSION,
            ".ui.ExternalInstallActivity" to null,
            ".ui.InstallerSettingsActivity" to PLUGIN_PERMISSION,
            ".launcher.AdaptiveLightIconAlias" to null,
            ".launcher.AdaptiveDarkIconAlias" to null,
            ".launcher.AdaptiveAutoIconAlias" to null,
            ".launcher.TransparentIconAlias" to null,
            "rikka.shizuku.ShizukuProvider" to "android.permission.INTERACT_ACROSS_USERS_FULL",
        )
        val components = listOf("activity", "activity-alias", "service", "receiver", "provider")
            .flatMap { manifest.child("application").children(it) }
        val exported = components.filter { it.androidAttribute("exported") == "true" }
        assertEquals(expected, exported.associate { it.androidAttribute("name") to it.androidAttributeOrNull("permission") })
        components.filterNot { it in exported }.forEach { assertEquals("false", it.androidAttribute("exported")) }
    }

    private fun assertDiscoveryContract(service: Element, action: String) {
        assertEquals("true", service.androidAttribute("exported"))
        assertEquals("true", service.androidAttribute("enabled"))
        assertEquals(PLUGIN_PERMISSION, service.androidAttribute("permission"))
        val filter = service.child("intent-filter")
        assertEquals(listOf(action), filter.children("action").map { it.androidAttribute("name") })
        assertEquals(listOf(ThreeSetupInstallerPlugin.SERVICE_CATEGORY), filter.children("category").map { it.androidAttribute("name") })
        val metaData = service.children("meta-data").associate { it.androidAttribute("name") to it.androidAttribute("value") }
        assertEquals(ThreeSetupInstallerPlugin.REQUIRED_HOST_VERSION.toString(), metaData["requiresHostVersion"])
    }

    private fun Element.children(tag: String): List<Element> {
        val nodes = childNodes
        return (0 until nodes.length)
            .map { nodes.item(it) }
            .filterIsInstance<Element>()
            .filter { it.tagName == tag }
    }

    private fun Element.child(tag: String): Element = children(tag).single()

    private fun Element.androidAttribute(name: String): String =
        androidAttributeOrNull(name) ?: error("Missing android:$name on <$tagName>")

    private fun Element.androidAttributeOrNull(name: String): String? =
        if (hasAttributeNS(ANDROID_NAMESPACE, name)) getAttributeNS(ANDROID_NAMESPACE, name) else null

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { path ->
        path.parent
    }.first { path -> Files.isDirectory(path.resolve("app/src/main")) }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val PLUGIN_PERMISSION = "org.autojs.permission.PLUGIN"
    }
}
