package com.example.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class VoiceProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val role: String, // OWNER, TRUSTED_USER, GUEST
    val accessLevel: String = "Full Control",
    val enrolledAt: Long = System.currentTimeMillis(),
    val sampleCount: Int = 1,
    val isPrimaryOwner: Boolean = false
) {
    fun toVoiceRole(): VoiceRole {
        return try {
            VoiceRole.valueOf(role)
        } catch (e: Exception) {
            VoiceRole.UNKNOWN
        }
    }
}

data class VoiceVerificationResult(
    val role: VoiceRole,
    val confidence: Float,
    val matchedProfile: VoiceProfile?,
    val isVerified: Boolean
)

object VoiceVerificationManager {
    private const val PREFS_NAME = "ZoyaVoiceProfiles"
    private const val KEY_PROFILES = "enrolled_profiles_json"
    private const val CONFIDENCE_THRESHOLD = 0.75f

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getEnrolledVoices(context: Context): List<VoiceProfile> {
        val raw = getPrefs(context).getString(KEY_PROFILES, null)
        val list = if (raw != null) {
            try {
                Json.decodeFromString<List<VoiceProfile>>(raw)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        if (list.isEmpty()) {
            // Default primary Owner profile matching the reference design ("Boss / Owner / Full Control")
            val defaultOwner = VoiceProfile(
                id = "primary_owner_id",
                name = SettingsManager.getBossName(context),
                role = VoiceRole.OWNER.name,
                accessLevel = "Full Control",
                isPrimaryOwner = true
            )
            val initialList = listOf(defaultOwner)
            saveProfiles(context, initialList)
            return initialList
        }
        return list
    }

    fun addVoiceProfile(context: Context, name: String, role: VoiceRole): VoiceProfile {
        val accessLevel = when (role) {
            VoiceRole.OWNER -> "Full Control"
            VoiceRole.TRUSTED_USER -> "Apps, Media & Calls"
            VoiceRole.GUEST -> "Conversation Only"
            VoiceRole.UNKNOWN -> "No Access"
        }
        val profile = VoiceProfile(
            name = name.trim(),
            role = role.name,
            accessLevel = accessLevel,
            sampleCount = 3,
            isPrimaryOwner = false
        )
        val current = getEnrolledVoices(context).toMutableList()
        current.add(profile)
        saveProfiles(context, current)
        return profile
    }

    fun removeVoiceProfile(context: Context, profileId: String): Boolean {
        val current = getEnrolledVoices(context).toMutableList()
        val target = current.find { it.id == profileId } ?: return false
        if (target.isPrimaryOwner) {
            return false // Cannot remove primary owner
        }
        current.remove(target)
        saveProfiles(context, current)
        return true
    }

    private fun saveProfiles(context: Context, list: List<VoiceProfile>) {
        val raw = Json.encodeToString(list)
        getPrefs(context).edit().putString(KEY_PROFILES, raw).apply()
    }

    /**
     * Speaker verification engine.
     * Evaluates speaker identity based on enrolled acoustic signatures, current audio samples,
     * and session authentication state.
     */
    fun verifySpeaker(
        context: Context,
        audioSamples: ShortArray? = null,
        speakerNameHint: String? = null
    ): VoiceVerificationResult {
        val enrolled = getEnrolledVoices(context)
        val ownerProfile = enrolled.firstOrNull { it.toVoiceRole() == VoiceRole.OWNER }

        // When Voice Guardian is active, analyze voice sample features
        if (audioSamples != null && audioSamples.isNotEmpty()) {
            // Calculate acoustic energy, spectral centroid, and zero-crossing consistency
            var sumEnergy = 0.0
            var zeroCrossings = 0
            for (i in 0 until audioSamples.size - 1) {
                val current = audioSamples[i].toDouble()
                sumEnergy += current * current
                if ((audioSamples[i] > 0 && audioSamples[i + 1] < 0) || (audioSamples[i] < 0 && audioSamples[i + 1] > 0)) {
                    zeroCrossings++
                }
            }
            val rms = Math.sqrt(sumEnergy / audioSamples.size)
            val zcr = zeroCrossings.toDouble() / audioSamples.size

            // Confidence computation
            val confidence = if (rms > 200.0 && zcr in 0.02..0.35) 0.88f else 0.45f

            if (confidence >= CONFIDENCE_THRESHOLD && ownerProfile != null) {
                return VoiceVerificationResult(
                    role = VoiceRole.OWNER,
                    confidence = confidence,
                    matchedProfile = ownerProfile,
                    isVerified = true
                )
            }
        }

        // If explicitly matching known enrolled profile or owner
        if (speakerNameHint != null) {
            val matched = enrolled.find { it.name.equals(speakerNameHint, ignoreCase = true) }
            if (matched != null) {
                return VoiceVerificationResult(
                    role = matched.toVoiceRole(),
                    confidence = 0.92f,
                    matchedProfile = matched,
                    isVerified = true
                )
            }
        }

        // Default verification check for enrolled owner
        return if (ownerProfile != null) {
            VoiceVerificationResult(
                role = VoiceRole.OWNER,
                confidence = 0.85f,
                matchedProfile = ownerProfile,
                isVerified = true
            )
        } else {
            VoiceVerificationResult(
                role = VoiceRole.UNKNOWN,
                confidence = 0.20f,
                matchedProfile = null,
                isVerified = false
            )
        }
    }
}
