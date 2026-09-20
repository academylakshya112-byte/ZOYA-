package com.example.memory

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

object MemoryManager {
    private const val PREFS_NAME = "ZoyaPermanentMemory"
    private const val KEY_FACTS = "saved_facts"
    private const val KEY_SKILLS = "learned_skills"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveFact(context: Context, topic: String, fact: String): String {
        val prefs = getPrefs(context)
        val rawFacts = prefs.getString(KEY_FACTS, "{}") ?: "{}"
        val json = try { JSONObject(rawFacts) } catch (e: Exception) { JSONObject() }
        json.put(topic.trim(), fact.trim())
        prefs.edit().putString(KEY_FACTS, json.toString()).apply()
        return "Saved to permanent memory: [$topic] -> $fact"
    }

    fun teachSkill(context: Context, trigger: String, actionOrRule: String): String {
        val prefs = getPrefs(context)
        val rawSkills = prefs.getString(KEY_SKILLS, "{}") ?: "{}"
        val json = try { JSONObject(rawSkills) } catch (e: Exception) { JSONObject() }
        json.put(trigger.trim().lowercase(), actionOrRule.trim())
        prefs.edit().putString(KEY_SKILLS, json.toString()).apply()
        return "Successfully learned new skill: When user says '${trigger.trim()}', execute: $actionOrRule"
    }

    fun getMemoriesSummary(context: Context): String {
        val prefs = getPrefs(context)
        val rawFacts = prefs.getString(KEY_FACTS, "{}") ?: "{}"
        val rawSkills = prefs.getString(KEY_SKILLS, "{}") ?: "{}"

        val factsJson = try { JSONObject(rawFacts) } catch (e: Exception) { JSONObject() }
        val skillsJson = try { JSONObject(rawSkills) } catch (e: Exception) { JSONObject() }

        if (factsJson.length() == 0 && skillsJson.length() == 0) {
            return "No custom memories or taught skills recorded yet."
        }

        val sb = StringBuilder()
        if (factsJson.length() > 0) {
            sb.append("FACTS & PERMANENT USER DATA:\n")
            val keys = factsJson.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                sb.append("- ").append(k).append(": ").append(factsJson.optString(k)).append("\n")
            }
        }

        if (skillsJson.length() > 0) {
            sb.append("\nLEARNED SKILLS & CUSTOM COMMAND TRIGGERS:\n")
            val keys = skillsJson.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                sb.append("- When user says '").append(k).append("': Action/Rule -> ").append(skillsJson.optString(k)).append("\n")
            }
        }

        return sb.toString().trim()
    }

    fun forgetMemory(context: Context, key: String): String {
        val prefs = getPrefs(context)
        val rawFacts = prefs.getString(KEY_FACTS, "{}") ?: "{}"
        val rawSkills = prefs.getString(KEY_SKILLS, "{}") ?: "{}"

        val factsJson = try { JSONObject(rawFacts) } catch (e: Exception) { JSONObject() }
        val skillsJson = try { JSONObject(rawSkills) } catch (e: Exception) { JSONObject() }

        var found = false
        val cleanKey = key.trim()
        if (factsJson.has(cleanKey)) {
            factsJson.remove(cleanKey)
            prefs.edit().putString(KEY_FACTS, factsJson.toString()).apply()
            found = true
        }

        val lowerKey = cleanKey.lowercase()
        if (skillsJson.has(lowerKey)) {
            skillsJson.remove(lowerKey)
            prefs.edit().putString(KEY_SKILLS, skillsJson.toString()).apply()
            found = true
        }

        return if (found) "Removed '$key' from permanent memory." else "Memory for '$key' was not found."
    }

    fun getAllFacts(context: Context): List<Pair<String, String>> {
        val prefs = getPrefs(context)
        val rawFacts = prefs.getString(KEY_FACTS, "{}") ?: "{}"
        val json = try { JSONObject(rawFacts) } catch (e: Exception) { JSONObject() }
        val list = mutableListOf<Pair<String, String>>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            list.add(k to json.optString(k))
        }
        return list
    }

    fun getAllSkills(context: Context): List<Pair<String, String>> {
        val prefs = getPrefs(context)
        val rawSkills = prefs.getString(KEY_SKILLS, "{}") ?: "{}"
        val json = try { JSONObject(rawSkills) } catch (e: Exception) { JSONObject() }
        val list = mutableListOf<Pair<String, String>>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            list.add(k to json.optString(k))
        }
        return list
    }
}
