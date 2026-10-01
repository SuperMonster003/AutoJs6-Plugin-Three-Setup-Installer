package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import android.content.Intent
import io.github.supermonster003.autojs6.plugin.three.setup.installer.auth.Authorizer
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.PrivilegedOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PlannedApk
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PreparedPackage
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.RandomAccessFile

class InstallEngineTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun `writes all splits and closes each stream before committing`() {
        val engine = FakeEngine()
        val first = apk("base.apk", byteArrayOf(1, 2, 3))
        val second = apk("split.apk", byteArrayOf(4, 5))
        val listener = RecordingListener()
        val result = engine.install(request(first, second), listener)
        assertEquals("example.fixture", result.packageName)
        assertEquals(listOf("writing", "committing"), listener.stages)
        assertEquals(listOf(0L to 5L, 3L to 5L, 5L to 5L), listener.progress)
        assertArrayEquals(byteArrayOf(1, 2, 3), engine.session.outputs[0].toByteArray())
        assertArrayEquals(byteArrayOf(4, 5), engine.session.outputs[1].toByteArray())
        assertEquals(2, engine.session.synced)
        assertTrue(engine.session.committed)
        assertTrue(engine.session.closed)
        assertFalse(engine.session.abandoned)
        assertTrue(first.file.exists())
    }

    @Test fun `does not open a session for a cancelled request`() {
        val engine = FakeEngine()
        val failure = assertThrows(InstallFailure::class.java) {
            engine.install(request(apk()), RecordingListener()) { throw InstallFailure(InstallerErrorCodes.CANCELLED, "stopped") }
        }
        assertEquals(InstallerErrorCodes.CANCELLED, failure.code)
        assertEquals(0, engine.opened)
    }

    @Test fun `cancellation during writing abandons without commit and preserves sources`() {
        val source = apk()
        val engine = FakeEngine()
        var cancelled = false
        val listener = RecordingListener { if (it > 0) cancelled = true }
        val failure = assertThrows(InstallFailure::class.java) {
            engine.install(request(source), listener) {
                if (cancelled) throw InstallFailure(InstallerErrorCodes.CANCELLED, "stopped")
            }
        }
        assertEquals(InstallerErrorCodes.CANCELLED, failure.code)
        assertTrue(engine.session.abandoned && engine.session.closed)
        assertFalse(engine.session.committed)
        assertTrue(engine.session.outputs.single().closed)
        assertTrue(source.file.exists())
    }

    @Test fun `uses the earlier batch deadline while writing`() {
        var now = 100L
        val engine = FakeEngine(clock = { now })
        val listener = RecordingListener { if (it > 0) now = 150 }
        val failure = assertThrows(InstallFailure::class.java) {
            engine.install(request(apk()).copy(deadlineMillis = 150), listener)
        }
        assertEquals(InstallerErrorCodes.TIMEOUT, failure.code)
        assertTrue(engine.session.abandoned && engine.session.closed)
        assertFalse(engine.session.committed)
    }

    @Test fun `rejects shortened and enlarged APK streams before commit`() {
        for (length in listOf(1L, 9L)) {
            val source = apk("base$length.apk", byteArrayOf(1, 2, 3))
            val engine = FakeEngine().apply {
                session.beforeWrite = { RandomAccessFile(source.file, "rw").use { it.setLength(length) } }
            }
            val failure = assertThrows(InstallFailure::class.java) { engine.install(request(source), RecordingListener()) }
            assertEquals(InstallerErrorCodes.INVALID_PACKAGE, failure.code)
            assertTrue(engine.session.abandoned && engine.session.closed)
            assertFalse(engine.session.committed)
        }
    }

    @Test fun `rejects same length source changes before commit`() {
        val source = apk().let { it.copy(sha256 = java.security.MessageDigest.getInstance("SHA-256")
            .digest(it.file.readBytes()).joinToString("") { byte -> "%02x".format(byte) }) }
        val engine = FakeEngine().apply { session.beforeWrite = { source.file.writeBytes(byteArrayOf(4, 5, 6)) } }
        val failure = assertThrows(InstallFailure::class.java) { engine.install(request(source), RecordingListener()) }
        assertEquals(InstallerErrorCodes.INVALID_PACKAGE, failure.code)
        assertTrue(engine.session.abandoned && engine.session.closed)
        assertFalse(engine.session.committed)
    }

    @Test fun `matching source digest can commit`() {
        val source = apk().let { it.copy(sha256 = java.security.MessageDigest.getInstance("SHA-256")
            .digest(it.file.readBytes()).joinToString("") { byte -> "%02x".format(byte) }) }
        val engine = FakeEngine()
        engine.install(request(source), RecordingListener())
        assertTrue(engine.session.committed)
        assertFalse(engine.session.abandoned)
    }

    @Test fun `duplicate split names and invalid prepared packages never allocate a session`() {
        val engine = FakeEngine()
        val source = apk()
        assertThrows(InstallFailure::class.java) { engine.install(request(source, source), RecordingListener()) }
        val aab = prepared(source, format = InstallerContract.FORMAT_AAB, installable = false)
        val failure = assertThrows(InstallFailure::class.java) {
            engine.install(request(source).copy(prepared = aab), RecordingListener())
        }
        assertEquals(InstallerErrorCodes.UNSUPPORTED_FORMAT, failure.code)
        assertEquals(0, engine.opened)
    }

    @Test fun `unprivileged engine refuses silent and privileged options before creating a session`() {
        val engine = FakeEngine(Authorizer.NONE)
        val base = request(apk())
        for (request in listOf(
            base.copy(interaction = InstallerContract.INTERACTION_SILENT),
            base.copy(options = InstallOptions(allowDowngrade = true)),
            base.copy(options = InstallOptions(allowTestOnly = true)),
            base.copy(options = InstallOptions(bypassLowTargetSdk = true)),
            base.copy(options = InstallOptions(installer = "example.installer")),
            base.copy(options = InstallOptions(user = InstallerContract.USER_ALL)),
        )) {
            val failure = assertThrows(InstallFailure::class.java) { engine.install(request, RecordingListener()) }
            assertEquals(InstallerErrorCodes.AUTHORIZER_REQUIRED, failure.code)
        }
        assertEquals(0, engine.opened)
    }

    @Test fun `notification interaction remains distinct for privileged and ordinary engines`() {
        for (authorizer in listOf(Authorizer.NONE, Authorizer.SHIZUKU, Authorizer.ROOT)) {
            val source = apk("base-${authorizer.id}.apk")
            val engine = FakeEngine(authorizer)
            val result = engine.install(request(source).copy(interaction = InstallerContract.INTERACTION_NOTIFICATION), RecordingListener())
            assertEquals(InstallerContract.INTERACTION_NOTIFICATION, result.interaction)
            assertTrue(engine.session.committed)
            assertFalse(engine.session.abandoned)
        }
    }

    @Test fun `maps privileged options including all users and reports unsupported low target override`() {
        for (sdk in listOf(24, 34)) {
            val engine = FakeEngine(sdk = sdk)
            val result = engine.install(request(apk("base$sdk.apk")).copy(options = InstallOptions(
                allowDowngrade = true, allowTestOnly = true, bypassLowTargetSdk = true,
                user = InstallerContract.USER_ALL,
            )), RecordingListener())
            val common = PrivilegedOptions.INSTALL_REPLACE_EXISTING or PrivilegedOptions.INSTALL_ALLOW_TEST or
                PrivilegedOptions.INSTALL_REQUEST_DOWNGRADE or PrivilegedOptions.INSTALL_ALLOW_DOWNGRADE or PrivilegedOptions.INSTALL_ALL_USERS
            assertEquals(common or if (sdk >= 34) PrivilegedOptions.INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK else 0, engine.parameters!!.flags)
            assertEquals(if (sdk >= 34) 0 else 1, result.notes.size)
        }
    }

    @Test fun `rejects mismatched engine and target user`() {
        val engine = FakeEngine()
        val base = request(apk())
        for (request in listOf(base.copy(options = InstallOptions(authorizer = "root")), base.copy(userId = 10),
            base.copy(userId = 10, options = InstallOptions(user = "11")))) {
            val failure = assertThrows(InstallFailure::class.java) { engine.install(request, RecordingListener()) }
            assertEquals(InstallerErrorCodes.INVALID_ARGUMENT, failure.code)
        }
        assertEquals(0, engine.opened)
    }

    @Test fun `system failures retain their status and message and only release the session`() {
        val engine = FakeEngine().apply {
            session.status = InstallStatusBridge.Status(InstallStatusMapper.STATUS_FAILURE_STORAGE, "disk full", "example.fixture", null)
        }
        val failure = assertThrows(InstallFailure::class.java) { engine.install(request(apk()), RecordingListener()) }
        assertEquals(InstallerErrorCodes.INSUFFICIENT_STORAGE, failure.code)
        assertEquals(InstallStatusMapper.STATUS_FAILURE_STORAGE, failure.status)
        assertEquals("disk full", failure.systemMessage)
        assertFalse(engine.session.abandoned)
        assertTrue(engine.session.closed)
    }

    @Test fun `reports progress at most one MiB apart`() {
        val source = apk(bytes = ByteArray(2 * 1024 * 1024 + 123))
        val listener = RecordingListener()
        FakeEngine().install(request(source), listener)
        assertTrue(listener.progress.size >= 4)
        assertEquals(source.size, listener.progress.last().first)
        assertTrue(listener.progress.zipWithNext().all { (before, after) -> after.first - before.first in 1..1024 * 1024 })
    }

    @Test fun `optimization failures and late cancellation preserve a confirmed installation`() {
        for ((failure, expected) in listOf(IOException("compiler unavailable") to "unknown",
            InstallFailure(InstallerErrorCodes.CANCELLED, "stopped after installation") to "cancelled",
            InstallFailure(InstallerErrorCodes.TIMEOUT, "no budget left") to "timeout")) {
            val engine = FakeEngine().apply { followUp = { throw failure } }
            val listener = RecordingListener()
            val result = engine.install(request(apk("base-$expected.apk")).copy(options = InstallOptions(dexopt = "speed")), listener)
            assertEquals("example.fixture", result.packageName)
            assertEquals(expected, result.followUp.dexopt?.status)
            assertTrue(result.notes.isNotEmpty())
            assertEquals(listOf("writing", "committing", "optimizing"), listener.stages)
            assertEquals(1, engine.followUpCalls)
            assertTrue(engine.session.closed)
            assertFalse(engine.session.abandoned)
        }
    }

    @Test fun `an unsuccessful installation never starts optimization`() {
        val engine = FakeEngine().apply { session.status = InstallStatusBridge.Status(1, "rejected", "example.fixture", null) }
        assertThrows(InstallFailure::class.java) { engine.install(request(apk()).copy(options = InstallOptions(dexopt = "speed")), RecordingListener()) }
        assertEquals(0, engine.followUpCalls)
    }

    @Test fun `none platform security refusals are nonretryable policy errors`() {
        val denial = SecurityException("DISALLOW_INSTALL_APPS")
        val engine = FakeEngine(Authorizer.NONE).apply {
            session.beforeCommit = { nonePlatformCall { throw denial } }
        }
        val failure = assertThrows(InstallFailure::class.java) { engine.install(request(apk()), RecordingListener()) }
        assertEquals(InstallerErrorCodes.BLOCKED_BY_POLICY, failure.code)
        assertEquals(denial.message, failure.systemMessage)
        assertSame(denial, failure.cause)
        assertFalse(failure.retryable)
        assertTrue(engine.session.abandoned && engine.session.closed)
        val creation = assertThrows(InstallFailure::class.java) { nonePlatformCall { throw denial } }
        assertEquals(InstallerErrorCodes.BLOCKED_BY_POLICY, creation.code)
    }

    @Test fun `privileged security refusals retain authorizer mapping`() {
        val engine = FakeEngine().apply { session.beforeCommit = { throw SecurityException("Caller refused") } }
        val failure = assertThrows(InstallFailure::class.java) { engine.install(request(apk()), RecordingListener()) }
        assertEquals(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, failure.code)
        assertTrue(failure.retryable)
        assertTrue(engine.session.abandoned && engine.session.closed)
    }

    @Test fun `write failure abandons and closes without masking the error`() {
        val engine = FakeEngine().apply { session.beforeWrite = { throw IOException("broken destination") } }
        val failure = assertThrows(InstallFailure::class.java) { engine.install(request(apk()), RecordingListener()) }
        assertEquals(InstallerErrorCodes.INSTALL_FAILED, failure.code)
        assertTrue(engine.session.abandoned && engine.session.closed)
        assertFalse(engine.session.committed)
    }

    @Test fun `private storage markers are exact and only decoded at private service calls`() {
        val marker = PrivilegedOptions.ERROR_INSUFFICIENT_STORAGE
        val original = IllegalStateException(marker)
        val engine = FakeEngine().apply { session.beforeWrite = { privilegedInstallerCall { throw original } } }
        val failure = assertThrows(InstallFailure::class.java) { engine.install(request(apk()), RecordingListener()) }
        assertEquals(InstallerErrorCodes.INSUFFICIENT_STORAGE, failure.code)
        assertSame(original, failure.cause)
        assertTrue(engine.session.abandoned && engine.session.closed)
        assertFalse(engine.session.committed)
        assertEquals(InstallerErrorCodes.INTERNAL, InstallFailure.from(IllegalStateException(marker)).code)
        val prefixed = IllegalStateException("Unexpected response: $marker")
        assertSame(prefixed, assertThrows(IllegalStateException::class.java) { privilegedInstallerCall { throw prefixed } })
    }

    @Test fun `interruption while awaiting a result remains cancellation and preserves interrupt flag`() {
        val engine = FakeEngine().apply { session.beforeAwait = { throw InterruptedException("stopped") } }
        try {
            val failure = assertThrows(InstallFailure::class.java) { engine.install(request(apk()), RecordingListener()) }
            assertEquals(InstallerErrorCodes.CANCELLED, failure.code)
            assertTrue(Thread.currentThread().isInterrupted)
            assertTrue(engine.session.abandoned && engine.session.closed)
        } finally { Thread.interrupted() }
    }

    private fun apk(name: String = "base.apk", bytes: ByteArray = byteArrayOf(1, 2, 3)): PlannedApk =
        temporary.newFile(name).also { it.writeBytes(bytes) }.let { PlannedApk(name, it, it.length(), null) }

    private fun prepared(vararg apks: PlannedApk, format: String = "apk", installable: Boolean = true) = PreparedPackage(
        format, "fixture.apk", apks.sumOf { it.size }, "example.fixture", "1", 1L, "Fixture", 24, 28,
        apks.toList(), emptyList(), null, emptyList(), emptyList(), installable,
    )

    private fun request(vararg apks: PlannedApk) = InstallEngine.Request(prepared(*apks), InstallOptions(), 0)

    private class RecordingListener(private val progressed: (Long) -> Unit = {}) : InstallEngine.Listener {
        val stages = mutableListOf<String>()
        val progress = mutableListOf<Pair<Long, Long>>()
        override fun onStage(stage: String) { stages += stage }
        override fun onProgress(bytesWritten: Long, totalBytes: Long) { progress += bytesWritten to totalBytes; progressed(bytesWritten) }
        override fun onUserAction(intent: Intent) = error("Unexpected confirmation")
    }

    private class FakeEngine(authorizer: Authorizer = Authorizer.SHIZUKU, sdk: Int = 35, clock: () -> Long = { 100L }) :
        SessionInstallEngine(authorizer, sdk, 0, clock) {
        var opened = 0
        var parameters: Parameters? = null
        val session = FakeSession()
        var followUp: () -> InstallFollowUp = { InstallFollowUp() }
        var followUpCalls = 0
        override fun afterInstallation(request: InstallEngine.Request, packageName: String,
            deadlineMillis: Long, checkActive: () -> Unit): InstallFollowUp {
            followUpCalls++
            return followUp()
        }
        override fun openSession(request: InstallEngine.Request, parameters: Parameters, deadlineMillis: Long): Session {
            opened++
            this.parameters = parameters
            return session
        }
    }

    private class RecordedOutput : ByteArrayOutputStream() {
        var closed = false
        override fun close() { closed = true; super.close() }
    }

    private class FakeSession : SessionInstallEngine.Session {
        var beforeWrite: () -> Unit = {}
        var beforeAwait: () -> Unit = {}
        var beforeCommit: () -> Unit = {}
        var status = InstallStatusBridge.Status(0, null, "example.fixture", null)
        val outputs = mutableListOf<RecordedOutput>()
        var synced = 0
        var committed = false
        var abandoned = false
        var closed = false
        override fun openWrite(apk: PlannedApk, checkActive: () -> Unit): OutputStream {
            beforeWrite()
            return RecordedOutput().also { outputs += it }
        }
        override fun fsync(output: OutputStream) { assertFalse((output as RecordedOutput).closed); synced++ }
        override fun commit() { assertTrue(outputs.all { it.closed }); beforeCommit(); committed = true }
        override fun await(deadlineMillis: Long, checkActive: () -> Unit, onUserAction: (Intent) -> Unit): InstallStatusBridge.Status {
            beforeAwait()
            checkActive()
            return status
        }
        override fun abandon() { abandoned = true }
        override fun close() { closed = true }
    }
}
