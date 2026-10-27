/*
 * YumaPlayer (2026) | Original work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

@file:OptIn(ExperimentalHazeApi::class)

package moe.rukamori.archivetune.ui.scaffold.accountmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.ui.settings.SettingsDimensions

@Composable
internal fun TopBarAccountDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    blurRadius: Float = SettingsDimensions.BlurRadiusDefault,
    blurNavBar: Boolean = true,
    pureBlack: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val surfaceColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface
    val isHazeActive = blurNavBar && hazeState != null
    val menuTitle = stringResource(R.string.more_options)
    val fixedTintAlpha = if (pureBlack) SettingsDimensions.HazePureBlackTintAlpha else SettingsDimensions.HazeDefaultTintAlpha

    val hazeModifier = if (blurNavBar && hazeState != null) {
        val blurStyle = remember(surfaceColor, blurRadius, fixedTintAlpha) {
            val tintEffect = HazeColorEffect.tint(surfaceColor.copy(alpha = fixedTintAlpha))
            HazeBlurStyle {
                backgroundColor(surfaceColor)
                colorEffects(listOf(tintEffect))
                fallbackColorEffect(tintEffect)
                blurRadius(blurRadius.dp)
                noiseFactor(SettingsDimensions.HazeNoiseFactor)
            }
        }
        Modifier.hazeBlur(
            input = HazeInput.Sources(hazeState),
            style = blurStyle,
            performanceMode = HazePerformanceMode.Fixed(SettingsDimensions.HazeQualityFraction),
        )
    } else {
        Modifier
    }

    val popupBackground = if (isHazeActive) Color.Transparent else surfaceColor
    val popupBorderColor = if (isHazeActive) {
        contentColor.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = null,
        shape = shape,
        modifier = modifier
            .widthIn(min = 230.dp, max = 290.dp)
            .shadow(SettingsDimensions.BottomSheetShadowElevation, shape)
            .clip(shape)
            .then(hazeModifier)
            .background(popupBackground)
            .border(
                width = SettingsDimensions.GlassBorderThickness,
                color = popupBorderColor,
                shape = shape,
            )
            .semantics { paneTitle = menuTitle },
        content = content,
    )
}
