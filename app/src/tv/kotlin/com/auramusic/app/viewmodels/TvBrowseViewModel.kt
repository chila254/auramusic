package com.auramusic.app.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auramusic.app.db.MusicDatabase
import com.auramusic.app.db.entities.LocalItem
import com.auramusic.app.db.entities.Song
import com.auramusic.app.db.entities.SongSortType
import com.auramusic.app.db.entities.YTItem
import com.auramusic.app.playback.DownloadUtil
import com.auramusic.app.utils.SyncUtils
import com.auramusic.app.viewmodels.CommunityPlaylistItem
import com.auramusic.app.viewmodels.SimilarRecommendation
import com.auramusic.app.viewmodels.SpeedDialItem
import com.auramusic.innertube.models.PlaylistItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TvBrowseViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    private val downloadUtil: DownloadUtil,
    private val syncUtils: SyncUtils,
) : ViewModel() {

    // Home screen personalized content
    val quickPicks = MutableStateFlow<List<Song>?>(null)
    val forgottenFavorites = MutableStateFlow<List<Song>?>(null)
    val keepListening = MutableStateFlow<List<LocalItem>?>(null)
    val similarRecommendations = MutableStateFlow<List<SimilarRecommendation>?>(null)
    val accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
    val communityPlaylists = MutableStateFlow<List<CommunityPlaylistItem>?>(null)

    val pinnedSpeedDialItems: StateFlow<List<SpeedDialItem>> =
        database.speedDialDao.getAll()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val speedDialItems: StateFlow<List<YTItem>> =
        combine(
            database.speedDialDao.getAll(),
            keepListening,
            quickPicks
        ) { pinned, keepListening, quick ->
            val pinnedItems = pinned.map { it.toYTItem() }
            val filled = pinnedItems.toMutableList()
            val targetSize = 12 // Smaller for TV

            // Add keep listening items
            keepListening?.take(targetSize - filled.size)?.let { filled.addAll(it) }

            // Add quick picks if still need more
            if (filled.size < targetSize) {
                quick?.take(targetSize - filled.size)?.let { filled.addAll(it) }
            }

            filled.take(targetSize)
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Library content
    // Recently played songs (by play time)
    val recentlyPlayed: StateFlow<List<Song>> = database.songsByPlayTimeAsc()
        .map { songs -> songs.reversed().take(20) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Liked songs
    val likedSongs: StateFlow<List<Song>> = database.likedSongs(SongSortType.CREATE_DATE, true)
        .map { songs -> songs.take(20) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Downloaded songs
    val downloadedSongs: StateFlow<List<Song>> = database.downloadedSongs(SongSortType.CREATE_DATE, true)
        .map { songs -> songs.take(20) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // User playlists
    val playlists: StateFlow<List<Playlist>> = database.playlists(PlaylistSortType.CREATE_DATE, true)
        .map { playlists -> playlists.take(20) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Artists
    val artists: StateFlow<List<Artist>> = database.artists(ArtistSortType.CREATE_DATE, true)
        .map { artists -> artists.take(20) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Albums
    val albums: StateFlow<List<Album>> = database.albums(AlbumSortType.CREATE_DATE, true)
        .map { albums -> albums.take(20) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadHomeScreenData()
    }

    private fun loadHomeScreenData() {
        viewModelScope.launch {
            try {
                // Load quick picks (AI recommendations)
                database.quickPicks(System.currentTimeMillis()).collect { songs ->
                    quickPicks.value = songs.take(10)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }

        viewModelScope.launch {
            try {
                // Load forgotten favorites
                database.forgottenFavorites(System.currentTimeMillis()).collect { songs ->
                    forgottenFavorites.value = songs.take(10)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }

        // Load other home data as needed
    }

    fun syncData() {
        viewModelScope.launch {
            syncUtils.syncLikedSongs()
            syncUtils.syncLibrarySongs()
        }
        loadHomeScreenData()
    }

    suspend fun searchSongs(query: String): List<Song> {
        // TODO: Implement proper search
        return likedSongs.value.filter {
            it.song.title.contains(query, ignoreCase = true) ||
            it.artists.any { artist -> artist.name.contains(query, ignoreCase = true) }
        }
    }

    suspend fun searchArtists(query: String): List<Artist> {
        // TODO: Implement proper search
        return artists.value.filter {
            it.artist.name.contains(query, ignoreCase = true)
        }
    }

    suspend fun searchAlbums(query: String): List<Album> {
        // TODO: Implement proper search
        return albums.value.filter {
            it.album.title.contains(query, ignoreCase = true) ||
            it.artists.any { artist -> artist.name.contains(query, ignoreCase = true) }
        }
    }
}