package com.example.core

import android.content.Context
import android.util.Log

object AssistantNotificationManager {
    private const val TAG = "AssistantNotification"

    private val seenHashes = mutableMapOf<String, Long>()

    fun onNotificationReceived(
        context: Context,
        id: String,
        packageName: String,
        title: String?,
        text: String?
    ) {
        val parsed = NotificationParser.parse(context, id, packageName, title, text)
        if (!parsed.shouldAnnounce) return

        // Deduplication
        val hash = "${parsed.packageName}_${parsed.sender}_${parsed.messageText}"
        val now = System.currentTimeMillis()
        val lastSeen = seenHashes[hash] ?: 0L
        if (now - lastSeen < 4000) {
            return
        }
        seenHashes[hash] = now

        // Cleanup old hashes
        if (seenHashes.size > 100) {
            seenHashes.entries.removeAll { now - it.value > 60000 }
        }

        EventManager.publish(
            AssistantEvent.NotificationEvent(
                id = id,
                app = parsed.appLabel,
                sender = parsed.sender,
                messageText = parsed.messageText
            )
        )

        val announcement = NotificationParser.buildAnnouncementText(context, parsed)
        VoiceOutputManager.speak(announcement, SpeechPriority.NOTIFICATION)
    }
}
