/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.settings.account.header

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import moe.rukamori.archivetune.home.state.AccountChannelUiModel
import moe.rukamori.archivetune.home.state.AccountChannelsState
import moe.rukamori.archivetune.ui.screens.settings.account.menu.AccountActionGroup
import moe.rukamori.archivetune.ui.screens.settings.account.menu.AccountChannelsSection
import moe.rukamori.archivetune.ui.screens.settings.account.menu.AccountMenuSheet
import moe.rukamori.archivetune.ui.screens.settings.account.menu.SavedAccountsSection
import moe.rukamori.archivetune.utils.SavedAccount
import moe.rukamori.archivetune.utils.SavedAccountCollection

@OptIn(ExperimentalMaterial3Api::class)
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

    AccountMenuSheet(
        onDismissRequest = onDismissRequest,
    ) {
        if (isLoggedIn) {
            AccountChannelsSection(
                accountChannelsState = accountChannelsState,
                activeDataSyncId = activeDataSyncId,
                onSwitchAccountChannel = onSwitchAccountChannel,
                onDismissRequest = onDismissRequest,
            )
        }

        SavedAccountsSection(
            savedAccounts = savedAccounts,
            activeInnerTubeCookie = activeInnerTubeCookie,
            onSwitchAccount = onSwitchAccount,
            onRemoveAccount = onRemoveAccount,
            onDismissRequest = onDismissRequest,
        )

        AccountActionGroup(
            isLoggedIn = isLoggedIn,
            isCurrentSaved = savedAccounts.accounts.any { it.innerTubeCookie == activeInnerTubeCookie },
            onSaveAccount = onSaveAccount,
            onAddAnotherAccount = onAddAnotherAccount,
            onAdvancedLogin = onAdvancedLogin,
            onDismissRequest = onDismissRequest,
            onLogout = onLogout,
            onBrowserLogin = onBrowserLogin,
        )
    }
}
