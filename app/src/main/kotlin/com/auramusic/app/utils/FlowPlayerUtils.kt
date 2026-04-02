package com.auramusic.app.utils

import com.auramusic.flow.FlowVideo
import com.auramusic.flow.VideoItem
import com.auramusic.flow.FlowVideo.VideoStreamResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

object FlowPlayerUtils {
    private const val logTag = "FlowPlayerUtils"

    suspend fun getVideoStreamUrl(videoId: String): Result<String> = withContext(Dispatchers.IO) {
        Timber.tag(logTag).d("FlowPlayerUtils: Fetching video stream URL for videoId: $videoId")
        
        FlowVideo.getVideoStreamUrl(videoId).map { result ->
            Timber.tag(logTag).d("FlowPlayerUtils: Successfully obtained video URL with mimeType: ${result.mimeType}")
            "${result.url}|${result.mimeType}"
        }.also { result ->
            result.onSuccess { _ ->
                Timber.tag(logTag).d("FlowPlayerUtils: Successfully obtained video URL")
            }.onFailure { e ->
                Timber.tag(logTag).e(e, "FlowPlayerUtils: Failed to get video URL")
            }
        }
    }

    suspend fun hasVideoPlayback(videoId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            FlowVideo.hasVideoPlayback(videoId)
        } catch (e: Exception) {
            Timber.tag(logTag).e(e, "FlowPlayerUtils: Error checking video availability")
            false
        }
    }

    suspend fun getVideoDetails(videoId: String): Result<VideoItem> = withContext(Dispatchers.IO) {
        FlowVideo.getVideoDetails(videoId)
    }
}