package moe.rukamori.archivetune.playback.history

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import java.util.concurrent.ConcurrentHashMap

internal class ServicePlaybackHistoryCoordinator {
    var nextHistorySessionToken = 0L
    var currentHistorySessionToken = 0L
    var currentHistoryMediaId: String? = null
    var currentHistoryAccumulatedPlayMs = 0L
    var currentHistoryStartedAtElapsedMs: Long? = null
    var currentHistoryEventId: Long? = null
    var currentHistoryRemoteRegistered = false
    var currentHistoryImmediateAttempted = false
    var currentHistorySessionQueued = false
    var historyThresholdJob: Job? = null
    val pendingHistoryFinalizations = mutableMapOf<String, MutableList<PendingHistoryFinalization>>()
    val historyRecordingJobs = ConcurrentHashMap<Long, Deferred<ImmediateHistoryResult>>()
}
