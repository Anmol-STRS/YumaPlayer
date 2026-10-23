package moe.rukamori.archivetune.home.effects

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TILT_TARGET_THRESHOLD = 0.02f
internal const val BACKGROUND_STEP_INTERVAL_MS = 16f
private const val ANIMATED_TIME_WRAP_MS = 3_600_000f

/**
 * Time accumulator that respects a [speedMultiplier] and steps once per 16ms.
 * Preserves compatibility with all Yuma backgrounds.
 */
@Composable
fun rememberAnimatedTime(
    speedMultiplier: Float,
    isVisible: Boolean = true,
    label: String = "rememberAnimatedTime",
): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    val targetSpeed = remember { mutableFloatStateOf(speedMultiplier) }
    SideEffect { targetSpeed.floatValue = speedMultiplier }

    val isVisibleState by rememberUpdatedState(isVisible)
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner, isVisibleState) {
        if (!isVisibleState) return@LaunchedEffect

        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var lastFrameMs = withInfiniteAnimationFrameMillis { it }
            var currentSpeed = targetSpeed.floatValue
            var elapsedMs = 0f
            var pendingMs = 0f

            while (true) {
                if (currentSpeed == 0f && targetSpeed.floatValue == 0f) {
                    snapshotFlow { targetSpeed.floatValue }.first { it > 0f }
                    lastFrameMs = withInfiniteAnimationFrameMillis { it }
                }

                withInfiniteAnimationFrameMillis { frameMs ->
                    val delta = (frameMs - lastFrameMs).coerceIn(0L, 64L).toFloat()
                    lastFrameMs = frameMs

                    currentSpeed += (targetSpeed.floatValue - currentSpeed) * (delta / 1000f) * 2.5f
                    if (kotlin.math.abs(currentSpeed) < 0.001f && targetSpeed.floatValue == 0f) {
                        currentSpeed = 0f
                    }

                    elapsedMs += delta
                    pendingMs += delta * currentSpeed

                    if (elapsedMs >= BACKGROUND_STEP_INTERVAL_MS) {
                        time.floatValue = (time.floatValue + pendingMs) % ANIMATED_TIME_WRAP_MS
                        elapsedMs -= BACKGROUND_STEP_INTERVAL_MS
                        pendingMs = 0f
                    }
                }
            }
        }
    }
    return time
}

/**
 * Parallax sensor state holder
 */
data class ParallaxState(
    val tiltX: State<Float>,
    val tiltY: State<Float>
)

/**
 * Morphe-based parallax effect using device accelerometer + Compose physics springs.
 */
@Composable
fun rememberParallaxState(
    enableParallax: Boolean,
    sensitivity: Float = 0.3f,
    context: Context
): ParallaxState {
    val smoothTiltX = remember { Animatable(0f) }
    val smoothTiltY = remember { Animatable(0f) }
    val tiltTarget = remember { MutableStateFlow(Offset.Zero) }

    val lifecycleOwner = LocalLifecycleOwner.current
    val currentSensitivity by rememberUpdatedState(sensitivity)

    LaunchedEffect(enableParallax) {
        if (!enableParallax) {
            tiltTarget.value = Offset.Zero
            smoothTiltX.snapTo(0f)
            smoothTiltY.snapTo(0f)
            return@LaunchedEffect
        }

        tiltTarget.collectLatest { target ->
            coroutineScope {
                launch {
                    smoothTiltX.animateTo(
                        targetValue = target.x,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
                launch {
                    smoothTiltY.animateTo(
                        targetValue = target.y,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
            }
        }
    }

    DisposableEffect(enableParallax, lifecycleOwner) {
        if (!enableParallax) {
            return@DisposableEffect onDispose { }
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?: return@DisposableEffect onDispose { }

        var baselineX = 0f
        var baselineY = 0f
        var isCalibrated = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (!isCalibrated) {
                    baselineX = event.values[0]
                    baselineY = event.values[1]
                    isCalibrated = true
                }

                val rawTiltX = event.values[0] - baselineX
                val rawTiltY = -(event.values[1] - baselineY)
                val target = Offset(rawTiltX * currentSensitivity, rawTiltY * currentSensitivity)

                if ((target - tiltTarget.value).getDistance() > TILT_TARGET_THRESHOLD) {
                    tiltTarget.value = target
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                sensorManager.registerListener(
                    listener,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_UI
                )
            } else if (event == Lifecycle.Event.ON_STOP) {
                sensorManager.unregisterListener(listener)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            sensorManager.unregisterListener(listener)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return ParallaxState(
        tiltX = smoothTiltX.asState(),
        tiltY = smoothTiltY.asState()
    )
}