package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.ExternalSources
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.ExternalInstallActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.ExternalInstaller
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallDialogActivity
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Real external-source/controller/UI integration. Package mutations require explicit runner opt-in. */
@RunWith(AndroidJUnit4::class)
class ExternalInstallDeviceTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @Test fun externalDocumentsAcceptViewAndSendMultipleButRejectUnsafeSchemesAndOversizedBatches() {
        val directory = directory()
        try {
            val uri = Uri.fromFile(File(directory, "owned.apk"))
            val view = ExternalSources.fromIntent(Intent(Intent.ACTION_VIEW, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
            assertEquals(listOf(uri), view.uris)
            assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, view.grantIntent.flags)
            assertEquals(uri, view.grantIntent.clipData!!.getItemAt(0).uri)
            val uris = ArrayList((0 until InstallerContract.MAX_BATCH_SOURCES).map { Uri.fromFile(File(directory, "item-$it.apk")) })
            val batch = ExternalSources.fromIntent(Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris))
            assertEquals(32, batch.uris.size)
            assertEquals(32, batch.grantIntent.clipData!!.itemCount)
            uris += uri
            invalid { ExternalSources.fromIntent(Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)) }
            invalid { ExternalSources.fromIntent(Intent(Intent.ACTION_SEND_MULTIPLE)) }
            invalid { ExternalSources.fromIntent(Intent("test.UNKNOWN", uri)) }
            for (scheme in listOf("https://example.invalid/fixture.apk", "javascript:fixture.apk", "data:application/vnd.android.package-archive;base64,AA==")) {
                invalid { ExternalSources.fromIntent(Intent(Intent.ACTION_VIEW, Uri.parse(scheme))) }
            }
        } finally { directory.deleteRecursively() }
    }

    @Test fun fileAndContentSourcesAreOpenedReadOnly() {
        val directory = providerDirectory()
        try {
            val file = copyFixture(directory)
            val before = file.readBytes()
            for (uri in listOf(Uri.fromFile(file), contentUri(directory))) {
                val sources = ExternalSources.fromIntent(Intent(Intent.ACTION_VIEW, uri))
                sources.open(context, {}).use { opened ->
                    assertEquals(1, opened.descriptors.size)
                    val mode = Os.fcntlInt(opened.descriptors.single().fileDescriptor, OsConstants.F_GETFL, 0)
                    assertEquals(OsConstants.O_RDONLY, mode and OsConstants.O_ACCMODE)
                    assertEquals(file.length(), opened.entries.single().size)
                }
                assertArrayEquals(before, file.readBytes())
            }
        } finally { directory.deleteRecursively() }
    }

    @Test fun externalViewActivityShowsTheOwnedSourceAndCancellationNeverAllocatesAnInstallSession() {
        unlocked()
        FixturePackageOwnership(setOf(FIXTURE)).use {
            val directory = providerDirectory()
            var record: InstallPresentation.Record? = null
            val monitor = instrumentation.addMonitor(InstallDialogActivity::class.java.name, null, false)
            val before = context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet()
            try {
                val source = copyFixture(directory)
                val uri = contentUri(directory)
                context.startActivity(Intent(Intent.ACTION_VIEW, uri, context, ExternalInstallActivity::class.java)
                    .setDataAndType(uri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                val activity = monitor.waitForActivityWithTimeout(15_000) as? InstallDialogActivity
                assertNotNull("External VIEW did not open the installation interface", activity)
                val token = requireNotNull(activity).intent.getStringExtra(InstallPresentation.EXTRA_TOKEN)
                record = requireNotNull(token?.let(InstallPresentation::find))
                assertEquals(uri, activity.intent.clipData!!.getItemAt(0).uri)
                waitUntil { requireNotNull(record).snapshot().prompt != null || requireNotNull(record).snapshot().terminal }
                assertFalse("Existing installation defaults prevented confirmation", requireNotNull(record).snapshot().terminal)
                assertEquals(FIXTURE, requireNotNull(record).snapshot().prompt!!.metadata.packageName)
                waitUntil { FixtureInstallUi.clickInstall(InstallDialogActivity.TAG_CANCEL, token = token, packageName = FIXTURE) }
                waitUntil { requireNotNull(record).snapshot().terminal }
                assertEquals(InstallerErrorCodes.USER_CANCELLED, requireNotNull(record).snapshot().failure!!.code)
                assertTrue(source.isFile)
                assertEquals(before, context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet())
            } finally { record?.close(); instrumentation.removeMonitor(monitor); directory.deleteRecursively() }
        }
    }

    @Test fun noneViewConfirmationInstallsAndDeletesOnlyTheOwnedPrivateFile() = installingFixture { directory, owner ->
        val file = copyFixture(directory)
        val sibling = File(directory, "keep.txt").apply { writeText("Not an installation source") }
        val sources = ExternalSources.fromIntent(Intent(Intent.ACTION_VIEW, Uri.fromFile(file)))
        val record = start(sources, deleteSource = true)
        try {
            owner.installationStarted()
            drive(record)
            val result = requireNotNull(record.snapshot().items.single().result)
            assertTrue(result.toString(), result[InstallerContract.FIELD_OK].asBoolean)
            assertTrue(result[InstallerContract.FIELD_SOURCE_DELETED].asBoolean)
            assertFalse(file.exists())
            assertEquals("Not an installation source", sibling.readText())
            assertEquals(1L, installedVersion())
            waitUntil { FixtureInstallUi.hasInstallView(record.token, InstallDialogActivity.TAG_DONE) }
        } finally { record.close() }
    }

    @Test fun noneContentInstallationStaysSuccessfulWhenTheProviderRefusesSourceDeletion() {
        installOptIn()
        FixturePackageOwnership(setOf(FIXTURE)).use { owner ->
            val directory = providerDirectory()
            val file = copyFixture(directory)
            val before = file.readBytes()
            val record = start(ExternalSources.fromIntent(Intent(Intent.ACTION_VIEW, contentUri(directory))), deleteSource = true)
            try {
                owner.installationStarted()
                drive(record)
                val result = requireNotNull(record.snapshot().items.single().result)
                assertTrue(result.toString(), result[InstallerContract.FIELD_OK].asBoolean)
                assertFalse(result[InstallerContract.FIELD_SOURCE_DELETED].asBoolean)
                assertTrue(result.getAsJsonArray(InstallerContract.FIELD_NOTES).size() > 0)
                assertArrayEquals(before, file.readBytes())
                assertEquals(1L, installedVersion())
                waitUntil { FixtureInstallUi.hasInstallView(record.token, InstallDialogActivity.TAG_SOURCE_NOT_DELETED) }
            } finally { record.close(); directory.deleteRecursively() }
        }
    }

    @Test fun sendMultipleRetainsTheFirstFailureAndRetryUpdatesOnlyThatSource() = installingFixture { directory, owner ->
        val repairable = File(directory, "retry-${UUID.randomUUID()}.apk").apply { writeText("Malformed installation source") }
        val good = copyFixture(directory)
        val sources = ExternalSources.fromIntent(Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM,
            arrayListOf(Uri.fromFile(repairable), Uri.fromFile(good))))
        val record = start(sources)
        var retried: InstallPresentation.Record? = null
        try {
            owner.installationStarted()
            drive(record)
            val initial = record.snapshot().items.map { requireNotNull(it.result).deepCopy() }
            assertEquals(listOf(false, true), initial.map { it[InstallerContract.FIELD_OK].asBoolean })
            assertEquals(1L, installedVersion())
            copyAsset("fixture-v2.apk", repairable)
            waitUntil { FixtureInstallUi.clickInstall("install_retry_0", token = record.token) }
            waitUntil {
                retried = FixtureInstallUi.resumedRecord { candidate ->
                    candidate.token != record.token && candidate.request.sources.singleOrNull()?.displayName == repairable.name
                }
                retried != null
            }
            val retry = requireNotNull(retried)
            assertEquals(1, retry.request.items.size)
            drive(retry)
            assertTrue(requireNotNull(retry.snapshot().items.single().result)[InstallerContract.FIELD_OK].asBoolean)
            assertEquals(2L, installedVersion())
            assertEquals(initial, record.snapshot().items.map { it.result })
            assertTrue(good.exists())
        } finally { retried?.close(); record.close() }
    }

    @Test fun cancellingTheBatchConfirmationCancelsTheRemainingPackagesWithoutWriting() {
        unlocked()
        FixturePackageOwnership(setOf(FIXTURE)).use {
            val directory = directory()
            var record: InstallPresentation.Record? = null
            val before = context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet()
            val monitor = instrumentation.addMonitor(InstallDialogActivity::class.java.name, null, false)
            try {
                val first = copyFixture(directory)
                val second = File(directory, "second.apk").also { copyAsset("fixture-v2.apk", it) }
                val uris = arrayListOf(Uri.fromFile(first), Uri.fromFile(second))
                context.startActivity(Intent(context, ExternalInstallActivity::class.java).setAction(Intent.ACTION_SEND_MULTIPLE)
                    .setType("application/vnd.android.package-archive").putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                val activity = monitor.waitForActivityWithTimeout(15_000) as? InstallDialogActivity
                assertNotNull("External SEND_MULTIPLE did not open the batch interface", activity)
                val token = requireNotNull(activity).intent.getStringExtra(InstallPresentation.EXTRA_TOKEN)
                val current = requireNotNull(token?.let(InstallPresentation::find))
                record = current
                assertEquals(2, current.request.items.size)
                assertEquals(uris, (0 until activity.intent.clipData!!.itemCount).map { activity.intent.clipData!!.getItemAt(it).uri })
                waitUntil { FixtureInstallUi.clickInstall(InstallDialogActivity.TAG_CANCEL, token = current.token, packageName = FIXTURE) }
                waitUntil { current.snapshot().terminal }
                assertTrue(current.snapshot().items.none { it.result?.get(InstallerContract.FIELD_OK)?.asBoolean == true })
                assertEquals(before, context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet())
                assertNull(installedVersion())
                assertTrue(first.isFile && second.isFile)
            } finally { record?.close(); instrumentation.removeMonitor(monitor); directory.deleteRecursively() }
        }
    }

    @Test fun blockedProviderQueriesAndOpensReachTheSessionTimeoutWithoutUserCancellation() {
        unlocked()
        for (phase in listOf("query", "open")) {
            val directory = providerDirectory()
            var record: InstallPresentation.Record? = null
            val before = context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet()
            try {
                copyFixture(directory)
                val uri = contentUri(directory).buildUpon().appendQueryParameter("waitForCancel", phase).build()
                val startedAt = SystemClock.elapsedRealtime()
                val current = startWithOptions(ExternalSources.fromIntent(Intent(Intent.ACTION_VIEW, uri)), InstallOptions(
                    authorizer = InstallerContract.AUTHORIZER_NONE, timeoutMillis = 5_000))
                record = current
                waitUntil(4_000, { "Provider $phase was not entered: ${sourceState(current)}" }) { providerWaiting(uri) }
                waitUntil(8_000, { "Provider $phase did not time out: ${sourceState(current)}" }) { current.snapshot().terminal }
                assertEquals(InstallerErrorCodes.TIMEOUT, current.snapshot().failure?.code)
                assertTrue("Provider cancellation was not delivered near the deadline", SystemClock.elapsedRealtime() - startedAt < 10_000)
                waitUntil { !providerWaiting(uri) }
                assertEquals(before, context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet())
            } finally { record?.close(); directory.deleteRecursively() }
        }
    }

    @Test fun cancellingABlockedProviderDoesNotCancelLaterPooledRequests() {
        unlocked()
        val directory = providerDirectory()
        var record: InstallPresentation.Record? = null
        try {
            copyFixture(directory)
            val uri = contentUri(directory).buildUpon().appendQueryParameter("waitForCancel", "open").build()
            val current = start(ExternalSources.fromIntent(Intent(Intent.ACTION_VIEW, uri)))
            record = current
            waitUntil(failureMessage = { "Provider open was not entered: ${sourceState(current)}" }) { providerWaiting(uri) }
            val cancelledAt = SystemClock.elapsedRealtime()
            current.cancel()
            waitUntil(5_000, { "Provider cancellation did not settle: ${sourceState(current)}" }) { current.snapshot().terminal }
            assertEquals(InstallerErrorCodes.CANCELLED, current.snapshot().failure?.code)
            assertTrue(SystemClock.elapsedRealtime() - cancelledAt < 5_000)
            waitUntil { !providerWaiting(uri) }
            current.close()
            // Cycle through the bounded pool, including the worker used by the cancelled call.
            repeat(InstallerContract.MAX_CONCURRENT_SESSIONS) { index ->
                val malformed = File(directory, "after-cancel-$index.apk").apply { writeText("Malformed test package") }
                val next = start(ExternalSources.fromIntent(Intent(Intent.ACTION_VIEW, Uri.fromFile(malformed))))
                try {
                    waitUntil { next.snapshot().terminal }
                    assertEquals(InstallerErrorCodes.INVALID_PACKAGE, next.snapshot().failure?.code)
                } finally { next.close() }
            }
        } finally { record?.close(); directory.deleteRecursively() }
    }

    @Test fun anUnreadableBatchItemKeepsItsIndexAndOnlyThatSourceIsRetried() {
        unlocked()
        FixturePackageOwnership(setOf(FIXTURE)).use {
            val directory = directory()
            var record: InstallPresentation.Record? = null
            var retry: InstallPresentation.Record? = null
            val before = context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet()
            try {
                val first = File(directory, "first.apk").apply { writeText("First malformed package") }
                val missing = File(directory, "missing-${UUID.randomUUID()}.apk")
                val last = File(directory, "last.apk").apply { writeText("Last malformed package") }
                val intent = Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM,
                    arrayListOf(Uri.fromFile(first), Uri.fromFile(missing), Uri.fromFile(last)))
                val current = start(ExternalSources.fromIntent(intent))
                record = current
                waitUntil { current.snapshot().terminal }
                val items = current.snapshot().items
                assertEquals(3, items.size)
                assertEquals(listOf(InstallerErrorCodes.INVALID_PACKAGE, InstallerErrorCodes.SOURCE_UNREADABLE, InstallerErrorCodes.INVALID_PACKAGE),
                    items.map { it.result!!.getAsJsonObject(InstallerContract.FIELD_ERROR)[InstallerContract.FIELD_ERROR_CODE].asString })
                // INVALID_PACKAGE on both readable sources proves each was parsed. Terminal
                // records deliberately release metadata/icons, so it is not a completion signal.
                val original = items.map { it.result!!.deepCopy() }
                copyAsset("fixture-v1.apk", missing)
                waitUntil { FixtureInstallUi.clickInstall("install_retry_1", token = current.token) }
                waitUntil {
                    retry = FixtureInstallUi.resumedRecord { candidate ->
                        candidate.token != current.token && candidate.request.sources.singleOrNull()?.displayName == missing.name
                    }
                    retry != null
                }
                val retried = requireNotNull(retry)
                waitUntil { FixtureInstallUi.clickInstall(InstallDialogActivity.TAG_CANCEL, token = retried.token, packageName = FIXTURE) }
                waitUntil { retried.snapshot().terminal }
                assertEquals(InstallerErrorCodes.USER_CANCELLED, retried.snapshot().failure?.code)
                assertEquals(original, current.snapshot().items.map { it.result })
                assertEquals(before, context.packageManager.packageInstaller.mySessions.map { it.sessionId }.toSet())
            } finally { retry?.close(); record?.close(); directory.deleteRecursively() }
        }
    }

    @Test fun unreadableSourceHonorsStopOnErrorWithoutOpeningTheNextItem() {
        unlocked()
        val directory = directory()
        var record: InstallPresentation.Record? = null
        try {
            val missing = File(directory, "missing.apk")
            val second = File(directory, "second.apk").apply { writeText("Must not be inspected") }
            val sources = ExternalSources.fromIntent(Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM,
                arrayListOf(Uri.fromFile(missing), Uri.fromFile(second))))
            val current = startWithOptions(sources, InstallOptions(authorizer = InstallerContract.AUTHORIZER_NONE, continueOnError = false))
            record = current
            waitUntil { current.snapshot().terminal }
            assertEquals(InstallerErrorCodes.SOURCE_UNREADABLE, current.snapshot().failure?.code)
            assertEquals(InstallerErrorCodes.SOURCE_UNREADABLE,
                current.snapshot().items.first().result!!.getAsJsonObject(InstallerContract.FIELD_ERROR)[InstallerContract.FIELD_ERROR_CODE].asString)
            assertNull(current.snapshot().items[1].metadata)
            assertNull(current.snapshot().items[1].result)
            assertEquals(InstallerContract.STAGE_CANCELLED, current.snapshot().items[1].stage)
        } finally { record?.close(); directory.deleteRecursively() }
    }

    private fun start(sources: ExternalSources, deleteSource: Boolean = false): InstallPresentation.Record =
        startWithOptions(sources, InstallOptions(authorizer = InstallerContract.AUTHORIZER_NONE, deleteSource = deleteSource, timeoutMillis = 90_000))

    private fun startWithOptions(sources: ExternalSources, options: InstallOptions): InstallPresentation.Record =
        requireNotNull(InstallPresentation.find(ExternalInstaller.start(context, sources, options)))

    private fun drive(record: InstallPresentation.Record) {
        var clickedConfirmation = false
        val deadline = SystemClock.elapsedRealtime() + 100_000
        while (!record.snapshot().terminal && SystemClock.elapsedRealtime() < deadline) {
            if (FixtureInstallUi.clickInstall(InstallDialogActivity.TAG_CONFIRM, token = record.token, packageName = FIXTURE)) clickedConfirmation = true
            if (clickedConfirmation) FixtureInstallUi.acceptSystemFixture(FIXTURE_LABEL)
            SystemClock.sleep(50)
        }
        assertTrue("Fixture confirmation was never displayed", clickedConfirmation)
        assertTrue("Installation did not finish: ${record.snapshot()}", record.snapshot().terminal)
    }

    private fun installingFixture(block: (File, FixturePackageOwnership) -> Unit) {
        installOptIn()
        FixturePackageOwnership(setOf(FIXTURE)).use { owner ->
            val directory = directory()
            try { block(directory, owner) } finally { directory.deleteRecursively() }
        }
    }

    private fun installOptIn() {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue("Opt in with engineAuthorizer=none and confirmFixture=true", arguments.getString("engineAuthorizer") == "none" && arguments.getString("confirmFixture") == "true")
        unlocked()
        if (Build.VERSION.SDK_INT >= 26) assumeTrue("Grant unknown-source installation manually before this test", context.packageManager.canRequestPackageInstalls())
    }
    private fun unlocked() = assumeFalse("Unlock the device before UI tests", context.getSystemService(KeyguardManager::class.java).isKeyguardLocked)
    private fun directory() = File(context.cacheDir, "p3-external-fixtures-${UUID.randomUUID()}").apply { check(mkdir()) }
    private fun providerDirectory() = File(context.cacheDir, "p2-source-fixtures-${UUID.randomUUID()}").apply { check(mkdir()) }
    private fun contentUri(directory: File) = Uri.parse("content://${context.packageName}.source-fixtures/${directory.name}/fixture.apk")
    private fun copyFixture(directory: File) = File(directory, "fixture.apk").also { copyAsset("fixture-v1.apk", it) }
    private fun copyAsset(name: String, file: File) = instrumentation.context.assets.open(name).use { input -> file.outputStream().use(input::copyTo); Unit }
    @Suppress("DEPRECATION") private fun installedVersion(): Long? = try {
        context.packageManager.getPackageInfo(FIXTURE, 0).let { if (Build.VERSION.SDK_INT >= 28) it.longVersionCode else it.versionCode.toLong() }
    } catch (_: PackageManager.NameNotFoundException) { null }
    private fun invalid(operation: () -> Unit) = assertEquals(InstallerErrorCodes.INVALID_ARGUMENT, assertThrows(InstallFailure::class.java) { operation() }.code)
    private fun providerWaiting(uri: Uri): Boolean =
        context.contentResolver.call(uri, "fixtureCancellationState", uri.toString(), null)?.getBoolean("waiting") == true
    private fun sourceState(record: InstallPresentation.Record): String = record.snapshot().let {
        "stage=${it.stage}, index=${it.index}, terminal=${it.terminal}, failure=${it.failure?.toJson()}, items=${it.items.map { item -> item.result }}"
    }
    private fun waitUntil(timeoutMillis: Long = 15_000, failureMessage: () -> String = { "External installation UI did not reach the expected state" }, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return
            SystemClock.sleep(25)
        }
        fail(failureMessage())
    }

    companion object {
        internal const val FIXTURE = FixtureInstallUi.PACKAGE_NAME
        internal const val FIXTURE_LABEL = FixtureInstallUi.LABEL
    }
}
