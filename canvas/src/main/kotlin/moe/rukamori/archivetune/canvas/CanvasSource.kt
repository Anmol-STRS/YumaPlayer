/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.canvas

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
enum class CanvasSource {
    BETTER_LYRICS,
    APPLE_MUSIC,
    TIDAL,
    SPOTIFY,
    ALL;

    fun accepts(provider: CanvasSource?): Boolean =
        if (this == ALL) {
            provider != ALL
        } else {
            provider != null && provider == this
        }

    companion object {
        fun fromPreference(value: String?): CanvasSource {
            val normalized = value?.trim()?.uppercase(Locale.ROOT)
            if (normalized == "BOTH") return ALL
            return entries.firstOrNull { it.name == normalized } ?: ALL
        }

        fun fromString(value: String?): CanvasSource = fromPreference(value)
    }
}
