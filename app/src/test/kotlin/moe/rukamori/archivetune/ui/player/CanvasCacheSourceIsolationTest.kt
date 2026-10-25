/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.ui.player

import moe.rukamori.archivetune.canvas.CanvasSource
import moe.rukamori.archivetune.canvas.models.CanvasArtwork
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CanvasCacheSourceIsolationTest {

    private lateinit var tempDir: File
    private lateinit var index: CanvasCacheIndex

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("canvas_cache_test").toFile()
        index = CanvasCacheIndex()
        index.init(tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun indexGetRejectsIncompatibleSourceAndExplicitSourceRejectsUntagged() {
        val appleArtwork =
            CanvasArtwork(
                source = CanvasSource.APPLE_MUSIC,
                name = "Track A",
                videoUrl = "https://example.com/apple.mp4",
            )
        val fileA = File(tempDir, "regular_apple.mp4").apply { writeBytes(ByteArray(1024)) }
        val appleEntry =
            CanvasCacheEntry(
                mediaId = "apple_track",
                artwork = appleArtwork,
                regularFileName = fileA.name,
                verticalFileName = null,
                createdAtMs = System.currentTimeMillis(),
                lastAccessedAtMs = System.currentTimeMillis(),
            )
        index.persistEntry(tempDir, appleEntry)

        // TIDAL rejects Apple Music entry without touching playback decoder
        assertNull(index.get("apple_track", source = CanvasSource.TIDAL))

        // Untagged entry (legacy backward compatibility)
        val untaggedArtwork =
            CanvasArtwork(
                source = null,
                name = "Legacy Track",
                videoUrl = "https://example.com/legacy.mp4",
            )
        val fileLegacy = File(tempDir, "legacy.mp4").apply { writeBytes(ByteArray(1024)) }
        val legacyEntry =
            CanvasCacheEntry(
                mediaId = "legacy_track",
                artwork = untaggedArtwork,
                regularFileName = fileLegacy.name,
                verticalFileName = null,
                createdAtMs = System.currentTimeMillis(),
                lastAccessedAtMs = System.currentTimeMillis(),
            )
        index.persistEntry(tempDir, legacyEntry)

        // Explicit sources reject untagged legacy entry
        assertNull(index.get("legacy_track", source = CanvasSource.TIDAL))
        assertNull(index.get("legacy_track", source = CanvasSource.APPLE_MUSIC))

        // ALL accepts entry source presence in index
        assertEquals(CanvasSource.APPLE_MUSIC, index.getEntry("apple_track")?.artwork?.source)
        assertNull(index.getEntry("legacy_track")?.artwork?.source)
    }

    @Test
    fun removeIfDifferentSourceDeletesFilesAndIndexEntry() {
        val appleArtwork =
            CanvasArtwork(
                source = CanvasSource.APPLE_MUSIC,
                name = "Track B",
                videoUrl = "https://example.com/apple_b.mp4",
            )
        val file = File(tempDir, "video_b.mp4").apply { writeBytes(ByteArray(2048)) }
        val entry =
            CanvasCacheEntry(
                mediaId = "track_b",
                artwork = appleArtwork,
                regularFileName = file.name,
                verticalFileName = null,
                createdAtMs = System.currentTimeMillis(),
                lastAccessedAtMs = System.currentTimeMillis(),
            )
        index.persistEntry(tempDir, entry)

        assertTrue(file.exists())
        assertNotNull(index.getEntry("track_b"))

        // Checking with same source does NOT remove
        val removedSame = index.removeIfDifferentSource("track_b", CanvasSource.APPLE_MUSIC)
        assertFalse(removedSame)
        assertTrue(file.exists())
        assertNotNull(index.getEntry("track_b"))

        // Checking with different source removes entry and deletes file
        val removedDifferent = index.removeIfDifferentSource("track_b", CanvasSource.TIDAL)
        assertTrue(removedDifferent)
        assertFalse(file.exists())
        assertNull(index.getEntry("track_b"))
    }

    @Test
    fun getCurrentPlayableWithRequiredSourceRejectsMismatchedSource() {
        val appleArtwork =
            CanvasArtwork(
                source = CanvasSource.APPLE_MUSIC,
                name = "Track C",
                videoUrl = "https://example.com/apple_c.mp4",
            )
        val file = File(tempDir, "video_c.mp4").apply { writeBytes(ByteArray(2048)) }
        val entry =
            CanvasCacheEntry(
                mediaId = "track_c",
                artwork = appleArtwork,
                regularFileName = file.name,
                verticalFileName = null,
                createdAtMs = System.currentTimeMillis(),
                lastAccessedAtMs = System.currentTimeMillis(),
            )
        index.persistEntry(tempDir, entry)

        // TIDAL required source returns null directly without touching playback decoder
        assertNull(index.getCurrentPlayable("track_c", requiredSource = CanvasSource.TIDAL))
        assertNull(index.getCurrentPlayable("track_c", requiredSource = CanvasSource.BETTER_LYRICS))
    }
}
