package io.github.supermonster003.autojs6.plugin.three.setup.installer.settings

import android.content.Context
import android.content.Intent
import io.github.supermonster003.autojs6.plugin.three.setup.installer.ui.ReleaseHistoryActivity
import java.io.IOException
import java.util.Locale

/** Maps the UI locale to the bundled changelog asset shipped for it. */
internal object ReleaseHistoryAssetPolicy {
    fun assetFor(locale: Locale): String = when (locale.language.lowercase(Locale.ROOT)) {
        "zh" -> when {
            locale.script.equals("Hans", ignoreCase = true) -> "CHANGELOG-zh-Hans.md"
            locale.country.equals("HK", ignoreCase = true) ||
                locale.country.equals("MO", ignoreCase = true) -> "CHANGELOG-zh-Hant-HK.md"
            locale.script.equals("Hant", ignoreCase = true) ||
                locale.country.equals("TW", ignoreCase = true) -> "CHANGELOG-zh-Hant-TW.md"
            else -> "CHANGELOG-zh-Hans.md"
        }
        "ar" -> "CHANGELOG-ar.md"
        "es" -> "CHANGELOG-es.md"
        "fr" -> "CHANGELOG-fr.md"
        "ja" -> "CHANGELOG-ja.md"
        "ko" -> "CHANGELOG-ko.md"
        "ru" -> "CHANGELOG-ru.md"
        else -> "CHANGELOG-en.md"
    }

    /** The asset paths to try in order: the one for [locale], then English. */
    fun candidatesFor(locale: Locale): List<String> =
        listOf(assetFor(locale), FALLBACK).distinct().map { "doc/$it" }

    private const val FALLBACK = "CHANGELOG-en.md"
}

internal data class ReleaseHistoryItem(val tag: String?, val text: String)

internal data class ReleaseHistoryEntry(
    val version: String,
    val date: String?,
    val items: List<ReleaseHistoryItem>,
) {
    /** The items by category, each category once, in the order the categories first appear (untagged: tag null). */
    val groups: List<ReleaseHistoryGroup>
        get() = items.groupBy { it.tag }.map { (tag, grouped) -> ReleaseHistoryGroup(tag, grouped.map { it.text }) }

    /** How many items each category has, in the same order; untagged items are not counted. */
    val categoryCounts: List<Pair<String, Int>>
        get() = groups.mapNotNull { group -> group.tag?.let { it to group.items.size } }
}

internal data class ReleaseHistoryGroup(val tag: String?, val items: List<String>)

/**
 * Parses the narrow markdown dialect used by the bundled changelogs: `# vX.Y.Z` or `## vX.Y.Z` version
 * headings, `###### date` lines, and `` * `tag` text `` bullets. Anything else (the section title, the
 * separator lines) is ignored.
 */
internal object ReleaseHistoryParser {
    private val BULLET_PATTERN = Regex("^\\*\\s+(?:`([^`]+)`\\s*)?(.*)$")
    private val VERSION_PATTERN = Regex("^#{1,2}\\s+(v\\S+)\\s*$")

    fun parse(markdown: String): List<ReleaseHistoryEntry> {
        val entries = mutableListOf<ReleaseHistoryEntry>()
        var version: String? = null
        var date: String? = null
        var items = mutableListOf<ReleaseHistoryItem>()

        fun flush() {
            val flushedVersion = version ?: return
            entries.add(ReleaseHistoryEntry(flushedVersion, date, items.toList()))
        }

        for (rawLine in markdown.lineSequence()) {
            val line = rawLine.trim()
            val versionMatch = VERSION_PATTERN.matchEntire(line)
            when {
                versionMatch != null -> {
                    flush()
                    version = versionMatch.groupValues[1]
                    date = null
                    items = mutableListOf()
                }
                line.startsWith("###### ") -> date = line.removePrefix("###### ").trim()
                line.startsWith("* ") && version != null -> {
                    val match = BULLET_PATTERN.matchEntire(line) ?: continue
                    val tag = match.groupValues[1].takeIf { it.isNotEmpty() }
                    val text = match.groupValues[2].trim()
                    if (tag != null || text.isNotEmpty()) {
                        items.add(ReleaseHistoryItem(tag, text))
                    }
                }
            }
        }
        flush()
        return entries
    }
}

/**
 * Returns the releases of the first candidate asset that holds at least one, so a missing, blank or unparsable
 * localized changelog falls back to the English one; empty when none of them does (the page shows its empty state).
 */
internal fun loadReleaseHistory(candidates: List<String>, loader: (String) -> String): List<ReleaseHistoryEntry> {
    for (path in candidates) {
        val markdown = try { loader(path) } catch (_: IOException) { continue }
        val entries = ReleaseHistoryParser.parse(markdown)
        if (entries.isNotEmpty()) return entries
    }
    return emptyList()
}

/** A link of a release note, as the range of its text in [ReleaseHistoryInline.text] (Q160 A). */
internal data class ReleaseHistoryLink(val start: Int, val end: Int, val url: String)

/** The text of a release note without the link markup, with its links. */
internal data class ReleaseHistoryInline(val text: String, val links: List<ReleaseHistoryLink>)

/**
 * Turns the markdown links of a release note (`[text](https://...)`) into their text (plugin-center Q160 A). A pair of
 * `_` around a link (the italic spelling of the issue links) and a pair of backticks around its text go too, so
 * `` _[`issue #575`](url)_ `` reads "issue #575". Only http and https links count; a note without one stays as it is.
 */
internal object ReleaseHistoryInlineParser {
    private val LINK_PATTERN = Regex("""([_`]?)\[(`?)([^\]]+?)\2\]\((https?://[^\s)]+)\)\1""")

    fun parse(text: String): ReleaseHistoryInline {
        val matches = LINK_PATTERN.findAll(text).toList()
        if (matches.isEmpty()) return ReleaseHistoryInline(text, emptyList())
        val plain = StringBuilder()
        val links = mutableListOf<ReleaseHistoryLink>()
        var last = 0
        for (match in matches) {
            plain.append(text, last, match.range.first)
            val start = plain.length
            plain.append(match.groupValues[3])
            links.add(ReleaseHistoryLink(start, plain.length, match.groupValues[4]))
            last = match.range.last + 1
        }
        plain.append(text, last, text.length)
        return ReleaseHistoryInline(plain.toString(), links)
    }
}

/** Opens the release history page; the settings row and the update dialog both lead here. */
internal fun Context.openReleaseHistory() {
    startActivity(Intent(this, ReleaseHistoryActivity::class.java))
}
