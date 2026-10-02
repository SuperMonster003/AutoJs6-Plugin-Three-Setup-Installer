package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.os.Build
import android.os.Process
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.InstalledSigningInfo
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.PackageManagerHidden
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Read-only queries of this test's own target; no authorization requests or package mutations. */
@RunWith(AndroidJUnit4::class)
class PrivilegedMetadataDeviceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Suppress("DEPRECATION")
    @Test fun actualFrameworkCertificatesAndVersionRemainBoundToTheQueriedTarget() {
        val user = Process.myUid() / 100_000
        val info = PackageManagerHidden().packageInfo(context.packageName, user, InstalledSigningInfo.flags())
        assertNotNull(info)
        val encoded = InstalledSigningInfo.encode(context.packageName, user, info)
        val public = context.packageManager.getPackageInfo(context.packageName, InstalledSigningInfo.flags())
        assertTrue(encoded.getBoolean("found")); assertTrue(encoded.getBoolean("installedForUser"))
        assertEquals(context.packageName, encoded.getString("packageName")); assertEquals(user, encoded.getInt("userId"))
        assertEquals(public.lastUpdateTime, encoded.getLong("lastUpdateTime"))
        assertEquals(if (Build.VERSION.SDK_INT >= 28) public.longVersionCode else public.versionCode.toLong(), encoded.getLong("versionCode"))
        val expected = if (Build.VERSION.SDK_INT >= 28) public.signingInfo!!.apkContentsSigners else public.signatures
        val actual = encoded.getParcelableArray("currentSigners")!!.map { (it as Signature).toByteArray().toList() }.toSet()
        assertTrue(actual.isNotEmpty())
        assertEquals(requireNotNull(expected).map { it.toByteArray().toList() }.toSet(), actual)
    }

    @Test fun unknownSignerFactsAreNotMisrepresentedAsAnAbsentPackage() {
        val name = "org.autojs.fixture.metadata"
        val retained = PackageInfo().apply {
            packageName = name
            applicationInfo = ApplicationInfo().apply { flags = 0 }
            lastUpdateTime = 123
        }
        val knownPackage = InstalledSigningInfo.encode(name, 0, retained)
        assertTrue(knownPackage.getBoolean("found"))
        assertFalse(knownPackage.getBoolean("installedForUser"))
        @Suppress("DEPRECATION")
        assertEquals(0, knownPackage.getParcelableArray("currentSigners")!!.size)
        assertEquals(123L, knownPackage.getLong("lastUpdateTime"))
        assertFalse(InstalledSigningInfo.encode(name, 0, null).getBoolean("found"))
        assertThrows(IllegalStateException::class.java) { InstalledSigningInfo.encode("org.autojs.fixture.other", 0, retained) }
    }
}
