package io.github.supermonster003.autojs6.plugin.three.setup.installer.auth

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class RootPersistentDefaultProtocolTest {
    private val token = "0123456789abcdef0123456789abcdef"
    private val directory = "/data/user/0/${RootPersistentDefaultProtocol.PACKAGE}/no_backup/root-persistent-default/$token"

    @Test fun commandQuotesInstalledApkAndKeepsTheOperationAndTargetClosed() {
        val apk = "/data/app/owned'package/base.apk"
        val command = RootPersistentDefaultProtocol.launch(apk, directory, RootPersistentDefaultProtocol.Operation.SET, token)
        assertTrue(command.contains("su 1000 -c"))
        assertTrue(command.contains(RootPersistentDefaultProtocol.ENTRY))
        assertTrue(command.contains("0<'$directory/control'"))
        assertTrue(command.contains("2<>'${directory.substringBeforeLast('/')}/guard'"))
        assertEquals("'a'\"'\"'b'", RootPersistentDefaultProtocol.quote("a'b"))
        assertThrows(IllegalArgumentException::class.java) { RootPersistentDefaultProtocol.quote("line\nbreak") }
        assertThrows(IllegalArgumentException::class.java) { RootPersistentDefaultProtocol.launch(apk, directory, RootPersistentDefaultProtocol.Operation.SET, "x;id") }
        assertThrows(IllegalArgumentException::class.java) { RootPersistentDefaultProtocol.launch(apk, directory + "/../other", RootPersistentDefaultProtocol.Operation.SET, token) }
        assertThrows(IllegalArgumentException::class.java) { RootPersistentDefaultProtocol.launch("relative.apk", directory, RootPersistentDefaultProtocol.Operation.SET, token) }
    }

    @Test fun handshakeRejectsWrongIdentityOverflowAndAnotherRun() {
        val hello = """{"type":"hello","token":"$token","uid":1000,"gid":1000,"pid":123,"startTicks":456}"""
        val packet = RootPersistentDefaultProtocol.packet(hello, token)
        assertEquals(RootPersistentDefaultProtocol.Identity(123, 456), RootPersistentDefaultProtocol.identity(packet))
        for (invalid in listOf(hello.replace("\"uid\":1000", "\"uid\":0"),
            hello.replace("\"gid\":1000", "\"gid\":1001"),
            hello.replace("\"uid\":1000", "\"uid\":4294968296"),
            hello.replace("\"uid\":1000", "\"uid\":1e3"),
            hello.replace("\"pid\":123", "\"pid\":4294967419"))) {
            assertThrows(IllegalArgumentException::class.java) { RootPersistentDefaultProtocol.identity(JsonParser.parseString(invalid).asJsonObject) }
        }
        assertThrows(IllegalArgumentException::class.java) { RootPersistentDefaultProtocol.packet(hello.replace(token, "f".repeat(32)), token) }
        assertThrows(IllegalArgumentException::class.java) { RootPersistentDefaultProtocol.packet(hello.replace("hello", "exec"), token) }
        assertThrows(IllegalArgumentException::class.java) { RootPersistentDefaultProtocol.packet("x".repeat(65_537), token) }
        val kill = RootPersistentDefaultProtocol.terminate(token, RootPersistentDefaultProtocol.Identity(123, 456))
        assertTrue(kill.contains("Uid:[[:space:]]*1000"))
        assertTrue(kill.contains("\${22}"))
        assertTrue(kill.contains("three_setup_default_$token"))
        assertTrue(kill.contains("kill -TERM 123"))
    }

    @Test fun existingAndForeignPoliciesAreProtectedBeforeAnyWrite() {
        val all = RootPersistentDefaultProtocol.keys.toList()
        val set = RootPersistentDefaultProtocol.Operation.SET
        val clear = RootPersistentDefaultProtocol.Operation.CLEAR
        assertTrue(RootPersistentDefaultProtocol.changes(set, emptyList(), false))
        assertFalse(RootPersistentDefaultProtocol.changes(set, all, false))
        assertFalse(RootPersistentDefaultProtocol.changes(set, all + all, false))
        assertTrue(RootPersistentDefaultProtocol.changes(clear, all, true))
        assertTrue(RootPersistentDefaultProtocol.changes(clear, all + all, true))
        assertFalse(RootPersistentDefaultProtocol.changes(clear, emptyList(), true))
        assertThrows(IllegalArgumentException::class.java) { RootPersistentDefaultProtocol.changes(set, listOf(all.first()), false) }
        assertThrows(IllegalArgumentException::class.java) { RootPersistentDefaultProtocol.changes(set, emptyList(), true) }
        assertThrows(IllegalArgumentException::class.java) { RootPersistentDefaultProtocol.changes(clear, null, false) }
        assertTrue(RootPersistentDefaultProtocol.changes(clear, listOf(all.first(), all.first()), false))
        assertFalse(RootPersistentDefaultProtocol.changes(RootPersistentDefaultProtocol.Operation.READ, null, true))
    }

    @Test fun rollbackNeverAbsorbsAnOlderOrDuplicatedPolicy() {
        val all = RootPersistentDefaultProtocol.keys.toList()
        assertTrue(RootPersistentDefaultProtocol.canRollbackNew(0, all.take(2), true))
        assertTrue(RootPersistentDefaultProtocol.canRollbackNew(0, all, true))
        assertFalse(RootPersistentDefaultProtocol.canRollbackNew(0, all + all.first(), true))
        assertFalse(RootPersistentDefaultProtocol.canRollbackNew(1, all, true))
        assertFalse(RootPersistentDefaultProtocol.canRollbackNew(0, all, false))
        assertFalse(RootPersistentDefaultProtocol.canRollbackNew(0, null, true))
        assertFalse(RootPersistentDefaultProtocol.canRollbackNew(0, listOf("unknown"), true))
    }

    @Test fun clearDoesNotConfuseRemainingOrdinaryDefaultsWithPersistentPolicy() {
        val clear = JsonParser.parseString("""{"persistentMatches":0,"resolvedMatches":-1,"ownedFilters":0}""").asJsonObject
        assertEquals(RootPersistentDefaultProtocol.State(0, -1, 0), RootPersistentDefaultProtocol.state(clear))
        assertThrows(IllegalArgumentException::class.java) {
            RootPersistentDefaultProtocol.state(JsonParser.parseString("""{"persistentMatches":4294967296,"resolvedMatches":0,"ownedFilters":0}""").asJsonObject)
        }
    }
}
