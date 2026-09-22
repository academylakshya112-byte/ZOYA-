package com.example.core

import android.content.Context

sealed class GuardianDecision {
    data class Allowed(
        val role: VoiceRole,
        val profile: VoiceProfile?
    ) : GuardianDecision()

    data class Blocked(
        val reason: String,
        val attemptCount: Int,
        val isLockTriggered: Boolean
    ) : GuardianDecision()
}

object VoiceGuardianManager {

    /**
     * Evaluates a command through the Voice Guardian protocol:
     * 1. Check if Voice Guardian is enabled
     * 2. If disabled -> Allow command
     * 3. If enabled -> Check whether the action is protected
     * 4. If action is protected -> Perform speaker verification
     * 5. If speaker verification fails -> Escalate to SecurityManager (Warning or Device Lock)
     */
    fun evaluateCommand(
        context: Context,
        command: String,
        permission: AssistantPermission,
        audioSamples: ShortArray? = null,
        speakerHint: String? = null
    ): GuardianDecision {
        val guardianEnabled = SettingsManager.isVoiceGuardianEnabled(context)

        // If Guardian is OFF, default to normal execution
        if (!guardianEnabled) {
            return GuardianDecision.Allowed(VoiceRole.OWNER, null)
        }

        val isProtected = PermissionManager.isProtectedAction(permission)
        if (!isProtected) {
            return GuardianDecision.Allowed(VoiceRole.GUEST, null)
        }

        // Verify speaker identity
        val verification = VoiceVerificationManager.verifySpeaker(context, audioSamples, speakerHint)

        // Check if verified role has the required permission
        if (verification.isVerified && PermissionManager.hasPermission(verification.role, permission)) {
            if (verification.role == VoiceRole.OWNER) {
                SecurityManager.onOwnerVerified(context)
            }
            return GuardianDecision.Allowed(verification.role, verification.matchedProfile)
        }

        // Unauthorized / Unknown voice detected on protected action
        val securityResult = SecurityManager.onUnauthorizedAttempt(context, command)
        return when (securityResult) {
            is SecurityAttemptResult.Warning -> {
                GuardianDecision.Blocked(
                    reason = securityResult.message,
                    attemptCount = securityResult.attemptCount,
                    isLockTriggered = false
                )
            }
            is SecurityAttemptResult.Locked -> {
                GuardianDecision.Blocked(
                    reason = securityResult.message,
                    attemptCount = securityResult.attemptCount,
                    isLockTriggered = true
                )
            }
            is SecurityAttemptResult.Allowed -> {
                GuardianDecision.Allowed(VoiceRole.OWNER, null)
            }
        }
    }
}
