/*
 * Copyright (C) 2026 MuwMix <https://github.com/MuwMix> (YumaPlayer)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package moe.rukamori.archivetune.playback

import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.analytics.AnalyticsListener

@OptIn(UnstableApi::class)
internal class SkipSilenceAudioDiagnostics : AnalyticsListener {

    private fun resolveMediaId(eventTime: AnalyticsListener.EventTime): String? {
        val timeline = if (!eventTime.timeline.isEmpty) eventTime.timeline else eventTime.currentTimeline
        val windowIndex = if (!eventTime.timeline.isEmpty) eventTime.windowIndex else eventTime.currentWindowIndex
        if (timeline.isEmpty || windowIndex < 0 || windowIndex >= timeline.windowCount) {
            return null
        }
        return runCatching {
            timeline.getWindow(windowIndex, Timeline.Window()).mediaItem.mediaId
        }.getOrNull()
    }

    override fun onAudioInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: Format,
        decoderReuseEvaluation: DecoderReuseEvaluation?,
    ) {
        val mediaId = resolveMediaId(eventTime)
        PlaybackTiming.log(
            event = "AUDIO_INPUT_FORMAT_CHANGED",
            mediaId = mediaId,
            extra = "encoding=${format.pcmEncoding} sampleRate=${format.sampleRate} channelCount=${format.channelCount}",
        )
    }

    override fun onAudioSinkError(
        eventTime: AnalyticsListener.EventTime,
        audioSinkError: Exception,
    ) {
        val mediaId = resolveMediaId(eventTime)
        val errClass = audioSinkError.javaClass.simpleName
        val errMsg = audioSinkError.message ?: ""
        PlaybackTiming.log(
            event = "AUDIO_SINK_ERROR",
            mediaId = mediaId,
            extra = "errorClass=$errClass message=$errMsg",
        )
    }

    override fun onAudioUnderrun(
        eventTime: AnalyticsListener.EventTime,
        bufferSize: Int,
        bufferSizeMs: Long,
        elapsedSinceLastFeedMs: Long,
    ) {
        val mediaId = resolveMediaId(eventTime)
        PlaybackTiming.log(
            event = "AUDIO_UNDERRUN",
            mediaId = mediaId,
            extra = "bufferSizeMs=$bufferSizeMs elapsedSinceLastFeedMs=$elapsedSinceLastFeedMs",
        )
    }
}
