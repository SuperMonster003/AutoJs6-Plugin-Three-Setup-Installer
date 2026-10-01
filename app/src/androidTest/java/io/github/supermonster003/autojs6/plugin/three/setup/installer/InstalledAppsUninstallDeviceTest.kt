package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.android.material.materialswitch.MaterialSwitch
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.AuthorizerStates
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.history.InstallHistoryEntry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.history.InstallHistoryStore
import io.github.supermonster003.autojs6.plugin.three.setup.installer.installedapps.InstalledApp
import io.github.supermonster003.autojs6.plugin.three.setup.installer.settings.InstallerPreferences
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.ExternalSources
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.ConfirmationActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.ExternalInstaller
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstalledAppsActivity
import org.autojs.plugin.installer.api.InstallerContract
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipFile

/** Explicit opt-in. Installs and removes only the pinned, code-free repository fixture. */
@RunWith(AndroidJUnit4::class)
class InstalledAppsUninstallDeviceTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @Test fun theSelectedFixtureIsUninstalledOnlyAfterTheInstalledAppsPageConfirmation() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("installedAppsUninstall") == "shizuku")
        assumeTrue("Unlock the device for the real application browser", !context.getSystemService(KeyguardManager::class.java).isKeyguardLocked)
        // State reads do not request authorization. A cold Shizuku binder may arrive shortly after
        // the runner attaches, but this test never asks the user to grant a new authorizer.
        await("Previously granted Shizuku did not become usable", 5_000) {
            AuthorizerStates.state(context, Authorizer.SHIZUKU).usable
        }
        assumeTrue("Existing authorization preferences must already select Shizuku; this test does not change them",
            InstallerPreferences.resolveAuthorizer(context, InstallerContract.AUTHORIZER_AUTO) == Authorizer.SHIZUKU)

        FixturePackageOwnership(setOf(FixtureInstallUi.PACKAGE_NAME)).use { ownership ->
            UnknownSourceGrantTrace(context).use { trace ->
                val folder = File(context.cacheDir, "installed-apps-uninstall-${System.nanoTime()}").apply { check(mkdir()) }
                val source = File(folder, "fixture.apk")
                var record: InstallPresentation.Record? = null
                var browser: ActivityScenario<InstalledAppsActivity>? = null
                var primaryFailure: Throwable? = null
                try {
                    instrumentation.context.assets.open("fixture-v1.apk").use { input -> source.outputStream().use(input::copyTo) }
                    assertFixture(source)
                    ownership.installationStarted()
                    val token = ExternalInstaller.start(context,
                        ExternalSources.fromIntent(Intent(Intent.ACTION_VIEW, Uri.fromFile(source))),
                        InstallOptions(authorizer = Authorizer.SHIZUKU.id, user = InstallerContract.USER_CURRENT,
                            deleteSource = false, timeoutMillis = 60_000),
                        interaction = InstallerContract.INTERACTION_SILENT)
                    val installation = requireNotNull(InstallPresentation.find(token)).also { record = it }
                    await("The pinned fixture installation did not finish", 65_000) { installation.snapshot().terminal }
                    val completed = installation.snapshot()
                    assertNull(completed.failure)
                    val installedResult = requireNotNull(completed.items.single().result)
                    assertTrue(installedResult.toString(), installedResult[InstallerContract.FIELD_OK].asBoolean)
                    assertEquals(Authorizer.SHIZUKU.id, installedResult[InstallerContract.FIELD_AUTHORIZER].asString)
                    assertEquals(FixtureInstallUi.PACKAGE_NAME, installedResult[InstallerContract.FIELD_PACKAGE_NAME].asString)
                    assertTrue("The fixture must actually be installed before exercising the browser", fixtureInstalled())
                    assertFixture(source)
                    installation.close()

                    val scenario = ActivityScenario.launch<InstalledAppsActivity>(Intent(context, InstalledAppsActivity::class.java)).also { browser = it }
                    await("The browser did not load the installed fixture", 20_000) {
                        var loaded = false
                        scenario.onActivity { activity ->
                            loaded = listed(activity).any { it.packageName == FixtureInstallUi.PACKAGE_NAME }
                        }
                        loaded
                    }
                    scenario.onActivity { activity ->
                        val input = activity.window.decorView.findViewWithTag<EditText>(InstalledAppsActivity.TAG_SEARCH)
                        assertTrue(input.isShown)
                        assertTrue("The search field rejected its accessibility text-input event",
                            input.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, FixtureInstallUi.PACKAGE_NAME)
                            }))
                    }
                    await("Searching by the fixed package did not isolate the fixture", 15_000) {
                        var isolated = false
                        scenario.onActivity { activity ->
                            val apps = listed(activity)
                            isolated = apps.size == 1 && apps.single().packageName == FixtureInstallUi.PACKAGE_NAME
                            if (isolated) activity.window.decorView.findViewWithTag<ListView>(InstalledAppsActivity.TAG_LIST).setSelection(1)
                        }
                        isolated
                    }
                    await("The fixture row could not be expanded", 10_000) {
                        var clicked = false
                        scenario.onActivity { activity ->
                            val header = activity.window.decorView.findViewWithTag<View>(InstalledAppsActivity.TAG_ROW + FixtureInstallUi.PACKAGE_NAME)
                            if (header != null && fullyVisible(header) && header.isEnabled) {
                                assertTrue(header.contentDescription.toString().contains(FixtureInstallUi.PACKAGE_NAME))
                                assertTrue(header.contentDescription.toString().contains(FixtureInstallUi.LABEL))
                                clicked = header.performClick()
                            }
                        }
                        clicked
                    }
                    assertTrue("Expanding a row must not uninstall the app", fixtureInstalled())
                    await("The fixture row's uninstall action was not available", 10_000) {
                        var clicked = false
                        scenario.onActivity { activity ->
                            val header = activity.window.decorView.findViewWithTag<View>(InstalledAppsActivity.TAG_ROW + FixtureInstallUi.PACKAGE_NAME)
                            val row = header?.parent as? ViewGroup
                            if (row != null) {
                                val candidates = descendants(row).filterIsInstance<TextView>().filter {
                                    it.text.toString() == activity.getString(R.string.action_uninstall) && it.isClickable && it.isShown
                                }
                                check(candidates.size <= 1) { "Ambiguous uninstall controls in the owned fixture row" }
                                candidates.singleOrNull()?.takeIf { it.isEnabled }?.let { button ->
                                    if (fullyVisible(button)) clicked = button.performClick()
                                    else button.requestRectangleOnScreen(Rect(0, 0, button.width, button.height), true)
                                }
                            }
                        }
                        clicked
                    }
                    await("The real privileged confirmation for the fixed fixture did not appear", 15_000) {
                        var visible = false
                        instrumentation.runOnMainSync {
                            ownedConfirmations().singleOrNull()?.let { activity ->
                                val decor = activity.window.decorView
                                assertTrue(containsText(decor, activity.getString(R.string.uninstall_authorization, "Shizuku")))
                                val keepData = requireNotNull(decor.findViewWithTag<MaterialSwitch>(ConfirmationActivity.TAG_KEEP_DATA))
                                assertTrue(keepData.isEnabled)
                                assertFalse("The fixture must be removed without keeping data", keepData.isChecked)
                                visible = true
                            }
                        }
                        visible
                    }
                    assertTrue("The selected app disappeared before the user confirmation", fixtureInstalled())
                    assertTrue("The owned confirmation did not accept its actual button event",
                        FixtureInstallUi.clickFixtureUninstall(FixtureInstallUi.PACKAGE_NAME))
                    await("The confirmed fixture was not removed by the application's uninstall engine", 30_000) { !fixtureInstalled() }
                    await("The browser did not report authoritative uninstall success and refresh its rows", 15_000) {
                        var finished = false
                        scenario.onActivity { activity ->
                            val status = activity.window.decorView.findViewWithTag<TextView>(InstalledAppsActivity.TAG_OPERATION)
                            finished = status.text.toString() == activity.getString(R.string.apps_uninstall_success, FixtureInstallUi.LABEL) &&
                                listed(activity).none { it.packageName == FixtureInstallUi.PACKAGE_NAME }
                        }
                        finished
                    }
                    assertFixture(source)
                    evidence("SUCCESS package=${FixtureInstallUi.PACKAGE_NAME} authorizer=shizuku explicitRow=true pluginConfirmation=true removed=true sourcePreserved=true")
                } catch (failure: Throwable) {
                    primaryFailure = failure
                    throw failure
                } finally {
                    var cleanupFailure: Throwable? = null
                    fun cleanup(action: () -> Unit) {
                        try { action() } catch (failure: Throwable) {
                            val primary = primaryFailure
                            if (primary != null) primary.addSuppressed(failure)
                            else if (cleanupFailure == null) cleanupFailure = failure
                            else cleanupFailure!!.addSuppressed(failure)
                        }
                    }
                    cleanup { browser?.close() }
                    cleanup { instrumentation.runOnMainSync { ownedConfirmations().forEach { it.finish() } } }
                    cleanup { record?.close() }
                    cleanup { trace.settleOwnedFixtureSessions() }
                    cleanup { record?.let(::removeOwnedHistory) }
                    cleanup { check(folder.deleteRecursively() || !folder.exists()) { "The owned fixture source remains" } }
                    if (primaryFailure == null) cleanupFailure?.let { throw it }
                }
            }
        }
        evidence("CLEANUP fixtureAbsent=true ownedHistoryRemoved=true sourceRemoved=true")
    }

    @Suppress("DEPRECATION")
    private fun assertFixture(source: File) {
        val digest = MessageDigest.getInstance("SHA-256").digest(source.readBytes()).joinToString("") { "%02x".format(it.toInt() and 0xff) }
        assertEquals("Only the pinned repository fixture is authorized", FIXTURE_SHA256, digest)
        val archive = requireNotNull(context.packageManager.getPackageArchiveInfo(source.absolutePath, 0))
        assertEquals(FixtureInstallUi.PACKAGE_NAME, archive.packageName)
        assertEquals(1L, if (Build.VERSION.SDK_INT >= 28) archive.longVersionCode else archive.versionCode.toLong())
        val info = requireNotNull(archive.applicationInfo).apply { sourceDir = source.absolutePath; publicSourceDir = source.absolutePath }
        assertEquals(0, info.flags and ApplicationInfo.FLAG_HAS_CODE)
        assertEquals(FixtureInstallUi.LABEL, info.loadLabel(context.packageManager).toString())
        ZipFile(source).use { zip ->
            assertFalse("The fixture must not contain executable app code", zip.entries().asSequence().any {
                it.name.endsWith(".dex", ignoreCase = true) || it.name.startsWith("lib/") && it.name.endsWith(".so", ignoreCase = true)
            })
        }
    }

    @Suppress("DEPRECATION")
    private fun fixtureInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(FixtureInstallUi.PACKAGE_NAME, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) { false }

    private fun listed(activity: InstalledAppsActivity): List<InstalledApp> {
        val adapter = activity.window.decorView.findViewWithTag<ListView>(InstalledAppsActivity.TAG_LIST).adapter
        return (0 until adapter.count).mapNotNull { adapter.getItem(it) as? InstalledApp }
    }

    private fun descendants(root: View): List<View> = buildList {
        add(root)
        if (root is ViewGroup) for (index in 0 until root.childCount) addAll(descendants(root.getChildAt(index)))
    }

    private fun containsText(root: View, value: String) = descendants(root).filterIsInstance<TextView>().any { it.text.toString() == value }

    private fun fullyVisible(view: View): Boolean {
        val bounds = Rect()
        return view.isShown && view.getGlobalVisibleRect(bounds) && bounds.width() >= view.width && bounds.height() >= view.height
    }

    private fun ownedConfirmations(): List<ConfirmationActivity> = ActivityLifecycleMonitorRegistry.getInstance()
        .getActivitiesInStage(Stage.RESUMED).filterIsInstance<ConfirmationActivity>().filter { activity ->
            containsText(activity.window.decorView, FixtureInstallUi.PACKAGE_NAME) && containsText(activity.window.decorView, FixtureInstallUi.LABEL)
        }

    private fun removeOwnedHistory(record: InstallPresentation.Record) {
        val store = InstallHistoryStore.get(context)
        val ids = record.request.items.indices.map { InstallHistoryEntry.id(record.token, it) }
        val completed = CountDownLatch(ids.size)
        val successful = AtomicBoolean(true)
        ids.forEach { id -> store.remove(id) { written -> if (!written) successful.set(false); completed.countDown() } }
        check(completed.await(5, TimeUnit.SECONDS)) { "Owned installation history deletion timed out" }
        check(successful.get() && store.list().none { it.token == record.token }) { "Owned installation history was not removed durably" }
    }

    private fun await(message: String, timeoutMillis: Long, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return
            SystemClock.sleep(40)
        }
        fail(message)
    }

    private fun evidence(message: String) = instrumentation.sendStatus(0, Bundle().apply { putString("installed-apps-uninstall", message) })

    private companion object {
        const val FIXTURE_SHA256 = "fec583a389978fdc298e9d85979b95feceb93e6fb3fd3f581511c826401e8b69"
    }
}
