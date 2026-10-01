package io.github.supermonster003.autojs6.plugin.three.setup.installer.source

import io.github.supermonster003.autojs6.plugin.three.setup.installer.engine.InstallFailure
import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes
import org.autojs.plugin.packagearchive.PackageDeviceSpec
import org.autojs.plugin.packagearchive.PackageInspectionLimits
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.zip.CRC32

class ArchiveOpenerTest {
    @get:Rule val temporary = TemporaryFolder()
    private val device = PackageDeviceSpec(35, listOf("arm64-v8a", "armeabi-v7a"), 440, listOf("en-US"))

    @Test fun `the pinned parser storage preflight is not mislabeled as an invalid archive`() {
        val original = IOException("Insufficient storage space to stage the package")
        val failure = ArchiveOpener.extractionFailure(original, "example.source")
        assertEquals(InstallerErrorCodes.INSUFFICIENT_STORAGE, failure.code)
        assertEquals("example.source", failure.packageName)
        assertSame(original, failure.cause)
        assertEquals(InstallerErrorCodes.INVALID_PACKAGE, ArchiveOpener.extractionFailure(
            IOException("Invalid entry: Insufficient storage space to stage the package"), null).code)
    }

    @Test fun `plain APK is recognized by content and retains its source digest`() {
        val file = file("renamed.bin", apk())
        val prepared = open(file)
        assertEquals("apk", prepared.format)
        assertEquals("example.source", prepared.packageName)
        assertEquals(42L, prepared.versionCode)
        assertTrue(prepared.installable)
        assertEquals(file, prepared.apks.single().file)
        assertEquals(sha256(file.readBytes()), prepared.apks.single().sha256)
        val json = ArchiveOpener.toInspectJson(prepared, null, null)
        assertEquals(file.length(), json["size"].asLong)
        assertEquals(24, json["minSdk"].asInt)
        assertEquals(35, json["targetSdk"].asInt)
        assertEquals("Fixture", json["label"].asString)
    }

    @Test fun `all generic containers extract only the device ABI density and language`() {
        val entries = arrayOf(
            "base.apk" to apk(),
            "split_config.arm64_v8a.apk" to apk(split = "config.arm64_v8a"),
            "split_config.x86.apk" to apk(split = "config.x86"),
            "split_config.hdpi.apk" to apk(split = "config.hdpi"),
            "split_config.xxhdpi.apk" to apk(split = "config.xxhdpi"),
            "split_config.en.apk" to apk(split = "config.en"),
            "split_config.fr.apk" to apk(split = "config.fr"),
        )
        for (extension in listOf("apks", "xapk", "apkm", "apkz", "zip")) {
            val metadata = when (extension) {
                "xapk" -> arrayOf("manifest.json" to "{\"package_name\":\"example.source\"}".toByteArray())
                "apkm" -> arrayOf("info.json" to "{\"package\":\"example.source\"}".toByteArray())
                "apkz" -> arrayOf("apkz.json" to "{\"package_name\":\"example.source\"}".toByteArray())
                else -> emptyArray()
            }
            val container = file("package.$extension", zip(*(entries + metadata)))
            val prepared = open(container)
            assertEquals(extension, prepared.format)
            assertTrue("$extension: ${prepared.problems}", prepared.installable)
            assertEquals(setOf(null, "config.arm64_v8a", "config.xxhdpi", "config.en"), prepared.apks.map { it.splitName }.toSet())
            assertEquals(4, prepared.splits.count { it.selected })
            assertEquals(3, prepared.splits.count { it.reason == "not for this device" })
            assertTrue(prepared.apks.all { it.file.isFile && it.file.parentFile.name == "apks" })
            assertEquals(sha256(entries.first().second), sha256(prepared.baseApk!!.file.readBytes()))
        }
    }

    @Test fun `source changing between initial digest and inspection is rejected`() {
        val original = apk(packageName = "example.source")
        val changed = apk(packageName = "example.change")
        assertEquals(original.size, changed.size)
        val file = file("mutable.apk", original)
        var checks = 0
        assertCode(InstallerErrorCodes.INVALID_PACKAGE) {
            ArchiveOpener.open(file, file.name, device, temporary.newFolder(), true) {
                // Initial check, first digest read, then its EOF check: change only after its
                // bytes were hashed and before the parser reads the replacement manifest.
                if (++checks == 3) file.writeBytes(changed)
            }
        }
    }

    @Test fun `bundletool toc chooses its declared base and ignores unrelated APKs`() {
        val container = file("bundle.apks", zip(
            "toc.pb" to singleBaseToc("splits/base-master.apk"),
            "splits/base-master.apk" to apk(),
            "unselected/broken.apk" to byteArrayOf(1, 2, 3),
        ))
        val prepared = open(container)
        assertTrue(prepared.installable)
        assertEquals(listOf("splits/base-master.apk"), prepared.splits.map { it.name })
        assertEquals(1, prepared.apks.size)
    }

    @Test fun `xapk metadata selects the matching application without trusting its version`() {
        val container = file("metadata.xapk", zip(
            "manifest.json" to "{\"package_name\":\"example.source\",\"version_code\":999}".toByteArray(),
            "base.apk" to apk(packageName = "example.other"),
            "real.apk" to apk(),
        ))
        val prepared = open(container)
        assertTrue(prepared.installable)
        assertEquals("example.source", prepared.packageName)
        assertEquals(42L, prepared.versionCode)
        assertEquals(listOf("real.apk"), prepared.splits.filter { it.selected }.map { it.name })
    }

    @Test fun `inspect lists splits without preparing every selected APK`() {
        val container = file("inspect.apkm", zip("base.apk" to apk(), "feature.apk" to apk(split = "feature.extra", feature = true)))
        val prepared = open(container, prepare = false)
        assertTrue(prepared.installable)
        assertTrue(prepared.apks.isEmpty())
        assertEquals(2, prepared.splits.size)
        assertTrue(prepared.displayApk!!.isFile)
        assertEquals(listOf("base", "feature"), prepared.splits.map { it.reason })
    }

    @Test fun `AAB keeps package version and modules while refusing installation`() {
        val container = file("app.aab", zip(
            "BundleConfig.pb" to byteArrayOf(),
            "base/manifest/AndroidManifest.xml" to aabManifest(),
            "feature/manifest/AndroidManifest.xml" to aabManifest(),
        ))
        for (prepare in listOf(false, true)) {
            val prepared = open(container, prepare)
            assertEquals("aab", prepared.format)
            assertEquals("example.source", prepared.packageName)
            assertEquals(42L, prepared.versionCode)
            assertEquals(listOf("base", "feature"), prepared.aabModules)
            assertFalse(prepared.installable)
            assertTrue(prepared.apks.isEmpty())
            assertEquals(InstallerErrorCodes.UNSUPPORTED_FORMAT, prepared.failure()!!.code)
            assertTrue(prepared.failure()!!.message!!.contains("bundletool"))
            assertEquals(2, ArchiveOpener.toInspectJson(prepared, null, null).getAsJsonArray("modules").size())
        }
    }

    @Test fun `broken ZIP and malformed nested APK return package errors`() {
        assertCode(InstallerErrorCodes.INVALID_PACKAGE) { open(file("broken.zip", byteArrayOf(1, 2, 3))) }
        val prepared = open(file("broken.xapk", zip("base.apk" to byteArrayOf(1, 2, 3))))
        assertFalse(prepared.installable)
        assertEquals(InstallerErrorCodes.INVALID_PACKAGE, prepared.failure()!!.code)
    }

    @Test fun `nonpackage ZIP remains inspectable as unknown and unsupported`() {
        val prepared = open(file("text.zip", zip("readme.txt" to byteArrayOf(1))))
        assertEquals("unknown", prepared.format)
        assertEquals(InstallerErrorCodes.UNSUPPORTED_FORMAT, prepared.failure()!!.code)
    }

    @Test fun `unsafe archive names never escape staging`() {
        for (path in listOf("../base.apk", "/base.apk", "C:/base.apk", "folder\\base.apk")) {
            assertCode(InstallerErrorCodes.INVALID_PACKAGE) { open(file("unsafe-${System.nanoTime()}.zip", zip(path to apk()))) }
        }
    }

    @Test fun `selected container APK count obeys the public 64 component limit`() {
        val entries = (listOf("base.apk" to apk()) + (1 until InstallerContract.MAX_SPLITS_PER_PACKAGE).map {
            "feature$it.apk" to apk(split = "feature$it", feature = true)
        }).toTypedArray()
        assertEquals(64, open(file("at-limit.apks", zip(*entries))).apks.size)
        assertCode(InstallerErrorCodes.INVALID_ARGUMENT) {
            open(file("over-limit.apks", zip(*entries, "extra.apk" to apk(split = "extra", feature = true))))
        }
    }

    @Test fun `generic container scan limit is checked before parsing nested APKs`() {
        val entries = (0..PackageInspectionLimits.GENERIC_APKS).map { "part$it.apk" to byteArrayOf(1) }.toTypedArray()
        assertCode(InstallerErrorCodes.INVALID_PACKAGE) { open(file("scan-limit.zip", zip(*entries))) }
    }

    @Test fun `a split collection orders the base and rechecks resolved dependencies`() {
        val base = open(file("base.apk", apk(usesSplit = "feature.extra", required = true)))
        val feature = open(file("feature.apk", apk(split = "feature.extra", feature = true)))
        assertFalse(base.installable)
        assertFalse(feature.installable)
        val combined = ArchiveOpener.combine(listOf(feature, base))
        assertTrue(combined.installable)
        assertEquals(listOf(null, "feature.extra"), combined.apks.map { it.splitName })
        assertEquals(base.apks.single().sha256, combined.apks.first().sha256)
        assertTrue(combined.problems.none { it.blocking })
    }

    @Test fun `split collection cannot hide missing uses-split or config parent`() {
        val base = open(file("base.apk", apk()))
        val depends = open(file("uses.apk", apk(split = "feature.child", feature = true, usesSplit = "feature.missing")))
        val config = open(file("config.apk", apk(split = "config.en", configFor = "feature.missing")))
        listOf(depends, config).forEach { part ->
            assertCode(InstallerErrorCodes.INVALID_PACKAGE) { ArchiveOpener.combine(listOf(base, part)) }
        }
    }

    @Test fun `container cannot select a configuration whose parent feature is missing`() {
        val container = file("missing-parent.apkm", zip(
            "base.apk" to apk(),
            "config.en.apk" to apk(split = "config.en", configFor = "feature.missing"),
        ))
        val prepared = open(container)
        assertFalse(prepared.installable)
        assertTrue(prepared.apks.isEmpty())
        assertEquals(InstallerErrorCodes.INVALID_PACKAGE, prepared.failure()!!.code)
        assertTrue(prepared.failure()!!.message!!.contains("feature.missing"))
    }

    @Test fun `split collection rejects missing base duplicates package mismatch and version mismatch`() {
        val base = open(file("base.apk", apk()))
        val extraBase = open(file("other-base.apk", apk()))
        val split = open(file("feature.apk", apk(split = "feature.extra", feature = true)))
        val otherPackage = open(file("package.apk", apk(split = "other", packageName = "example.other")))
        val otherVersion = open(file("version.apk", apk(split = "other", version = 43)))
        val missingVersion = open(file("no-version.apk", apk(split = "other", version = null)))
        listOf(listOf(split, split), listOf(base, extraBase), listOf(base, split, split), listOf(base, otherPackage),
            listOf(base, otherVersion), listOf(base, missingVersion)).forEach { parts ->
            assertCode(InstallerErrorCodes.INVALID_PACKAGE) { ArchiveOpener.combine(parts) }
        }
        assertCode(InstallerErrorCodes.INVALID_ARGUMENT) { ArchiveOpener.combine(List(65) { split }) }
        assertCode(InstallerErrorCodes.INVALID_ARGUMENT) { ArchiveOpener.combine(emptyList()) }
    }

    @Test fun `split collection rejects containers instead of silently nesting them`() {
        val base = open(file("base.apk", apk()))
        val container = open(file("nested.xapk", zip("base.apk" to apk())))
        assertCode(InstallerErrorCodes.INVALID_PACKAGE) { ArchiveOpener.combine(listOf(base, container)) }
    }

    @Test fun `unsupported ABI is reported distinctly from malformed packages`() {
        val prepared = open(file("wrong-abi.apks", zip("base.apk" to apk(), "split_config.x86.apk" to apk(split = "config.x86"))))
        assertFalse(prepared.installable)
        assertEquals(InstallerErrorCodes.INCOMPATIBLE_DEVICE, prepared.failure()!!.code)
    }

    @Test fun `UI selection may drop optional splits without replacing inspected bytes`() {
        val prepared = open(file("optional.apks", zip(
            "base.apk" to apk(),
            "feature.apk" to apk(split = "feature.extra", feature = true),
        )))
        assertTrue(prepared.installable)
        val base = requireNotNull(prepared.baseApk)
        val selected = ArchiveOpener.select(prepared, setOf(base.name))
        assertSame(base, selected.apks.single())
        assertEquals(prepared.packageName, selected.packageName)
        assertEquals(prepared.versionCode, selected.versionCode)
        assertEquals(prepared.sourceSize, selected.sourceSize)
        assertEquals(base.size, selected.totalBytes)
        assertEquals(prepared.displayApk, selected.displayApk)
        assertEquals(2, prepared.apks.size)
        assertTrue(prepared.apks.all { it.file.isFile })
        assertNull(selected.failure())
    }

    @Test fun `UI selection rejects an empty set and cannot omit the base APK`() {
        val prepared = open(file("must-keep-base.apks", zip(
            "base.apk" to apk(), "feature.apk" to apk(split = "feature.extra", feature = true),
        )))
        assertCode(InstallerErrorCodes.INVALID_ARGUMENT) { ArchiveOpener.select(prepared, emptySet()) }
        val feature = prepared.apks.single { it.splitName != null }
        assertCode(InstallerErrorCodes.INVALID_PACKAGE) { ArchiveOpener.select(prepared, setOf(feature.name)) }
    }

    @Test fun `UI selection cannot add unknown paths or APKs excluded by device matching`() {
        val prepared = open(file("device-selection.apks", zip(
            "base.apk" to apk(),
            "split_config.arm64_v8a.apk" to apk(split = "config.arm64_v8a"),
            "split_config.x86.apk" to apk(split = "config.x86"),
        )))
        assertTrue(prepared.installable)
        val base = requireNotNull(prepared.baseApk)
        val excluded = prepared.splits.single { !it.selected }.name
        for (added in listOf("uninspected.apk", "../replacement.apk", excluded)) {
            assertCode(InstallerErrorCodes.INVALID_ARGUMENT) { ArchiveOpener.select(prepared, setOf(base.name, added)) }
        }
        assertEquals(setOf(null, "config.arm64_v8a"), prepared.apks.map { it.splitName }.toSet())
    }

    @Test fun `UI selection rechecks uses split and configuration parent dependencies`() {
        val prepared = open(file("dependencies.apks", zip(
            "base.apk" to apk(),
            "parent.apk" to apk(split = "feature.parent", feature = true),
            "child.apk" to apk(split = "feature.child", feature = true, usesSplit = "feature.parent"),
            "config.en.apk" to apk(split = "config.en", configFor = "feature.child"),
        )))
        assertTrue("${prepared.problems}", prepared.installable)
        val all = prepared.apks.map { it.name }.toSet()
        fun name(split: String) = prepared.apks.single { it.splitName == split }.name
        assertCode(InstallerErrorCodes.INVALID_PACKAGE) { ArchiveOpener.select(prepared, all - name("feature.parent")) }
        assertCode(InstallerErrorCodes.INVALID_PACKAGE) { ArchiveOpener.select(prepared, all - name("feature.child")) }
        val independent = ArchiveOpener.select(prepared, all - name("feature.child") - name("config.en"))
        assertEquals(setOf(null, "feature.parent"), independent.apks.map { it.splitName }.toSet())
        assertNull(independent.failure())
    }

    @Test fun `UI selection cannot drop a dependency explicitly required by the base`() {
        val prepared = open(file("base-dependency.apks", zip(
            "base.apk" to apk(usesSplit = "feature.required"),
            "required.apk" to apk(split = "feature.required", feature = true),
            "optional.apk" to apk(split = "feature.optional", feature = true),
        )))
        assertTrue(prepared.installable)
        val selected = prepared.apks.filter { it.splitName != "feature.required" }.map { it.name }.toSet()
        assertCode(InstallerErrorCodes.INVALID_PACKAGE) { ArchiveOpener.select(prepared, selected) }
    }

    @Test fun `a base declaring required splits cannot be made standalone by UI selection`() {
        val prepared = open(file("split-required.apks", zip(
            "base.apk" to apk(required = true),
            "config.en.apk" to apk(split = "config.en"),
        )))
        assertTrue("${prepared.problems}", prepared.installable)
        assertCode(InstallerErrorCodes.INVALID_PACKAGE) { ArchiveOpener.select(prepared, setOf(requireNotNull(prepared.baseApk).name)) }
    }

    @Test fun `UI selection cannot clear an existing package inspection failure`() {
        val prepared = open(file("missing-dependency.apk", apk(usesSplit = "feature.missing")))
        assertFalse(prepared.installable)
        assertCode(InstallerErrorCodes.INVALID_PACKAGE) { ArchiveOpener.select(prepared, prepared.apks.map { it.name }.toSet()) }
    }

    private fun open(file: File, prepare: Boolean = true) = ArchiveOpener.open(file, file.name, device, temporary.newFolder(), prepare)
    private fun file(name: String, bytes: ByteArray) = temporary.newFile(name).apply { writeBytes(bytes) }
    private fun assertCode(code: String, block: () -> Unit) = assertEquals(code, assertThrows(InstallFailure::class.java) { block() }.code)
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun apk(
        packageName: String = "example.source", split: String? = null, version: Int? = 42,
        usesSplit: String? = null, configFor: String? = null, feature: Boolean = false, required: Boolean = false,
    ): ByteArray {
        val attributes = buildString {
            split?.let { append(" split=\"$it\"") }
            version?.let { append(" android:versionCode=\"$it\"") }
            configFor?.let { append(" android:configForSplit=\"$it\"") }
            if (feature) append(" android:isFeatureSplit=\"true\"")
            if (required) append(" android:isSplitRequired=\"true\"")
        }
        return zip("AndroidManifest.xml" to """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="$packageName" android:versionName="4.2"$attributes>
                <uses-sdk android:minSdkVersion="24" android:targetSdkVersion="35" />
                ${usesSplit?.let { "<uses-split android:name=\"$it\" />" }.orEmpty()}
                <application android:label="Fixture" />
            </manifest>
        """.trimIndent().toByteArray())
    }

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray = ByteArrayOutputStream().use { bytes ->
        ZipOutputStream(bytes).use { output -> entries.forEach { (name, content) ->
            output.putNextEntry(ZipEntry(name).apply {
                method = ZipEntry.STORED
                size = content.size.toLong()
                crc = CRC32().apply { update(content) }.value
            })
            output.write(content); output.closeEntry()
        } }
        bytes.toByteArray()
    }

    // Minimal wire fixtures follow the shared parser's AAPT2 / bundletool test schemas.
    private fun singleBaseToc(path: String): ByteArray = Proto().apply {
        string(4, "example.source")
        message(1) { message(2) {
            message(1) { string(1, "base"); varint(6, 1) }
            message(2) { string(2, path); message(3) { string(1, "base"); varint(2, 1) } }
        } }
    }.bytes()

    private fun aabManifest(): ByteArray = Proto().apply {
        message(1) {
            message(1) { string(1, "android"); string(2, "http://schemas.android.com/apk/res/android") }
            string(3, "manifest")
            message(4) { string(2, "package"); string(3, "example.source") }
            message(4) { string(1, "http://schemas.android.com/apk/res/android"); string(2, "versionCode"); string(3, "42") }
            message(4) { string(1, "http://schemas.android.com/apk/res/android"); string(2, "versionName"); string(3, "4.2") }
        }
    }.bytes()

    private class Proto {
        private val output = ByteArrayOutputStream()
        fun string(field: Int, value: String) = content(field, value.toByteArray())
        fun message(field: Int, block: Proto.() -> Unit) = content(field, Proto().apply(block).bytes())
        fun varint(field: Int, value: Int) { raw(field shl 3); raw(value) }
        private fun content(field: Int, value: ByteArray) { raw((field shl 3) or 2); raw(value.size); output.write(value) }
        private fun raw(value: Int) {
            var next = value
            while (next and -128 != 0) { output.write((next and 127) or 128); next = next ushr 7 }
            output.write(next)
        }
        fun bytes(): ByteArray = output.toByteArray()
    }
}
