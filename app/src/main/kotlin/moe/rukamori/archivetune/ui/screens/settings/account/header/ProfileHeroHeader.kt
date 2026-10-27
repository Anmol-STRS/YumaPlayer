/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.settings.account.header

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.ui.theme.yumaClickable
import kotlin.math.cos
import kotlin.math.sin

private data class StarPoint(val angle: Float, val scale: Float, val alpha: Float)

private val FrontOrbitStars = listOf(
    StarPoint(angle = -60f, scale = 1.0f, alpha = 0.50f),
    StarPoint(angle = 70f, scale = 0.66f, alpha = 0.35f),
    StarPoint(angle = 190f, scale = 0.50f, alpha = 0.225f)
)

private val BackOrbitStars = listOf(
    StarPoint(angle = 20f, scale = 0.55f, alpha = 0.22f),
    StarPoint(angle = 145f, scale = 0.42f, alpha = 0.165f),
    StarPoint(angle = 265f, scale = 0.50f, alpha = 0.193f)
)

@Composable
fun ProfileHeroHeader(
    avatarUrl: String?,
    displayName: String,
    handle: String?,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "heroOrbits")
    val frontRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 32000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "frontOrbitRotation"
    )
    val backRotation by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 48000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "backOrbitRotation"
    )

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        AvatarBackdropGlow(
            accentColor = accentColor,
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                OrbitRing(
                    stars = BackOrbitStars,
                    rotation = backRotation,
                    radius = 56.dp
                )

                AsyncImage(
                    model = avatarUrl,
                    placeholder = painterResource(R.drawable.person),
                    error = painterResource(R.drawable.person),
                    fallback = painterResource(R.drawable.person),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.18f),
                            shape = CircleShape
                        )
                )

                OrbitRing(
                    stars = FrontOrbitStars,
                    rotation = frontRotation,
                    radius = 48.dp
                )
            }

            Text(
                text = displayName.ifBlank { stringResource(R.string.account) },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (!handle.isNullOrBlank()) {
                Text(
                    text = if (handle.startsWith("@")) handle else "@$handle",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .height(36.dp)
                    .yumaClickable(pressedScale = 0.95f, onClick = onSettingsClick)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(50)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.settings),
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun OrbitRing(
    stars: List<StarPoint>,
    rotation: Float,
    radius: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .rotate(rotation),
        contentAlignment = Alignment.Center
    ) {
        stars.forEach { star ->
            val rad = Math.toRadians(star.angle.toDouble())
            val x = (cos(rad) * radius.value).dp
            val y = (sin(rad) * radius.value).dp
            val starSize = 12.dp * star.scale

            StarShapeIcon(
                size = starSize,
                color = Color.White.copy(alpha = star.alpha),
                modifier = Modifier.offset(x = x, y = y)
            )
        }
    }
}

@Composable
private fun StarShapeIcon(
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                val w = this.size.width
                val h = this.size.height
                val path = Path().apply {
                    moveTo(w * 0.5f, 0f)
                    cubicTo(w * 0.5375f, h * 0.308f, w * 0.692f, h * 0.4625f, w, h * 0.5f)
                    cubicTo(w * 0.692f, h * 0.5375f, w * 0.5375f, h * 0.692f, w * 0.5f, h)
                    cubicTo(w * 0.4625f, h * 0.692f, w * 0.308f, h * 0.5375f, 0f, h * 0.5f)
                    cubicTo(w * 0.308f, h * 0.4625f, w * 0.4625f, h * 0.308f, w * 0.5f, 0f)
                    close()
                }
                drawPath(path = path, color = color, style = Fill)
            }
    )
}