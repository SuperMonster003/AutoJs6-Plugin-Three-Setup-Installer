package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallDialogActivity
import org.autojs.plugin.installer.api.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
import java.io.Closeable
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class InstallerBinderDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val endpoint = "${context.packageName}.spike.InstallerBinderTestService"

    @Test fun realOperationsThroughTheMainProcessRouter() {
        val selected = io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer.fromId(InstrumentationRegistry.getArguments().getString("engineAuthorizer"))
        assumeTrue("Opt in to fixture operations with engineAuthorizer", selected != null)
        val authorizer = requireNotNull(selected)
        fun installed(includeData: Boolean = false): Boolean = try {
            context.packageManager.getPackageInfo(FIXTURE, if (includeData) android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES else 0); true
        } catch (_: android.content.pm.PackageManager.NameNotFoundException) { false }
        check(!installed(true)) { "Fixture or retained data already exists; refusing to modify it" }
        val ownership = FixturePackageOwnership(setOf(FIXTURE))
        val router = localRouter()
        val files = mutableListOf<File>()
        val descriptors = mutableListOf<ParcelFileDescriptor>()
        var handle: IInstallerSession? = null
        val stopUi = java.util.concurrent.atomic.AtomicBoolean()
        val presentation = java.util.concurrent.atomic.AtomicReference<io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation.Record>()
        var uiDriver: Thread? = null
        var operationReply: SessionReply? = null
        try {
            val grant = Reply()
            router.requestAuthorizer(authorizer.id, grant)
            assertTrue(grant.result()["granted"].asBoolean)
            val interaction = InstrumentationRegistry.getArguments().getString("binderInteraction") ?: "auto"
            val id = "real-${UUID.randomUUID()}"
            if ((interaction == "dialog" || !authorizer.privileged) && InstrumentationRegistry.getArguments().getString("confirmFixture") == "true") {
                // Explicitly opted-in instrumentation driver, restricted to the fresh test fixture.
                // Use the runner's accessibility connection: an external uiautomator can steal it.
                uiDriver = Thread {
                    while (!stopUi.get()) {
                        FixtureInstallUi.resumedRecord { it.request.id == id }?.let { presentation.compareAndSet(null, it) }
                        FixtureInstallUi.clickInstall(InstallDialogActivity.TAG_CONFIRM, sessionId = id, packageName = FIXTURE)
                        FixtureInstallUi.clickFixtureUninstall(FIXTURE)
                        FixtureInstallUi.acceptSystemFixture(FixtureInstallUi.LABEL)
                        SystemClock.sleep(100)
                    }
                }.apply { start() }
            }
            val sources = listOf(1, 0, 2).mapIndexed { i, version ->
                val file = File(context.cacheDir, "route-fixture-$i-${UUID.randomUUID()}.apk").also { files += it }
                if (version == 0) file.writeText("malformed package")
                else instrumentation.context.assets.open("fixture-v$version.apk").use { input -> file.outputStream().use(input::copyTo) }
                descriptors += ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                """{"item":$i,"displayName":"fixture.apk","size":${file.length()}}"""
            }
            val reply = SessionReply()
            operationReply = reply
            ownership.installationStarted()
            handle = router.openSession(descriptors.toTypedArray(), request("""{"id":"$id","sources":[${sources.joinToString()}],"interaction":"$interaction","options":{"authorizer":"${authorizer.id}","timeoutMillis":120000}}""", id), reply)
            assertNotNull(handle)
            val results = reply.result().getAsJsonArray("results").map { it.asJsonObject }
            assertEquals(listOf(true, false, true), results.map { it["ok"].asBoolean })
            assertEquals(1, results.last()["previousVersionCode"].asInt)
            assertEquals(2, results.last()["versionCode"].asInt)
            if (interaction == "dialog" || !authorizer.privileged) assertEquals("dialog", results.last()["interaction"].asString)
            assertTrue(descriptors.all { it.fileDescriptor.valid() })
            val inspected = Reply()
            ParcelFileDescriptor.open(files.first(), ParcelFileDescriptor.MODE_READ_ONLY).use { source ->
                router.inspect(source, request("""{"displayName":"fixture.apk","size":${files.first().length()}}"""), inspected)
                val info = inspected.result().getAsJsonObject("installed")
                assertEquals(2, info["versionCode"].asInt)
                assertEquals("match", info["signerMatch"].asString)
            }
            val removal = Reply()
            router.uninstall(request("""{"packageName":"$FIXTURE","authorizer":"${authorizer.id}","interaction":"$interaction","timeoutMillis":120000}"""), removal)
            assertTrue(removal.result(150)["ok"].asBoolean)
            assertFalse(installed())
        } finally {
            stopUi.set(true)
            uiDriver?.join(2_000)
            handle?.close()
            router.close()
            operationReply?.let { assertTrue("Installation worker did not release its sources", it.done.await(10, TimeUnit.SECONDS)) }
            descriptors.forEach { it.close() }
            files.forEach { it.delete() }
            presentation.get()?.close()
            ownership.close()
            val client = io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.PrivilegedClient.get(context)
            val binder = if (authorizer.privileged) runCatching { client.acquire(authorizer).asBinder() }.getOrNull() else null
            val stopped = CountDownLatch(1)
            binder?.let { runCatching { it.linkToDeath({ stopped.countDown() }, 0) }.onFailure { stopped.countDown() } }
            client.releaseAll()
            if (binder != null) assertTrue(stopped.await(10, TimeUnit.SECONDS))
        }
    }

    @Test fun closedDescriptorIsRejectedAtTheLocalBinderBoundary() {
        localRouter().use { router ->
            val pipe = ParcelFileDescriptor.createPipe()
            pipe[0].close()
            try {
                val reply = Reply()
                router.inspect(pipe[0], request("{}"), reply)
                assertEquals("INVALID_ARGUMENT", reply.error()["code"].asString)
            } finally { pipe[1].close() }
        }
    }

    private fun localRouter() = io.github.supermonster003.autojs6.plugin.three.setup.installer.binder.InstallerBinder(context,
        object : io.github.supermonster003.autojs6.plugin.three.setup.installer.binder.CallerGuard {
            override fun enforceHost(): Int = android.os.Process.myUid()
        })

    @Test fun productionRouterExposesCapabilitiesButRejectsPluginUidAsHost() = bind(ThreeSetupInstallerPluginService::class.java.name).use { service ->
        val installer = IInstallerPlugin.Stub.asInterface(service.binder)
        assertEquals(ThreeSetupInstallerPlugin.ID, installer.info.id)
        assertEquals(32, installer.capabilities.getInt(InstallerCapabilityKeys.MAX_BATCH))
        val callback = Reply()
        assertThrows(SecurityException::class.java) { installer.getUsers(request("{}"), callback) }
        assertEquals(0, callback.count.get())
    }

    @Test fun versionTwoIsNegotiatedWithoutBreakingTheVersionOneEnvelope() = bind(endpoint).use { service ->
        val installer = IInstallerPlugin.Stub.asInterface(service.binder)
        val capabilities = installer.capabilities
        assertEquals(1, capabilities.getInt(InstallerCapabilityKeys.CONTRACT_VERSION))
        assertEquals(2, capabilities.getInt(InstallerCapabilityKeys.MAX_CONTRACT_VERSION))
        fun envelope(json: String, version: Int) = request(json).apply { putInt(InstallerContract.KEY_CONTRACT_VERSION, version) }
        for (version in listOf(1, 2)) {
            val reply = Reply()
            installer.getUsers(envelope("""{"authorizer":"none"}""", version), reply)
            assertEquals(1, reply.result().getAsJsonArray("users").size())
            assertEquals(1, reply.result!!.getInt(InstallerContract.KEY_CONTRACT_VERSION))
            assertEquals(1, reply.count.get())
        }
        val legacy = Reply()
        installer.getUsers(envelope("""{"authorizer":"dhizuku"}""", 1), legacy)
        assertEquals("INVALID_ARGUMENT", legacy.error()["code"].asString)
        assertEquals(1, legacy.count.get())
        for (version in listOf(1, 2)) {
            val reply = Reply()
            installer.setDefaultInstallerV2(true, envelope("""{"authorizer":"none","mode":"persistent"}""", version), reply)
            assertEquals("INVALID_ARGUMENT", reply.error()["code"].asString)
            assertEquals(1, reply.count.get())
        }
    }

    @Test fun inspectAndUsersRoundTripThroughTheRemoteRouter() = bind(endpoint).use { service ->
        assertNull(service.binder.queryLocalInterface(IInstallerPlugin.DESCRIPTOR))
        val installer = IInstallerPlugin.Stub.asInterface(service.binder)
        val file = fixture()
        try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { source ->
                val reply = Reply()
                installer.inspect(source, request("""{"displayName":"fixture.apk","size":${file.length()}}"""), reply)
                val document = reply.result()
                assertEquals(FIXTURE, document["packageName"].asString)
                assertEquals(1, document["versionCode"].asInt)
                assertEquals("apk", document["format"].asString)
                assertTrue(document.getAsJsonArray("signatureSchemes").size() > 0)
                assertTrue(document["icon"].asString.isNotEmpty())
                assertTrue(document.toString().toByteArray().size <= InstallerContract.MAX_JSON_BYTES)
                assertTrue(source.fileDescriptor.valid())
            }
            val users = Reply()
            installer.getUsers(request("""{"authorizer":"none"}"""), users)
            assertEquals(1, users.result().getAsJsonArray("users").size())
            val state = json(installer.getDefaultInstallerState(), InstallerContract.KEY_STATUS_JSON)
            assertTrue(state.has("isSelf"))
            val none = json(installer.getAuthorizerState("none"), InstallerContract.KEY_STATUS_JSON)
            assertTrue(none["granted"].asBoolean)
        } finally { file.delete() }
    }

    @Test fun hostileDocumentsAndSourcesAreRefusedExactlyOnce() = bind(endpoint).use { service ->
        val installer = IInstallerPlugin.Stub.asInterface(service.binder)
        val file = fixture()
        try {
            for (bad in listOf("{", "{\"displayName\":true}", "{\"size\":1.5}", "{\"size\":-2}", " ".repeat(65_537))) {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { source ->
                    val reply = Reply()
                    installer.inspect(source, request(bad), reply)
                    assertEquals("INVALID_ARGUMENT", reply.error()["code"].asString)
                    assertEquals(1, reply.count.get())
                }
            }
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE).use { source ->
                val reply = Reply()
                installer.inspect(source, request("{}"), reply)
                assertEquals("INVALID_ARGUMENT", reply.error()["code"].asString)
            }
            val missing = Reply()
            installer.inspect(null, request("{}"), missing)
            assertEquals("INVALID_ARGUMENT", missing.error()["code"].asString)
            val badEnvelope = Reply()
            installer.getUsers(request("{}").apply { putString(InstallerContract.KEY_CONTRACT_VERSION, "1") }, badEnvelope)
            assertEquals("INVALID_ARGUMENT", badEnvelope.error()["code"].asString)
            val unknown = Reply()
            installer.getUsers(request("""{"authorizer":"unknown"}"""), unknown)
            assertEquals("INVALID_ARGUMENT", unknown.error()["code"].asString)
            assertEquals("INVALID_ARGUMENT", json(installer.getAuthorizerState("unknown"), InstallerContract.KEY_ERROR_JSON)["code"].asString)
            assertEquals("INVALID_ARGUMENT", json(installer.getAuthorizerState(null), InstallerContract.KEY_ERROR_JSON)["code"].asString)
            val id = "oversized"
            val callback = SessionReply()
            assertNull(installer.openSession(arrayOfNulls<ParcelFileDescriptor>(2049), request("{}", id), callback))
            assertEquals("INVALID_ARGUMENT", callback.error()["code"].asString)
            assertEquals(id, callback.id)
        } finally { file.delete() }
    }

    @Test fun fourLiveSessionsRejectTheFifthAndCloseReleasesCapacityAfterCleanup() = bind(endpoint).use { service ->
        val installer = IInstallerPlugin.Stub.asInterface(service.binder)
        val pipes = mutableListOf<Array<ParcelFileDescriptor>>()
        val handles = mutableListOf<IInstallerSession>()
        val replies = mutableListOf<SessionReply>()
        try {
            repeat(4) { index ->
                val pipe = ParcelFileDescriptor.createPipe().also { pipes += it }
                val reply = SessionReply()
                replies += reply
                val id = "blocked-$index-${UUID.randomUUID()}"
                handles += requireNotNull(installer.openSession(arrayOf(pipe[0]), installRequest(id), reply))
                assertTrue(reply.preparing.await(5, TimeUnit.SECONDS))
            }
            val fifthPipe = ParcelFileDescriptor.createPipe().also { pipes += it }
            val fifth = SessionReply()
            assertNull(installer.openSession(arrayOf(fifthPipe[0]), installRequest("fifth"), fifth))
            assertEquals("INVALID_ARGUMENT", fifth.error()["code"].asString)
            handles.first().close()
            assertTrue(replies.first().done.await(5, TimeUnit.SECONDS))
            await { json(handles.first().status, InstallerContract.KEY_STATUS_JSON)["state"].asString == "cancelled" }
            val replacement = SessionReply()
            val next = installer.openSession(arrayOf(fifthPipe[0]), installRequest("replacement"), replacement)
            assertNotNull(next)
            handles += requireNotNull(next)
        } finally { handles.forEach { it.close() }; pipes.forEach { pipe -> pipe.forEach { it.close() } } }
    }

    @Test fun clientProcessDeathCancelsAndReclaimsItsSession() = bind(endpoint).use { service ->
        bind("${context.packageName}.spike.InstallerDeathProbeService").use { client ->
            val installer = IInstallerPlugin.Stub.asInterface(service.binder)
            val callback = IInstallerSessionCallback.Stub.asInterface(client.binder)
            val pipe = ParcelFileDescriptor.createPipe()
            var session: IInstallerSession? = null
            try {
                session = installer.openSession(arrayOf(pipe[0]), installRequest("death-${UUID.randomUUID()}"), callback)
                assertNotNull(session)
                await { json(requireNotNull(session).status, InstallerContract.KEY_STATUS_JSON)["state"].asString == "preparing" }
                callback.onStage("test", "die", Bundle())
                await { json(requireNotNull(session).status, InstallerContract.KEY_STATUS_JSON)["state"].asString == "cancelled" }
                assertFalse(requireNotNull(session).cancel())
                assertTrue(pipe[0].fileDescriptor.valid())
            } finally { session?.close(); pipe.forEach { it.close() } }
        }
    }

    private fun installRequest(id: String) = request("""{"id":"$id","sources":[{"displayName":"waiting.apk","size":-1}],"options":{"authorizer":"none","timeoutMillis":30000}}""", id)
    private fun request(document: String, id: String? = null) = Bundle().apply {
        putInt(InstallerContract.KEY_CONTRACT_VERSION, InstallerContract.CONTRACT_VERSION)
        putLong(InstallerContract.KEY_HOST_VERSION_CODE, ThreeSetupInstallerPlugin.REQUIRED_HOST_VERSION)
        putString(InstallerContract.KEY_REQUEST_JSON, document)
        id?.let { putString(InstallerContract.KEY_HOST_SESSION_ID, it) }
    }
    private fun fixture() = File(context.cacheDir, "binder-fixture-${UUID.randomUUID()}.apk").also { file ->
        instrumentation.context.assets.open("fixture-v1.apk").use { input -> file.outputStream().use(input::copyTo) }
    }
    private fun json(bundle: Bundle, key: String) = JsonParser.parseString(requireNotNull(bundle.getString(key))).asJsonObject
    private fun await(condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 8_000
        while (!condition() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(25)
        assertTrue("Condition did not settle", condition())
    }
    private inner class Reply : IInstallerCallback.Stub() {
        val count = AtomicInteger()
        val done = CountDownLatch(1)
        var result: Bundle? = null
        var error: Bundle? = null
        override fun onResult(result: Bundle) { this.result = result; count.incrementAndGet(); done.countDown() }
        override fun onError(error: Bundle) { this.error = error; count.incrementAndGet(); done.countDown() }
        fun result(seconds: Long = 15): JsonObject { assertTrue(done.await(seconds, TimeUnit.SECONDS)); assertNull(error?.toString(), error); return json(requireNotNull(result), InstallerContract.KEY_RESULT_JSON) }
        fun error(): JsonObject { assertTrue(done.await(15, TimeUnit.SECONDS)); assertNull(result); return json(requireNotNull(error), InstallerContract.KEY_ERROR_JSON) }
    }
    private inner class SessionReply : IInstallerSessionCallback.Stub() {
        val preparing = CountDownLatch(1)
        val done = CountDownLatch(1)
        var id: String? = null
        var error: Bundle? = null
        var result: Bundle? = null
        override fun onStage(id: String, stage: String, detail: Bundle?) { if (stage == "preparing") preparing.countDown() }
        override fun onProgress(id: String?, progress: Float, detail: Bundle?) = Unit
        override fun onCompleted(id: String?, result: Bundle?) { this.result = result; done.countDown() }
        override fun onFailed(id: String?, error: Bundle?) { this.id = id; this.error = error; done.countDown() }
        fun error(): JsonObject { assertTrue(done.await(10, TimeUnit.SECONDS)); return json(requireNotNull(error), InstallerContract.KEY_ERROR_JSON) }
        fun result(): JsonObject { assertTrue(done.await(150, TimeUnit.SECONDS)); assertNull(error?.toString(), error); return json(requireNotNull(result), InstallerContract.KEY_RESULT_JSON) }
    }
    private fun bind(className: String): Bound {
        val ready = CountDownLatch(1)
        var binder: IBinder? = null
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) { binder = service; ready.countDown() }
            override fun onServiceDisconnected(name: ComponentName?) = Unit
        }
        assertTrue(context.bindService(Intent().setComponent(ComponentName(context.packageName, className)), connection, Context.BIND_AUTO_CREATE))
        if (!ready.await(10, TimeUnit.SECONDS)) { context.unbindService(connection); error("Service did not bind") }
        return Bound(requireNotNull(binder), connection)
    }
    private inner class Bound(val binder: IBinder, val connection: ServiceConnection) : Closeable {
        override fun close() = context.unbindService(connection)
    }
    companion object { private const val FIXTURE = "io.github.supermonster003.autojs6.installer.spike.fixture" }
}
