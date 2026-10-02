package io.github.supermonster003.autojs6.plugin.three.setup.installer.profiles

import org.junit.Assert.*
import org.junit.Test

class InstallProfilePreferenceWriterTest {
    @Test fun successfulSaveCommitsExactlyOnceAndPreservesOtherPreferenceKeys() {
        val storage = MemoryStorage(mapOf("document" to "original", "unrelated" to 42))
        val writer = InstallProfilePreferenceWriter()
        assertTrue(writer.write(storage, "new revision", wasUnconfigured = false))
        assertEquals(1, storage.commits.size)
        assertEquals(mapOf("document" to "new revision", "unrelated" to 42), storage.memory)
        assertNull(writer.failureRevision)
        assertEquals(0, storage.deletions)
    }

    @Test fun falseCommitAfterMemoryUpdateRestoresTheOriginalDocumentWithOneCompensatingCommit() {
        val storage = MemoryStorage(mapOf("document" to "saved revision", "unrelated" to true))
        storage.result = { count -> count != 1 }
        val writer = InstallProfilePreferenceWriter()
        assertFalse(writer.write(storage, "unconfirmed revision", wasUnconfigured = false))
        assertEquals(listOf("unconfirmed revision", "saved revision"), storage.commits.map { it.value })
        assertEquals(mapOf("document" to "saved revision", "unrelated" to true), storage.memory)
        assertNull(writer.failureRevision)
        assertEquals(0, storage.deletions)
    }

    @Test fun rollbackPreservesEverySupportedOriginalFieldTypeIncludingMalformedProfileTypes() {
        for (original in listOf<Any>("saved", true, 17, 17L, 1.25f, setOf("first", "second"))) {
            val storage = MemoryStorage(mapOf("document" to original, "other" to "kept"))
            storage.result = { count -> count != 1 }
            val writer = InstallProfilePreferenceWriter()
            assertFalse(writer.write(storage, "unconfirmed", wasUnconfigured = false))
            assertEquals(InstallProfilePreferenceWriter.Field(true, original), storage.commits.last())
            if (original !is Set<*>) assertEquals(original.javaClass, storage.memory.getValue("document").javaClass)
            assertEquals(original, storage.memory["document"])
            assertEquals("kept", storage.memory["other"])
            assertNull(writer.failureRevision)
        }
    }

    @Test fun missingDocumentInAnExistingFileIsRestoredAsMissingWithoutDeletingTheFile() {
        val storage = MemoryStorage(mapOf("other" to "kept"), existed = true)
        storage.result = { count -> count != 1 }
        val writer = InstallProfilePreferenceWriter()
        assertFalse(writer.write(storage, "unconfirmed", wasUnconfigured = false))
        assertEquals(InstallProfilePreferenceWriter.Field(false, null), storage.commits.last())
        assertEquals(mapOf("other" to "kept"), storage.memory)
        assertTrue(storage.exists)
        assertEquals(0, storage.deletions)
        assertNull(writer.failureRevision)
    }

    @Test fun originallyUnconfiguredStorageRemovesOnlyItsConfirmedEmptyRollbackFile() {
        val storage = MemoryStorage(emptyMap(), existed = false)
        storage.result = { count -> count != 1 }
        val writer = InstallProfilePreferenceWriter()
        assertFalse(writer.write(storage, "unconfirmed", wasUnconfigured = true))
        assertEquals(2, storage.commits.size)
        assertEquals(1, storage.deletions)
        assertTrue(storage.memory.isEmpty())
        assertFalse(storage.exists)
        assertNull(writer.failureRevision)
    }

    @Test fun unrelatedKeyAddedDuringFailurePreventsEmptyFileDeletionAndRetainsAStableFailureIdentity() {
        val storage = MemoryStorage(emptyMap(), existed = false)
        storage.result = { count -> if (count == 1) { storage.memory["external"] = "retained"; false } else true }
        val writer = InstallProfilePreferenceWriter()
        assertFalse(writer.write(storage, "unconfirmed", wasUnconfigured = true))
        assertEquals(mapOf("external" to "retained"), storage.memory)
        assertTrue(storage.exists)
        assertEquals(0, storage.deletions)
        val failedRevision = requireNotNull(writer.failureRevision)
        assertEquals(failedRevision, writer.failureRevision)
    }

    @Test fun externalDocumentReplacementIsNeverAutomaticallyOverwrittenByRollback() {
        val storage = MemoryStorage(mapOf("document" to "baseline"))
        storage.result = { storage.memory["document"] = "external change"; false }
        val writer = InstallProfilePreferenceWriter()
        assertFalse(writer.write(storage, "our failed revision", wasUnconfigured = false))
        assertEquals(1, storage.commits.size)
        assertEquals("external change", storage.memory["document"])
        assertNotNull(writer.failureRevision)
        assertEquals(0, storage.deletions)
    }

    @Test fun failedRollbackCannotCertifyItsRestoredMemoryAsDurableConfiguration() {
        val storage = MemoryStorage(mapOf("document" to "baseline"))
        storage.result = { false }
        val writer = InstallProfilePreferenceWriter()
        assertFalse(writer.write(storage, "unconfirmed", wasUnconfigured = false))
        assertEquals("baseline", storage.memory["document"])
        assertEquals(2, storage.commits.size)
        assertNotNull(writer.failureRevision)
    }

    @Test fun rollbackExceptionAndEmptyFileRemovalFailureRemainUnreadable() {
        val rollbackThrows = MemoryStorage(mapOf("document" to "baseline"))
        rollbackThrows.result = { count -> if (count == 1) false else error("Fixture rollback failure") }
        val first = InstallProfilePreferenceWriter()
        assertFalse(first.write(rollbackThrows, "unconfirmed", wasUnconfigured = false))
        assertNotNull(first.failureRevision)

        val deletionFails = MemoryStorage(emptyMap(), existed = false)
        deletionFails.result = { count -> count != 1 }
        deletionFails.deleteResult = false
        val second = InstallProfilePreferenceWriter()
        assertFalse(second.write(deletionFails, "unconfirmed", wasUnconfigured = true))
        assertTrue(deletionFails.memory.isEmpty())
        assertTrue(deletionFails.exists)
        assertEquals(1, deletionFails.deletions)
        assertNotNull(second.failureRevision)
    }

    @Test fun stableUnreadableRevisionAllowsAnExplicitResetAndOnlySuccessfulSaveClearsPriorUncertainty() {
        val storage = MemoryStorage(mapOf("document" to "baseline"))
        storage.result = { false }
        val writer = InstallProfilePreferenceWriter()
        assertFalse(writer.write(storage, "first unconfirmed", wasUnconfigured = false))
        val failure = requireNotNull(writer.failureRevision)
        // Public read() uses this exact snapshot for compare-before-save, including after reload.
        val observed = requireNotNull(writer.unreadableSnapshot)
        assertFalse(observed.readable)
        assertEquals(observed, writer.unreadableSnapshot)

        val attemptStart = storage.commits.size
        storage.result = { count -> count != attemptStart + 1 }
        assertFalse(writer.write(storage, "second unconfirmed", wasUnconfigured = false))
        assertEquals(failure, writer.failureRevision)
        assertEquals(observed, writer.unreadableSnapshot)

        storage.result = { true }
        val resetStart = storage.commits.size
        assertTrue(writer.write(storage, "explicit empty-profile reset", wasUnconfigured = false))
        assertEquals(resetStart + 1, storage.commits.size)
        assertNull(writer.failureRevision)
        assertNull(writer.unreadableSnapshot)
        assertEquals("explicit empty-profile reset", storage.memory["document"])
    }

    /** Android commit() mutates the in-memory map before it reports the disk result. */
    private class MemoryStorage(initial: Map<String, Any>, existed: Boolean = true) : InstallProfilePreferenceWriter.Storage {
        val memory = initial.toMutableMap()
        var exists = existed
        var result: (Int) -> Boolean = { true }
        var deleteResult = true
        var deletions = 0
        val commits = mutableListOf<InstallProfilePreferenceWriter.Field>()
        override fun values(): Map<String, *> = memory.toMap()
        override fun fileExists() = exists
        override fun commit(field: InstallProfilePreferenceWriter.Field): Boolean {
            commits += field
            if (field.present) memory["document"] = requireNotNull(field.value) else memory.remove("document")
            exists = true
            return result(commits.size)
        }
        override fun deleteEmptyFile(): Boolean {
            deletions++
            check(memory.isEmpty()) { "The writer must not delete unrelated preference keys" }
            if (deleteResult) exists = false
            return deleteResult
        }
    }
}
