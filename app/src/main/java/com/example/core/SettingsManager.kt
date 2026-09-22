package com.example.core

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {
    private const val PREFS_NAME = "ZoyaPrefs"
    private const val KEY_VOICE_GUARDIAN = "voice_guardian_enabled"
    private const val KEY_UNKNOWN_VOICE_LOCK = "unknown_voice_lock_enabled"
    private const val KEY_UNAUTHORIZED_ATTEMPTS = "unauthorized_attempts_limit"
    private const val KEY_BOSS_NAME = "boss_name"
    private const val KEY_ASSISTANT_NAME = "assistant_name"
    private const val KEY_COUNTRY_CODE = "country_code"
    private const val KEY_APP_LANGUAGE = "app_language"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_CONFIRMATION_MODE = "confirmation_mode"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isVoiceGuardianEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_VOICE_GUARDIAN, false)
    }

    fun setVoiceGuardianEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_VOICE_GUARDIAN, enabled).apply()
    }

    fun isUnknownVoiceLockEnabled(context: Context): Boolean {
        // Unknown Voice Lock works ONLY when Voice Guardian is ON
        val guardianOn = isVoiceGuardianEnabled(context)
        return guardianOn && getPrefs(context).getBoolean(KEY_UNKNOWN_VOICE_LOCK, false)
    }

    fun setUnknownVoiceLockEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_UNKNOWN_VOICE_LOCK, enabled).apply()
    }

    fun getUnauthorizedAttemptsLimit(context: Context): Int {
        return getPrefs(context).getInt(KEY_UNAUTHORIZED_ATTEMPTS, 2)
    }

    fun setUnauthorizedAttemptsLimit(context: Context, limit: Int) {
        val validLimit = when (limit) {
            1, 2, 3, 5 -> limit
            else -> 2
        }
        getPrefs(context).edit().putInt(KEY_UNAUTHORIZED_ATTEMPTS, validLimit).apply()
    }

    fun getBossName(context: Context): String {
        return getPrefs(context).getString(KEY_BOSS_NAME, "Boss") ?: "Boss"
    }

    fun setBossName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_BOSS_NAME, name).apply()
    }

    fun getAssistantName(context: Context): String {
        return getPrefs(context).getString(KEY_ASSISTANT_NAME, "MAYA") ?: "MAYA"
    }

    fun setAssistantName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_ASSISTANT_NAME, name).apply()
    }

    fun getCountryCode(context: Context): String {
        return getPrefs(context).getString(KEY_COUNTRY_CODE, "India (+91)") ?: "India (+91)"
    }

    fun setCountryCode(context: Context, code: String) {
        getPrefs(context).edit().putString(KEY_COUNTRY_CODE, code).apply()
    }

    fun getAppLanguage(context: Context): String {
        return getPrefs(context).getString(KEY_APP_LANGUAGE, "Hinglish (Hindi + English) — default") ?: "Hinglish"
    }

    fun getApiKey(context: Context): String {
        return getPrefs(context).getString(KEY_API_KEY, "") ?: ""
    }

    fun setApiKey(context: Context, key: String) {
        getPrefs(context).edit().putString(KEY_API_KEY, key.trim()).apply()
    }

    fun isConfirmationMode(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_CONFIRMATION_MODE, false)
    }

    fun setConfirmationMode(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_CONFIRMATION_MODE, enabled).apply()
    }
}
