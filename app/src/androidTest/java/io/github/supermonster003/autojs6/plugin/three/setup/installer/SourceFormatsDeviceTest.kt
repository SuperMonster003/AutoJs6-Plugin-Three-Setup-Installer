package io.github.supermonster003.autojs6.plugin.three.setup.installer

import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.PackageInspector
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.ArchiveOpener
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PackageSource
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.PackageStaging
import io.github.supermonster003.autojs6.plugin.three.setup.installer.source.SeekableSource
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.autojs.plugin.packagearchive.PackageDeviceSpec
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Read-only source and real binary-manifest format tests; never install or remove packages. */
@RunWith(AndroidJUnit4::class)
class SourceFormatsDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device get() = PackageDeviceSpec.from(context)

    @Test fun seekableDescriptorDoesNotCopyOrMoveTheHostOffset() = fixture { file, _ ->
        val directory = PackageStaging.newDirectory(context)
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { host ->
            try {
                Os.lseek(host.fileDescriptor, 17, OsConstants.SEEK_SET)
                PackageStaging.open(context, directory, PackageSource.Descriptor(host, file.name, file.length())).use { opened ->
                    assertFalse("A regular readable descriptor should have a direct view", opened.staged)
                    assertEquals("/proc/self/fd/", Os.readlink(opened.file.absolutePath).substringBeforeLast('/') + "/")
                    val prepared = ArchiveOpener.open(opened.file, opened.displayName, device, directory, true)
                    assertEquals(FIXTURE, prepared.packageName)
                    assertTrue(prepared.installable)
                    assertNotNull(prepared.apks.single().sha256)
                    assertEquals(17L, Os.lseek(host.fileDescriptor, 0, OsConstants.SEEK_CUR))
                    assertArrayEquals(file.readBytes(), opened.file.readBytes())
                }
                assertTrue(host.fileDescriptor.valid())
                assertTrue(directory.listFiles().orEmpty().isEmpty())
                assertTrue(file.isFile)
            } finally { PackageStaging.discard(directory) }
        }
    }

    @Test fun contentResolverInspectsMetadataAndCleansItsDescriptorView() = fixture { file, uri ->
        val before = stagingDirectories()
        val directory = PackageStaging.newDirectory(context)
        try {
            PackageStaging.open(context, directory, PackageSource.Content(uri)).use { opened ->
                assertEquals("fixture.apk", opened.displayName)
                assertEquals(file.length(), opened.size)
                assertFalse(opened.staged)
                assertNull(opened.ownedFile)
            }
        } finally { PackageStaging.discard(directory) }
        val inspected = PackageInspector(context).inspect(PackageSource.Content(uri))
        assertEquals("apk", inspected["format"].asString)
        assertEquals(FIXTURE, inspected["packageName"].asString)
        assertEquals(1L, inspected["versionCode"].asLong)
        assertEquals(file.length(), inspected["size"].asLong)
        assertEquals(before, stagingDirectories())
        assertTrue(file.isFile)
    }

    @Test fun nonSeekableContentIsCopiedOnceAndRemainsInspectable() = fixture { file, uri ->
        val pipeUri = uri.buildUpon().appendQueryParameter("pipe", "1").build()
        val directory = PackageStaging.newDirectory(context)
        val before = stagingDirectories() - directory.name
        var copied = 0L
        try {
            PackageStaging.open(context, directory, PackageSource.Content(pipeUri), progress = { copied = it }).use { opened ->
                assertEquals(SeekableSource.StagingReason.NOT_SEEKABLE, opened.stagingReason)
                assertArrayEquals(file.readBytes(), opened.file.readBytes())
                assertEquals(file.length(), copied)
                assertEquals(1, directory.listFiles().orEmpty().size)
                assertEquals(FIXTURE, ArchiveOpener.open(opened.file, opened.displayName, device, directory, false).packageName)
            }
        } finally { PackageStaging.discard(directory) }
        assertEquals(FIXTURE, PackageInspector(context).inspect(PackageSource.Content(pipeUri))["packageName"].asString)
        assertEquals(before, stagingDirectories())
    }

    @Test fun legacyFileUrisAndPathsKeepTheirOriginalFile() = fixture { file, _ ->
        listOf(Uri.fromFile(file), Uri.parse(file.absolutePath)).forEach { uri ->
            val source = PackageSource.fromUri(uri)
            assertTrue(source is PackageSource.LocalFile)
            val directory = PackageStaging.newDirectory(context)
            try {
                PackageStaging.open(context, directory, source).use { opened ->
                    assertEquals(file, opened.ownedFile)
                    assertFalse(opened.staged)
                    // Deleting the staging tree may only unlink the view, never follow it.
                    PackageStaging.discard(directory)
                    assertTrue(file.isFile)
                }
            } finally { PackageStaging.discard(directory) }
            assertEquals(FIXTURE, PackageInspector(context).inspect(source)["packageName"].asString)
            assertTrue(file.isFile)
        }
    }

    @Test fun inaccessibleProcReopenUsesPositionalStagingWithoutChangingTheHostOffset() = fixture { file, _ ->
        val directory = PackageStaging.newDirectory(context)
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { host ->
            try {
                Os.lseek(host.fileDescriptor, 17, OsConstants.SEEK_SET)
                // The held descriptor remains readable after inode permissions deny any new open.
                Os.chmod(file.absolutePath, 0)
                PackageStaging.open(context, directory, PackageSource.Descriptor(host, file.name, file.length())).use { opened ->
                    assertEquals(SeekableSource.StagingReason.DESCRIPTOR_REOPEN_UNAVAILABLE, opened.stagingReason)
                    assertEquals(FIXTURE, ArchiveOpener.open(opened.file, opened.displayName, device, directory, false).packageName)
                    assertEquals(17L, Os.lseek(host.fileDescriptor, 0, OsConstants.SEEK_CUR))
                }
                assertTrue(host.fileDescriptor.valid())
            } finally {
                Os.chmod(file.absolutePath, 384)
                PackageStaging.discard(directory)
            }
        }
        assertTrue(file.isFile)
    }

    @Test fun invalidSchemesAndSizeBoundsFailWithoutConsumingTheDescriptor() = fixture { file, _ ->
        val directory = PackageStaging.newDirectory(context)
        try {
            assertCode(InstallerErrorCodes.INVALID_ARGUMENT) { PackageSource.fromUri(Uri.parse("https://example.com/package.apk")) }
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { host ->
                listOf(-2L, PackageStaging.MAX_SOURCE_BYTES + 1).forEach { size ->
                    assertCode(InstallerErrorCodes.INVALID_ARGUMENT) { PackageStaging.open(context, directory, PackageSource.Descriptor(host, file.name, size)) }
                }
                listOf(0L, file.length() - 1, file.length() + 1).forEach { size ->
                    assertCode(InstallerErrorCodes.INVALID_PACKAGE) { PackageStaging.open(context, directory, PackageSource.Descriptor(host, file.name, size)) }
                }
                assertTrue(host.fileDescriptor.valid())
                assertEquals(0L, Os.lseek(host.fileDescriptor, 0, OsConstants.SEEK_CUR))
            }
            assertTrue(directory.listFiles().orEmpty().isEmpty())
        } finally { PackageStaging.discard(directory) }
    }

    @Test fun stalledPipeCancellationClosesOnlyOwnedDescriptorsAndDeletesPartialCopy() {
        val pipe = ParcelFileDescriptor.createPipe()
        val directory = PackageStaging.newDirectory(context)
        val started = SystemClock.elapsedRealtime()
        try {
            assertCode(InstallerErrorCodes.TIMEOUT) {
                PackageStaging.open(context, directory, PackageSource.Descriptor(pipe[0], "fixture.apk", -1), checkActive = {
                    if (SystemClock.elapsedRealtime() - started >= 200) throw InstallFailure(InstallerErrorCodes.TIMEOUT, "Source timed out")
                })
            }
            assertTrue(SystemClock.elapsedRealtime() - started < 3_000)
            assertTrue(pipe[0].fileDescriptor.valid())
            assertTrue(directory.listFiles().orEmpty().isEmpty())
        } finally { pipe.forEach { it.close() }; PackageStaging.discard(directory) }
    }

    @Test fun genericContainerFormatsSelectAndExtractRealBinaryManifestSplits() = fixture { file, _ ->
        val base = instrumentation.context.assets.open("core-fixtures/split-base.apk").use { it.readBytes() }
        val feature = instrumentation.context.assets.open("core-fixtures/split-feature.apk").use { it.readBytes() }
        listOf("apks", "xapk", "apkm", "apkz", "zip").forEach { format ->
            val container = File(file.parentFile, "package.$format")
            ZipOutputStream(container.outputStream()).use { zip ->
                listOf("base.apk" to base, "feature.apk" to feature).forEach { (name, content) ->
                    zip.putNextEntry(ZipEntry(name)); zip.write(content); zip.closeEntry()
                }
                val metadata = when (format) { "xapk" -> "manifest.json"; "apkm" -> "info.json"; "apkz" -> "apkz.json"; else -> null }
                if (metadata != null) {
                    zip.putNextEntry(ZipEntry(metadata)); zip.write("{\"package_name\":\"$SPLIT_FIXTURE\"}".toByteArray()); zip.closeEntry()
                }
            }
            val directory = PackageStaging.newDirectory(context)
            try {
                PackageStaging.open(context, directory, PackageSource.LocalFile(container)).use { opened ->
                    assertFalse(opened.staged)
                    val prepared = ArchiveOpener.open(opened.file, opened.displayName, device, directory, true)
                    assertEquals(format, prepared.format)
                    assertEquals(SPLIT_FIXTURE, prepared.packageName)
                    assertTrue("$format: ${prepared.problems}", prepared.installable)
                    assertEquals(setOf(null, "feature.extra"), prepared.apks.map { it.splitName }.toSet())
                    assertArrayEquals(base, prepared.baseApk!!.file.readBytes())
                    assertArrayEquals(feature, prepared.apks.single { it.splitName != null }.file.readBytes())
                }
            } finally { PackageStaging.discard(directory) }
            assertTrue(container.isFile)
        }
    }

    private fun fixture(test: (File, Uri) -> Unit) {
        val directory = File(context.cacheDir, "p2-source-fixtures-${UUID.randomUUID()}").apply { check(mkdir()) }
        val file = File(directory, "fixture.apk")
        try {
            instrumentation.context.assets.open("fixture-v1.apk").use { input -> file.outputStream().use { input.copyTo(it) } }
            val uri = Uri.Builder().scheme("content").authority("${context.packageName}.source-fixtures").appendPath(directory.name).appendPath(file.name).build()
            test(file, uri)
        } finally { directory.deleteRecursively() }
    }

    private fun assertCode(code: String, block: () -> Unit) = assertEquals(code, assertThrows(InstallFailure::class.java) { block() }.code)
    private fun stagingDirectories() = PackageStaging.root(context).listFiles().orEmpty().map { it.name }.toSet()

    companion object {
        private const val FIXTURE = "io.github.supermonster003.autojs6.installer.spike.fixture"
        private const val SPLIT_FIXTURE = "io.github.supermonster003.autojs6.installer.core.splits"
    }
}
