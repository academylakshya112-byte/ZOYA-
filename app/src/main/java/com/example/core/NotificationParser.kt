package com.example.core

import android.content.Context
import com.example.notification.NotificationManagerHelper

data class ParsedNotification(
    val id: String,
    val packageName: String,
    val appLabel: String,
    val sender: String,
    val messageText: String,
    val isWhatsApp: Boolean,
    val isSms: Boolean,
    val shouldAnnounce: Boolean
)

object NotificationParser {

    private val JUNK_PATTERNS = listOf(
        "checking for new messages",
        "whatsapp web is currently active",
        "backup in progress",
        "finished backup",
        "running in the background",
        "tap for more info"
    )

    fun parse(
        context: Context,
        id: String,
        packageName: String,
        title: String?,
        text: String?
    ): ParsedNotification {
        val isWa = packageName.contains("whatsapp", ignoreCase = true)
        val isSms = packageName.contains("messaging", ignoreCase = true) ||
                    packageName.contains("mms", ignoreCase = true) ||
                    packageName.contains("sms", ignoreCase = true)

        val appLabel = if (isWa) "WhatsApp" else if (isSms) "SMS" else "App"

        val rawTitle = title?.trim() ?: ""
        val rawText = text?.trim() ?: ""

        val isJunk = JUNK_PATTERNS.any {
            rawText.contains(it, ignoreCase = true) || rawTitle.contains(it, ignoreCase = true)
        }

        val sender = if (rawTitle.isBlank() || rawTitle.equals("WhatsApp", ignoreCase = true) || rawTitle.equals("Messages", ignoreCase = true)) {
            "Someone"
        } else {
            rawTitle
        }

        return ParsedNotification(
            id = id,
            packageName = packageName,
            appLabel = appLabel,
            sender = sender,
            messageText = rawText,
            isWhatsApp = isWa,
            isSms = isSms,
            shouldAnnounce = !isJunk && (isWa || isSms)
        )
    }

    fun buildAnnouncementText(
        context: Context,
        notification: ParsedNotification
    ): String {
        val bossName = SettingsManager.getBossName(context)
        val privacyMode = NotificationManagerHelper.isPrivacyMode(context)
        val previewEnabled = NotificationManagerHelper.isMessagePreviewEnabled(context)

        return when {
            privacyMode -> "$bossName, ${notification.appLabel} पर नया संदेश आया है।"
            !previewEnabled || notification.messageText.isBlank() -> "$bossName, ${notification.sender} का ${notification.appLabel} संदेश आया है।"
            else -> "$bossName, ${notification.sender} का ${notification.appLabel} संदेश: '${notification.messageText}'"
        }
    }
}
