/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.onboarding.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import moe.rukamori.archivetune.ui.settings.SettingsDimensions

internal val OnboardingContentMaxWidth = 540.dp
internal val OnboardingPagePadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun rememberExpressiveShapes(): List<Shape> {
    val s0 = MaterialShapes.Cookie4Sided.toShape()
    val s1 = MaterialShapes.Clover4Leaf.toShape()
    val s2 = MaterialShapes.Ghostish.toShape()
    val s3 = MaterialShapes.Sunny.toShape()
    return remember(s0, s1, s2, s3) { listOf(s0, s1, s2, s3) }
}

internal fun segmentedOnboardingItemShape(
    index: Int,
    count: Int,
): Shape {
    val large = SettingsDimensions.SegmentedCornerLarge
    val small = SettingsDimensions.SegmentedCornerSmall
    return when {
        count <= 1 -> {
            RoundedCornerShape(large)
        }

        index == 0 -> {
            RoundedCornerShape(
                topStart = large,
                topEnd = large,
                bottomEnd = small,
                bottomStart = small,
            )
        }

        index == count - 1 -> {
            RoundedCornerShape(
                topStart = small,
                topEnd = small,
                bottomEnd = large,
                bottomStart = large,
            )
        }

        else -> {
            RoundedCornerShape(small)
        }
    }
}
