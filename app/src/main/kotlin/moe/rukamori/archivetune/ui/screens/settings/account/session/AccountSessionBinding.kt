/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.settings.account.session

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.utils.hasYouTubeLoginCookie
import moe.rukamori.archivetune.utils.PreferenceStore
import moe.rukamori.archivetune.utils.SavedAccountCollection
import moe.rukamori.archivetune.utils.dataStore
import moe.rukamori.archivetune.utils.decodeSavedAccounts
import moe.rukamori.archivetune.utils.putLegacyPoToken
import moe.rukamori.archivetune.utils.rememberPreference

internal class AccountSessionData(
    val accountNamePref: String,
    val onAccountNameChange: (String) -> Unit,
    val accountEmail: String,
    val onAccountEmailChange: (String) -> Unit,
    val accountChannelHandle: String,
    val onAccountChannelHandleChange: (String) -> Unit,
    val innerTubeCookie: String,
    val onInnerTubeCookieChange: (String) -> Unit,
    val visitorData: String,
    val onVisitorDataChange: (String) -> Unit,
    val dataSyncId: String,
    val onDataSyncIdChange: (String) -> Unit,
    val useLoginForBrowse: Boolean,
    val onUseLoginForBrowseChange: (Boolean) -> Unit,
    val ytmSync: Boolean,
    val onYtmSyncChange: (Boolean) -> Unit,
    val forceSyncOnAccountSwitch: Boolean,
    val onForceSyncOnAccountSwitchChange: (Boolean) -> Unit,
    val selectedYtmPlaylists: String,
    val savedAccountsJson: String,
    val onSavedAccountsJsonChange: (String) -> Unit,
    val savedAccounts: SavedAccountCollection,
    val onLegacyPoTokenChange: (String) -> Unit,
    val isLoggedIn: Boolean,
    val showSpotifyPlaylists: Boolean,
    val onShowSpotifyPlaylistsChange: (Boolean) -> Unit,
    val useSpotifyHome: Boolean,
    val onUseSpotifyHomeChange: (Boolean) -> Unit,
    val spotifySyncLikes: Boolean,
    val onSpotifySyncLikesChange: (Boolean) -> Unit,
    val hideYtmLikedSongs: Boolean,
    val onHideYtmLikedSongsChange: (Boolean) -> Unit,
)

@Composable
internal fun rememberAccountSessionBinding(context: Context): AccountSessionData {
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

    val onLegacyPoTokenChange: (String) -> Unit = { value ->
        PreferenceStore.launchEdit(context.dataStore) { putLegacyPoToken(value) }
    }

    val isLoggedIn = remember(innerTubeCookie) { hasYouTubeLoginCookie(innerTubeCookie) }

    LaunchedEffect(useLoginForBrowse) {
        YouTube.useLoginForBrowse = useLoginForBrowse
    }

    val (showSpotifyPlaylists, onShowSpotifyPlaylistsChange) = rememberPreference(ShowSpotifyPlaylistsKey, true)
    val (useSpotifyHome, onUseSpotifyHomeChange) = rememberPreference(UseSpotifyHomeKey, false)
    val (spotifySyncLikes, onSpotifySyncLikesChange) = rememberPreference(SpotifySyncLikesKey, false)
    val (hideYtmLikedSongs, onHideYtmLikedSongsChange) = rememberPreference(HideYtmLikedSongsKey, false)

    return AccountSessionData(
        accountNamePref = accountNamePref,
        onAccountNameChange = onAccountNameChange,
        accountEmail = accountEmail,
        onAccountEmailChange = onAccountEmailChange,
        accountChannelHandle = accountChannelHandle,
        onAccountChannelHandleChange = onAccountChannelHandleChange,
        innerTubeCookie = innerTubeCookie,
        onInnerTubeCookieChange = onInnerTubeCookieChange,
        visitorData = visitorData,
        onVisitorDataChange = onVisitorDataChange,
        dataSyncId = dataSyncId,
        onDataSyncIdChange = onDataSyncIdChange,
        useLoginForBrowse = useLoginForBrowse,
        onUseLoginForBrowseChange = onUseLoginForBrowseChange,
        ytmSync = ytmSync,
        onYtmSyncChange = onYtmSyncChange,
        forceSyncOnAccountSwitch = forceSyncOnAccountSwitch,
        onForceSyncOnAccountSwitchChange = onForceSyncOnAccountSwitchChange,
        selectedYtmPlaylists = selectedYtmPlaylists,
        savedAccountsJson = savedAccountsJson,
        onSavedAccountsJsonChange = onSavedAccountsJsonChange,
        savedAccounts = savedAccounts,
        onLegacyPoTokenChange = onLegacyPoTokenChange,
        isLoggedIn = isLoggedIn,
        showSpotifyPlaylists = showSpotifyPlaylists,
        onShowSpotifyPlaylistsChange = onShowSpotifyPlaylistsChange,
        useSpotifyHome = useSpotifyHome,
        onUseSpotifyHomeChange = onUseSpotifyHomeChange,
        spotifySyncLikes = spotifySyncLikes,
        onSpotifySyncLikesChange = onSpotifySyncLikesChange,
        hideYtmLikedSongs = hideYtmLikedSongs,
        onHideYtmLikedSongsChange = onHideYtmLikedSongsChange,
    )
}
