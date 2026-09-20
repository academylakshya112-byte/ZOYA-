package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.ZoyaForegroundService

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val prefs = context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE)
            val startOnBoot = prefs.getBoolean("start_on_boot", true)
            Log.d(TAG, "Boot event received. start_on_boot=$startOnBoot")
            if (startOnBoot) {
                try {
                    val serviceIntent = Intent(context, ZoyaForegroundService::class.java)
                    ContextCompat.startForegroundService(context, serviceIntent)
                    Log.i(TAG, "ZoyaForegroundService started on boot.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start Zoya on boot", e)
                }
            }
        }
    }
}
