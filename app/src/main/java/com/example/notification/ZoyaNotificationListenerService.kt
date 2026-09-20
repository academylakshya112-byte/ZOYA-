package com.example.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.accessibility.ZoyaAccessibilityService

class ZoyaNotificationListenerService : NotificationListenerService() {

    companion object {
        var isConnected = false
            private set
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        Log.d("ZoyaNotifListener", "Zoya NotificationListenerService connected successfully!")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        Log.d("ZoyaNotifListener", "Zoya NotificationListenerService disconnected.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        try {
            val comm = NotificationManagerHelper.processNotification(applicationContext, sbn)
            if (comm != null) {
                // Update legacy accessibility variables for fallback compatibility
                ZoyaAccessibilityService.lastNotificationSender = comm.sender
                ZoyaAccessibilityService.lastNotificationText = comm.preview
                ZoyaAccessibilityService.lastNotificationPackage = comm.packageName
                ZoyaAccessibilityService.lastNotificationTime = comm.timestamp
                Log.d("ZoyaNotifListener", "Intercepted: [${comm.app}] from ${comm.sender}: ${comm.preview}")
            }
        } catch (e: Exception) {
            Log.e("ZoyaNotifListener", "Error processing notification", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        val pkg = sbn.packageName ?: ""
        if (pkg.contains("whatsapp") || pkg.contains("dialer") || pkg.contains("phone") || pkg.contains("telecom")) {
            CallAnnouncer.stopAnnouncement()
        }
    }
}
