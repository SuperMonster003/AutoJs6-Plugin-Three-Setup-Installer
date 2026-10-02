package io.github.supermonster003.autojs6.plugin.three.setup.installer.profiles

import org.autojs.plugin.installer.api.InstallerContract as C
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class InstallProfileMatchingTest {
    private val revision = UUID(1, 2).toString()
    private fun rule(index: Int, source: String = InstallProfileSources.ANY, prefix: String = "", enabled: Boolean = true) =
        InstallProfile(UUID(0, index.toLong()).toString(), "Rule $index", enabled, source, prefix)

    @Test fun firstEnabledMatchWinsWithoutAutomaticallyPreferringALongerPrefix() {
        val disabled = rule(1, prefix = "org.", enabled = false)
        val wrongOrigin = rule(2, C.SOURCE_SCRIPT, "org.example.")
        val broad = rule(3, C.SOURCE_HOST, "org.")
        val narrow = rule(4, C.SOURCE_HOST, "org.example.")
        val snapshot = InstallProfileSnapshot(revision, listOf(disabled, wrongOrigin, broad, narrow)).frozen()
        assertEquals(broad, snapshot.match(C.SOURCE_HOST, "org.example.app"))
        assertEquals(wrongOrigin, snapshot.match(C.SOURCE_SCRIPT, "org.example.app"))
        val reordered = snapshot.copy(profiles = listOf(narrow, broad)).frozen()
        assertEquals(narrow, reordered.match(C.SOURCE_HOST, "org.example.app"))
    }

    @Test fun originAndPackagePrefixMustBothMatch() {
        val value = rule(1, C.SOURCE_EXTERNAL, "org.example.")
        InstallProfileRules.validate(value)
        assertTrue(value.matches(C.SOURCE_EXTERNAL, "org.example.app"))
        assertFalse(value.matches(C.SOURCE_HOST, "org.example.app"))
        assertFalse(value.matches(C.SOURCE_EXTERNAL, "org.other.app"))
        assertFalse(value.matches(C.SOURCE_SCRIPT, "org.other.app"))
        assertFalse(value.copy(enabled = false).matches(C.SOURCE_EXTERNAL, "org.example.app"))
    }

    @Test fun anySourceIncludesHomeWithoutTreatingHomeAsAnExternalIntent() {
        val any = rule(1, prefix = "org.example.")
        val external = rule(2, C.SOURCE_EXTERNAL, "org.example.")
        val allOrigins = listOf(C.SOURCE_HOST, C.SOURCE_SCRIPT, C.SOURCE_EXTERNAL, C.SOURCE_HOME)
        allOrigins.forEach { origin -> assertTrue(origin, any.matches(origin, "org.example.app")) }
        assertFalse(external.matches(C.SOURCE_HOME, "org.example.app"))
        assertFalse(InstallProfileSnapshot(revision, listOf(external)).hasCandidates(C.SOURCE_HOME))
        assertEquals(any, InstallProfileSnapshot(revision, listOf(external, any)).match(C.SOURCE_HOME, "org.example.app"))
    }

    @Test fun emptyPrefixMatchesTheSelectedOriginAndOtherPrefixesRemainLiteralAndCaseSensitive() {
        val allHostPackages = rule(1, C.SOURCE_HOST)
        assertTrue(allHostPackages.matches(C.SOURCE_HOST, "another.vendor.app"))
        assertFalse(allHostPackages.matches(C.SOURCE_SCRIPT, "another.vendor.app"))
        val literal = rule(2, prefix = "org.Example")
        assertTrue(literal.matches(C.SOURCE_HOST, "org.ExampleExtra.app"))
        assertFalse(literal.matches(C.SOURCE_HOST, "org.example.app"))
        assertFalse(literal.matches(C.SOURCE_HOST, "other.org.Example.app"))
        val dotted = literal.copy(packagePrefix = "org.Example.")
        assertFalse(dotted.matches(C.SOURCE_HOST, "org.ExampleExtra.app"))
        assertTrue(dotted.matches(C.SOURCE_HOST, "org.Example.app"))
    }

    @Test fun candidatesAreKnownBeforeParsingButAnUnmatchedPrefixIsNotApplied() {
        val snapshot = InstallProfileSnapshot(revision, listOf(
            rule(1, C.SOURCE_HOST, "org.special."), rule(2, enabled = false)))
        assertTrue(snapshot.hasCandidates(C.SOURCE_HOST))
        assertNull(snapshot.match(C.SOURCE_HOST, "org.other.app"))
        assertFalse(snapshot.hasCandidates(C.SOURCE_SCRIPT))
        assertFalse(InstallProfileSnapshot().hasCandidates(C.SOURCE_HOME))
        assertNull(InstallProfileSnapshot().match(C.SOURCE_HOME, "org.example.app"))
    }

    @Test fun aFrozenSnapshotCannotBeChangedThroughTheEditorListOrItsReturnedCollection() {
        val first = rule(1, prefix = "org.")
        val input = mutableListOf(first)
        val frozen = InstallProfileSnapshot(revision, input).frozen()
        input[0] = rule(2, prefix = "other.")
        input.clear()
        assertEquals(listOf(first), frozen.profiles)
        assertEquals(first, frozen.match(C.SOURCE_HOME, "org.example.app"))
        assertThrows(UnsupportedOperationException::class.java) { (frozen.profiles as MutableList<InstallProfile>).clear() }
        val decoded = InstallProfileCodec.decode(InstallProfileCodec.encode(frozen))
        assertThrows(UnsupportedOperationException::class.java) { (decoded.profiles as MutableList<InstallProfile>).add(rule(3)) }
        assertEquals(frozen, decoded)
    }
}
