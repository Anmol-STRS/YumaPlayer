package moe.rukamori.archivetune.home.effects

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import moe.rukamori.archivetune.constants.HomeBackgroundStyle

data class HomeBackgroundSettings(
    val style: HomeBackgroundStyle = HomeBackgroundStyle.TONAL,
    val parallaxEnabled: Boolean = true,
    val parallaxSensitivity: Float = 0.6f,
    val brightness: Float = 1f,
)

val LocalHomeBackgroundStyle = compositionLocalOf { HomeBackgroundSettings() }

@Composable
fun ScreenBackground(modifier: Modifier = Modifier, isVisible: Boolean = true) {
    val homeBackground = LocalHomeBackgroundStyle.current

    if (homeBackground.style == HomeBackgroundStyle.OFF || !isVisible) {
        return
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .drawWithContent {
                    drawContent()
            }
    ) {
        when (homeBackground.style) {
            HomeBackgroundStyle.TONAL -> {
                HomePremiumBackground(
                    blobColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(430.dp)
                        .align(Alignment.TopCenter)
                )
            }
            HomeBackgroundStyle.CIRCLES -> {
                CirclesBackground(
                    parallaxEnabled = homeBackground.parallaxEnabled,
                    parallaxSensitivity = homeBackground.parallaxSensitivity,
                    brightness = homeBackground.brightness,
                    isVisible = true,
                )
            }
            HomeBackgroundStyle.RINGS -> {
                RingsBackground(
                    parallaxEnabled = homeBackground.parallaxEnabled,
                    parallaxSensitivity = homeBackground.parallaxSensitivity,
                    brightness = homeBackground.brightness,
                    isVisible = true,
                )
            }
            HomeBackgroundStyle.MESH -> {
                MeshBackground(
                    parallaxEnabled = homeBackground.parallaxEnabled,
                    parallaxSensitivity = homeBackground.parallaxSensitivity,
                    brightness = homeBackground.brightness,
                    isVisible = true,
                )
            }
            HomeBackgroundStyle.GRID -> {
                GridBackground(
                    parallaxEnabled = homeBackground.parallaxEnabled,
                    parallaxSensitivity = homeBackground.parallaxSensitivity,
                    brightness = homeBackground.brightness,
                    isVisible = true,
                )
            }
            HomeBackgroundStyle.PARTICLES -> {
                ParticlesBackground(
                    parallaxEnabled = homeBackground.parallaxEnabled,
                    parallaxSensitivity = homeBackground.parallaxSensitivity,
                    brightness = homeBackground.brightness,
                    isVisible = true,
                )
            }
            HomeBackgroundStyle.SNOW -> {
                SnowBackground(
                    parallaxEnabled = homeBackground.parallaxEnabled,
                    parallaxSensitivity = homeBackground.parallaxSensitivity,
                    brightness = homeBackground.brightness,
                    isVisible = true,
                )
            }
            HomeBackgroundStyle.SPACE -> {
                SpaceBackground(
                    parallaxEnabled = homeBackground.parallaxEnabled,
                    parallaxSensitivity = homeBackground.parallaxSensitivity,
                    brightness = homeBackground.brightness,
                    isVisible = true,
                )
            }
            HomeBackgroundStyle.LAVA -> {
                LavaBackground(
                    parallaxEnabled = homeBackground.parallaxEnabled,
                    parallaxSensitivity = homeBackground.parallaxSensitivity,
                    brightness = homeBackground.brightness,
                    isVisible = true,
                )
            }
        }
    }
}