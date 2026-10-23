/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.playlist.auto

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.TextFieldValue
import moe.rukamori.archivetune.db.entities.Song
import moe.rukamori.archivetune.ui.utils.ItemWrapper

@Stable
class AutoPlaylistSearchState(
    isSearchingState: MutableState<Boolean>,
    queryState: MutableState<TextFieldValue>,
    selectionState: MutableState<Boolean>,
    val focusRequester: FocusRequester,
    val wrappedSongs: SnapshotStateList<ItemWrapper<Song>>,
    val filteredSongs: List<ItemWrapper<Song>>,
    private val selectedCountState: State<Int>,
) {
    var isSearching by isSearchingState
    var query by queryState
    var selection by selectionState

    val selectedCount: Int
        get() = selectedCountState.value

    fun clearSelection() {
        selection = false
        wrappedSongs.forEach { it.isSelected = false }
    }

    fun toggleSelectAll() {
        if (selectedCount == wrappedSongs.size) {
            wrappedSongs.forEach { it.isSelected = false }
            selection = false
        } else {
            wrappedSongs.forEach { it.isSelected = true }
        }
    }

    fun openSearch() {
        isSearching = true
    }

    fun closeSearch(focusManager: FocusManager? = null) {
        isSearching = false
        query = TextFieldValue()
        focusManager?.clearFocus()
    }
}

@Composable
fun rememberAutoPlaylistSearchState(
    songs: List<Song>,
): AutoPlaylistSearchState {
    val isSearchingState = remember { mutableStateOf(false) }
    val queryState = remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchingState.value) {
        if (isSearchingState.value) {
            focusRequester.requestFocus()
        }
    }

    val wrappedSongs =
        remember(songs) {
            songs.map { item -> ItemWrapper(item) }.toMutableStateList()
        }

    val selectionState = remember { mutableStateOf(false) }
    val selectedCountState =
        remember(wrappedSongs) {
            derivedStateOf { wrappedSongs.count { it.isSelected } }
        }

    LaunchedEffect(selectionState.value, selectedCountState.value) {
        if (selectionState.value && selectedCountState.value == 0) {
            selectionState.value = false
        }
    }

    if (isSearchingState.value) {
        BackHandler {
            isSearchingState.value = false
            queryState.value = TextFieldValue()
        }
    } else if (selectionState.value) {
        BackHandler {
            selectionState.value = false
            wrappedSongs.forEach { it.isSelected = false }
        }
    }

    val query = queryState.value
    val filteredSongs =
        remember(wrappedSongs, query) {
            val searchQuery = query.text.trim()
            if (searchQuery.isEmpty()) {
                wrappedSongs
            } else {
                wrappedSongs.filter { wrapper ->
                    val song = wrapper.item
                    song.song.title.contains(searchQuery, true) ||
                        song.artists.any { it.name.contains(searchQuery, true) }
                }
            }
        }

    return remember(wrappedSongs, filteredSongs) {
        AutoPlaylistSearchState(
            isSearchingState = isSearchingState,
            queryState = queryState,
            selectionState = selectionState,
            focusRequester = focusRequester,
            wrappedSongs = wrappedSongs,
            filteredSongs = filteredSongs,
            selectedCountState = selectedCountState,
        )
    }
}
