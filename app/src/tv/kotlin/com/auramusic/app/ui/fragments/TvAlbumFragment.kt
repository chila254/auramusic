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
import com.auramusic.app.db.entities.Album
import com.auramusic.app.db.entities.Song
import com.auramusic.app.playback.PlayerConnection
import com.auramusic.app.ui.components.CardPresenter
import com.auramusic.app.viewmodels.TvBrowseViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TvAlbumFragment : DetailsSupportFragment() {

    @Inject
    lateinit var viewModel: TvBrowseViewModel

    private var album: Album? = null
    var playerConnection: PlayerConnection? = null

    fun setAlbum(album: Album) {
        this.album = album
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        album?.let { alb ->
            val selector = ClassPresenterSelector()
            val presenter = FullWidthDetailsOverviewRowPresenter(
                AlbumDetailsDescriptionPresenter()
            )
            selector.addClassPresenter(DetailsOverviewRow::class.java, presenter)

            val adapter = ArrayObjectAdapter(selector)

            // Create details overview row
            val overviewRow = DetailsOverviewRow(alb).apply {
                imageDrawable = resources.getDrawable(R.drawable.ic_album, null)
            }

            adapter.add(overviewRow)

            // Add album songs as a row
            val headerItem = HeaderItem("Songs")
            val listRowAdapter = ArrayObjectAdapter(CardPresenter().apply {
                onItemClicked = { item ->
                    handleSongClick(item as Song)
                }
            })

            val songsRow = ListRow(headerItem, listRowAdapter)
            adapter.add(songsRow)

            this.adapter = adapter

            // Load album songs
            loadAlbumSongs(alb.album.id, listRowAdapter)
        }
    }

    private fun loadAlbumSongs(albumId: String, adapter: ArrayObjectAdapter) {
        lifecycleScope.launch {
            // TODO: Load album songs from database
        }
    }

    private fun handleSongClick(song: Song) {
        playerConnection?.playQueue(
            com.auramusic.app.playback.queues.YouTubeQueue(
                endpoint = com.auramusic.innertube.models.WatchEndpoint(videoId = song.song.id)
            )
        )
    }

    private class AlbumDetailsDescriptionPresenter : androidx.leanback.widget.Presenter() {
        override fun onCreateViewHolder(parent: android.view.ViewGroup): ViewHolder {
            val view = android.widget.TextView(parent.context)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
            val textView = viewHolder.view as android.widget.TextView
            val album = item as Album
            textView.text = "${album.songCount} songs"
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder) {}
    }
}