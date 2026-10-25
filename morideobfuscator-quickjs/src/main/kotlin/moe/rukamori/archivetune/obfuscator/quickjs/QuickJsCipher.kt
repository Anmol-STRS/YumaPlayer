/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package moe.rukamori.archivetune.obfuscator.quickjs

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.obfuscator.quickjs.youtubei.YoutubeiDiskCache
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Collections
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QuickJsCipher(
    private val context: Context? = null,
    private val playerScriptProvider: QuickJsPlayerScriptProvider,
    private val executorService: ExecutorService =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "QuickJsCipherWorker").apply { isDaemon = true }
        },
    private val dispatcher: CoroutineDispatcher = executorService.asCoroutineDispatcher(),
) {
    private val diskCache = context?.let { YoutubeiDiskCache(it.applicationContext) }
    private val worker = QuickJsCipherWorker(context, diskCache, dispatcher)
    private val mutex = Mutex()
    private val knownPlayers = Collections.synchronizedSet(LinkedHashSet<String>())

    @Volatile
    private var closed = false

    suspend fun signatureTimestamp(videoId: String): Result<Int?> {
        if (closed) return Result.failure(IllegalStateException("QuickJsCipher is closed"))
        return try {
            val script = playerScriptProvider.getPlayerScript(videoId)
            currentCoroutineContext().ensureActive()
            val timestamp = extractSignatureTimestamp(script.source)
            Result.success(timestamp)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun transformN(videoId: String, url: String): Result<String> {
        if (closed) return Result.failure(IllegalStateException("QuickJsCipher is closed"))
        return try {
            val httpUrl = url.toHttpUrlOrNull()
                ?: throw IllegalArgumentException("Invalid URL: $url")
            val nParam = httpUrl.queryParameter("n")
            if (nParam == null) {
                return Result.success(url)
            }
            val script = playerScriptProvider.getPlayerScript(videoId)
            currentCoroutineContext().ensureActive()
            val transformed = executeDecipher(script = script, url = url, cipher = null)
            Result.success(transformed)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun resolveStreamUrl(videoId: String, cipher: String): Result<String> {
        if (closed) return Result.failure(IllegalStateException("QuickJsCipher is closed"))
        return try {
            val params = parseFormEncoded(cipher)
            val signature = params["s"]
            val directUrl = params["url"]
            val sp = params["sp"]?.takeIf(String::isNotBlank) ?: "signature"

            if (signature != null) {
                val streamUrl = directUrl
                    ?: throw IllegalArgumentException("signatureCipher is missing url parameter")
                streamUrl.toHttpUrlOrNull()
                    ?: throw IllegalArgumentException("Invalid stream url in signatureCipher: $streamUrl")

                val normalizedCipher = "s=" + URLEncoder.encode(signature, StandardCharsets.UTF_8.name()) +
                    "&sp=" + URLEncoder.encode(sp, StandardCharsets.UTF_8.name()) +
                    "&url=" + URLEncoder.encode(streamUrl, StandardCharsets.UTF_8.name())

                val script = playerScriptProvider.getPlayerScript(videoId)
                currentCoroutineContext().ensureActive()
                val resolved = executeDecipher(script = script, url = null, cipher = normalizedCipher)
                Result.success(resolved)
            } else if (directUrl != null) {
                transformN(videoId, directUrl)
            } else {
                val httpUrl = cipher.toHttpUrlOrNull()
                    ?: throw IllegalArgumentException("Input is neither a valid signatureCipher nor an HTTP(S) URL: $cipher")
                if (httpUrl.queryParameter("n") == null) {
                    Result.success(cipher)
                } else {
                    transformN(videoId, cipher)
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private suspend fun executeDecipher(
        script: QuickJsPlayerScript,
        url: String?,
        cipher: String?,
    ): String =
        mutex.withLock {
            if (closed) throw IllegalStateException("QuickJsCipher is closed")
            withContext(dispatcher) {
                currentCoroutineContext().ensureActive()
                val sourceHash = sha256(script.source)
                val compositePlayerId = "${script.playerId}_${sourceHash}_v$ADAPTER_VERSION"
                val isKnown = knownPlayers.contains(compositePlayerId)

                val request = JSONObject().put("playerId", compositePlayerId)
                if (!isKnown) {
                    request.put("playerSource", script.source)
                }
                if (url != null) request.put("url", url)
                if (cipher != null) request.put("cipher", cipher)

                val result = worker.decipherUrl(request.toString())
                recordKnownPlayer(compositePlayerId)
                result
            }
        }

    private fun recordKnownPlayer(playerId: String) {
        synchronized(knownPlayers) {
            if (knownPlayers.size >= MAX_KNOWN_PLAYERS) {
                val iterator = knownPlayers.iterator()
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
            knownPlayers.add(playerId)
        }
    }

    suspend fun close() =
        mutex.withLock {
            if (closed) return@withLock
            closed = true
            withContext(dispatcher) {
                worker.close()
            }
            executorService.shutdown()
        }

    companion object {
        private const val ADAPTER_VERSION = 1
        private const val MAX_KNOWN_PLAYERS = 100
        private val SIGNATURE_TIMESTAMP_REGEX = Regex("""signatureTimestamp\s*:\s*(\d{5,})""")

        fun extractSignatureTimestamp(source: String): Int {
            val matches = SIGNATURE_TIMESTAMP_REGEX.findAll(source).map { it.groupValues[1] }.toSet()
            if (matches.size != 1) {
                throw IllegalStateException("Could not extract player signature timestamp (candidates: ${matches.size})")
            }
            val value = matches.first().toIntOrNull()
                ?: throw IllegalStateException("Invalid signature timestamp value: ${matches.first()}")
            if (value <= 0) {
                throw IllegalStateException("Signature timestamp must be positive: $value")
            }
            return value
        }

        fun parseFormEncoded(query: String): Map<String, String> {
            val trimmed = query.trim().removePrefix("?")
            if (trimmed.isEmpty()) return emptyMap()
            val result = mutableMapOf<String, String>()
            for (token in trimmed.split("&")) {
                if (token.isEmpty()) continue
                val separator = token.indexOf('=')
                val rawKey = if (separator >= 0) token.substring(0, separator) else token
                val rawValue = if (separator >= 0) token.substring(separator + 1) else ""
                val key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8.name())
                val value = URLDecoder.decode(rawValue, StandardCharsets.UTF_8.name())
                if (key.isNotEmpty()) {
                    result[key] = value
                }
            }
            return result
        }

        private fun sha256(input: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(input.toByteArray(Charsets.UTF_8))
                .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }
}
