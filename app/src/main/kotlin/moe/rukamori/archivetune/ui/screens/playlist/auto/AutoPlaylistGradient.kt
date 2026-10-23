/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.playlist.auto

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex

@Composable
internal fun AutoPlaylistGradientBackground(
    lazyListState: LazyListState,
    gradientColors: List<Color>,
    surfaceColor: Color,
    disableBlur: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!disableBlur && gradientColors.isNotEmpty()) {
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .fillMaxSize(0.55f)
                    .zIndex(-1f)
                    .drawBehind {
                        val alpha =
                            if (lazyListState.firstVisibleItemIndex == 0) {
                                (1f - (lazyListState.firstVisibleItemScrollOffset / 600f)).coerceIn(0f, 1f)
                            } else {
                                0f
                            }

                        if (alpha <= 0f) return@drawBehind

                        val width = size.width
                        val height = size.height

                        if (gradientColors.size >= 3) {
                            val c0 = gradientColors[0]
                            val c1 = gradientColors[1]
                            val c2 = gradientColors[2]
                            val c3 = gradientColors.getOrElse(3) { c0 }
                            val c4 = gradientColors.getOrElse(4) { c1 }

                            drawRect(
                                brush =
                                    Brush.radialGradient(
                                        colors =
                                            listOf(
                                                c0.copy(alpha = alpha * 0.75f),
                                                c0.copy(alpha = alpha * 0.4f),
                                                Color.Transparent,
                                            ),
                                        center = Offset(width * 0.5f, height * 0.15f),
                                        radius = width * 0.8f,
                                    ),
                            )
                            drawRect(
                                brush =
                                    Brush.radialGradient(
                                        colors =
                                            listOf(
                                                c1.copy(alpha = alpha * 0.55f),
                                                c1.copy(alpha = alpha * 0.3f),
                                                Color.Transparent,
                                            ),
                                        center = Offset(width * 0.1f, height * 0.4f),
                                        radius = width * 0.6f,
                                    ),
                            )
                            drawRect(
                                brush =
                                    Brush.radialGradient(
                                        colors =
                                            listOf(
                                                c2.copy(alpha = alpha * 0.5f),
                                                c2.copy(alpha = alpha * 0.25f),
                                                Color.Transparent,
                                            ),
                                        center = Offset(width * 0.9f, height * 0.35f),
                                        radius = width * 0.55f,
                                    ),
                            )
                            drawRect(
                                brush =
                                    Brush.radialGradient(
                                        colors =
                                            listOf(
                                                c3.copy(alpha = alpha * 0.35f),
                                                c3.copy(alpha = alpha * 0.18f),
                                                Color.Transparent,
                                            ),
                                        center = Offset(width * 0.25f, height * 0.65f),
                                        radius = width * 0.75f,
                                    ),
                            )
                            drawRect(
                                brush =
                                    Brush.radialGradient(
                                        colors =
                                            listOf(
                                                c4.copy(alpha = alpha * 0.3f),
                                                c4.copy(alpha = alpha * 0.15f),
                                                Color.Transparent,
                                            ),
                                        center = Offset(width * 0.55f, height * 0.85f),
                                        radius = width * 0.9f,
                                    ),
                            )
                        } else {
                            drawRect(
                                brush =
                                    Brush.radialGradient(
                                        colors =
                                            listOf(
                                                gradientColors[0].copy(alpha = alpha * 0.7f),
                                                gradientColors[0].copy(alpha = alpha * 0.35f),
                                                Color.Transparent,
                                            ),
                                        center = Offset(width * 0.5f, height * 0.25f),
                                        radius = width * 0.85f,
                                    ),
                            )
                        }

                        drawRect(
                            brush =
                                Brush.verticalGradient(
                                    colors =
                                        listOf(
                                            Color.Transparent,
                                            Color.Transparent,
                                            surfaceColor.copy(alpha = alpha * 0.22f),
                                            surfaceColor.copy(alpha = alpha * 0.55f),
                                            surfaceColor,
                                        ),
                                    startY = height * 0.4f,
                                    endY = height,
                                ),
                        )
                    },
        )
    }
}
