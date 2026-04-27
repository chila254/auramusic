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
    @ApplicationContext context: Context,
    database: MusicDatabase,
) : ViewModel() {
    private val query = MutableStateFlow("")
    val queryFlow = query

    private val _searchResults = MutableStateFlow<CombinedSearchResult>(CombinedSearchResult())
    val searchResults: StateFlow<CombinedSearchResult> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches

    // Local database search results
    private val localSearchResults = MutableStateFlow<List<LocalItem>>(emptyList())
    
    // YouTube search results
    private val ytSearchResults = MutableStateFlow<List<YTItem>>(emptyList())

    init {
        // Load recent searches from DataStore
        loadRecentSearches()
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
