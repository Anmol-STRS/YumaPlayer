/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.canvas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasSourceTest {

    @Test
    fun fromStringNormalizesKnownValues() {
        assertEquals(CanvasSource.BETTER_LYRICS, CanvasSource.fromString("BETTER_LYRICS"))
        assertEquals(CanvasSource.BETTER_LYRICS, CanvasSource.fromString("better_lyrics"))
        assertEquals(CanvasSource.APPLE_MUSIC, CanvasSource.fromString(" apple_music "))
        assertEquals(CanvasSource.TIDAL, CanvasSource.fromString("Tidal"))
        assertEquals(CanvasSource.SPOTIFY, CanvasSource.fromString("spotify"))
        assertEquals(CanvasSource.ALL, CanvasSource.fromString("ALL"))
    }

    @Test
    fun fromStringMapsBothAndUnknownToAll() {
        assertEquals(CanvasSource.ALL, CanvasSource.fromString("BOTH"))
        assertEquals(CanvasSource.ALL, CanvasSource.fromString("both"))
        assertEquals(CanvasSource.ALL, CanvasSource.fromString(null))
        assertEquals(CanvasSource.ALL, CanvasSource.fromString(""))
        assertEquals(CanvasSource.ALL, CanvasSource.fromString("INVALID_SOURCE"))
    }

    @Test
    fun acceptsValidatesProviderMembership() {
        assertTrue(CanvasSource.ALL.accepts(CanvasSource.BETTER_LYRICS))
        assertTrue(CanvasSource.ALL.accepts(CanvasSource.APPLE_MUSIC))
        assertTrue(CanvasSource.ALL.accepts(CanvasSource.TIDAL))
        assertTrue(CanvasSource.ALL.accepts(CanvasSource.SPOTIFY))
        assertTrue(CanvasSource.ALL.accepts(null))
        assertFalse(CanvasSource.ALL.accepts(CanvasSource.ALL))

        assertTrue(CanvasSource.TIDAL.accepts(CanvasSource.TIDAL))
        assertFalse(CanvasSource.TIDAL.accepts(CanvasSource.APPLE_MUSIC))
        assertFalse(CanvasSource.TIDAL.accepts(CanvasSource.BETTER_LYRICS))
        assertFalse(CanvasSource.TIDAL.accepts(null))

        assertFalse(CanvasSource.APPLE_MUSIC.accepts(null))
        assertFalse(CanvasSource.BETTER_LYRICS.accepts(null))
        assertFalse(CanvasSource.SPOTIFY.accepts(null))
    }
}
