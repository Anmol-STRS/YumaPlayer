/*
 * YumaPlayer (2026) | Original work by MuwMix
 * GPL-3.0 License | Contributors: see git history
 */
package moe.rukamori.archivetune.cipher

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CipherTraceTest {

    @Before
    @After
    fun cleanup() {
        CipherTrace.sink = null
    }

    @Test
    fun nullSinkDoesNotThrowOrLog() {
        CipherTrace.sink = null
        CipherTrace.log("EVENT", "details=test")
        CipherTrace.log("EVENT")
    }

    @Test
    fun formattingWithAndWithoutDetails() {
        val messages = mutableListOf<String>()
        CipherTrace.sink = { messages.add(it) }

        CipherTrace.log("SIMPLE_EVENT")
        CipherTrace.log("EVENT_WITH_DETAILS", "key=value test=1")

        assertEquals(2, messages.size)
        assertEquals("CipherTrace | SIMPLE_EVENT", messages[0])
        assertEquals("CipherTrace | EVENT_WITH_DETAILS key=value test=1", messages[1])
    }

    @Test
    fun throwingSinkIsolation() {
        var callCount = 0
        CipherTrace.sink = {
            callCount++
            throw RuntimeException("Sink failure")
        }

        CipherTrace.log("EVENT_FAIL")
        assertEquals(1, callCount)
    }
}
