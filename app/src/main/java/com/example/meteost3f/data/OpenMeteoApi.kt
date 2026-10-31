package com.example.meteost3f.data

import com.example.meteost3f.data.model.GeocodingResponse
import com.example.meteost3f.data.model.GeocodingResult
import com.example.meteost3f.data.model.ReverseGeocodingResponse
import com.example.meteost3f.data.model.WeatherResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class OpenMeteoApi(
    private val httpClient: HttpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    },
) {
    suspend fun searchCity(query: String): List<GeocodingResult> {
        val response: GeocodingResponse = httpClient
            .get("https://geocoding-api.open-meteo.com/v1/search") {
                parameter("name", query)
                parameter("count", 5)
                parameter("language", "fr")
                parameter("format", "json")
            }
            .body()
        return response.results.orEmpty()
    }

    suspend fun getForecast(latitude: Double, longitude: Double): WeatherResponse =
        httpClient
            .get("https://api.open-meteo.com/v1/forecast") {
                parameter("latitude", latitude)
                parameter("longitude", longitude)
                parameter("current", "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,is_day")
                parameter("hourly", "temperature_2m,weather_code,is_day")
                parameter("daily", "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset")
                parameter("timezone", "auto")
                parameter("forecast_days", 14)
            }
            .body()

    /** Reverse geocoding via l'API publique Nominatim (OpenStreetMap). */
    suspend fun reverseGeocode(latitude: Double, longitude: Double): String? {
        val response: ReverseGeocodingResponse = httpClient
            .get("https://nominatim.openstreetmap.org/reverse") {
                header(HttpHeaders.UserAgent, "MeteoSt3f-AndroidApp/1.0")
                parameter("format", "jsonv2")
                parameter("lat", latitude)
                parameter("lon", longitude)
                parameter("accept-language", "fr")
                parameter("zoom", 10)
            }
            .body()

        val address = response.address ?: return null
        val place = address.city ?: address.town ?: address.village ?: address.municipality ?: address.county
        return listOfNotNull(place, address.country).joinToString(", ").ifBlank { null }
    }
}
