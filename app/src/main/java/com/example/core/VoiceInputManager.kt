package com.example.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VoiceInputState {
    IDLE,
    LISTENING,
    PROCESSING,
    MUTED_FOR_SPEECH
}

object VoiceInputManager {
    private val _inputState = MutableStateFlow(VoiceInputState.IDLE)
    val inputState: StateFlow<VoiceInputState> = _inputState.asStateFlow()

    private var lastRecordedChunk: ShortArray? = null

    fun updateState(state: VoiceInputState) {
        _inputState.value = state
    }

    fun onAudioData(buffer: ShortArray, length: Int) {
        if (length > 0) {
            lastRecordedChunk = buffer.copyOf(minOf(length, 1600))
        }
    }

    fun getLastVoiceSample(): ShortArray? {
        return lastRecordedChunk
    }
}
