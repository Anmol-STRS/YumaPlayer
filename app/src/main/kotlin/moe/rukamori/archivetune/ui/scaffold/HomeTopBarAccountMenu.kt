/*
 * YumaPlayer (2026) | Original work by MuwMix
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.scaffold

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.ui.settings.SettingsDimensions
import moe.rukamori.archivetune.ui.theme.yumaClickable

@Composable
fun HomeTopBarAccountMenu(
    avatarUrl: String?,
    hasUnreadNews: Boolean,
    hasUpdate: Boolean,
    onNavigateProfile: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateNews: () -> Unit,
    onNavigateNewRelease: () -> Unit,
    onNavigateSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val hasBadge = hasUnreadNews || hasUpdate

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .yumaClickable(
                    pressedScale = 0.94f,
                    onClick = { expanded = !expanded },
                ),
            contentAlignment = Alignment.Center,
        ) {
            BadgedBox(
                badge = {
                    if (hasBadge) {
                        Badge(
                            modifier = Modifier.offset(x = (-2).dp, y = 2.dp),
                        )
                    }
                },
            ) {
                AsyncImage(
                    model = avatarUrl,
                    placeholder = painterResource(R.drawable.person),
                    error = painterResource(R.drawable.person),
                    fallback = painterResource(R.drawable.person),
                    contentDescription = stringResource(R.string.account),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(
                            width = SettingsDimensions.GlassBorderThickness,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f),
                            shape = CircleShape,
                        ),
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(
                width = SettingsDimensions.GlassBorderThickness,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            ),
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.account)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.account),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                },
                onClick = {
                    expanded = false
                    onNavigateProfile()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.history)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.history),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                },
                onClick = {
                    expanded = false
                    onNavigateHistory()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.news)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.newspaper),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                },
                trailingIcon = if (hasUnreadNews) {
                    { Badge() }
                } else null,
                onClick = {
                    expanded = false
                    onNavigateNews()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.new_release_albums)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.new_release),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                },
                onClick = {
                    expanded = false
                    onNavigateNewRelease()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.settings)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                },
                trailingIcon = if (hasUpdate) {
                    { Badge() }
                } else null,
                onClick = {
                    expanded = false
                    onNavigateSettings()
                },
            )
        }
    }
}
