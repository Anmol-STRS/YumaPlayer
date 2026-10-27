/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.settings.account.header

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.first
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.ui.theme.yumaClickable
import moe.rukamori.archivetune.ui.utils.LocalPlayerIsExpanded
import kotlin.math.cos
import kotlin.math.sin
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import moe.rukamori.archivetune.utils.ImageBlurUtils

private data class StarPoint(val angle: Float, val scale: Float, val alpha: Float)

private val FrontOrbitStars = listOf(
    StarPoint(angle = -60f, scale = 1.0f, alpha = 0.50f),
    StarPoint(angle = 70f, scale = 0.66f, alpha = 0.35f),
    StarPoint(angle = 190f, scale = 0.50f, alpha = 0.225f)
)

private val BackOrbitStars = listOf(
    StarPoint(angle = 20f, scale = 0.55f, alpha = 0.22f),
    StarPoint(angle = 145f, scale = 0.42f, alpha = 0.165f),
    StarPoint(angle = 265f, scale = 0.50f, alpha = 0.193f)
)

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
        stringResource(R.string.account)
    } else {
        stringResource(R.string.login)
    }
    val actionIcon = if (isLoggedIn) R.drawable.account else R.drawable.login

    var blurredAvatar by remember { mutableStateOf<Bitmap?>(null) }
    val coroutineScope = rememberCoroutineScope()

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

                            // Подготовка текстуры 192x192 с blur 8px ровно как в reLynth
                            coroutineScope.launch(Dispatchers.Default) {
                                val scaled = Bitmap.createScaledBitmap(bitmap, 192, 192, true)
                                val blurred = ImageBlurUtils.blur(scaled, 8f)
                                withContext(Dispatchers.Main) {
                                    blurredAvatar = blurred
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.18f),
                            shape = CircleShape
                        )
                )

                ActiveFrontOrbitRing(isPaused = shouldPauseAnimation)
            }
            // ... далее разметка имени, хэндла и кнопок без изменений

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
@Composable
private fun ActiveBackOrbitRing(
    isPaused: Boolean,
    modifier: Modifier = Modifier,
) {
    val rotationProvider = rememberOrbitRotation(
        durationMillis = 48000L,
        startAngle = 360f,
        endAngle = 0f,
        isPaused = isPaused,
    )
    OrbitRing(
        stars = BackOrbitStars,
        rotationProvider = rotationProvider,
        radius = 56.dp,
        modifier = modifier,
    )
}

@Composable
private fun ActiveFrontOrbitRing(
    isPaused: Boolean,
    modifier: Modifier = Modifier,
) {
    val rotationProvider = rememberOrbitRotation(
        durationMillis = 32000L,
        startAngle = 0f,
        endAngle = 360f,
        isPaused = isPaused,
    )
    OrbitRing(
        stars = FrontOrbitStars,
        rotationProvider = rotationProvider,
        radius = 48.dp,
        modifier = modifier,
    )
}

@Composable
private fun rememberOrbitRotation(
    durationMillis: Long,
    startAngle: Float,
    endAngle: Float,
    isPaused: Boolean,
): () -> Float {
    var currentRotation by rememberSaveable { mutableFloatStateOf(startAngle) }

    LaunchedEffect(durationMillis, startAngle, endAngle, isPaused) {
        if (isPaused) return@LaunchedEffect

        val sweep = endAngle - startAngle
        val speed = sweep / durationMillis.toFloat()
        var lastFrameMs = withInfiniteAnimationFrameMillis { it }

        while (true) {
            withInfiniteAnimationFrameMillis { frameMs ->
                val delta = (frameMs - lastFrameMs).coerceAtLeast(0L)
                lastFrameMs = frameMs
                currentRotation = startAngle + ((currentRotation - startAngle + delta * speed) % sweep)
            }
        }
    }
    return remember { { currentRotation } }
}

@Composable
private fun OrbitRing(
    stars: List<StarPoint>,
    rotationProvider: () -> Float,
    radius: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationZ = rotationProvider()
            },
        contentAlignment = Alignment.Center,
    ) {
        stars.forEach { star ->
            val rad = Math.toRadians(star.angle.toDouble())
            val x = (cos(rad) * radius.value).dp
            val y = (sin(rad) * radius.value).dp
            val starSize = 12.dp * star.scale

            StarShapeIcon(
                size = starSize,
                color = Color.White.copy(alpha = star.alpha),
                modifier = Modifier.offset(x = x, y = y)
            )
        }
    }
}

@Composable
private fun StarShapeIcon(
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                val w = this.size.width
                val h = this.size.height
                val path = Path().apply {
                    moveTo(w * 0.5f, 0f)
                    cubicTo(w * 0.5375f, h * 0.308f, w * 0.692f, h * 0.4625f, w, h * 0.5f)
                    cubicTo(w * 0.692f, h * 0.5375f, w * 0.5375f, h * 0.692f, w * 0.5f, h)
                    cubicTo(w * 0.4625f, h * 0.692f, w * 0.308f, h * 0.5375f, 0f, h * 0.5f)
                    cubicTo(w * 0.308f, h * 0.4625f, w * 0.4625f, h * 0.308f, w * 0.5f, 0f)
                    close()
                }
                drawPath(path = path, color = color, style = Fill)
            }
    )
}