package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.Manifest
import android.app.ActivityManager
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.PrivilegedClient
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.DescriptorInstallEnvironment
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallRequest
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallSession
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.SourceEntry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallDialogActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallForegroundService
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallNotifications
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation
import org.autojs.plugin.installer.api.InstallerContract as C
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Explicit opt-in real package operations. Notification permission is observed and never changed.
 * A 2 GiB xapk is supplied separately; it is not embedded in the instrumentation APK.
 */
@RunWith(AndroidJUnit4::class)
class LargeInstallForegroundDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val arguments = InstrumentationRegistry.getArguments()
    private val notifications = context.getSystemService(NotificationManager::class.java)

    @Test fun realLargeXapkWritesInBackgroundAndKeepsForegroundProgress() {
        val path = arguments.getString("largeFixturePath")
        assumeTrue("Supply -e largeFixturePath with the generated xapk in the plugin cache", !path.isNullOrBlank())
        require(Build.VERSION.SDK_INT >= 34) { "P3.4 large-package acceptance requires API 34+" }
        val file = File(requireNotNull(path)).canonicalFile
        require(file.path.startsWith(context.cacheDir.canonicalPath + File.separator)) { "Place the fixture inside the plugin cache" }
        require(file.isFile && file.name.endsWith(".xapk", ignoreCase = true)) { "The fixture must be a readable xapk" }
        require(file.length() >= LARGE_MINIMUM_BYTES) { "A small or padded simulation cannot satisfy the 2 GiB package check" }
        runRealInstall(file, LARGE_PACKAGE, LARGE_MINIMUM_BYTES, shortObservation = false)
    }

    @Test fun realShortPackageChecksBackgroundForegroundAndExistingNotificationPermission() {
        assumeTrue("Opt in with -e foregroundShortFixture true", arguments.getString("foregroundShortFixture") == "true")
        val file = File(context.cacheDir, "foreground-short-${UUID.randomUUID()}.apk")
        try {
            instrumentation.context.assets.open("core-fixtures/split-base.apk").use { input -> file.outputStream().use(input::copyTo) }
            runRealInstall(file, SHORT_PACKAGE, 1L, shortObservation = true)
        } finally {
            check(file.delete() || !file.exists()) { "Could not remove the short test source" }
        }
    }

    private fun runRealInstall(source: File, packageName: String, minimumApkBytes: Long, shortObservation: Boolean) {
        val authorizer = Authorizer.fromId(arguments.getString("foregroundAuthorizer") ?: arguments.getString("engineAuthorizer"))
        require(authorizer?.privileged == true) { "Explicit foregroundAuthorizer=shizuku|root is required" }
        check(!context.getSystemService(KeyguardManager::class.java).isKeyguardLocked) { "Unlock the selected test device before running" }
        val users = allUsers()
        // Refuse before acquiring sources or services. Retained app data and every user count.
        check(users.none { packageName in listedPackages(it) }) { "Fixture or retained data already exists for a device user; refusing to modify it" }
        val needed = source.length() * 3 + 256 * MIB
        check(context.cacheDir.usableSpace >= needed) { "Insufficient cache space for extraction and the real package session: need $needed bytes" }
        val permissionBefore = postPermissionGranted()
        val drawerEnabledBefore = drawerEnabled()
        if (arguments.getString("foregroundRequireNotifications") == "true") {
            check(permissionBefore && drawerEnabledBefore) {
                "Visible-notification acceptance requires the existing notification permission and drawer to be enabled"
            }
        }
        check(AuthorizerStates.request(context, requireNotNull(authorizer), 30_000)) { "The selected authorizer is not granted" }

        val sessionRef = AtomicReference<InstallSession>()
        val request = InstallRequest("foreground-${UUID.randomUUID()}", listOf(SourceEntry(0, 0, source.name, source.length())),
            C.INTERACTION_SILENT, InstallOptions(authorizer = authorizer.id, timeoutMillis = 1_800_000))
        val record = InstallPresentation.create(context, request, InstallPresentation.Callbacks(cancel = { sessionRef.get()?.cancel() }))
        val token = record.token
        val executor = Executors.newSingleThreadExecutor()
        val writing = CountDownLatch(1)
        val backgroundReady = CountDownLatch(1)
        val done = CountDownLatch(1)
        val result = AtomicReference<JsonObject>()
        val error = AtomicReference<InstallFailure>()
        val expectedApkBytes = AtomicLong()
        val writtenBytes = AtomicLong()
        val firstShortProgress = AtomicBoolean(true)
        val startedAt = SystemClock.elapsedRealtime()
        var ownedEnvironment: DescriptorInstallEnvironment? = null
        var started = false
        val fixtureValidated = AtomicBoolean()
        var foregroundSamples = 0
        var backgroundAt = 0L
        val progressValues = sortedSetOf<Int>()
        val initialPid = Process.myPid()
        val descriptor = ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY)
        try {
            val environment = DescriptorInstallEnvironment.acquire(context, listOf(descriptor), preparedListener = { index, prepared ->
                validateFixture(prepared, packageName, minimumApkBytes)
                fixtureValidated.set(true)
                expectedApkBytes.set(prepared.totalBytes)
                record.onPrepared(index, prepared)
                evidence("prepared", "sourceBytes=${source.length()} apkBytes=${prepared.totalBytes} splits=${prepared.apks.size}")
            })
            ownedEnvironment = environment
            val session = InstallSession(request, environment, object : InstallSession.Listener {
                override fun onStage(stage: String, detail: JsonObject) {
                    record.onStage(stage, detail)
                    InstallNotifications.update(context, token, source.name, stage, sessionRef.get()?.status()?.progress ?: 0f, record.activityIntent()) {
                        sessionRef.get()?.cancel()
                    }
                    if (stage == C.STAGE_WRITING) {
                        writing.countDown()
                        // The real platform session already exists. Keep its writer at the stage
                        // boundary only until the test has observed foreground coverage and pressed HOME.
                        check(backgroundReady.await(20, TimeUnit.SECONDS)) { "Foreground/background observation did not start in time" }
                    }
                }

                override fun onProgress(progress: Float, detail: JsonObject) {
                    writtenBytes.set(detail.get(C.FIELD_BYTES_WRITTEN).asLong)
                    record.onProgress(progress, detail)
                    InstallNotifications.update(context, token, source.name, C.STAGE_WRITING, progress, record.activityIntent()) {
                        sessionRef.get()?.cancel()
                    }
                    // A tiny real APK can finish within one notification frame. Hold its actual
                    // byte callback for observation; this mode is explicitly not large-package evidence.
                    if (shortObservation && progress > 0f && firstShortProgress.compareAndSet(true, false)) Thread.sleep(SHORT_OBSERVATION_MILLIS)
                }

                override fun onItemResult(index: Int, result: JsonObject) = record.onItemResult(index, result)

                override fun onCompleted(value: JsonObject) {
                    result.set(value)
                    record.onCompleted(value)
                    InstallNotifications.complete(context, token, true, openIntent = record.activityIntent())
                    done.countDown()
                }

                override fun onFailed(failure: InstallFailure) {
                    error.set(failure)
                    record.onFailed(failure)
                    InstallNotifications.complete(context, token, false, openIntent = record.activityIntent())
                    done.countDown()
                }
            }, SystemClock::elapsedRealtime)
            sessionRef.set(session)
            withInstallScenario(record) { scenario ->
                waitUntil(10_000) {
                    var focused = false
                    scenario.onActivity { focused = it.hasWindowFocus() }
                    focused
                }
                session.start(executor)
                started = true
                awaitWriting(writing, done, error)
                try {
                    waitUntil(10_000, "Foreground service did not enter foreground state") { foregroundRunning() }
                    if (drawerEnabledBefore) {
                        waitUntil(10_000, "The allowed foreground notification was not published") { foregroundNotification() != null }
                        progressValues += foregroundNotification()!!.extras.getInt(Notification.EXTRA_PROGRESS)
                    }
                    foregroundSamples++
                    evidence("foreground-observed", "foreground=true postPermission=$permissionBefore drawerEnabled=$drawerEnabledBefore " +
                        "notificationProgress=${foregroundNotification()?.extras?.getInt(Notification.EXTRA_PROGRESS)}")
                    val service = context.packageManager.getServiceInfo(ComponentName(context, InstallForegroundService::class.java), 0)
                    if (Build.VERSION.SDK_INT >= 29) assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC, service.foregroundServiceType)
                    val homeResult = shell("input keyevent KEYCODE_HOME").trim()
                    var windowState = "not observed"
                    waitUntil(10_000, "HOME did not stop and unfocus the actual installer Activity", diagnostics = {
                        "$windowState homeOutput=$homeResult accessibilityPackage=" +
                            instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString()
                    }) {
                        var background = false
                        scenario.onActivity { activity ->
                            val stage = ActivityLifecycleMonitorRegistry.getInstance().getLifecycleStageOf(activity)
                            val focus = activity.hasWindowFocus()
                            windowState = "stage=$stage focus=$focus visibility=${activity.window.decorView.windowVisibility}"
                            background = stage == Stage.STOPPED && !focus
                        }
                        background
                    }
                    backgroundAt = SystemClock.elapsedRealtime()
                    evidence("background", "pid=$initialPid foreground=true postPermission=$permissionBefore drawerEnabled=$drawerEnabledBefore $windowState")
                } finally {
                    backgroundReady.countDown()
                }
                val deadline = startedAt + request.options.timeoutMillis + 30_000
                while (!done.await(150, TimeUnit.MILLISECONDS)) {
                    check(SystemClock.elapsedRealtime() < deadline) { "The real background installation timed out" }
                    if (session.status().stage == C.STAGE_WRITING && done.count > 0) {
                        check(foregroundRunning()) { "The foreground service disappeared during the real write" }
                        foregroundSamples++
                        foregroundNotification()?.extras?.getInt(Notification.EXTRA_PROGRESS)?.let(progressValues::add)
                    }
                }
                error.get()?.let { throw AssertionError("The real installation failed: ${it.code}: ${it.systemMessage ?: it.message}", it) }
                val answer = requireNotNull(result.get())
                assertTrue(answer.toString(), answer.get(C.FIELD_OK).asBoolean)
                assertEquals(packageName, answer.get(C.FIELD_PACKAGE_NAME).asString)
                assertEquals(C.INTERACTION_SILENT, answer.get(C.FIELD_INTERACTION).asString)
                assertEquals(1L, answer.get(C.FIELD_VERSION_CODE).asLong)
                assertEquals(expectedApkBytes.get(), writtenBytes.get())
                assertEquals(initialPid, Process.myPid())
                assertTrue("No sustained foreground observation was recorded", foregroundSamples >= 2)
                if (drawerEnabledBefore) {
                    assertTrue("The real write did not produce different notification progress values: $progressValues", progressValues.size >= 2)
                    if (!shortObservation) assertTrue("No intermediate large-transfer notification was observed: $progressValues", progressValues.any { it in 1..99 })
                }
                @Suppress("DEPRECATION")
                val installed = context.packageManager.getPackageInfo(packageName, 0)
                @Suppress("DEPRECATION")
                assertEquals(1L, if (Build.VERSION.SDK_INT >= 28) installed.longVersionCode else installed.versionCode.toLong())
                val app = requireNotNull(installed.applicationInfo)
                val installedBytes = (listOf(app.sourceDir) + app.splitSourceDirs.orEmpty()).sumOf { File(it).length() }
                assertEquals("The installed APKs must contain the bytes actually transferred", expectedApkBytes.get(), installedBytes)
                assertTrue(installedBytes >= minimumApkBytes)
                waitUntil(10_000) { !foregroundRunning() }
                if (!drawerEnabledBefore) assertTrue(notifications.activeNotifications.none { it.tag == "installation.result:$token" })
                assertEquals("The test must not change notification permission", permissionBefore, postPermissionGranted())
                evidence("passed", "mode=${if (shortObservation) "short" else "large"} apkBytes=$installedBytes writtenBytes=${writtenBytes.get()} " +
                    "backgroundMillis=${SystemClock.elapsedRealtime() - backgroundAt} foregroundSamples=$foregroundSamples notificationProgress=$progressValues " +
                    "shortObservationHoldMillis=${if (shortObservation) SHORT_OBSERVATION_MILLIS else 0} " +
                    "elapsedMillis=${SystemClock.elapsedRealtime() - startedAt} pid=$initialPid")
                // Execute the screen's real completion action, even though HOME has paused its task.
                // Waiting for destruction avoids asking ActivityScenario to synthesize that transition.
                waitUntil(10_000) {
                    var clicked = false
                    scenario.onActivity { activity ->
                        activity.window.decorView.findViewWithTag<View>(InstallDialogActivity.TAG_DONE)?.let { button ->
                            if (button.isEnabled) clicked = button.performClick()
                        }
                    }
                    clicked
                }
                waitUntil(10_000) { scenario.state == Lifecycle.State.DESTROYED }
            }
        } finally {
            backgroundReady.countDown()
            sessionRef.get()?.cancel()
            sessionRef.get()?.close()
            executor.shutdownNow()
            val stopped = executor.awaitTermination(30, TimeUnit.SECONDS)
            instrumentation.waitForIdleSync()
            InstallNotifications.remove(context, token)
            record.close()
            if (!started) ownedEnvironment?.close()
            descriptor.close()
            try {
                check(stopped) { "Writer did not stop; refusing to race fixture cleanup against an active install" }
                if (fixtureValidated.get() && users.any { packageName in listedPackages(it) }) {
                    val answer = shell("pm uninstall $packageName")
                    check(answer.lineSequence().any { it.trim() == "Success" }) { "Fixture cleanup failed: $answer" }
                    check(users.none { packageName in listedPackages(it) }) { "Fixture data remained after cleanup" }
                }
            } finally {
                PrivilegedClient.get(context).releaseAll()
            }
        }
    }

    private fun validateFixture(prepared: PreparedPackage, expectedPackage: String, minimumBytes: Long) {
        prepared.failure()?.let { throw it }
        check(prepared.packageName == expectedPackage && prepared.versionCode == 1L) { "The supplied source is not the dedicated fixture" }
        check(prepared.totalBytes >= minimumBytes) { "The APK content, not container padding, must meet the size requirement" }
        val explicitPermissions = prepared.apks.associate { apk ->
            val manifest = requireNotNull(apk.manifest) { "Fixture manifest summary is missing: ${apk.name}" }
            check(manifest.packageName == expectedPackage && manifest.versionCode == 1L) { "Unexpected split identity: ${apk.name}" }
            apk.name to manifest.requestedPermissions
        }
        val base = requireNotNull(prepared.baseApk)
        @Suppress("DEPRECATION")
        val info = requireNotNull(context.packageManager.getPackageArchiveInfo(base.file.path,
            PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS or PackageManager.GET_PERMISSIONS))
        val app = requireNotNull(info.applicationInfo)
        val diagnostic = "package=$expectedPackage targetSdk=${app.targetSdkVersion} appFlags=0x${app.flags.toUInt().toString(16)} " +
            "explicitPermissions=$explicitPermissions platformPermissions=${info.requestedPermissions.orEmpty().toList()} " +
            "activities=${info.activities.orEmpty().map { it.name }} services=${info.services.orEmpty().map { it.name }} " +
            "receivers=${info.receivers.orEmpty().map { it.name }} providers=${info.providers.orEmpty().map { it.name }}"
        evidence("fixture-validation", diagnostic)
        check(app.flags and ApplicationInfo.FLAG_HAS_CODE == 0) { "The fixture must not contain application code: $diagnostic" }
        check(info.activities.isNullOrEmpty() && info.services.isNullOrEmpty() && info.receivers.isNullOrEmpty() &&
            info.providers.isNullOrEmpty()) { "The fixture must not have components: $diagnostic" }
        // PackageManager may add compatibility permissions to old-target packages. Require every
        // actual APK manifest to declare no permissions; never whitelist an explicit permission.
        check(explicitPermissions.values.all { it.isEmpty() }) { "The fixture must not declare permissions: $diagnostic" }
    }

    private fun withInstallScenario(record: InstallPresentation.Record, run: (ActivityScenario<InstallDialogActivity>) -> Unit) {
        ActivityScenario.launch<InstallDialogActivity>(record.activityIntent()).use { scenario ->
            try {
                run(scenario)
            } catch (failure: Throwable) {
                record.close()
                runCatching { waitUntil(10_000) { scenario.state == Lifecycle.State.DESTROYED } }
                    .exceptionOrNull()?.let(failure::addSuppressed)
                throw failure
            }
            record.close()
            waitUntil(10_000) { scenario.state == Lifecycle.State.DESTROYED }
        }
    }

    private fun awaitWriting(writing: CountDownLatch, done: CountDownLatch, failure: AtomicReference<InstallFailure>) {
        val deadline = SystemClock.elapsedRealtime() + 1_800_000
        var nextReport = SystemClock.elapsedRealtime() + 30_000
        while (!writing.await(200, TimeUnit.MILLISECONDS)) {
            failure.get()?.let { throw AssertionError("Preparation failed: ${it.code}: ${it.message}", it) }
            check(done.count > 0 && SystemClock.elapsedRealtime() < deadline) { "The session never reached actual writing" }
            if (SystemClock.elapsedRealtime() >= nextReport) {
                evidence("preparing", "waiting for real archive inspection and extraction")
                nextReport += 30_000
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun foregroundRunning(): Boolean = context.getSystemService(ActivityManager::class.java).getRunningServices(100)
        .any { it.service == ComponentName(context, InstallForegroundService::class.java) && it.foreground && it.pid == Process.myPid() }

    private fun foregroundNotification(): Notification? = notifications.activeNotifications
        .firstOrNull { it.id == InstallNotifications.FOREGROUND_ID && it.tag == null }?.notification

    private fun postPermissionGranted() = Build.VERSION.SDK_INT < 33 ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun drawerEnabled() = postPermissionGranted() && notifications.areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < 26 || notifications.getNotificationChannel(InstallNotifications.CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE)

    private fun allUsers(): List<String> = Regex("UserInfo\\{(\\d+):").findAll(shell("pm list users"))
        .map { it.groupValues[1] }.toList().also { check(it.isNotEmpty()) { "Cannot enumerate all users safely" } }

    private fun listedPackages(user: String): Set<String> {
        val lines = shell("pm list packages -u --user $user").lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
        check("package:android" in lines && lines.all { it.startsWith("package:") }) { "Cannot safely enumerate user $user packages" }
        return lines.map { it.removePrefix("package:") }.toSet()
    }

    private fun shell(command: String): String = instrumentation.uiAutomation.executeShellCommand(command).use { descriptor ->
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
    }

    private fun waitUntil(timeoutMillis: Long, description: String = "Expected foreground/notification/window state did not appear",
        diagnostics: () -> String = { "" }, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        while (!condition()) {
            check(SystemClock.elapsedRealtime() < deadline) { "$description; ${diagnostics()}" }
            SystemClock.sleep(50)
        }
    }

    private fun evidence(phase: String, message: String) = instrumentation.sendStatus(0, Bundle().apply {
        putString("foregroundInstall", "${Build.MODEL} API=${Build.VERSION.SDK_INT} $phase $message")
    })

    private companion object {
        const val MIB = 1024L * 1024
        const val LARGE_MINIMUM_BYTES = 1984L * MIB
        const val LARGE_PACKAGE = "io.github.supermonster003.autojs6.installer.large.fixture"
        const val SHORT_PACKAGE = "io.github.supermonster003.autojs6.installer.core.splits"
        const val SHORT_OBSERVATION_MILLIS = 1200L
    }
}
