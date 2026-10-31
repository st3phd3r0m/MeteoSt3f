package com.example.meteost3f.ui

import com.example.meteost3f.data.model.WeatherResponse
import com.example.meteost3f.domain.WeatherCode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

internal fun WeatherResponse.toUiState(locationName: String): WeatherUiState.Success {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val currentUi = CurrentWeatherUi(
        temperature = "${current.temperature.roundToInt()}°",
        apparentTemperature = "Ressenti ${current.apparentTemperature.roundToInt()}°",
        description = WeatherCode.describe(current.weatherCode),
        emoji = WeatherCode.emoji(current.weatherCode),
        humidity = "${current.humidity}% humidité",
        wind = "${current.windSpeed.roundToInt()} km/h",
        sunrise = daily.sunrise.firstOrNull()?.let { LocalDateTime.parse(it).format(timeFormatter) } ?: "--:--",
        sunset = daily.sunset.firstOrNull()?.let { LocalDateTime.parse(it).format(timeFormatter) } ?: "--:--",
    )

    val startIndex = hourly.time.indexOfFirst { it >= current.time }.let { if (it == -1) 0 else it }
    val hourFormatter = DateTimeFormatter.ofPattern("HH'h'")
    val hourlyUi = (startIndex until minOf(startIndex + 24, hourly.time.size)).map { i ->
        HourlyUi(
            label = LocalDateTime.parse(hourly.time[i]).format(hourFormatter),
            temperature = "${hourly.temperature[i].roundToInt()}°",
            emoji = WeatherCode.emoji(hourly.weatherCode[i]),
        )
    }

    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM")
    val dailyUi = daily.time.indices.map { i ->
        val date = LocalDate.parse(daily.time[i])
        val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.FRENCH)
            .replaceFirstChar { it.uppercase() }
        DailyUi(
            label = "$dayName ${date.format(dateFormatter)}",
            emoji = WeatherCode.emoji(daily.weatherCode[i]),
            temperatureMin = "${daily.temperatureMin[i].roundToInt()}°",
            temperatureMax = "${daily.temperatureMax[i].roundToInt()}°",
        )
    }

    return WeatherUiState.Success(
        locationName = locationName,
        current = currentUi,
        hourly = hourlyUi,
        daily = dailyUi,
    )
}
