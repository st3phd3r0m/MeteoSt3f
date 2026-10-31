package com.example.meteost3f.ui

sealed interface WeatherUiState {
    data object Loading : WeatherUiState

    data class Error(val message: String) : WeatherUiState

    data class Success(
        val locationName: String,
        val current: CurrentWeatherUi,
        val hourly: List<HourlyUi>,
        val daily: List<DailyUi>,
    ) : WeatherUiState
}

data class CurrentWeatherUi(
    val temperature: String,
    val apparentTemperature: String,
    val description: String,
    val emoji: String,
    val humidity: String,
    val wind: String,
    val sunrise: String,
    val sunset: String,
)

data class HourlyUi(
    val label: String,
    val temperature: String,
    val emoji: String,
)

data class DailyUi(
    val label: String,
    val emoji: String,
    val temperatureMin: String,
    val temperatureMax: String,
)
