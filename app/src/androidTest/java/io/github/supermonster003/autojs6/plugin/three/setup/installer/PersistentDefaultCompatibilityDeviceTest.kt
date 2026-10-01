package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.content.ComponentName
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AndroidDhizukuAccess
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
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


}
