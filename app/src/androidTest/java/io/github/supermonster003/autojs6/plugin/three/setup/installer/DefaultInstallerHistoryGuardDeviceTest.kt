package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.PackageManagerHidden
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises preservation proof rejection without writing a system preference or starting a service. */
@RunWith(AndroidJUnit4::class)
class DefaultInstallerHistoryGuardDeviceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val thirdParty = ComponentName("test.installer.history", "test.installer.history.Entry")
    private val resolver = ComponentName("android", "com.android.internal.app.ResolverActivity").flattenToString()

    @Test fun ordinaryAuditStillProtectsHistoryAndExplicitProofPreservesOnlyTheExactBaseline() {
        val baseline = snapshot()
        assertNotNull(DefaultInstallerUiRecovery.unsafeReason(context, baseline))
        val proof = DefaultInstallerUiRecovery.preserveUnrelatedLastChosen(context, baseline)
        assertNull(DefaultInstallerUiRecovery.unsafeReason(context, baseline, proof))
        val changed = baseline.copy(shellEntries = baseline.shellEntries + entry(canonical = "new-shell"))
        assertNotNull(DefaultInstallerUiRecovery.unsafeReason(context, changed, proof))
    }

    @Test fun realDefaultsUnknownFlagsAndFiltersThatCouldBeReplacedAreRejected() {
        for (flag in listOf(true, null)) reject(snapshot().copy(shellEntries = listOf(entry(always = flag))))
        reject(snapshot().copy(resolved = List(4) { thirdParty.flattenToString() }))
        reject(snapshot(filter = filter().apply { addDataScheme("content") }))
        reject(snapshot(filter = filter().apply { addAction(Intent.ACTION_SEND) }))
    }

    @Test fun publicRecordsNeedOneExactShellProofAndPluginHistoryRemainsProtected() {
        val baseline = snapshot()
        reject(baseline.copy(shellEntries = emptyList()))
        reject(baseline.copy(shellEntries = baseline.shellEntries + entry(canonical = "duplicate-shell")))
        reject(baseline.copy(publicEntries = listOf(entry(component = ComponentName("other.installer", "other.installer.Entry")))))
        reject(snapshot(component = ComponentName(context.packageName, "${context.packageName}.ui.ExternalInstallActivity")))
    }

    @Test fun wildcardHistoryRequiresTheSameAlwaysFalseProof() {
        val baseline = snapshot(filter = filter("*/*"))
        val proof = DefaultInstallerUiRecovery.preserveUnrelatedLastChosen(context, baseline)
        assertNull(DefaultInstallerUiRecovery.unsafeReason(context, baseline, proof))
        reject(baseline.copy(shellEntries = listOf(entry(filter = filter("*/*"), always = true))))
        reject(baseline.copy(shellEntries = listOf(entry())))
    }

    @Test fun approvedPluginModeCannotAcceptAnUnrelatedSyntheticBaseline() {
        assertThrows(IllegalStateException::class.java) {
            DefaultInstallerUiRecovery.preserveApprovedPluginHistory(InstrumentationRegistry.getInstrumentation(), snapshot())
        }
    }

    @Test fun recoveryRejectsWildcardMimeAndDuplicateNewFilters() {
        val component = ComponentName(context.packageName, "${context.packageName}.ui.ExternalInstallActivity")
        fun candidate(type: String) = snapshot(component, filter(type).apply { addDataScheme("content") }).let {
            it.copy(shellEntries = it.shellEntries.map { entry -> entry.copy(always = true) })
        }
        val valid = candidate(PackageManagerHidden.APK_MIME)
        DefaultInstallerUiRecovery.assertOwnedEntries(context, valid)
        for (type in listOf("application/*", "*/*")) assertThrows(IllegalStateException::class.java) {
            DefaultInstallerUiRecovery.assertOwnedEntries(context, candidate(type))
        }
        assertThrows(IllegalStateException::class.java) {
            DefaultInstallerUiRecovery.assertOwnedEntries(context, valid.copy(publicEntries = valid.publicEntries + valid.publicEntries))
        }
        assertThrows(IllegalStateException::class.java) {
            DefaultInstallerUiRecovery.assertOwnedEntries(context, valid.copy(shellEntries = valid.shellEntries + valid.shellEntries))
        }
    }

    private fun reject(snapshot: DefaultInstallerUiRecovery.Snapshot) {
        assertThrows(IllegalStateException::class.java) {
            DefaultInstallerUiRecovery.preserveUnrelatedLastChosen(context, snapshot)
        }
    }

    private fun filter(type: String = PackageManagerHidden.APK_MIME) = IntentFilter(Intent.ACTION_VIEW).apply {
        addCategory(Intent.CATEGORY_DEFAULT)
        addDataType(type)
    }

    private fun entry(
        component: ComponentName = thirdParty,
        filter: IntentFilter = filter(),
        canonical: String = "shell",
        always: Boolean? = false,
    ) = DefaultInstallerUiRecovery.Preferred(component, filter, canonical, always)

    private fun snapshot(component: ComponentName = thirdParty, filter: IntentFilter = filter()) =
        DefaultInstallerUiRecovery.Snapshot(
            publicEntries = listOf(entry(component, filter, "public", null)),
            shellEntries = listOf(entry(component, filter)),
            preferences = JsonObject(),
            resolved = List(4) { resolver },
        )
}
