/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.playback.audio

internal object AudioPolicyConstants {
    const val AUDIO_EFFECT_INITIALIZATION_MAX_ATTEMPTS = 4
    const val AUDIO_EFFECT_INITIALIZATION_RETRY_DELAY_MS = 250L

    const val AUDIO_ROUTE_CHANGE_DEBOUNCE_MS = 350L
    const val AUDIO_EFFECT_ROUTE_REBIND_DELAY_MS = 200L
    const val AUDIO_ROUTE_RECOVERY_MIN_INTERVAL_MS = 1_500L
    const val AUDIO_ROUTE_RECOVERY_RESUME_DELAY_MS = 150L
    const val DEVICE_MUTE_PLAYBACK_NOTICE_INTERVAL_MS = 1_200L

    const val MIN_AUDIO_FOCUS_VOLUME_FACTOR = 0.2f
    const val MIN_AUDIO_NORMALIZATION_FACTOR = 0.25f
    const val MAX_AUDIO_NORMALIZATION_FACTOR = 1.414f

    const val EFFECTIVE_VOLUME_RAMP_FRAME_MS = 16L
    const val EFFECTIVE_VOLUME_RAMP_UP_MS = 350L
    const val EFFECTIVE_VOLUME_RAMP_DOWN_MS = 180L
    const val EFFECTIVE_VOLUME_RAMP_MIN_DELTA = 0.015f

    const val MIN_AUDIBLE_EFFECTIVE_VOLUME = 0.01f
    const val STUCK_MUTED_VOLUME_EPSILON = 0.001f
    const val AUDIBLE_PLAYBACK_VOLUME_CHECK_MS = 2_000L
}
