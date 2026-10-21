/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.screens.settings

import moe.rukamori.archivetune.utils.ReleaseInfo
import moe.rukamori.archivetune.utils.ReleaseNotes
import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseExpansionStateTest {

    @Test
    fun `new latest expands while previously seen releases keep their state`() {
        val releases = listOf(release("v1.1.0"), release("v1.0.0"), release("v0.9.0"))
        val current = mapOf("v1.0.0" to true)

        val result = initializeReleaseExpansion(releases, current)

        assertEquals(true, result["v1.1.0"])
        assertEquals(true, result["v1.0.0"])
        assertEquals(false, result["v0.9.0"])
    }

    @Test
    fun `explicit collapse of the latest release is preserved`() {
        val releases = listOf(release("v1.1.0"), release("v1.0.0"))
        val current = mapOf("v1.1.0" to false)

        val result = initializeReleaseExpansion(releases, current)

        assertEquals(false, result["v1.1.0"])
        assertEquals(false, result["v1.0.0"])
    }

    @Test
    fun `seen release removed from the list keeps its stored state`() {
        val current = mapOf("v0.8.0" to true)

        val result = initializeReleaseExpansion(listOf(release("v1.0.0")), current)

        assertEquals(true, result["v0.8.0"])
        assertEquals(true, result["v1.0.0"])
    }

    @Test
    fun `empty release list returns the existing map`() {
        val current = mapOf("v1.0.0" to true)

        assertEquals(current, initializeReleaseExpansion(emptyList(), current))
    }

    private fun release(tag: String): ChangelogRelease =
        ChangelogRelease(
            info =
                ReleaseInfo(
                    tagName = tag,
                    name = tag,
                    body = null,
                    publishedAt = "2026-01-01T00:00:00Z",
                    htmlUrl = "https://example.com/$tag",
                ),
            notes = ReleaseNotes.Fallback(source = ""),
        )
}
