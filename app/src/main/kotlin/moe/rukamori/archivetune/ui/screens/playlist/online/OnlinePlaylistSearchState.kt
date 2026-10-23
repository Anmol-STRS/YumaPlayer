/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.TextFieldValue
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.ui.utils.ItemWrapper

@Stable
class OnlinePlaylistSearchState(
    isSearchingState: MutableState<Boolean>,
    queryState: MutableState<TextFieldValue>,
    selectionState: MutableState<Boolean>,
    val focusRequester: FocusRequester,
    val wrappedSongs: SnapshotStateList<WrappedSongItem>,
) {
    var isSearching by isSearchingState
    var query by queryState
    var selection by selectionState

    val selectedSongsCount: Int
        get() = wrappedSongs.count { it.isSelected }

    fun toggleSelectAll() {
        val count = selectedSongsCount
        if (count == wrappedSongs.size) {
            wrappedSongs.forEach { it.isSelected = false }
        } else {
            wrappedSongs.forEach { it.isSelected = true }
        }
    }

    fun closeSearch() {
        isSearching = false
        query = TextFieldValue()
    }
}

@Composable
fun rememberOnlinePlaylistSearchState(songs: List<SongItem>): OnlinePlaylistSearchState {
    val isSearchingState = rememberSaveable { mutableStateOf(false) }
    val queryState = rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val selectionState = remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val query = queryState.value
    val filteredSongs =
        remember(songs, query) {
            filterPlaylistSongs(songs, query.text)
        }

    val wrappedSongs =
        remember(filteredSongs) { filteredSongs.map { item -> ItemWrapper(item) } }
            .toMutableStateList()

    if (isSearchingState.value) {
        BackHandler {
            isSearchingState.value = false
            queryState.value = TextFieldValue()
        }
    } else if (selectionState.value) {
        BackHandler { selectionState.value = false }
    }

    return remember(wrappedSongs) {
        OnlinePlaylistSearchState(
            isSearchingState = isSearchingState,
            queryState = queryState,
            selectionState = selectionState,
            focusRequester = focusRequester,
            wrappedSongs = wrappedSongs,
        )
    }
}

@Composable
fun OnlinePlaylistSearchFocusEffect(
    isSearching: Boolean,
    focusRequester: FocusRequester,
) {
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }
}
