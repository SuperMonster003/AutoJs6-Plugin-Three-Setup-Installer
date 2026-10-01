package io.github.supermonster003.autojs6.plugin.three.setup.installer.engine

import com.google.gson.JsonParser
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException

class InstallFailureTest {

    @Test
    fun `the error document carries the contract fields`() {
        val failure = InstallFailure(InstallerErrorCodes.INSTALL_FAILED, "boom", status = 4, systemMessage = "INSTALL_FAILED_VERSION_DOWNGRADE", packageName = "com.x")
        val json = JsonParser.parseString(failure.toJson().toString()).asJsonObject
        assertEquals("INSTALL_FAILED", json["code"].asString)
        assertEquals("boom", json["message"].asString)
        assertEquals(4, json["status"].asInt)
        assertEquals("INSTALL_FAILED_VERSION_DOWNGRADE", json["systemMessage"].asString)
        assertEquals("com.x", json["packageName"].asString)
        assertFalse(json["retryable"].asBoolean)
        assertTrue(InstallFailure(InstallerErrorCodes.TIMEOUT, "slow").retryable)
    }

    @Test
    fun `throwables map to contract codes and keep the package`() {
        assertEquals(InstallerErrorCodes.SOURCE_NOT_FOUND, InstallFailure.from(FileNotFoundException("x")).code)
        assertEquals(InstallerErrorCodes.SOURCE_UNREADABLE, InstallFailure.from(IOException("x")).code)
        assertEquals(InstallerErrorCodes.INVALID_ARGUMENT, InstallFailure.from(IllegalArgumentException("x")).code)
        assertEquals(InstallerErrorCodes.AUTHORIZER_UNAVAILABLE, InstallFailure.from(SecurityException("x")).code)
        assertEquals(InstallerErrorCodes.CANCELLED, InstallFailure.from(InterruptedException()).code)
        assertEquals(InstallerErrorCodes.INTERNAL, InstallFailure.from(IllegalStateException("x")).code)
        val known = InstallFailure(InstallerErrorCodes.USER_CANCELLED, "no")
        assertEquals(known, InstallFailure.from(known))
        assertEquals("com.y", InstallFailure.from(known, "com.y").packageName)
        assertEquals("com.x", InstallFailure.from(known.withPackage("com.x"), "com.y").packageName)
        assertNull(InstallFailure.from(IOException("x")).status)
    }

    @Test
    fun `rejected downgrade is an install failure while malformed packages remain invalid`() {
        val message = "INSTALL_FAILED_VERSION_DOWNGRADE: version 1 is older than 2"
        val failure = InstallStatusMapper.toFailure(4, message, "com.example.fixture")
        assertEquals(InstallerErrorCodes.INSTALL_FAILED, failure.code)
        assertEquals(4, failure.status)
        assertEquals(message, failure.systemMessage)
        assertEquals("com.example.fixture", failure.packageName)
        assertEquals(InstallerErrorCodes.INVALID_PACKAGE, InstallStatusMapper.toFailure(4, "INSTALL_PARSE_FAILED_BAD_MANIFEST", null).code)
        assertEquals(InstallerErrorCodes.INVALID_PACKAGE, InstallStatusMapper.toFailure(4, null, null).code)
        assertEquals(InstallerErrorCodes.UNINSTALL_FAILED, InstallStatusMapper.toFailure(4, message, null, uninstall = true).code)
    }

    @Test fun `generic OEM storage failures recognize only an exact legacy reason prefix`() {
        val reason = "INSTALL_FAILED_INSUFFICIENT_STORAGE: Failed to allocate internal storage"
        val failure = InstallStatusMapper.toFailure(InstallStatusMapper.STATUS_FAILURE, reason, "example.fixture")
        assertEquals(InstallerErrorCodes.INSUFFICIENT_STORAGE, failure.code)
        assertEquals(reason, failure.systemMessage)
        assertEquals(InstallStatusMapper.STATUS_FAILURE, failure.status)
        assertEquals(InstallerErrorCodes.INSTALL_FAILED, InstallStatusMapper.toFailure(1, "Invalid filename: $reason", null).code)
        assertEquals(InstallerErrorCodes.UNINSTALL_FAILED, InstallStatusMapper.toFailure(1, reason, null, uninstall = true).code)
    }

    @Test fun `arbitrary exception messages never masquerade as a storage errno`() {
        val text = "ENOSPC (No space left on device)"
        assertEquals(InstallerErrorCodes.SOURCE_UNREADABLE, InstallFailure.from(IOException(text)).code)
        assertEquals(InstallerErrorCodes.INVALID_ARGUMENT, InstallFailure.from(IllegalArgumentException(text)).code)
        val cyclic = IOException("a")
        val inner = IOException("b", cyclic)
        cyclic.initCause(inner)
        assertFalse(StorageErrors.isInsufficientStorage(cyclic))
    }
}
