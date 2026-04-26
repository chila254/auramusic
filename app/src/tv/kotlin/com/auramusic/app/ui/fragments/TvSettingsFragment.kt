package com.auramusic.app.ui.fragments

import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.VerticalGridPresenter
import com.auramusic.app.ui.components.CardPresenter

class TvSettingsFragment : VerticalGridSupportFragment() {

    data class SettingItem(val title: String, val description: String, val iconRes: Int)

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        val gridPresenter = VerticalGridPresenter()
        gridPresenter.numberOfColumns = 2
        setGridPresenter(gridPresenter)

        title = "Settings"

        val adapter = ArrayObjectAdapter(CardPresenter().apply {
            onItemClicked = { item ->
                handleSettingClick(item as SettingItem)
            }
        })

        // Add settings items
        adapter.add(SettingItem("Appearance", "Theme and display settings", R.drawable.ic_launcher_background))
        adapter.add(SettingItem("Audio", "Playback and audio settings", R.drawable.ic_music_note))
        adapter.add(SettingItem("Library", "Sync and library management", R.drawable.ic_playlist))
        adapter.add(SettingItem("About", "App information and updates", R.drawable.ic_launcher_background))

        this.adapter = adapter
    }

    private fun handleSettingClick(item: SettingItem) {
        // TODO: Navigate to specific settings screens
        when (item.title) {
            "Appearance" -> {
                // Open appearance settings
            }
            "Audio" -> {
                // Open audio settings
            }
            "Library" -> {
                // Open library settings
            }
            "About" -> {
                // Open about screen
            }
        }
    }
}