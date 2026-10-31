package com.example.meteost3f.data

import android.content.Context
import com.example.meteost3f.data.model.GeocodingResult
import com.example.meteost3f.data.model.WeatherResponse

class WeatherRepository(
    context: Context,
    private val api: OpenMeteoApi = OpenMeteoApi(),
) {
    private val cache = ForecastCache(context)

    suspend fun findCity(query: String): List<GeocodingResult> = api.searchCity(query)

    suspend fun loadForecast(latitude: Double, longitude: Double): WeatherResponse {
        cache.get(latitude, longitude)?.let { return it }
        val forecast = api.getForecast(latitude, longitude)
        cache.put(latitude, longitude, forecast)
        return forecast
    }

    suspend fun reverseGeocode(latitude: Double, longitude: Double): String? =
        api.reverseGeocode(latitude, longitude)
}
