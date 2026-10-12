/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package moe.rukamori.archivetune.ui.screens.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.LocalAnimationsDisabled
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.constants.AudioQuality
import moe.rukamori.archivetune.constants.AutomixTransitionDuration
import moe.rukamori.archivetune.constants.FlacQuality
import moe.rukamori.archivetune.constants.PlaybackSource
import moe.rukamori.archivetune.sponsorblock.DEFAULT_SPONSOR_BLOCK_API_URL
import moe.rukamori.archivetune.ui.component.CrossfadeSliderPreference
import moe.rukamori.archivetune.ui.component.EditTextPreference
import moe.rukamori.archivetune.ui.component.EnumListPreference
import moe.rukamori.archivetune.ui.component.PreferenceEntry
import moe.rukamori.archivetune.ui.component.PreferenceGroup
import moe.rukamori.archivetune.ui.component.PreferenceGroupScope
import moe.rukamori.archivetune.ui.component.PreferenceSelectionOption
import moe.rukamori.archivetune.ui.component.SwitchPreference
import moe.rukamori.archivetune.ui.settings.SettingsDimensions
import moe.rukamori.archivetune.ui.theme.yumaClickable
import moe.rukamori.archivetune.ui.theme.yumaSegmentPosition
import moe.rukamori.archivetune.viewmodels.SponsorBlockCategoryUiModel
import moe.rukamori.archivetune.viewmodels.SponsorBlockSettingsScreenState

@Composable
fun PlaybackSourceSelector(
    playbackSource: PlaybackSource,
    onPlaybackSourceChange: (PlaybackSource) -> Unit,
    onEnableLosslessChange: (Boolean) -> Unit,
) {
    EnumListPreference(
        title = { Text(stringResource(R.string.playback_source)) },
        icon = { Icon(painterResource(R.drawable.album), null) },
        selectedValue = playbackSource,
        onValueSelected = {
            onPlaybackSourceChange(it)
            onEnableLosslessChange(it == PlaybackSource.FLAC)
        },
        valueText = { source ->
            when (source) {
                PlaybackSource.YT_MUSIC -> stringResource(R.string.source_yt_music)
                PlaybackSource.FLAC -> stringResource(R.string.source_flac)
            }
        },
    )
}

fun PreferenceGroupScope.FlacTokenInputs(
    qobuzAppId: String,
    onQobuzAppIdChange: (String) -> Unit,
    qobuzAppSecret: String,
    onQobuzAppSecretChange: (String) -> Unit,
    qobuzUserAuthToken: String,
    onQobuzUserAuthTokenChange: (String) -> Unit,
) {
    item {
        EditTextPreference(
            title = { Text(stringResource(R.string.qobuz_app_id)) },
            icon = { Icon(painterResource(R.drawable.lock), null) },
            value = qobuzAppId,
            onValueChange = onQobuzAppIdChange,
            isMasked = true,
        )
    }
    item {
        EditTextPreference(
            title = { Text(stringResource(R.string.qobuz_app_secret)) },
            icon = { Icon(painterResource(R.drawable.lock), null) },
            value = qobuzAppSecret,
            onValueChange = onQobuzAppSecretChange,
            isMasked = true,
        )
    }
    item {
        EditTextPreference(
            title = { Text(stringResource(R.string.qobuz_user_auth_token)) },
            icon = { Icon(painterResource(R.drawable.lock), null) },
            value = qobuzUserAuthToken,
            onValueChange = onQobuzUserAuthTokenChange,
            isMasked = true,
        )
    }
}

@Composable
internal fun PlayerLosslessSection(
    state: PlayerSettingsUiState,
    actions: PlayerSettingsUiActions,
    modifier: Modifier = Modifier,
) {
    PreferenceGroup(
        title = stringResource(R.string.lossless_integration),
        modifier = modifier,
    ) {
        item {
            PlaybackSourceSelector(
                playbackSource = state.playbackSource,
                onPlaybackSourceChange = actions.onPlaybackSourceChange,
                onEnableLosslessChange = actions.onEnableLosslessChange,
            )
        }
        if (state.playbackSource == PlaybackSource.FLAC) {
            item {
                EnumListPreference(
                    title = { Text(stringResource(R.string.flac_streaming_quality)) },
                    icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
                    selectedValue = state.flacStreamingQuality,
                    onValueSelected = actions.onFlacStreamingQualityChange,
                    valueText = { quality ->
                        when (quality) {
                            FlacQuality.CD -> stringResource(R.string.flac_quality_cd)
                            FlacQuality.HI_RES -> stringResource(R.string.flac_quality_hi_res)
                            FlacQuality.MAX -> stringResource(R.string.flac_quality_max)
                        }
                    },
                )
            }
            item {
                EnumListPreference(
                    title = { Text(stringResource(R.string.flac_download_quality)) },
                    icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
                    selectedValue = state.flacDownloadQuality,
                    onValueSelected = actions.onFlacDownloadQualityChange,
                    valueText = { quality ->
                        when (quality) {
                            FlacQuality.CD -> stringResource(R.string.flac_quality_cd)
                            FlacQuality.HI_RES -> stringResource(R.string.flac_quality_hi_res)
                            FlacQuality.MAX -> stringResource(R.string.flac_quality_max)
                        }
                    },
                )
            }
            item {
                SwitchPreference(
                    title = { Text(stringResource(R.string.memory_cache_toggle)) },
                    icon = { Icon(painterResource(R.drawable.cached), null) },
                    checked = state.memoryCacheToggle,
                    onCheckedChange = actions.onMemoryCacheToggleChange,
                )
            }
            item {
                PreferenceEntry(
                    title = { Text(stringResource(R.string.select_flac_download_folder)) },
                    description = state.flacFolderPath,
                    icon = { Icon(painterResource(R.drawable.snippet_folder), null) },
                    onClick = actions.onSelectFlacDownloadFolder,
                )
            }
            FlacTokenInputs(
                qobuzAppId = state.qobuzAppId,
                onQobuzAppIdChange = actions.onQobuzAppIdChange,
                qobuzAppSecret = state.qobuzAppSecret,
                onQobuzAppSecretChange = actions.onQobuzAppSecretChange,
                qobuzUserAuthToken = state.qobuzUserAuthToken,
                onQobuzUserAuthTokenChange = actions.onQobuzUserAuthTokenChange,
            )
        } else {
            item {
                EnumListPreference(
                    title = { Text(stringResource(R.string.audio_quality)) },
                    icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
                    selectedValue = state.audioQuality,
                    onValueSelected = actions.onAudioQualityChange,
                    valueText = { quality ->
                        when (quality) {
                            AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                            AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                            AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                            AudioQuality.HIGHEST -> stringResource(R.string.audio_quality_highest)
                        }
                    },
                )
            }
        }
    }
}

@Composable
internal fun PlayerAudioSection(
    state: PlayerSettingsUiState,
    actions: PlayerSettingsUiActions,
    modifier: Modifier = Modifier,
) {
    PreferenceGroup(
        title = stringResource(R.string.player_and_audio),
        modifier = modifier,
    ) {
        item {
            SwitchPreference(
                title = { Text(stringResource(R.string.skip_silence)) },
                icon = { Icon(painterResource(R.drawable.skip_next), null) },
                checked = state.skipSilence,
                onCheckedChange = actions.onSkipSilenceChange,
            )
        }
        item {
            SwitchPreference(
                title = { Text(stringResource(R.string.audio_normalization)) },
                icon = { Icon(painterResource(R.drawable.volume_up), null) },
                checked = state.audioNormalization,
                onCheckedChange = actions.onAudioNormalizationChange,
            )
        }
        item {
            PreferenceEntry(
                title = { Text(stringResource(R.string.equalizer)) },
                icon = { Icon(painterResource(R.drawable.equalizer), null) },
                onClick = actions.onEqualizerClick,
                showChevron = true,
            )
        }
    }
}

@Composable
internal fun PlayerCrossfadeSection(
    state: PlayerSettingsUiState,
    actions: PlayerSettingsUiActions,
    modifier: Modifier = Modifier,
) {
    PreferenceGroup(
        title = stringResource(R.string.audio_crossfade_title),
        modifier = modifier,
    ) {
        item {
            SwitchPreference(
                title = { Text(stringResource(R.string.audio_crossfade_title)) },
                icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
                checked = state.crossfadeEnabled,
                onCheckedChange = actions.onCrossfadeEnabledChange,
            )
        }
        item {
            SwitchPreference(
                title = { Text(stringResource(R.string.automix_title)) },
                description = stringResource(R.string.automix_description),
                icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
                checked = state.automixEnabled,
                onCheckedChange = actions.onAutomixEnabledChange,
                isEnabled = state.crossfadeEnabled,
            )
        }
        item {
            EnumListPreference(
                title = { Text(stringResource(R.string.automix_transition_length)) },
                icon = { Icon(painterResource(R.drawable.speed), null) },
                selectedValue = state.automixTransitionDuration,
                onValueSelected = actions.onAutomixTransitionDurationChange,
                valueText = { duration ->
                    when (duration) {
                        AutomixTransitionDuration.AUTO -> stringResource(R.string.automix_duration_auto)
                        AutomixTransitionDuration.S4 -> stringResource(R.string.automix_duration_4s)
                        AutomixTransitionDuration.S8 -> stringResource(R.string.automix_duration_8s)
                        AutomixTransitionDuration.S12 -> stringResource(R.string.automix_duration_12s)
                    }
                },
                isEnabled = state.crossfadeEnabled && state.automixEnabled,
            )
        }
        item {
            CrossfadeSliderPreference(
                valueSeconds = state.crossfadeDurationSeconds,
                onValueChange = actions.onCrossfadeDurationSecondsChange,
                isEnabled = state.crossfadeEnabled,
            )
        }
        item {
            SwitchPreference(
                title = { Text(stringResource(R.string.crossfade_gapless_title)) },
                description = stringResource(R.string.crossfade_gapless_description),
                icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
                checked = state.crossfadeGapless,
                onCheckedChange = actions.onCrossfadeGaplessChange,
                isEnabled = state.crossfadeEnabled,
            )
        }
    }
}

@Composable
internal fun PlayerBehaviorSection(
    state: PlayerSettingsUiState,
    actions: PlayerSettingsUiActions,
    modifier: Modifier = Modifier,
) {
    PreferenceGroup(
        title = stringResource(R.string.player),
        modifier = modifier,
    ) {
        item {
            SwitchPreference(
                title = { Text(stringResource(R.string.auto_start_on_bluetooth)) },
                icon = { Icon(painterResource(R.drawable.bluetooth), null) },
                checked = state.autoStartOnBluetooth,
                onCheckedChange = actions.onAutoStartOnBluetoothChange,
            )
        }
        item {
            SwitchPreference(
                title = { Text(stringResource(R.string.pause_on_device_mute)) },
                icon = { Icon(painterResource(R.drawable.volume_off), null) },
                checked = state.pauseOnDeviceMute,
                onCheckedChange = actions.onPauseOnDeviceMuteChange,
            )
        }
        item {
            SwitchPreference(
                title = { Text(stringResource(R.string.auto_skip_next_on_error)) },
                icon = { Icon(painterResource(R.drawable.skip_next), null) },
                checked = state.autoSkipNextOnError,
                onCheckedChange = actions.onAutoSkipNextOnErrorChange,
            )
        }
        item {
            SwitchPreference(
                title = { Text(stringResource(R.string.wakelock)) },
                description = stringResource(R.string.wakelock_desc),
                icon = { Icon(painterResource(R.drawable.lock), null) },
                checked = state.wakelockEnabled,
                onCheckedChange = actions.onWakelockChange,
            )
        }
        item {
            SwitchPreference(
                title = { Text(stringResource(R.string.low_data_mode_title)) },
                description = stringResource(R.string.low_data_mode_description),
                icon = { Icon(painterResource(R.drawable.android_cell), null) },
                checked = state.lowDataMode,
                onCheckedChange = actions.onLowDataModeChange,
            )
        }
        item {
            SwitchPreference(
                title = { Text(stringResource(R.string.stop_music_on_task_clear)) },
                icon = { Icon(painterResource(R.drawable.swipe), null) },
                checked = state.stopMusicOnTaskClear,
                onCheckedChange = actions.onStopMusicOnTaskClearChange,
            )
        }
    }
}

@Composable
internal fun PlayerSponsorBlockSection(
    state: SponsorBlockSettingsScreenState,
    onEnabledChange: (Boolean) -> Unit,
    onCategorySheetOpen: () -> Unit,
    onCategorySheetDismiss: () -> Unit,
    onCategoryCheckedChange: (SponsorBlockCategoryUiModel, Boolean) -> Unit,
    onCategorySelectionConfirm: () -> Unit,
    onApiUrlEditorOpen: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val data = (state as? SponsorBlockSettingsScreenState.Success)?.data
    val controlsEnabled = data != null
    val configurationEnabled = data != null && data.enabled
    val categoryOptions = data?.categoryOptions.orEmpty()
    val draftCategoryOptions = data?.draftCategoryOptions.orEmpty()
    val selectedCount = data?.selectedCategoryOptions?.size ?: 0

    if (data?.isCategorySheetVisible == true) {
        SponsorBlockCategoryBottomSheet(
            title = { Text(stringResource(R.string.sponsor_block_categories)) },
            values = categoryOptions,
            checkedValues = draftCategoryOptions,
            onDismiss = onCategorySheetDismiss,
            onValueCheckedChange = onCategoryCheckedChange,
            onConfirm = onCategorySelectionConfirm,
        )
    }

    val animationsDisabled = LocalAnimationsDisabled.current
    val contentAnimationSpec =
        remember(animationsDisabled) {
            if (animationsDisabled) {
                snap<IntSize>()
            } else {
                spring<IntSize>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)
            }
        }

    PreferenceGroup(
        title = stringResource(R.string.sponsor_block_group),
        modifier = modifier.animateContentSize(contentAnimationSpec),
    ) {
        item {
            SwitchPreference(
                title = { Text(stringResource(R.string.sponsor_block_use)) },
                icon = { Icon(painterResource(R.drawable.block), null) },
                checked = data?.enabled ?: false,
                onCheckedChange = onEnabledChange,
                isEnabled = controlsEnabled,
            )
        }

        item(visible = configurationEnabled) {
            PreferenceEntry(
                title = { Text(stringResource(R.string.sponsor_block_categories)) },
                description = stringResource(R.string.sponsor_block_categories_desc),
                icon = { Icon(painterResource(R.drawable.fast_forward), null) },
                content = {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.n_selected,
                                selectedCount,
                                selectedCount,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                        )
                    }
                },
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                },
                onClick = onCategorySheetOpen,
                isEnabled = configurationEnabled,
            )
        }

        item(visible = configurationEnabled) {
            PreferenceEntry(
                title = { Text(stringResource(R.string.sponsor_block_api_url)) },
                description = data?.apiUrl ?: DEFAULT_SPONSOR_BLOCK_API_URL,
                icon = { Icon(painterResource(R.drawable.link), null) },
                onClick = onApiUrlEditorOpen,
                isEnabled = configurationEnabled,
            )
        }

        if (state is SponsorBlockSettingsScreenState.Error) {
            item {
                PreferenceEntry(
                    title = { Text(stringResource(R.string.retry)) },
                    description = stringResource(state.messageRes),
                    onClick = onRetry,
                )
            }
        }
    }
}

@Composable
private fun SponsorBlockCategoryBottomSheet(
    title: @Composable () -> Unit,
    values: List<SponsorBlockCategoryUiModel>,
    checkedValues: List<SponsorBlockCategoryUiModel>,
    onDismiss: () -> Unit,
    onValueCheckedChange: (SponsorBlockCategoryUiModel, Boolean) -> Unit,
    onConfirm: () -> Unit,
) {
    @Suppress("DEPRECATION")
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val hideSheet: (() -> Unit) -> Unit =
        remember(coroutineScope, sheetState) {
            { completion ->
                coroutineScope
                    .launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        completion()
                    }
            }
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = null,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SettingsDimensions.BottomSheetHorizontalPadding)
                .padding(bottom = SettingsDimensions.BottomSheetBottomPadding)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(SettingsDimensions.BottomSheetCornerRadius),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(
                width = SettingsDimensions.GlassBorderThickness,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            ),
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SettingsDimensions.BottomSheetContentPaddingH)
                    .padding(
                        top = SettingsDimensions.BottomSheetContentPaddingTop,
                        bottom = SettingsDimensions.BottomSheetContentPaddingBottom,
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = SettingsDimensions.BottomSheetDragHandleBottomPadding)
                        .size(
                            width = SettingsDimensions.BottomSheetDragHandleWidth,
                            height = SettingsDimensions.BottomSheetDragHandleHeight,
                        )
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)),
                )

                ProvideTextStyle(
                    MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = SettingsDimensions.BottomSheetTitleBottomPadding),
                    ) {
                        title()
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = SettingsDimensions.BottomSheetListMaxHeight),
                    verticalArrangement = Arrangement.spacedBy(SettingsDimensions.SegmentedItemGap),
                ) {
                    itemsIndexed(
                        items = values,
                        key = { _, item -> item.category.name },
                    ) { index, option ->
                        val isChecked = option in checkedValues
                        val shape = remember(index, values.size) {
                            segmentedSettingsItemShape(index, values.size)
                        }
                        val position = remember(index, values.size) {
                            yumaSegmentPosition(index, values.size)
                        }
                        PreferenceSelectionOption(
                            text = stringResource(option.labelRes),
                            selected = isChecked,
                            shape = shape,
                            position = position,
                            onClick = { onValueCheckedChange(option, !isChecked) },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { hideSheet(onDismiss) },
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.yumaClickable(pressedScale = 0.96f, onClick = { hideSheet(onDismiss) }),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Text(
                            text = stringResource(android.R.string.cancel),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = { hideSheet(onConfirm) },
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.yumaClickable(pressedScale = 0.96f, onClick = { hideSheet(onConfirm) }),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text(
                            text = stringResource(android.R.string.ok),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun PlayerSettingsContent(
    state: PlayerSettingsUiState,
    actions: PlayerSettingsUiActions,
    modifier: Modifier = Modifier,
    sponsorBlockState: SponsorBlockSettingsScreenState = SponsorBlockSettingsScreenState.Loading,
    onSponsorBlockEnabledChange: (Boolean) -> Unit = {},
    onCategorySheetOpen: () -> Unit = {},
    onCategorySheetDismiss: () -> Unit = {},
    onCategoryCheckedChange: (SponsorBlockCategoryUiModel, Boolean) -> Unit = { _, _ -> },
    onCategorySelectionConfirm: () -> Unit = {},
    onApiUrlEditorOpen: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .playerAwareBottomPadding()
            .padding(bottom = SettingsDimensions.ScreenBottomPadding),
    ) {
        PlayerLosslessSection(state = state, actions = actions)
        PlayerAudioSection(state = state, actions = actions)
        PlayerCrossfadeSection(state = state, actions = actions)
        PlayerSponsorBlockSection(
            state = sponsorBlockState,
            onEnabledChange = onSponsorBlockEnabledChange,
            onCategorySheetOpen = onCategorySheetOpen,
            onCategorySheetDismiss = onCategorySheetDismiss,
            onCategoryCheckedChange = onCategoryCheckedChange,
            onCategorySelectionConfirm = onCategorySelectionConfirm,
            onApiUrlEditorOpen = onApiUrlEditorOpen,
            onRetry = onRetry,
        )
        PlayerBehaviorSection(state = state, actions = actions)
    }
}
