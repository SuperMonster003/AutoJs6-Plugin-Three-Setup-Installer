package io.github.supermonster003.autojs6.plugin.three.setup.installer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory

/** Both Android 12 transfer routes must exclude the same complete set of app data domains. */
class SecurityConfigurationTest {
    @Test fun `backup and device transfer rules exclude every application data domain`() {
        val root = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main")) }
        val rules = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(root.resolve("app/src/main/res/xml/data_extraction_rules.xml").toFile()).documentElement
        assertEquals("data-extraction-rules", rules.tagName)
        assertEquals(setOf("cloud-backup", "device-transfer"), rules.elements().map { it.tagName }.toSet())
        val domains = setOf("root", "file", "database", "sharedpref", "external",
            "device_root", "device_file", "device_database", "device_sharedpref")
        rules.elements().forEach { mode ->
            val entries = mode.elements()
            assertEquals("Duplicate or missing exclusion in ${mode.tagName}", domains.size, entries.size)
            assertTrue("Includes would re-enable user data transfer", entries.all { it.tagName == "exclude" })
            assertEquals(domains, entries.map { it.getAttribute("domain") }.toSet())
            entries.forEach { assertEquals(".", it.getAttribute("path")) }
        }
    }

    private fun Element.elements(): List<Element> = (0 until childNodes.length)
        .map { childNodes.item(it) }.filterIsInstance<Element>()
}
