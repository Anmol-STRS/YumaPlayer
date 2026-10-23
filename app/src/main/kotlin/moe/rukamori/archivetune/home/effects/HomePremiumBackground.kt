package moe.rukamori.archivetune.home.effects

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun HomePremiumBackground(
    blobColor: Color,
    surfaceColor: Color = MaterialTheme.colorScheme.background,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Основное мягкое пятно света по центру сверху
        val primaryCenter = Offset(width * 0.5f, height * 0.08f)
        val primaryRadius = width * 1.05f

        val primaryBrush = Brush.radialGradient(
            0.0f to blobColor.copy(alpha = 0.38f),
            0.35f to blobColor.copy(alpha = 0.20f),
            0.70f to blobColor.copy(alpha = 0.06f),
            1.0f to Color.Transparent,
            center = primaryCenter,
            radius = primaryRadius
        )

        drawCircle(
            brush = primaryBrush,
            radius = primaryRadius,
            center = primaryCenter
        )

        val secondaryCenter = Offset(width * 0.5f, height * 0.35f)
        val secondaryRadius = width * 0.85f

        val secondaryBrush = Brush.radialGradient(
            0.0f to blobColor.copy(alpha = 0.12f),
            0.5f to blobColor.copy(alpha = 0.03f),
            1.0f to Color.Transparent,
            center = secondaryCenter,
            radius = secondaryRadius
        )

        drawCircle(
            brush = secondaryBrush,
            radius = secondaryRadius,
            center = secondaryCenter
        )
    }
}