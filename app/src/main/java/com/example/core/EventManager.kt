package com.example.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AssistantEvent {
    data class NotificationEvent(
        val id: String,
        val app: String,
        val sender: String,
        val messageText: String,
        val timestamp: Long = System.currentTimeMillis(),
        val isProtected: Boolean = false
    ) : AssistantEvent()

    data class CallEvent(
        val number: String,
        val callerName: String,
        val isWhatsApp: Boolean,
        val state: String, // RINGING, OFFHOOK, IDLE
        val timestamp: Long = System.currentTimeMillis()
    ) : AssistantEvent()

    data class VoiceEvent(
        val sampleLength: Int,
        val speakerId: String,
        val confidence: Float,
        val state: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : AssistantEvent()

    data class UserCommandEvent(
        val command: String,
        val speakerRole: VoiceRole,
        val isProtected: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    ) : AssistantEvent()

    data class SecurityEvent(
        val eventType: String,
        val message: String,
        val details: String = "",
        val timestamp: Long = System.currentTimeMillis()
    ) : AssistantEvent()

    data class ScreenEvent(
        val currentPackage: String,
        val currentActivity: String,
        val screenText: String = "",
        val timestamp: Long = System.currentTimeMillis()
    ) : AssistantEvent()

    data class ActionResultEvent(
        val actionName: String,
        val target: String,
        val success: Boolean,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : AssistantEvent()
}

object EventManager {
    private val _events = MutableSharedFlow<AssistantEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<AssistantEvent> = _events.asSharedFlow()

    private val _recentEvents = MutableStateFlow<List<AssistantEvent>>(emptyList())
    val recentEvents: StateFlow<List<AssistantEvent>> = _recentEvents.asStateFlow()

    fun publish(event: AssistantEvent) {
        _events.tryEmit(event)
        val updated = (_recentEvents.value + event).takeLast(50)
        _recentEvents.value = updated
    }
}
