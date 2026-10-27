/*
 * YumaPlayer (2026) | Original work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.HazeState
import moe.rukamori.archivetune.ui.scaffold.accountmenu.TopBarAccountAnchor
import moe.rukamori.archivetune.ui.scaffold.accountmenu.TopBarAccountDropdownMenu
import moe.rukamori.archivetune.ui.scaffold.accountmenu.TopBarAccountMenuContent
import moe.rukamori.archivetune.ui.settings.SettingsDimensions

@Composable
fun HomeTopBarAccountMenu(
    avatarUrl: String?,
    accountName: String,
    hasUnreadNews: Boolean,
    hasUpdate: Boolean,
    onNavigateProfile: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateNews: () -> Unit,
    onNavigateNewRelease: () -> Unit,
    onNavigateSettings: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    hazeState: HazeState? = null,
    blurRadius: Float = SettingsDimensions.BlurRadiusDefault,
    blurNavBar: Boolean = true,
    pureBlack: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        TopBarAccountAnchor(
            avatarUrl = avatarUrl,
            hasBadge = hasUnreadNews || hasUpdate,
            onClick = { expanded = !expanded },
        )

        TopBarAccountDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            hazeState = hazeState,
            blurRadius = blurRadius,
            blurNavBar = blurNavBar,
            pureBlack = pureBlack,
        ) {
            TopBarAccountMenuContent(
                avatarUrl = avatarUrl,
                accountName = accountName,
                accentColor = accentColor,
                hasUnreadNews = hasUnreadNews,
                hasUpdate = hasUpdate,
                onNavigateProfile = {
                    expanded = false
                    onNavigateProfile()
                },
                onNavigateHistory = {
                    expanded = false
                    onNavigateHistory()
                },
                onNavigateNews = {
                    expanded = false
                    onNavigateNews()
                },
                onNavigateNewRelease = {
                    expanded = false
                    onNavigateNewRelease()
                },
                onNavigateSettings = {
                    expanded = false
                    onNavigateSettings()
                },
            )
        }
    }
}
