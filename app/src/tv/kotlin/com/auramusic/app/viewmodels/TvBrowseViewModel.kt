package com.auramusic.app.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auramusic.app.db.MusicDatabase
import com.auramusic.app.db.entities.*
import com.auramusic.app.playback.DownloadUtil
import com.auramusic.app.utils.SyncUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    fun syncData() {
        viewModelScope.launch {
            syncUtils.syncLikedSongs()
            syncUtils.syncLibrarySongs()
        }
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