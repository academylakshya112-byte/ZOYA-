package com.example.notification

import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.ZoyaForegroundService
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

object CallAnnouncer {
    private const val TAG = "CallAnnouncer"
    private const val PREFS_NAME = "ZoyaNotificationPrefs"

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var repeatingJob: Job? = null
    private val isCurrentlyRinging = AtomicBoolean(false)
    private var lastAnnouncedCallId: String? = null
    private var lastAnnouncementTime = 0L

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun init(context: Context) {
        if (tts != null) return
        val appContext = context.applicationContext
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                try {
                    // Try Hindi first, then Indian English, then default
                    val hiLocale = Locale("hi", "IN")
                    val result = tts?.setLanguage(hiLocale)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        val enInLocale = Locale("en", "IN")
                        val enResult = tts?.setLanguage(enInLocale)
                        if (enResult == TextToSpeech.LANG_MISSING_DATA || enResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                            tts?.language = Locale.getDefault()
                        }
                    }
                    tts?.setSpeechRate(0.95f)
                    tts?.setPitch(1.05f)
                    Log.i(TAG, "CallAnnouncer TTS initialized successfully.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting TTS language", e)
                }
            } else {
                Log.e(TAG, "TTS Initialization failed with status: $status")
            }
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isCallAnnouncementEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("call_announcement_enabled", true)
    }

    fun setCallAnnouncementEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("call_announcement_enabled", enabled).apply()
    }

    fun isPhoneCallAnnouncementEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("phone_call_announcement_enabled", true)
    }

    fun setPhoneCallAnnouncementEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("phone_call_announcement_enabled", enabled).apply()
    }

    fun isWhatsAppCallAnnouncementEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("whatsapp_call_announcement_enabled", true)
    }

    fun setWhatsAppCallAnnouncementEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("whatsapp_call_announcement_enabled", enabled).apply()
    }

    fun isMessageAnnouncementEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("message_announcement_enabled", true)
    }

    fun setMessageAnnouncementEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("message_announcement_enabled", enabled).apply()
    }

    fun isWhatsAppMessageAnnouncementEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("whatsapp_msg_announcement_enabled", true)
    }

    fun setWhatsAppMessageAnnouncementEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("whatsapp_msg_announcement_enabled", enabled).apply()
    }

    fun isSmsMessageAnnouncementEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("sms_msg_announcement_enabled", true)
    }

    fun setSmsMessageAnnouncementEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("sms_msg_announcement_enabled", enabled).apply()
    }

    fun isRepeatEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("call_announcement_repeat", true)
    }

    fun setRepeatEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("call_announcement_repeat", enabled).apply()
    }

    fun resolveContactNameFromNumber(context: Context, phoneNumber: String?): String {
        if (phoneNumber.isNullOrBlank()) return "Unknown Number"
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val name = it.getString(nameIndex)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up contact name for number: $phoneNumber", e)
        }
        // If no contact name found, format number cleanly
        val digits = phoneNumber.replace(Regex("[^0-9+]"), "")
        return if (digits.length >= 10) {
            "number ${digits.takeLast(5)}"
        } else {
            "Unknown Caller"
        }
    }

    fun announceIncomingPhoneCall(context: Context, phoneNumber: String?) {
        if (!isCallAnnouncementEnabled(context) || !isPhoneCallAnnouncementEnabled(context)) {
            Log.d(TAG, "Phone call announcement disabled in settings.")
            return
        }

        val callerName = resolveContactNameFromNumber(context, phoneNumber)
        startCallAnnouncement(context, callerName, isWhatsApp = false)
    }

    fun announceWhatsAppCall(context: Context, senderName: String) {
        if (!isCallAnnouncementEnabled(context) || !isWhatsAppCallAnnouncementEnabled(context)) {
            Log.d(TAG, "WhatsApp call announcement disabled in settings.")
            return
        }

        val cleanName = if (senderName.isBlank() || senderName.equals("WhatsApp", ignoreCase = true)) {
            "Someone"
        } else {
            senderName
        }

        startCallAnnouncement(context, cleanName, isWhatsApp = true)
    }

    private fun startCallAnnouncement(context: Context, callerName: String, isWhatsApp: Boolean) {
        val prefs = context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE)
        val bossName = prefs.getString("boss_name", "Boss") ?: "Boss"
        val privacyMode = NotificationManagerHelper.isPrivacyMode(context)

        val callKey = "${if (isWhatsApp) "WA" else "PHONE"}_$callerName"
        val now = System.currentTimeMillis()
        if (callKey == lastAnnouncedCallId && (now - lastAnnouncementTime) < 3000) {
            return
        }
        lastAnnouncedCallId = callKey
        lastAnnouncementTime = now

        isCurrentlyRinging.set(true)
        init(context)

        val appLanguage = prefs.getString("app_language", "Hinglish") ?: "Hinglish"
        val isBhojpuri = appLanguage.contains("Bhojpuri", ignoreCase = true)
        val appTypeLabel = if (isWhatsApp) "WhatsApp" else "Phone"

        val firstPhrase = if (isBhojpuri) {
            if (privacyMode) "$bossName, $appTypeLabel फोन आवत बाटे।"
            else "$bossName, $callerName के $appTypeLabel फोन आवत बाटे।"
        } else {
            if (privacyMode) "$bossName, incoming $appTypeLabel call aa raha hai."
            else "$bossName, $callerName ka $appTypeLabel call aa raha hai."
        }

        val repeatPhrase = if (isBhojpuri) {
            if (privacyMode) "फोन आवत बाटे मालिक।"
            else "$callerName के फोन आवत बाटे।"
        } else {
            if (privacyMode) "Incoming call aa raha hai."
            else "$callerName ka call aa raha hai."
        }

        repeatingJob?.cancel()
        repeatingJob = scope.launch {
            // First instant announcement
            speakPhrase(firstPhrase)
            
            // Notify live service if running
            try {
                ZoyaForegroundService.activeService?.sendTextMessage(
                    "[SYSTEM EVENT: Incoming $appTypeLabel call from '$callerName'. Boss address: $bossName]"
                )
            } catch (e: Exception) {}

            val shouldRepeat = isRepeatEnabled(context)
            if (shouldRepeat) {
                while (isActive && isCurrentlyRinging.get()) {
                    delay(4500)
                    if (isCurrentlyRinging.get()) {
                        speakPhrase(repeatPhrase)
                    }
                }
            }
        }
    }

    fun stopAnnouncement() {
        isCurrentlyRinging.set(false)
        repeatingJob?.cancel()
        repeatingJob = null
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
    }

    private fun speakPhrase(text: String) {
        com.example.core.VoiceOutputManager.speak(text, com.example.core.SpeechPriority.CALL_ANNOUNCEMENT)
        Log.d(TAG, "Routed call announcement to VoiceOutputManager: $text")
    }

    fun speakText(text: String) {
        com.example.core.VoiceOutputManager.speak(text, com.example.core.SpeechPriority.NORMAL)
    }

    fun stopSpeaking() {
        com.example.core.VoiceOutputManager.stopSpeaking()
    }

    fun announceIncomingMessage(context: Context, senderName: String, messageText: String, isWhatsApp: Boolean) {
        if (!isMessageAnnouncementEnabled(context)) {
            Log.d(TAG, "Message announcement disabled globally in settings.")
            return
        }
        if (isWhatsApp && !isWhatsAppMessageAnnouncementEnabled(context)) {
            Log.d(TAG, "WhatsApp message announcement disabled in settings.")
            return
        }
        if (!isWhatsApp && !isSmsMessageAnnouncementEnabled(context)) {
            Log.d(TAG, "SMS message announcement disabled in settings.")
            return
        }

        val prefs = context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE)
        val bossName = prefs.getString("boss_name", "Boss") ?: "Boss"
        val privacyMode = NotificationManagerHelper.isPrivacyMode(context)
        val previewEnabled = NotificationManagerHelper.isMessagePreviewEnabled(context)

        val cleanSender = if (senderName.isBlank() || senderName.equals("WhatsApp", ignoreCase = true) || senderName.equals("Messages", ignoreCase = true)) {
            "Someone"
        } else {
            senderName.trim()
        }

        val msgKey = "${if (isWhatsApp) "WA_MSG" else "SMS"}_${cleanSender}_$messageText"
        val now = System.currentTimeMillis()
        if (msgKey == lastAnnouncedCallId && (now - lastAnnouncementTime) < 4000) {
            return
        }
        lastAnnouncedCallId = msgKey
        lastAnnouncementTime = now

        init(context)

        val appLanguage = prefs.getString("app_language", "Hinglish") ?: "Hinglish"
        val isBhojpuri = appLanguage.contains("Bhojpuri", ignoreCase = true)
        val appLabel = if (isWhatsApp) "WhatsApp" else "SMS"

        val phrase = if (isBhojpuri) {
            when {
                privacyMode -> "$bossName, $appLabel पर नया संदेश आइल बा। का जवाब दे दीं?"
                !previewEnabled || messageText.isBlank() -> "$bossName, $cleanSender के $appLabel संदेश आइल बा। का जवाब दे दीं?"
                else -> "$bossName, $cleanSender के $appLabel संदेश आइल बा: '$messageText'। का जवाब दे दीं?"
            }
        } else {
            when {
                privacyMode -> "$bossName, $appLabel par naya message aaya hai. Kya reply de du?"
                !previewEnabled || messageText.isBlank() -> "$bossName, $cleanSender ka $appLabel message aaya hai. Kya reply de du?"
                else -> "$bossName, $cleanSender ka $appLabel message aaya hai: '$messageText'. Kya reply de du?"
            }
        }

        scope.launch {
            delay(150)
            speakPhrase(phrase)

            // Inject prompt to live session
            try {
                ZoyaForegroundService.activeService?.sendTextMessage(
                    "[SYSTEM NOTIFICATION: New $appLabel message from '$cleanSender': \"$messageText\". If boss asks you to reply, handle it, or chat back (e.g. 'tum ise handle kar lo' / 'reply kar do'), use replyToLatestNotification or sendWhatsAppMessage/sendSmsMessage to execute.]"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying live session", e)
            }
        }
    }

    fun testAnnouncement(context: Context, isWhatsApp: Boolean = false) {
        val prefs = context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE)
        val bossName = prefs.getString("boss_name", "Boss") ?: "Boss"
        val testName = if (isWhatsApp) "Rahul" else "Kamlesh Sir"
        val phrase = if (isWhatsApp) {
            "$bossName, $testName ka WhatsApp call aa raha hai."
        } else {
            "$bossName, $testName ka call aa raha hai."
        }
        init(context)
        scope.launch {
            delay(200)
            speakPhrase(phrase)
        }
    }

    fun testMessageAnnouncement(context: Context, isWhatsApp: Boolean = true) {
        val prefs = context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE)
        val bossName = prefs.getString("boss_name", "Boss") ?: "Boss"
        val testName = if (isWhatsApp) "Kamlesh Sir" else "Rahul"
        val testMsg = if (isWhatsApp) "Hello boss, kal coaching aana hai" else "OTP 482910 for login"
        val phrase = if (isWhatsApp) {
            "$bossName, $testName ka WhatsApp message aaya hai: '$testMsg'. Kya reply de du?"
        } else {
            "$bossName, $testName ka SMS message aaya hai: '$testMsg'. Kya reply de du?"
        }
        init(context)
        scope.launch {
            delay(200)
            speakPhrase(phrase)
        }
    }
}
