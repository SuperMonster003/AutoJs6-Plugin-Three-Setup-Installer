package io.github.supermonster003.autojs6.plugin.three.setup.installer.installedapps

import java.text.Collator
import java.util.Locale

/** Current-user metadata only; application icons are loaded separately when a row is visible. */
internal data class InstalledApp(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val versionCode: Long,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val system: Boolean,
)

internal enum class InstalledAppsSort {
    NAME, INSTALLED, UPDATED;

    companion object {
        fun restore(value: String?) = entries.firstOrNull { it.name == value } ?: NAME
    }
}

internal data class InstalledAppsFilter(
    val query: String = "",
    val sort: InstalledAppsSort = InstalledAppsSort.NAME,
    val showSystem: Boolean = false,
)

/** Filtering and sorting run on the list worker, using the page's effective language. */
internal object InstalledAppsQuery {
    fun apply(
        apps: List<InstalledApp>,
        filter: InstalledAppsFilter,
        locale: Locale,
        checkActive: () -> Unit = {},
    ): List<InstalledApp> {
        val query = filter.query.trim()
        val collator = Collator.getInstance(locale).apply { strength = Collator.SECONDARY }
        val names = Comparator<InstalledApp> { first, second ->
            val label = collator.compare(first.label, second.label)
            if (label != 0) label else first.packageName.compareTo(second.packageName)
        }
        val comparator = Comparator<InstalledApp> { first, second ->
            checkActive()
            val time = when (filter.sort) {
                InstalledAppsSort.NAME -> 0
                InstalledAppsSort.INSTALLED -> second.firstInstallTime.compareTo(first.firstInstallTime)
                InstalledAppsSort.UPDATED -> second.lastUpdateTime.compareTo(first.lastUpdateTime)
            }
            if (time != 0) time else names.compare(first, second)
        }
        return apps.filter { app ->
            checkActive()
            (filter.showSystem || !app.system) && (query.isEmpty() ||
                app.label.contains(query, ignoreCase = true) || app.packageName.contains(query, ignoreCase = true))
        }.sortedWith(comparator)
    }
}

/** An access-ordered, byte-budgeted cache. Oversized values are never retained. */
internal class BoundedIconCache<K, V>(private val maximumWeight: Int, private val weightOf: (V) -> Int) {
    private val entries = LinkedHashMap<K, Pair<V, Int>>(16, 0.75f, true)
    private var usedWeight = 0

    init { require(maximumWeight > 0) }

    @Synchronized operator fun get(key: K): V? = entries[key]?.first

    @Synchronized fun put(key: K, value: V) {
        val weight = weightOf(value)
        require(weight > 0)
        entries.remove(key)?.let { usedWeight -= it.second }
        if (weight > maximumWeight) return
        val iterator = entries.entries.iterator()
        while (usedWeight > maximumWeight - weight && iterator.hasNext()) {
            usedWeight -= iterator.next().value.second
            iterator.remove()
        }
        entries[key] = value to weight
        usedWeight += weight
    }

    @Synchronized fun clear() { entries.clear(); usedWeight = 0 }
    @Synchronized fun weight() = usedWeight
    @Synchronized fun size() = entries.size
}
