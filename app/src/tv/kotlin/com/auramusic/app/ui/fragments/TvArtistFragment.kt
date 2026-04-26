package com.auramusic.app.ui.fragments

import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.DetailsOverviewRow
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.lifecycle.lifecycleScope
import coil3.load
import com.auramusic.app.db.entities.Artist
import com.auramusic.app.db.entities.Song
import com.auramusic.app.playback.PlayerConnection
import com.auramusic.app.ui.components.CardPresenter
import com.auramusic.app.ui.fragments.TvAlbumFragment
import com.auramusic.app.viewmodels.TvBrowseViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TvArtistFragment : DetailsSupportFragment() {

    @Inject
    lateinit var viewModel: TvBrowseViewModel

    private var artist: Artist? = null
    var playerConnection: PlayerConnection? = null

    fun setArtist(artist: Artist) {
        this.artist = artist
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        artist?.let { art ->
            val selector = ClassPresenterSelector()
            val presenter = FullWidthDetailsOverviewRowPresenter(
                ArtistDetailsDescriptionPresenter()
            )
            selector.addClassPresenter(DetailsOverviewRow::class.java, presenter)

            val adapter = ArrayObjectAdapter(selector)

            // Create details overview row
            val overviewRow = DetailsOverviewRow(art).apply {
                imageDrawable = resources.getDrawable(R.drawable.ic_person, null)
            }

            adapter.add(overviewRow)

            // Add artist songs as a row
            val headerItem = HeaderItem("Songs")
            val listRowAdapter = ArrayObjectAdapter(CardPresenter().apply {
                onItemClicked = { item ->
                    handleSongClick(item as Song)
                }
            })

            val songsRow = ListRow(headerItem, listRowAdapter)
            adapter.add(songsRow)

            // Add albums row
            val albumsHeader = HeaderItem("Albums")
            val albumsAdapter = ArrayObjectAdapter(CardPresenter().apply {
                onItemClicked = { item ->
                    handleAlbumClick(item as com.auramusic.app.db.entities.Album)
                }
            })

            val albumsRow = ListRow(albumsHeader, albumsAdapter)
            adapter.add(albumsRow)

            this.adapter = adapter

            // Load artist data
            loadArtistData(art.artist.id, listRowAdapter, albumsAdapter)
        }
    }

    private fun loadArtistData(artistId: String, songsAdapter: ArrayObjectAdapter, albumsAdapter: ArrayObjectAdapter) {
        lifecycleScope.launch {
            // TODO: Load artist songs and albums
        }
    }

    private fun handleSongClick(song: Song) {
        playerConnection?.playQueue(
            com.auramusic.app.playback.queues.YouTubeQueue(
                endpoint = com.auramusic.innertube.models.WatchEndpoint(videoId = song.song.id)
            )
        )
    }

    private fun handleAlbumClick(album: com.auramusic.app.db.entities.Album) {
        val albumFragment = TvAlbumFragment()
        albumFragment.setAlbum(album)
        albumFragment.playerConnection = playerConnection
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, albumFragment)
            .addToBackStack(null)
            .commit()
    }

    private class ArtistDetailsDescriptionPresenter : androidx.leanback.widget.Presenter() {
        override fun onCreateViewHolder(parent: android.view.ViewGroup): ViewHolder {
            val view = android.widget.TextView(parent.context)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
            val textView = viewHolder.view as android.widget.TextView
            val artist = item as Artist
            textView.text = "${artist.songCount} songs"
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder) {}
    }
}