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
import com.auramusic.app.db.entities.Playlist
import com.auramusic.app.db.entities.Song
import com.auramusic.app.playback.PlayerConnection
import com.auramusic.app.ui.components.CardPresenter
import com.auramusic.app.viewmodels.TvBrowseViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TvPlaylistFragment : DetailsSupportFragment() {

    @Inject
    lateinit var viewModel: TvBrowseViewModel

    private var playlist: Playlist? = null
    var playerConnection: PlayerConnection? = null

    fun setPlaylist(playlist: Playlist) {
        this.playlist = playlist
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        playlist?.let { pl ->
            val selector = ClassPresenterSelector()
            val presenter = FullWidthDetailsOverviewRowPresenter(
                DetailsDescriptionPresenter()
            )
            selector.addClassPresenter(DetailsOverviewRow::class.java, presenter)

            val adapter = ArrayObjectAdapter(selector)

            // Create details overview row
            val overviewRow = DetailsOverviewRow(pl).apply {
                imageDrawable = resources.getDrawable(R.drawable.ic_playlist, null)
            }

            adapter.add(overviewRow)

            // Add playlist songs as a row
            val headerItem = HeaderItem("Songs")
            val listRowAdapter = ArrayObjectAdapter(CardPresenter().apply {
                onItemClicked = { item ->
                    handleSongClick(item as Song)
                }
            })

            val songsRow = ListRow(headerItem, listRowAdapter)
            adapter.add(songsRow)

            this.adapter = adapter

            // Load playlist songs
            loadPlaylistSongs(pl.playlist.id, listRowAdapter)
        }
    }

    private fun loadPlaylistSongs(playlistId: String, adapter: ArrayObjectAdapter) {
        lifecycleScope.launch {
            // TODO: Get playlist songs from database
            // For now, show empty
        }
    }

    private fun handleSongClick(song: Song) {
        playerConnection?.playQueue(
            com.auramusic.app.playback.queues.YouTubeQueue(
                endpoint = com.auramusic.innertube.models.WatchEndpoint(videoId = song.song.id)
            )
        )
    }

    private class DetailsDescriptionPresenter : androidx.leanback.widget.Presenter() {
        override fun onCreateViewHolder(parent: android.view.ViewGroup): ViewHolder {
            val view = android.widget.TextView(parent.context)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
            val textView = viewHolder.view as android.widget.TextView
            val playlist = item as Playlist
            textView.text = "${playlist.songCount} songs"
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder) {}
    }
}