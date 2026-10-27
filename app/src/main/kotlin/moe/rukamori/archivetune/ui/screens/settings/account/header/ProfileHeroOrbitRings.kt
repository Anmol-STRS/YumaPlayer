/*
 * YumaPlayer (2026) | Original work by MuwMix
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.settings.account.header

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
internal fun ActiveBackOrbitRing(
    isPaused: Boolean,
    modifier: Modifier = Modifier,
) {
    val rotationProvider = rememberOrbitRotation(
        durationMillis = 48000L,
        startAngle = 360f,
        endAngle = 0f,
        isPaused = isPaused,
    )
    OrbitRing(
        stars = BackOrbitStars,
        rotationProvider = rotationProvider,
        radius = 56.dp,
        modifier = modifier,
    )
}

@Composable
internal fun ActiveFrontOrbitRing(
    isPaused: Boolean,
    modifier: Modifier = Modifier,
) {
    val rotationProvider = rememberOrbitRotation(
        durationMillis = 32000L,
        startAngle = 0f,
        endAngle = 360f,
        isPaused = isPaused,
    )
    OrbitRing(
        stars = FrontOrbitStars,
        rotationProvider = rotationProvider,
        radius = 48.dp,
        modifier = modifier,
    )
}

@Composable
private fun rememberOrbitRotation(
    durationMillis: Long,
    startAngle: Float,
    endAngle: Float,
    isPaused: Boolean,
): () -> Float {
    var currentRotation by rememberSaveable { mutableFloatStateOf(startAngle) }

    LaunchedEffect(durationMillis, startAngle, endAngle, isPaused) {
        if (isPaused) return@LaunchedEffect

        val sweep = endAngle - startAngle
        val speed = sweep / durationMillis.toFloat()
        var lastFrameMs = withInfiniteAnimationFrameMillis { it }

        while (true) {
            withInfiniteAnimationFrameMillis { frameMs ->
                val delta = (frameMs - lastFrameMs).coerceAtLeast(0L)
                lastFrameMs = frameMs
                currentRotation = startAngle + ((currentRotation - startAngle + delta * speed) % sweep)
            }
        }
    }
    return remember { { currentRotation } }
}

@Composable
private fun OrbitRing(
    stars: List<StarPoint>,
    rotationProvider: () -> Float,
    radius: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationZ = rotationProvider()
            },
        contentAlignment = Alignment.Center,
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
