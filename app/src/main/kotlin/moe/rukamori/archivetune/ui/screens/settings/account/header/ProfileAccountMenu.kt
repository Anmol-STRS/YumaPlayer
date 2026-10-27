/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.settings.account.header

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.home.state.AccountChannelUiModel
import moe.rukamori.archivetune.home.state.AccountChannelsState
import moe.rukamori.archivetune.ui.component.DefaultDialog
import moe.rukamori.archivetune.ui.component.IconButton
import moe.rukamori.archivetune.ui.screens.settings.segmentedSettingsItemShape
import moe.rukamori.archivetune.ui.settings.SettingsDimensions
import moe.rukamori.archivetune.ui.theme.yumaClickable
import moe.rukamori.archivetune.utils.SavedAccount
import moe.rukamori.archivetune.utils.SavedAccountCollection

@Composable
fun ProfileAccountMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    accountChannelsState: AccountChannelsState,
    activeDataSyncId: String?,
    onSwitchAccountChannel: (AccountChannelUiModel) -> Unit,
    savedAccounts: SavedAccountCollection,
    activeInnerTubeCookie: String,
    onRemoveAccount: (SavedAccount) -> Unit,
    onSwitchAccount: (SavedAccount) -> Unit,
    isLoggedIn: Boolean,
    onSaveAccount: () -> Unit,
    onAddAnotherAccount: () -> Unit,
    onLogout: () -> Unit = {},
    onBrowserLogin: () -> Unit = {},
    onAdvancedLogin: () -> Unit = {},
) {
    if (!expanded) return

    DefaultDialog(
        onDismiss = onDismissRequest,
        contentScrollable = true,
        title = {
            Text(
                text = stringResource(R.string.account),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        buttons = {
            TextButton(
                onClick = onDismissRequest,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(R.string.close))
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SettingsDimensions.SectionSpacing),
        ) {
            if (isLoggedIn) {
                when (accountChannelsState) {
                    is AccountChannelsState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }

                    is AccountChannelsState.Error -> {
                        Text(
                            text = accountChannelsState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }

                    is AccountChannelsState.Success -> {
                        val channelList: List<AccountChannelUiModel> = accountChannelsState.channels.items
                        if (channelList.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(SettingsDimensions.SegmentedItemGap)) {
                                Text(
                                    text = stringResource(R.string.youtube_channels),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                                )
                                channelList.forEachIndexed { index, channel ->
                                    val isSelected = channel.isSelected || channel.dataSyncId == activeDataSyncId
                                    val shape = segmentedSettingsItemShape(index, channelList.size)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(shape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                                else MaterialTheme.colorScheme.surfaceContainer
                                            )
                                            .yumaClickable {
                                                onDismissRequest()
                                                onSwitchAccountChannel(channel)
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.person),
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp),
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = channel.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (isSelected) {
                                            Icon(
                                                painter = painterResource(R.drawable.check),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AccountChannelsState.Empty -> Unit
                }
            }

            if (savedAccounts.accounts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(SettingsDimensions.SegmentedItemGap)) {
                    Text(
                        text = stringResource(R.string.saved_accounts),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    )
                    savedAccounts.accounts.forEachIndexed { index, account ->
                        val isCurrent = account.innerTubeCookie == activeInnerTubeCookie
                        val shape = segmentedSettingsItemShape(index, savedAccounts.accounts.size)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    else MaterialTheme.colorScheme.surfaceContainer
                                )
                                .yumaClickable(enabled = !isCurrent) {
                                    onDismissRequest()
                                    onSwitchAccount(account)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.account),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account.name.ifBlank { stringResource(R.string.account) },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (account.email.isNotBlank()) {
                                    Text(
                                        text = account.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (isCurrent) {
                                Icon(
                                    painter = painterResource(R.drawable.check),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            } else {
                                IconButton(
                                    onClick = { onRemoveAccount(account) },
                                    onLongClick = {},
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.close),
                                        contentDescription = stringResource(R.string.remove_account),
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(SettingsDimensions.SegmentedItemGap)) {
                if (isLoggedIn) {
                    val isCurrentSaved = savedAccounts.accounts.any { it.innerTubeCookie == activeInnerTubeCookie }
                    val actionsCount = (if (!isCurrentSaved) 1 else 0) + 3
                    var actionIndex = 0

                    if (!isCurrentSaved) {
                        AccountActionItem(
                            title = stringResource(R.string.save_current_account),
                            icon = painterResource(R.drawable.download),
                            tint = MaterialTheme.colorScheme.primary,
                            shape = segmentedSettingsItemShape(actionIndex++, actionsCount),
                            onClick = onSaveAccount,
                        )
                    }

                    AccountActionItem(
                        title = stringResource(R.string.add_another_account),
                        icon = painterResource(R.drawable.add),
                        tint = MaterialTheme.colorScheme.primary,
                        shape = segmentedSettingsItemShape(actionIndex++, actionsCount),
                        onClick = {
                            onDismissRequest()
                            onAddAnotherAccount()
                        },
                    )

                    AccountActionItem(
                        title = stringResource(R.string.token_settings),
                        subtitle = stringResource(R.string.token_adv_login_description),
                        icon = painterResource(R.drawable.token),
                        tint = MaterialTheme.colorScheme.secondary,
                        shape = segmentedSettingsItemShape(actionIndex++, actionsCount),
                        onClick = {
                            onDismissRequest()
                            onAdvancedLogin()
                        },
                    )

                    AccountActionItem(
                        title = stringResource(R.string.action_logout),
                        icon = painterResource(R.drawable.logout),
                        tint = MaterialTheme.colorScheme.error,
                        textColor = MaterialTheme.colorScheme.error,
                        shape = segmentedSettingsItemShape(actionIndex, actionsCount),
                        onClick = {
                            onDismissRequest()
                            onLogout()
                        },
                    )
                } else {
                    AccountActionItem(
                        title = stringResource(R.string.login),
                        subtitle = stringResource(R.string.login_browser),
                        icon = painterResource(R.drawable.login),
                        tint = MaterialTheme.colorScheme.primary,
                        shape = segmentedSettingsItemShape(0, 2),
                        onClick = {
                            onDismissRequest()
                            onBrowserLogin()
                        },
                    )

                    AccountActionItem(
                        title = stringResource(R.string.advanced_login),
                        subtitle = stringResource(R.string.token_adv_login_description),
                        icon = painterResource(R.drawable.token),
                        tint = MaterialTheme.colorScheme.secondary,
                        shape = segmentedSettingsItemShape(1, 2),
                        onClick = {
                            onDismissRequest()
                            onAdvancedLogin()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountActionItem(
    title: String,
    icon: Painter,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .yumaClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
