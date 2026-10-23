package moe.rukamori.archivetune.ui.player.player_0

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.ui.theme.ExtractedColors
import moe.rukamori.archivetune.ui.theme.PlayerColorExtractor

private val globalPaletteCache = object : LruCache<String, ExtractedColors>(128) {}
private val paletteCacheLock = Any()
@Composable
internal fun PlayerPaletteExtractor(
    targetUrl: String?,
    trackUrl: String,
    onColorsExtracted: (vibrant: Int, darkMuted: Int, gradient: Int) -> Unit
) {
    val context = LocalContext.current

    val paletteImageRequest = remember(targetUrl) {
        ImageRequest.Builder(context)
            .data(targetUrl)
            .apply {
                if (targetUrl != null) {
                    memoryCacheKey("palette:$targetUrl")
                    diskCacheKey("palette:$targetUrl")
                }
            }
            .size(128, 128)
            .allowHardware(false)
            .build()
    }

    LaunchedEffect(targetUrl, trackUrl) {
        if (targetUrl == null) return@LaunchedEffect

        val cached = synchronized(paletteCacheLock) { globalPaletteCache.get(targetUrl) }
        if (cached != null) {
            onColorsExtracted(cached.vibrant, cached.darkMuted, cached.gradient)
            return@LaunchedEffect
        }

        val result = runCatching {
            context.imageLoader.execute(paletteImageRequest)
        }.getOrNull()

        val bitmap = withContext(Dispatchers.IO) {
            runCatching { result?.image?.toBitmap() }.getOrNull()
        } ?: return@LaunchedEffect

        val colors = withContext(Dispatchers.Default) {
            PlayerColorExtractor.extractColors(bitmap)
        }

        synchronized(paletteCacheLock) {
            globalPaletteCache.put(targetUrl, colors)
        }
        onColorsExtracted(colors.vibrant, colors.darkMuted, colors.gradient)
    }
}