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
            // Validate LRC has proper line-level timing
            if (!hasValidLineTiming(lrc)) {
                throw IllegalStateException("Lyrics have invalid timestamps")
            }
            return@runCatching lrc
        }

        // Fall back to TTML and convert to LRC
        val ttml = fetchTTML(title, artist)
            ?: throw IllegalStateException("Lyrics unavailable")
        
        val parsedLines = TTMLParser.parseTTML(ttml)
        if (parsedLines.isEmpty()) {
            throw IllegalStateException("Failed to parse lyrics")
        }
        
        // Check if TTML has any valid line timing (at least some lines with non-zero times)
        val hasValidLineTime = parsedLines.any { it.startTime > 0 }
        if (!hasValidLineTime) {
            throw IllegalStateException("Lyrics have invalid timestamps")
        }
        
        // Check if TTML has word-level timing, if not warn but still return
        if (!TTMLParser.hasWordLevelTiming(parsedLines)) {
            // Log or handle as needed - line-level sync will still work
        }
        
        TTMLParser.toLRC(parsedLines)
    }

    /**
     * Check if LRC has valid line-level timing (not all zeros or same time)
     */
    private fun hasValidLineTiming(lrc: String): Boolean {
        val linePattern = Regex("\\[(\\d{1,2}):(\\d{2})\\.(\\d{2,3})\\]")
        val times = linePattern.findAll(lrc).map { match ->
            val min = match.groupValues[1].toLongOrNull() ?: 0L
            val sec = match.groupValues[2].toLongOrNull() ?: 0L
            val ms = match.groupValues[3].let { 
                if (it.length == 3) it.toLongOrNull() ?: 0L else (it.toLongOrNull() ?: 0L) * 10 
            }
            min * 60000 + sec * 1000 + ms
        }.toList()
        
        // Must have at least some lines and at least one non-zero time
        return times.isNotEmpty() && times.any { it > 0 }
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
