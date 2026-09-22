package com.example.core

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

enum class SecurityState {
    NORMAL,
    WARNING,
    SECURITY_LOCK_PENDING,
    DEVICE_LOCKED_BY_VOICE_GUARDIAN
}

@Serializable
data class SecurityLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val description: String,
    val speakerRole: String
)

sealed class SecurityAttemptResult {
    data class Warning(
        val attemptCount: Int,
        val limit: Int,
        val message: String
    ) : SecurityAttemptResult()

    data class Locked(
        val attemptCount: Int,
        val limit: Int,
        val message: String
    ) : SecurityAttemptResult()

    object Allowed : SecurityAttemptResult()
}

object SecurityManager {
    private val _securityState = MutableStateFlow(SecurityState.NORMAL)
    val securityState: StateFlow<SecurityState> = _securityState.asStateFlow()

    private val _unauthorizedAttempts = MutableStateFlow(0)
    val unauthorizedAttempts: StateFlow<Int> = _unauthorizedAttempts.asStateFlow()

    private val _securityLogs = MutableStateFlow<List<SecurityLogEntry>>(emptyList())
    val securityLogs: StateFlow<List<SecurityLogEntry>> = _securityLogs.asStateFlow()

    private const val PREFS_NAME = "ZoyaSecurityPrefs"
    private const val KEY_LOGS = "security_logs_json"

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_LOGS, null)
        if (raw != null) {
            try {
                _securityLogs.value = Json.decodeFromString(raw)
            } catch (e: Exception) {
                _securityLogs.value = emptyList()
            }
        }
    }

    /**
     * Called when an unknown or unauthorized voice attempts a protected command.
     */
    fun onUnauthorizedAttempt(context: Context, command: String): SecurityAttemptResult {
        val guardianEnabled = SettingsManager.isVoiceGuardianEnabled(context)
        if (!guardianEnabled) {
            return SecurityAttemptResult.Allowed
        }

        val currentCount = _unauthorizedAttempts.value + 1
        _unauthorizedAttempts.value = currentCount
        val limit = SettingsManager.getUnauthorizedAttemptsLimit(context)
        val lockEnabled = SettingsManager.isUnknownVoiceLockEnabled(context)

        addLog(
            context,
            eventType = "UNAUTHORIZED_ATTEMPT",
            description = "Blocked protected command '$command'. Attempt $currentCount of $limit.",
            speakerRole = VoiceRole.UNKNOWN.name
        )

        EventManager.publish(
            AssistantEvent.SecurityEvent(
                eventType = "UNAUTHORIZED_ATTEMPT",
                message = "Blocked attempt $currentCount/$limit: $command",
                details = "Voice verification failed for protected action"
            )
        )

        if (currentCount >= limit && lockEnabled) {
            _securityState.value = SecurityState.DEVICE_LOCKED_BY_VOICE_GUARDIAN
            DeviceLockManager.lockDevice(context)
            addLog(
                context,
                eventType = "DEVICE_LOCKED",
                description = "Security lock activated after $currentCount unauthorized attempts.",
                speakerRole = VoiceRole.UNKNOWN.name
            )
            return SecurityAttemptResult.Locked(
                attemptCount = currentCount,
                limit = limit,
                message = "Unauthorized voice attempts limit reached. Device security lock activated."
            )
        } else {
            _securityState.value = SecurityState.WARNING
            return SecurityAttemptResult.Warning(
                attemptCount = currentCount,
                limit = limit,
                message = "यह voice verify नहीं हुई है। Boss की voice से verification करें।"
            )
        }
    }

    /**
     * Called when Owner voice is recognized and verified.
     */
    fun onOwnerVerified(context: Context) {
        _unauthorizedAttempts.value = 0
        if (_securityState.value != SecurityState.DEVICE_LOCKED_BY_VOICE_GUARDIAN) {
            _securityState.value = SecurityState.NORMAL
        }
        addLog(
            context,
            eventType = "OWNER_VERIFIED",
            description = "Owner verified successfully. Security counter reset.",
            speakerRole = VoiceRole.OWNER.name
        )
    }

    /**
     * Called when genuine user unlocks the phone using Android PIN/Biometrics.
     */
    fun onDeviceUnlockedByOwner(context: Context) {
        _unauthorizedAttempts.value = 0
        _securityState.value = SecurityState.NORMAL
        addLog(
            context,
            eventType = "GENUINE_UNLOCK",
            description = "Device unlocked legitimately via Android system security.",
            speakerRole = VoiceRole.OWNER.name
        )
    }

    private fun addLog(context: Context, eventType: String, description: String, speakerRole: String) {
        val entry = SecurityLogEntry(
            eventType = eventType,
            description = description,
            speakerRole = speakerRole
        )
        val updated = (_securityLogs.value + entry).takeLast(100)
        _securityLogs.value = updated

        try {
            val json = Json.encodeToString(updated)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_LOGS, json).apply()
        } catch (e: Exception) {
            // Ignore write errors
        }
    }
}
