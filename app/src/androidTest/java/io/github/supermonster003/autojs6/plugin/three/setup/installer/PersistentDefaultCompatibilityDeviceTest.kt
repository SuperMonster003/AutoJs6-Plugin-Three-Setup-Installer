package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.content.ComponentName
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AndroidDhizukuAccess
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.PersistentDefaultPolicy
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Read-only compatibility boundaries: never provision an owner or change a real policy. */
@RunWith(AndroidJUnit4::class)
class PersistentDefaultCompatibilityDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun androidSevenDoesNotInitializeTheAndroidEightDhizukuApi() {
        assumeTrue(Build.VERSION.SDK_INT < 26)
        val state = AndroidDhizukuAccess(context).state()
        assertFalse(state.available)
        assertFalse(state.running)
        assertFalse(state.granted)
    }

    @Test fun unobservableModernDhizukuPolicyIsRejectedBeforeAnyWrite() {
        assumeTrue(Build.VERSION.SDK_INT >= 34)
        val file = File(context.applicationInfo.dataDir, "shared_prefs/installer_persistent_default.xml")
        val existed = file.exists()
        val before = if (existed) file.readBytes() else null
        val policy = PersistentDefaultPolicy(context)
        val component = ComponentName(context.packageName, "${context.packageName}.ui.ExternalInstallActivity")
        for (enable in listOf(true, false)) {
            assertEquals("INVALID_ARGUMENT", assertThrows(InstallFailure::class.java) {
                policy.set(enable, component) { error("Unsupported policy must fail before the operation starts") }
            }.code)
        }
        assertEquals(existed, file.exists())
        if (existed) assertArrayEquals(before, file.readBytes())
    }
}
