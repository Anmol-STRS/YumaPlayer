/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.settings.account.header

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import moe.rukamori.archivetune.R

@Composable
internal fun ProfileAvatarBadge(
    accountImageUrl: String?,
    isLoggedIn: Boolean,
    primaryColor: Color,
    tertiaryColor: Color,
    onAvatarPixelsReady: (IntArray) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd,
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.20f),
                            tertiaryColor.copy(alpha = 0.10f),
                        ),
                    )
                ).border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.70f),
                            primaryColor.copy(alpha = 0.20f),
                        ),
                    ),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            val avatarImageRequest =
                remember(accountImageUrl, context) {
                    val url = accountImageUrl
                    if (url.isNullOrBlank()) {
                        null
                    } else {
                        ImageRequest.Builder(context)
                            .data(url)
                            .memoryCacheKey(url)
                            .diskCacheKey(url)
                            .crossfade(true)
                            .allowHardware(false)
                            .build()
                    }
                }

            if (isLoggedIn && avatarImageRequest != null) {
                AsyncImage(
                    model = avatarImageRequest,
                    placeholder = painterResource(R.drawable.person),
                    error = painterResource(R.drawable.person),
                    fallback = painterResource(R.drawable.person),
                    contentDescription = null,
                    onSuccess = { success ->
                        val bmp = success.result.image.toBitmap()
                        val w = bmp.width
                        val h = bmp.height
                        if (w > 0 && h > 0) {
                            val pixels = IntArray(w * h)
                            bmp.getPixels(pixels, 0, w, 0, 0, w, h)
                            onAvatarPixelsReady(pixels)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    painter = painterResource(
                        if (isLoggedIn) R.drawable.account else R.drawable.login,
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = primaryColor,
                )
            }
        }

        AnimatedVisibility(
            visible = isLoggedIn,
            enter = scaleIn(spring(stiffness = Spring.StiffnessHigh)),
            exit = scaleOut(),
        ) {
            Surface(
                shape = CircleShape,
                color = primaryColor,
                modifier = Modifier.size(18.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}
