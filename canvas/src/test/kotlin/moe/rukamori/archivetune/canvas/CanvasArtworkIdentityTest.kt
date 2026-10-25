/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.canvas

import moe.rukamori.archivetune.canvas.models.CanvasArtwork
import moe.rukamori.archivetune.canvas.models.matchesSongIdentity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasArtworkIdentityTest {

    @Test
    fun matchesSongIdentityWithCleanMetadata() {
        val artwork =
            CanvasArtwork(
                name = "Blinding Lights",
                artist = "The Weeknd",
            )
        assertTrue(artwork.matchesSongIdentity("Blinding Lights", "The Weeknd"))
    }

    @Test
    fun matchesSongIdentityWithFeaturingAndPunctuation() {
        val artwork =
            CanvasArtwork(
                name = "Starboy (feat. Daft Punk)",
                artist = "The Weeknd & Daft Punk",
            )
        assertTrue(artwork.matchesSongIdentity("Starboy", "The Weeknd"))
    }

    @Test
    fun matchesSongIdentityWithVideoQualifiers() {
        val artwork =
            CanvasArtwork(
                name = "Save Your Tears - Official Music Video",
                artist = "The Weeknd",
            )
        assertTrue(artwork.matchesSongIdentity("Save Your Tears", "The Weeknd"))
    }

    @Test
    fun rejectsMismatchedTrackOrArtist() {
        val artwork =
            CanvasArtwork(
                name = "Save Your Tears",
                artist = "The Weeknd",
            )
        assertFalse(artwork.matchesSongIdentity("Blinding Lights", "The Weeknd"))
        assertFalse(artwork.matchesSongIdentity("Save Your Tears", "Taylor Swift"))
    }
}
