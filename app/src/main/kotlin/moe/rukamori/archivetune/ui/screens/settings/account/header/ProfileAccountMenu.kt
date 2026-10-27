/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.settings.account.header

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.home.state.AccountChannelUiModel
import moe.rukamori.archivetune.home.state.AccountChannelsState
import moe.rukamori.archivetune.utils.SavedAccount
import moe.rukamori.archivetune.utils.SavedAccountCollection

@Composable
internal fun ProfileAccountMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    accountChannelsState: AccountChannelsState,
    activeDataSyncId: String,
    onSwitchAccountChannel: (AccountChannelUiModel) -> Unit,
    savedAccounts: SavedAccountCollection,
    activeInnerTubeCookie: String,
    onRemoveAccount: (SavedAccount) -> Unit,
    onSwitchAccount: (SavedAccount) -> Unit,
    isLoggedIn: Boolean,
    onSaveAccount: () -> Unit,
    onAddAnotherAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        val accountChannels = (accountChannelsState as? AccountChannelsState.Success)?.channels
        if (accountChannels != null && accountChannels.items.size > 1) {
            Text(
                text = stringResource(R.string.youtube_channels),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            accountChannels.items.forEach { channel ->
                val isActive = channel.isSelected || channel.dataSyncId == activeDataSyncId
                DropdownMenuItem(
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = channel.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val channelSubtitle = channel.channelHandle.ifBlank { channel.byline }
                            if (channelSubtitle.isNotBlank()) {
                                Text(
                                    text = channelSubtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.account),
                            contentDescription = null,
                            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    onClick = {
                        if (!isActive) onSwitchAccountChannel(channel)
                        onDismissRequest()
                    },
                )
            }
            Spacer(modifier = Modifier.padding(vertical = 4.dp))
        }

        if (savedAccounts.accounts.isNotEmpty()) {
            Text(
                text = stringResource(R.string.saved_accounts),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            savedAccounts.accounts.forEach { account ->
                val isActive = account.innerTubeCookie == activeInnerTubeCookie
                DropdownMenuItem(
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = account.name.ifBlank { account.email },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (account.email.isNotBlank()) {
                                Text(
                                    text = account.email,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.account),
                            contentDescription = null,
                            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = {
                        OutlinedIconButton(
                            onClick = { onRemoveAccount(account) },
                            modifier = Modifier.size(32.dp),
                            border = null,
                            colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.delete),
                                contentDescription = stringResource(R.string.remove_account),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    },
                    onClick = {
                        if (!isActive) onSwitchAccount(account)
                        onDismissRequest()
                    },
                )
            }
            Spacer(modifier = Modifier.padding(vertical = 4.dp))
        }

        if (isLoggedIn) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.save_current_account),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.bookmark),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = {
                    onSaveAccount()
                    onDismissRequest()
                },
            )

            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.add_another_account),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.add_circle),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = {
                    onDismissRequest()
                    onAddAnotherAccount()
                },
            )
        }
    }
}
