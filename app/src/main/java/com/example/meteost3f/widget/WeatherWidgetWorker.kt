package com.example.meteost3f.widget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.meteost3f.R
import com.example.meteost3f.data.LastLocationStore
import com.example.meteost3f.data.WeatherRepository
import com.example.meteost3f.data.model.WeatherResponse
import com.example.meteost3f.domain.WeatherCode
import com.example.meteost3f.ui.toUiState
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private const val NOTIFICATION_CHANNEL_ID = "weather_widget_refresh"
private const val NOTIFICATION_ID = 1

object WidgetKeys {
    val LOCATION = stringPreferencesKey("location")
    val TEMPERATURE = stringPreferencesKey("temperature")
    val DESCRIPTION = stringPreferencesKey("description")
    val EMOJI = stringPreferencesKey("emoji")
    val DAILY_LABELS = stringPreferencesKey("daily_labels")
    val DAILY_EMOJIS = stringPreferencesKey("daily_emojis")
    val DAILY_MIN = stringPreferencesKey("daily_min")
    val DAILY_MAX = stringPreferencesKey("daily_max")
    val ERROR = stringPreferencesKey("error")
}

const val WIDGET_DAILY_SEPARATOR = "|"

private const val DEFAULT_CITY = "Paris"

/**
 * Récupère la météo en tâche de fond, hors du délai d'exécution limité imposé aux
 * BroadcastReceiver des widgets (ce qui bloquait le widget en chargement sur les
 * appareils/réseaux lents, cf. Samsung A10).
 */
class WeatherWidgetWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = runCatching {
        doRefresh()
    }.getOrElse { Result.retry() }

    // Requis pour qu'une requête "expedited" puisse tourner en foreground service sur les
    // appareils pré-Android 12 (ex. Samsung A10), où elle sinon attend dans la file normale du
    // JobScheduler/Doze — c'est cette attente qui bloquait le widget sur "Chargement…".
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Rafraîchissement du widget météo",
                NotificationManager.IMPORTANCE_MIN,
            ),
        )
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Mise à jour de la météo")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private suspend fun doRefresh(): Result {
        val repository = WeatherRepository(applicationContext)
        val store = LastLocationStore(applicationContext)
        val glanceIds = GlanceAppWidgetManager(applicationContext).getGlanceIds(WeatherWidget::class.java)
        if (glanceIds.isEmpty()) return Result.success()

        val fetchResult = runCatching {
            val saved = store.load()
            val (latitude, longitude, label) = if (saved != null) {
                Triple(saved.latitude, saved.longitude, saved.label)
            } else {
                val city = repository.findCity(DEFAULT_CITY).firstOrNull()
                    ?: error("Ville introuvable")
                Triple(city.latitude, city.longitude, listOfNotNull(city.name, city.country).joinToString(", "))
            }
            val forecast = repository.loadForecast(latitude, longitude)
            forecast to forecast.toUiState(label)
        }

        glanceIds.forEach { glanceId ->
            updateAppWidgetState(applicationContext, glanceId) { prefs ->
                fetchResult.onSuccess { (forecast, state) -> writeWidgetPrefs(prefs, forecast, state) }
                fetchResult.onFailure { error ->
                    prefs[WidgetKeys.ERROR] = error.message ?: "Erreur réseau"
                }
            }
            WeatherWidget().update(applicationContext, glanceId)
        }

        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "weather_widget_refresh"
    }
}

internal fun writeWidgetPrefs(
    prefs: MutablePreferences,
    forecast: WeatherResponse,
    state: com.example.meteost3f.ui.WeatherUiState.Success,
) {
    val days = widgetDailySummaries(forecast)
    prefs[WidgetKeys.LOCATION] = state.locationName
    prefs[WidgetKeys.TEMPERATURE] = state.current.temperature
    prefs[WidgetKeys.DESCRIPTION] = state.current.description
    prefs[WidgetKeys.EMOJI] = state.current.emoji
    prefs[WidgetKeys.DAILY_LABELS] = days.joinToString(WIDGET_DAILY_SEPARATOR) { it.label }
    prefs[WidgetKeys.DAILY_EMOJIS] = days.joinToString(WIDGET_DAILY_SEPARATOR) { it.emoji }
    prefs[WidgetKeys.DAILY_MIN] = days.joinToString(WIDGET_DAILY_SEPARATOR) { it.temperatureMin }
    prefs[WidgetKeys.DAILY_MAX] = days.joinToString(WIDGET_DAILY_SEPARATOR) { it.temperatureMax }
    prefs.remove(WidgetKeys.ERROR)
}

internal data class WidgetDaySummary(
    val label: String,
    val emoji: String,
    val temperatureMin: String,
    val temperatureMax: String,
)

internal fun widgetDailySummaries(forecast: WeatherResponse): List<WidgetDaySummary> {
    val daily = forecast.daily
    return daily.time.indices.take(4).map { i ->
        val label = when (i) {
            0 -> "Aujourd'hui"
            1 -> "Demain"
            else -> LocalDate.parse(daily.time[i]).dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRENCH)
        }
        WidgetDaySummary(
            label = label,
            emoji = WeatherCode.emoji(daily.weatherCode[i]),
            temperatureMin = "${daily.temperatureMin[i].roundToInt()}°",
            temperatureMax = "${daily.temperatureMax[i].roundToInt()}°",
        )
    }
}
