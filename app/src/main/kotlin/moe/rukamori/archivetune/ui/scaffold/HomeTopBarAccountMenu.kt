/*
 * YumaPlayer (2026) | Original work by MuwMix
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.scaffold

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.ui.settings.SettingsDimensions
import moe.rukamori.archivetune.ui.theme.LocalYumaColors
import moe.rukamori.archivetune.ui.theme.yumaClickable

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
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        AccountMenuTriggerButton(
            avatarUrl = avatarUrl,
            hasBadge = hasUnreadNews || hasUpdate,
            onClick = { expanded = !expanded },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 230.dp, max = 290.dp),
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(
                width = SettingsDimensions.GlassBorderThickness,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            ),
        ) {
            AccountMenuProfileCard(
                avatarUrl = avatarUrl,
                accountName = accountName,
                accentColor = accentColor,
                onClick = {
                    expanded = false
                    onNavigateProfile()

                },
            )

            TopBarDropdownMenuItem(
                icon = painterResource(R.drawable.history),
                title = stringResource(R.string.history),
                onClick = {
                    expanded = false
                    onNavigateHistory()
                },
            )

            TopBarDropdownMenuItem(
                icon = painterResource(R.drawable.newspaper),
                title = stringResource(R.string.news),
                badge = hasUnreadNews,
                onClick = {
                    expanded = false
                    onNavigateNews()
                },
            )

            TopBarDropdownMenuItem(
                icon = painterResource(R.drawable.new_release),
                title = stringResource(R.string.new_release_albums),
                onClick = {
                    expanded = false
                    onNavigateNewRelease()
                },
            )

            TopBarDropdownMenuItem(
                icon = painterResource(R.drawable.ic_settings),
                title = stringResource(R.string.settings),
                badge = hasUpdate,
                onClick = {
                    expanded = false
                    onNavigateSettings()
                },
            )
        }
    }
}

@Composable
private fun AccountMenuTriggerButton(
    avatarUrl: String?,
    hasBadge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .yumaClickable(
                pressedScale = 0.94f,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BadgedBox(
            badge = {
                if (hasBadge) {
                    Badge(modifier = Modifier.offset(x = (-2).dp, y = 2.dp))
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
}

@Composable
private fun AccountMenuProfileCard(
    avatarUrl: String?,
    accountName: String,
    accentColor: Color?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalYumaColors.current
    val cardShape = RoundedCornerShape(16.dp)

    val animatedAccentColor by animateColorAsState(
        targetValue = accentColor ?: MaterialTheme.colorScheme.primaryContainer,
        label = "accountAccentColorAnimation",
    )

    val gradientStart = if (accentColor != null) {
        animatedAccentColor.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(cardShape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(gradientStart, colors.glassBackground),
                ),
            )
            .border(
                width = SettingsDimensions.GlassBorderThickness,
                color = colors.glassBorder,
                shape = cardShape,
            )
            .yumaClickable(
                pressedScale = 0.96f,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = avatarUrl,
                placeholder = painterResource(R.drawable.person),
                error = painterResource(R.drawable.person),
                fallback = painterResource(R.drawable.person),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(
                        width = SettingsDimensions.GlassBorderThickness,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f),
                        shape = CircleShape,
                    ),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = accountName.ifBlank { stringResource(R.string.account) },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (accountName.isNotBlank()) {
                        stringResource(R.string.account)
                    } else {
                        stringResource(R.string.not_logged_in)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = SettingsDimensions.RowChevronAlpha,
                ),
                modifier = Modifier.size(SettingsDimensions.ChevronSize),
            )
        }
    }
}

@Composable
private fun TopBarDropdownMenuItem(
    icon: Painter,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Boolean = false,
) {
    val itemShape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(itemShape)
            .yumaClickable(
                pressedScale = 0.96f,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (badge) {
            Badge(modifier = Modifier.size(8.dp))
        }

        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = SettingsDimensions.RowChevronAlpha,
            ),
            modifier = Modifier.size(SettingsDimensions.ChevronSize),
        )
    }
}