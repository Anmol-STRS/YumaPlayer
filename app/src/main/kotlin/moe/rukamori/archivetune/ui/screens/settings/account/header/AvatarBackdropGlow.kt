/*
 * YumaPlayer (2026) | Original work by MuwMix
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.settings.account.header

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.unit.dp
import org.intellij.lang.annotations.Language
import kotlin.math.exp

@Language("AGSL")
private const val AVATAR_WARP_SHADER = """
uniform float2 resolution;
uniform float time;
layout(color) uniform half4 baseColor;

// ==========================================
// VISUAL CONTROLS
// ==========================================
const float2 GLOW_CENTER = float2(0.50, 0.32);
const float GLOW_WIDTH = 1.20;
const float GLOW_HEIGHT = 1.30;
const float GLOW_SOFTNESS = 1.70;

const float NOISE_SCALE = 0.70;
const float NOISE_DETAIL = 0.90;
const float NOISE_WARP = 2.20;
const float WARP_AMOUNT = 0.12;

const float LIGHT_VARIATION = 0.18;
const float LOOP_DISTANCE = 4.0;

const float FADE_START = 0.40;
const float FADE_END = 0.88;

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
// DOMAIN WARPING (Бесшовный цикл)
// ==========================================
vec2 warpedNoise(vec2 uv, float t) {
    float phase = fract(t / LOOP_DISTANCE);
    float blend = phase * phase * (3.0 - 2.0 * phase);

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
// ОСНОВНОЙ ВЫЧИСЛИТЕЛЬ
// ==========================================
half4 main(float2 fragCoord) {
    vec2 uv = fragCoord / resolution;

    vec2 warp = warpedNoise(uv, time);

    vec2 domeUv = (uv - GLOW_CENTER) + warp * WARP_AMOUNT;

    float dBase = length(domeUv * vec2(1.0, GLOW_HEIGHT)) * GLOW_WIDTH;
    float baseGlow = exp(-GLOW_SOFTNESS * dBase * dBase);

    float dCore = length(domeUv * vec2(1.0, 1.6)) * 2.1;
    float coreGlow = exp(-3.8 * dCore * dCore) * 0.45;

    float totalGlow = baseGlow + coreGlow;

    float bottomFade = smoothstep(FADE_END, FADE_START, uv.y);

    float dither = (fract(dot(fragCoord, vec2(0.7548776662, 0.5698402909))) - 0.5) / 255.0;

    float finalAlpha = clamp(totalGlow * bottomFade * baseColor.a + dither, 0.0, 1.0);
    half3 finalRgb = baseColor.rgb * finalAlpha;

    return half4(finalRgb, finalAlpha);
}
"""

@Composable
fun AvatarBackdropGlow(
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AgslBackdropGlow(
            accentColor = accentColor,
            modifier = modifier
        )
    } else {
        FallbackBackdropGlow(
            accentColor = accentColor,
            modifier = modifier
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun AgslBackdropGlow(
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "avatarBackdropTransition")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 4.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 55000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shaderTime"
    )

    val shader = remember { RuntimeShader(AVATAR_WARP_SHADER) }
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp + statusBarHeight)
    ) {
        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setFloatUniform("time", time)
        shader.setColorUniform(
            "baseColor",
            android.graphics.Color.valueOf(
                accentColor.red,
                accentColor.green,
                accentColor.blue,
                0.70f
            )
        )

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