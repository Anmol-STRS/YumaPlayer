package moe.rukamori.archivetune.utils

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import moe.rukamori.archivetune.playback.resolvers.StreamUrl
import moe.rukamori.archivetune.playback.resolvers.StreamUrlCache as FlacStreamUrlCache
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class YouTubeStreamUrlCacheTest {

    private val mediaId = "testVideo123"
    private val authFingerprint = "fingerprint_user_1"
    private val streamUrl = "https://rr1---sn-test.googlevideo.com/videoplayback?expire=1800000000"
    private val itag = 251

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.getStackTraceString(any()) } answers { firstArg<Throwable>().stackTraceToString() }
        StreamUrlCache.clearPlaybackAuthCaches()
    }

    @After
    fun tearDown() {
        StreamUrlCache.clearPlaybackAuthCaches()
        unmockkStatic(Log::class)
    }

    @Test
    fun testSharedCacheHitAfterPrefetch() {
        StreamUrlCache.putCachedStreamUrl(
            mediaId = mediaId,
            url = streamUrl,
            itag = itag,
            expiresInSeconds = 300,
            authFingerprint = authFingerprint,
        )

        val cached = StreamUrlCache.getValidCachedStreamUrl(mediaId, authFingerprint)
        assertNotNull(cached)
        assertEquals(streamUrl, cached?.url)
        assertEquals(itag, cached?.itag)
        assertEquals(authFingerprint, cached?.authFingerprint)
    }

    @Test
    fun testAuthMismatchRejected() {
        StreamUrlCache.putCachedStreamUrl(
            mediaId = mediaId,
            url = streamUrl,
            itag = itag,
            expiresInSeconds = 300,
            authFingerprint = authFingerprint,
        )

        val differentAuth = "fingerprint_user_2"
        val cached = StreamUrlCache.getValidCachedStreamUrl(mediaId, differentAuth)
        assertNull(cached)
    }

    @Test
    fun testExpiryAndSafetyMargin() {
        val nowMs = 1_000_000L

        val safeEntry = StreamUrlCache.CachedStreamUrl(
            url = streamUrl,
            itag = itag,
            expiresAtMs = nowMs + StreamUrlCache.STREAM_URL_EXPIRY_SAFETY_MS + 10_000L,
            authFingerprint = authFingerprint,
        )
        assertEquals(true, safeEntry.isValidFor(authFingerprint, nowMs = nowMs))

        val unsafeEntry = StreamUrlCache.CachedStreamUrl(
            url = streamUrl,
            itag = itag,
            expiresAtMs = nowMs + StreamUrlCache.STREAM_URL_EXPIRY_SAFETY_MS,
            authFingerprint = authFingerprint,
        )
        assertEquals(false, unsafeEntry.isValidFor(authFingerprint, nowMs = nowMs))

        val expiredEntry = StreamUrlCache.CachedStreamUrl(
            url = streamUrl,
            itag = itag,
            expiresAtMs = nowMs - 1_000L,
            authFingerprint = authFingerprint,
        )
        assertEquals(false, expiredEntry.isValidFor(authFingerprint, nowMs = nowMs))
    }

    @Test
    fun testInvalidationRemovesTargetMediaOnly() {
        val otherMediaId = "otherVideo456"
        StreamUrlCache.putCachedStreamUrl(mediaId, streamUrl, itag, 300, authFingerprint)
        StreamUrlCache.putCachedStreamUrl(otherMediaId, streamUrl, itag, 300, authFingerprint)

        StreamUrlCache.invalidateCachedStreamUrls(mediaId)

        assertNull(StreamUrlCache.getCachedStreamUrl(mediaId))
        assertNotNull(StreamUrlCache.getCachedStreamUrl(otherMediaId))
    }

    @Test
    fun testClearPlaybackAuthCachesWipesAll() {
        StreamUrlCache.putCachedStreamUrl(mediaId, streamUrl, itag, 300, authFingerprint)
        StreamUrlCache.cachePoToken("session1", "player1", "session1")
        StreamUrlCache.markStreamClientFailed(mediaId, "ANDROID_VR", 403, authFingerprint)

        StreamUrlCache.clearPlaybackAuthCaches()

        assertNull(StreamUrlCache.getCachedStreamUrl(mediaId))
        assertNull(StreamUrlCache.getCachedPoToken("session1"))
        assertEquals(false, StreamUrlCache.isStreamClientTemporarilyBlocked(mediaId, "ANDROID_VR", authFingerprint))
    }

    @Test
    fun testSourceIsolationBetweenYouTubeAndFlac() {
        val flacCache = FlacStreamUrlCache()
        val flacUrl = StreamUrl("https://flac.example/stream", System.currentTimeMillis() + 300_000L, origin = "qobuz")

        StreamUrlCache.putCachedStreamUrl(mediaId, streamUrl, itag, 300, authFingerprint)
        flacCache.put(mediaId, flacUrl)

        StreamUrlCache.invalidateCachedStreamUrls(mediaId)

        assertNull(StreamUrlCache.getCachedStreamUrl(mediaId))
        assertEquals(flacUrl, flacCache.get(mediaId))
    }

    @Test
    fun testPutDoesNotExtendExpiryOnReput() {
        StreamUrlCache.putCachedStreamUrl(mediaId, streamUrl, itag, 300, authFingerprint)
        val initialExpiry = StreamUrlCache.getCachedStreamUrl(mediaId)?.expiresAtMs
        assertNotNull(initialExpiry)

        StreamUrlCache.putCachedStreamUrl(mediaId, streamUrl, itag, 600, authFingerprint)
        val rePutExpiry = StreamUrlCache.getCachedStreamUrl(mediaId)?.expiresAtMs

        assertEquals(initialExpiry, rePutExpiry)
    }

    @Test
    fun testExpiredUrlNotValidDespitePositiveExpiresInSeconds() {
        // Absolute expire timestamp in the past (e.g., expire=1000 => 1_000_000 ms)
        val expiredStreamUrl = "https://rr1---sn-test.googlevideo.com/videoplayback?expire=1000"
        StreamUrlCache.putCachedStreamUrl(
            mediaId = mediaId,
            url = expiredStreamUrl,
            itag = itag,
            expiresInSeconds = 300,
            authFingerprint = authFingerprint,
        )

        val cached = StreamUrlCache.getCachedStreamUrl(mediaId)
        assertNotNull(cached)
        // Entry's expiresAtMs must be capped by parsed absolute expiry (1_000_000L)
        assertEquals(1_000_000L, cached?.expiresAtMs)

        // getValidCachedStreamUrl must reject it
        val validCached = StreamUrlCache.getValidCachedStreamUrl(mediaId, authFingerprint)
        assertNull(validCached)
    }
}
