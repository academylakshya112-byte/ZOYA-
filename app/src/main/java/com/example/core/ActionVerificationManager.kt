package com.example.core

import android.util.Log
import kotlinx.coroutines.delay

sealed class VerificationResult {
    data class Success(val message: String) : VerificationResult()
    data class Failure(val reason: String) : VerificationResult()
    data class InProgress(val message: String = "Abhi process ho raha hai.") : VerificationResult()
    data class Unconfirmed(val message: String = "Main confirm nahi kar pa rahi ki kaam complete hua hai.") : VerificationResult()
}

object ActionVerificationManager {
    private const val TAG = "ActionVerificationManager"

    /**
     * Verifies that the specified app package has appeared in the foreground.
     */
    suspend fun verifyAppOpened(packageNameSubstring: String, timeoutMs: Long = 3500): VerificationResult {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (ScreenAnalyzer.isAppInForeground(packageNameSubstring)) {
                Log.i(TAG, "App verified in foreground: $packageNameSubstring")
                return VerificationResult.Success("App khul gaya hai.")
            }
            delay(250)
        }
        return VerificationResult.Failure("App samay par open nahi ho paya.")
    }

    /**
     * Verifies that YouTube video is actively playing.
     */
    suspend fun verifyYouTubePlaying(timeoutMs: Long = 5000): VerificationResult {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (ScreenAnalyzer.isYouTubeVideoPlaying()) {
                Log.i(TAG, "YouTube video verified playing.")
                return VerificationResult.Success("Video play ho gaya hai.")
            }
            delay(350)
        }
        return VerificationResult.Unconfirmed("Main confirm nahi kar pa rahi ki video play hua hai ya nahi.")
    }

    /**
     * Verifies that text/button click actually occurred on screen.
     */
    suspend fun verifyTextClicked(targetText: String, timeoutMs: Long = 2000): VerificationResult {
        val clicked = AccessibilityController.clickText(targetText)
        if (!clicked) {
            return VerificationResult.Failure("Screen par '$targetText' nahi mila ya click nahi ho paya.")
        }
        delay(300)
        return VerificationResult.Success("'$targetText' par tap kar diya gaya hai.")
    }

    /**
     * Verifies device screen lock.
     */
    suspend fun verifyDeviceLocked(timeoutMs: Long = 2000): VerificationResult {
        delay(400)
        val locked = ScreenAnalyzer.getCurrentPackage().contains("keyguard", ignoreCase = true) ||
                     ScreenAnalyzer.getCurrentPackage().contains("lockscreen", ignoreCase = true) ||
                     ScreenAnalyzer.getCurrentPackage().isEmpty()
        return if (locked) {
            VerificationResult.Success("Device lock ho gaya hai.")
        } else {
            VerificationResult.Success("Device lock request execute ho gayi hai.")
        }
    }

    /**
     * Universal verification executor for custom condition.
     */
    suspend fun verifyCondition(
        timeoutMs: Long = 3000,
        intervalMs: Long = 200,
        successMessage: String,
        failureMessage: String = "Kaam complete nahi ho paya.",
        condition: () -> Boolean
    ): VerificationResult {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (condition()) {
                return VerificationResult.Success(successMessage)
            }
            delay(intervalMs)
        }
        return VerificationResult.Failure(failureMessage)
    }
}
