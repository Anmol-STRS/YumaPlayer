/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.onboarding.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.onboarding.OnboardingCommunityActionUiModel
import moe.rukamori.archivetune.onboarding.OnboardingUiState
import moe.rukamori.archivetune.ui.screens.onboarding.components.ExpressivePageHeader
import moe.rukamori.archivetune.ui.screens.onboarding.components.OnboardingContentMaxWidth
import moe.rukamori.archivetune.ui.screens.onboarding.components.OnboardingPagePadding
import moe.rukamori.archivetune.ui.screens.onboarding.components.rememberExpressiveShapes
import moe.rukamori.archivetune.ui.screens.onboarding.components.segmentedOnboardingItemShape
import moe.rukamori.archivetune.ui.settings.SettingsDimensions
import moe.rukamori.archivetune.ui.theme.LocalArchiveTuneFontFamily
import moe.rukamori.archivetune.ui.theme.LocalYumaColors
import moe.rukamori.archivetune.ui.theme.YumaSegmentPosition
import moe.rukamori.archivetune.ui.theme.yumaClickable
import moe.rukamori.archivetune.ui.theme.yumaGlassCard
import moe.rukamori.archivetune.ui.theme.yumaSegmentPosition

@Composable
internal fun CommunityPage(
    uiState: OnboardingUiState,
    pageIndex: Int,
    onCommunityAction: (OnboardingCommunityActionUiModel) -> Unit,
) {
    val page = uiState.pages[pageIndex]
    val expressiveShapes = rememberExpressiveShapes()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = OnboardingPagePadding,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = page.id.name, contentType = "header") {
            ExpressivePageHeader(
                iconResId = page.iconResId,
                titleResId = page.titleResId,
                subtitleResId = page.subtitleResId,
            )
        }
        item(key = "community_group", contentType = "community_group") {
            val count = uiState.communityActions.size
            Column(
                modifier =
                    Modifier
                        .widthIn(max = OnboardingContentMaxWidth)
                        .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SettingsDimensions.SegmentedItemGap),
            ) {
                uiState.communityActions.forEachIndexed { index, item ->
                    val shape = remember(index, count) { segmentedOnboardingItemShape(index, count) }
                    val position = remember(index, count) { yumaSegmentPosition(index, count) }
                    GlassCommunityRow(
                        action = item,
                        iconShape = expressiveShapes[index % expressiveShapes.size],
                        shape = shape,
                        position = position,
                        onCommunityAction = onCommunityAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassCommunityRow(
    action: OnboardingCommunityActionUiModel,
    iconShape: Shape,
    shape: Shape,
    position: YumaSegmentPosition,
    onCommunityAction: (OnboardingCommunityActionUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalYumaColors.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .yumaClickable { onCommunityAction(action) }
                .yumaGlassCard(
                    shape = shape,
                    backgroundColor = colors.glassBackground,
                    borderColor = colors.glassBorder,
                    strokeWidth = SettingsDimensions.GlassBorderThickness,
                    position = position,
                )
                .clip(shape)
                .padding(
                    horizontal = SettingsDimensions.SegmentedItemPaddingHorizontal,
                    vertical = SettingsDimensions.SegmentedItemPaddingVertical,
                ),
        horizontalArrangement = Arrangement.spacedBy(SettingsDimensions.RowIconSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(SettingsDimensions.SegmentedIconBoxSize),
            shape = iconShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(action.iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(action.titleResId),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = LocalArchiveTuneFontFamily.current,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(SettingsDimensions.SegmentedRowSpacing))
            Text(
                text = stringResource(action.descriptionResId),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = LocalArchiveTuneFontFamily.current,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
