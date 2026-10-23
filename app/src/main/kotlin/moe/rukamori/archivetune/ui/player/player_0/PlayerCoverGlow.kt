
/*
 * YumaPlayer (2026) | Original work by MuwMix
 * https://github.com/MuwMix/YumaPlayer
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.player.player_0

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

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
            .size(256, 256)
            .build()
    }

    Box(modifier = modifier) {
        AsyncImage(
            model = glowRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .blur(
                    radius = 38.dp,
                    edgeTreatment = BlurredEdgeTreatment.Unbounded,
                )
                .graphicsLayer { alpha = 0.8f },
        )

        AsyncImage(
            model = glowRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .blur(
                    radius = 18.dp,
                    edgeTreatment = BlurredEdgeTreatment.Unbounded,
                )
                .graphicsLayer { alpha = 0.9f },
        )
    }
}
