/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.AlbumItem
import com.auramusic.innertube.models.SongItem
import com.auramusic.innertube.models.YTItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NewReleasesViewModel @Inject constructor() : ViewModel() {
    private val _albumReleases = MutableStateFlow<List<AlbumItem>>(emptyList())
    val albumReleases = _albumReleases.asStateFlow()

    private val _songReleases = MutableStateFlow<List<SongItem>>(emptyList())
    val songReleases = _songReleases.asStateFlow()

    private val _videoReleases = MutableStateFlow<List<YTItem>>(emptyList())
    val videoReleases = _videoReleases.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        loadNewReleases()
    }

    fun loadNewReleases() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            try {
                // Browse new releases using YouTube Music's browse endpoint
                YouTube.browse("FEmusic_new_releases", null)
                    .onSuccess { browseResult ->
                        val albums = mutableListOf<AlbumItem>()
                        val songs = mutableListOf<SongItem>()
                        val videos = mutableListOf<YTItem>()

                        // Parse the browse result and separate by type
                        browseResult.items.forEach { section ->
                            section.items.forEach { item ->
                                when (item) {
                                    is AlbumItem -> albums.add(item)
                                    is SongItem -> songs.add(item)
                                    else -> videos.add(item)
                                }
                            }
                        }

                        _albumReleases.value = albums
                        _songReleases.value = songs
                        _videoReleases.value = videos
                    }
                    .onFailure { throwable ->
                        _error.value = throwable.message ?: "Unknown error"
                        Timber.e(throwable, "Failed to load new releases")
                    }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
                Timber.e(e, "Error loading new releases")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
