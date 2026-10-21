/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.settings

import android.os.Bundle
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.constants.UpdateChannel
import moe.rukamori.archivetune.utils.ReleaseInfo
import moe.rukamori.archivetune.utils.ReleaseNotes
import moe.rukamori.archivetune.utils.ReleaseNotesParser
import moe.rukamori.archivetune.utils.ReleaseNotesSummary
import moe.rukamori.archivetune.utils.Updater

internal data class ChangelogRelease(
    val info: ReleaseInfo,
    val notes: ReleaseNotes,
)

internal val ReleaseExpansionSaver =
    Saver<Map<String, Boolean>, Bundle>(
        save = { expansion ->
            Bundle().apply {
                expansion.forEach { (key, value) -> putBoolean(key, value) }
            }
        },
        restore = { bundle ->
            bundle.keySet().associateWith { key -> bundle.getBoolean(key) }
        },
    )

internal val StringSetSaver =
    listSaver<Set<String>, String>(
        save = { it.toList() },
        restore = { it.toSet() },
    )

internal suspend fun toChangelogReleases(releases: List<ReleaseInfo>): List<ChangelogRelease> =
    withContext(Dispatchers.Default) {
        releases.map { release ->
            ChangelogRelease(
                info = release,
                notes = ReleaseNotesParser.parse(release.body),
            )
        }
    }

internal fun buildReleaseTimelineState(
    releases: List<ChangelogRelease>,
    releaseExpansion: Map<String, Boolean>,
    collapsedSections: Set<String>,
    showAllSections: Set<String>,
    channel: UpdateChannel,
    installedVersion: String,
): ReleaseTimelineState =
    ReleaseTimelineState(
        entries =
            releases.mapIndexed { index, changelogRelease ->
                val info = changelogRelease.info
                val releaseKey = info.timelineKey()
                val version =
                    if (channel == UpdateChannel.DAILY_NIGHTLY) {
                        Updater.getCanaryReleaseVersionName(info)
                    } else {
                        Updater.getReleaseVersionName(info)
                    }
                val rawName = info.name.trim()
                val name =
                    rawName.takeIf {
                        it.isNotBlank() &&
                            !it.contains(version, ignoreCase = true) &&
                            !it.equals(info.tagName, ignoreCase = true)
                    }
                val installed =
                    info.tagName.trim().isNotEmpty() &&
                        Updater.isSameVersion(info.tagName.trim(), installedVersion)
                val parsedSections = (changelogRelease.notes as? ReleaseNotes.Parsed)?.sections.orEmpty()
                ReleaseTimelineEntry(
                    releaseKey = releaseKey,
                    version = version,
                    name = name,
                    publishedAt = info.publishedAt,
                    isLatest = index == 0,
                    isInstalled = installed,
                    isPrerelease = info.prerelease,
                    isExpanded = releaseExpansion[releaseKey] ?: false,
                    imageUrl = info.imageUrl?.takeIf { it.isNotBlank() },
                    htmlUrl = info.htmlUrl.takeIf { it.isNotBlank() },
                    sections =
                        parsedSections.map { section ->
                            val sectionKey = sectionStateKey(releaseKey, section.id)
                            ReleaseTimelineSectionState(
                                id = section.id,
                                title = section.title,
                                category = section.category,
                                items = section.items,
                                isExpanded = sectionKey !in collapsedSections,
                                isShowingAll = sectionKey in showAllSections,
                            )
                        },
                    summary = (changelogRelease.notes as? ReleaseNotes.Parsed)?.summary ?: ReleaseNotesSummary(),
                    fallbackMarkdown = (changelogRelease.notes as? ReleaseNotes.Fallback)?.source,
                )
            },
    )

internal fun sectionStateKey(
    releaseKey: String,
    sectionId: String,
): String = "$releaseKey|$sectionId"
