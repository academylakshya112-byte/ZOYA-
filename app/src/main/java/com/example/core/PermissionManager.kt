package com.example.core

enum class VoiceRole {
    OWNER,
    TRUSTED_USER,
    GUEST,
    UNKNOWN
}

enum class AssistantPermission {
    CHAT,
    OPEN_APPS,
    SCREEN_CONTROL,
    YOUTUBE,
    WHATSAPP,
    SMS,
    CALLS,
    SETTINGS,
    VOICE_MANAGEMENT,
    SECURITY,
    SOS,
    DEVICE_LOCK
}

object PermissionManager {

    /**
     * Protected actions require verified identity (Owner or Authorized role) when Voice Guardian is ON.
     */
    fun isProtectedAction(permission: AssistantPermission): Boolean {
        return when (permission) {
            AssistantPermission.CHAT -> false
            AssistantPermission.OPEN_APPS -> true
            AssistantPermission.SCREEN_CONTROL -> true
            AssistantPermission.YOUTUBE -> true
            AssistantPermission.WHATSAPP -> true
            AssistantPermission.SMS -> true
            AssistantPermission.CALLS -> true
            AssistantPermission.SETTINGS -> true
            AssistantPermission.VOICE_MANAGEMENT -> true
            AssistantPermission.SECURITY -> true
            AssistantPermission.SOS -> true
            AssistantPermission.DEVICE_LOCK -> true
        }
    }

    /**
     * Checks if a given role possesses the requested permission.
     */
    fun hasPermission(role: VoiceRole, permission: AssistantPermission): Boolean {
        return when (role) {
            VoiceRole.OWNER -> true // Owner has full control over all operations
            VoiceRole.TRUSTED_USER -> {
                when (permission) {
                    AssistantPermission.CHAT,
                    AssistantPermission.OPEN_APPS,
                    AssistantPermission.SCREEN_CONTROL,
                    AssistantPermission.YOUTUBE,
                    AssistantPermission.CALLS -> true
                    AssistantPermission.WHATSAPP,
                    AssistantPermission.SMS,
                    AssistantPermission.SETTINGS,
                    AssistantPermission.VOICE_MANAGEMENT,
                    AssistantPermission.SECURITY,
                    AssistantPermission.SOS,
                    AssistantPermission.DEVICE_LOCK -> false
                }
            }
            VoiceRole.GUEST -> {
                when (permission) {
                    AssistantPermission.CHAT -> true
                    else -> false
                }
            }
            VoiceRole.UNKNOWN -> {
                false // Unknown voices have NO permissions for protected operations
            }
        }
    }
}
