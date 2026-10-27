/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.settings.account.header

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

internal class ProfileMeshGlowState(
    val primaryColor: Color,
    val tertiaryColor: Color,
    val timeState: State<Float>,
    val isDark: Boolean,
)

@Composable
internal fun rememberProfileMeshGlowState(
    isLoggedIn: Boolean,
    extractedColorHex: String?,
): ProfileMeshGlowState {
    val extractedColor = remember(extractedColorHex) {
        extractedColorHex?.let {
            try {
                Color(android.graphics.Color.parseColor(it))
            } catch (e: Exception) {
                null
            }
        }
    }

    val targetPrimary = if (isLoggedIn && extractedColor != null) extractedColor else MaterialTheme.colorScheme.primary
    val targetTertiary = MaterialTheme.colorScheme.tertiary

    val animatedPrimary by animateColorAsState(
        targetValue = targetPrimary,
        animationSpec = tween(durationMillis = 400),
        label = "primaryGlowColor"
    )
    val animatedTertiary by animateColorAsState(
        targetValue = targetTertiary,
        animationSpec = tween(durationMillis = 400),
        label = "tertiaryGlowColor"
    )

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val transition = rememberInfiniteTransition(label = "profileMeshGlow")
    val time = transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    return remember(animatedPrimary, animatedTertiary, isDark, time) {
        ProfileMeshGlowState(
            primaryColor = animatedPrimary,
            tertiaryColor = animatedTertiary,
            timeState = time,
            isDark = isDark,
        )
    }
}

internal fun Modifier.profileMeshGlow(
    shape: Shape,
    glassBackground: Color,
    glassBorder: Color,
    glowState: ProfileMeshGlowState,
): Modifier = this
    .clip(shape)
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithCache {
        val time = glowState.timeState.value
        val isDark = glowState.isDark
        val blendMode = if (isDark) BlendMode.Plus else BlendMode.SrcOver

        val spot1Alpha = if (isDark) 0.22f else 0.18f
        val spot1AlphaMid = if (isDark) 0.08f else 0.05f

        val spot2Alpha = if (isDark) 0.25f else 0.20f
        val spot2AlphaMid = if (isDark) 0.09f else 0.06f

        val spot1X = size.width * (0.5f + 0.35f * kotlin.math.cos(time))
        val spot1Y = size.height * (0.5f + 0.30f * kotlin.math.sin(time))

        val spot2X = size.width * (0.5f + 0.40f * kotlin.math.sin(time + 1.8f))
        val spot2Y = size.height * (0.5f + 0.35f * kotlin.math.cos(time + 1.8f))

        val spot1Gradient = Brush.radialGradient(
            colors = listOf(
                glowState.primaryColor.copy(alpha = spot1Alpha),
                glowState.primaryColor.copy(alpha = spot1AlphaMid),
                Color.Transparent
            ),
            center = Offset(spot1X, spot1Y),
            radius = size.width * 0.85f
        )

        val spot2Gradient = Brush.radialGradient(
            colors = listOf(
                glowState.tertiaryColor.copy(alpha = spot2Alpha),
                glowState.tertiaryColor.copy(alpha = spot2AlphaMid),
                Color.Transparent
            ),
            center = Offset(spot2X, spot2Y),
            radius = size.width * 0.90f
        )

        onDrawBehind {
            drawRect(color = glassBackground)
            drawRect(brush = spot1Gradient, blendMode = blendMode)
            drawRect(brush = spot2Gradient, blendMode = blendMode)
        }
    }
    .border(1.dp, glassBorder, shape)
