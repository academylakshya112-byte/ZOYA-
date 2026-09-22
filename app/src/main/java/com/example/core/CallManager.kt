package com.example.core

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.util.Log

object CallManager {
    private const val TAG = "CallManager"

    fun onIncomingCall(context: Context, callerName: String, isWhatsApp: Boolean) {
        val guardianEnabled = SettingsManager.isVoiceGuardianEnabled(context)
        Log.i(TAG, "Incoming call from: $callerName (WhatsApp: $isWhatsApp)")

        EventManager.publish(
            AssistantEvent.CallEvent(
                number = callerName,
                callerName = callerName,
                isWhatsApp = isWhatsApp,
                state = "RINGING"
            )
        )

        // Unified voice output for call announcement
        VoiceOutputManager.speakCallAnnouncement(callerName, isWhatsApp)
    }

    fun answerIncomingCall(context: Context): VerificationResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (context.checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                try {
                    val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                    telecom?.acceptRingingCall()
                    return VerificationResult.Success("Call utha liya gaya hai.")
                } catch (e: Exception) {
                    Log.e(TAG, "TelecomManager answer failed", e)
                }
            }
        }

        // Accessibility click fallback
        val clicked = AccessibilityController.clickText("Answer") ||
                      AccessibilityController.clickText("Accept") ||
                      AccessibilityController.clickText("Uthao") ||
                      AccessibilityController.clickText("Receive")
        return if (clicked) {
            VerificationResult.Success("Call utha liya gaya hai.")
        } else {
            VerificationResult.Failure("Call uthana sambhav nahi hua. Kripya screen par dekhein.")
        }
    }

    fun rejectIncomingCall(context: Context): VerificationResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (context.checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                try {
                    val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                    telecom?.endCall()
                    return VerificationResult.Success("Call cut kar diya gaya hai.")
                } catch (e: Exception) {
                    Log.e(TAG, "TelecomManager endCall failed", e)
                }
            }
        }

        // Accessibility click fallback
        val clicked = AccessibilityController.clickText("Decline") ||
                      AccessibilityController.clickText("Reject") ||
                      AccessibilityController.clickText("Dismiss") ||
                      AccessibilityController.clickText("Kaat do")
        return if (clicked) {
            VerificationResult.Success("Call reject kar diya gaya hai.")
        } else {
            VerificationResult.Failure("Call reject nahi kiya ja saka.")
        }
    }
}
