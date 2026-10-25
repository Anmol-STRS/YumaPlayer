/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.screens.settings.canvas

import androidx.compose.runtime.Immutable
import moe.rukamori.archivetune.canvas.CanvasSource

enum class CanvasHealth {
    NOT_CHECKED,
    CHECKING,
    AVAILABLE,
    UNAVAILABLE,
    NOT_SELECTED,
    DISABLED,
    OFFLINE,
}

@Immutable
data class CanvasHealthStatus(
    val betterLyrics: CanvasHealth = CanvasHealth.NOT_CHECKED,
    val appleMusic: CanvasHealth = CanvasHealth.NOT_CHECKED,
    val tidal: CanvasHealth = CanvasHealth.NOT_CHECKED,
) {
    val checking: Boolean
        get() = betterLyrics == CanvasHealth.CHECKING ||
            appleMusic == CanvasHealth.CHECKING ||
            tidal == CanvasHealth.CHECKING
}

@Immutable
data class CanvasSettingsUiState(
    val enabled: Boolean = false,
    val source: CanvasSource = CanvasSource.ALL,
    val cacheLimitMb: Int = 256,
    val cacheBytes: Long = 0L,
    val health: CanvasHealthStatus = CanvasHealthStatus(),
    val isBusy: Boolean = false,
    val errorMessageRes: Int? = null,
)

sealed interface CanvasSettingsUiIntent {
    data class SetEnabled(val enabled: Boolean) : CanvasSettingsUiIntent
    data class SelectSource(val source: CanvasSource) : CanvasSettingsUiIntent
    data class SetCacheLimit(val limitMb: Int) : CanvasSettingsUiIntent
    data object ClearCache : CanvasSettingsUiIntent
    data object RefreshHealth : CanvasSettingsUiIntent
}
