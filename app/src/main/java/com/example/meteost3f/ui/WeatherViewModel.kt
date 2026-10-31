package com.example.meteost3f.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meteost3f.data.LastLocation
import com.example.meteost3f.data.LastLocationStore
import com.example.meteost3f.data.WeatherRepository
import com.example.meteost3f.data.fetchCurrentLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val DEFAULT_CITY = "Paris"

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WeatherRepository(application)
    private val locationStore = LastLocationStore(application)

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _cityQuery = MutableStateFlow("")
    val cityQuery: StateFlow<String> = _cityQuery.asStateFlow()

    private var lastAction: () -> Unit = { searchCity(DEFAULT_CITY) }

    init {
        val saved = locationStore.load()
        if (saved != null) {
            lastAction = { loadSavedLocation(saved) }
            loadSavedLocation(saved)
        } else {
            searchCity(DEFAULT_CITY)
        }
    }

    private fun loadSavedLocation(saved: LastLocation) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            loadForecast(saved.latitude, saved.longitude, saved.label)
        }
    }

    fun onCityQueryChange(query: String) {
        _cityQuery.value = query
    }

    fun onSearchSubmit() {
        val query = _cityQuery.value.trim()
        if (query.isNotEmpty()) searchCity(query)
    }

    fun retry() = lastAction()

    fun useCurrentLocation() {
        lastAction = { useCurrentLocation() }
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            val location = fetchCurrentLocation(getApplication())
            if (location == null) {
                _uiState.value = WeatherUiState.Error(
                    "Position indisponible. Vérifiez que la localisation est activée."
                )
                return@launch
            }
            val label = runCatching { repository.reverseGeocode(location.latitude, location.longitude) }
                .getOrNull() ?: "Ma position"
            loadForecast(location.latitude, location.longitude, label)
        }
    }

    private fun searchCity(query: String) {
        lastAction = { searchCity(query) }
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            val city = runCatching { repository.findCity(query) }.getOrNull()?.firstOrNull()
            if (city == null) {
                _uiState.value = WeatherUiState.Error("Ville introuvable : $query")
                return@launch
            }
            val label = listOfNotNull(city.name, city.country).joinToString(", ")
            loadForecast(city.latitude, city.longitude, label)
        }
    }

    private suspend fun loadForecast(latitude: Double, longitude: Double, label: String) {
        runCatching { repository.loadForecast(latitude, longitude) }
            .onSuccess {
                _uiState.value = it.toUiState(label)
                locationStore.save(LastLocation(label, latitude, longitude))
            }
            .onFailure { _uiState.value = WeatherUiState.Error(it.message ?: "Erreur réseau") }
    }
}
