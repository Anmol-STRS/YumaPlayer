/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

@file:OptIn(ExperimentalLayoutApi::class)

package moe.rukamori.archivetune.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.ui.settings.SettingsAnimations
import moe.rukamori.archivetune.ui.settings.SettingsDimensions
import moe.rukamori.archivetune.ui.theme.LocalYumaColors
import moe.rukamori.archivetune.ui.theme.yumaClickable
import moe.rukamori.archivetune.ui.theme.yumaGlassCard
import moe.rukamori.archivetune.utils.ReleaseNoteCategory
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
internal fun ReleaseTimelineCard(
    entry: ReleaseTimelineEntry,
    actions: ReleaseTimelineActions,
    modifier: Modifier = Modifier,
) {
    val colors = LocalYumaColors.current
    val cardShape = RoundedCornerShape(SettingsDimensions.SegmentedCornerLarge)
    val imageShape = RoundedCornerShape(SettingsDimensions.LibraryCardRadius)
    val stateLabel =
        stringResource(
            if (entry.isExpanded) R.string.release_state_expanded else R.string.release_state_collapsed,
        )
    val rotation by animateFloatAsState(
        targetValue = if (entry.isExpanded) 180f else 0f,
        animationSpec = SettingsAnimations.resizeSpring(),
        label = "release_timeline_chevron",
    )
    val dateLabel = releaseDateLabel(entry.publishedAt)
    val features = entry.summary.features
    val fixes = entry.summary.fixes
    val performance = entry.summary.performance

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .yumaGlassCard(
                    shape = cardShape,
                    backgroundColor = colors.glassBackground,
                    borderColor = colors.glassBorder,
                    strokeWidth = SettingsDimensions.GlassBorderThickness,
                ).padding(SettingsDimensions.BannerContentPadding),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SettingsDimensions.SectionSpacing)) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .yumaClickable(onClick = { actions.onToggleRelease(entry.releaseKey) })
                        .semantics { stateDescription = stateLabel },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SettingsDimensions.BannerColumnSpacing),
                ) {
                    Text(
                        text = entry.version,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    entry.name?.let { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (dateLabel.isNotBlank()) {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(
                    painter = painterResource(R.drawable.expand_more),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .size(SettingsDimensions.ChevronSize)
                            .rotate(rotation),
                )
            }

            ReleaseBadges(entry = entry)

            if (features > 0 || fixes > 0 || performance > 0) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(SettingsDimensions.SegmentedItemGap),
                    verticalArrangement = Arrangement.spacedBy(SettingsDimensions.SegmentedItemGap),
                ) {
                    if (features > 0) {
                        ReleaseSummaryChip(
                            text = stringResource(R.string.release_notes_summary_features, features),
                            category = ReleaseNoteCategory.FEATURES,
                        )
                    }
                    if (fixes > 0) {
                        ReleaseSummaryChip(
                            text = stringResource(R.string.release_notes_summary_fixes, fixes),
                            category = ReleaseNoteCategory.FIXES,
                        )
                    }
                    if (performance > 0) {
                        ReleaseSummaryChip(
                            text = stringResource(R.string.release_notes_summary_performance, performance),
                            category = ReleaseNoteCategory.PERFORMANCE,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = entry.isExpanded,
                enter = expandVertically(animationSpec = SettingsAnimations.resizeSpring()),
                exit = shrinkVertically(animationSpec = SettingsAnimations.resizeSpring()),
                label = "release_timeline_content",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(SettingsDimensions.SectionSpacing)) {
                    ReleaseImage(
                        imageUrl = entry.imageUrl,
                        shape = imageShape,
                    )
                    ReleaseNotesContent(entry = entry, actions = actions)
                    val htmlUrl = entry.htmlUrl
                    if (!htmlUrl.isNullOrBlank()) {
                        ReleaseGithubAction(
                            url = htmlUrl,
                            onOpenRelease = actions.onOpenRelease,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseBadges(entry: ReleaseTimelineEntry) {
    if (!entry.isLatest && !entry.isInstalled && !entry.isPrerelease) return

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(SettingsDimensions.SegmentedItemGap),
        verticalArrangement = Arrangement.spacedBy(SettingsDimensions.SegmentedItemGap),
    ) {
        if (entry.isLatest) {
            StatusBadgeChip(
                text = stringResource(R.string.release_badge_latest),
                iconRes = R.drawable.new_release,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        if (entry.isInstalled) {
            StatusBadgeChip(
                text = stringResource(R.string.release_badge_installed),
                iconRes = R.drawable.check,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
        if (entry.isPrerelease) {
            StatusBadgeChip(
                text = stringResource(R.string.release_badge_prerelease),
                iconRes = R.drawable.update,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun ReleaseSummaryChip(
    text: String,
    category: ReleaseNoteCategory,
) {
    val categoryColors = releaseCategoryColors(category)
    Surface(
        shape = RoundedCornerShape(SettingsDimensions.LibrarySmallRadius),
        color = categoryColors.container,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = categoryColors.content,
            modifier =
                Modifier.padding(
                    horizontal = SettingsDimensions.BadgePaddingH,
                    vertical = SettingsDimensions.BadgePaddingV,
                ),
        )
    }
}

@Composable
private fun releaseDateLabel(publishedAt: String): String {
    val inputFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ROOT) }
    val outputFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    return remember(publishedAt) {
        if (publishedAt.isBlank()) {
            ""
        } else {
            runCatching {
                inputFormat.parse(publishedAt.take(10))?.let(outputFormat::format)
            }.getOrNull() ?: publishedAt
        }
    }
}
