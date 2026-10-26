package moe.rukamori.archivetune.playback.history

import io.mockk.every
import io.mockk.mockk
import moe.rukamori.archivetune.playback.MusicService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServicePlaybackHistoryCoordinatorTest {
    private val state = ServicePlaybackHistoryCoordinator()
    private val service = mockk<MusicService> {
        every { historyCoordinator } returns state
    }
    private val store = PlaybackHistoryStore(service)

    @Test
    fun finalizationUsesTheSingleServiceSessionState() {
        state.currentHistoryMediaId = "track"
        state.currentHistorySessionToken = 7L
        state.currentHistoryEventId = 12L
        state.currentHistoryRemoteRegistered = true

        store.enqueueCurrentHistorySessionForFinalization()
        store.enqueueCurrentHistorySessionForFinalization()

        assertTrue(state.currentHistorySessionQueued)
        assertEquals(1, state.pendingHistoryFinalizations["track"]?.size)
        assertEquals(PendingHistoryFinalization(7L, 12L, true), store.popPendingHistoryFinalization("track"))
        assertNull(store.popPendingHistoryFinalization("track"))
        assertTrue(state.pendingHistoryFinalizations.isEmpty())
    }

    @Test
    fun delayedRecordingUpdatesOnlyTheMatchingPendingSession() {
        state.pendingHistoryFinalizations["track"] = mutableListOf(
            PendingHistoryFinalization(1L, null, false),
            PendingHistoryFinalization(2L, null, false),
        )

        store.updatePendingHistoryFinalization("track", 2L, ImmediateHistoryResult(42L, true))

        assertEquals(PendingHistoryFinalization(1L, null, false), store.popPendingHistoryFinalization("track"))
        assertEquals(PendingHistoryFinalization(2L, 42L, true), store.popPendingHistoryFinalization("track"))
    }

    @Test
    fun serviceInstancesDoNotSharePendingSessions() {
        state.currentHistoryMediaId = "track"
        state.currentHistorySessionToken = 1L
        store.enqueueCurrentHistorySessionForFinalization()

        val otherState = ServicePlaybackHistoryCoordinator()
        val otherService = mockk<MusicService> {
            every { historyCoordinator } returns otherState
        }

        assertNull(PlaybackHistoryStore(otherService).popPendingHistoryFinalization("track"))
        assertEquals(1, state.pendingHistoryFinalizations["track"]?.size)
        assertTrue(otherState.historyRecordingJobs.isEmpty())
    }
}
