package io.github.demiterhugo.weatherflow.widget.glance

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import io.github.demiterhugo.weatherflow.widget.WeatherWidgetScheduler
import io.github.demiterhugo.weatherflow.widget.WeatherWidgetRegistry

class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override fun onAppWidgetOptionsChanged(
        context: Context, appWidgetManager: AppWidgetManager,
        appWidgetId: Int, newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        if (WeatherWidgetRegistry.hasRadarWidgets(context)) {
            WeatherWidgetScheduler.radarSizeChanged(context)
        }
    }

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
        WeatherWidgetScheduler.stopIfUnused(context)
    }
}
