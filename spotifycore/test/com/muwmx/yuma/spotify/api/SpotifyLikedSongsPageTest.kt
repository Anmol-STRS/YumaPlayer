/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.spotify.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import moe.rukamori.archivetune.spotify.Spotify.SpotifyException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyLikedSongsPageTest {
    @Test
    fun rejectsMissingItems() {
        assertInvalid("""{"totalCount":0}""")
    }

    @Test
    fun rejectsNullItems() {
        assertInvalid("""{"items":null,"totalCount":0}""")
    }

    @Test
    fun rejectsNonArrayItems() {
        listOf("{}", "0", "false", "\"empty\"").forEach { items ->
            assertInvalid("""{"items":$items,"totalCount":0}""")
        }
    }

    @Test
    fun rejectsMissingTotal() {
        assertInvalid("""{"items":[]}""")
    }

    @Test
    fun rejectsNullTotal() {
        assertInvalid("""{"items":[],"totalCount":null}""")
    }

    @Test
    fun rejectsNonIntegerTotal() {
        listOf("{}", "[]", "false", "0.5", "\"0\"", "2147483648").forEach { total ->
            assertInvalid("""{"items":[],"totalCount":$total}""")
        }
    }

    @Test
    fun rejectsNegativeTotal() {
        assertInvalid("""{"items":[],"totalCount":-1}""")
    }

    @Test
    fun rejectsMissingTracksEnvelope() {
        assertThrows(SpotifyException::class.java) {
            SpotifyLibraryQueryApi.parseLikedSongsPage(Json.parseToJsonElement("{}").jsonObject, 50, 0)
        }
    }

    @Test
    fun acceptsExplicitEmptyLibrary() {
        val page = parse("""{"items":[],"totalCount":0}""")
        assertTrue(page.items.isEmpty())
        assertEquals(0, page.total)
        assertEquals(50, page.limit)
        assertEquals(0, page.offset)
    }

    @Test
    fun parsesValidTrackAndPagingInfo() {
        val page = parse(
            """{"items":[{"track":{"_uri":"spotify:track:present","data":{"name":"Song"}}}],
                "totalCount":51,"pagingInfo":{"limit":50,"offset":50}}""",
        )
        assertEquals(listOf("present"), page.items.map { it.track.id })
        assertEquals("Song", page.items.single().track.name)
        assertEquals(51, page.total)
        assertEquals(50, page.offset)
    }

    @Test
    fun filteredTracksDoNotReduceReportedTotal() {
        val page = parse("""{"items":[{"track":{"data":{"name":"Missing ID"}}}],"totalCount":1}""")
        assertTrue(page.items.isEmpty())
        assertEquals(1, page.total)
    }

    @Test
    fun rejectsFilteredItemsInReportedEmptyLibrary() {
        assertInvalid("""{"items":[{"track":{"data":{"name":"Missing ID"}}}],"totalCount":0}""")
    }

    @Test
    fun rejectsRawItemCountExceedingTotalBeforeFiltering() {
        assertInvalid(
            """{"items":[{"track":{"_uri":"spotify:track:present","data":{"name":"Song"}}},
                {"track":{"data":{"name":"Missing ID"}}}],"totalCount":1}""",
        )
    }

    @Test
    fun rejectsPageExtendingBeyondReportedTotal() {
        assertInvalid("""{"items":[{}],"totalCount":50,"pagingInfo":{"offset":50}}""")
    }

    @Test
    fun usesRequestedPagingWhenMetadataIsMissing() {
        val page = SpotifyLibraryQueryApi.parseLikedSongsPage(response("""{"items":[],"totalCount":51}"""), 25, 50)
        assertEquals(25, page.limit)
        assertEquals(50, page.offset)
    }

    private fun assertInvalid(tracks: String) {
        assertThrows(SpotifyException::class.java) { parse(tracks) }
    }

    private fun parse(tracks: String) = SpotifyLibraryQueryApi.parseLikedSongsPage(response(tracks), 50, 0)

    private fun response(tracks: String) =
        Json.parseToJsonElement("""{"data":{"me":{"library":{"tracks":$tracks}}}}""").jsonObject
}
