package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.KeyguardManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonParser
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallDialogActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** The host probe starts a real production host client. No plugin caller guard is injected here. */
@RunWith(AndroidJUnit4::class)
class HostInstallIntegrationDeviceTest {
    @Test fun visibleOfficialHostLaunchesThePluginAndRestoresItsEnablePreference() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("hostInstallerProbe") == "true" && Build.VERSION.SDK_INT >= 34)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assumeTrue("Unlock the device before verifying a visible host", !context.getSystemService(KeyguardManager::class.java).isKeyguardLocked)
        val grantDeadline = SystemClock.elapsedRealtime() + 10_000
        while (!AuthorizerStates.state(context, Authorizer.SHIZUKU).usable && SystemClock.elapsedRealtime() < grantDeadline) SystemClock.sleep(50)
        check(AuthorizerStates.state(context, Authorizer.SHIZUKU).usable) { "A previously granted Shizuku authorizer is required" }
        val host = "org.autojs.autojs6"
        val fixture = FixtureInstallUi.PACKAGE_NAME
        val caseId = "p3-installer-${UUID.randomUUID()}"
        val document = "three-setup-host-probe://$caseId"
        var record: InstallPresentation.Record? = null
        FixturePackageOwnership(setOf(fixture)).use { ownership ->
            try {
                ownership.installationStarted()
                val answer = shell("am start -W -f 0x10080000 -d $document -n $host/org.autojs.autojs.compat.ActivityLaunchCompatibilityProbeActivity --es case $caseId --es route installer-integration")
                assertFalse(answer, answer.contains("Error") || answer.contains("Exception"))
                val deadline = SystemClock.elapsedRealtime() + 120_000
                while (record == null && SystemClock.elapsedRealtime() < deadline) {
                    record = FixtureInstallUi.resumedRecord { candidate ->
                        candidate.request.sources.any { it.displayName == "p3-installer-foreground-fixture.apk" }
                    }
                    if (record == null) SystemClock.sleep(50)
                }
                val visible = requireNotNull(record) { "The official host never opened the plugin UI" }
                while (!visible.snapshot().terminal && SystemClock.elapsedRealtime() < deadline) {
                    if (visible.snapshot().prompt?.metadata?.packageName == fixture) {
                        FixtureInstallUi.clickInstall(InstallDialogActivity.TAG_CONFIRM, token = visible.token, packageName = fixture)
                    }
                    SystemClock.sleep(50)
                }
                assertTrue(visible.snapshot().terminal)
                assertTrue(visible.snapshot().items.single().result!!.get("ok").asBoolean)
                var evidence = JsonParser.parseString(shell("run-as $host cat files/sdk37-bal-$caseId.json")).asJsonObject
                val finishDeadline = SystemClock.elapsedRealtime() + 10_000
                while (evidence.get("completed")?.asBoolean != true && SystemClock.elapsedRealtime() < finishDeadline) {
                    SystemClock.sleep(100)
                    evidence = JsonParser.parseString(shell("run-as $host cat files/sdk37-bal-$caseId.json")).asJsonObject
                }
                assertTrue(evidence.toString(), evidence["installerSucceeded"]?.asBoolean == true)
                assertTrue(evidence["installerPreferenceRestored"].asBoolean)
                assertEquals(context.packageManager.getApplicationInfo(host, 0).uid, evidence["uid"].asInt)
                assertNotEquals(android.os.Process.myUid(), evidence["uid"].asInt)
                instrumentation.sendStatus(0, android.os.Bundle().apply { putString("host-installer", evidence.toString()) })
            } finally {
                record?.close()
                // End a pending debug probe through its own lifecycle and restore the user's setting.
                shell("am start -f 0x30080000 -d $document -n $host/org.autojs.autojs.compat.ActivityLaunchCompatibilityProbeActivity --es cancelInstallerProbe $caseId")
            }
        }
    }

    private fun shell(command: String): String = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).use {
        ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().use { input -> input.readText() }
    }
}
