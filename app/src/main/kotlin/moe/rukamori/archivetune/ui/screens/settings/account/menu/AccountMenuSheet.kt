/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.settings.account.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.ui.settings.SettingsDimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountMenuSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = null,
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = SettingsDimensions.BottomSheetHorizontalPadding)
                .padding(bottom = SettingsDimensions.BottomSheetBottomPadding)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(SettingsDimensions.BottomSheetCornerRadius),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(
                width = SettingsDimensions.GlassBorderThickness,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            ),
            tonalElevation = SettingsDimensions.BottomSheetTonalElevation,
            shadowElevation = SettingsDimensions.BottomSheetShadowElevation,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SettingsDimensions.BottomSheetContentPaddingH)
                    .padding(
                        top = SettingsDimensions.BottomSheetContentPaddingTop,
                        bottom = SettingsDimensions.BottomSheetContentPaddingBottom,
                    ),
                verticalArrangement = Arrangement.spacedBy(SettingsDimensions.SectionSpacing),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = SettingsDimensions.BottomSheetDragHandleBottomPadding)
                        .size(
                            width = SettingsDimensions.BottomSheetDragHandleWidth,
                            height = SettingsDimensions.BottomSheetDragHandleHeight,
                        )
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)),
                )

                Text(
                    text = stringResource(R.string.account),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                content()
            }
        }
    }
}
