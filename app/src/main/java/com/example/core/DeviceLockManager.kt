package com.example.core

import android.accessibilityservice.AccessibilityService
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.example.accessibility.ZoyaAccessibilityService

object DeviceLockManager {
    private const val TAG = "DeviceLockManager"

    private var unlockReceiver: BroadcastReceiver? = null
    private var isReceiverRegistered = false

    /**
     * Executes real Android system device lock.
     * Uses AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN (Android 9+) or DevicePolicyManager.lockNow().
     * NEVER fakes the lock using an Activity or overlay.
     */
    fun lockDevice(context: Context): Boolean {
        Log.w(TAG, "Executing REAL system device lock via DeviceLockManager")

        // 1. Accessibility Service real Android system lock (Android 9 / API 28+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val accessibility = ZoyaAccessibilityService.instance
            if (accessibility != null) {
                val locked = accessibility.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
                if (locked) {
                    Log.i(TAG, "Device locked successfully via AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN.")
                    registerUnlockListener(context)
                    return true
                }
            }
        }

        // 2. DevicePolicyManager fallback
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            if (dpm != null) {
                dpm.lockNow()
                Log.i(TAG, "Device locked successfully via DevicePolicyManager.")
                registerUnlockListener(context)
                return true
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "DevicePolicyManager.lockNow() not permitted: ${e.message}")
        }

        // 3. Fallback log
        Log.e(TAG, "Device lock requires Zoya Accessibility Service enabled in Android Settings.")
        return false
    }

    /**
     * Listens for genuine user device unlock (PIN, Pattern, Fingerprint, Face).
     * Android fires ACTION_USER_PRESENT upon successful genuine keyguard unlock.
     */
    fun registerUnlockListener(context: Context) {
        if (isReceiverRegistered) return
        val appContext = context.applicationContext
        unlockReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_USER_PRESENT) {
                    Log.i(TAG, "Genuine user unlock detected via ACTION_USER_PRESENT.")
                    SecurityManager.onDeviceUnlockedByOwner(appContext)
                    unregisterUnlockListener(appContext)
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        appContext.registerReceiver(unlockReceiver, filter)
        isReceiverRegistered = true
    }

    fun unregisterUnlockListener(context: Context) {
        if (!isReceiverRegistered) return
        try {
            unlockReceiver?.let { context.applicationContext.unregisterReceiver(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering unlock receiver", e)
        }
        unlockReceiver = null
        isReceiverRegistered = false
    }
}
