package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.PrivilegedClient
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.*
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.IPrivilegedInstaller
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.PrivilegedOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.ArchiveOpener
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import org.autojs.plugin.installer.api.InstallerContract as C
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.autojs.plugin.packagearchive.PackageDeviceSpec
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/** The fixed payload has DEX for compilation, no Android components or permission-using code. */
@RunWith(AndroidJUnit4::class)
class AdvancedInstallOptionsDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val arguments = InstrumentationRegistry.getArguments()
    private val user = Process.myUid() / 100_000

    @Test fun unsupportedOptionsAreRejectedBeforeAnySessionIsOpened() = withSource { prepared ->
        val baseline = context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet()
        val cases = arrayListOf<Pair<Authorizer, InstallOptions>>()
        for (authorizer in listOf(Authorizer.NONE, Authorizer.DHIZUKU)) {
            cases += authorizer to InstallOptions(authorizer = authorizer.id, grantAllRequestedPermissions = true)
            cases += authorizer to InstallOptions(authorizer = authorizer.id, dexopt = C.DEXOPT_SPEED)
        }
        if (Build.VERSION.SDK_INT < 34) cases += Authorizer.ROOT to InstallOptions(authorizer = "root", requestUpdateOwnership = true)
        if (Build.VERSION.SDK_INT < 33) cases += Authorizer.ROOT to InstallOptions(authorizer = "root", packageSource = C.PACKAGE_SOURCE_UNSPECIFIED)
        if (Build.VERSION.SDK_INT < 26) {
            cases += Authorizer.ROOT to InstallOptions(authorizer = "root", installReason = C.INSTALL_REASON_UNKNOWN)
            cases += Authorizer.ROOT to InstallOptions(authorizer = "root", dexopt = C.DEXOPT_VERIFY)
        }
        for ((authorizer, options) in cases) {
            var opens = 0
            val engine = object : SessionInstallEngine(authorizer) {
                override fun openSession(request: InstallEngine.Request, parameters: Parameters, deadlineMillis: Long): Session {
                    opens++
                    error("A rejected option reached platform session creation")
                }
            }
            val failure = assertThrows(InstallFailure::class.java) {
                engine.install(InstallEngine.Request(prepared, options, user, C.INTERACTION_AUTO), noPrompt())
            }
            assertEquals(if (authorizer in setOf(Authorizer.NONE, Authorizer.DHIZUKU)) InstallerErrorCodes.AUTHORIZER_REQUIRED else InstallerErrorCodes.INVALID_ARGUMENT, failure.code)
            assertEquals(0, opens)
        }
        assertEquals(baseline, context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet())
        evidence("preSessionRejected=${cases.size} platformOpens=0 authorizerRequests=0")
    }

    @Test fun runtimeGrantMetadataDexoptAndLateTimeoutPreserveInstalledFacts() {
        assumeTrue("Opt in with confirmAdvancedFixture=true", arguments.getString("confirmAdvancedFixture") == "true")
        val authorizer = Authorizer.fromId(arguments.getString("advancedAuthorizer"))
        assumeTrue("Choose the actual Root or Shizuku identity", authorizer in setOf(Authorizer.ROOT, Authorizer.SHIZUKU))
        val selected = requireNotNull(authorizer)
        FixturePackageOwnership(setOf(FIXTURE)).use { ownership ->
            FixtureHistoryOwnership(context).use {
                withSource { prepared ->
                    check(AuthorizerStates.request(context, selected, 30_000)) { "The selected authorizer must be explicitly granted" }
                    val client = PrivilegedClient.get(context)
                    val before = context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet()
                    check(context.packageManager.packageInstaller.mySessions.none { it.appPackageName == FIXTURE })
                    val service = client.acquire(selected)
                    if (selected == Authorizer.ROOT) assertEquals(0, service.uid)
                    assertTrue(service.uid == 0 || service.uid == 2000)
                    val sessionOptions = arrayListOf<Bundle>()
                    val followUps = arrayListOf<Bundle>()
                    var forceZeroBudget = false
                    val observed = object : IPrivilegedInstaller by service {
                        override fun createSession(params: Bundle, installerPackageName: String, userId: Int): Int {
                            sessionOptions += Bundle(params)
                            ownership.installationStarted()
                            return service.createSession(params, installerPackageName, userId)
                        }
                        override fun postInstall(packageName: String, userId: Int, dexopt: String, readUpdateOwner: Boolean, timeoutMillis: Long): Bundle {
                            assertEquals(FIXTURE, packageName)
                            return service.postInstall(packageName, userId, dexopt, readUpdateOwner,
                                if (forceZeroBudget) 0 else timeoutMillis).also { followUps += Bundle(it) }
                        }
                    }
                    val engine = PrivilegedInstallEngine(context, selected,
                        acquire = { observed }, acquireRecovery = { error("This test must not replay or rebind an installation") })
                    val options = InstallOptions(authorizer = selected.id, timeoutMillis = 90_000,
                        installReason = C.INSTALL_REASON_USER.takeIf { Build.VERSION.SDK_INT >= 26 },
                        packageSource = C.PACKAGE_SOURCE_OTHER.takeIf { Build.VERSION.SDK_INT >= 33 },
                        requestUpdateOwnership = Build.VERSION.SDK_INT >= 34)
                    try {
                        PackageInstallLocks.acquire(FIXTURE) {}.use {
                            val initial = engine.install(InstallEngine.Request(prepared, options, user, C.INTERACTION_SILENT), noPrompt())
                            assertEquals(FIXTURE, initial.packageName)
                            assertEquals(PackageManager.PERMISSION_DENIED, context.packageManager.checkPermission(Manifest.permission.READ_CALENDAR, FIXTURE))
                            assertEquals(PackageManager.PERMISSION_GRANTED, context.packageManager.checkPermission(Manifest.permission.INTERNET, FIXTURE))
                            assertEquals(0, sessionOptions.single().getInt(PrivilegedOptions.FLAGS) and PrivilegedOptions.INSTALL_GRANT_ALL_REQUESTED_PERMISSIONS)
                            if (Build.VERSION.SDK_INT >= 26) assertEquals(4, sessionOptions.single().getInt(PrivilegedOptions.INSTALL_REASON))
                            if (Build.VERSION.SDK_INT >= 33) {
                                assertEquals(1, sessionOptions.single().getInt(PrivilegedOptions.PACKAGE_SOURCE))
                                assertEquals(PackageManager.PERMISSION_DENIED, context.packageManager.checkPermission(Manifest.permission.READ_CALENDAR, FIXTURE))
                                assertEquals(1, context.packageManager.getInstallSourceInfo(FIXTURE).packageSource)
                            }
                            if (Build.VERSION.SDK_INT >= 34) {
                                assertTrue(sessionOptions.single().getBoolean(PrivilegedOptions.REQUEST_UPDATE_OWNERSHIP))
                                assertTrue(initial.followUp.updateOwnerRead)
                                // Public PM may filter the owner for this app identity. Compare the
                                // privileged result with an independent shell observation instead.
                                val visible = context.packageManager.getInstallSourceInfo(FIXTURE).updateOwnerPackageName
                                val dump = instrumentation.uiAutomation.executeShellCommand("dumpsys package $FIXTURE").use {
                                    android.os.ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().use { reader -> reader.readText() }
                                }
                                val owners = Regex("(?m)^\\s+updateOwnerPackageName=([^\\r\\n]+)$").findAll(dump)
                                    .map { it.groupValues[1].trim() }.toList()
                                check(owners.size <= 1) { "The package dump has ambiguous update ownership" }
                                val actual = owners.singleOrNull()?.takeUnless { it == "null" }
                                assertEquals(actual, initial.followUp.updateOwner)
                                evidence("ownershipRequested=true privilegedObserved=${actual ?: "null"} publicVisible=${visible ?: "null"}")
                            }
                            val enabled = options.copy(grantAllRequestedPermissions = true, dexopt = C.DEXOPT_SPEED)
                            val granted = engine.install(InstallEngine.Request(prepared, enabled, user, C.INTERACTION_SILENT), noPrompt())
                            assertEquals(FIXTURE, granted.packageName)
                            assertEquals(PackageManager.PERMISSION_GRANTED, context.packageManager.checkPermission(Manifest.permission.READ_CALENDAR, FIXTURE))
                            assertTrue(sessionOptions.last().getInt(PrivilegedOptions.FLAGS) and PrivilegedOptions.INSTALL_GRANT_ALL_REQUESTED_PERMISSIONS != 0)
                            assertEquals(C.DEXOPT_STATUS_ACCEPTED, granted.followUp.dexopt?.status)
                            assertEquals(C.DEXOPT_SPEED, granted.followUp.dexopt?.filter)
                            assertEquals(0, followUps.last().getInt("dexoptExitCode", -1))
                            assertInstalledFixture()
                            evidence("authorizer=${selected.id} serviceUid=${service.uid} grantBefore=denied grantAfter=granted normalPermission=granted dexopt=${granted.followUp.dexopt}")

                            // A real zero-budget backend response after successful replacement must
                            // remain a completed installation, with a structured follow-up timeout.
                            forceZeroBudget = true
                            val timedOut = engine.install(InstallEngine.Request(prepared, enabled, user, C.INTERACTION_SILENT), noPrompt())
                            assertEquals(FIXTURE, timedOut.packageName)
                            assertEquals(C.DEXOPT_STATUS_TIMEOUT, timedOut.followUp.dexopt?.status)
                            assertFalse(followUps.last().containsKey("dexoptExitCode"))
                            assertInstalledFixture()
                            val document = InstallDocuments.installResult(FIXTURE, "1.0", 1, 1, selected.id,
                                timedOut.interaction, 0, false, timedOut.notes, timedOut.followUp)
                            assertTrue(document[C.FIELD_OK].asBoolean)
                            assertEquals(C.DEXOPT_STATUS_TIMEOUT, document.getAsJsonObject(C.FIELD_DEXOPT)["status"].asString)
                            evidence("postTimeout=true installOk=true version=1 compileWasNotStarted=true")
                        }
                        val removed = PrivilegedUninstallEngine(context, selected).uninstall(UninstallRequest(FIXTURE, false,
                            C.USER_CURRENT, selected.id, C.INTERACTION_SILENT, 60_000), user, noPrompt())
                        assertEquals(FIXTURE, removed.packageName)
                    } finally {
                        try {
                            val deadline = SystemClock.elapsedRealtime() + 10_000
                            while (context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet() != before &&
                                SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(25)
                            assertEquals(before, context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet())
                        } finally { client.releaseAll() }
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun assertInstalledFixture() {
        val info = context.packageManager.getPackageInfo(FIXTURE, PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or
            PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS)
        assertEquals(1, info.versionCode)
        assertTrue(info.activities.isNullOrEmpty() && info.services.isNullOrEmpty() && info.receivers.isNullOrEmpty() && info.providers.isNullOrEmpty())
    }

    private fun withSource(action: (PreparedPackage) -> Unit) {
        val directory = File(context.cacheDir, "advanced-options-${UUID.randomUUID()}")
        check(!directory.exists() && directory.mkdir() && directory.canonicalFile.parentFile == context.cacheDir.canonicalFile)
        try {
            val file = File(directory, "fixture.apk")
            val bytes = instrumentation.context.assets.open("advanced-fixtures/v1.apk").use { it.readBytes() }
            assertEquals(SHA256, MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) })
            file.writeBytes(bytes)
            val prepared = ArchiveOpener.open(file, file.name, PackageDeviceSpec.from(context), File(directory, "prepared").apply { check(mkdir()) }, true)
            prepared.failure()?.let { throw it }
            assertEquals(FIXTURE, prepared.packageName)
            action(prepared)
        } finally {
            check(directory.canonicalFile.parentFile == context.cacheDir.canonicalFile)
            check(directory.deleteRecursively() || !directory.exists())
        }
    }
    private fun noPrompt() = object : InstallEngine.Listener {
        override fun onUserAction(intent: Intent) { error("The explicitly silent fixed fixture unexpectedly requested confirmation") }
    }
    private fun evidence(value: String) = instrumentation.sendStatus(0, Bundle().apply {
        putString("advanced-options", "${Build.MODEL} API=${Build.VERSION.SDK_INT} $value")
    })
    companion object {
        const val FIXTURE = "io.github.supermonster003.autojs6.installer.advanced.fixture"
        private const val SHA256 = "bae693d55c5dd912ef22e2e87e5a3a6fb83a40dac15318fbca8a230e00b577a0"
    }
}
