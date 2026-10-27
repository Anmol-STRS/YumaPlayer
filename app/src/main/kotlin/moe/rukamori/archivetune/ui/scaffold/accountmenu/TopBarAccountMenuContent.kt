/*
 * YumaPlayer (2026) | Original work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.scaffold.accountmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import moe.rukamori.archivetune.ui.theme.yumaClickable

@Composable
internal fun TopBarAccountMenuContent(
    avatarUrl: String?,
    accountName: String,
    onNavigateProfile: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateNews: () -> Unit,
    onNavigateNewRelease: () -> Unit,
    onNavigateSettings: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    hasUnreadNews: Boolean = false,
    hasUpdate: Boolean = false,
) {
    val gradientStart = accentColor?.copy(alpha = 0.35f)
        ?: MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .yumaClickable(pressedScale = 0.96f, onClick = onNavigateProfile),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            gradientStart,
                            Color.Transparent,
                        ),
                    ),
                )
                .border(
                    width = SettingsDimensions.GlassBorderThickness,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
                    shape = RoundedCornerShape(16.dp),
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = SettingsDimensions.RowChevronAlpha),
                    modifier = Modifier.size(SettingsDimensions.ChevronSize),
                )
            }
        }
    }

    TopBarDropdownMenuItem(
        icon = painterResource(R.drawable.history),
        title = stringResource(R.string.history),
        onClick = onNavigateHistory,
    )

    TopBarDropdownMenuItem(
        icon = painterResource(R.drawable.newspaper),
        title = stringResource(R.string.news),
        badge = hasUnreadNews,
        onClick = onNavigateNews,
    )

    TopBarDropdownMenuItem(
        icon = painterResource(R.drawable.new_release),
        title = stringResource(R.string.new_release_albums),
        onClick = onNavigateNewRelease,
    )

    TopBarDropdownMenuItem(
        icon = painterResource(R.drawable.ic_settings),
        title = stringResource(R.string.settings),
        badge = hasUpdate,
        onClick = onNavigateSettings,
    )
}

@Composable
private fun TopBarDropdownMenuItem(
    icon: Painter,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Boolean = false,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .yumaClickable(pressedScale = 0.96f, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                Badge(
                    modifier = Modifier.size(8.dp),
                )
            }

            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = SettingsDimensions.RowChevronAlpha),
                modifier = Modifier.size(SettingsDimensions.ChevronSize),
            )
        }
    }
}
