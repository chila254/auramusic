package com.auramusic.app.ui.fragments

import androidx.leanback.app.PlaybackSupportFragment
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.PlaybackControlsRow
import androidx.leanback.widget.PlaybackControlsRowPresenter
import com.auramusic.app.playback.PlayerConnection
import com.auramusic.app.playback.queues.YouTubeQueue
import com.auramusic.innertube.models.WatchEndpoint

class TvPlaybackOverlayFragment : PlaybackSupportFragment() {

    var playerConnection: PlayerConnection? = null
    private var currentSong: com.auramusic.app.db.entities.Song? = null

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        setupPlaybackControls()
        observePlayerState()
    }

    private fun setupPlaybackControls() {
        val controlsRow = PlaybackControlsRow(currentSong).apply {
            // Set up actions
            val playPauseAction = PlaybackControlsRow.PlayPauseAction(context).apply {
                index = PlaybackControlsRow.PlayPauseAction.INDEX_PLAY_PAUSE
            }
            val skipNextAction = PlaybackControlsRow.SkipNextAction(context)
            val skipPreviousAction = PlaybackControlsRow.SkipPreviousAction(context)
            val repeatAction = PlaybackControlsRow.RepeatAction(context)
            val shuffleAction = PlaybackControlsRow.ShuffleAction(context)

            primaryActionsAdapter = ArrayObjectAdapter().apply {
                add(playPauseAction)
                add(skipNextAction)
                add(skipPreviousAction)
                add(repeatAction)
                add(shuffleAction)
            }
        }

        val adapter = ArrayObjectAdapter(PlaybackControlsRowPresenter())
        adapter.add(controlsRow)
        this.adapter = adapter
    }

    private fun observePlayerState() {
        // TODO: Observe player state changes
    }

    fun playSong(song: com.auramusic.app.db.entities.Song) {
        currentSong = song
        playerConnection?.playQueue(
            YouTubeQueue(WatchEndpoint(videoId = song.song.id))
        )
        // Update UI
        adapter?.notifyItemRangeChanged(0, adapter.size())
    }

    override fun onActionClicked(action: Action) {
        super.onActionClicked(action)
        when (action) {
            is PlaybackControlsRow.PlayPauseAction -> {
                playerConnection?.player?.let { player ->
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        player.play()
                    }
                }
            }
            is PlaybackControlsRow.SkipNextAction -> {
                playerConnection?.player?.seekToNextMediaItem()
            }
            is PlaybackControlsRow.SkipPreviousAction -> {
                playerConnection?.player?.seekToPreviousMediaItem()
            }
        }
    }
}