package com.example.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class ActionRecord(
    val id: String = UUID.randomUUID().toString(),
    val actionName: String,
    val target: String,
    val params: Map<String, String>,
    val success: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

object ActionHistoryManager {
    private val _history = MutableStateFlow<List<ActionRecord>>(emptyList())
    val history: StateFlow<List<ActionRecord>> = _history.asStateFlow()

    private val _currentApp = MutableStateFlow("")
    val currentApp: StateFlow<String> = _currentApp.asStateFlow()

    private val _lastAction = MutableStateFlow<ActionRecord?>(null)
    val lastAction: StateFlow<ActionRecord?> = _lastAction.asStateFlow()

    fun updateCurrentApp(packageName: String) {
        _currentApp.value = packageName
    }

    fun recordAction(
        actionName: String,
        target: String,
        params: Map<String, String> = emptyMap(),
        success: Boolean,
        message: String
    ): ActionRecord {
        val record = ActionRecord(
            actionName = actionName,
            target = target,
            params = params,
            success = success,
            message = message
        )
        val updated = (_history.value + record).takeLast(50)
        _history.value = updated
        _lastAction.value = record
        return record
    }
}
