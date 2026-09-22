package com.example.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.accessibility.ZoyaAccessibilityService
import kotlinx.coroutines.delay

object ScreenControlManager {
    private const val TAG = "ScreenControlManager"

    suspend fun openApp(context: Context, appName: String): VerificationResult {
        val lower = appName.lowercase()
        val pm = context.packageManager

        var targetPackage: String? = null
        if (lower == "whatsapp") {
            targetPackage = "com.whatsapp"
        } else if (lower == "youtube") {
            targetPackage = "com.google.android.youtube"
        } else if (lower == "camera" || lower == "kamera") {
            val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                return ActionVerificationManager.verifyAppOpened("camera")
            } catch (e: Exception) {}
        }

        if (targetPackage == null) {
            val installed = pm.getInstalledApplications(0)
            for (app in installed) {
                val label = pm.getApplicationLabel(app).toString().lowercase()
                if (label.contains(lower)) {
                    targetPackage = app.packageName
                    break
                }
            }
        }

        if (targetPackage == null) {
            return VerificationResult.Failure("App '$appName' nahi mila.")
        }

        val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return ActionVerificationManager.verifyAppOpened(targetPackage)
        }
        return VerificationResult.Failure("App '$appName' launch nahi ho paya.")
    }

    suspend fun searchAndPlayYouTube(context: Context, query: String): VerificationResult {
        Log.i(TAG, "Executing YouTube search & play with verification for: $query")
        ZoyaAccessibilityService.startYouTubePlayAutomation(query, context)
        // Wait and verify active playback
        delay(1200)
        return ActionVerificationManager.verifyYouTubePlaying(timeoutMs = 4500)
    }

    fun navigateYouTube(action: String): VerificationResult {
        return when (action.lowercase()) {
            "scroll_down", "down", "niche" -> {
                val ok = AccessibilityController.scroll("down")
                if (ok) VerificationResult.Success("Neeche scroll kar diya.") else VerificationResult.Failure("Scroll nahi ho paya.")
            }
            "scroll_up", "up", "upar" -> {
                val ok = AccessibilityController.scroll("up")
                if (ok) VerificationResult.Success("Upar scroll kar diya.") else VerificationResult.Failure("Scroll nahi ho paya.")
            }
            "back", "piche" -> {
                val ok = AccessibilityController.pressBack()
                if (ok) VerificationResult.Success("Peeche chale gaye.") else VerificationResult.Failure("Peeche nahi ja sake.")
            }
            "pause", "play" -> {
                val clicked = AccessibilityController.clickText("Pause video") ||
                              AccessibilityController.clickText("Play video") ||
                              AccessibilityController.clickText("Pause") ||
                              AccessibilityController.clickText("Play")
                if (clicked) VerificationResult.Success("Video toggle kar diya.") else VerificationResult.Failure("Play/Pause button nahi mila.")
            }
            else -> VerificationResult.Failure("Unknown YouTube action: $action")
        }
    }

    suspend fun clickOnScreen(targetText: String): VerificationResult {
        return ActionVerificationManager.verifyTextClicked(targetText)
    }

    fun scrollScreen(direction: String): VerificationResult {
        val ok = AccessibilityController.scroll(direction)
        return if (ok) VerificationResult.Success("Screen $direction scroll ho gayi.")
        else VerificationResult.Failure("Scroll nahi ho paya.")
    }

    fun lockDevice(context: Context): VerificationResult {
        val ok = DeviceLockManager.lockDevice(context)
        return if (ok) VerificationResult.Success("Device lock ho gaya hai.")
        else VerificationResult.Failure("Device lock karne ke liye Accessibility Service permission zaroori hai.")
    }
}
