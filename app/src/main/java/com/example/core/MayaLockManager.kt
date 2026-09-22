package com.example.core

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import com.example.accessibility.ZoyaAccessibilityService
import java.security.MessageDigest

enum class MayaLockType {
    NONE,
    PIN,
    PATTERN
}

object MayaLockManager {
    private const val TAG = "MayaLockManager"
    private const val PREFS_NAME = "MayaLockPrefs"

    private const val KEY_LOCK_TYPE = "maya_lock_type"
    private const val KEY_PIN_HASH = "maya_pin_hash"
    private const val KEY_PATTERN_HASH = "maya_pattern_hash"
    private const val KEY_ENABLED = "maya_lock_enabled"
    private const val KEY_AUTH_REMEMBERED = "maya_auth_remembered"
    private const val KEY_LAST_TEST_SUCCESS = "maya_last_test_success"
    private const val KEY_LAST_TEST_TIMESTAMP = "maya_last_test_timestamp"

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLockType(context: Context): MayaLockType {
        val raw = getPrefs(context).getString(KEY_LOCK_TYPE, MayaLockType.NONE.name)
        return try {
            MayaLockType.valueOf(raw ?: MayaLockType.NONE.name)
        } catch (e: Exception) {
            MayaLockType.NONE
        }
    }

    fun hasCredential(context: Context): Boolean {
        val type = getLockType(context)
        return when (type) {
            MayaLockType.PIN -> getPrefs(context).getString(KEY_PIN_HASH, null)?.isNotBlank() == true
            MayaLockType.PATTERN -> getPrefs(context).getString(KEY_PATTERN_HASH, null)?.isNotBlank() == true
            MayaLockType.NONE -> false
        }
    }

    fun isLockSystemEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ENABLED, true) && hasCredential(context)
    }

    fun setLockSystemEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    private fun hashCredential(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun savePin(context: Context, pin: String) {
        val hashed = hashCredential(pin)
        getPrefs(context).edit()
            .putString(KEY_LOCK_TYPE, MayaLockType.PIN.name)
            .putString(KEY_PIN_HASH, hashed)
            .remove(KEY_PATTERN_HASH)
            .putBoolean(KEY_ENABLED, true)
            .apply()
        Log.i(TAG, "Maya PIN configured successfully.")
    }

    fun savePattern(context: Context, patternDots: List<Int>) {
        val patternString = patternDots.joinToString(",")
        val hashed = hashCredential(patternString)
        getPrefs(context).edit()
            .putString(KEY_LOCK_TYPE, MayaLockType.PATTERN.name)
            .putString(KEY_PATTERN_HASH, hashed)
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_ENABLED, true)
            .apply()
        Log.i(TAG, "Maya Pattern configured successfully.")
    }

    fun verifyPin(context: Context, inputPin: String): Boolean {
        val storedHash = getPrefs(context).getString(KEY_PIN_HASH, null) ?: return false
        return hashCredential(inputPin) == storedHash
    }

    fun verifyPattern(context: Context, patternDots: List<Int>): Boolean {
        val patternString = patternDots.joinToString(",")
        val storedHash = getPrefs(context).getString(KEY_PATTERN_HASH, null) ?: return false
        return hashCredential(patternString) == storedHash
    }

    fun removeCredential(context: Context) {
        getPrefs(context).edit()
            .putString(KEY_LOCK_TYPE, MayaLockType.NONE.name)
            .remove(KEY_PIN_HASH)
            .remove(KEY_PATTERN_HASH)
            .putBoolean(KEY_ENABLED, false)
            .apply()
    }

    /**
     * Checks if accessibility permission is currently active.
     */
    fun isAccessibilityPermissionGranted(context: Context): Boolean {
        if (ZoyaAccessibilityService.instance != null) return true

        val expectedServiceName = "${context.packageName}/${ZoyaAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedServiceName, ignoreCase = true) ||
                componentName.contains(context.packageName, ignoreCase = true)
            ) {
                return true
            }
        }
        return false
    }

    /**
     * One-time authorization rule:
     * Returns true if permission is NOT currently granted.
     * Once granted, it remains granted and har baar popup nahi aayega.
     * If user later revokes in Android Settings, it detects revocation and re-prompts.
     */
    fun needsAuthorization(context: Context): Boolean {
        val granted = isAccessibilityPermissionGranted(context)
        if (granted) {
            // Update stored state
            getPrefs(context).edit().putBoolean(KEY_AUTH_REMEMBERED, true).apply()
            return false
        }
        return true
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open accessibility settings", e)
        }
    }

    /**
     * Performs Maya authorized Phone Lock.
     */
    fun lockPhone(context: Context): Boolean {
        if (!isAccessibilityPermissionGranted(context)) {
            Log.w(TAG, "Lock failed: Accessibility permission not granted.")
            return false
        }
        return DeviceLockManager.lockDevice(context)
    }

    /**
     * Wakes the device screen if it is off or locked.
     */
    fun wakeScreen(context: Context) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm != null && !pm.isInteractive) {
                @Suppress("DEPRECATION")
                val wakeLock = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "Maya:WakeLockForUnlock"
                )
                wakeLock.acquire(3000)
                Log.i(TAG, "Acquired WakeLock to turn screen ON.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error waking screen", e)
        }
    }

    /**
     * Performs a swipe-up gesture from the bottom of the screen to reveal keyguard entry.
     */
    fun performSwipeUp(context: Context): Boolean {
        val service = ZoyaAccessibilityService.instance
        if (service != null) {
            val metrics = context.resources.displayMetrics
            val w = metrics.widthPixels.toFloat()
            val h = metrics.heightPixels.toFloat()
            val startX = w / 2f
            val startY = h * 0.85f
            val endY = h * 0.20f
            return AccessibilityController.swipe(startX, startY, startX, endY, 250)
        }
        return false
    }

    /**
     * Initiates the Unlock workflow:
     * Screen Wake -> Swipe Up -> Launch Maya Unlock Credential Interface (if set) -> Keyguard Dismissal.
     */
    fun executeUnlockWorkflow(context: Context) {
        wakeScreen(context)
        performSwipeUp(context)

        // If Maya Lock system is enabled and configured, present Maya's secure unlock overlay
        if (isLockSystemEnabled(context)) {
            try {
                val intent = Intent(context, Class.forName("com.example.ui.MayaUnlockActivity")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch MayaUnlockActivity", e)
            }
        }
    }

    fun recordTestResult(context: Context, success: Boolean) {
        getPrefs(context).edit()
            .putBoolean(KEY_LAST_TEST_SUCCESS, success)
            .putLong(KEY_LAST_TEST_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun getLastTestResult(context: Context): Pair<Boolean, Long> {
        val success = getPrefs(context).getBoolean(KEY_LAST_TEST_SUCCESS, false)
        val time = getPrefs(context).getLong(KEY_LAST_TEST_TIMESTAMP, 0L)
        return Pair(success, time)
    }
}
