/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

@file:OptIn(ExperimentalMaterial3Api::class)

package moe.rukamori.archivetune.ui.screens.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import moe.rukamori.archivetune.App.Companion.forgetAccount
import moe.rukamori.archivetune.BuildConfig
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.constants.AccountChannelHandleKey
import moe.rukamori.archivetune.constants.AccountEmailKey
import moe.rukamori.archivetune.constants.AccountNameKey
import moe.rukamori.archivetune.constants.DataSyncIdKey
import moe.rukamori.archivetune.constants.ForceSyncOnAccountSwitchKey
import moe.rukamori.archivetune.constants.HideYtmLikedSongsKey
import moe.rukamori.archivetune.constants.InnerTubeCookieKey
import moe.rukamori.archivetune.constants.SavedAccountsKey
import moe.rukamori.archivetune.constants.SelectedYtmPlaylistsKey
import moe.rukamori.archivetune.constants.ShowSpotifyPlaylistsKey
import moe.rukamori.archivetune.constants.SpotifySyncLikesKey
import moe.rukamori.archivetune.constants.UseLoginForBrowse
import moe.rukamori.archivetune.constants.UseSpotifyHomeKey
import moe.rukamori.archivetune.constants.VisitorDataKey
import moe.rukamori.archivetune.constants.YtmSyncKey
import moe.rukamori.archivetune.home.state.AccountChannelUiModel
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.utils.hasYouTubeLoginCookie
import moe.rukamori.archivetune.spotify.SpotifyAccountViewModel
import moe.rukamori.archivetune.ui.screens.buildLoginRoute
import moe.rukamori.archivetune.ui.screens.settings.account.AccountSettingsContent
import moe.rukamori.archivetune.ui.screens.settings.account.AccountSettingsViewModel
import moe.rukamori.archivetune.ui.screens.settings.account.header.ProfileAccountMenu
import moe.rukamori.archivetune.ui.screens.settings.account.providers.AccountDialogsHost
import moe.rukamori.archivetune.ui.state.UpdateState
import moe.rukamori.archivetune.ui.theme.TestThemeWrapper
import moe.rukamori.archivetune.ui.theme.ThemePreviews
import moe.rukamori.archivetune.ui.utils.appBarScrollBehavior
import moe.rukamori.archivetune.ui.utils.backToMain
import moe.rukamori.archivetune.utils.PreferenceStore
import moe.rukamori.archivetune.utils.SavedAccount
import moe.rukamori.archivetune.utils.SavedAccountCollection
import moe.rukamori.archivetune.utils.Updater
import moe.rukamori.archivetune.utils.dataStore
import moe.rukamori.archivetune.utils.decodeSavedAccounts
import moe.rukamori.archivetune.utils.encodeSavedAccounts
import moe.rukamori.archivetune.utils.putLegacyPoToken
import moe.rukamori.archivetune.utils.rememberPreference
import moe.rukamori.archivetune.viewmodels.HomeViewModel
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun AccountSettings(
    navController: NavController,
    updateState: UpdateState,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scrollBehavior = appBarScrollBehavior()

    val accountLabel = stringResource(R.string.account)
    val loginLabel = stringResource(R.string.login)
    val tokenDescription = stringResource(R.string.token_adv_login_description)

    val (accountNamePref, onAccountNameChange) = rememberPreference(AccountNameKey, "")
    val (accountEmail, onAccountEmailChange) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, onAccountChannelHandleChange) = rememberPreference(AccountChannelHandleKey, "")
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val (visitorData, onVisitorDataChange) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, onDataSyncIdChange) = rememberPreference(DataSyncIdKey, "")
    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(UseLoginForBrowse, true)
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, true)
    val (forceSyncOnAccountSwitch, onForceSyncOnAccountSwitchChange) =
        rememberPreference(ForceSyncOnAccountSwitchKey, false)
    val (selectedYtmPlaylists, _) = rememberPreference(SelectedYtmPlaylistsKey, "")
    val (savedAccountsJson, onSavedAccountsJsonChange) = rememberPreference(SavedAccountsKey, "")
    val savedAccounts =
        remember(savedAccountsJson) {
            SavedAccountCollection(decodeSavedAccounts(savedAccountsJson))
        }

    var showAccountMenu by remember { mutableStateOf(false) }

    val onLegacyPoTokenChange: (String) -> Unit = { value ->
        PreferenceStore.launchEdit(context.dataStore) { putLegacyPoToken(value) }
    }

    val isLoggedIn = remember(innerTubeCookie) { hasYouTubeLoginCookie(innerTubeCookie) }

    LaunchedEffect(useLoginForBrowse) {
        YouTube.useLoginForBrowse = useLoginForBrowse
    }

    val viewModel: HomeViewModel = hiltViewModel()
    val accountNameFromViewModel by viewModel.accountName.collectAsStateWithLifecycle()
    val accountImageUrl by viewModel.accountImageUrl.collectAsStateWithLifecycle()
    val accountChannelsState by viewModel.accountChannelsState.collectAsStateWithLifecycle()

    val accountSettingsViewModel: AccountSettingsViewModel = hiltViewModel()
    val accountUiState by accountSettingsViewModel.uiState.collectAsStateWithLifecycle()

    val displayName =
        when {
            accountNameFromViewModel.isNotBlank() -> accountNameFromViewModel
            accountNamePref.isNotBlank() -> accountNamePref
            isLoggedIn -> accountLabel
            else -> loginLabel
        }

    val spotifyAccountViewModel: SpotifyAccountViewModel = hiltViewModel()
    val spotifyState by spotifyAccountViewModel.uiState.collectAsStateWithLifecycle()
    val (showSpotifyPlaylists, onShowSpotifyPlaylistsChange) = rememberPreference(ShowSpotifyPlaylistsKey, true)
    val (useSpotifyHome, onUseSpotifyHomeChange) = rememberPreference(UseSpotifyHomeKey, false)
    val (spotifySyncLikes, onSpotifySyncLikesChange) = rememberPreference(SpotifySyncLikesKey, false)
    val (hideYtmLikedSongs, onHideYtmLikedSongsChange) = rememberPreference(HideYtmLikedSongsKey, false)
    var showSpotifyOptionsDialog by remember { mutableStateOf(false) }
    var showSpotifyLogin by remember { mutableStateOf(false) }

    var showToken by remember { mutableStateOf(false) }
    var showTokenEditor by remember { mutableStateOf(false) }
    var showUnsavedAccountDialog by remember { mutableStateOf(false) }
    var showLoginChoiceDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            showToken = false
        }
    }

    LaunchedEffect(showToken) {
        if (showToken) {
            delay(5000.milliseconds)
            showToken = false
        }
    }

    val hasUpdate = updateState is UpdateState.SoftUpdate || updateState is UpdateState.CriticalUpdate
    val latestVersionName = when (updateState) {
        is UpdateState.SoftUpdate -> updateState.versionName
        is UpdateState.CriticalUpdate -> updateState.versionName
        else -> BuildConfig.VERSION_NAME
    }

    val tokenActionTitle =
        when {
            !isLoggedIn -> stringResource(R.string.advanced_login)
            showToken -> stringResource(R.string.token_shown)
            else -> stringResource(R.string.token_hidden)
        }

    val tokenPreview = if (isLoggedIn && innerTubeCookie.isNotBlank()) previewSecureValue(innerTubeCookie) else null

    val saveCurrentAccount: () -> Unit = {
        val existing = decodeSavedAccounts(savedAccountsJson)
        if (isLoggedIn && existing.none { it.innerTubeCookie == innerTubeCookie }) {
            val newAccount =
                SavedAccount(
                    id = UUID.randomUUID().toString(),
                    name = if (accountNameFromViewModel.isNotBlank()) accountNameFromViewModel else accountNamePref,
                    email = accountEmail,
                    channelHandle = accountChannelHandle,
                    innerTubeCookie = innerTubeCookie,
                    visitorData = visitorData,
                    dataSyncId = dataSyncId,
                    ytmSync = ytmSync,
                    selectedYtmPlaylists = selectedYtmPlaylists,
                )
            onSavedAccountsJsonChange(encodeSavedAccounts(existing + newAccount))
        }
    }

    val switchToAccount: (SavedAccount) -> Unit = { account ->
        viewModel.switchToAccount(account = account, forceSyncOnSwitch = forceSyncOnAccountSwitch)
    }

    val switchToAccountChannel: (AccountChannelUiModel) -> Unit = { channel ->
        viewModel.switchToAccountChannel(channel = channel, forceSyncOnSwitch = forceSyncOnAccountSwitch)
    }

    val removeAccount: (SavedAccount) -> Unit = { account ->
        val existing = decodeSavedAccounts(savedAccountsJson)
        onSavedAccountsJsonChange(encodeSavedAccounts(existing.filter { it.id != account.id }))
    }

    val uiState =
        AccountSettingsUiState(
            displayName = displayName,
            accountEmail = accountEmail,
            accountHandle = accountChannelHandle,
            accountImageUrl = accountImageUrl,
            isLoggedIn = isLoggedIn,
            savedAccounts = savedAccounts,
            activeInnerTubeCookie = innerTubeCookie,
            activeDataSyncId = dataSyncId,
            accountChannelsState = accountChannelsState,
            extractedColorHex = accountUiState.extractedColorHex,
            useLoginForBrowse = useLoginForBrowse,
            ytmSync = ytmSync,
            forceSyncOnAccountSwitch = forceSyncOnAccountSwitch,
            spotifyState = spotifyState,
            useSpotifyHome = useSpotifyHome,
            showSpotifyPlaylists = showSpotifyPlaylists,
            spotifySyncLikes = spotifySyncLikes,
            hideYtmLikedSongs = hideYtmLikedSongs,
            hasUpdate = hasUpdate,
            latestVersionName = latestVersionName,
            showToken = showToken,
            tokenActionTitle = tokenActionTitle,
            tokenDescription = tokenDescription,
            tokenPreview = tokenPreview,
        )

    val uiActions =
        AccountSettingsUiActions(
            onNavigateUp = navController::navigateUp,
            onNavigateHome = navController::backToMain,
            onOpenTokenEditor = { showTokenEditor = true },
            onOpenAccountMenu = {
                showAccountMenu = true
            },
            onOpenUpdateDownload = { uriHandler.openUri(Updater.getLatestDownloadUrl()) },
            onAvatarPixelsReady = accountSettingsViewModel::processAvatarPixels,
            onPrimaryAction = {
                if (isLoggedIn) navController.navigate("account") else showLoginChoiceDialog = true
            },
            onSecondaryAction = {
                if (isLoggedIn) {
                    showToken = false
                    onInnerTubeCookieChange("")
                    forgetAccount(context, clearWebAuthSession = true)
                } else {
                    showTokenEditor = true
                }
            },
            onSaveAccount = saveCurrentAccount,
            onSwitchAccount = switchToAccount,
            onSwitchAccountChannel = switchToAccountChannel,
            onRemoveAccount = removeAccount,
            onAddAnotherAccount = {
                val isSaved = savedAccounts.accounts.any { it.innerTubeCookie == innerTubeCookie }
                if (isLoggedIn && !isSaved) showUnsavedAccountDialog = true else navController.navigate(buildLoginRoute())
            },
            onUseLoginForBrowseChange = onUseLoginForBrowseChange,
            onYtmSyncChange = onYtmSyncChange,
            onForceSyncOnAccountSwitchChange = onForceSyncOnAccountSwitchChange,
            onSpotifyClick = {
                if (spotifyState.isAuthenticated) showSpotifyOptionsDialog = true else showSpotifyLogin = true
            },
            onUseSpotifyHomeChange = { isSpotify ->
                if (isSpotify && !spotifyState.isAuthenticated) showSpotifyLogin = true else onUseSpotifyHomeChange(isSpotify)
            },
            onSpotifySyncLikesChange = onSpotifySyncLikesChange,
            onHideYtmLikedSongsChange = onHideYtmLikedSongsChange,
            onNavigateIntegration = { navController.navigate("settings/integration") },
            onNavigateMusicTogether = { navController.navigate("settings/music_together") },
            onNavigateHiddenPlaylists = { navController.navigate("settings/hidden_playlists") },
            onTokenRowClick = { showTokenEditor = true },
            onRevealToken = { showToken = true },
        )

    val dialogsState =
        AccountDialogsState(
            showSpotifyLogin = showSpotifyLogin,
            showSpotifyOptionsDialog = showSpotifyOptionsDialog,
            showLoginChoiceDialog = showLoginChoiceDialog,
            showTokenEditor = showTokenEditor,
            showUnsavedAccountDialog = showUnsavedAccountDialog,
            spotifyState = spotifyState,
            showSpotifyPlaylists = showSpotifyPlaylists,
            tokenEditorState =
                TokenEditorDialogState(
                    innerTubeCookie = innerTubeCookie,
                    visitorData = visitorData,
                    dataSyncId = dataSyncId,
                    accountNamePref = accountNamePref,
                    accountEmail = accountEmail,
                    accountChannelHandle = accountChannelHandle,
                ),
        )

    val dialogsActions =
        AccountDialogsActions(
            onDismissSpotifyLogin = { showSpotifyLogin = false },
            onCookiesCaptured = { spDc, spKey ->
                spotifyAccountViewModel.connectWithCookies(spDc = spDc, spKey = spKey)
                showSpotifyLogin = false
            },
            onShowSpotifyPlaylistsChange = onShowSpotifyPlaylistsChange,
            onConnectSpotify = { showSpotifyOptionsDialog = false; showSpotifyLogin = true },
            onSpotifyLogout = { spotifyAccountViewModel.logout(); showSpotifyOptionsDialog = false },
            onDismissSpotifyOptions = { showSpotifyOptionsDialog = false },
            onDismissLoginChoice = { showLoginChoiceDialog = false },
            onBrowserLogin = { showLoginChoiceDialog = false; navController.navigate(buildLoginRoute()) },
            onAdvancedLogin = { showLoginChoiceDialog = false; showTokenEditor = true },
            tokenEditorActions =
                TokenEditorDialogActions(
                    onInnerTubeCookieChange = onInnerTubeCookieChange,
                    onPoTokenChange = onLegacyPoTokenChange,
                    onVisitorDataChange = onVisitorDataChange,
                    onDataSyncIdChange = onDataSyncIdChange,
                    onAccountNameChange = onAccountNameChange,
                    onAccountEmailChange = onAccountEmailChange,
                    onAccountChannelHandleChange = onAccountChannelHandleChange,
                    onDismiss = { showTokenEditor = false },
                ),
            onDismissUnsavedAccount = { showUnsavedAccountDialog = false },
            onCancelUnsavedAccount = { showUnsavedAccountDialog = false },
            onNoThanksUnsavedAccount = { showUnsavedAccountDialog = false; navController.navigate(buildLoginRoute()) },
            onSaveYesUnsavedAccount = {
                showUnsavedAccountDialog = false
                saveCurrentAccount()
                navController.navigate(buildLoginRoute())
            },
        )

    AccountSettingsContent(
        state = uiState,
        actions = uiActions,
        scrollBehavior = scrollBehavior,
    )

    AccountDialogsHost(
        state = dialogsState,
        actions = dialogsActions,
    )
    if (showAccountMenu) {
        ProfileAccountMenu(
            expanded = true,
            onDismissRequest = { showAccountMenu = false },
            accountChannelsState = uiState.accountChannelsState,
            activeDataSyncId = uiState.activeDataSyncId,
            onSwitchAccountChannel = { channel ->
                showAccountMenu = false
                uiActions.onSwitchAccountChannel(channel)
            },
            savedAccounts = uiState.savedAccounts,
            activeInnerTubeCookie = uiState.activeInnerTubeCookie,
            onRemoveAccount = uiActions.onRemoveAccount,
            onSwitchAccount = { account ->
                showAccountMenu = false
                uiActions.onSwitchAccount(account)
            },
            isLoggedIn = uiState.isLoggedIn,
            onSaveAccount = {
                uiActions.onSaveAccount()
            },
            onAddAnotherAccount = {
                showAccountMenu = false
                uiActions.onAddAnotherAccount()
            },
            onLogout = {
                showAccountMenu = false
                uiActions.onSecondaryAction()
            },
            onBrowserLogin = {
                showAccountMenu = false
                dialogsActions.onBrowserLogin()
            },
            onAdvancedLogin = {
                showAccountMenu = false
                dialogsActions.onAdvancedLogin()
            },
        )
    }
}

@ThemePreviews
@Composable
private fun AccountSettingsPreview() {
    TestThemeWrapper {
        AccountSettings(
            navController = rememberNavController(),
            updateState = UpdateState.NoUpdate,
        )
    }
}
