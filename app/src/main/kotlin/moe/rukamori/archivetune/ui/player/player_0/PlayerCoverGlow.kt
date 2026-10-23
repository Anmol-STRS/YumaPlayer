
/*
 * YumaPlayer (2026) | Original work by MuwMix
 * https://github.com/MuwMix/YumaPlayer
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.player.player_0

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware

@Composable
fun PlayerCoverGlow(
    coverUrl: String?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!enabled || coverUrl.isNullOrBlank()) return

    val context = LocalContext.current
    val glowRequest = remember(coverUrl) {
        ImageRequest.Builder(context)
            .data(coverUrl)
            .memoryCacheKey("cover-glow:$coverUrl")
            .diskCacheKey("palette:$coverUrl")
            .size(128, 128)
            .allowHardware(true)
            .build()
    }

    val glowShape = remember { RoundedCornerShape(26.dp) }

    // Контейнер выполняет ровно 1 проход размытия на GPU
    Box(
        modifier = modifier
            .blur(
                radius = 32.dp,
                edgeTreatment = BlurredEdgeTreatment.Unbounded,
            )
            .graphicsLayer {
                // Плотность свечения у кромок без пережигания фона
                alpha = 0.95f
                scaleX = 1.02f
                scaleY = 1.02f
            }
    ) {
        AsyncImage(
            model = glowRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(glowShape),
        )
    }
}