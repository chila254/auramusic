package com.auramusic.app.ui.screens.settings

import android.speech.tts.Voice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auramusic.app.voice.VoiceFeedbackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceFeedbackSettingsViewModel @Inject constructor(
    private val voiceFeedbackManager: VoiceFeedbackManager,
) : ViewModel() {

    init {
        // Ensure TTS is initialized
        voiceFeedbackManager.initialize()
    }

    val isEnabled = voiceFeedbackManager.isEnabled()
    val availableVoices = voiceFeedbackManager.getAvailableVoices()
    val selectedVoice = voiceFeedbackManager.getCurrentVoice()
    val pitch = voiceFeedbackManager.getPitch()
    val speechRate = voiceFeedbackManager.getSpeechRate()

    fun setEnabled(enabled: Boolean) {
        voiceFeedbackManager.setEnabled(enabled)
    }

    fun setVoice(voice: android.speech.tts.Voice) {
        voiceFeedbackManager.setVoice(voice)
    }

    fun setPitch(pitch: Float) {
        voiceFeedbackManager.setPitch(pitch)
    }

    fun setSpeechRate(rate: Float) {
        voiceFeedbackManager.setSpeechRate(rate)
    }
}
