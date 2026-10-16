/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.constants.ListItemHeight
import moe.rukamori.archivetune.constants.ListThumbnailSize
import moe.rukamori.archivetune.constants.ThumbnailCornerRadius
import moe.rukamori.archivetune.extensions.togglePlayPause
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.innertube.models.WatchEndpoint
import moe.rukamori.archivetune.innertube.pages.HomePage
import moe.rukamori.archivetune.innertube.pages.toPlaylistItem
import moe.rukamori.archivetune.playback.queues.YouTubeQueue
import moe.rukamori.archivetune.ui.component.PlaylistThumbnail
import moe.rukamori.archivetune.ui.component.YouTubeListItem
import moe.rukamori.archivetune.ui.menu.YouTubePlaylistMenu
import moe.rukamori.archivetune.ui.menu.YouTubeSongMenu
import moe.rukamori.archivetune.ui.settings.SettingsDimensions

@Composable
internal fun CommunityPlaylistSection(
    section: HomePage.Section,
    mediaContext: HomeFeedMediaContext,
    modifier: Modifier = Modifier,
) {
    if (section.featuredCards.isEmpty()) return

    val songsById = remember(section.items) {
        section.items.filterIsInstance<SongItem>().associateBy(SongItem::id)
    }
    val horizontalInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val startPadding = horizontalInsets.calculateStartPadding(layoutDirection) + 16.dp
    val endPadding = horizontalInsets.calculateEndPadding(layoutDirection) + 16.dp

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cardWidth = (maxWidth - 40.dp).coerceAtLeast(264.dp).coerceAtMost(520.dp)
        LazyRow(
            contentPadding = PaddingValues(start = startPadding, end = endPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = section.featuredCards,
                key = HomePage.Section.FeaturedCard::id,
                contentType = { "featured_playlist_card" },
            ) { card ->
                val songs = remember(card.itemIds, songsById) {
                    card.itemIds.mapNotNull(songsById::get).take(3)
                }
                CommunityPlaylistCard(
                    card = card,
                    songs = songs,
                    mediaContext = mediaContext,
                    modifier = Modifier.width(cardWidth),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CommunityPlaylistCard(
    card: HomePage.Section.FeaturedCard,
    songs: List<SongItem>,
    mediaContext: HomeFeedMediaContext,
    modifier: Modifier = Modifier,
) {
    val playlist = remember(card) { card.toPlaylistItem() }
    val openPlaylist = remember(playlist.id, mediaContext.navController) {
        { mediaContext.navController.navigate("online_playlist/${playlist.id}") }
    }
    val sequentialEndpoint = remember(card.playEndpoint, card.id) {
        card.playEndpoint ?: WatchEndpoint(playlistId = card.id)
    }
    val playPlaylist = remember(sequentialEndpoint, mediaContext.playerConnection) {
        { mediaContext.playerConnection.playQueue(YouTubeQueue.playlist(sequentialEndpoint)) }
    }
    val onRadioPlay = remember(card.radioEndpoint, mediaContext.playerConnection) {
        card.radioEndpoint?.let { endpoint ->
            { mediaContext.playerConnection.playQueue(YouTubeQueue.playlist(endpoint)) }
        }
    }
    val onShufflePlay = remember(card.shuffleEndpoint, mediaContext.playerConnection) {
        card.shuffleEndpoint?.let { endpoint ->
            { mediaContext.playerConnection.playQueue(YouTubeQueue.playlist(endpoint)) }
        }
    }
    val showPlaylistMenu = remember(playlist, songs, mediaContext.menuState, mediaContext.scope) {
        {
            mediaContext.menuState.show {
                YouTubePlaylistMenu(
                    playlist = playlist,
                    songs = songs,
                    coroutineScope = mediaContext.scope,
                    onDismiss = mediaContext.menuState::dismiss,
                )
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(SettingsDimensions.GlassBorderThickness, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(14.dp),
        ) {
            CommunityPlaylistCardHeader(
                card = card,
                songs = songs,
                onOpen = openPlaylist,
                onLongClick = showPlaylistMenu,
            )

            if (songs.isNotEmpty()) {
                CommunityPlaylistCardTrackRows(
                    card = card,
                    songs = songs,
                    mediaContext = mediaContext,
                )
            } else if (card.itemIds.isEmpty()) {
                CommunityPlaylistCardPlaceholderRows()
            }

            CommunityPlaylistCardActionsRow(
                onPlay = playPlaylist,
                onRadio = onRadioPlay,
                onShuffle = onShufflePlay,
                onMore = showPlaylistMenu,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommunityPlaylistCardHeader(
    card: HomePage.Section.FeaturedCard,
    songs: List<SongItem>,
    onOpen: () -> Unit,
    onLongClick: () -> Unit,
) {
    val coverThumbnails = remember(card.thumbnail, songs) {
        val isAvatar = card.thumbnail?.contains("yt3.") == true
        val songThumbs = songs.mapNotNull { it.thumbnail }.distinct()
        when {
            isAvatar && songThumbs.size >= 4 -> songThumbs.take(4)
            isAvatar && songThumbs.isNotEmpty() -> listOf(songThumbs.first())
            !card.thumbnail.isNullOrBlank() -> listOfNotNull(card.thumbnail)
            songThumbs.size >= 4 -> songThumbs.take(4)
            songThumbs.isNotEmpty() -> listOf(songThumbs.first())
            else -> emptyList()
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onOpen, onLongClick = onLongClick)
            .padding(2.dp),
    ) {
        PlaylistThumbnail(
            thumbnails = coverThumbnails,
            size = 96.dp,
            placeHolder = {
                Icon(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp),
                )
            },
            shape = RoundedCornerShape(ThumbnailCornerRadius),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            card.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommunityPlaylistCardTrackRows(
    card: HomePage.Section.FeaturedCard,
    songs: List<SongItem>,
    mediaContext: HomeFeedMediaContext,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
        songs.forEachIndexed { index, song ->
            val songEndpoint = remember(song.endpoint, song.id, card.id, index) {
                song.endpoint?.let { endpoint ->
                    endpoint.copy(playlistId = endpoint.playlistId ?: card.id, index = endpoint.index ?: index)
                } ?: WatchEndpoint(videoId = song.id, playlistId = card.id, index = index)
            }
            YouTubeListItem(
                item = song,
                isActive = song.id == mediaContext.mediaMetadata?.id,
                isPlaying = mediaContext.isPlaying,
                isSwipeable = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .combinedClickable(
                        onClick = {
                            if (song.id == mediaContext.mediaMetadata?.id) {
                                mediaContext.playerConnection.player.togglePlayPause()
                            } else {
                                mediaContext.playerConnection.playQueue(YouTubeQueue.playlist(songEndpoint))
                            }
                        },
                        onLongClick = {
                            mediaContext.menuState.show {
                                YouTubeSongMenu(
                                    song = song,
                                    navController = mediaContext.navController,
                                    onDismiss = mediaContext.menuState::dismiss,
                                )
                            }
                        },
                    ),
            )
        }
    }
}

@Composable
private fun CommunityPlaylistCardPlaceholderRows() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
        repeat(3) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(ListItemHeight).padding(horizontal = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(ListThumbnailSize)
                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.65f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(0.4f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunityPlaylistCardActionsRow(
    onPlay: () -> Unit,
    onRadio: (() -> Unit)?,
    onShuffle: (() -> Unit)?,
    onMore: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    ) {
        FilledIconButton(onClick = onPlay, modifier = Modifier.size(48.dp)) {
            Icon(painter = painterResource(R.drawable.play), contentDescription = stringResource(R.string.play))
        }
        if (onRadio != null) {
            FilledTonalIconButton(onClick = onRadio, modifier = Modifier.size(48.dp)) {
                Icon(painter = painterResource(R.drawable.radio), contentDescription = stringResource(R.string.start_radio))
            }
        }
        if (onShuffle != null) {
            FilledTonalIconButton(onClick = onShuffle, modifier = Modifier.size(48.dp)) {
                Icon(painter = painterResource(R.drawable.shuffle), contentDescription = stringResource(R.string.shuffle))
            }
        }
        FilledTonalIconButton(onClick = onMore, modifier = Modifier.size(48.dp)) {
            Icon(painter = painterResource(R.drawable.more_horiz), contentDescription = stringResource(R.string.more_label))
        }
    }
}
