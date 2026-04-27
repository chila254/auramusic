/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.AlbumItem
import com.auramusic.innertube.models.ArtistItem
import com.auramusic.innertube.models.PlaylistItem
import com.auramusic.innertube.models.SongItem
import com.auramusic.innertube.models.YTItem
import com.auramusic.innertube.models.filterExplicit
import com.auramusic.innertube.models.filterVideoSongs
import com.auramusic.innertube.pages.SearchResult
import com.auramusic.app.db.MusicDatabase
import com.auramusic.app.db.entities.Album
import com.auramusic.app.db.entities.Artist
import com.auramusic.app.db.entities.LocalItem
import com.auramusic.app.db.entities.Playlist
import com.auramusic.app.db.entities.Song
import com.auramusic.app.utils.dataStore
import com.auramusic.app.constants.HideExplicitKey
import com.auramusic.app.constants.HideVideoSongsKey
import com.auramusic.app.constants.InnerTubeCookieKey
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CombinedSearchResult(
    val localItems: List<LocalItem> = emptyList(),
    val ytItems: List<YTItem> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class TvSearchViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) : ViewModel() {
    val query = MutableStateFlow("")

    private val _searchResults = MutableStateFlow<CombinedSearchResult>(CombinedSearchResult())
    val searchResults: StateFlow<CombinedSearchResult> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches

    // Local database search results
    private val localSongs = MutableStateFlow<List<Song>>(emptyList())
    private val localArtists = MutableStateFlow<List<Artist>>(emptyList())
    private val localAlbums = MutableStateFlow<List<Album>>(emptyList())
    private val localPlaylists = MutableStateFlow<List<Playlist>>(emptyList())

    // YouTube search results
    private val ytSongs = MutableStateFlow<List<SongItem>>(emptyList())
    private val ytAlbums = MutableStateFlow<List<AlbumItem>>(emptyList())
    private val ytArtists = MutableStateFlow<List<ArtistItem>>(emptyList())
    private val ytPlaylists = MutableStateFlow<List<PlaylistItem>>(emptyList())

    val result = combine(
        localSongs,
        localArtists,
        localAlbums,
        localPlaylists,
        ytSongs,
        ytAlbums,
        ytArtists,
        ytPlaylists,
        _isSearching
    ) { localSongs, localArtists, localAlbums, localPlaylists, ytSongs, ytAlbums, ytArtists, ytPlaylists, isSearching ->
        val localItems = (localSongs + localArtists + localAlbums + localPlaylists)
        val ytItems = (ytSongs + ytAlbums + ytArtists + ytPlaylists)
        CombinedSearchResult(localItems, ytItems, isSearching)
    }.stateIn(viewModelScope, SharingStarted.Lazily, CombinedSearchResult())

    fun setQuery(newQuery: String) {
        query.value = newQuery
        if (newQuery.isNotEmpty()) {
            performSearch(newQuery)
        } else {
            clearResults()
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSearching.value = true
            try {
                // Local database search
                val localResults = performLocalSearch(query)
                updateLocalResults(localResults)

                // YouTube search
                val ytResults = performYouTubeSearch(query)
                updateYTResults(ytResults)

                // Update combined results
                updateCombinedResults()
            } finally {
                _isSearching.value = false
            }
        }
    }

    private suspend fun performLocalSearch(query: String): Map<String, List<LocalItem>> {
        val results = mutableMapOf<String, List<LocalItem>>()

        try {
            val songs = database.searchSongs("%$query%", 20).first()
            results["songs"] = songs
        } catch (e: Exception) {
            results["songs"] = emptyList()
        }

        try {
            val artists = database.searchArtists("%$query%", 10).first()
            results["artists"] = artists
        } catch (e: Exception) {
            results["artists"] = emptyList()
        }

        try {
            val albums = database.searchAlbums("%$query%", 10).first()
            results["albums"] = albums
        } catch (e: Exception) {
            results["albums"] = emptyList()
        }

        try {
            val playlists = database.searchPlaylists("%$query%", 10).first()
            results["playlists"] = playlists
        } catch (e: Exception) {
            results["playlists"] = emptyList()
        }

        return results
    }

    private suspend fun performYouTubeSearch(query: String): SearchResult? {
        return try {
            YouTube.search(query, YouTube.SearchFilter.SONG)
        } catch (e: Exception) {
            null
        }
    }

    private fun updateLocalResults(results: Map<String, List<LocalItem>>) {
        localSongs.value = results["songs"]?.filterIsInstance<Song>() ?: emptyList()
        localArtists.value = results["artists"]?.filterIsInstance<Artist>() ?: emptyList()
        localAlbums.value = results["albums"]?.filterIsInstance<Album>() ?: emptyList()
        localPlaylists.value = results["playlists"]?.filterIsInstance<Playlist>() ?: emptyList()
    }

    private fun updateYTResults(result: SearchResult?) {
        ytSongs.value = result?.songs?.filterExplicit()?.filterVideoSongs() ?: emptyList()
        ytAlbums.value = result?.albums ?: emptyList()
        ytArtists.value = result?.artists ?: emptyList()
        ytPlaylists.value = result?.playlists ?: emptyList()
    }

    private fun updateCombinedResults() {
        val combined = CombinedSearchResult(
            localItems = (localSongs.value + localArtists.value + localAlbums.value + localPlaylists.value),
            ytItems = (ytSongs.value + ytAlbums.value + ytArtists.value + ytPlaylists.value),
            isLoading = false
        )
        _searchResults.value = combined
    }

    private fun clearResults() {
        localSongs.value = emptyList()
        localArtists.value = emptyList()
        localAlbums.value = emptyList()
        localPlaylists.value = emptyList()
        ytSongs.value = emptyList()
        ytAlbums.value = emptyList()
        ytArtists.value = emptyList()
        ytPlaylists.value = emptyList()
        _searchResults.value = CombinedSearchResult()
    }

    fun addToRecentSearches(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _recentSearches.value.toMutableList()
            current.remove(query) // Remove if exists
            current.add(0, query) // Add to front
            if (current.size > 10) current.removeAt(current.lastIndex)
            _recentSearches.value = current
        }
    }
}

    private fun loadRecentSearches() {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[RecentSearchesKey]?.split(",")?.filter { it.isNotBlank() } ?: emptyList() }
                .distinctUntilChanged()
                .collect { searches ->
                    _recentSearches.value = searches
                }
        }
    }

    fun setQuery(newQuery: String) {
        query.value = newQuery
        if (newQuery.isNotEmpty()) {
            performSearch(newQuery)
        } else {
            clearResults()
        }
    }

    fun performSearch(searchQuery: String) {
        if (searchQuery.isBlank()) {
            clearResults()
            return
        }

        _isSearching.value = true
        query.value = searchQuery

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Save to recent searches
                saveRecentSearch(searchQuery)

                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)

                // Perform local database search
                val localResults = combine(
                    database.searchSongs(searchQuery),
                    database.searchAlbums(searchQuery),
                    database.searchArtists(searchQuery),
                    database.searchPlaylists(searchQuery),
                ) { songs, albums, artists, playlists ->
                    val filteredSongs = if (hideVideoSongs) songs.filter { !it.song.isVideo } else songs
                    filteredSongs + albums + artists + playlists
                }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

                localSearchResults.value = localResults.value

                // Perform YouTube search if user has internet
                val ytResults = try {
                    YouTube.search(searchQuery, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items?.filterExplicit(hideExplicit)?.filterVideoSongs(hideVideoSongs).orEmpty()
                } catch (e: Exception) {
                    emptyList()
                }

                ytSearchResults.value = ytResults

                _searchResults.value = CombinedSearchResult(
                    localItems = localSearchResults.value,
                    ytItems = ytSearchResults.value,
                    isLoading = false
                )
            } catch (e: Exception) {
                _searchResults.value = CombinedSearchResult(
                    localItems = localSearchResults.value,
                    ytItems = ytSearchResults.value,
                    isLoading = false
                )
            } finally {
                _isSearching.value = false
            }
        }
    }

    private fun saveRecentSearch(searchQuery: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSearches = context.dataStore.data.first()[RecentSearchesKey]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            val updatedSearches = (listOf(searchQuery) + currentSearches).distinct().take(10)
            context.dataStore.edit { settings ->
                settings[RecentSearchesKey] = updatedSearches.joinToString(",")
            }
        }
    }

    fun clearResults() {
        query.value = ""
        localSearchResults.value = emptyList()
        ytSearchResults.value = emptyList()
        _searchResults.value = CombinedSearchResult()
        _isSearching.value = false
    }

    fun clearRecentSearches() {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.edit { settings ->
                settings.remove(RecentSearchesKey)
            }
            _recentSearches.value = emptyList()
        }
    }

    companion object {
        private const val RecentSearchesKey = "recent_searches_tv"
    }
}
