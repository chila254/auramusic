package com.auramusic.app.ui.fragments

import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.ObjectAdapter
import androidx.lifecycle.lifecycleScope
import com.auramusic.app.playback.PlayerConnection
import com.auramusic.app.ui.components.CardPresenter
import com.auramusic.app.viewmodels.TvBrowseViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TvSearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider {

    @Inject
    lateinit var viewModel: TvBrowseViewModel

    var playerConnection: PlayerConnection? = null

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        setSearchResultProvider(this)
        setOnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->
            handleItemClick(item)
        }
    }

    override fun getResultsAdapter(): ObjectAdapter {
        return ArrayObjectAdapter(ListRowPresenter())
    }

    override fun onQueryTextChange(newQuery: String): Boolean {
        if (newQuery.isNotEmpty()) {
            search(newQuery)
        }
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        search(query)
        return true
    }

    private fun search(query: String) {
        val adapter = getResultsAdapter() as ArrayObjectAdapter
        adapter.clear()

        lifecycleScope.launch {
            // Search songs
            val songResults = viewModel.searchSongs(query)
            if (songResults.isNotEmpty()) {
                val songsHeader = HeaderItem("Songs")
                val songsAdapter = ArrayObjectAdapter(CardPresenter().apply {
                    onItemClicked = { item ->
                        handleItemClick(item)
                    }
                })
                songResults.forEach { songsAdapter.add(it) }
                adapter.add(ListRow(songsHeader, songsAdapter))
            }

            // Search artists
            val artistResults = viewModel.searchArtists(query)
            if (artistResults.isNotEmpty()) {
                val artistsHeader = HeaderItem("Artists")
                val artistsAdapter = ArrayObjectAdapter(CardPresenter().apply {
                    onItemClicked = { item ->
                        handleItemClick(item)
                    }
                })
                artistResults.forEach { artistsAdapter.add(it) }
                adapter.add(ListRow(artistsHeader, artistsAdapter))
            }

            // Search albums
            val albumResults = viewModel.searchAlbums(query)
            if (albumResults.isNotEmpty()) {
                val albumsHeader = HeaderItem("Albums")
                val albumsAdapter = ArrayObjectAdapter(CardPresenter().apply {
                    onItemClicked = { item ->
                        handleItemClick(item)
                    }
                })
                albumResults.forEach { albumsAdapter.add(it) }
                adapter.add(ListRow(albumsHeader, albumsAdapter))
            }
        }
    }

    private fun handleItemClick(item: Any) {
        when (item) {
            is com.auramusic.app.db.entities.Song -> {
                // Play the song
                playerConnection?.playQueue(
                    com.auramusic.app.playback.queues.YouTubeQueue(
                        endpoint = com.auramusic.innertube.models.WatchEndpoint(videoId = item.song.id)
                    )
                )
            }
            // Handle other types
        }
    }
}