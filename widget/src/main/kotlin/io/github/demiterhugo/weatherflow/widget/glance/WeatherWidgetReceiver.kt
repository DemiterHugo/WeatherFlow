package io.github.demiterhugo.weatherflow.widget.glance

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import io.github.demiterhugo.weatherflow.widget.WeatherWidgetScheduler

class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = WeatherGlanceWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WeatherWidgetScheduler.start(context)
        WeatherWidgetScheduler.refresh(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WeatherWidgetScheduler.start(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WeatherWidgetScheduler.stop(context)
    }
}
