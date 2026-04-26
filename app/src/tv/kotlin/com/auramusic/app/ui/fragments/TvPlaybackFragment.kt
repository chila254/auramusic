package com.auramusic.app.ui.fragments

import androidx.leanback.app.PlaybackSupportFragment
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.PlaybackControlsRow
import androidx.leanback.widget.PlaybackControlsRowPresenter
import com.auramusic.app.playback.PlayerConnection

class TvPlaybackFragment : PlaybackSupportFragment() {

    var playerConnection: PlayerConnection? = null

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up playback controls
        setupPlaybackControls()
    }

    private fun setupPlaybackControls() {
        val controlsRow = PlaybackControlsRow()

        // Create actions
        val playPauseAction = PlaybackControlsRow.PlayPauseAction(context)
        val skipNextAction = PlaybackControlsRow.SkipNextAction(context)
        val skipPreviousAction = PlaybackControlsRow.SkipPreviousAction(context)

        // Set up adapter
        val adapter = ArrayObjectAdapter(PlaybackControlsRowPresenter())
        controlsRow.primaryActionsAdapter = ArrayObjectAdapter().apply {
            add(playPauseAction)
            add(skipNextAction)
            add(skipPreviousAction)
        }

        adapter.add(controlsRow)
        this.adapter = adapter
    }
}