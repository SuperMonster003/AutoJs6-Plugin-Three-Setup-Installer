package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import com.google.gson.JsonParser
import io.github.supermonster003.autojs6.plugin.three.setup.installer.priv.hidden.DhizukuOwnerIdentity
import org.junit.Assert.*
import org.junit.Test

class DhizukuSessionJournalTest {
    private val owner = DhizukuOwnerIdentity("com.test.owner/com.test.owner.Admin", "com.test.owner", 10042, 0,
        "com.test.owner/com.test.owner.Provider", "a".repeat(64))
    private val record = DhizukuSessionJournal.Record("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", 47, owner,
        "com.test.fixture", 8192, 123456, 2345, "345678")
    private val actual get() = DhizukuSessionJournal.Snapshot(record.id, owner.packageName, record.packageName, record.size, record.origin, 0, record.createdMillis)

    @Test fun recordRoundTripContainsOnlyOwnedIdentifiersAndNoSourceLocation() {
        val json = DhizukuSessionJournal.encode(record)
        assertEquals(record, DhizukuSessionJournal.decode(json))
        assertFalse(json.contains("content://"))
        assertFalse(json.contains("file://"))
        assertTrue(record.matches(owner, actual))
        assertTrue(record.origin.endsWith("/dhizuku-session/${record.token}"))
    }

    @Test fun everyRemoteOwnershipMismatchIsRejected() {
        val variants = listOf(actual.copy(id = 48), actual.copy(installerPackage = "com.other"),
            actual.copy(packageName = "com.other"), actual.copy(size = 8193), actual.copy(origin = null),
            actual.copy(origin = record.origin + "-other"), actual.copy(userId = 10), actual.copy(createdMillis = 123457))
        variants.forEach { assertFalse(it.toString(), record.matches(owner, it)) }
    }

    @Test fun changedOwnerProviderUserUidOrSigningIdentityCannotRecoverASession() {
        val variants = listOf(owner.copy(component = "com.test.owner/com.test.owner.OtherAdmin"),
            owner.copy(packageName = "com.other"), owner.copy(uid = 10043), owner.copy(userId = 10),
            owner.copy(provider = "com.test.owner/com.test.owner.OtherProvider"), owner.copy(signingSha256 = "b".repeat(64)))
        variants.forEach { assertFalse(it.toString(), record.matches(it, actual)) }
    }

    @Test fun oldPlatformsWithoutNonceProofAreNotTreatedAsRecoverable() {
        assertFalse(record.copy(createdMillis = null).matches(owner, actual.copy(origin = null, userId = null, createdMillis = null)))
        assertFalse(record.matches(owner, actual.copy(size = -1)))
        assertTrue(record.copy(createdMillis = null).matches(owner, actual.copy(userId = null)))
    }

    @Test fun malformedOrFutureRecordsNeverSupplyAnAbandonTarget() {
        val valid = JsonParser.parseString(DhizukuSessionJournal.encode(record)).asJsonObject
        val variants = listOf(
            valid.deepCopy().apply { addProperty("schema", 2) },
            valid.deepCopy().apply { addProperty("token", "../../other") },
            valid.deepCopy().apply { addProperty("id", -1) },
            valid.deepCopy().apply { addProperty("ownerUid", 1000042) },
            valid.deepCopy().apply { addProperty("signingSha256", "unknown") },
            valid.deepCopy().apply { addProperty("provider", "com.other/Provider") },
            valid.deepCopy().apply { addProperty("size", 0) },
            valid.deepCopy().apply { addProperty("size", 1.5) },
            valid.deepCopy().apply { addProperty("creatorPid", 0) },
            valid.deepCopy().apply { addProperty("creatorStartTicks", "unknown") },
            valid.deepCopy().apply { addProperty("extra", "unrecognized") },
        )
        variants.forEach { assertThrows(Exception::class.java) { DhizukuSessionJournal.decode(it.toString()) } }
        assertThrows(Exception::class.java) { DhizukuSessionJournal.decode(DhizukuSessionJournal.encode(record) + "{}") }
    }
}
