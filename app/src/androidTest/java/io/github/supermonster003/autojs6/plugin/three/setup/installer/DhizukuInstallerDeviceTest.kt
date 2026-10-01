package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.*
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.DhizukuFramework
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.ArchiveOpener
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.packagearchive.PackageDeviceSpec
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Only a newly prepared, dedicated owner device should opt into the policy mutation below. */
@RunWith(AndroidJUnit4::class)
class DhizukuInstallerDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val args = InstrumentationRegistry.getArguments()
    private val fixture = FixtureInstallUi.PACKAGE_NAME

    @Test fun deviceOwnerInstallsUpdatesAndUninstallsTheOwnedFixture() {
        requireDhizuku()
        FixturePackageOwnership(setOf(fixture)).use { ownership ->
            val directory = File(context.cacheDir, "p8-dhizuku-${UUID.randomUUID()}").apply { check(mkdir()) }
            val framework = DhizukuFramework(context)
            val baseline = framework.installer.mySessions.map { it.sessionId }.toSet()
            try {
                for (version in listOf(1, 2)) {
                    val source = File(directory, "fixture-v$version.apk")
                    instrumentation.context.assets.open(source.name).use { input -> source.outputStream().use(input::copyTo) }
                    val prepared = ArchiveOpener.open(source, source.name, PackageDeviceSpec.from(context),
                        File(directory, "prepared-$version").apply { check(mkdir()) }, true)
                    prepared.failure()?.let { throw it }
                    assertEquals(fixture, prepared.packageName)
                    ownership.installationStarted()
                    val result = DhizukuInstallEngine(context).install(InstallEngine.Request(prepared,
                        InstallOptions(authorizer = Authorizer.DHIZUKU.id, timeoutMillis = 60_000), Process.myUid() / 100000,
                        InstallerContract.INTERACTION_SILENT), noSystemPrompt())
                    assertEquals(InstallerContract.INTERACTION_SILENT, result.interaction)
                    @Suppress("DEPRECATION") val installed = context.packageManager.getPackageInfo(fixture, 0)
                    @Suppress("DEPRECATION") assertEquals(version, installed.versionCode)
                    @Suppress("DEPRECATION") assertEquals(framework.owner.packageName, context.packageManager.getInstallerPackageName(fixture))
                }
                val result = DhizukuUninstallEngine(context).uninstall(UninstallRequest(fixture, false, InstallerContract.USER_CURRENT,
                    Authorizer.DHIZUKU.id, InstallerContract.INTERACTION_SILENT, 60_000), Process.myUid() / 100000, noSystemPrompt())
                assertEquals(Authorizer.DHIZUKU.id, result.authorizer)
                assertTrue(runCatching { context.packageManager.getPackageInfo(fixture, 0) }.isFailure)
                assertEquals(baseline, framework.installer.mySessions.map { it.sessionId }.toSet())
                evidence("owner=${framework.owner.packageName} install=1 update=2 uninstall=true interaction=silent sessionsRestored=true")
            } finally { directory.deleteRecursively() }
        }
    }

    private fun requireDhizuku() {
        assumeTrue("Explicit confirmDhizukuFixture=true is required", args.getString("confirmDhizukuFixture") == "true")
        check(AuthorizerStates.state(context, Authorizer.DHIZUKU).usable) { "Activate the dedicated Dhizuku owner and grant the plugin first" }
        assertEquals(0, Process.myUid() / 100000)
    }
    private fun noSystemPrompt() = object : InstallEngine.Listener {
        override fun onUserAction(intent: Intent) { error("The explicitly silent device-owner operation requested a confirmation") }
    }
    private fun evidence(value: String) = instrumentation.sendStatus(0, Bundle().apply { putString("dhizuku", value) })
}
