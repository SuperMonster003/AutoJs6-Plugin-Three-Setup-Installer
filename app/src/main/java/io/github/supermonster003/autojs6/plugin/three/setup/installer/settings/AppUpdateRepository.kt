package io.github.supermonster003.autojs6.plugin.three.setup.installer.settings

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal data class ReleaseInfo(val tag: String, val url: String, val notes: String, val publishedAt: String)
internal object ReleaseInfoCodec {
    const val REPOSITORY = "SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer"
    const val SOURCE = "https://github.com/$REPOSITORY"
    fun validUrl(url: String, tag: String) = runCatching { URI(url).let {
        it.scheme == "https" && it.host == "github.com" && it.port == -1 && it.userInfo == null &&
            it.query == null && it.fragment == null && it.path == "/$REPOSITORY/releases/tag/$tag"
    } }.getOrDefault(false)

    fun decode(text: String): ReleaseInfo {
        require(text.toByteArray(Charsets.UTF_8).size <= 256 * 1024)
        val value = JsonReader(StringReader(text)).use { reader ->
            reader.strictness = Strictness.STRICT
            JsonParser.parseReader(reader).asJsonObject.also { require(reader.peek() == JsonToken.END_DOCUMENT) }
        }
        fun string(key: String): String? = value.get(key)?.takeUnless { it.isJsonNull }?.let {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isString)
            it.asString
        }
        for (key in listOf("draft", "prerelease")) {
            val flag = requireNotNull(value.get(key))
            require(flag.isJsonPrimitive && flag.asJsonPrimitive.isBoolean && !flag.asBoolean)
        }
        val published = requireNotNull(string("published_at"))
        require(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z").matches(published))
        val position = java.text.ParsePosition(0)
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.ROOT).apply {
            isLenient = false
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.parse(published, position)
        require(timestamp != null && position.index == published.length)
        val tag = requireNotNull(string("tag_name")).also { require(AppVersionPolicy.parse(it)?.pre?.isEmpty() == true) }
        val url = requireNotNull(string("html_url")).also { require(validUrl(it, tag)) }
        return ReleaseInfo(tag, url, string("body").orEmpty().take(16 * 1024), published)
    }

    fun encode(release: ReleaseInfo): String = JsonObject().apply {
        addProperty("draft", false); addProperty("prerelease", false)
        addProperty("published_at", release.publishedAt)
        addProperty("tag_name", release.tag); addProperty("html_url", release.url); addProperty("body", release.notes)
    }.toString().also(::decode)
}

internal object UpdateSchedulePolicy {
    const val INTERVAL_MILLIS = 12L * 60 * 60 * 1000
    fun due(lastAttempt: Long?, now: Long) = lastAttempt == null || now < lastAttempt || now - lastAttempt >= INTERVAL_MILLIS
    fun ignored(tag: String, ignored: Set<String>) = ignored.any { AppVersionPolicy.isIgnored(tag, it) }
}

internal sealed class UpdateResult {
    data class Success(val release: ReleaseInfo?) : UpdateResult()
    data object Failure : UpdateResult()
}

internal class UpdateCancellation {
    private val stopped = AtomicBoolean()
    private val connection = AtomicReference<HttpURLConnection?>()
    val cancelled get() = stopped.get()
    fun attach(value: HttpURLConnection) { connection.set(value); if (cancelled) value.disconnect() }
    fun cancel() { stopped.set(true); connection.getAndSet(null)?.disconnect() }
}

/** One fixed API, no redirect, bounded UTF-8 response, cancellation and short socket timeouts. */
internal class AppUpdateRepository(private val connect: () -> HttpURLConnection = { URL(ENDPOINT).openConnection() as HttpURLConnection }) {
    fun fetchLatest(cancellation: UpdateCancellation): UpdateResult {
        if (cancellation.cancelled) return UpdateResult.Failure
        val connection = try { connect() } catch (_: Exception) { return UpdateResult.Failure }
        cancellation.attach(connection)
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.instanceFollowRedirects = false
        connection.useCaches = false
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        connection.setRequestProperty("User-Agent", "AutoJs6-Plugin-Three-Setup-Installer")
        return try {
            when (connection.responseCode) {
                404 -> UpdateResult.Success(null)
                200 -> connection.inputStream.use { input ->
                    val bytes = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (!cancellation.cancelled) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (bytes.size() + count > 256 * 1024) return UpdateResult.Failure
                        bytes.write(buffer, 0, count)
                    }
                    if (cancellation.cancelled) return UpdateResult.Failure
                    val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes.toByteArray())).toString()
                    UpdateResult.Success(ReleaseInfoCodec.decode(text))
                }
                else -> UpdateResult.Failure
            }
        } catch (_: Exception) { UpdateResult.Failure }
        finally { connection.disconnect() }
    }

    companion object { const val ENDPOINT = "https://api.github.com/repos/${ReleaseInfoCodec.REPOSITORY}/releases/latest" }
}
