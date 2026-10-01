package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import org.autojs.plugin.installer.api.InstallerContract as C
import org.junit.Assert.*
import org.junit.Test

class InstallFollowUpResultTest {
    private fun result(followUp: InstallFollowUp) = InstallDocuments.installResult(
        "example.fixture", "1", 1, null, "root", "silent", 12, false, followUp.notes, followUp)

    @Test fun `unread owner is absent while confirmed no owner is explicit null`() {
        assertFalse(result(InstallFollowUp()).has(C.FIELD_UPDATE_OWNER))
        assertTrue(result(InstallFollowUp(updateOwnerRead = true))[C.FIELD_UPDATE_OWNER].isJsonNull)
        assertEquals("example.owner", result(InstallFollowUp(updateOwnerRead = true,
            updateOwner = "example.owner"))[C.FIELD_UPDATE_OWNER].asString)
    }

    @Test fun `all follow-up outcomes preserve installation success and selected filter`() {
        for (status in C.DEXOPT_STATUSES) {
            val document = result(InstallFollowUp(dexopt = InstallFollowUp.Optimization(C.DEXOPT_SPEED, status)))
            assertTrue(document[C.FIELD_OK].asBoolean)
            assertFalse(document.has(C.FIELD_ERROR))
            assertEquals(status, document.getAsJsonObject(C.FIELD_DEXOPT)[C.FIELD_DEXOPT_STATUS].asString)
            assertEquals(C.DEXOPT_SPEED, document.getAsJsonObject(C.FIELD_DEXOPT)[C.FIELD_FILTER].asString)
        }
    }

    @Test fun `wire budget drops optional details without discarding confirmed batch outcomes`() {
        val entries = List(C.MAX_BATCH_SOURCES) {
            result(InstallFollowUp(updateOwnerRead = true, updateOwner = "example.owner",
                dexopt = InstallFollowUp.Optimization(C.DEXOPT_SPEED, C.DEXOPT_STATUS_UNKNOWN, "x".repeat(4096))))
        }
        val original = InstallDocuments.batch(entries)
        val bounded = InstallDocuments.boundedResult(original)
        assertTrue(bounded.toString().toByteArray().size <= C.MAX_JSON_BYTES)
        assertEquals(C.MAX_BATCH_SOURCES, bounded.getAsJsonArray(C.FIELD_RESULTS).size())
        bounded.getAsJsonArray(C.FIELD_RESULTS).forEach { entry ->
            assertTrue(entry.asJsonObject[C.FIELD_OK].asBoolean)
            assertEquals("example.fixture", entry.asJsonObject[C.FIELD_PACKAGE_NAME].asString)
            assertFalse(entry.asJsonObject.has(C.FIELD_DEXOPT))
            assertFalse(entry.asJsonObject.has(C.FIELD_UPDATE_OWNER))
        }
        assertTrue(original.getAsJsonArray(C.FIELD_RESULTS)[0].asJsonObject.has(C.FIELD_DEXOPT))
    }
}
