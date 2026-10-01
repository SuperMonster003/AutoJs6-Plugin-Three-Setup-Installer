package io.github.supermonster003.autojs6.plugin.three.setup.installer.installedapps

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class InstalledAppsQueryTest {
    private val alpha = app("example.alpha", "Alpha", installed = 10, updated = 30)
    private val beta = app("example.beta", "Beta", installed = 30, updated = 10)
    private val system = app("example.system", "System", system = true, installed = 20, updated = 20)

    @Test fun `search matches labels and package names ignoring case and outer whitespace`() {
        val apps = listOf(beta, alpha, system)
        assertEquals(listOf(alpha), select(apps, InstalledAppsFilter("  ALpHa  ")))
        assertEquals(listOf(beta), select(apps, InstalledAppsFilter("EXAMPLE.BETA")))
        assertTrue(select(apps, InstalledAppsFilter("absent")).isEmpty())
        assertEquals(listOf(alpha, beta), select(apps, InstalledAppsFilter("  ")))
        assertEquals(listOf(beta, alpha, system), apps)
    }

    @Test fun `system apps require an explicit switch even when the query matches them`() {
        assertTrue(select(listOf(system), InstalledAppsFilter("system")).isEmpty())
        assertEquals(listOf(system), select(listOf(system), InstalledAppsFilter("system", showSystem = true)))
    }

    @Test fun `installation and update sorts place newest first with deterministic ties`() {
        val tie = alpha.copy(packageName = "example.zeta")
        val apps = listOf(tie, system, beta, alpha)
        assertEquals(listOf(beta, system, alpha, tie), select(apps,
            InstalledAppsFilter(sort = InstalledAppsSort.INSTALLED, showSystem = true)))
        assertEquals(listOf(alpha, tie, system, beta), select(apps,
            InstalledAppsFilter(sort = InstalledAppsSort.UPDATED, showSystem = true)))
        assertEquals(listOf(alpha, tie, beta, system), select(apps, InstalledAppsFilter(showSystem = true)))
    }

    @Test fun `alphabetical sorting uses the effective locale`() {
        val angstrom = app("example.angstrom", "Åke")
        val zed = app("example.zed", "Zed")
        val apps = listOf(angstrom, zed)
        assertEquals(listOf(angstrom, zed), InstalledAppsQuery.apply(apps, InstalledAppsFilter(), Locale.ENGLISH))
        assertEquals(listOf(zed, angstrom), InstalledAppsQuery.apply(apps, InstalledAppsFilter(), Locale.forLanguageTag("sv")))
    }

    @Test fun `obsolete work stops before returning stale search results`() {
        var checks = 0
        assertThrows(InterruptedException::class.java) {
            InstalledAppsQuery.apply(listOf(alpha, beta), InstalledAppsFilter(), Locale.ENGLISH) {
                checks++
                if (checks == 2) throw InterruptedException()
            }
        }
        assertEquals(2, checks)
    }

    @Test fun `unknown saved sort values safely restore to name`() {
        assertEquals(InstalledAppsSort.NAME, InstalledAppsSort.restore(null))
        assertEquals(InstalledAppsSort.NAME, InstalledAppsSort.restore("future-sort"))
        InstalledAppsSort.entries.forEach { assertEquals(it, InstalledAppsSort.restore(it.name)) }
    }

    private fun select(apps: List<InstalledApp>, filter: InstalledAppsFilter) = InstalledAppsQuery.apply(apps, filter, Locale.ENGLISH)
    private fun app(name: String, label: String, installed: Long = 0, updated: Long = 0, system: Boolean = false) =
        InstalledApp(name, label, "1.0", 1, installed, updated, system)
}
