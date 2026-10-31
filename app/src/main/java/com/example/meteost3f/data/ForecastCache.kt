package com.example.meteost3f.data

import android.content.Context
import com.example.meteost3f.data.model.WeatherResponse
import kotlin.math.abs
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val PREFS_NAME = "meteost3f_forecast_cache"
private const val KEY_LATITUDE = "latitude"
private const val KEY_LONGITUDE = "longitude"
private const val KEY_TIMESTAMP = "timestamp"
private const val KEY_FORECAST = "forecast"
private const val TTL_MILLIS = 15 * 60 * 1000L
private const val COORDINATE_EPSILON = 0.01

/**
 * Cache court (15 min) du dernier résultat de prévisions, partagé entre l'appli et le widget
 * (tous deux passent par [WeatherRepository]) pour éviter un appel réseau à chaque ouverture.
 */
class ForecastCache(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun get(latitude: Double, longitude: Double): WeatherResponse? {
        val cachedLat = prefs.getString(KEY_LATITUDE, null)?.toDoubleOrNull() ?: return null
        val cachedLon = prefs.getString(KEY_LONGITUDE, null)?.toDoubleOrNull() ?: return null
        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)
        val raw = prefs.getString(KEY_FORECAST, null) ?: return null

        val isSameLocation = abs(cachedLat - latitude) < COORDINATE_EPSILON &&
            abs(cachedLon - longitude) < COORDINATE_EPSILON
        val isFresh = System.currentTimeMillis() - timestamp < TTL_MILLIS
        if (!isSameLocation || !isFresh) return null

        return runCatching { json.decodeFromString<WeatherResponse>(raw) }.getOrNull()
    }

    fun put(latitude: Double, longitude: Double, forecast: WeatherResponse) {
        prefs.edit()
            .putString(KEY_LATITUDE, latitude.toString())
            .putString(KEY_LONGITUDE, longitude.toString())
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .putString(KEY_FORECAST, json.encodeToString(forecast))
            .apply()
    }
}
