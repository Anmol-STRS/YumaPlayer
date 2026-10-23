/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.playlist.auto

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.db.entities.Song
import moe.rukamori.archivetune.ui.theme.PlayerColorExtractor

@Composable
fun AutoPlaylistPaletteEffect(
    songs: List<Song>,
    context: Context,
    fallbackColor: Int,
    onColorsExtracted: (List<Color>) -> Unit,
) {
    LaunchedEffect(songs) {
        val thumbnailUrl = songs.firstOrNull()?.song?.thumbnailUrl
        if (thumbnailUrl != null) {
            val request =
                ImageRequest
                    .Builder(context)
                    .data(thumbnailUrl)
                    .size(
                        PlayerColorExtractor.Config.IMAGE_SIZE,
                        PlayerColorExtractor.Config.IMAGE_SIZE,
                    ).allowHardware(false)
                    .build()

            val result =
                runCatching {
                    context.imageLoader.execute(request)
                }.getOrNull()

            if (result != null) {
                val bitmap = result.image?.toBitmap()
                if (bitmap != null) {
                    val palette =
                        withContext(Dispatchers.Default) {
                            Palette
                                .from(bitmap)
                                .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                                .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
                                .generate()
                        }

                    val extractedColors =
                        PlayerColorExtractor.extractGradientColors(
                            palette = palette,
                            fallbackColor = fallbackColor,
                        )
                    onColorsExtracted(extractedColors)
                }
            }
        } else {
            onColorsExtracted(emptyList())
        }
    }
}

@Composable
fun rememberAutoPlaylistGradientColors(
    songs: List<Song>,
    context: Context = LocalContext.current,
    fallbackColor: Int = MaterialTheme.colorScheme.surface.toArgb(),
): List<Color> {
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    AutoPlaylistPaletteEffect(
        songs = songs,
        context = context,
        fallbackColor = fallbackColor,
    ) { gradientColors = it }
    return gradientColors
}
