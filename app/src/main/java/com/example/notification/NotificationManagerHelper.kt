package com.example.notification

import android.content.Context
import android.content.SharedPreferences
import android.service.notification.StatusBarNotification
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class IncomingCommunication(
    val id: String,
    val sender: String,
    val app: String,
    val packageName: String,
    val type: String, // "whatsapp_message", "sms", "phone_call", "whatsapp_call", "other"
    val preview: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSensitiveOrFinancial: Boolean = false,
    val priority: String = "NORMAL" // "IMPORTANT", "NORMAL", "LOW_PRIORITY", "SPAM"
)

object NotificationManagerHelper {
    private const val PREFS_NAME = "ZoyaNotificationPrefs"
    private const val MAX_HISTORY = 30

    // Recent notifications store
    val recentCommunications = CopyOnWriteArrayList<IncomingCommunication>()
    
    // Deduplication tracking: Map of (Sender + Text) -> Timestamp
    private val seenMessageHashes = ConcurrentHashMap<String, Long>()

    // Listener callback for live spoken announcements
    var onNewCommunicationReceived: ((IncomingCommunication) -> Unit)? = null

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isPrivacyMode(context: Context): Boolean = getPrefs(context).getBoolean("privacy_mode", false)
    fun setPrivacyMode(context: Context, enabled: Boolean) = getPrefs(context).edit().putBoolean("privacy_mode", enabled).apply()

    fun isMessagePreviewEnabled(context: Context): Boolean = getPrefs(context).getBoolean("preview_mode", true)
    fun setMessagePreviewEnabled(context: Context, enabled: Boolean) = getPrefs(context).edit().putBoolean("preview_mode", enabled).apply()

    fun isAutonomousMode(context: Context): Boolean = getPrefs(context).getBoolean("autonomous_mode", false)
    fun setAutonomousMode(context: Context, enabled: Boolean) = getPrefs(context).edit().putBoolean("autonomous_mode", enabled).apply()

    fun getReplyStyle(context: Context): String = getPrefs(context).getString("reply_style", "casual") ?: "casual"
    fun setReplyStyle(context: Context, style: String) = getPrefs(context).edit().putString("reply_style", style).apply()

    fun isAppMonitored(context: Context, packageName: String): Boolean {
        val prefs = getPrefs(context)
        if (packageName.contains("whatsapp")) return prefs.getBoolean("monitor_whatsapp", true)
        if (packageName.contains("messaging") || packageName.contains("mms") || packageName.contains("sms") || packageName.contains("google.android.apps.messaging")) {
            return prefs.getBoolean("monitor_sms", true)
        }
        if (packageName.contains("dialer") || packageName.contains("telecom") || packageName.contains("phone")) {
            return prefs.getBoolean("monitor_calls", true)
        }
        return true
    }

    fun setAppMonitoring(context: Context, appType: String, enabled: Boolean) {
        val prefs = getPrefs(context)
        when (appType.lowercase()) {
            "whatsapp" -> prefs.edit().putBoolean("monitor_whatsapp", enabled).apply()
            "sms" -> prefs.edit().putBoolean("monitor_sms", enabled).apply()
            "calls" -> prefs.edit().putBoolean("monitor_calls", enabled).apply()
        }
    }

    fun processNotification(context: Context, sbn: StatusBarNotification): IncomingCommunication? {
        val packageName = sbn.packageName ?: return null
        if (packageName == context.packageName || packageName == "android") return null
        if (!isAppMonitored(context, packageName)) return null

        val notification = sbn.notification ?: return null
        val extras = notification.extras ?: return null

        val title = extras.getString(android.app.Notification.EXTRA_TITLE) 
            ?: extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() 
            ?: ""
        val text = extras.getString(android.app.Notification.EXTRA_TEXT) 
            ?: extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() 
            ?: ""

        if (title.isBlank() && text.isBlank()) return null

        // Determine App & Communication Type
        val (appName, commType) = when {
            packageName.contains("com.whatsapp") || packageName.contains("com.whatsapp.w4b") -> {
                val isCall = (notification.category == android.app.Notification.CATEGORY_CALL) 
                    || text.contains("Incoming voice call", ignoreCase = true) 
                    || text.contains("Incoming video call", ignoreCase = true) 
                    || title.contains("WhatsApp call", ignoreCase = true)
                if (isCall) Pair("WhatsApp", "whatsapp_call") else Pair("WhatsApp", "whatsapp_message")
            }
            packageName.contains("messaging") || packageName.contains("mms") || packageName.contains("sms") -> {
                Pair("SMS", "sms")
            }
            packageName.contains("dialer") || packageName.contains("phone") || notification.category == android.app.Notification.CATEGORY_CALL -> {
                Pair("Phone", "phone_call")
            }
            else -> Pair(packageName, "other")
        }

        // Deduplication & spam filtering
        val hashKey = "$packageName|$title|$text"
        val now = System.currentTimeMillis()
        val lastSeen = seenMessageHashes[hashKey]
        if (lastSeen != null && (now - lastSeen) < 10000) {
            // Duplicate notification within 10 seconds (e.g. WhatsApp progress update)
            return null
        }
        seenMessageHashes[hashKey] = now

        // Clean old hash cache
        if (seenMessageHashes.size > 200) {
            seenMessageHashes.entries.removeIf { now - it.value > 60000 }
        }

        // Financial & sensitive pattern detection
        val lowerText = text.lowercase()
        val isFinancialOrSensitive = lowerText.contains("₹") || 
            lowerText.contains("rs.") || 
            lowerText.contains("rs ") || 
            lowerText.contains("rupees") || 
            lowerText.contains("payment") || 
            lowerText.contains("paytm") || 
            lowerText.contains("gpay") || 
            lowerText.contains("phonepe") || 
            lowerText.contains("otp") || 
            lowerText.contains("password") || 
            lowerText.contains("pin") || 
            lowerText.contains("bank") || 
            lowerText.contains("account") || 
            lowerText.contains("bhej dena") || 
            lowerText.contains("transfer")

        val priority = when {
            commType == "phone_call" || commType == "whatsapp_call" -> "IMPORTANT"
            isFinancialOrSensitive -> "IMPORTANT"
            title.contains("group", ignoreCase = true) || title.contains("chat", ignoreCase = true) -> "NORMAL"
            else -> "NORMAL"
        }

        val comm = IncomingCommunication(
            id = "${sbn.id}_${now}",
            sender = if (title.isNotBlank()) title else "Someone",
            app = appName,
            packageName = packageName,
            type = commType,
            preview = text,
            timestamp = now,
            isSensitiveOrFinancial = isFinancialOrSensitive,
            priority = priority
        )

        // Store in history
        recentCommunications.add(0, comm)
        if (recentCommunications.size > MAX_HISTORY) {
            recentCommunications.removeAt(recentCommunications.size - 1)
        }

        // Trigger call announcer for incoming calls
        if (commType == "whatsapp_call") {
            CallAnnouncer.announceWhatsAppCall(context, comm.sender)
        } else if (commType == "phone_call") {
            CallAnnouncer.announceIncomingPhoneCall(context, comm.sender)
        }

        // Trigger live spoken announcement callback
        try {
            onNewCommunicationReceived?.invoke(comm)
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error in announcement callback", e)
        }

        return comm
    }

    fun getRecentSummary(): String {
        if (recentCommunications.isEmpty()) {
            return "No new notifications or communication events recorded recently."
        }
        val sb = StringBuilder()
        sb.append("Recent Incoming Communications (Latest first):\n")
        recentCommunications.take(8).forEachIndexed { index, item ->
            sb.append("${index + 1}. [${item.app}] from '${item.sender}': ")
            if (item.preview.isNotBlank()) {
                sb.append("\"${item.preview}\"")
            } else {
                sb.append("(Call / No preview text)")
            }
            if (item.isSensitiveOrFinancial) {
                sb.append(" [⚠️ SENSITIVE/FINANCIAL CONTENT]")
            }
            sb.append("\n")
        }
        return sb.toString().trim()
    }
}
