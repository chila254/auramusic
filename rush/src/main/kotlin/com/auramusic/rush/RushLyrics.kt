package com.auramusic.rush

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.auramusic.rush.TTMLParser

object RushLyrics {
    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    },
                )
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }

            expectSuccess = false
        }
    }

    private val servers = listOf(
        "https://lyricsplus.atomix.one",
        "https://lyricsplus-seven.vercel.app",
        "https://lyricsplus.prjktla.workers.dev",
        "https://lyrics-plus-backend.vercel.app",
        "https://youlyplus.binimum.org",
    )

    @Serializable
    private data class TTMLResponse(val ttml: String)

    @Serializable
    private data class LRCResponse(val lrc: String)

    @Serializable
    private data class SearchResponse(
        val results: List<SearchResult> = emptyList()
    )

    @Serializable
    data class SearchResult(
        val id: String = "",
        val title: String = "",
        val artist: String = "",
        val album: String? = null,
        val duration: Int? = null,
        val provider: String = ""
    )

    /**
     * Fetch TTML lyrics from LyricsPlus API
     */
    private suspend fun fetchTTML(
        title: String,
        artist: String,
    ): String? = runCatching {
        for (server in servers) {
            try {
                val response = client.get("$server/v1/ttml/get") {
                    parameter("title", title)
                    parameter("artist", artist)
                }
                if (response.status == HttpStatusCode.OK) {
                    return@runCatching response.body<TTMLResponse>().ttml
                }
            } catch (e: Exception) {
                // Try next server
                continue
            }
        }
        null
    }.getOrNull()

    /**
     * Fetch LRC lyrics directly from LyricsPlus API
     */
    private suspend fun fetchLRC(
        title: String,
        artist: String,
    ): String? = runCatching {
        for (server in servers) {
            try {
                val response = client.get("$server/v1/lrc/get") {
                    parameter("title", title)
                    parameter("artist", artist)
                }
                if (response.status == HttpStatusCode.OK) {
                    return@runCatching response.body<LRCResponse>().lrc
                }
            } catch (e: Exception) {
                // Try next server
                continue
            }
        }
        null
    }.getOrNull()

    /**
     * Search for lyrics using LyricsPlus API
     */
    suspend fun searchLyrics(
        title: String,
        artist: String,
    ): List<SearchResult> = runCatching {
        for (server in servers) {
            try {
                val response = client.get("$server/v1/search") {
                    parameter("title", title)
                    parameter("artist", artist)
                }
                if (response.status == HttpStatusCode.OK) {
                    return@runCatching response.body<SearchResponse>().results
                }
            } catch (e: Exception) {
                // Try next server
                continue
            }
        }
        emptyList()
    }.getOrDefault(emptyList())

    /**
     * Get lyrics - tries TTML first, then falls back to LRC
     */
    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int = -1,
        album: String? = null,
    ) = runCatching {
        // Try LRC first (faster and more direct)
        val lrc = fetchLRC(title, artist)
        if (lrc != null && lrc.isNotBlank()) {
            // Fix malformed timestamps if needed
            val fixedLrc = fixMalformedLrc(lrc, duration)
            return@runCatching fixedLrc
        }

        // Fall back to TTML and convert to LRC
        val ttml = fetchTTML(title, artist)
            ?: throw IllegalStateException("Lyrics unavailable")
        
        val parsedLines = TTMLParser.parseTTML(ttml)
        if (parsedLines.isEmpty()) {
            throw IllegalStateException("Failed to parse lyrics")
        }
        
        // If no word-level timing, try to generate line-level sync
        if (!TTMLParser.hasWordLevelTiming(parsedLines)) {
            val linesWithTiming = generateLineTiming(parsedLines, duration)
            if (linesWithTiming != null) {
                return@runCatching TTMLParser.toLRC(linesWithTiming)
            }
        }
        
        TTMLParser.toLRC(parsedLines)
    }

    /**
     * Fix malformed LRC by distributing timestamps evenly if all are at 0
     */
    private fun fixMalformedLrc(lrc: String, duration: Int): String {
        val lines = lrc.lines().filter { it.isNotBlank() }
        val linePattern = Regex("\\[(\\d{1,2}):(\\d{2})\\.(\\d{2,3})\\](.*)")
        
        val timestamps = lines.mapNotNull { line ->
            val match = linePattern.matchEntire(line.trim())
            match?.let {
                val min = it.groupValues[1].toLongOrNull() ?: 0L
                val sec = it.groupValues[2].toLongOrNull() ?: 0L
                val ms = it.groupValues[3].let { ms -> 
                    if (ms.length == 3) ms.toLongOrNull() ?: 0L else (ms.toLongOrNull() ?: 0L) * 10 
                }
                min * 60000 + sec * 1000 + ms
            }
        }
        
        // Check if all timestamps are 0 or very small (malformed)
        val maxTimestamp = timestamps.maxOrNull() ?: 0L
        if (maxTimestamp < 1000 && timestamps.isNotEmpty()) {
            // All timestamps are near 0 - need to distribute evenly
            val validLines = lines.filter { linePattern.matches(it.trim()) }
            if (validLines.isNotEmpty() && duration > 0) {
                val estimatedDuration = duration.toLong() * 1000
                val interval = estimatedDuration / validLines.size
                
                return buildString {
                    validLines.forEachIndexed { index, line ->
                        val text = linePattern.matchEntire(line.trim())?.groupValues?.get(4) ?: ""
                        val timeMs = index * interval
                        val minutes = timeMs / 60000
                        val seconds = (timeMs % 60000) / 1000
                        val centiseconds = (timeMs % 1000) / 10
                        appendLine(String.format("[%02d:%02d.%02d]%s", minutes, seconds, centiseconds, text))
                    }
                }
            }
        }
        
        return lrc
    }

    /**
     * Generate line-level timing for TTML parsed lines that lack word timing
     */
    private fun generateLineTiming(lines: List<TTMLParser.ParsedLine>, duration: Int): List<TTMLParser.ParsedLine>? {
        if (lines.isEmpty() || duration <= 0) return null
        
        val totalDuration = duration.toDouble() * 1000
        val interval = totalDuration / lines.size
        
        return lines.mapIndexed { index, line ->
            line.copy(startTime = index * interval / 1000)
        }
    }

    /**
     * Get all available lyrics (for search results)
     */
    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int = -1,
        album: String? = null,
        callback: (String) -> Unit,
    ) {
        getLyrics(title, artist, duration, album)
            .onSuccess { lrcString ->
                callback(lrcString)
            }
    }

    data class LyricsResult(
        val lrc: String,
        val hasWordSync: Boolean,
        val quality: TTMLParser.LyricsQuality
    )

    /**
     * Get lyrics with quality metadata - helps UI decide between word/line sync
     */
    suspend fun getLyricsWithQuality(
        title: String,
        artist: String,
        duration: Int = -1,
        album: String? = null,
    ) = runCatching {
        // Try LRC first (faster and more direct)
        val lrc = fetchLRC(title, artist)
        if (lrc != null && lrc.isNotBlank()) {
            val lines = lrc.lines().filter { it.startsWith("[") && it.contains("]") }
            return@runCatching LyricsResult(
                lrc = lrc,
                hasWordSync = lrc.contains(Regex("<.*>")),
                quality = TTMLParser.LyricsQuality(
                    hasLineSync = lines.isNotEmpty(),
                    hasWordSync = lrc.contains(Regex("<.*>")),
                    totalLines = lines.size,
                    linesWithWords = 0
                )
            )
        }

        // Fall back to TTML and convert to LRC
        val ttml = fetchTTML(title, artist)
            ?: throw IllegalStateException("Lyrics unavailable")
        
        val parsedLines = TTMLParser.parseTTML(ttml)
        if (parsedLines.isEmpty()) {
            throw IllegalStateException("Failed to parse lyrics")
        }
        
        val quality = TTMLParser.analyzeQuality(parsedLines)
        LyricsResult(
            lrc = TTMLParser.toLRC(parsedLines),
            hasWordSync = quality.hasWordSync,
            quality = quality
        )
    }
}
