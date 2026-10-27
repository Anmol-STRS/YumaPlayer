/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec2 mod289(vec2 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec3 permute(vec3 x) { return mod289(((x * 34.0) + 1.0) * x); }

float snoise(vec2 v) {
    const vec4 C = vec4(0.211324865405187, 0.366025403784439,
                        -0.577350269189626, 0.024390243902439);
    vec2 i  = floor(v + dot(v, C.yy));
    vec2 x0 = v -   i + dot(i, C.xx);
    vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
    vec4 x12 = x0.xyxy + C.xxzz;
    x12.xy -= i1;
    i = mod289(i);
    vec3 p = permute(permute(i.y + vec3(0.0, i1.y, 1.0))
                   + i.x + vec3(0.0, i1.x, 1.0));
    vec3 m = max(0.5 - vec3(dot(x0, x0), dot(x12.xy, x12.xy),
                            dot(x12.zw, x12.zw)), 0.0);
    m = m * m; m = m * m;
    vec3 x = 2.0 * fract(p * C.www) - 1.0;
    vec3 h = abs(x) - 0.5;
    vec3 ox = floor(x + 0.5);
    vec3 a0 = x - ox;
    m *= 1.79284291400159 - 0.85373472095314 * (a0 * a0 + h * h);
    vec3 g;
    g.x  = a0.x  * x0.x  + h.x  * x0.y;
    g.yz = a0.yz * x12.xz + h.yz * x12.yw;
    return 130.0 * dot(m, g);
}

half4 main(float2 fragCoord) {
    vec2 uv = fragCoord / resolution;
    float t = time * 0.018;

    vec2 domeUv = uv - vec2(0.5, 0.28);

    float d = length(domeUv * vec2(1.0, 1.30)) * 1.35;
    float edge = exp(-1.70 * d * d);

    vec2 q = vec2(
        snoise(uv * 0.9 - vec2(0.0, t * 0.8)),
        snoise(uv * 0.9 + vec2(5.2, 1.3 - t * 0.8))
    );
    float wave = snoise(uv * 1.1 + 1.6 * q + vec2(1.7, 9.2 - t * 0.5));
    
    float lightMod = 0.82 + 0.36 * (wave * 0.5 + 0.5);

    float dither = (fract(dot(fragCoord, vec2(0.7548776662, 0.5698402909))) - 0.5) / 255.0;
    
    float finalAlpha = clamp(edge * baseColor.a * lightMod + dither, 0.0, 1.0);
    half3 finalRgb = baseColor.rgb * finalAlpha;

    return half4(finalRgb, finalAlpha);
}
"""

@Composable
fun AvatarBackdropGlow(
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "avatarBackdropTransition")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shaderTime"
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AgslBackdropGlow(
            accentColor = accentColor,
            time = time,
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
    time: Float,
    modifier: Modifier = Modifier,
) {
    val shader = remember { RuntimeShader(AVATAR_WARP_SHADER) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
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
            .height(240.dp)
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