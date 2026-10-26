/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.utils

import android.util.Log
import moe.rukamori.archivetune.constants.PlayerStreamClient
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.YouTubeClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

object StreamUrlCache {
    private const val logTag = "StreamUrlCache"
    const val FAILED_CLIENT_BACKOFF_MS = 10 * 60 * 1000L
    const val DEFAULT_STREAM_EXPIRE_SECONDS = 300
    const val STREAM_URL_EXPIRY_SAFETY_MS = 60_000L
    const val PO_TOKEN_VALIDITY_MS = 10 * 60 * 1000L

    data class CachedStreamUrl(
        val url: String,
        val itag: Int,
        val expiresAtMs: Long,
        val authFingerprint: String,
    ) {
        fun isValidFor(
            authFingerprint: String,
            nowMs: Long = System.currentTimeMillis(),
            minimumRemainingMs: Long = STREAM_URL_EXPIRY_SAFETY_MS,
        ): Boolean = this.authFingerprint == authFingerprint && expiresAtMs > nowMs + minimumRemainingMs

        fun toAuthScopedCacheValue(): AuthScopedCacheValue =
            AuthScopedCacheValue(
                url = url,
                expiresAtMs = expiresAtMs,
                authFingerprint = authFingerprint,
            )
    }

    data class CachedPoToken(
        val playerToken: String,
        val sessionToken: String,
        val expiresAtMs: Long,
    )

    private val streamUrlCache = ConcurrentHashMap<String, CachedStreamUrl>()
    private val failedStreamClientsUntil = ConcurrentHashMap<String, Long>()
    private val poTokenCache = ConcurrentHashMap<String, CachedPoToken>()

    @Volatile
    var lastSuccessfulClientKey: String? = null

    fun extractExpireTimestampMsFromUrl(url: String): Long? {
        val expireTimestamp =
            url
                .toHttpUrlOrNull()
                ?.queryParameter("expire")
                ?.toLongOrNull()
                ?: return null
        return expireTimestamp * 1000L
    }

    fun extractExpireSecondsFromUrl(url: String): Int? {
        val expiresAtMs = extractExpireTimestampMsFromUrl(url) ?: return null
        val remaining = (expiresAtMs - System.currentTimeMillis()) / 1000L
        return remaining.toInt().takeIf { it > 0 }
    }

    fun resolveExpireSeconds(
        apiExpire: Int?,
        streamUrl: String?,
    ): Int {
        apiExpire?.let { return it }
        streamUrl?.let { url ->
            extractExpireSecondsFromUrl(url)?.let { fromUrl ->
                Timber.tag(logTag).w("Using expire time extracted from stream URL: ${fromUrl}s")
                return fromUrl
            }
        }
        Timber.tag(logTag).w("No expire time available from API or URL, using default: ${DEFAULT_STREAM_EXPIRE_SECONDS}s")
        return DEFAULT_STREAM_EXPIRE_SECONDS
    }

    fun getCachedPoToken(sessionId: String): CachedPoToken? {
        val cached = poTokenCache[sessionId] ?: return null
        if (cached.expiresAtMs < System.currentTimeMillis()) {
            poTokenCache.remove(sessionId)
            return null
        }
        return cached
    }

    fun cachePoToken(sessionId: String, playerToken: String, sessionToken: String) {
        poTokenCache[sessionId] = CachedPoToken(
            playerToken = playerToken,
            sessionToken = sessionToken,
            expiresAtMs = System.currentTimeMillis() + PO_TOKEN_VALIDITY_MS,
        )
    }

    fun getCachedStreamUrl(mediaId: String): CachedStreamUrl? {
        val res = streamUrlCache[mediaId]
        Timber.tag("StreamUrlCache").d("RAW_GET [$mediaId] -> ${if (res != null) "FOUND" else "NULL"}")
        return res
    }

    fun getValidCachedStreamUrl(
        mediaId: String,
        authFingerprint: String,
        nowMs: Long = System.currentTimeMillis(),
        minimumRemainingMs: Long = STREAM_URL_EXPIRY_SAFETY_MS,
    ): CachedStreamUrl? {
        val entry = streamUrlCache[mediaId]
        if (entry == null) {
            Timber.tag("StreamUrlCache").d("GET_VALID [$mediaId] -> NULL (no entry)")
            return null
        }
        val fpMatch = entry.authFingerprint == authFingerprint
        val timeOk = entry.expiresAtMs > nowMs + minimumRemainingMs
        Timber.tag("StreamUrlCache").d("GET_VALID [$mediaId] -> entryExists=true, fpMatch=$fpMatch (entry='${entry.authFingerprint}', query='$authFingerprint'), timeOk=$timeOk (exp=${entry.expiresAtMs}, need>${nowMs + minimumRemainingMs})")
        return entry.takeIf { fpMatch && timeOk }
    }

    fun putCachedStreamUrl(
        mediaId: String,
        url: String,
        itag: Int,
        expiresInSeconds: Int,
        authFingerprint: String,
    ) {
        val calculatedExpiryMs = System.currentTimeMillis() + (expiresInSeconds * 1000L)
        val urlExpiryMs = extractExpireTimestampMsFromUrl(url)
        val cappedExpiryMs = if (urlExpiryMs != null) {
            minOf(urlExpiryMs, calculatedExpiryMs)
        } else {
            calculatedExpiryMs
        }

        streamUrlCache.compute(mediaId) { _, existing ->
            val safeExpiresAtMs = if (existing != null && existing.url == url) {
                minOf(existing.expiresAtMs, cappedExpiryMs)
            } else {
                cappedExpiryMs
            }
            Timber.tag("StreamUrlCache").d("PUT [$mediaId]: itag=$itag, fp='$authFingerprint', exp=$safeExpiresAtMs")
            CachedStreamUrl(
                url = url,
                itag = itag,
                expiresAtMs = safeExpiresAtMs,
                authFingerprint = authFingerprint,
            )
        }
    }

    fun clearPlaybackAuthCaches() {
        val trace = Log.getStackTraceString(Throwable())
        Timber.tag("StreamUrlCache").e("CLEAR_ALL_AUTH CALLED! StackTrace:\n$trace")
        streamUrlCache.clear()
        failedStreamClientsUntil.clear()
        poTokenCache.clear()
        lastSuccessfulClientKey = null
    }

    fun clearStreamUrls() {
        val trace = Log.getStackTraceString(Throwable())
        Timber.tag("StreamUrlCache").e("CLEAR_STREAM_URLS CALLED! StackTrace:\n$trace")
        streamUrlCache.clear()
    }

    fun invalidateCachedStreamUrls(videoId: String) {
        Timber.tag("StreamUrlCache").d("INVALIDATE [$videoId]")
        streamUrlCache.remove(videoId)
    }

    fun isExpiredOrNearExpiredStreamUrl(
        url: String,
        nowMs: Long = System.currentTimeMillis(),
    ): Boolean {
        val expiresAtMs = extractExpireTimestampMsFromUrl(url) ?: return false
        return expiresAtMs <= nowMs + STREAM_URL_EXPIRY_SAFETY_MS
    }

    fun markStreamClientFailed(
        videoId: String,
        clientKey: String?,
        httpStatusCode: Int?,
        authFingerprint: String = YouTube.currentPlaybackAuthState().fingerprint,
    ) {
        if (httpStatusCode !in setOf(403, 404, 410, 416)) return
        val normalizedClientKey = normalizeStreamClientKey(clientKey)
        if (normalizedClientKey.isEmpty()) return
        failedStreamClientsUntil[buildFailedClientKey(videoId, normalizedClientKey, authFingerprint)] =
            System.currentTimeMillis() + FAILED_CLIENT_BACKOFF_MS
    }

    fun markPreferredClientFailed(
        videoId: String,
        client: PlayerStreamClient,
        httpStatusCode: Int?,
        authFingerprint: String = YouTube.currentPlaybackAuthState().fingerprint,
    ) {
        markStreamClientFailed(videoId, client.name, httpStatusCode, authFingerprint)
    }

    fun isStreamClientTemporarilyBlocked(
        videoId: String,
        clientKey: String?,
        authFingerprint: String,
    ): Boolean {
        val normalizedClientKey = normalizeStreamClientKey(clientKey)
        if (normalizedClientKey.isEmpty()) return false
        val key = buildFailedClientKey(videoId, normalizedClientKey, authFingerprint)
        val until = failedStreamClientsUntil[key] ?: return false
        if (until <= System.currentTimeMillis()) {
            failedStreamClientsUntil.remove(key)
            return false
        }
        return true
    }

    private fun normalizeStreamClientKey(clientKey: String?): String = StreamClientUtils.normalizeClientKey(clientKey)

    fun buildFailedClientKey(
        videoId: String,
        clientKey: String,
        authFingerprint: String,
    ): String = "$authFingerprint:$videoId:${normalizeStreamClientKey(clientKey)}"
}
