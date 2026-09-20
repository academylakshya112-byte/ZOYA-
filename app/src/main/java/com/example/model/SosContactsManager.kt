package com.example.model

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SosContact(
    val name: String,
    val phone: String
)

object SosContactsManager {
    private const val PREFS_NAME = "ZoyaSosPrefs"
    private const val KEY_CONTACTS = "sos_contacts_list"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSosContacts(context: Context): List<SosContact> {
        val raw = getPrefs(context).getString(KEY_CONTACTS, null) ?: return emptyList()
        return try {
            Json.decodeFromString<List<SosContact>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addSosContact(context: Context, contact: SosContact) {
        val current = getSosContacts(context).toMutableList()
        current.removeAll { it.phone == contact.phone || it.name.equals(contact.name, ignoreCase = true) }
        current.add(contact)
        saveList(context, current)
    }

    fun removeSosContact(context: Context, phone: String) {
        val current = getSosContacts(context).toMutableList()
        current.removeAll { it.phone == phone }
        saveList(context, current)
    }

    private fun saveList(context: Context, list: List<SosContact>) {
        val jsonStr = Json.encodeToString(list)
        getPrefs(context).edit().putString(KEY_CONTACTS, jsonStr).apply()
    }
}
