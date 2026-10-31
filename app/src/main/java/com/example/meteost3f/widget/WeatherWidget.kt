package com.example.meteost3f.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.meteost3f.MainActivity
import com.example.meteost3f.data.ForecastCache
import com.example.meteost3f.data.LastLocationStore
import com.example.meteost3f.ui.toUiState

private val WidgetBackground = ColorProvider(day = Color(0xFFBEE3F8), night = Color(0xFF1B3A4B))
private val WidgetTextColor = ColorProvider(day = Color(0xFF123247), night = Color.White)

/**
 * Le widget lit uniquement l'état mis en cache (préférences Glance) : aucun appel réseau ici.
 * La récupération des données est déléguée à [WeatherWidgetWorker], qui n'est pas soumis
 * au délai d'exécution court imposé aux BroadcastReceiver des widgets.
 */
class WeatherWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        seedFromCacheIfEmpty(context, id)
        provideContent {
            WeatherWidgetContent()
        }
    }

    /**
     * Pré-remplit l'affichage à partir du cache local (lecture disque, pas de réseau) si le
     * widget n'a encore aucune donnée. Évite d'attendre le passage du WeatherWidgetWorker
     * (soumis à la latence de planification du système) alors qu'une prévision récente existe
     * déjà, p. ex. parce que l'appli vient d'être ouverte.
     */
    private suspend fun seedFromCacheIfEmpty(context: Context, id: GlanceId) {
        val existing = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        if (existing[WidgetKeys.TEMPERATURE] != null) return

        val saved = LastLocationStore(context).load() ?: return
        val forecast = ForecastCache(context).get(saved.latitude, saved.longitude) ?: return
        val state = forecast.toUiState(saved.label)

        updateAppWidgetState(context, id) { prefs -> writeWidgetPrefs(prefs, forecast, state) }
    }
}

@Composable
private fun WeatherWidgetContent() {
    val prefs = currentState<Preferences>()
    val location = prefs[WidgetKeys.LOCATION]
    val temperature = prefs[WidgetKeys.TEMPERATURE]
    val error = prefs[WidgetKeys.ERROR]

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetBackground)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        RefreshRow()
        Spacer(GlanceModifier.height(4.dp))
        when {
            location != null && temperature != null -> WeatherWidgetSuccess(
                location = location,
                temperature = temperature,
                description = prefs[WidgetKeys.DESCRIPTION].orEmpty(),
                emoji = prefs[WidgetKeys.EMOJI].orEmpty(),
                dailyLabels = prefs[WidgetKeys.DAILY_LABELS].orEmpty().split(WIDGET_DAILY_SEPARATOR),
                dailyEmojis = prefs[WidgetKeys.DAILY_EMOJIS].orEmpty().split(WIDGET_DAILY_SEPARATOR),
                dailyMin = prefs[WidgetKeys.DAILY_MIN].orEmpty().split(WIDGET_DAILY_SEPARATOR),
                dailyMax = prefs[WidgetKeys.DAILY_MAX].orEmpty().split(WIDGET_DAILY_SEPARATOR),
            )

            error != null -> Text(error, style = TextStyle(color = WidgetTextColor, fontSize = 12.sp))

            else -> Text("Chargement…", style = TextStyle(color = WidgetTextColor, fontSize = 12.sp))
        }
    }
}

@Composable
private fun RefreshRow() {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
        Spacer(GlanceModifier.defaultWeight())
        Text(
            "↻",
            style = TextStyle(color = WidgetTextColor, fontSize = 16.sp),
            modifier = GlanceModifier.clickable(actionRunCallback<RefreshAction>()),
        )
    }
}

@Composable
private fun WeatherWidgetSuccess(
    location: String,
    temperature: String,
    description: String,
    emoji: String,
    dailyLabels: List<String>,
    dailyEmojis: List<String>,
    dailyMin: List<String>,
    dailyMax: List<String>,
) {
    Text(
        location,
        style = TextStyle(color = WidgetTextColor, fontSize = 14.sp, fontWeight = FontWeight.Bold),
        maxLines = 1,
    )
    Spacer(GlanceModifier.height(6.dp))
    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
        Text(emoji, style = TextStyle(fontSize = 40.sp))
        Spacer(GlanceModifier.width(8.dp))
        Column {
            Text(
                temperature,
                style = TextStyle(color = WidgetTextColor, fontSize = 32.sp, fontWeight = FontWeight.Bold),
            )
            Text(
                description,
                style = TextStyle(color = WidgetTextColor, fontSize = 11.sp),
                maxLines = 1,
            )
        }
    }
    Spacer(GlanceModifier.height(8.dp))
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        for (i in dailyLabels.indices) {
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            ) {
                Text(
                    dailyLabels[i],
                    style = TextStyle(color = WidgetTextColor, fontSize = 11.sp),
                    maxLines = 1,
                )
                Text(dailyEmojis.getOrElse(i) { "" }, style = TextStyle(fontSize = 28.sp))
                Text(
                    "${dailyMin.getOrElse(i) { "" }}/${dailyMax.getOrElse(i) { "" }}",
                    style = TextStyle(color = WidgetTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }
        }
    }
}
