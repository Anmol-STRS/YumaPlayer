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

@Language("AGSL")
private const val RELYNTH_WARP_SHADER = """
uniform float2 resolution;
uniform float time;
layout(color) uniform half4 baseColor;
uniform float hasAvatar;
uniform shader avatarTex;

const float2 GLOW_CENTER = float2(0.50, 0.42);

const float K_ASPECT = 1.70;
const float F_FALLOFF = 2.60;
const float OPACITY = 0.62;

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

half4 main(float2 fragCoord) {
    vec2 uv = fragCoord / resolution;
    float t = time * 0.055;

    vec2 q = vec2(
        snoise(uv * 1.0 + vec2(0.0, t)),
        snoise(uv * 1.0 + vec2(5.2, 1.3 + t))
    );
    vec2 r = vec2(
        snoise(uv * 1.3 + 2.2 * q + vec2(1.7, 9.2 + t * 0.7)),
        snoise(uv * 1.3 + 2.2 * q + vec2(8.3, 2.8 + t * 0.6))
    );

    vec2 warped = uv + 0.12 * r;

    float d = length((uv - GLOW_CENTER) * vec2(1.0, K_ASPECT)) * 2.0;
    float edge = exp(-F_FALLOFF * d * d) * OPACITY;

    float dither = (fract(dot(fragCoord, vec2(0.7548776662, 0.5698402909))) - 0.5) / 255.0;
    float alpha = clamp(edge + (edge > 0.005 ? dither : 0.0), 0.0, 1.0);

    half3 color;
    if (hasAvatar > 0.5) {
        vec2 sampleUv = clamp((warped - GLOW_CENTER) * 0.9 + 0.5, 0.0, 1.0);
        color = avatarTex.eval(sampleUv * resolution).rgb + dither;
    } else {
        color = baseColor.rgb;
    }

    return half4(color * alpha, alpha);
}
"""
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
                currentTime += delta / 1000f
            }
        }
    }

    val shader = remember { RuntimeShader(RELYNTH_WARP_SHADER) }
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