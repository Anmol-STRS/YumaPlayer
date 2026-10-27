/*
 * YumaPlayer (2026) | Original work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.scaffold.accountmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
internal fun TopBarAccountAnchor(
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
}
