package com.auramusic.app.voice

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class VoiceCommandHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.settingsDataStore

    companion object {
        // Player settings keys
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
        private val PURE_BLACK = booleanPreferencesKey("pure_black")
        private val SHOW_LYRICS = booleanPreferencesKey("show_lyrics")
        private val VIDEO_MODE = booleanPreferencesKey("video_mode")
    }

    suspend fun handleCommand(command: VoiceCommand): CommandResult {
        return when (command) {
            is VoiceCommand.Play -> CommandResult.Playback("Playing")
            is VoiceCommand.Pause -> CommandResult.Playback("Paused")
            is VoiceCommand.TogglePlayPause -> CommandResult.Playback("Toggled play/pause")
            is VoiceCommand.Next -> CommandResult.Playback("Skipped to next")
            is VoiceCommand.Previous -> CommandResult.Playback("Went to previous")
            is VoiceCommand.Shuffle -> CommandResult.Playback("Toggled shuffle")
            is VoiceCommand.Repeat -> CommandResult.Playback("Toggled repeat")
            
            is VoiceCommand.VolumeUp -> CommandResult.Volume("Volume increased")
            is VoiceCommand.VolumeDown -> CommandResult.Volume("Volume decreased")
            is VoiceCommand.Mute -> CommandResult.Volume("Muted")
            is VoiceCommand.Unmute -> CommandResult.Volume("Unmuted")
            
            is VoiceCommand.SetDarkMode -> {
                updateSetting(DARK_MODE, command.enabled)
                if (command.enabled) {
                    CommandResult.Settings("Dark mode enabled")
                } else {
                    CommandResult.Settings("Light mode enabled")
                }
            }
            is VoiceCommand.ToggleTheme -> {
                val current = dataStore.data.map { it[DARK_MODE] ?: false }.first()
                updateSetting(DARK_MODE, !current)
                CommandResult.Settings("Theme toggled")
            }
            
            is VoiceCommand.ShowLyrics -> {
                updateSetting(SHOW_LYRICS, true)
                CommandResult.Settings("Lyrics shown")
            }
            is VoiceCommand.HideLyrics -> {
                updateSetting(SHOW_LYRICS, false)
                CommandResult.Settings("Lyrics hidden")
            }
            
            is VoiceCommand.EnableVideo -> {
                updateSetting(VIDEO_MODE, true)
                CommandResult.Settings("Video mode enabled")
            }
            is VoiceCommand.DisableVideo -> {
                updateSetting(VIDEO_MODE, false)
                CommandResult.Settings("Video mode disabled")
            }
            
            is VoiceCommand.ToggleLike -> CommandResult.Playback("Toggled like")
            is VoiceCommand.ShowQueue -> CommandResult.Navigation("Opening queue")
            
            is VoiceCommand.OpenHome -> CommandResult.Navigation("Opening home")
            is VoiceCommand.OpenLibrary -> CommandResult.Navigation("Opening library")
            is VoiceCommand.OpenSearch -> CommandResult.Navigation("Opening search")
            is VoiceCommand.OpenSettings -> CommandResult.Navigation("Opening settings")
            
            is VoiceCommand.Search -> CommandResult.Search(command.query)
            
            is VoiceCommand.Unknown -> CommandResult.Error("Didn't understand: ${command.text}")
        }
    }

    private suspend fun updateSetting(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { prefs ->
            prefs[key] = value
        }
    }
}

sealed class CommandResult {
    data class Playback(val message: String) : CommandResult()
    data class Volume(val message: String) : CommandResult()
    data class Settings(val message: String) : CommandResult()
    data class Navigation(val message: String) : CommandResult()
    data class Search(val query: String) : CommandResult()
    data class Error(val message: String) : CommandResult()
}
