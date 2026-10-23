/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.playlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import moe.rukamori.archivetune.db.entities.Playlist
import moe.rukamori.archivetune.innertube.models.PlaylistItem
import moe.rukamori.archivetune.innertube.models.SongItem

@Composable
fun rememberOnlinePlaylistUiState(
    playlist: PlaylistItem?,
    songs: List<SongItem>,
    viewCounts: Map<String, Int>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    error: String?,
    dbPlaylist: Playlist?,
    hasContinuation: Boolean,
): OnlinePlaylistUiState {
    val isBookmarked = dbPlaylist?.playlist?.bookmarkedAt != null
    return remember(
        playlist,
        songs,
        viewCounts,
        isLoading,
        isRefreshing,
        isLoadingMore,
        error,
        isBookmarked,
        hasContinuation,
    ) {
        OnlinePlaylistUiState(
            playlist = playlist,
            songs = songs,
            viewCounts = viewCounts,
            isLoading = isLoading,
            isRefreshing = isRefreshing,
            isLoadingMore = isLoadingMore,
            error = error,
            isBookmarked = isBookmarked,
            hasContinuation = hasContinuation,
        )
    }
}
