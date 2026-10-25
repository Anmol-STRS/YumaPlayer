/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.canvas

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import moe.rukamori.archivetune.canvas.ArchiveTuneCanvas.hasUsableCanvasAnimation
import moe.rukamori.archivetune.canvas.models.CanvasArtwork
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasFacadeContractTest {

    @Test
    fun spotifySourceReturnsNullWithoutNetwork() {
        runBlocking {
            val result =
                ArchiveTuneCanvas.getBySongArtist(
                    song = "Test Song",
                    artist = "Test Artist",
                    source = CanvasSource.SPOTIFY,
                )
            assertNull(result)
        }
    }

    @Test
    fun unconfiguredTidalPathReturnsNullWithoutNetwork() {
        runBlocking {
            try {
                TidalCanvasProvider.initialize(null)
                assertFalse(TidalCanvasProvider.isHealthy())
                assertNull(TidalCanvasProvider.getBySongArtist("Unconfigured Track", "Unconfigured Artist"))
            } finally {
                TidalCanvasProvider.initialize(null)
            }
        }
    }

    @Test
    fun tidalVerticalSelectionThroughFacadeSkipsNetwork() {
        runBlocking {
            val result =
                ArchiveTuneCanvas.getBySongArtist(
                    song = "Test Track",
                    artist = "Test Artist",
                    source = CanvasSource.TIDAL,
                    requireVertical = true,
                )
            assertNull(result)
        }
    }

    @Test(expected = CancellationException::class)
    fun getBySongArtistPropagatesExternalCancellation() {
        runBlocking {
            val cancelledScope = CoroutineScope(Dispatchers.Unconfined + Job().apply { cancel() })
            cancelledScope.async {
                ArchiveTuneCanvas.getBySongArtist(
                    song = "Cancelled Test",
                    artist = "Cancelled Artist",
                    source = CanvasSource.ALL,
                )
            }.await()
        }
    }

    @Test
    fun executeProviderAttemptReturnsNullOnTimeoutWithoutThrowing() {
        runBlocking {
            val result =
                ArchiveTuneCanvas.executeProviderAttempt(
                    provider = CanvasSource.BETTER_LYRICS,
                    requireVertical = false,
                    timeoutMs = 20L,
                ) {
                    delay(100L)
                    CanvasArtwork(name = "Delayed", videoUrl = "https://example.com/video.mp4")
                }
            assertNull(result)
        }
    }

    @Test(expected = CancellationException::class)
    fun executeProviderAttemptPropagatesParentCancellation() {
        runBlocking {
            val cancelledScope = CoroutineScope(Dispatchers.Unconfined + Job().apply { cancel() })
            cancelledScope.async {
                ArchiveTuneCanvas.executeProviderAttempt(
                    provider = CanvasSource.BETTER_LYRICS,
                    requireVertical = false,
                ) {
                    CanvasArtwork(name = "Cancelled")
                }
            }.await()
        }
    }

    @Test
    fun hasUsableCanvasAnimationRejectsBlankOrMetadataOnlyArtwork() {
        val metadataOnly = CanvasArtwork(name = "Only Meta", artist = "Artist")
        assertFalse(metadataOnly.hasUsableCanvasAnimation(requireVertical = false))
        assertFalse(metadataOnly.hasUsableCanvasAnimation(requireVertical = true))

        val blankUrl = CanvasArtwork(name = "Blank", videoUrl = "   ", animated = "")
        assertFalse(blankUrl.hasUsableCanvasAnimation(requireVertical = false))
        assertFalse(blankUrl.hasUsableCanvasAnimation(requireVertical = true))
    }

    @Test
    fun hasUsableCanvasAnimationValidatesSquareAndVerticalUrls() {
        val squareArtwork =
            CanvasArtwork(
                name = "Square Track",
                videoUrl = "https://resources.tidal.com/videos/123/1280x1280.mp4",
            )
        assertTrue(squareArtwork.hasUsableCanvasAnimation(requireVertical = false))
        assertFalse(squareArtwork.hasUsableCanvasAnimation(requireVertical = true))

        val verticalArtwork =
            CanvasArtwork(
                name = "Vertical Track",
                videoUrl = "https://resources.tidal.com/videos/123/1280x1280.mp4",
                videoUrlVertical = "https://example.com/vertical.mp4",
            )
        assertTrue(verticalArtwork.hasUsableCanvasAnimation(requireVertical = false))
        assertTrue(verticalArtwork.hasUsableCanvasAnimation(requireVertical = true))

        val animatedVerticalArtwork =
            CanvasArtwork(
                name = "Animated Vertical",
                animatedVertical = "https://example.com/animated-vertical.m3u8",
            )
        assertFalse(animatedVerticalArtwork.hasUsableCanvasAnimation(requireVertical = false))
        assertTrue(animatedVerticalArtwork.hasUsableCanvasAnimation(requireVertical = true))
    }
}
