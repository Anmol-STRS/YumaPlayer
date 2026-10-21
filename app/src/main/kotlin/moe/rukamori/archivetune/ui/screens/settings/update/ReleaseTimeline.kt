/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import moe.rukamori.archivetune.ui.settings.SettingsDimensions

@Composable
internal fun ReleaseTimeline(
    state: ReleaseTimelineState,
    actions: ReleaseTimelineActions,
    listState: LazyListState,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        contentPadding =
            PaddingValues(
                top = SettingsDimensions.SectionSpacing,
                bottom = bottomPadding,
            ),
    ) {
        itemsIndexed(
            items = state.entries,
            key = { _, entry -> "release:${entry.releaseKey}" },
            contentType = { _, _ -> "release_timeline_entry" },
        ) { index, entry ->
            val isLast = index == state.entries.lastIndex
            Box(modifier = Modifier.fillMaxWidth()) {
                ReleaseTimelineCard(
                    entry = entry,
                    actions = actions,
                    modifier =
                        Modifier
                            .padding(
                                start = TimelineRailWidth,
                                bottom = if (isLast) 0.dp else SettingsDimensions.SectionSpacing,
                            ).fillMaxWidth(),
                )
                ReleaseTimelineRail(
                    isFirst = index == 0,
                    isLast = isLast,
                    isLatest = entry.isLatest,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
    }
}

@Composable
private fun ReleaseTimelineRail(
    isFirst: Boolean,
    isLast: Boolean,
    isLatest: Boolean,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val nodeColor = if (isLatest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

    Canvas(modifier = modifier) {
        val centerX = TimelineRailWidth.toPx() / 2f
        val nodeY = TimelineNodeTop.toPx()
        val nodeRadius = TimelineNodeRadius.toPx()
        val strokeWidth = TimelineLineWidth.toPx()

        if (!isFirst) {
            drawLine(
                color = lineColor,
                start = Offset(centerX, 0f),
                end = Offset(centerX, nodeY - nodeRadius),
                strokeWidth = strokeWidth,
            )
        }
        if (!isLast) {
            drawLine(
                color = lineColor,
                start = Offset(centerX, nodeY + nodeRadius),
                end = Offset(centerX, size.height),
                strokeWidth = strokeWidth,
            )
        }
        drawCircle(
            color = nodeColor,
            radius = nodeRadius,
            center = Offset(centerX, nodeY),
        )
    }
}

private val TimelineRailWidth = 18.dp
private val TimelineNodeTop = 30.dp
private val TimelineNodeRadius = 3.dp
private val TimelineLineWidth = 1.dp
