package com.auramusic.flow

import android.util.Log
import com.auramusic.innertube.NewPipeExtractor
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.auramusic.innertube.YouTube.SearchFilter
import com.auramusic.innertube.models.YTItem

object FlowVideo {
    private const val TAG = "FlowVideo"
    data class VideoStreamResult(
        val url: String,
        val mimeType: String
    )

    data class VideoSearchResult(
        val videoId: String,
        val title: String,
        val channelName: String,
        val thumbnailUrl: String
    )

    /**
     * Available video quality options
     */
    enum class VideoQuality(val height: Int, val label: String) {
        QUALITY_360P(360, "360p"),
        QUALITY_480P(480, "480p"),
        QUALITY_720P(720, "720p"),
        QUALITY_1080P(1080, "1080p")
    }

    // Default preferred quality - 720p
    var currentPreferredQuality: VideoQuality = VideoQuality.QUALITY_720P

    /**
     * Set preferred video quality
     */
    fun setPreferredVideoQuality(quality: VideoQuality) {
        currentPreferredQuality = quality
    }

    /**
     * Get all available qualities from a list of video streams
     */
    private fun getAvailableQualities(streams: List<org.schabi.newpipe.extractor.stream.VideoStream>): List<VideoQuality> {
        val qualities = mutableSetOf<VideoQuality>()
        for (stream in streams) {
            val height = stream.height
            when {
                height >= 1080 -> qualities.add(VideoQuality.QUALITY_1080P)
                height >= 720 -> qualities.add(VideoQuality.QUALITY_720P)
                height >= 480 -> qualities.add(VideoQuality.QUALITY_480P)
                height >= 360 -> qualities.add(VideoQuality.QUALITY_360P)
            }
        }
        return qualities.sortedBy { it.height }
    }

    /**
     * Find the best stream matching the preferred quality
     * Falls back to lower quality if preferred not available
     */
    private fun findBestStream(
        streams: List<org.schabi.newpipe.extractor.stream.VideoStream>,
        requireAudio: Boolean
    ): org.schabi.newpipe.extractor.stream.VideoStream? {
        if (streams.isEmpty()) return null

        // Filter streams based on whether we need audio
        val filteredStreams = if (requireAudio) {
            streams.filter { !it.isVideoOnly() }
        } else {
            streams
        }

        // Try to find exact quality match
        for (quality in listOf(currentPreferredQuality, VideoQuality.QUALITY_1080P, VideoQuality.QUALITY_720P, VideoQuality.QUALITY_480P, VideoQuality.QUALITY_360P)) {
            val matching = filteredStreams.filter { stream ->
                val height = stream.height
                // Less restrictive: prefer exact match or higher quality
                // For 720p, accept 720-1080+ (any quality at or above)
                height >= quality.height
            }
            if (matching.isNotEmpty()) {
                // Return highest quality among matching (max height, not max bitrate)
                return matching.maxByOrNull { it.height }
            }
        }

        // Fallback to any stream with audio if required, or any stream
        // Use max height (quality) instead of max bitrate
        return if (requireAudio && filteredStreams.isNotEmpty()) {
            filteredStreams.maxByOrNull { it.height }
        } else {
            streams.maxByOrNull { it.height }
        }
    }

    /**
     * Search for an official music video for a song
     */
    suspend fun searchOfficialMusicVideo(songTitle: String, artistName: String): Result<VideoSearchResult> = runCatching {
        val searchQuery = "$songTitle $artistName official music video"
        
        val searchResult = YouTube.search(searchQuery, SearchFilter.FILTER_VIDEO).getOrThrow()
        
        // Find the best matching video - prefer official music videos
        var bestVideo: YTItem? = null
        
        for (item in searchResult.items) {
            if (item is YTItem) {
                val title = item.title.lowercase()
                // Prefer videos that contain "official", "music video", "mv", "vevo", or the song title
                val isPreferred = title.contains("official") || 
                                  title.contains("music video") ||
                                  title.contains(" mv ") ||
                                  title.contains("vevo") ||
                                  title.contains(songTitle.lowercase())
                
                if (isPreferred && bestVideo == null) {
                    bestVideo = item
                }
            }
        }
        
        // Fall back to first result if no preferred video found
        bestVideo = bestVideo ?: searchResult.items.firstOrNull()
        
        if (bestVideo == null) {
            throw Exception("No music video found")
        }
        
        // Build the VideoSearchResult from the YTItem
        VideoSearchResult(
            videoId = bestVideo.id,
            title = bestVideo.title,
            channelName = bestVideo.title, // YTItem doesn't have channelName, use title as fallback
            thumbnailUrl = bestVideo.thumbnail ?: ""
        )
    }

    /**
     * Get video stream URL, with automatic search fallback
     */
    suspend fun getVideoStreamUrlWithFallback(songTitle: String, artistName: String, videoId: String): Result<VideoSearchResult> = runCatching {
        // First try: direct video lookup
        val directResult = runCatching { getVideoStreamUrl(videoId).getOrNull() }
        
        if (directResult.isSuccess && directResult.getOrNull() != null) {
            val details = getVideoDetails(videoId).getOrNull()
            return@runCatching VideoSearchResult(
                videoId = videoId,
                title = details?.title ?: songTitle,
                channelName = details?.channelName ?: artistName,
                thumbnailUrl = details?.thumbnailUrl ?: ""
            )
        }
        
        // Direct lookup failed - try searching for official music video
        val searchResult = searchOfficialMusicVideo(songTitle, artistName).getOrNull()
            ?: throw Exception("No video available for this song")
        
        searchResult
    }

    /**
     * Get video stream URL using NewPipeExtractor
     * Properly prioritizes muxed streams (video+audio) and respects quality preference
     */
    suspend fun getVideoStreamUrl(videoId: String): Result<VideoStreamResult> = runCatching {
        val streamInfo = NewPipeExtractor.getStreamInfo(videoId)
        
        if (streamInfo != null) {
            val muxedVideoStreams = streamInfo.videoStreams // These have both video and audio
            val videoOnlyStreams = streamInfo.videoOnlyStreams // These are video only (no audio)
            
            Log.d(TAG,"FlowVideo: Found ${muxedVideoStreams.size} muxed and ${videoOnlyStreams.size} video-only streams")
            
            // First try: find muxed stream (has audio) matching preferred quality
            val bestMuxedStream = findBestStream(muxedVideoStreams, requireAudio = true)
            if (bestMuxedStream != null) {
                val url = bestMuxedStream.content ?: bestMuxedStream.url
                val mimeType = bestMuxedStream.format?.mimeType ?: "video/mp4"
                if (url != null) {
                    Log.d(TAG,"FlowVideo: Using muxed stream with resolution ${bestMuxedStream.resolution}")
                    return@runCatching VideoStreamResult(url, mimeType)
                }
            }
            
            // Second try: find video-only stream matching preferred quality
            val bestVideoOnlyStream = findBestStream(videoOnlyStreams, requireAudio = false)
            if (bestVideoOnlyStream != null) {
                val url = bestVideoOnlyStream.content ?: bestVideoOnlyStream.url
                val mimeType = bestVideoOnlyStream.format?.mimeType ?: "video/mp4"
                if (url != null) {
                    Log.d(TAG,"FlowVideo: Using video-only stream with resolution ${bestVideoOnlyStream.resolution}")
                    return@runCatching VideoStreamResult(url, mimeType)
                }
            }
            
            // Last resort: use highest bitrate muxed stream regardless of quality
            if (muxedVideoStreams.isNotEmpty()) {
                val fallbackStream = muxedVideoStreams.maxByOrNull { it.bitrate }
                if (fallbackStream != null) {
                    val url = fallbackStream.content ?: fallbackStream.url
                    val mimeType = fallbackStream.format?.mimeType ?: "video/mp4"
                    if (url != null) {
                        Log.d(TAG,"FlowVideo: Using fallback muxed stream with resolution ${fallbackStream.resolution}")
                        return@runCatching VideoStreamResult(url, mimeType)
                    }
                }
            }
            
            // Last resort: use highest bitrate video-only stream
            if (videoOnlyStreams.isNotEmpty()) {
                val fallbackStream = videoOnlyStreams.maxByOrNull { it.bitrate }
                if (fallbackStream != null) {
                    val url = fallbackStream.content ?: fallbackStream.url
                    val mimeType = fallbackStream.format?.mimeType ?: "video/mp4"
                    if (url != null) {
                        Log.d(TAG,"FlowVideo: Using fallback video-only stream with resolution ${fallbackStream.resolution}")
                        return@runCatching VideoStreamResult(url, mimeType)
                    }
                }
            }
        }

        // Fallback to YouTube player API if NewPipe fails
        Log.d(TAG,"FlowVideo: NewPipe failed, trying YouTube player API")
        val playerResponse = YouTube.player(videoId, client = WEB_REMIX).getOrThrow()
        
        if (playerResponse.playabilityStatus.status != "OK") {
            throw Exception("Playability error: ${playerResponse.playabilityStatus.reason ?: playerResponse.playabilityStatus.status}")
        }

        val streamingData = playerResponse.streamingData ?: throw Exception("No streaming data")

        // Check if we need signature deobfuscation
        val needsDeobfuscation = streamingData.formats?.any { it.signatureCipher != null || it.cipher != null } == true ||
                streamingData.adaptiveFormats.any { it.signatureCipher != null || it.cipher != null }

        // Try muxed formats first (has both audio and video)
        val muxedFormats = streamingData.formats ?: emptyList()
        
        // Find best muxed format based on preferred quality
        val bestMuxedFormat = findBestFormat(muxedFormats, currentPreferredQuality, requireAudio = true)
        
        var muxedUrl = bestMuxedFormat?.url
        val muxedMimeType = bestMuxedFormat?.mimeType
        
        // If URL is missing and we have signature cipher, try to deobfuscate
        if (muxedUrl == null && bestMuxedFormat != null && needsDeobfuscation) {
            muxedUrl = NewPipeExtractor.getStreamUrl(bestMuxedFormat, videoId)
        }
        
        if (muxedUrl != null) {
            Log.d(TAG,"FlowVideo: Using YouTube muxed format with height ${bestMuxedFormat?.height}")
            return@runCatching VideoStreamResult(muxedUrl, muxedMimeType ?: "video/mp4")
        }

        // Try adaptive formats
        val adaptiveFormats = streamingData.adaptiveFormats
        val bestAdaptiveFormat = findBestFormat(adaptiveFormats, currentPreferredQuality, requireAudio = false)
        
        var adaptiveUrl = bestAdaptiveFormat?.url
        val adaptiveMimeType = bestAdaptiveFormat?.mimeType
        
        // If URL is missing and we have signature cipher, try to deobfuscate
        if (adaptiveUrl == null && bestAdaptiveFormat != null && needsDeobfuscation) {
            adaptiveUrl = NewPipeExtractor.getStreamUrl(bestAdaptiveFormat, videoId)
        }
        
        if (adaptiveUrl != null) {
            Log.d(TAG, "Using YouTube adaptive format with height ${bestAdaptiveFormat?.height}")
            return@runCatching VideoStreamResult(adaptiveUrl, adaptiveMimeType ?: "video/mp4")
        }

        throw Exception("No video URL available")
    }

    /**
     * Find best format based on preferred quality
     */
    private fun findBestFormat(
        formats: List<com.auramusic.innertube.models.response.PlayerResponse.StreamingData.Format>,
        quality: VideoQuality,
        requireAudio: Boolean
    ): com.auramusic.innertube.models.response.PlayerResponse.StreamingData.Format? {
        if (formats.isEmpty()) return null

        // Filter to video formats
        val videoFormats = formats.filter { it.isVideo }

        // Try to find exact quality match
        val targetHeight = quality.height
        val exactMatch = videoFormats.find { (it.height ?: 0) == targetHeight }
        if (exactMatch != null) {
            return exactMatch
        }

        // Try to find close quality (within range)
        val closeMatch = videoFormats.find { (it.height ?: 0) in (targetHeight - 120)..(targetHeight + 60) }
        if (closeMatch != null) {
            return closeMatch
        }

        // Fallback to highest bitrate video format
        return videoFormats.maxByOrNull { it.bitrate }
    }

    suspend fun hasVideoPlayback(videoId: String): Boolean {
        return try {
            // First try NewPipeExtractor
            val streamInfo = NewPipeExtractor.getStreamInfo(videoId)
            if (streamInfo != null) {
                val hasVideo = streamInfo.videoStreams.isNotEmpty() || streamInfo.videoOnlyStreams.isNotEmpty()
                if (hasVideo) return true
            }

            // Fallback to YouTube player API
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