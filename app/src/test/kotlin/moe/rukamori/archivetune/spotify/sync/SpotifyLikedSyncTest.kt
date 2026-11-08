/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.spotify.sync

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import moe.rukamori.archivetune.constants.LikeSource
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.db.entities.Song
import moe.rukamori.archivetune.db.entities.SongEntity
import moe.rukamori.archivetune.db.entities.SpotifyMatchEntity
import moe.rukamori.archivetune.models.MediaMetadata
import moe.rukamori.archivetune.spotify.Spotify
import moe.rukamori.archivetune.spotify.SpotifyAccountSession
import moe.rukamori.archivetune.spotify.SpotifyLibraryRepository
import moe.rukamori.archivetune.spotify.SpotifyPlaybackResolver
import moe.rukamori.archivetune.spotify.models.SpotifyPaging
import moe.rukamori.archivetune.spotify.models.SpotifySavedTrack
import moe.rukamori.archivetune.spotify.models.SpotifyTrack
import moe.rukamori.archivetune.utils.SyncState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

class SpotifyLikedSyncTest {
    private val database = mockk<MusicDatabase>(relaxed = true)
    private val repository = mockk<SpotifyLibraryRepository>()
    private val state = mockk<SyncState>()
    private val sync = SpotifyLikedSync(state)
    private val updates = mutableListOf<SongEntity>()
    private var localSongs = listOf(likedSong("removed"))

    @Before
    fun setUp() {
        mockkObject(Spotify, SpotifyPlaybackResolver)
        every { state.database } returns database
        every { state.spotifyRepository } returns repository
        every { state.spotifyPlaylistSyncMutex } returns Mutex()
        every { state.syncGeneration } returns AtomicLong(0)
        every { state.isSyncStillEnabled(any()) } returns true
        coEvery { repository.restoreSession() } returns SpotifyAccountSession(isAuthenticated = true)
        coEvery { SpotifyPlaybackResolver.resolveToMetadata(any(), any(), any()) } returns null
        every { database.likedSongsByNameAsc(LikeSource.SPOTIFY) } answers { flowOf(localSongs) }
        every { database.getSpotifyMatchesByYouTubeIds(any()) } answers {
            localSongs.map { song ->
                SpotifyMatchEntity(
                    spotifyId = song.id.removePrefix("yt-"),
                    youtubeId = song.id,
                    title = song.title,
                    artist = "Artist",
                    matchScore = 1.0,
                )
            }
        }
        every { database.update(any<SongEntity>()) } answers { updates += firstArg<SongEntity>() }
        coEvery { database.withTransaction<Unit>(any()) } coAnswers {
            firstArg<suspend MusicDatabase.() -> Unit>().invoke(database)
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun firstPageFailurePreservesExistingLikes() = runTest {
        coEvery { Spotify.likedSongs(any(), any()) } returns Result.failure(IOException("offline"))

        sync.syncSpotifyLikedSongs(authoritative = true)

        assertTrue(updates.isEmpty())
        coVerify(exactly = 0) { database.withTransaction<Unit>(any()) }
    }

    @Test
    fun laterPageFailurePreservesExistingLikes() = runTest {
        coEvery { Spotify.likedSongs(any(), 0) } returns Result.success(page((0 until 50).map { "track-$it" }, total = 51))
        coEvery { Spotify.likedSongs(any(), 50) } returns Result.failure(IOException("rate limited"))

        sync.syncSpotifyLikedSongs(authoritative = true)

        assertTrue(updates.isEmpty())
        coVerify(exactly = 1) { Spotify.likedSongs(50, 50) }
    }

    @Test
    fun shortPageBeforeTotalPreservesExistingLikes() = runTest {
        coEvery { Spotify.likedSongs(any(), any()) } returns Result.success(page(listOf("present"), total = 2))

        sync.syncSpotifyLikedSongs(authoritative = true)

        assertTrue(updates.isEmpty())
    }

    @Test
    fun emptyPageBeforeTotalPreservesExistingLikes() = runTest {
        coEvery { Spotify.likedSongs(any(), any()) } returns Result.success(page(emptyList(), total = 1))

        sync.syncSpotifyLikedSongs(authoritative = true)

        assertTrue(updates.isEmpty())
    }

    @Test
    fun pageLimitPreservesLikesBeyondFetchedPages() = runTest {
        coEvery { Spotify.likedSongs(any(), any()) } answers {
            val offset = secondArg<Int>()
            Result.success(page((offset until offset + 50).map { "track-$it" }, total = 3001, offset = offset))
        }

        sync.syncSpotifyLikedSongs(authoritative = true)

        assertTrue(updates.isEmpty())
        coVerify(exactly = 60) { Spotify.likedSongs(any(), any()) }
    }

    @Test
    fun failedYouTubeMatchDoesNotRemoveRemoteLike() = runTest {
        localSongs = listOf(likedSong("present"))
        coEvery { Spotify.likedSongs(any(), any()) } returns Result.success(page(listOf("present")))

        sync.syncSpotifyLikedSongs(authoritative = true)

        assertTrue(updates.isEmpty())
        coVerify(exactly = 1) { SpotifyPlaybackResolver.resolveToMetadata(any(), database, false) }
    }

    @Test
    fun completeSnapshotRemovesOnlyTracksAbsentFromSpotify() = runTest {
        localSongs = listOf(likedSong("present"), likedSong("removed"))
        coEvery { Spotify.likedSongs(any(), any()) } returns Result.success(page(listOf("present")))

        sync.syncSpotifyLikedSongs(authoritative = true)

        assertEquals(listOf(localSongs[1].song.copy(liked = false, likedSpotify = false, likedDate = null)), updates)
    }

    @Test
    fun retainedSpotifyIdPreservesLikeWhenMultipleIdsShareYouTubeMatch() = runTest {
        localSongs = listOf(likedSong("shared"))
        every { database.getSpotifyMatchesByYouTubeIds(any()) } returns
            listOf("present", "removed").map { spotifyId ->
                SpotifyMatchEntity(
                    spotifyId = spotifyId,
                    youtubeId = "yt-shared",
                    title = "Shared song",
                    artist = "Artist",
                    matchScore = 1.0,
                )
            }
        coEvery { Spotify.likedSongs(any(), any()) } returns Result.success(page(listOf("present")))

        sync.syncSpotifyLikedSongs(authoritative = true)

        assertTrue(updates.isEmpty())
        coVerify(exactly = 1) { SpotifyPlaybackResolver.resolveToMetadata(any(), database, false) }
    }

    @Test
    fun completeTwoPageSnapshotRemovesAbsentTracks() = runTest {
        localSongs = listOf(likedSong("present"), likedSong("removed"))
        coEvery { Spotify.likedSongs(any(), 0) } returns
            Result.success(page((0 until 50).map { "track-$it" }, total = 51))
        coEvery { Spotify.likedSongs(any(), 50) } returns
            Result.success(page(listOf("present"), total = 51, offset = 50))

        sync.syncSpotifyLikedSongs(authoritative = true)

        assertEquals(listOf(localSongs[1].song.copy(liked = false, likedSpotify = false, likedDate = null)), updates)
        coVerify(exactly = 1) { Spotify.likedSongs(50, 50) }
    }

    @Test
    fun completeEmptyLibraryClearsSpotifyLikesAndPreservesYouTubeLikes() = runTest {
        localSongs = listOf(likedSong("spotify-only"), likedSong("both", likedYtm = true))
        coEvery { Spotify.likedSongs(any(), any()) } returns Result.success(page(emptyList()))

        sync.syncSpotifyLikedSongs(authoritative = true)

        assertEquals(
            listOf(
                localSongs[0].song.copy(liked = false, likedSpotify = false, likedDate = null),
                localSongs[1].song.copy(likedSpotify = false),
            ),
            updates,
        )
    }

    @Test
    fun nonAuthoritativeSyncNeverRemovesLikes() = runTest {
        coEvery { Spotify.likedSongs(any(), any()) } returns Result.success(page(emptyList()))

        sync.syncSpotifyLikedSongs(authoritative = false)

        assertTrue(updates.isEmpty())
    }

    @Test
    fun duplicateRemoteIdsDoNotProveSnapshotIsComplete() = runTest {
        coEvery { Spotify.likedSongs(any(), any()) } returns Result.success(page(listOf("present", "present")))

        sync.syncSpotifyLikedSongs(authoritative = true)

        assertTrue(updates.isEmpty())
    }

    @Test
    fun unexpectedOffsetPreservesExistingLikes() = runTest {
        coEvery { Spotify.likedSongs(any(), any()) } returns Result.success(page(listOf("present"), offset = 1))

        sync.syncSpotifyLikedSongs(authoritative = true)

        assertTrue(updates.isEmpty())
    }

    @Test
    fun changingTotalPreservesExistingLikes() = runTest {
        coEvery { Spotify.likedSongs(any(), 0) } returns Result.success(page((0 until 50).map { "track-$it" }, total = 51))
        coEvery { Spotify.likedSongs(any(), 50) } returns Result.success(page(emptyList(), total = 50, offset = 50))

        sync.syncSpotifyLikedSongs(authoritative = true)

        assertTrue(updates.isEmpty())
    }

    @Test
    fun incompleteSnapshotStillImportsResolvedLikes() = runTest {
        val newSong = likedSong("new").copy(song = likedSong("new").song.copy(liked = false, likedSpotify = false, likedDate = null))
        coEvery { Spotify.likedSongs(any(), any()) } returns Result.success(page(listOf("new"), total = 2))
        coEvery { SpotifyPlaybackResolver.resolveToMetadata(any(), any(), any()) } returns
            MediaMetadata(id = newSong.id, title = newSong.title, artists = emptyList(), duration = 180)
        every { database.getSongByIdBlocking(newSong.id) } returns newSong
        every { database.getSpotifyMatch("new") } returns null

        sync.syncSpotifyLikedSongs(authoritative = true)

        assertEquals(listOf(newSong.id), updates.map { it.id })
        assertTrue(updates.single().likedSpotify)
        assertTrue(updates.single().liked)
    }

    @Test
    fun cancellationIsPropagatedWithoutWritingLikes() = runTest {
        val cancellation = CancellationException("sync cancelled")
        coEvery { Spotify.likedSongs(any(), any()) } returns Result.failure(cancellation)

        try {
            sync.syncSpotifyLikedSongs(authoritative = true)
            fail("Cancellation must propagate to the caller")
        } catch (expected: CancellationException) {
            assertEquals(cancellation, expected)
        }
        assertTrue(updates.isEmpty())
    }

    private fun page(ids: List<String>, total: Int = ids.size, offset: Int = 0) =
        SpotifyPaging(
            items = ids.map { SpotifySavedTrack(track = SpotifyTrack(id = it, name = "Song $it")) },
            total = total,
            offset = offset,
            limit = 50,
        )

    private fun likedSong(id: String, likedYtm: Boolean = false) =
        Song(
            song = SongEntity(
                id = "yt-$id",
                title = "Song $id",
                liked = true,
                likedSpotify = true,
                likedYtm = likedYtm,
                likedDate = LocalDateTime.of(2026, 1, 1, 12, 0),
            ),
            artists = emptyList(),
        )
}
