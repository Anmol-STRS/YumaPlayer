/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.settings.account.header

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import moe.rukamori.archivetune.LocalMarqueeEnabled
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.home.state.AccountChannelUiModel
import moe.rukamori.archivetune.home.state.AccountChannelsState
import moe.rukamori.archivetune.ui.component.LocalPreferenceGroupPosition
import moe.rukamori.archivetune.ui.component.PreferenceGroupPosition
import moe.rukamori.archivetune.ui.settings.SettingsDimensions
import moe.rukamori.archivetune.ui.theme.LocalYumaColors
import moe.rukamori.archivetune.ui.theme.yumaClickable
import moe.rukamori.archivetune.utils.SavedAccount
import moe.rukamori.archivetune.utils.SavedAccountCollection

@Composable
fun ProfileIdentityCard(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    isLoggedIn: Boolean,
    accountName: String,
    accountEmail: String,
    accountHandle: String,
    accountImageUrl: String?,
    savedAccounts: SavedAccountCollection,
    activeInnerTubeCookie: String,
    activeDataSyncId: String,
    accountChannelsState: AccountChannelsState,
    extractedColorHex: String? = null,
    onAvatarPixelsReady: (IntArray) -> Unit = {},
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
    onSaveAccount: () -> Unit,
    onSwitchAccount: (SavedAccount) -> Unit,
    onSwitchAccountChannel: (AccountChannelUiModel) -> Unit,
    onRemoveAccount: (SavedAccount) -> Unit,
    onAddAnotherAccount: () -> Unit,
) {
    var accountMenuExpanded by remember { mutableStateOf(false) }
    val menuChevronRotation by animateFloatAsState(
        targetValue = if (accountMenuExpanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "accountMenuChevron",
    )

    val colors = LocalYumaColors.current

    val groupPosition = LocalPreferenceGroupPosition.current
    val resolvedShape = shape ?: when (groupPosition) {
        PreferenceGroupPosition.First -> RoundedCornerShape(
            topStart = SettingsDimensions.SegmentedCornerLarge,
            topEnd = SettingsDimensions.SegmentedCornerLarge,
            bottomStart = SettingsDimensions.SegmentedCornerSmall,
            bottomEnd = SettingsDimensions.SegmentedCornerSmall
        )
        PreferenceGroupPosition.Middle -> RoundedCornerShape(
            SettingsDimensions.SegmentedCornerSmall
        )
        PreferenceGroupPosition.Last -> RoundedCornerShape(
            topStart = SettingsDimensions.SegmentedCornerSmall,
            topEnd = SettingsDimensions.SegmentedCornerSmall,
            bottomStart = SettingsDimensions.SegmentedCornerLarge,
            bottomEnd = SettingsDimensions.SegmentedCornerLarge
        )
        else -> RoundedCornerShape(SettingsDimensions.SegmentedCornerLarge)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(resolvedShape)
            .background(colors.glassBackground)
            .border(1.dp, colors.glassBorder, resolvedShape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ProfileAvatarBadge(
                accountImageUrl = accountImageUrl,
                isLoggedIn = isLoggedIn,
                primaryColor = MaterialTheme.colorScheme.primary,
                tertiaryColor = MaterialTheme.colorScheme.tertiary,
                onAvatarPixelsReady = onAvatarPixelsReady,
            )

            val marqueeEnabled = LocalMarqueeEnabled.current
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = accountName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                    overflow = if (marqueeEnabled) TextOverflow.Clip else TextOverflow.Ellipsis,
                    modifier = if (marqueeEnabled) Modifier.basicMarquee() else Modifier,
                )

                if (accountHandle.isNotBlank()) {
                    Text(
                        text = accountHandle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        maxLines = 1,
                        overflow = if (marqueeEnabled) TextOverflow.Clip else TextOverflow.Ellipsis,
                        modifier = if (marqueeEnabled) Modifier.basicMarquee() else Modifier,
                    )
                } else if (!isLoggedIn) {
                    Text(
                        text = stringResource(R.string.not_logged_in),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
                    )
                }

                Box(modifier = Modifier.padding(top = 6.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .height(38.dp)
                                .yumaClickable(pressedScale = 0.95f, onClick = onPrimaryAction),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    painter = painterResource(if (isLoggedIn) R.drawable.account else R.drawable.login),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = if (isLoggedIn) stringResource(R.string.account) else stringResource(R.string.login),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        if (isLoggedIn) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(38.dp)
                                    .yumaClickable(
                                        pressedScale = 0.95f,
                                        onClick = { accountMenuExpanded = !accountMenuExpanded },
                                    ),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.expand_more),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .rotate(menuChevronRotation),
                                    )
                                }
                            }
                        }
                    }

                    ProfileAccountMenu(
                        expanded = accountMenuExpanded,
                        onDismissRequest = { accountMenuExpanded = false },
                        accountChannelsState = accountChannelsState,
                        activeDataSyncId = activeDataSyncId,
                        onSwitchAccountChannel = onSwitchAccountChannel,
                        savedAccounts = savedAccounts,
                        activeInnerTubeCookie = activeInnerTubeCookie,
                        onRemoveAccount = onRemoveAccount,
                        onSwitchAccount = onSwitchAccount,
                        isLoggedIn = isLoggedIn,
                        onSaveAccount = onSaveAccount,
                        onAddAnotherAccount = onAddAnotherAccount,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .yumaClickable(
                        pressedScale = 0.94f,
                        onClick = onSecondaryAction,
                    )
                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
                    .background(colors.glassBackground, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isLoggedIn) stringResource(R.string.action_logout) else stringResource(R.string.advanced_login),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}