package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.PrivilegedClient
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.*
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.ArchiveOpener
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.autojs.plugin.packagearchive.PackageDeviceSpec
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Opt-in package operations, restricted to code-free fixtures shipped with this test APK. */
@RunWith(AndroidJUnit4::class)
class CoreInstallMatrixDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val arguments = InstrumentationRegistry.getArguments()
    private val selected get() = Authorizer.fromId(arguments.getString("engineAuthorizer"))

    @Test fun testOnlyRequiresAnExplicitPrivilegedFlag() = withFixture("testonly") { authorizer, directory ->
        val prepared = prepare(directory, "test-only.apk")
        val failure = assertThrows(InstallFailure::class.java) { install(prepared, authorizer) }
        assertTrue(failure.toJson().toString(), failure.systemMessage.orEmpty().contains("INSTALL_FAILED_TEST_ONLY"))
        if (authorizer.privileged) {
            install(prepared, authorizer, InstallOptions(authorizer = authorizer.id, allowTestOnly = true))
            assertEquals(1L, installedVersion(prepared.packageName!!, authorizer, 0))
        } else {
            assertEquals(InstallerErrorCodes.AUTHORIZER_REQUIRED,
                assertThrows(InstallFailure::class.java) {
                    install(prepared, authorizer, InstallOptions(authorizer = authorizer.id, allowTestOnly = true))
                }.code)
        }
    }

    @Test fun debuggableDowngradeRequiresTheFlag() {
        assumeTrue(selected?.privileged == true)
        withFixture("downgrade") { authorizer, directory ->
            val v2 = prepare(directory, "debug-v2.apk")
            val v1 = prepare(directory, "debug-v1.apk")
            install(v2, authorizer)
            val refused = assertThrows(InstallFailure::class.java) { install(v1, authorizer) }
            assertEquals(InstallerErrorCodes.INSTALL_FAILED, refused.code)
            assertTrue(refused.systemMessage.orEmpty().contains("INSTALL_FAILED_VERSION_DOWNGRADE"))
            install(v1, authorizer, InstallOptions(authorizer = authorizer.id, allowDowngrade = true))
            assertEquals(1L, installedVersion(v1.packageName!!, authorizer, 0))
        }
    }

    @Test fun lowTargetSdkBypassAndPre34Note() = withFixture("lowtarget") { authorizer, directory ->
        val prepared = prepare(directory, "low-target.apk")
        assertEquals(22, prepared.targetSdk)
        if (Build.VERSION.SDK_INT >= 34) {
            try {
                install(prepared, authorizer)
                assertEquals(1L, installedVersion(prepared.packageName!!, authorizer, 0))
                evidence("${authorizer.id}: ROM accepts targetSdk 22 without bypass; no block to bypass on this device")
                assertTrue(shell("pm uninstall ${prepared.packageName}").contains("Success"))
                assertNull(installedVersion(prepared.packageName!!, authorizer, 0))
            } catch (failure: InstallFailure) {
                assertTrue(failure.toJson().toString(), failure.systemMessage.orEmpty().contains("INSTALL_FAILED_DEPRECATED_SDK_VERSION"))
                evidence("${authorizer.id}: ${failure.systemMessage}")
            }
        }
        if (authorizer.privileged) {
            val result = install(prepared, authorizer, InstallOptions(authorizer = authorizer.id, bypassLowTargetSdk = true))
            assertEquals(Build.VERSION.SDK_INT < 34, result.notes.any { it.contains("was ignored") })
            assertEquals(1L, installedVersion(prepared.packageName!!, authorizer, 0))
        } else if (Build.VERSION.SDK_INT < 34) {
            install(prepared, authorizer)
            assertEquals(1L, installedVersion(prepared.packageName!!, authorizer, 0))
        }
    }

    @Test fun releaseDowngradeRecordsTheActualRomPolicy() {
        assumeTrue(selected?.privileged == true)
        withFixture("release") { authorizer, directory ->
            val v2 = prepare(directory, "release-v2.apk")
            val v1 = prepare(directory, "release-v1.apk")
            install(v2, authorizer)
            try {
                install(v1, authorizer, InstallOptions(authorizer = authorizer.id, allowDowngrade = true))
                assertEquals(1L, installedVersion(v1.packageName!!, authorizer, 0))
                evidence("${authorizer.id} ${Build.TYPE}: non-debuggable downgrade accepted with both flags")
            } catch (failure: InstallFailure) {
                assertEquals(InstallerErrorCodes.INSTALL_FAILED, failure.code)
                assertTrue(failure.toJson().toString(), failure.systemMessage.orEmpty().contains("INSTALL_FAILED_VERSION_DOWNGRADE"))
                assertEquals(2L, installedVersion(v1.packageName!!, authorizer, 0))
                evidence("${authorizer.id} ${Build.TYPE}: ${failure.systemMessage}")
            }
        }
    }

    @Test fun xapkInstallsBothBaseAndFeatureSplit() = withFixture("splits") { authorizer, directory ->
        val prepared = splitContainer(directory)
        assertEquals(2, prepared.apks.size)
        install(prepared, authorizer)
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(prepared.packageName!!, 0)
        assertArrayEquals(arrayOf("feature.extra"), info.splitNames)
        assertEquals(1L, installedVersion(prepared.packageName!!, authorizer, 0))
    }

    @Test fun explicitInstallerAttributionIsRecorded() {
        assumeTrue(selected?.privileged == true)
        withFixture("splits") { authorizer, directory ->
            val prepared = splitContainer(directory)
            install(prepared, authorizer, InstallOptions(authorizer = authorizer.id, installer = context.packageName))
            @Suppress("DEPRECATION")
            assertEquals(context.packageName, context.packageManager.getInstallerPackageName(prepared.packageName!!))
        }
    }

    @Test fun secondaryUserReceivesOnlyTheRequestedFixture() {
        val userId = arguments.getString("coreUser")?.toIntOrNull()
        assumeTrue("Opt in with an existing disposable coreUser", selected?.privileged == true && userId != null && userId > 0)
        withFixture("splits") { authorizer, directory ->
            val target = requireNotNull(userId)
            assertEquals(target, DeviceUsers(context).resolve(target.toString(), authorizer, 15_000))
            val prepared = splitContainer(directory)
            install(prepared, authorizer, InstallOptions(authorizer = authorizer.id, user = target.toString()), target)
            assertEquals(1L, installedVersion(prepared.packageName!!, authorizer, target))
            assertNull("Installing for another user must not install for the current user", installedVersion(prepared.packageName!!, authorizer, 0))
        }
    }

    @Test fun threeDistinctPackagesCompleteInOneBatch() {
        assumeTrue(selected?.privileged == true)
        withFixture("downgrade") { authorizer, directory ->
            withFixture("release") { _, _ ->
                withFixture("splits") { _, _ ->
                    val files = listOf("debug-v1.apk", "release-v1.apk", "split-base.apk").map { name ->
                        File(directory, name).also { file ->
                            instrumentation.context.assets.open("core-fixtures/$name").use { input -> file.outputStream().use(input::copyTo) }
                        }
                    }
                    val descriptors = files.map { ParcelFileDescriptor.open(it, ParcelFileDescriptor.MODE_READ_ONLY) }
                    try {
                        var result: com.google.gson.JsonObject? = null
                        var failure: InstallFailure? = null
                        val request = InstallRequest("matrix-batch-${System.nanoTime()}",
                            files.mapIndexed { i, file -> SourceEntry(i, i, file.name, file.length()) },
                            InstallerContract.INTERACTION_SILENT, InstallOptions(authorizer = authorizer.id))
                        InstallSession(request, DescriptorInstallEnvironment.acquire(context, descriptors), object : InstallSession.Listener {
                            override fun onCompleted(value: com.google.gson.JsonObject) { result = value }
                            override fun onFailed(value: InstallFailure) { failure = value }
                        }, SystemClock::elapsedRealtime).use { session ->
                            session.start(java.util.concurrent.Executor { it.run() })
                        }
                        failure?.let { throw it }
                        val items = requireNotNull(result).getAsJsonArray("results")
                        assertEquals(3, items.size())
                        assertTrue(items.all { it.asJsonObject["ok"].asBoolean })
                        for (suffix in listOf("downgrade", "release", "splits")) {
                            assertEquals(1L, installedVersion("io.github.supermonster003.autojs6.installer.core.$suffix", authorizer, 0))
                        }
                        assertTrue(descriptors.all { it.fileDescriptor.valid() })
                    } finally { descriptors.forEach { it.close() } }
                }
            }
        }
    }

    private fun withFixture(suffix: String, run: (Authorizer, File) -> Unit) {
        assumeTrue("Opt in with engineAuthorizer=none|shizuku|root", selected != null)
        val authorizer = requireNotNull(selected)
        val packageName = "io.github.supermonster003.autojs6.installer.core.$suffix"
        // -u includes retained data; checking every user prevents replacing a fixture owned elsewhere.
        val users = Regex("UserInfo\\{(\\d+):").findAll(shell("pm list users")).map { it.groupValues[1] }.toList()
        check(users.isNotEmpty()) { "Cannot enumerate users for fixture ownership checks" }
        check(users.none { packageName in listedPackages(it) }) {
            "Fixture already exists for a device user; refusing to modify it"
        }
        val directory = File(context.cacheDir, "core-matrix-${System.nanoTime()}").apply { check(mkdir()) }
        try {
            if (authorizer.privileged) check(AuthorizerStates.request(context, authorizer, 30_000)) { "Authorizer grant is required" }
            run(authorizer, directory)
            evidence("${authorizer.id} $suffix verified")
        } finally {
            try {
                val answer = shell("pm uninstall $packageName")
                check(answer.contains("Success") || answer.contains("not installed") || answer.contains("DELETE_FAILED_INTERNAL_ERROR")) { answer }
                check(users.none { packageName in listedPackages(it) }) { "Fixture cleanup failed" }
            } finally {
                directory.deleteRecursively()
                PrivilegedClient.get(context).releaseAll()
            }
        }
    }

    private fun prepare(directory: File, name: String): PreparedPackage {
        val file = File(directory, name)
        instrumentation.context.assets.open("core-fixtures/$name").use { input -> file.outputStream().use(input::copyTo) }
        return ArchiveOpener.open(file, name, PackageDeviceSpec.from(context), File(directory, "prepared-$name").apply { mkdir() }, true)
            .also { it.failure()?.let { failure -> throw failure } }
    }

    private fun splitContainer(directory: File): PreparedPackage {
        val file = File(directory, "fixture.xapk")
        ZipOutputStream(file.outputStream()).use { zip ->
            for ((asset, entry) in listOf("split-base.apk" to "base.apk", "split-feature.apk" to "feature.extra.apk")) {
                zip.putNextEntry(ZipEntry(entry))
                instrumentation.context.assets.open("core-fixtures/$asset").use { it.copyTo(zip) }
                zip.closeEntry()
            }
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write("""{"package_name":"io.github.supermonster003.autojs6.installer.core.splits","version_code":1}""".toByteArray())
            zip.closeEntry()
        }
        return ArchiveOpener.open(file, file.name, PackageDeviceSpec.from(context), File(directory, "extracted").apply { mkdir() }, true)
            .also { it.failure()?.let { failure -> throw failure } }
    }

    private fun install(prepared: PreparedPackage, authorizer: Authorizer,
        options: InstallOptions = InstallOptions(authorizer = authorizer.id), userId: Int = 0): InstallEngine.Result {
        val engine = if (authorizer.privileged) PrivilegedInstallEngine(context, authorizer) else NoneInstallEngine(context)
        return engine.install(InstallEngine.Request(prepared, options.copy(timeoutMillis = 60_000), userId), object : InstallEngine.Listener {
            override fun onUserAction(intent: Intent) {
                UserActionLauncher.launch(context, intent)
                val deadline = SystemClock.elapsedRealtime() + 15_000
                while (SystemClock.elapsedRealtime() < deadline) {
                    val root = instrumentation.uiAutomation.rootInActiveWindow
                    val owner = root?.packageName?.toString().orEmpty()
                    // Only this test's fixture prompt, never an unrelated permission/dialog.
                    if (owner in setOf("com.android.packageinstaller", "com.google.android.packageinstaller") &&
                        root?.findAccessibilityNodeInfosByText("3-Setup Core Fixture")?.isNotEmpty() == true) {
                        val button = (root.findAccessibilityNodeInfosByViewId("android:id/button1") +
                            root.findAccessibilityNodeInfosByViewId("$owner:id/ok_button") +
                            root.findAccessibilityNodeInfosByText("INSTALL").filter { it.text?.toString().equals("INSTALL", true) })
                            .firstOrNull { it.isEnabled && it.isClickable }
                        if (button?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) return
                    }
                    SystemClock.sleep(50)
                }
                throw AssertionError("The fixture's system confirmation could not be accepted")
            }
        })
    }

    private fun installedVersion(packageName: String, authorizer: Authorizer, userId: Int): Long? {
        if (authorizer.privileged) {
            val reply = PrivilegedClient.get(context).acquire(authorizer).getInstalledVersion(packageName, userId)
            return reply.takeIf { it.containsKey("versionCode") }?.getLong("versionCode")
        }
        return try {
            @Suppress("DEPRECATION") val info = context.packageManager.getPackageInfo(packageName, 0)
            @Suppress("DEPRECATION") if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        } catch (_: PackageManager.NameNotFoundException) { null }
    }

    private fun listedPackages(user: String): Set<String> {
        // UiAutomation on API 24 exposes no exit code. Require the framework sentinel and the
        // complete listing grammar, so empty/denied queries cannot prove fixture ownership.
        val lines = shell("pm list packages -u --user $user").lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
        check("package:android" in lines && lines.all { it.startsWith("package:") }) { "Cannot safely enumerate user $user packages" }
        return lines.map { it.removePrefix("package:") }.toSet()
    }

    private fun shell(command: String): String = instrumentation.uiAutomation.executeShellCommand(command).use {
        ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().use { reader -> reader.readText() }
    }

    private fun evidence(message: String) = instrumentation.sendStatus(0, Bundle().apply {
        putString("matrix", "${Build.MODEL} API=${Build.VERSION.SDK_INT} $message")
    })
}
