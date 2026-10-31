package com.example.meteost3f.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class LastLocation(
    val label: String,
    val latitude: Double,
    val longitude: Double,
)

/** Persiste la dernière position consultée (ville ou géolocalisation) sous forme de JSON. */
class LastLocationStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun save(location: LastLocation) {
        prefs.edit().putString(KEY_LAST_LOCATION, json.encodeToString(location)).apply()
    }

    fun load(): LastLocation? {
        val raw = prefs.getString(KEY_LAST_LOCATION, null) ?: return null
        return runCatching { json.decodeFromString<LastLocation>(raw) }.getOrNull()
    }

    private companion object {
        const val PREFS_NAME = "meteost3f_prefs"
        const val KEY_LAST_LOCATION = "last_location"
    }
}
