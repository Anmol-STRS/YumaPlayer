/*
 * YumaPlayer (2026) | Original work by MuwMix
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.settings.account.header

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.unit.dp
import org.intellij.lang.annotations.Language
import kotlin.math.exp

// ==========================================
// ANIMATION TIMING CONTROLS
// ==========================================
private const val GLOW_LOOP_DURATION_MS = 45000f
private const val GLOW_LOOP_DISTANCE = 4.0f
private const val GLOW_SPEED = GLOW_LOOP_DISTANCE / GLOW_LOOP_DURATION_MS

@Language("AGSL")
private const val AVATAR_WARP_SHADER = """
uniform float2 resolution;
uniform float time;
layout(color) uniform half4 baseColor;
uniform float hasAvatar;
uniform shader avatarTex;

// ==========================================
// LIGHT GEOMETRY & FALLOFF
// ==========================================
const float2 GLOW_CENTER = float2(0.50, 0.22);
const float GLOW_WIDTH = 0.90;
const float GLOW_HEIGHT = 1.10;
const float GLOW_FALLOFF = 1.45;
const float OPACITY = 0.65;

const float FADE_START = 0.35;
const float FADE_END = 0.85;

// ==========================================
// WAVE CONTROLS & DOWNWARD MOTION
// ==========================================
const float NOISE_SCALE = 0.90;
const float NOISE_DETAIL = 1.20;
const float NOISE_WARP = 2.20;
const float WARP_AMOUNT = 0.14;
const float LOOP_DISTANCE = 4.0;

// ==========================================
// SIMPLEX NOISE
// ==========================================
vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec2 mod289(vec2 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec3 permute(vec3 x) { return mod289(((x * 34.0) + 1.0) * x); }

float snoise(vec2 v) {
    const vec4 C = vec4(
        0.211324865405187,
        0.366025403784439,
        -0.577350269189626,
        0.024390243902439
    );

    vec2 i = floor(v + dot(v, C.yy));
    vec2 x0 = v - i + dot(i, C.xx);
    vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);

    vec4 x12 = x0.xyxy + C.xxzz;
    x12.xy -= i1;
    i = mod289(i);

    vec3 p = permute(
        permute(i.y + vec3(0.0, i1.y, 1.0))
        + i.x + vec3(0.0, i1.x, 1.0)
    );

    vec3 m = max(
        0.5 - vec3(
            dot(x0, x0),
            dot(x12.xy, x12.xy),
            dot(x12.zw, x12.zw)
        ),
        0.0
    );

    m = m * m;
    m = m * m;

    vec3 x = 2.0 * fract(p * C.www) - 1.0;
    vec3 h = abs(x) - 0.5;
    vec3 ox = floor(x + 0.5);
    vec3 a0 = x - ox;

    m *= 1.79284291400159 - 0.85373472095314 * (a0 * a0 + h * h);

    vec3 g;
    g.x = a0.x * x0.x + h.x * x0.y;
    g.yz = a0.yz * x12.xz + h.yz * x12.yw;

    return 130.0 * dot(m, g);
}

// ==========================================
// DOMAIN WARPING (Seamless Loop)
// ==========================================
vec2 warpedNoise(vec2 uv, float t) {
    float phase = fract(t / LOOP_DISTANCE);
    float blend = phase * phase * (3.0 - 2.0 * phase);

    // Y-axis phase offset creates seamless downward flow
    float offsetA = -phase * LOOP_DISTANCE;
    float offsetB = (1.0 - phase) * LOOP_DISTANCE;

    vec2 qA = vec2(
        snoise(uv * NOISE_SCALE + vec2(0.0, offsetA)),
        snoise(uv * NOISE_SCALE + vec2(5.2, 1.3 + offsetA))
    );
    vec2 qB = vec2(
        snoise(uv * NOISE_SCALE + vec2(0.0, offsetB)),
        snoise(uv * NOISE_SCALE + vec2(5.2, 1.3 + offsetB))
    );

    vec2 rA = vec2(
        snoise(uv * NOISE_DETAIL + NOISE_WARP * qA + vec2(1.7, 9.2 + offsetA * 0.7)),
        snoise(uv * NOISE_DETAIL + NOISE_WARP * qA + vec2(8.3, 2.8 + offsetA * 0.6))
    );
    vec2 rB = vec2(
        snoise(uv * NOISE_DETAIL + NOISE_WARP * qB + vec2(1.7, 9.2 + offsetB * 0.7)),
        snoise(uv * NOISE_DETAIL + NOISE_WARP * qB + vec2(8.3, 2.8 + offsetB * 0.6))
    );

    return mix(rA, rB, blend);
}

// ==========================================
// MAIN SHADER
// ==========================================
half4 main(float2 fragCoord) {
    vec2 uv = fragCoord / resolution;

    // Distortion vector
    vec2 warp = warpedNoise(uv, time);

    // Warped glow dome coordinates
    vec2 domeUv = (uv - GLOW_CENTER) + warp * WARP_AMOUNT;

    // Broad soft elliptical Gaussian falloff
    float d = length(domeUv * vec2(GLOW_WIDTH, GLOW_HEIGHT));
    float edge = exp(-GLOW_FALLOFF * d * d) * OPACITY;

    // Bottom edge fade-out
    float bottomFade = smoothstep(FADE_END, FADE_START, uv.y);

    // Dithering
    float dither = (fract(dot(fragCoord, vec2(0.7548776662, 0.5698402909))) - 0.5) / 255.0;

    float rawAlpha = edge * bottomFade;
    float alpha = clamp(rawAlpha + (rawAlpha > 0.01 ? dither : 0.0), 0.0, 1.0);

    half3 color;
    if (hasAvatar > 0.5) {
        vec2 sampleUv = clamp((domeUv * 0.85) + 0.5, 0.0, 1.0);
        color = avatarTex.eval(sampleUv * resolution).rgb + dither;
    } else {
        color = baseColor.rgb;
    }

    return half4(color * alpha, alpha);
}
"""

@Composable
fun AvatarBackdropGlow(
    accentColor: Color,
    avatarBitmap: Bitmap? = null,
    modifier: Modifier = Modifier,
    isPaused: Boolean = false,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AgslBackdropGlow(
            accentColor = accentColor,
            avatarBitmap = avatarBitmap,
            isPaused = isPaused,
            modifier = modifier,
        )
    } else {
        FallbackBackdropGlow(
            accentColor = accentColor,
            modifier = modifier,
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun AgslBackdropGlow(
    accentColor: Color,
    avatarBitmap: Bitmap?,
    isPaused: Boolean,
    modifier: Modifier = Modifier,
) {
    var currentTime by rememberSaveable { mutableFloatStateOf(0f) }

    LaunchedEffect(isPaused) {
        if (isPaused) return@LaunchedEffect

        var lastFrameMs = withInfiniteAnimationFrameMillis { it }
        while (true) {
            withInfiniteAnimationFrameMillis { frameMs ->
                val delta = (frameMs - lastFrameMs).coerceAtLeast(0L)
                lastFrameMs = frameMs
                currentTime = (currentTime + delta * GLOW_SPEED) % GLOW_LOOP_DISTANCE
            }
        }
    }

    val shader = remember { RuntimeShader(AVATAR_WARP_SHADER) }
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val bitmapShader = remember(avatarBitmap) {
        avatarBitmap?.let {
            BitmapShader(it, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp + statusBarHeight)
    ) {
        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setFloatUniform("time", currentTime)
        shader.setColorUniform(
            "baseColor",
            android.graphics.Color.valueOf(
                accentColor.red,
                accentColor.green,
                accentColor.blue,
                1.0f
            )
        )

        if (bitmapShader != null && avatarBitmap != null) {
            val matrix = Matrix()
            matrix.setScale(
                size.width / avatarBitmap.width.toFloat(),
                size.height / avatarBitmap.height.toFloat()
            )
            bitmapShader.setLocalMatrix(matrix)

            shader.setFloatUniform("hasAvatar", 1.0f)
            shader.setInputShader("avatarTex", bitmapShader)
        } else {
            shader.setFloatUniform("hasAvatar", 0.0f)
        }

        drawRect(brush = ShaderBrush(shader))
    }
}

@Composable
private fun FallbackBackdropGlow(
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val colorStops = remember(accentColor) {
        val f = 2.6f
        Array(6) { i ->
            val l = i / 5.0f
            val alpha = if (i == 5) 0f else exp((-f * l * l).toDouble()).toFloat() * 0.62f
            l to accentColor.copy(alpha = alpha)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        drawRect(
            brush = Brush.radialGradient(
                colorStops = colorStops,
                center = Offset(size.width / 2f, size.height * 0.28f),
                radius = size.width * 0.65f
            )
        )
    }
}