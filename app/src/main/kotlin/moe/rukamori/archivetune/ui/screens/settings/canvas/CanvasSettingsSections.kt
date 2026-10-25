/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.screens.settings.canvas

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.canvas.CanvasSource
import moe.rukamori.archivetune.ui.component.ListPreference
import moe.rukamori.archivetune.ui.component.PreferenceEntry
import moe.rukamori.archivetune.ui.component.PreferenceGroup
import moe.rukamori.archivetune.ui.component.PreferenceGroupScope
import moe.rukamori.archivetune.ui.component.SwitchPreference
import moe.rukamori.archivetune.ui.settings.SettingsDimensions
import moe.rukamori.archivetune.ui.utils.formatFileSize

internal val SupportedCanvasSources = listOf(
    CanvasSource.ALL,
    CanvasSource.BETTER_LYRICS,
    CanvasSource.APPLE_MUSIC,
    CanvasSource.TIDAL,
)

internal val CanvasCacheLimits = listOf(0, 64, 128, 256, 512, 1024, 2048, 4096, 8192, -1)

@Composable
internal fun CanvasGeneralSection(
    enabled: Boolean,
    source: CanvasSource,
    isBusy: Boolean,
    onIntent: (CanvasSettingsUiIntent) -> Unit,
) {
    PreferenceGroup(title = stringResource(R.string.general)) {
        item {
            SwitchPreference(
                title = { Text(stringResource(R.string.canvas_enable)) },
                description = stringResource(R.string.archivetune_canvas_desc),
                icon = { Icon(painterResource(R.drawable.motion_photos_on), null, modifier = Modifier.size(SettingsDimensions.RowIconSize)) },
                checked = enabled,
                onCheckedChange = { onIntent(CanvasSettingsUiIntent.SetEnabled(it)) },
                isEnabled = !isBusy,
            )
        }

        item(visible = enabled) {
            ListPreference(
                title = { Text(stringResource(R.string.canvas_source)) },
                description = if (source == CanvasSource.ALL) {
                    stringResource(R.string.canvas_source_all_desc)
                } else null,
                icon = { Icon(painterResource(R.drawable.music_note), null, modifier = Modifier.size(SettingsDimensions.RowIconSize)) },
                selectedValue = source,
                values = SupportedCanvasSources,
                valueText = { s -> stringResource(s.labelResource()) },
                onValueSelected = { onIntent(CanvasSettingsUiIntent.SelectSource(it)) },
                isEnabled = !isBusy,
            )
        }
    }
}

@Composable
internal fun CanvasHealthSection(
    health: CanvasHealthStatus,
    selectedSource: CanvasSource,
    isBusy: Boolean,
    onRefresh: () -> Unit,
) {
    val betterLyricsTitle = stringResource(R.string.canvas_better_lyrics)
    val appleMusicTitle = stringResource(R.string.canvas_apple_music)
    val tidalTitle = stringResource(R.string.canvas_tidal)

    PreferenceGroup(title = stringResource(R.string.canvas_provider_health)) {
        item {
            PreferenceEntry(
                title = { Text(stringResource(R.string.refresh)) },
                icon = { Icon(painterResource(R.drawable.sync), null, modifier = Modifier.size(SettingsDimensions.RowIconSize)) },
                trailingContent = {
                    if (health.checking) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                },
                onClick = onRefresh,
                isEnabled = !health.checking && !isBusy,
            )
        }

        if (selectedSource.accepts(CanvasSource.BETTER_LYRICS)) {
            canvasHealthItem(
                title = betterLyricsTitle,
                health = health.betterLyrics,
                iconRes = R.drawable.motion_photos_on,
            )
        }

        if (selectedSource.accepts(CanvasSource.APPLE_MUSIC)) {
            canvasHealthItem(
                title = appleMusicTitle,
                health = health.appleMusic,
                iconRes = R.drawable.music_note,
            )
        }

        if (selectedSource.accepts(CanvasSource.TIDAL)) {
            canvasHealthItem(
                title = tidalTitle,
                health = health.tidal,
                iconRes = R.drawable.music_note,
            )
        }
    }
}

private fun PreferenceGroupScope.canvasHealthItem(
    title: String,
    health: CanvasHealth,
    iconRes: Int,
) {
    item {
        PreferenceEntry(
            title = { Text(title) },
            description = stringResource(health.labelResource()),
            icon = { Icon(painterResource(iconRes), null, modifier = Modifier.size(SettingsDimensions.RowIconSize)) },
            trailingContent = {
                if (health == CanvasHealth.CHECKING) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                }
            },
        )
    }
}

@Composable
internal fun CanvasCacheSection(
    cacheLimitMb: Int,
    cacheBytes: Long,
    isBusy: Boolean,
    onLimitSelected: (Int) -> Unit,
    onClearClick: () -> Unit,
) {
    PreferenceGroup(title = stringResource(R.string.canvas_cache)) {
        item {
            ListPreference(
                title = { Text(stringResource(R.string.max_cache_size)) },
                icon = { Icon(painterResource(R.drawable.storage), null, modifier = Modifier.size(SettingsDimensions.RowIconSize)) },
                selectedValue = cacheLimitMb,
                values = CanvasCacheLimits,
                valueText = { limit ->
                    when (limit) {
                        0 -> stringResource(R.string.disable)
                        -1 -> stringResource(R.string.unlimited)
                        else -> formatFileSize(limit.toLong() * 1024L * 1024L)
                    }
                },
                onValueSelected = onLimitSelected,
                isEnabled = !isBusy,
            )
        }

        item {
            val limitBytes = if (cacheLimitMb > 0) cacheLimitMb.toLong() * 1024L * 1024L else 0L
            val progress = if (limitBytes > 0) {
                (cacheBytes.toFloat() / limitBytes.toFloat()).coerceIn(0f, 1f)
            } else 0f

            PreferenceEntry(
                title = { Text(stringResource(R.string.size_used, formatFileSize(cacheBytes))) },
                description = stringResource(R.string.canvas_cache_desc),
                icon = { Icon(painterResource(R.drawable.storage), null, modifier = Modifier.size(SettingsDimensions.RowIconSize)) },
                content = {
                    if (isBusy || cacheLimitMb > 0) {
                        Spacer(modifier = Modifier.padding(top = 8.dp))
                        if (isBusy) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        } else {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                },
            )
        }

        item {
            PreferenceEntry(
                title = { Text(stringResource(R.string.clear_canvas_cache)) },
                icon = { Icon(painterResource(R.drawable.delete), null, modifier = Modifier.size(SettingsDimensions.RowIconSize)) },
                onClick = onClearClick,
                isEnabled = !isBusy,
            )
        }
    }
}

internal fun CanvasSource.labelResource(): Int = when (this) {
    CanvasSource.BETTER_LYRICS -> R.string.canvas_better_lyrics
    CanvasSource.APPLE_MUSIC -> R.string.canvas_apple_music
    CanvasSource.TIDAL -> R.string.canvas_tidal
    CanvasSource.ALL -> R.string.canvas_source_all
    CanvasSource.SPOTIFY -> R.string.canvas_source_all
}

internal fun CanvasHealth.labelResource(): Int = when (this) {
    CanvasHealth.NOT_CHECKED -> R.string.canvas_health_not_checked
    CanvasHealth.CHECKING -> R.string.canvas_health_checking
    CanvasHealth.AVAILABLE -> R.string.canvas_health_available
    CanvasHealth.UNAVAILABLE -> R.string.canvas_health_unavailable
    CanvasHealth.NOT_SELECTED -> R.string.canvas_health_not_selected
    CanvasHealth.DISABLED -> R.string.canvas_health_disabled
    CanvasHealth.OFFLINE -> R.string.canvas_health_offline
}
