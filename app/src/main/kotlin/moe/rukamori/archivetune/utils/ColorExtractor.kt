/*
 * YumaPlayer (2026) | Original work by MuwMix
 *  * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.utils

import android.annotation.SuppressLint
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.QuantizerCelebi
import com.google.android.material.color.utilities.Scheme
import com.google.android.material.color.utilities.Score

/**
 * Pure JVM color extraction utility.
 * Free of Android Context and Jetpack Compose framework dependencies.
 */
@SuppressLint("RestrictedApi")
object ColorExtractor {
    /**
     * Extracts vibrant accent color as ARGB hex string (#AARRGGBB) from raw ARGB pixel array.
     *
     * @param argbPixels Raw ARGB pixel array from downscaled avatar raster.
     * @return Hex string representation of extracted vibrant accent color, or null if input is empty or extraction fails.
     */
    fun extractVibrantHex(argbPixels: IntArray): String? {
        if (argbPixels.isEmpty()) return null
        return try {
            val quantizerResult = QuantizerCelebi.quantize(argbPixels, 128)
            val rankedColors = Score.score(quantizerResult)
            val topColorInt = rankedColors.firstOrNull() ?: return null
            val hct = Hct.fromInt(topColorInt)
            val darkScheme = Scheme.dark(hct.toInt())
            String.format("#%08X", darkScheme.primary)
        } catch (_: Exception) {
            null
        }
    }
}