package io.github.supermonster003.autojs6.plugin.three.setup.installer.history

import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallOptions
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallRequest
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallSession
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.SourceEntry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.InstallPresentation
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class InstallHistoryCaptureTest {
    private val token = UUID.randomUUID().toString()
    private val now = 1_800_000_000_000L
    private val request = InstallRequest("host-request", listOf(SourceEntry(0, 0, "first.apk", 10),
        SourceEntry(1, 1, "second.apk", 20), SourceEntry(2, 2, "third.apk", 30)),
        InstallerContract.INTERACTION_SILENT, InstallOptions(), origin = InstallerContract.SOURCE_SCRIPT)

    @Test fun terminalBatchKeepsInstalledFactsAndCancelsOnlyUnstartedItems() {
        val metadata = InstallPresentation.Metadata("Example", "example.archive", "2.0", 2,
            InstallSession.Version("1.0", 1), 0, true, 10, 24, 35, "signature", null, "apk", emptyList(), emptyList())
        val installed = JsonObject().apply {
            addProperty("ok", true); addProperty("packageName", "example.installed"); addProperty("versionName", "2.1")
            addProperty("versionCode", 21); addProperty("previousVersionCode", 10); addProperty("authorizer", "shizuku")
            addProperty("durationMillis", 8)
        }
        val failure = InstallFailure(InstallerErrorCodes.SOURCE_UNREADABLE, "No source", systemMessage = "Provider rejected access")
        val failed = JsonObject().apply { addProperty("ok", false); add("error", failure.toJson()) }
        val state = InstallPresentation.Snapshot(4, "failed", 1, 0f, listOf(
            InstallPresentation.Item("first.apk", "completed", metadata, installed),
            InstallPresentation.Item("second.apk", "failed", result = failed),
            InstallPresentation.Item("third.apk", "cancelled")), null, true, failure, false, false)
        val entries = InstallHistoryCapture.capture(token, request, state, now, now + 50)
        assertEquals(listOf("completed", "failed", "cancelled"), entries.map { it.result })
        assertEquals(listOf("script", "script", "script"), entries.map { it.origin })
        val first = entries.first()
        assertEquals("Example", first.label)
        assertEquals("example.installed", first.packageName)
        assertEquals("2.1", first.versionName)
        assertEquals(21L, first.versionCode)
        assertEquals("1.0", first.previousVersionName)
        assertEquals(10L, first.previousVersionCode)
        assertEquals("shizuku", first.authorizer)
        assertEquals(8L, first.durationMillis)
        assertEquals("Provider rejected access", entries[1].systemMessage)
        assertEquals(InstallerErrorCodes.CANCELLED, entries[2].errorCode)
        assertNull(entries[2].packageName)
        assertEquals(entries, InstallHistoryCodec.decode(InstallHistoryCodec.encode(entries)))
    }

    @Test fun userDeclinedAndExplicitCancellationMapToCancelledRows() {
        for (code in listOf(InstallerErrorCodes.USER_CANCELLED, InstallerErrorCodes.CANCELLED)) {
            val item = InstallPresentation.Item("source.apk", "failed", result = JsonObject().apply {
                addProperty("ok", false); add("error", InstallFailure(code, "Cancelled").toJson())
            })
            val state = InstallPresentation.Snapshot(1, "failed", 0, 0f, listOf(item), null, true, null, false, false)
            val result = InstallHistoryCapture.capture(token, request, state, now, now + 10).single()
            assertEquals("cancelled", result.result)
            assertEquals(code, result.errorCode)
            assertTrue(result.terminal)
        }
    }

    @Test fun anActiveItemRemainsPendingForRestartRecoveryWithoutAResultOrSourceUri() {
        val item = InstallPresentation.Item("content://private.provider/sensitive.apk", "preparing")
        val state = InstallPresentation.Snapshot(1, "preparing", 0, 0f, listOf(item), null, false, null, false, false)
        val result = InstallHistoryCapture.capture(token, request.copy(origin = "home"), state, now, now + 10).single()
        assertFalse(result.terminal)
        assertNull(result.finishedAt)
        assertEquals("home", result.origin)
        assertFalse(InstallHistoryCodec.encode(listOf(result)).toString(Charsets.UTF_8).contains("content://"))
    }
}
