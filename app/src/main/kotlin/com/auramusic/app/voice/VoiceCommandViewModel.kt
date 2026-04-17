package com.auramusic.app.voice

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auramusic.app.constants.EnableVoiceCommandsKey
import com.auramusic.app.constants.EnableVoiceWakeWordKey
import com.auramusic.app.constants.VoiceWakeWordKey
import com.auramusic.app.playback.PlayerConnection
import com.auramusic.app.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceCommandViewModel @Inject constructor(
    private val voiceCommandManager: VoiceCommandManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private var playerConnectionProvider: (() -> PlayerConnection?)? = null
    private var onSearchCallback: ((String) -> Unit)? = null
    private var onNavigateCallback: ((String) -> Unit)? = null

    private var isAppInForeground = false
    private var hasMicPermission = false
    private var voiceEnabled = false
    private var wakeWordEnabled = false
    private var customWakeWord = "aura"

    private var restartJob: Job? = null
    private var feedbackJob: Job? = null
    private var restartDelay = 300L
    private val maxRestartDelay = 2000L

    init {
        observePreferences()
        collectRecognitionEvents()
    }

    fun bindHandlers(
        playerConnectionProvider: () -> PlayerConnection?,
        onSearch: (String) -> Unit,
        onNavigate: (String) -> Unit,
    ) {
        this.playerConnectionProvider = playerConnectionProvider
        this.onSearchCallback = onSearch
        this.onNavigateCallback = onNavigate
    }

    fun onAppForeground() {
        isAppInForeground = true
        maybeStartWakeWordListening()
    }

    fun onAppBackground() {
        isAppInForeground = false
        voiceCommandManager.stopListening()
        restartJob?.cancel()
        _uiState.update { VoiceUiState() }
    }

    fun onMicPermissionChanged(granted: Boolean) {
        hasMicPermission = granted
        if (granted) maybeStartWakeWordListening()
    }

    fun startManualSession() {
        feedbackJob?.cancel()
        restartJob?.cancel()
        _uiState.update {
            VoiceUiState(
                isVisible = true,
                mode = VoiceMode.MANUAL,
                phase = VoicePhase.LISTENING,
            )
        }
        voiceCommandManager.startListening(RecognitionMode.COMMAND)
    }

    fun dismissOverlay() {
        feedbackJob?.cancel()
        restartJob?.cancel()
        voiceCommandManager.stopListening()
        _uiState.update { VoiceUiState() }
        maybeStartWakeWordListening()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            context.dataStore.data
                .map { prefs ->
                    Triple(
                        prefs[EnableVoiceCommandsKey] ?: false,
                        prefs[EnableVoiceWakeWordKey] ?: false,
                        prefs[VoiceWakeWordKey] ?: "aura"
                    )
                }
                .distinctUntilChanged()
                .collect { (enabled, wakeWord, customWake) ->
                    voiceEnabled = enabled
                    wakeWordEnabled = wakeWord
                    customWakeWord = customWake
                    if (enabled && wakeWord && isAppInForeground && hasMicPermission) {
                        maybeStartWakeWordListening()
                    } else if (!enabled || !wakeWord) {
                        if (_uiState.value.mode == VoiceMode.WAKE_WORD) {
                            voiceCommandManager.stopListening()
                        }
                    }
                }
        }
    }

    private fun collectRecognitionEvents() {
        viewModelScope.launch {
            voiceCommandManager.events.collect { event ->
                when (event) {
                    is VoiceRecognitionEvent.Ready -> {
                        if (_uiState.value.isVisible) {
                            _uiState.update { it.copy(phase = VoicePhase.LISTENING) }
                        }
                        restartDelay = 300L
                    }

                    is VoiceRecognitionEvent.Rms -> {
                        if (_uiState.value.isVisible) {
                            _uiState.update { it.copy(amplitude = event.value) }
                        }
                    }

                    is VoiceRecognitionEvent.PartialText -> {
                        val currentMode = _uiState.value.mode
                        if (currentMode == VoiceMode.WAKE_WORD) {
                            // Check for wake word in partial results for faster detection
                            val match = VoiceCommandParser.extractWakeWord(event.text, customWakeWord)
                            if (match.detected) {
                                handleWakeWordDetected(match.remainingText)
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    recognizedText = event.text,
                                    phase = VoicePhase.LISTENING
                                )
                            }
                        }
                    }

                    is VoiceRecognitionEvent.FinalText -> {
                        handleFinalText(event.text)
                    }

                    is VoiceRecognitionEvent.EndOfSpeech -> {
                        if (_uiState.value.isVisible) {
                            _uiState.update { it.copy(phase = VoicePhase.PROCESSING) }
                        }
                    }

                    is VoiceRecognitionEvent.Error -> {
                        handleError(event)
                    }
                }
            }
        }
    }

    private fun handleWakeWordDetected(remainingText: String) {
        voiceCommandManager.stopListening()
        if (remainingText.isNotEmpty()) {
            // Wake word + command in same utterance
            _uiState.update {
                VoiceUiState(
                    isVisible = true,
                    mode = VoiceMode.COMMAND,
                    phase = VoicePhase.PROCESSING,
                    recognizedText = remainingText,
                )
            }
            processCommand(remainingText)
        } else {
            // Just wake word, switch to command mode
            _uiState.update {
                VoiceUiState(
                    isVisible = true,
                    mode = VoiceMode.COMMAND,
                    phase = VoicePhase.LISTENING,
                )
            }
            voiceCommandManager.startListening(RecognitionMode.COMMAND)
        }
    }

    private fun handleFinalText(text: String) {
        val currentMode = _uiState.value.mode

        when (currentMode) {
            VoiceMode.WAKE_WORD -> {
                if (text.isNotEmpty()) {
                    val match = VoiceCommandParser.extractWakeWord(text, customWakeWord)
                    if (match.detected) {
                        handleWakeWordDetected(match.remainingText)
                    } else {
                        // No wake word, restart passive listening
                        scheduleRestart(VoiceMode.WAKE_WORD)
                    }
                } else {
                    scheduleRestart(VoiceMode.WAKE_WORD)
                }
            }

            VoiceMode.COMMAND, VoiceMode.MANUAL -> {
                if (text.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            recognizedText = text,
                            phase = VoicePhase.PROCESSING
                        )
                    }
                    processCommand(text)
                } else {
                    _uiState.update {
                        it.copy(
                            phase = VoicePhase.ERROR,
                            errorMessage = "I didn't catch that"
                        )
                    }
                    scheduleFeedbackAndRestart()
                }
            }
        }
    }

    private fun handleError(event: VoiceRecognitionEvent.Error) {
        val currentMode = _uiState.value.mode

        if (currentMode == VoiceMode.WAKE_WORD) {
            // Silently restart for passive mode
            if (event.recoverable) {
                scheduleRestart(VoiceMode.WAKE_WORD)
            }
            return
        }

        if (event.recoverable && _uiState.value.isVisible) {
            // For command/manual mode, show brief error then restart
            _uiState.update {
                it.copy(
                    phase = VoicePhase.ERROR,
                    errorMessage = if (event.code == android.speech.SpeechRecognizer.ERROR_NO_MATCH ||
                        event.code == android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        "I didn't catch that"
                    } else {
                        event.message
                    }
                )
            }
            scheduleFeedbackAndRestart()
        } else {
            _uiState.update {
                it.copy(
                    phase = VoicePhase.ERROR,
                    errorMessage = event.message
                )
            }
            viewModelScope.launch {
                delay(1500)
                dismissOverlay()
            }
        }
    }

    private fun processCommand(text: String) {
        viewModelScope.launch {
            val command = VoiceCommandParser.parseCommand(text, customWakeWord)
            when (command) {
                is VoiceCommand.WakeWordDetected -> {
                    // Just wake word again, listen for command
                    _uiState.update {
                        it.copy(
                            mode = VoiceMode.COMMAND,
                            phase = VoicePhase.LISTENING,
                            recognizedText = "",
                        )
                    }
                    voiceCommandManager.startListening(RecognitionMode.COMMAND)
                }

                is VoiceCommand.Unknown -> {
                    _uiState.update {
                        it.copy(
                            phase = VoicePhase.FEEDBACK,
                            feedbackText = "I didn't understand: \"$text\"",
                        )
                    }
                    scheduleFeedbackAndRestart()
                }

                else -> {
                    val feedback = VoiceCommandActionExecutor.execute(
                        command = command,
                        playerConnection = playerConnectionProvider?.invoke(),
                        onSearch = { query -> onSearchCallback?.invoke(query) },
                        onNavigate = { route -> onNavigateCallback?.invoke(route) },
                    )
                    _uiState.update {
                        it.copy(
                            phase = VoicePhase.FEEDBACK,
                            feedbackText = feedback,
                        )
                    }
                    scheduleFeedbackAndRestart()
                }
            }
        }
    }

    private fun scheduleFeedbackAndRestart() {
        feedbackJob?.cancel()
        feedbackJob = viewModelScope.launch {
            delay(1200)
            _uiState.update { VoiceUiState() }
            maybeStartWakeWordListening()
        }
    }

    private fun scheduleRestart(mode: VoiceMode) {
        restartJob?.cancel()
        restartJob = viewModelScope.launch {
            delay(restartDelay)
            restartDelay = (restartDelay * 1.5).toLong().coerceAtMost(maxRestartDelay)
            if (isAppInForeground && voiceEnabled) {
                when (mode) {
                    VoiceMode.WAKE_WORD -> {
                        if (wakeWordEnabled && hasMicPermission) {
                            voiceCommandManager.startListening(RecognitionMode.WAKE_WORD)
                        }
                    }
                    VoiceMode.COMMAND, VoiceMode.MANUAL -> {
                        if (_uiState.value.isVisible) {
                            voiceCommandManager.startListening(RecognitionMode.COMMAND)
                        }
                    }
                }
            }
        }
    }

    private fun maybeStartWakeWordListening() {
        if (voiceEnabled && wakeWordEnabled && isAppInForeground && hasMicPermission &&
            _uiState.value.mode != VoiceMode.COMMAND && _uiState.value.mode != VoiceMode.MANUAL
        ) {
            _uiState.update {
                VoiceUiState(mode = VoiceMode.WAKE_WORD, phase = VoicePhase.IDLE)
            }
            voiceCommandManager.startListening(RecognitionMode.WAKE_WORD)
        }
    }

    override fun onCleared() {
        super.onCleared()
        restartJob?.cancel()
        feedbackJob?.cancel()
        voiceCommandManager.destroy()
    }
}
