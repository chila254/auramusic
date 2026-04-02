package com.auramusic.flow

import com.auramusic.innertube.NewPipeExtractor
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.YouTubeClient.Companion.WEB_REMIX

object FlowVideo {
    data class VideoStreamResult(
        val url: String,
        val mimeType: String
    )

    suspend fun getVideoStreamUrl(videoId: String): Result<VideoStreamResult> = runCatching {
        // First try: Use NewPipeExtractor directly to get streams (most reliable)
        val streamInfo = NewPipeExtractor.getStreamInfo(videoId)
        
        if (streamInfo != null) {
            val videoStreams = streamInfo.videoStreams + streamInfo.videoOnlyStreams
            
            if (videoStreams.isNotEmpty()) {
                val bestStream = videoStreams.firstOrNull()
                
                if (bestStream != null) {
                    val url = bestStream.content ?: bestStream.url
                    val mimeType = bestStream.format?.mimeType ?: "video/mp4"
                    if (url != null) {
                        return@runCatching VideoStreamResult(url, mimeType)
                    }
                }
            }
        }

        // Second try: Use innerTube with WEB client and try to get stream URL
        val playerResponse = YouTube.player(videoId, client = WEB_REMIX).getOrThrow()
        
        if (playerResponse.playabilityStatus.status != "OK") {
            throw Exception("Playability error: ${playerResponse.playabilityStatus.reason ?: playerResponse.playabilityStatus.status}")
        }

        val streamingData = playerResponse.streamingData ?: throw Exception("No streaming data")

        // Try muxed formats
        val muxedFormats = streamingData.formats ?: emptyList()
        val videoMuxed = muxedFormats.filter { it.isVideo }.maxByOrNull { it.bitrate }
        val muxedUrl = videoMuxed?.url
        val muxedMimeType = videoMuxed?.mimeType
        if (muxedUrl != null) {
            return@runCatching VideoStreamResult(muxedUrl, muxedMimeType ?: "video/mp4")
        }

        // Try adaptive video formats
        val adaptiveFormats = streamingData.adaptiveFormats
        val adaptiveVideo = adaptiveFormats.filter { it.isVideo }.maxByOrNull { it.bitrate }
        val adaptiveUrl = adaptiveVideo?.url
        val adaptiveMimeType = adaptiveVideo?.mimeType
        if (adaptiveUrl != null) {
            return@runCatching VideoStreamResult(adaptiveUrl, adaptiveMimeType ?: "video/mp4")
        }

        throw Exception("No video URL available")
    }

    suspend fun hasVideoPlayback(videoId: String): Boolean {
        return try {
            val streamInfo = NewPipeExtractor.getStreamInfo(videoId)
            if (streamInfo != null) {
                val hasVideo = streamInfo.videoStreams.isNotEmpty() || streamInfo.videoOnlyStreams.isNotEmpty()
                if (hasVideo) return true
            }

            // Fallback to innerTube check
            val playerResponse = YouTube.player(videoId, client = WEB_REMIX).getOrNull()
            val streamingData = playerResponse?.streamingData
            
            if (streamingData != null) {
                val hasMuxedVideo = streamingData.formats?.any { it.isVideo } == true
                val hasAdaptiveVideo = streamingData.adaptiveFormats.any { it.isVideo }
                return hasMuxedVideo || hasAdaptiveVideo
            }
            
            false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getVideoDetails(videoId: String): Result<VideoItem> = runCatching {
        val response = YouTube.player(videoId, client = WEB_REMIX).getOrThrow()
        val videoDetails = response.videoDetails ?: throw Exception("No video details")

        VideoItem(
            id = videoDetails.videoId,
            title = videoDetails.title ?: "",
            channelName = videoDetails.author ?: "",
            channelId = videoDetails.channelId,
            thumbnailUrl = videoDetails.thumbnail?.thumbnails?.lastOrNull()?.url ?: "",
            duration = videoDetails.lengthSeconds.toIntOrNull() ?: 0,
            viewCount = videoDetails.viewCount?.toLongOrNull() ?: 0L,
            uploadDate = null,
            description = null,
        )
    }
}