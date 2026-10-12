/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.sponsorblock

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import moe.rukamori.archivetune.constants.PlaybackSource
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SponsorBlockPlaybackControllerTest {
    private val observeSettings = mockk<ObserveSponsorBlockSettingsUseCase>()
    private val getSegments = mockk<GetSponsorBlockSegmentsUseCase>()
    private val controller = SponsorBlockPlaybackController(observeSettings, getSegments)

    private val defaultSettings =
        SponsorBlockSettings(
            enabled = true,
            categories = setOf(SponsorBlockCategory.SPONSOR),
            apiUrl = DEFAULT_SPONSOR_BLOCK_API_URL,
        )

    @Test
    fun sourceGate_blocksFlacAndLocalMediaFromSeeking() = runTest {
        val player = mockk<Player>(relaxed = true)
        val flacItem = MediaItem.Builder().setMediaId("dQw4w9WgXcQ").build()
        every { player.currentMediaItem } returns flacItem
        every { player.isPlaying } returns true
        every { player.playbackState } returns Player.STATE_READY
        every { player.currentPosition } returns 5_000L
        every { player.duration } returns 60_000L

        coEvery { observeSettings() } returns flowOf(defaultSettings)

        controller.attach(
            player = player,
            scope = this,
            playbackSourceProvider = { PlaybackSource.FLAC },
            isTransitionActive = { false },
        )

        advanceTimeBy(500)
        coVerify(exactly = 0) { getSegments(any(), any(), any()) }
        verify(exactly = 0) { player.seekTo(any()) }

        controller.detach()
    }

    @Test
    fun sourceGate_blocksLocalMediaIdFromSeeking() = runTest {
        val player = mockk<Player>(relaxed = true)
        val localItem = MediaItem.Builder().setMediaId("content://media/external/audio/1").build()
        every { player.currentMediaItem } returns localItem
        every { player.isPlaying } returns true
        every { player.playbackState } returns Player.STATE_READY

        coEvery { observeSettings() } returns flowOf(defaultSettings)

        controller.attach(
            player = player,
            scope = this,
            playbackSourceProvider = { PlaybackSource.YT_MUSIC },
            isTransitionActive = { false },
        )

        advanceTimeBy(500)
        coVerify(exactly = 0) { getSegments(any(), any(), any()) }
        verify(exactly = 0) { player.seekTo(any()) }

        controller.detach()
    }

    @Test
    fun activeSegment_seeksToSegmentEndForYtMusic() = runTest {
        val videoId = "dQw4w9WgXcQ"
        val player = mockk<Player>(relaxed = true)
        val item = MediaItem.Builder().setMediaId(videoId).build()
        every { player.currentMediaItem } returns item
        every { player.isPlaying } returns true
        every { player.playbackState } returns Player.STATE_READY
        every { player.currentPosition } returns 10_000L
        every { player.duration } returns 120_000L

        coEvery { observeSettings() } returns flowOf(defaultSettings)
        coEvery {
            getSegments(videoId, defaultSettings, PlaybackSource.YT_MUSIC)
        } returns listOf(SponsorBlockSegment(startMs = 5_000L, endMs = 25_000L))

        controller.attach(
            player = player,
            scope = this,
            playbackSourceProvider = { PlaybackSource.YT_MUSIC },
            isTransitionActive = { false },
        )

        advanceTimeBy(500)
        verify { player.seekTo(25_000L) }

        controller.detach()
    }

    @Test
    fun transitionActive_suppressesSeeking() = runTest {
        val videoId = "dQw4w9WgXcQ"
        val player = mockk<Player>(relaxed = true)
        val item = MediaItem.Builder().setMediaId(videoId).build()
        every { player.currentMediaItem } returns item
        every { player.isPlaying } returns true
        every { player.playbackState } returns Player.STATE_READY
        every { player.currentPosition } returns 10_000L
        every { player.duration } returns 120_000L

        coEvery { observeSettings() } returns flowOf(defaultSettings)
        coEvery {
            getSegments(videoId, defaultSettings, PlaybackSource.YT_MUSIC)
        } returns listOf(SponsorBlockSegment(startMs = 5_000L, endMs = 25_000L))

        controller.attach(
            player = player,
            scope = this,
            playbackSourceProvider = { PlaybackSource.YT_MUSIC },
            isTransitionActive = { true },
        )

        advanceTimeBy(500)
        verify(exactly = 0) { player.seekTo(any()) }

        controller.detach()
    }

    @Test
    fun manualSeek_triggersCooldownAndSuppressesImmediateReskip() = runTest {
        var virtualTime = 10_000L
        controller.clock = { virtualTime }

        val videoId = "dQw4w9WgXcQ"
        val player = mockk<Player>(relaxed = true)
        val listenerSlot = slot<Player.Listener>()
        every { player.addListener(capture(listenerSlot)) } returns Unit
        val item = MediaItem.Builder().setMediaId(videoId).build()
        every { player.currentMediaItem } returns item
        every { player.isPlaying } returns true
        every { player.playbackState } returns Player.STATE_READY
        every { player.currentPosition } returns 10_000L
        every { player.duration } returns 120_000L

        coEvery { observeSettings() } returns flowOf(defaultSettings)
        coEvery {
            getSegments(videoId, defaultSettings, PlaybackSource.YT_MUSIC)
        } returns listOf(SponsorBlockSegment(startMs = 5_000L, endMs = 25_000L))

        controller.attach(
            player = player,
            scope = this,
            playbackSourceProvider = { PlaybackSource.YT_MUSIC },
            isTransitionActive = { false },
        )

        val posInfo = mockk<Player.PositionInfo>(relaxed = true)
        listenerSlot.captured.onPositionDiscontinuity(posInfo, posInfo, Player.DISCONTINUITY_REASON_SEEK)

        advanceTimeBy(300)
        verify(exactly = 0) { player.seekTo(any()) }

        virtualTime += 2_000L
        advanceTimeBy(300)
        verify { player.seekTo(25_000L) }

        controller.detach()
    }

    @Test
    fun apiError_doesNotBreakPlayback() = runTest {
        val videoId = "dQw4w9WgXcQ"
        val player = mockk<Player>(relaxed = true)
        val item = MediaItem.Builder().setMediaId(videoId).build()
        every { player.currentMediaItem } returns item
        every { player.isPlaying } returns true
        every { player.playbackState } returns Player.STATE_READY

        coEvery { observeSettings() } returns flowOf(defaultSettings)
        coEvery {
            getSegments(videoId, defaultSettings, PlaybackSource.YT_MUSIC)
        } throws RuntimeException("Network timeout")

        controller.attach(
            player = player,
            scope = this,
            playbackSourceProvider = { PlaybackSource.YT_MUSIC },
            isTransitionActive = { false },
        )

        advanceTimeBy(500)
        verify(exactly = 0) { player.seekTo(any()) }

        controller.detach()
    }
}
