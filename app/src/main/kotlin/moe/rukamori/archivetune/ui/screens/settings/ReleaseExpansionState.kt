/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.settings

import moe.rukamori.archivetune.utils.ReleaseInfo

internal fun ReleaseInfo.timelineKey(): String {
    val tag = tagName.trim()
    if (tag.isNotEmpty()) return tag
    val base =
        name
            .trim()
            .ifBlank { htmlUrl.trim() }
            .ifBlank { "release" }
    return "$base|$publishedAt"
}

internal fun initializeReleaseExpansion(
    releases: List<ChangelogRelease>,
    current: Map<String, Boolean>,
): Map<String, Boolean> {
    if (releases.isEmpty()) return current
    val keys = releases.map { it.info.timelineKey() }
    val defaults = keys.withIndex().associate { (index, key) -> key to (index == 0) }
    return defaults + current
}
