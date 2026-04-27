/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.auramusic.app.constants.HideExplicitKey
import com.auramusic.app.constants.HideVideoSongsKey
import com.auramusic.app.db.MusicDatabase
import com.auramusic.app.db.entities.Album
import com.auramusic.app.db.entities.Artist
import com.auramusic.app.db.entities.LocalItem
import com.auramusic.app.db.entities.Playlist
import com.auramusic.app.db.entities.Song
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.YTItem
import com.auramusic.innertube.models.filterExplicit
import com.auramusic.innertube.models.filterVideoSongs
import com.auramusic.app.utils.dataStore
import com.auramusic.app.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TvSearchViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    private val database: MusicDatabase,
) : ViewModel() {
    val query = MutableStateFlow("")

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchResults = MutableStateFlow<CombinedSearchResult>(CombinedSearchResult())
    val searchResults: StateFlow<CombinedSearchResult> = _searchResults

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches

    val localSearchResults = combine(
        query,
        context.dataStore.data.map { it[HideVideoSongsKey] ?: false }.distinctUntilChanged()
    ) { query, hideVideoSongs ->
        Pair(query, hideVideoSongs)
    }.flatMapLatest { (query, hideVideoSongs) ->
        if (query.isBlank()) {
            flowOf(emptyList())
        } else {
            combine(
                database.searchSongs(query),
                database.searchAlbums(query),
                database.searchArtists(query),
                database.searchPlaylists(query),
            ) { songs, albums, artists, playlists ->
                val filteredSongs = if (hideVideoSongs) songs.filter { !it.song.isVideo } else songs
                (filteredSongs + albums + artists + playlists).take(20)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val ytSearchResults = MutableStateFlow<List<YTItem>>(emptyList())

    init {
        loadRecentSearches()
    }

    fun updateQuery(newQuery: String) {
        query.value = newQuery
        if (newQuery.isNotBlank()) {
            performSearch(newQuery)
        } else {
            clearResults()
        }
    }

    private fun performSearch(searchQuery: String) {
        if (searchQuery.isBlank()) {
            clearResults()
            return
        }

        _isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Save to recent searches
                saveRecentSearch(searchQuery)

                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)

                // YouTube search
                val ytResults = try {
                    YouTube.search(searchQuery, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items?.filterExplicit(hideExplicit)?.filterVideoSongs(hideVideoSongs).orEmpty()
                } catch (e: Exception) {
                    emptyList()
                }

                ytSearchResults.value = ytResults.take(20)

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
                _isLoading.value = false
            }
        }
    }

    private fun clearResults() {
        _searchResults.value = CombinedSearchResult()
    }

    fun addToRecentSearches(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _recentSearches.value.toMutableList()
            current.remove(query) // Remove if exists
            current.add(0, query) // Add to front
            if (current.size > 10) current.removeAt(current.lastIndex)
            _recentSearches.value = current
            saveRecentSearch(query)
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

    private fun saveRecentSearch(searchQuery: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSearches = context.dataStore.data.first()[RecentSearchesKey]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            val updatedSearches = (listOf(searchQuery) + currentSearches).distinct().take(10)
            context.dataStore.edit { settings ->
                settings[RecentSearchesKey] = updatedSearches.joinToString(",")
            }
        }
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
        private val RecentSearchesKey = stringPreferencesKey("recent_searches_tv")
    }
}

data class CombinedSearchResult(
    val localItems: List<LocalItem> = emptyList(),
    val ytItems: List<YTItem> = emptyList(),
    val isLoading: Boolean = false,
)