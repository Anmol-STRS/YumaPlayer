/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package moe.rukamori.archivetune.obfuscator

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.Proxy
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class MoriPlayerScriptProviderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    @Test
    fun freshDiskHitReadsStoreWithoutCallingFetcherAndRetainsUrl() = runTest {
        val cacheDir = tempFolder.newFolder("cipher_cache")
        val store = CipherArtifactStore(cacheDir)
        val dummyJs = "function baseJs() { return 42; }"
        val dummyPlan = TransformPlan(
            playerId = "testPlayer123",
            playerUrl = "https://www.youtube.com/s/player/testPlayer123/base.js",
            sourceSha256 = sha256(dummyJs),
            signatureTimestamp = 19842,
            signatureProgram = null,
            signatureFunction = null,
            nProgram = null,
            nFunction = null,
            createdAtMillis = 10_000L,
        )
        store.write(CachedTransformArtifact(dummyPlan, dummyJs))

        val fetcherCalls = AtomicInteger(0)
        val clock = AtomicLong(15_000L)

        val provider = MoriPlayerScriptProvider(
            client = PlayerScriptClient { null },
            store = store,
            refreshIntervalMillis = MORI_CIPHER_REFRESH_INTERVAL_MILLIS,
            clock = { clock.get() },
            fetcher = {
                fetcherCalls.incrementAndGet()
                PlayerScript("networkPlayer", "https://network.url", "networkSource")
            },
        )

        val result = provider.getPlayerScript("someVideo")

        assertEquals(0, fetcherCalls.get())
        assertEquals("testPlayer123", result.playerId)
        assertEquals(dummyJs, result.source)
        assertEquals("https://www.youtube.com/s/player/testPlayer123/base.js", result.url)

        clock.set(10_000L + MORI_CIPHER_REFRESH_INTERVAL_MILLIS)
        val refreshed = provider.getPlayerScript("someVideo")
        assertEquals(1, fetcherCalls.get())
        assertEquals("networkPlayer", refreshed.playerId)
    }

    @Test
    fun expiredDiskHitTriggersNetworkFetcher() = runTest {
        val cacheDir = tempFolder.newFolder("cipher_cache_expired")
        val store = CipherArtifactStore(cacheDir)
        val dummyJs = "function oldJs() {}"
        val dummyPlan = TransformPlan(
            playerId = "oldPlayer",
            playerUrl = "https://www.youtube.com/old/base.js",
            sourceSha256 = sha256(dummyJs),
            signatureTimestamp = 100,
            signatureProgram = null,
            signatureFunction = null,
            nProgram = null,
            nFunction = null,
            createdAtMillis = 1_000L,
        )
        store.write(CachedTransformArtifact(dummyPlan, dummyJs))

        val fetcherCalls = AtomicInteger(0)
        val clock = AtomicLong(1_000L + MORI_CIPHER_REFRESH_INTERVAL_MILLIS + 5_000L)

        val provider = MoriPlayerScriptProvider(
            client = PlayerScriptClient { null },
            store = store,
            refreshIntervalMillis = MORI_CIPHER_REFRESH_INTERVAL_MILLIS,
            clock = { clock.get() },
            fetcher = {
                fetcherCalls.incrementAndGet()
                PlayerScript("freshPlayer", "https://fresh.url/base.js", "freshSource")
            },
        )

        val result = provider.getPlayerScript("someVideo")

        assertEquals(1, fetcherCalls.get())
        assertEquals("freshPlayer", result.playerId)
        assertEquals("freshSource", result.source)
        assertEquals("https://fresh.url/base.js", result.url)
    }

    @Test
    fun memoryCacheAvoidsDiskReadAndFetchUntilExpired() = runTest {
        val cacheDir = tempFolder.newFolder("cipher_cache_mem")
        val store = CipherArtifactStore(cacheDir)
        val fetcherCalls = AtomicInteger(0)
        val clock = AtomicLong(10_000L)

        val provider = MoriPlayerScriptProvider(
            client = PlayerScriptClient { null },
            store = store,
            refreshIntervalMillis = 60_000L,
            clock = { clock.get() },
            fetcher = {
                fetcherCalls.incrementAndGet()
                PlayerScript("memPlayer", "https://mem.url", "memSource_${fetcherCalls.get()}")
            },
        )

        val first = provider.getPlayerScript("vid")
        assertEquals(1, fetcherCalls.get())
        assertEquals("memSource_1", first.source)

        clock.addAndGet(30_000L)
        val second = provider.getPlayerScript("vid")
        assertEquals(1, fetcherCalls.get())
        assertEquals("memSource_1", second.source)

        clock.addAndGet(40_000L)
        val third = provider.getPlayerScript("vid")
        assertEquals(2, fetcherCalls.get())
        assertEquals("memSource_2", third.source)
    }

    @Test
    fun concurrentRequestsTriggerOnlySingleFlightFetch() = runTest {
        val cacheDir = tempFolder.newFolder("cipher_cache_concurrent")
        val store = CipherArtifactStore(cacheDir)
        val fetcherCalls = AtomicInteger(0)
        val fetchStarted = CompletableDeferred<Unit>()
        val allowComplete = CompletableDeferred<Unit>()

        val provider = MoriPlayerScriptProvider(
            client = PlayerScriptClient { null },
            store = store,
            clock = { 10_000L },
            fetcher = {
                fetcherCalls.incrementAndGet()
                fetchStarted.complete(Unit)
                allowComplete.await()
                PlayerScript("concurrentPlayer", "https://concurrent.url", "concurrentSource")
            },
        )

        val task1 = async { provider.getPlayerScript("v1") }
        val task2 = async { provider.getPlayerScript("v2") }
        val task3 = async { provider.getPlayerScript("v3") }

        fetchStarted.await()
        assertEquals(1, fetcherCalls.get())

        allowComplete.complete(Unit)

        val r1 = task1.await()
        val r2 = task2.await()
        val r3 = task3.await()

        assertEquals("concurrentPlayer", r1.playerId)
        assertEquals("concurrentPlayer", r2.playerId)
        assertEquals("concurrentPlayer", r3.playerId)
        assertEquals(1, fetcherCalls.get())
    }

    @Test
    fun cancellationReleasesAndDoesNotPoisonSingleFlight() = runTest {
        val cacheDir = tempFolder.newFolder("cipher_cache_cancel")
        val store = CipherArtifactStore(cacheDir)
        val fetcherCalls = AtomicInteger(0)

        val firstFetchStarted = CompletableDeferred<Unit>()
        val provider = MoriPlayerScriptProvider(
            client = PlayerScriptClient { null },
            store = store,
            clock = { 10_000L },
            fetcher = {
                val callNum = fetcherCalls.incrementAndGet()
                if (callNum == 1) {
                    firstFetchStarted.complete(Unit)
                    delay(5_000L)
                }
                PlayerScript("player_$callNum", "https://url_$callNum", "source_$callNum")
            },
        )

        val cancelledJob = launch {
            provider.getPlayerScript("cancelVid")
        }

        firstFetchStarted.await()
        cancelledJob.cancelAndJoin()

        val subsequent = provider.getPlayerScript("nextVid")
        assertEquals(2, fetcherCalls.get())
        assertEquals("player_2", subsequent.playerId)
    }
}
