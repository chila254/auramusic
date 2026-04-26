package com.auramusic.app.ui.fragments

import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.lifecycle.lifecycleScope
import com.auramusic.app.db.entities.*
import com.auramusic.app.playback.PlayerConnection
import com.auramusic.app.ui.components.CardPresenter
import com.auramusic.app.ui.fragments.TvAlbumFragment
import com.auramusic.app.ui.fragments.TvArtistFragment
import com.auramusic.app.ui.fragments.TvPlaylistFragment
import com.auramusic.app.viewmodels.TvBrowseViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TvLibraryFragment : BrowseSupportFragment() {

    @Inject
    lateinit var viewModel: TvBrowseViewModel

    var playerConnection: PlayerConnection? = null

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Library"

        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        adapter = rowsAdapter

        brandColor = resources.getColor(R.color.ic_launcher_background, null)

        // Add library sections
        addLibrarySections(rowsAdapter)

        // Load data
        loadLibraryData(rowsAdapter)
    }

    private fun addLibrarySections(adapter: ArrayObjectAdapter) {
        // Songs section
        val songsHeader = HeaderItem("Songs")
        val songsAdapter = ArrayObjectAdapter(CardPresenter().apply {
            onItemClicked = { item ->
                handleSongClick(item as Song)
            }
        })
        adapter.add(ListRow(songsHeader, songsAdapter))

        // Playlists section
        val playlistsHeader = HeaderItem("Playlists")
        val playlistsAdapter = ArrayObjectAdapter(CardPresenter().apply {
            onItemClicked = { item ->
                handlePlaylistClick(item as Playlist)
            }
        })
        adapter.add(ListRow(playlistsHeader, playlistsAdapter))

        // Artists section
        val artistsHeader = HeaderItem("Artists")
        val artistsAdapter = ArrayObjectAdapter(CardPresenter().apply {
            onItemClicked = { item ->
                handleArtistClick(item as Artist)
            }
        })
        adapter.add(ListRow(artistsHeader, artistsAdapter))

        // Albums section
        val albumsHeader = HeaderItem("Albums")
        val albumsAdapter = ArrayObjectAdapter(CardPresenter().apply {
            onItemClicked = { item ->
                handleAlbumClick(item as Album)
            }
        })
        adapter.add(ListRow(albumsHeader, albumsAdapter))
    }

    private fun loadLibraryData(adapter: ArrayObjectAdapter) {
        // Songs
        lifecycleScope.launch {
            viewModel.likedSongs.collectLatest { songs ->
                updateRow(adapter, "Songs", songs)
            }
        }

        // Playlists
        lifecycleScope.launch {
            viewModel.playlists.collectLatest { playlists ->
                updateRow(adapter, "Playlists", playlists)
            }
        }

        // Artists
        lifecycleScope.launch {
            viewModel.artists.collectLatest { artists ->
                updateRow(adapter, "Artists", artists)
            }
        }

        // Albums
        lifecycleScope.launch {
            viewModel.albums.collectLatest { albums ->
                updateRow(adapter, "Albums", albums)
            }
        }
    }

    private fun updateRow(adapter: ArrayObjectAdapter, title: String, items: List<Any>) {
        val row = (0 until adapter.size()).map { adapter[it] }
            .filterIsInstance<ListRow>()
            .find { it.headerItem.name == title }

        row?.adapter?.let { rowAdapter ->
            rowAdapter.clear()
            items.forEach { rowAdapter.add(it) }
        }
    }

    private fun handleSongClick(song: Song) {
        playerConnection?.playQueue(
            com.auramusic.app.playback.queues.YouTubeQueue(
                endpoint = com.auramusic.innertube.models.WatchEndpoint(videoId = song.song.id)
            )
        )
    }

    private fun handlePlaylistClick(playlist: Playlist) {
        val playlistFragment = TvPlaylistFragment()
        playlistFragment.setPlaylist(playlist)
        playlistFragment.playerConnection = playerConnection
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, playlistFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun handleArtistClick(artist: Artist) {
        val artistFragment = TvArtistFragment()
        artistFragment.setArtist(artist)
        artistFragment.playerConnection = playerConnection
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, artistFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun handleAlbumClick(album: Album) {
        val albumFragment = TvAlbumFragment()
        albumFragment.setAlbum(album)
        albumFragment.playerConnection = playerConnection
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, albumFragment)
            .addToBackStack(null)
            .commit()
    }
}