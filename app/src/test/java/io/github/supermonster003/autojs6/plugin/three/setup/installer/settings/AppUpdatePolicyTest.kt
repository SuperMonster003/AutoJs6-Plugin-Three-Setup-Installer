package io.github.supermonster003.autojs6.plugin.three.setup.installer.settings

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL

class AppUpdatePolicyTest {
    private fun document(tag: String = "v1.1.0", draft: Boolean = false, published: String = "\"2026-10-01T00:00:00Z\"") =
        """{"draft":$draft,"prerelease":false,"published_at":$published,"tag_name":"$tag","html_url":"${ReleaseInfoCodec.SOURCE}/releases/tag/$tag","body":"Notes"}"""

    @Test fun currentUnreleasedVersionCannotBecomeItsOwnUpdate() {
        assertFalse(AppVersionPolicy.isNewer("v1.0.0", "1.0.0"))
        assertTrue(AppVersionPolicy.isNewer("v1.1.0", "1.0.0"))
        assertTrue(AppVersionPolicy.isNewer("1.0.0", "1.0.0-rc.2"))
        assertTrue(AppVersionPolicy.isNewer("100000000000000000000.0.0", "2.0.0"))
        assertFalse(AppVersionPolicy.isNewer("garbage", "1.0.0"))
    }

    @Test fun ignoredVersionsNormalizePrefixesAndMetadata() {
        assertTrue(UpdateSchedulePolicy.ignored("v1.1.0+release", setOf("1.1.0")))
        assertFalse(UpdateSchedulePolicy.ignored("v1.2.0", setOf("1.1.0")))
        assertFalse(UpdateSchedulePolicy.ignored("broken", setOf("broken")))
    }

    @Test fun checksAreLimitedForTwelveHoursButClockRollbackDoesNotLockPermanently() {
        assertTrue(UpdateSchedulePolicy.due(null, 1_000))
        assertFalse(UpdateSchedulePolicy.due(1_000, 1_000 + UpdateSchedulePolicy.INTERVAL_MILLIS - 1))
        assertTrue(UpdateSchedulePolicy.due(1_000, 1_000 + UpdateSchedulePolicy.INTERVAL_MILLIS))
        assertTrue(UpdateSchedulePolicy.due(2_000, 1_000))
    }

    @Test fun releasesMustBePublishedStableAndBoundToThisRepository() {
        val release = ReleaseInfoCodec.decode(document())
        assertEquals(release, ReleaseInfoCodec.decode(ReleaseInfoCodec.encode(release)))
        assertThrows(IllegalArgumentException::class.java) { ReleaseInfoCodec.decode(document(draft = true)) }
        assertThrows(IllegalArgumentException::class.java) { ReleaseInfoCodec.decode(document(published = "null")) }
        assertThrows(IllegalArgumentException::class.java) { ReleaseInfoCodec.decode(document(tag = "v2.0.0-rc.1")) }
        assertFalse(ReleaseInfoCodec.validUrl("https://github.com/elsewhere/releases/tag/v1.1.0", "v1.1.0"))
        assertFalse(ReleaseInfoCodec.validUrl("${ReleaseInfoCodec.SOURCE}/releases/tag/v1.1.0?redirect=1", "v1.1.0"))
    }

    private class Connection(private val status: Int, private val body: ByteArray) : HttpURLConnection(URL(AppUpdateRepository.ENDPOINT)) {
        var disconnected = false
        override fun connect() = Unit
        override fun disconnect() { disconnected = true }
        override fun usingProxy() = false
        override fun getResponseCode() = status
        override fun getInputStream() = ByteArrayInputStream(body)
    }

    @Test fun repositoryHandlesUnpublishedRedirectsLimitsAndCancellationWithoutFollowingRemoteUrls() {
        val notPublished = Connection(404, byteArrayOf())
        assertEquals(UpdateResult.Success(null), AppUpdateRepository { notPublished }.fetchLatest(UpdateCancellation()))
        assertTrue(notPublished.disconnected)
        val redirected = Connection(302, byteArrayOf())
        assertEquals(UpdateResult.Failure, AppUpdateRepository { redirected }.fetchLatest(UpdateCancellation()))
        assertFalse(redirected.instanceFollowRedirects)
        assertEquals(10_000, redirected.readTimeout)
        val huge = Connection(200, ByteArray(256 * 1024 + 1) { 0x20 })
        assertEquals(UpdateResult.Failure, AppUpdateRepository { huge }.fetchLatest(UpdateCancellation()))
        var connected = false
        val stopped = UpdateCancellation().apply { cancel() }
        assertEquals(UpdateResult.Failure, AppUpdateRepository { connected = true; notPublished }.fetchLatest(stopped))
        assertFalse(connected)
    }
}
