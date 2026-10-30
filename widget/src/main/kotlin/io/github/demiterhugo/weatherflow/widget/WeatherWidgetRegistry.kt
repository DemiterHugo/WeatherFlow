package io.github.demiterhugo.weatherflow.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import io.github.demiterhugo.weatherflow.widget.glance.WeatherWidgetReceiver
import io.github.demiterhugo.weatherflow.widget.remote.RemoteWeatherWidgetReceiver
import io.github.demiterhugo.weatherflow.widget.radar.WidgetRadarSize

internal object WeatherWidgetRegistry {
    fun hasRadarWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val glance = manager.getAppWidgetIds(ComponentName(context, WeatherWidgetReceiver::class.java))
        return (glance + remoteIds(context)).any { id ->
            val options = manager.getAppWidgetOptions(id)
            val landscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val width = options.getInt(if (landscape) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val height = options.getInt(if (landscape) AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            WidgetRadarSize.fits(width, height)
        }
    }

    fun hasWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        return manager.getAppWidgetIds(ComponentName(context, WeatherWidgetReceiver::class.java)).isNotEmpty() ||
            (Build.VERSION.SDK_INT >= 37 && remoteIds(context).isNotEmpty())
    }

    fun remoteIds(context: Context): IntArray =
        if (Build.VERSION.SDK_INT >= 37) {
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, RemoteWeatherWidgetReceiver::class.java))
        } else intArrayOf()
}
