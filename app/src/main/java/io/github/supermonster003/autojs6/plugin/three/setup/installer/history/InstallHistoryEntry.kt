package io.github.supermonster003.autojs6.plugin.three.setup.installer.history

import org.autojs.plugin.installer.api.InstallerContract
import org.autojs.plugin.installer.api.InstallerErrorCodes

/** Display facts only. No URI, descriptor, Intent, source bytes or executable request is stored. */
internal data class InstallHistoryEntry(
    val id: String,
    val token: String,
    val itemIndex: Int,
    val label: String,
    val packageName: String?,
    val versionName: String?,
    val versionCode: Long?,
    val previousVersionName: String?,
    val previousVersionCode: Long?,
    val result: String,
    val startedAt: Long,
    val updatedAt: Long,
    val finishedAt: Long?,
    val origin: String,
    val authorizer: String,
    val errorCode: String? = null,
    val systemMessage: String? = null,
    val durationMillis: Long? = null,
    val interrupted: Boolean = false,
) {
    val terminal: Boolean get() = result in InstallerContract.TERMINAL_STAGES

    fun interrupted(now: Long): InstallHistoryEntry = if (terminal) this else copy(
        result = InstallerContract.STAGE_CANCELLED, updatedAt = now.coerceAtLeast(startedAt),
        finishedAt = now.coerceAtLeast(startedAt), errorCode = InstallerErrorCodes.CANCELLED,
        systemMessage = null, interrupted = true,
    )

    companion object {
        const val MAX_ENTRIES = 200
        private val TOKEN = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        private val PACKAGE = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")
        val ORIGINS = setOf(InstallerContract.SOURCE_HOST, InstallerContract.SOURCE_SCRIPT,
            InstallerContract.SOURCE_EXTERNAL, InstallerContract.SOURCE_HOME)

        fun validToken(value: String): Boolean = TOKEN.matches(value)
        fun id(token: String, itemIndex: Int): String = "$token:$itemIndex"
        fun packageName(value: String?): String? = value?.takeIf { it.length <= 255 && PACKAGE.matches(it) }

        /** PackageManager errors may contain private file names; retain the diagnostic without paths. */
        fun text(value: String?, limit: Int): String? = value?.let {
            val safe = it.replace(Regex("[A-Za-z][A-Za-z0-9+.-]*://\\S+"), "...")
                // A filename is not restricted to ASCII. Quotes and diagnostic delimiters
                // bound a path, while Unicode letters, marks, symbols and spaces remain valid.
                .replace(Regex("""(?:[A-Za-z]:)?(?:[/\\][^/\\\p{Cntrl}"'<>|?:*]+){2,}[^\s"'<>]*"""), "...")
                .replace(Regex("[\\p{Cntrl}]"), " ").trim()
            if (safe.length <= limit) safe else safe.take(limit - 3).trimEnd { character -> Character.isHighSurrogate(character) } + "..."
        }

        fun validate(value: InstallHistoryEntry) {
            require(validToken(value.token) && value.itemIndex in 0 until InstallerContract.MAX_BATCH_SOURCES)
            require(value.id == id(value.token, value.itemIndex))
            require(value.label.length <= 256 && text(value.label, 256) == value.label)
            require(value.packageName == packageName(value.packageName))
            require(value.versionName == text(value.versionName, 128) && value.previousVersionName == text(value.previousVersionName, 128))
            require(value.result in InstallerContract.STAGES && value.origin in ORIGINS)
            require(InstallerContract.isAuthorizer(value.authorizer))
            require(value.startedAt > 0 && value.updatedAt >= value.startedAt)
            require(value.terminal == (value.finishedAt != null))
            require(value.finishedAt == null || value.finishedAt in value.startedAt..value.updatedAt)
            require(value.errorCode == null || InstallerErrorCodes.isKnown(value.errorCode))
            require(value.systemMessage == text(value.systemMessage, 1024))
            require(value.durationMillis == null || value.durationMillis >= 0)
            require(!value.interrupted || value.result == InstallerContract.STAGE_CANCELLED)
            require(value.result != InstallerContract.STAGE_COMPLETED || value.errorCode == null)
        }
    }
}
