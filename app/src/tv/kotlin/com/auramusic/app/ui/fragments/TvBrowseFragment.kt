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
import com.auramusic.app.ui.fragments.TvLibraryFragment
import com.auramusic.app.ui.fragments.TvSearchFragment
import com.auramusic.app.ui.fragments.TvSettingsFragment
import com.auramusic.app.viewmodels.TvBrowseViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TvBrowseFragment : BrowseSupportFragment() {

    @Inject
    lateinit var viewModel: TvBrowseViewModel

    var playerConnection: PlayerConnection? = null

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        title = "AuraMusic"

        // Set up the adapter
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        adapter = rowsAdapter

        // Set up branding
        brandColor = resources.getColor(R.color.ic_launcher_background, null)

        // Enable search
        setOnSearchClickedListener {
            navigateToSearch()
        }

        // Handle back navigation
        setOnItemViewSelectedListener { _, item, _, _ ->
            // Update selected item if needed
        }

        // Add settings to the first row or as an action
        // For now, we'll add a settings item to the adapter

        // Add navigation rows
        addNavigationRows(rowsAdapter)

        // Load data
        loadData()
    }

    private fun setupCardClickHandler(rowsAdapter: ArrayObjectAdapter) {
        // We need to set the click handler on each card presenter
        // This is a bit tricky since we create presenters in updateRow
        // For now, we'll handle clicks when updating rows
    }

    private fun loadData() {
        lifecycleScope.launch {
            // Recently Played
            viewModel.recentlyPlayed.collectLatest { songs ->
                updateRow("Recently Played", songs)
            }
        }

        lifecycleScope.launch {
            // Liked Songs
            viewModel.likedSongs.collectLatest { songs ->
                updateRow("Liked Songs", songs)
            }
        }

        lifecycleScope.launch {
            // Downloaded Songs
            viewModel.downloadedSongs.collectLatest { songs ->
                updateRow("Downloaded", songs)
            }
        }

        lifecycleScope.launch {
            // Playlists
            viewModel.playlists.collectLatest { playlists ->
                updateRow("Playlists", playlists)
            }
        }

        lifecycleScope.launch {
            // Artists
            viewModel.artists.collectLatest { artists ->
                updateRow("Artists", artists)
            }
        }

        lifecycleScope.launch {
            // Albums
            viewModel.albums.collectLatest { albums ->
                updateRow("Albums", albums)
            }
        }
    }

    private fun addNavigationRows(adapter: ArrayObjectAdapter) {
        // Add a navigation/library row
        val navHeader = HeaderItem("Browse")
        val navAdapter = ArrayObjectAdapter(CardPresenter().apply {
            onItemClicked = { item ->
                handleNavigationClick(item as String)
            }
        })

        // Add navigation items
        navAdapter.add("Library")
        navAdapter.add("Settings")

        val navRow = ListRow(navHeader, navAdapter)
        adapter.add(navRow)
    }

    private fun updateRow(title: String, items: List<Any>) {
        val adapter = adapter as? ArrayObjectAdapter ?: return

        // Find existing row or create new one
        val existingRow = (0 until adapter.size()).map { adapter[it] }
            .filterIsInstance<ListRow>()
            .find { it.headerItem.name == title }

        val cardAdapter = existingRow?.adapter as? ArrayObjectAdapter ?: ArrayObjectAdapter(CardPresenter().apply {
            onItemClicked = { item ->
                handleItemClick(item)
            }
        })

        // Clear and add new items
        cardAdapter.clear()
        items.forEach { cardAdapter.add(it) }

        if (existingRow == null) {
            // Add new row
            val headerItem = HeaderItem(title)
            val row = ListRow(headerItem, cardAdapter)
            adapter.add(row)
        }
    }

    private fun navigateToSearch() {
        val searchFragment = TvSearchFragment()
        searchFragment.playerConnection = playerConnection
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, searchFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToLibrary() {
        val libraryFragment = TvLibraryFragment()
        libraryFragment.playerConnection = playerConnection
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, libraryFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToSettings() {
        val settingsFragment = TvSettingsFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, settingsFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun handleNavigationClick(item: String) {
        when (item) {
            "Library" -> navigateToLibrary()
            "Settings" -> navigateToSettings()
        }
    }

    private fun handleItemClick(item: Any) {
        when (item) {
            is Song -> {
                // Play the song
                playerConnection?.playQueue(
                    com.auramusic.app.playback.queues.YouTubeQueue(
                        endpoint = com.auramusic.innertube.models.WatchEndpoint(videoId = item.song.id)
                    )
                )
            }
            is Playlist -> {
                // Navigate to playlist details
                val playlistFragment = TvPlaylistFragment()
                playlistFragment.setPlaylist(item)
                playlistFragment.playerConnection = playerConnection
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(android.R.id.content, playlistFragment)
                    .addToBackStack(null)
                    .commit()
            }
            is Artist -> {
                // Navigate to artist details
                val artistFragment = TvArtistFragment()
                artistFragment.setArtist(item)
                artistFragment.playerConnection = playerConnection
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(android.R.id.content, artistFragment)
                    .addToBackStack(null)
                    .commit()
            }
            is Album -> {
                // Navigate to album details
                val albumFragment = TvAlbumFragment()
                albumFragment.setAlbum(item)
                albumFragment.playerConnection = playerConnection
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(android.R.id.content, albumFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
}

    private fun createAdapter(): ArrayObjectAdapter {
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

        // Home/Quick Access row
        val headerItem1 = HeaderItem("Quick Access")
        val listRowAdapter1 = ArrayObjectAdapter(CardPresenter())
        // TODO: Add quick access items like continue playing, recent songs, etc.
        val row1 = ListRow(headerItem1, listRowAdapter1)
        rowsAdapter.add(row1)

        // Recently Played row
        val headerItem2 = HeaderItem("Recently Played")
        val listRowAdapter2 = ArrayObjectAdapter(CardPresenter())
        // TODO: Load recently played songs
        val row2 = ListRow(headerItem2, listRowAdapter2)
        rowsAdapter.add(row2)

        // Playlists row
        val headerItem3 = HeaderItem("Playlists")
        val listRowAdapter3 = ArrayObjectAdapter(CardPresenter())
        // TODO: Load user playlists
        val row3 = ListRow(headerItem3, listRowAdapter3)
        rowsAdapter.add(row3)

        // Artists row
        val headerItem4 = HeaderItem("Artists")
        val listRowAdapter4 = ArrayObjectAdapter(CardPresenter())
        // TODO: Load artists
        val row4 = ListRow(headerItem4, listRowAdapter4)
        rowsAdapter.add(row4)

        // Albums row
        val headerItem5 = HeaderItem("Albums")
        val listRowAdapter5 = ArrayObjectAdapter(CardPresenter())
        // TODO: Load albums
        val row5 = ListRow(headerItem5, listRowAdapter5)
        rowsAdapter.add(row5)

        return rowsAdapter
    }
}