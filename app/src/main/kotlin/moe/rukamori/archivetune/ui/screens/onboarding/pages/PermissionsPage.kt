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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.onboarding.OnboardingPermissionAction
import moe.rukamori.archivetune.onboarding.OnboardingPermissionStatus
import moe.rukamori.archivetune.onboarding.OnboardingPermissionUiModel
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
internal fun PermissionsPage(
    uiState: OnboardingUiState,
    pageIndex: Int,
    onPermissionAction: (OnboardingPermissionAction) -> Unit,
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
        item(key = "permissions_group", contentType = "permissions_group") {
            val count = uiState.permissions.size
            Column(
                modifier =
                    Modifier
                        .widthIn(max = OnboardingContentMaxWidth)
                        .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SettingsDimensions.SegmentedItemGap),
            ) {
                uiState.permissions.forEachIndexed { index, item ->
                    val shape = remember(index, count) { segmentedOnboardingItemShape(index, count) }
                    val position = remember(index, count) { yumaSegmentPosition(index, count) }
                    GlassPermissionRow(
                        permission = item,
                        iconShape = expressiveShapes[index % expressiveShapes.size],
                        shape = shape,
                        position = position,
                        onPermissionAction = onPermissionAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassPermissionRow(
    permission: OnboardingPermissionUiModel,
    iconShape: Shape,
    shape: Shape,
    position: YumaSegmentPosition,
    onPermissionAction: (OnboardingPermissionAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val action = permission.action
    val onClick =
        remember(action, onPermissionAction) {
            {
                if (action != null) {
                    onPermissionAction(action)
                }
            }
        }
    val colors = LocalYumaColors.current

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .yumaClickable(
                    enabled = action != null,
                    onClick = onClick,
                )
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
        PermissionIcon(permission = permission, iconShape = iconShape)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(permission.titleResId),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = LocalArchiveTuneFontFamily.current,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(SettingsDimensions.SegmentedRowSpacing))
            Text(
                text = stringResource(permission.descriptionResId),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = LocalArchiveTuneFontFamily.current,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        PermissionStatusAction(
            permission = permission,
            onPermissionAction = onPermissionAction,
        )
    }
}

@Composable
private fun PermissionIcon(
    permission: OnboardingPermissionUiModel,
    iconShape: Shape,
) {
    val containerColor =
        when (permission.status) {
            OnboardingPermissionStatus.ALLOWED -> MaterialTheme.colorScheme.primary
            OnboardingPermissionStatus.NEEDS_ACTION -> MaterialTheme.colorScheme.tertiary
            OnboardingPermissionStatus.ALLOWED_BY_INSTALL -> MaterialTheme.colorScheme.secondary
            OnboardingPermissionStatus.UNAVAILABLE -> LocalYumaColors.current.glassBackground
        }

    val iconTint =
        when (permission.status) {
            OnboardingPermissionStatus.ALLOWED -> MaterialTheme.colorScheme.onPrimary
            OnboardingPermissionStatus.NEEDS_ACTION -> MaterialTheme.colorScheme.onTertiary
            OnboardingPermissionStatus.ALLOWED_BY_INSTALL -> MaterialTheme.colorScheme.onSecondary
            OnboardingPermissionStatus.UNAVAILABLE -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    Surface(
        shape = iconShape,
        color = containerColor,
        modifier = Modifier.size(SettingsDimensions.SegmentedIconBoxSize),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(permission.iconResId),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun PermissionStatusAction(
    permission: OnboardingPermissionUiModel,
    onPermissionAction: (OnboardingPermissionAction) -> Unit,
) {
    val action = permission.action
    val colors = LocalYumaColors.current

    if (action != null) {
        Surface(
            modifier =
                Modifier
                    .yumaClickable { onPermissionAction(action) }
                    .clip(RoundedCornerShape(12.dp)),
            color = MaterialTheme.colorScheme.primary,
        ) {
            Text(
                text = stringResource(R.string.allow),
                fontFamily = LocalArchiveTuneFontFamily.current,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    } else {
        val badgeShape = RoundedCornerShape(12.dp)
        Box(
            modifier =
                Modifier
                    .yumaGlassCard(
                        shape = badgeShape,
                        backgroundColor = colors.glassBackground,
                        borderColor = colors.glassBorder,
                        strokeWidth = SettingsDimensions.GlassBorderThickness,
                    )
                    .clip(badgeShape),
        ) {
            Text(
                text = stringResource(permission.status.labelResId()),
                fontFamily = LocalArchiveTuneFontFamily.current,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

private fun OnboardingPermissionStatus.labelResId(): Int =
    when (this) {
        OnboardingPermissionStatus.ALLOWED -> R.string.permission_status_allowed
        OnboardingPermissionStatus.NEEDS_ACTION -> R.string.allow
        OnboardingPermissionStatus.ALLOWED_BY_INSTALL -> R.string.onboarding_permission_allowed_by_install
        OnboardingPermissionStatus.UNAVAILABLE -> R.string.onboarding_permission_unavailable
    }
