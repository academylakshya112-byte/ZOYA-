package com.example.core

sealed class AssistantIntent(val permission: AssistantPermission) {
    data class OpenApp(val appName: String) : AssistantIntent(AssistantPermission.OPEN_APPS)
    data class YouTubePlay(val query: String) : AssistantIntent(AssistantPermission.YOUTUBE)
    data class YouTubeNav(val action: String) : AssistantIntent(AssistantPermission.YOUTUBE)
    data class SendWhatsApp(val contact: String, val message: String) : AssistantIntent(AssistantPermission.WHATSAPP)
    data class SendSms(val contact: String, val message: String) : AssistantIntent(AssistantPermission.SMS)
    data class MakeCall(val contact: String) : AssistantIntent(AssistantPermission.CALLS)
    data class AnswerCall(val isAccept: Boolean) : AssistantIntent(AssistantPermission.CALLS)
    data class ScreenTap(val targetText: String) : AssistantIntent(AssistantPermission.SCREEN_CONTROL)
    data class ScreenScroll(val direction: String) : AssistantIntent(AssistantPermission.SCREEN_CONTROL)
    data class DeviceLock(val requested: Boolean = true) : AssistantIntent(AssistantPermission.DEVICE_LOCK)
    data class DeviceUnlock(val requested: Boolean = true) : AssistantIntent(AssistantPermission.DEVICE_LOCK)
    data class BuildWebsite(val topic: String) : AssistantIntent(AssistantPermission.OPEN_APPS)
    data class SecurityConfig(val settingKey: String, val value: Any) : AssistantIntent(AssistantPermission.SECURITY)
    data class VoiceConfig(val action: String) : AssistantIntent(AssistantPermission.VOICE_MANAGEMENT)
    data class GeneralChat(val query: String) : AssistantIntent(AssistantPermission.CHAT)
}

object IntentManager {

    fun parse(command: String): AssistantIntent {
        val lower = command.lowercase().trim()

        return when {
            lower.contains("website bana") || lower.contains("web site bana") || lower.contains("build website") || lower.contains("create website") -> {
                AssistantIntent.BuildWebsite(command)
            }
            lower.contains("unlock karo") || lower.contains("unlock the phone") || lower.contains("phone unlock") || lower.contains("screen unlock") -> {
                AssistantIntent.DeviceUnlock()
            }
            lower.contains("lock karo") || lower.contains("lock the phone") || lower.contains("phone lock") || lower.contains("screen lock") -> {
                AssistantIntent.DeviceLock()
            }
            lower.contains("youtube") && (lower.contains("play") || lower.contains("chalao") || lower.contains("search") || lower.contains("song")) -> {
                val query = lower.replace(Regex(".*(play|chalao|song)\\s*"), "").trim()
                AssistantIntent.YouTubePlay(if (query.isBlank()) "Latest Hindi Songs" else query)
            }
            lower.contains("scroll") -> {
                val dir = if (lower.contains("up") || lower.contains("upar")) "up" else "down"
                AssistantIntent.ScreenScroll(dir)
            }
            lower.contains("whatsapp") && (lower.contains("message") || lower.contains("bhejo") || lower.contains("send")) -> {
                AssistantIntent.SendWhatsApp("", "")
            }
            lower.contains("call uthao") || lower.contains("answer call") || lower.contains("pick up") -> {
                AssistantIntent.AnswerCall(true)
            }
            lower.contains("call kaat") || lower.contains("reject call") || lower.contains("decline") -> {
                AssistantIntent.AnswerCall(false)
            }
            lower.contains("call") || lower.contains("phone milao") -> {
                val target = lower.replace(Regex(".*(call|phone milao)\\s*"), "").trim()
                AssistantIntent.MakeCall(target)
            }
            lower.contains("kholo") || lower.contains("open") -> {
                val app = lower.replace(Regex(".*(kholo|open)\\s*"), "").replace("app", "").trim()
                AssistantIntent.OpenApp(app)
            }
            lower.contains("tap karo") || lower.contains("click karo") -> {
                val target = lower.replace(Regex(".*(tap karo|click karo)\\s*"), "").trim()
                AssistantIntent.ScreenTap(target)
            }
            else -> AssistantIntent.GeneralChat(command)
        }
    }
}
