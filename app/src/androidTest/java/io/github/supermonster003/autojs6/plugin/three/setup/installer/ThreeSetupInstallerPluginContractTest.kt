package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.autojs.plugin.common.api.IPluginInfoProvider
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.installer.api.InstallerCapabilityKeys
import org.autojs.plugin.installer.api.InstallerContract
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Verifies the host-facing activation and discovery contract against the installed APK: the Wake
 * Activity, the INFO service (with a real `getInfo()` round trip), the `org.autojs.plugin.INSTALLER`
 * service with the placeholder Binder of roadmap P0, and the Shizuku provider registration.
 */
@RunWith(AndroidJUnit4::class)
class ThreeSetupInstallerPluginContractTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val packageName: String
        get() = context.packageName

    @Test
    fun wakeActivityFollowsTheHostActivationContract() {
        val applicationInfo = context.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val wakeActivity = applicationInfo.metaData?.getString(WAKE_ACTIVITY_META_DATA)
        assertEquals(".WakeActivity", wakeActivity)
        assertEquals(context.getString(R.string.plugin_author), applicationInfo.metaData?.getString(AUTHOR_META_DATA))
        assertEquals(0, applicationInfo.metaData?.getInt(NATIVE_PAGE_ALIGNMENT_META_DATA, -1))

        val component = ComponentName(packageName, packageName + wakeActivity)
        val activityInfo = context.packageManager.getActivityInfo(component, 0)
        assertTrue("Wake Activity must be exported", activityInfo.exported)
        assertTrue("Wake Activity must be enabled", activityInfo.enabled)
        assertEquals(PLUGIN_PERMISSION, activityInfo.permission)
        assertEquals(android.R.style.Theme_NoDisplay, activityInfo.theme)
        assertTrue(activityInfo.flags and ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS != 0)
        assertTrue(activityInfo.flags and ActivityInfo.FLAG_FINISH_ON_TASK_LAUNCH != 0)

        val wakeIntent = Intent(WAKE_ACTION).addCategory(Intent.CATEGORY_DEFAULT).setPackage(packageName)
        @Suppress("DEPRECATION")
        val matches = context.packageManager.queryIntentActivities(wakeIntent, 0)
        assertEquals("The WAKE action must resolve to exactly one activity", 1, matches.size)
        assertEquals(component.className, matches.single().activityInfo.name)
    }

    @Test
    fun exactlyOneLauncherAliasOpensTheStandaloneHome() {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(packageName)
        @Suppress("DEPRECATION")
        val matches = context.packageManager.queryIntentActivities(launcherIntent, 0)
        assertEquals("Exactly one icon choice must be enabled", 1, matches.size)
        assertTrue(matches.single().activityInfo.name.startsWith("$packageName.launcher."))
        assertEquals("$packageName.ui.HomeActivity", matches.single().activityInfo.targetActivity)
    }

    @Test
    fun infoServiceIsDiscoverableAndReportsPluginInfo() {
        val serviceInfo = discoverSingleService(
            ThreeSetupInstallerPlugin.INFO_ACTION,
            ThreeSetupInstallerPluginInfoService::class.java.name,
        )
        assertEquals(packageName, serviceInfo.processName)

        withBoundService(serviceInfo) { binder ->
            assertEquals(IPluginInfoProvider.DESCRIPTOR, binder.interfaceDescriptor)
            val info = IPluginInfoProvider.Stub.asInterface(binder).info
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            val expectedVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            assertEquals("3-Setup Installer", info.name)
            assertEquals(context.getString(R.string.app_name), info.name)
            assertEquals(context.getString(R.string.plugin_description), info.description)
            assertTrue("instruction must be read from the raw resource", info.instruction?.isNotBlank() == true)
            assertEquals(ThreeSetupInstallerPlugin.AUTHOR, info.author)
            assertEquals(context.getString(R.string.plugin_author), info.author)
            assertEquals(ThreeSetupInstallerPlugin.ID, info.id)
            assertEquals(context.getString(R.string.plugin_id), info.id)
            assertEquals(ThreeSetupInstallerPlugin.ENGINE, info.engine)
            assertEquals(context.getString(R.string.plugin_engine), info.engine)
            assertEquals(ThreeSetupInstallerPlugin.VARIANT, info.variant)
            assertEquals(context.getString(R.string.plugin_variant), info.variant)
            assertEquals(packageInfo.versionName, info.versionName)
            assertEquals(expectedVersionCode, info.versionCode)
            assertEquals(context.getString(R.string.plugin_version_date), info.versionDate)
            assertTrue(info.versionDate?.isNotBlank() == true)
            // Explicit empty array: no ABI restriction, as opposed to a null (unspecified) value.
            assertArrayEquals(emptyArray<String>(), info.supportedAbis)
            assertCapabilities(requireNotNull(info.capabilities))
        }
    }

    @Test
    fun installerServiceAnswersTheInstallerContractBinder() {
        val serviceInfo = discoverSingleService(
            ThreeSetupInstallerPlugin.SERVICE_ACTION,
            ThreeSetupInstallerPluginService::class.java.name,
        )
        assertEquals(packageName, serviceInfo.processName)

        withBoundService(serviceInfo) { binder ->
            assertEquals(ThreeSetupInstallerPlugin.SERVICE_DESCRIPTOR, binder.interfaceDescriptor)
            assertTrue(binder.isBinderAlive)
            assertTrue(binder.pingBinder())
            val installer = org.autojs.plugin.installer.api.IInstallerPlugin.Stub.asInterface(binder)
            assertEquals(ThreeSetupInstallerPlugin.ID, installer.info.id)
            assertCapabilities(installer.capabilities)
        }
    }

    @Test
    fun shizukuProviderIsRegisteredUnderTheApplicationAuthority() {
        val providerInfo = context.packageManager.resolveContentProvider("$packageName.shizuku", 0)
        assertNotNull("The Shizuku provider must be registered", providerInfo)
        assertEquals("rikka.shizuku.ShizukuProvider", providerInfo?.name)
        assertEquals("android.permission.INTERACT_ACROSS_USERS_FULL", providerInfo?.readPermission)
        assertTrue(providerInfo?.exported == true)
    }

    private fun assertCapabilities(capabilities: Bundle) {
        assertEquals(ThreeSetupInstallerPlugin.REQUIRED_HOST_VERSION, capabilities.getLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION))
        // The old key remains 1 so installed V1 hosts can still discover the plugin. New hosts
        // negotiate the separate maximum instead of unconditionally sending the newest version.
        assertEquals(1, capabilities.getInt(InstallerCapabilityKeys.CONTRACT_VERSION))
        assertEquals(3, capabilities.getInt(InstallerCapabilityKeys.MAX_CONTRACT_VERSION))
        assertEquals(InstallerContract.AUTHORIZERS, capabilities.getStringArray(InstallerCapabilityKeys.AUTHORIZERS)?.toList())
        assertEquals(InstallerContract.MAX_BATCH_SOURCES, capabilities.getInt(InstallerCapabilityKeys.MAX_BATCH))
        assertEquals(InstallerContract.MAX_SPLITS_PER_PACKAGE, capabilities.getInt(InstallerCapabilityKeys.MAX_SPLITS))
        assertEquals(setOf(InstallerCapabilityKeys.FEATURE_BATCH, InstallerCapabilityKeys.FEATURE_SPLITS,
            InstallerCapabilityKeys.FEATURE_INSPECT, InstallerCapabilityKeys.FEATURE_USERS, InstallerCapabilityKeys.FEATURE_SILENT_UNINSTALL,
            InstallerCapabilityKeys.FEATURE_DELETE_SOURCE, InstallerCapabilityKeys.FEATURE_DEFAULT_INSTALLER,
            InstallerCapabilityKeys.FEATURE_NOTIFICATION_INSTALL, InstallerCapabilityKeys.FEATURE_PERSISTENT_DEFAULT_INSTALLER,
            InstallerCapabilityKeys.FEATURE_ADVANCED_INSTALL_OPTIONS),
            capabilities.getStringArray(InstallerCapabilityKeys.FEATURES_KEY)?.toSet())
    }

    private fun discoverSingleService(action: String, expectedClassName: String): ServiceInfo {
        val discoveryIntent = Intent(action)
            .addCategory(ThreeSetupInstallerPlugin.SERVICE_CATEGORY)
            .setPackage(packageName)
        @Suppress("DEPRECATION")
        val matches = context.packageManager.queryIntentServices(discoveryIntent, PackageManager.GET_META_DATA)
        assertEquals("The discovery contract for $action must resolve exactly one service", 1, matches.size)

        val serviceInfo = matches.single().serviceInfo
        assertEquals(packageName, serviceInfo.packageName)
        assertEquals(expectedClassName, serviceInfo.name)
        assertTrue("$expectedClassName must be exported", serviceInfo.exported)
        assertTrue("$expectedClassName must be enabled", serviceInfo.enabled)
        assertEquals(PLUGIN_PERMISSION, serviceInfo.permission)
        val requiresHostVersion = requireNotNull(serviceInfo.metaData) { "requiresHostVersion meta-data is missing" }
            .getInt(REQUIRES_HOST_VERSION_META_DATA)
        assertEquals(ThreeSetupInstallerPlugin.REQUIRED_HOST_VERSION, requiresHostVersion.toLong())
        return serviceInfo
    }

    private fun withBoundService(serviceInfo: ServiceInfo, block: (IBinder) -> Unit) {
        val binderReference = AtomicReference<IBinder>()
        val connected = CountDownLatch(1)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                binderReference.set(service)
                connected.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName) = Unit

            override fun onNullBinding(name: ComponentName) {
                connected.countDown()
            }
        }

        val explicitIntent = Intent().setComponent(ComponentName(serviceInfo.packageName, serviceInfo.name))
        assertTrue("bindService returned false", context.bindService(explicitIntent, connection, Context.BIND_AUTO_CREATE))
        try {
            assertTrue("Timed out waiting for the Binder service", connected.await(10, TimeUnit.SECONDS))
            val binder = binderReference.get()
            assertNotNull("The service returned a null Binder", binder)
            block(binder)
        } finally {
            context.unbindService(connection)
        }
    }

    private companion object {
        const val PLUGIN_PERMISSION = "org.autojs.permission.PLUGIN"
        const val WAKE_ACTION = "org.autojs.plugin.action.WAKE"
        const val WAKE_ACTIVITY_META_DATA = "org.autojs.plugin.WAKE_ACTIVITY"
        const val AUTHOR_META_DATA = "org.autojs.plugin.info.AUTHOR"
        const val NATIVE_PAGE_ALIGNMENT_META_DATA = "org.autojs.plugin.contract.NATIVE_PAGE_ALIGNMENT"
        const val REQUIRES_HOST_VERSION_META_DATA = "requiresHostVersion"
    }
}
