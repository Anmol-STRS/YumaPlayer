/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.settings.account.providers

import androidx.compose.runtime.Composable
import moe.rukamori.archivetune.ui.screens.settings.AccountDialogsActions
import moe.rukamori.archivetune.ui.screens.settings.AccountDialogsState

@Composable
internal fun AccountDialogsHost(
    state: AccountDialogsState,
    actions: AccountDialogsActions,
) {
    AccountSpotifyLoginSheet(
        show = state.showSpotifyLogin,
        onDismiss = actions.onDismissSpotifyLogin,
        onCookiesCaptured = actions.onCookiesCaptured,
    )

    if (state.showSpotifyOptionsDialog) {
        SpotifyOptionsDialog(
            spotifyState = state.spotifyState,
            showSpotifyPlaylists = state.showSpotifyPlaylists,
            onShowSpotifyPlaylistsChange = actions.onShowSpotifyPlaylistsChange,
            onConnectSpotify = actions.onConnectSpotify,
            onLogout = actions.onSpotifyLogout,
            onDismissRequest = actions.onDismissSpotifyOptions,
        )
    }

    if (state.showLoginChoiceDialog) {
        LoginChoiceDialog(
            onDismissRequest = actions.onDismissLoginChoice,
            onBrowserLogin = actions.onBrowserLogin,
            onAdvancedLogin = actions.onAdvancedLogin,
        )
    }

    if (state.showTokenEditor) {
        val editorState = state.tokenEditorState
        val editorActions = actions.tokenEditorActions
        TokenEditorDialog(
            innerTubeCookie = editorState.innerTubeCookie,
            visitorData = editorState.visitorData,
            dataSyncId = editorState.dataSyncId,
            accountNamePref = editorState.accountNamePref,
            accountEmail = editorState.accountEmail,
            accountChannelHandle = editorState.accountChannelHandle,
            onInnerTubeCookieChange = editorActions.onInnerTubeCookieChange,
            onPoTokenChange = editorActions.onPoTokenChange,
            onVisitorDataChange = editorActions.onVisitorDataChange,
            onDataSyncIdChange = editorActions.onDataSyncIdChange,
            onAccountNameChange = editorActions.onAccountNameChange,
            onAccountEmailChange = editorActions.onAccountEmailChange,
            onAccountChannelHandleChange = editorActions.onAccountChannelHandleChange,
            onDismiss = editorActions.onDismiss,
        )
    }

    if (state.showUnsavedAccountDialog) {
        UnsavedAccountDialog(
            onDismissRequest = actions.onDismissUnsavedAccount,
            onCancel = actions.onCancelUnsavedAccount,
            onNoThanks = actions.onNoThanksUnsavedAccount,
            onSaveYes = actions.onSaveYesUnsavedAccount,
        )
    }
}
