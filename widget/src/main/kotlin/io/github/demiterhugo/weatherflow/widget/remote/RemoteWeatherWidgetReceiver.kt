package io.github.demiterhugo.weatherflow.widget.remote

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import io.github.demiterhugo.weatherflow.widget.WeatherWidgetScheduler
import io.github.demiterhugo.weatherflow.widget.WeatherWidgetRegistry

/** The receiver only enqueues work; document capture and network access run in a worker. */
class RemoteWeatherWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (Build.VERSION.SDK_INT < 37) return
        WeatherWidgetScheduler.start(context)
        WeatherWidgetScheduler.refresh(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        if (Build.VERSION.SDK_INT >= 37) {
            WeatherWidgetScheduler.render(context)
            if (WeatherWidgetRegistry.hasRadarWidgets(context)) {
                WeatherWidgetScheduler.radarSizeChanged(context)
            }
        }
    }

    override fun onEnabled(context: Context) {
        if (Build.VERSION.SDK_INT >= 37) WeatherWidgetScheduler.start(context)
    }

    override fun onDisabled(context: Context) {
        WeatherWidgetScheduler.stopIfUnused(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH && Build.VERSION.SDK_INT >= 37) {
            WeatherWidgetScheduler.refresh(context)
        } else super.onReceive(context, intent)
    }

    companion object {
        internal const val ACTION_REFRESH = "io.github.demiterhugo.weatherflow.action.REFRESH_REMOTE_WEATHER"
    }
}
