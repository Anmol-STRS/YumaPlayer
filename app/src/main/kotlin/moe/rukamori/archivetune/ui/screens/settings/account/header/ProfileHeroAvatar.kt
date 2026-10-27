/*
 * YumaPlayer (2026) | Original work by MuwMix
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.settings.account.header

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.scale
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.utils.ImageBlurUtils

@Composable
internal fun ProfileHeroAvatar(
    avatarUrl: String?,
    onAvatarBlurred: (Bitmap) -> Unit,
    modifier: Modifier = Modifier,
    onAvatarPixelsReady: ((IntArray) -> Unit)? = null,
) {
    val coroutineScope = rememberCoroutineScope()

    AsyncImage(
        model = avatarUrl,
        placeholder = painterResource(R.drawable.person),
        error = painterResource(R.drawable.person),
        fallback = painterResource(R.drawable.person),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        onSuccess = { successResult ->
            val drawable = successResult.result.image
            val bitmap = (drawable as? coil3.BitmapImage)?.bitmap
            if (bitmap != null) {
                if (onAvatarPixelsReady != null) {
                    val pixels = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    onAvatarPixelsReady(pixels)
                }

                coroutineScope.launch(Dispatchers.Default) {
                    val scaled = bitmap.scale(192, 192)
                    val blurred = ImageBlurUtils.blur(scaled, 8f)
                    withContext(Dispatchers.Main) {
                        onAvatarBlurred(blurred)
                    }
                }
            }
        },
        modifier = modifier
            .size(88.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.18f),
                shape = CircleShape
            )
    )
}
