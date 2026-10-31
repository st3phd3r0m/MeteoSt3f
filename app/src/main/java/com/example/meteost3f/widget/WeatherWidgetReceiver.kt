package com.example.meteost3f.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager

class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        enqueueRefresh(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        enqueueRefresh(context)
    }

    private fun enqueueRefresh(context: Context) {
        // KEEP : ne pas annuler un fetch déjà en cours. onUpdate et onEnabled se déclenchent
        // souvent l'un après l'autre ; REPLACE tuait le premier (WorkerStoppedException),
        // ce qui pouvait boucler indéfiniment sur un appareil lent et bloquer le widget
        // sur "Chargement…".
        WorkManager.getInstance(context).enqueueUniqueWork(
            WeatherWidgetWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<WeatherWidgetWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build(),
        )
    }
}
