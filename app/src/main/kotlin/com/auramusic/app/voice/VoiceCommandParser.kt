package com.auramusic.app.voice

object VoiceCommandParser {

    fun parseCommand(text: String): VoiceCommand {
        val lowerText = text.lowercase().trim()
        
        // Search commands
        if (lowerText.contains("search") || lowerText.contains("find") || lowerText.contains("play")) {
            val query = lowerText
                .replace("search for", "")
                .replace("search", "")
                .replace("find", "")
                .replace("play", "")
                .replace("play song", "")
                .replace("play music", "")
                .trim()
            if (query.isNotEmpty()) {
                return VoiceCommand.Search(query)
            }
        }

        // Playback commands
        return when {
            // Play commands
            lowerText.contains("play") || lowerText.contains("start") || lowerText.contains("resume") -> {
                VoiceCommand.Play
            }
            
            // Pause commands
            lowerText.contains("pause") || lowerText.contains("stop") -> {
                VoiceCommand.Pause
            }
            
            // Toggle play/pause
            lowerText.contains("toggle") && lowerText.contains("play") -> {
                VoiceCommand.TogglePlayPause
            }
            
            // Next commands
            lowerText.contains("next") || lowerText.contains("skip") || lowerText.contains("forward") -> {
                VoiceCommand.Next
            }
            
            // Previous commands  
            lowerText.contains("previous") || lowerText.contains("back") || lowerText.contains("last") -> {
                VoiceCommand.Previous
            }
            
            // Shuffle commands
            lowerText.contains("shuffle") -> {
                VoiceCommand.Shuffle
            }
            
            // Repeat commands
            lowerText.contains("repeat") || lowerText.contains("loop") -> {
                VoiceCommand.Repeat
            }
            
            // Volume commands
            lowerText.contains("volume up") || lowerText.contains("louder") || lowerText.contains("increase volume") -> {
                VoiceCommand.VolumeUp
            }
            lowerText.contains("volume down") || lowerText.contains("quieter") || lowerText.contains("decrease volume") -> {
                VoiceCommand.VolumeDown
            }
            lowerText.contains("mute") || lowerText.contains("silent") -> {
                VoiceCommand.Mute
            }
            lowerText.contains("unmute") || lowerText.contains("unmute") -> {
                VoiceCommand.Unmute
            }
            
            // Settings commands
            lowerText.contains("dark mode") || lowerText.contains("dark theme") -> {
                VoiceCommand.SetDarkMode(true)
            }
            lowerText.contains("light mode") || lowerText.contains("light theme") -> {
                VoiceCommand.SetDarkMode(false)
            }
            lowerText.contains("toggle theme") || lowerText.contains("switch theme") -> {
                VoiceCommand.ToggleTheme
            }
            
            // Lyrics commands
            lowerText.contains("show lyrics") || lowerText.contains("lyrics on") -> {
                VoiceCommand.ShowLyrics
            }
            lowerText.contains("hide lyrics") || lowerText.contains("lyrics off") -> {
                VoiceCommand.HideLyrics
            }
            
            // Video commands
            lowerText.contains("video on") || lowerText.contains("show video") -> {
                VoiceCommand.EnableVideo
            }
            lowerText.contains("video off") || lowerText.contains("hide video") -> {
                VoiceCommand.DisableVideo
            }
            
            // Like commands
            lowerText.contains("like") || lowerText.contains("favorite") || lowerText.contains("love") -> {
                VoiceCommand.ToggleLike
            }
            
            // Queue commands
            lowerText.contains("queue") || lowerText.contains("playlist") -> {
                VoiceCommand.ShowQueue
            }
            
            // Open commands
            lowerText.contains("open home") -> {
                VoiceCommand.OpenHome
            }
            lowerText.contains("open library") -> {
                VoiceCommand.OpenLibrary
            }
            lowerText.contains("open search") -> {
                VoiceCommand.OpenSearch
            }
            lowerText.contains("open settings") -> {
                VoiceCommand.OpenSettings
            }
            
            // Unknown command
            else -> VoiceCommand.Unknown(text)
        }
    }
}

sealed class VoiceCommand {
    // Playback
    data object Play : VoiceCommand()
    data object Pause : VoiceCommand()
    data object TogglePlayPause : VoiceCommand()
    data object Next : VoiceCommand()
    data object Previous : VoiceCommand()
    data object Shuffle : VoiceCommand()
    data object Repeat : VoiceCommand()
    
    // Volume
    data object VolumeUp : VoiceCommand()
    data object VolumeDown : VoiceCommand()
    data object Mute : VoiceCommand()
    data object Unmute : VoiceCommand()
    
    // Search
    data class Search(val query: String) : VoiceCommand()
    
    // Settings
    data class SetDarkMode(val enabled: Boolean) : VoiceCommand()
    data object ToggleTheme : VoiceCommand()
    
    // Lyrics
    data object ShowLyrics : VoiceCommand()
    data object HideLyrics : VoiceCommand()
    
    // Video
    data object EnableVideo : VoiceCommand()
    data object DisableVideo : VoiceCommand()
    
    // Media
    data object ToggleLike : VoiceCommand()
    data object ShowQueue : VoiceCommand()
    
    // Navigation
    data object OpenHome : VoiceCommand()
    data object OpenLibrary : VoiceCommand()
    data object OpenSearch : VoiceCommand()
    data object OpenSettings : VoiceCommand()
    
    // Unknown
    data class Unknown(val text: String) : VoiceCommand()
}
