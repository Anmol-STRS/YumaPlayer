/*
 * YumaPlayer (2026) | Original work by MuwMix
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.settings.account.header

import android.graphics.Bitmap
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.ui.theme.yumaClickable
import moe.rukamori.archivetune.ui.utils.LocalPlayerIsExpanded

@Composable
fun ProfileHeroHeader(
    avatarUrl: String?,
    displayName: String,
    handle: String?,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    isLoggedIn: Boolean = true,
    actionLabel: String? = null,
    isEffectActive: Boolean = true,
    isViewportVisible: Boolean = true,
    onAvatarPixelsReady: ((IntArray) -> Unit)? = null,
    onAccountActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPlayerExpanded = LocalPlayerIsExpanded.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLifecycleResumed by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isLifecycleResumed = event.targetState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val shouldPauseAnimation = !isEffectActive || !isLifecycleResumed || isPlayerExpanded || !isViewportVisible

    val resolvedActionLabel = actionLabel ?: if (isLoggedIn) {
        stringResource(R.string.settings)
    } else {
        stringResource(R.string.login)
    }
    val actionIcon = if (isLoggedIn) R.drawable.settings else R.drawable.login

    var blurredAvatar by remember { mutableStateOf<Bitmap?>(null) }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        AvatarBackdropGlow(
            accentColor = accentColor,
            avatarBitmap = blurredAvatar,
            isPaused = shouldPauseAnimation,
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                ActiveBackOrbitRing(isPaused = shouldPauseAnimation)

                ProfileHeroAvatar(
                    avatarUrl = avatarUrl,
                    onAvatarBlurred = { blurredAvatar = it },
                    onAvatarPixelsReady = onAvatarPixelsReady,
                )

                ActiveFrontOrbitRing(isPaused = shouldPauseAnimation)
            }

            Text(
                text = displayName.ifBlank { stringResource(R.string.account) },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 0.dp)
            )

            if (!handle.isNullOrBlank()) {
                Text(
                    text = if (handle.startsWith("@")) handle else "@$handle",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .height(36.dp)
                    .yumaClickable(pressedScale = 0.95f, onClick = onAccountActionClick)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(50)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(actionIcon),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = resolvedActionLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}
