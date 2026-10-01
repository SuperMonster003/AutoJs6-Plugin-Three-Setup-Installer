package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.PrivilegedClient
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.AndroidDefaultInstaller
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.DefaultInstaller
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.PackageManagerHidden
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.ExternalInstallActivity
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExternalEntryContractDeviceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun formalEntryResolvesApkContainersAndSharingWithoutAHostInstallerEntry() {
        val packages = context.packageManager
        val expected = ExternalInstallActivity::class.java.name
        val types = listOf("application/vnd.android.package-archive", "application/x-apks", "application/vnd.apkm",
            "application/xapk-package-archive", "application/x-apkz", "application/x-aab", "application/vnd.android.aab")
        for (action in listOf(Intent.ACTION_VIEW, Intent.ACTION_INSTALL_PACKAGE)) {
            for (scheme in listOf("content", "file")) for (type in types) {
                val intent = Intent(action).setDataAndType(Uri.parse("$scheme://fixture/package.apk"), type)
                @Suppress("DEPRECATION") val activities = packages.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                assertTrue("Missing handler for $action/$scheme/$type", activities.any { it.activityInfo.packageName == context.packageName && it.activityInfo.name == expected })
                val host = runCatching { packages.getPackageInfo(ThreeSetupInstallerPlugin.HOST_PACKAGE_NAME, 0) }.getOrNull()
                @Suppress("DEPRECATION")
                if (host != null && host.versionCode >= ThreeSetupInstallerPlugin.REQUIRED_HOST_VERSION) {
                    assertFalse(activities.any { it.activityInfo.packageName == ThreeSetupInstallerPlugin.HOST_PACKAGE_NAME })
                }
            }
        }
        for (extension in listOf("apk", "apks", "xapk", "apkm", "apkz", "aab")) for (name in listOf(extension, extension.uppercase())) {
            val intent = Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("content://fixture/package.$name"), "application/octet-stream").setPackage(context.packageName)
            @Suppress("DEPRECATION")
            assertTrue(packages.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).any { it.activityInfo.name == expected })
        }
        for (action in listOf(Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE)) {
            @Suppress("DEPRECATION")
            assertTrue(packages.queryIntentActivities(Intent(action).setType("application/zip").setPackage(context.packageName), PackageManager.MATCH_DEFAULT_ONLY)
                .any { it.activityInfo.name == expected })
        }
    }

    @Test fun formalDefaultCanBeSetAndClearedOnlyWhenThereWasNoPreferredApkHandler() {
        val selected = Authorizer.fromId(InstrumentationRegistry.getArguments().getString("defaultEntryAuthorizer"))
        assumeTrue("Opt in only on a disposable device with no APK default", selected?.privileged == true)
        val authorizer = requireNotNull(selected)
        val filters = mutableListOf<IntentFilter>()
        val preferred = mutableListOf<ComponentName>()
        @Suppress("DEPRECATION") context.packageManager.getPreferredActivities(filters, preferred, null)
        assumeTrue("Preserve every existing APK preference", filters.none { filter ->
            filter.hasDataType("application/vnd.android.package-archive") && PackageManagerHidden.INSTALL_ACTIONS.any(filter::hasAction)
        })
        check(AuthorizerStates.request(context, authorizer, 30_000))
        val backend = AndroidDefaultInstaller(context)
        val installer = DefaultInstaller(backend)
        assertTrue(installer.available)
        try {
            assertTrue(installer.set(true, authorizer)["isSelf"].asBoolean)
            assertEquals(ComponentName(context, ExternalInstallActivity::class.java).flattenToString(), installer.state()["component"].asString)
        } finally {
            installer.set(false, authorizer)
            assertFalse(installer.state()["isSelf"].asBoolean)
            PrivilegedClient.get(context).releaseAll()
        }
    }
}
