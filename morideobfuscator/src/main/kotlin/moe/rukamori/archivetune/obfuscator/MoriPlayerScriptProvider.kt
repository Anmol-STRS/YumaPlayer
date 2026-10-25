/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package moe.rukamori.archivetune.obfuscator

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.cipher.PlayerScriptProvider
import moe.rukamori.archivetune.cipher.RawPlayerScript
import java.io.File
import java.net.Proxy

class MoriPlayerScriptProvider internal constructor(
    private val client: PlayerScriptClient,
    private val store: CipherArtifactStore,
    private val refreshIntervalMillis: Long = MORI_CIPHER_REFRESH_INTERVAL_MILLIS,
    private val clock: () -> Long = System::currentTimeMillis,
    private val fetcher: suspend (String?) -> PlayerScript = { vid -> client.fetch(vid) {} },
) : PlayerScriptProvider {

    constructor(
        cacheDirectory: File,
        proxyProvider: () -> Proxy?,
        refreshIntervalMillis: Long = MORI_CIPHER_REFRESH_INTERVAL_MILLIS,
        clock: () -> Long = System::currentTimeMillis,
    ) : this(
        client = PlayerScriptClient(proxyProvider),
        store = CipherArtifactStore(cacheDirectory),
        refreshIntervalMillis = refreshIntervalMillis,
        clock = clock,
    )

    private class CachedEntry(
        val script: RawPlayerScript,
        val fetchedAtMillis: Long,
    )

    @Volatile
    private var memoryEntry: CachedEntry? = null
    private val mutex = Mutex()
    private var activeFetch: CompletableDeferred<RawPlayerScript>? = null

    override suspend fun getPlayerScript(videoId: String): RawPlayerScript =
        withContext(Dispatchers.IO) {
            val now = clock()
            memoryEntry?.takeIf { now - it.fetchedAtMillis < refreshIntervalMillis }?.let {
                return@withContext it.script
            }

            val (deferred, isInitiator) = mutex.withLock {
                val currentNow = clock()
                memoryEntry?.takeIf { currentNow - it.fetchedAtMillis < refreshIntervalMillis }?.let {
                    return@withContext it.script
                }
                activeFetch?.takeIf { !it.isCompleted }?.let { return@withLock it to false }
                CompletableDeferred<RawPlayerScript>().also { activeFetch = it } to true
            }

            if (!isInitiator) {
                return@withContext deferred.await()
            }

            try {
                val artifact = store.read()
                val diskNow = clock()
                if (
                    artifact != null &&
                    artifact.playerJavaScript.isNotBlank() &&
                    diskNow - artifact.plan.createdAtMillis < refreshIntervalMillis
                ) {
                    val script = RawPlayerScript(
                        playerId = artifact.plan.playerId,
                        source = artifact.playerJavaScript,
                        url = artifact.plan.playerUrl,
                    )
                    memoryEntry = CachedEntry(script, artifact.plan.createdAtMillis)
                    deferred.complete(script)
                    return@withContext script
                }

                val fetched = fetcher(videoId)
                val script = RawPlayerScript(
                    playerId = fetched.playerId,
                    source = fetched.source,
                    url = fetched.url,
                )
                memoryEntry = CachedEntry(script, clock())
                deferred.complete(script)
                script
            } catch (throwable: Throwable) {
                deferred.completeExceptionally(throwable)
                throw throwable
            } finally {
                withContext(NonCancellable) {
                    mutex.withLock {
                        if (activeFetch === deferred) {
                            activeFetch = null
                        }
                    }
                }
            }
        }
}
