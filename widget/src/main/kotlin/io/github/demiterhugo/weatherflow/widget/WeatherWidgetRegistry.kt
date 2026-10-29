package io.github.demiterhugo.weatherflow.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import io.github.demiterhugo.weatherflow.widget.glance.WeatherWidgetReceiver
import io.github.demiterhugo.weatherflow.widget.remote.RemoteWeatherWidgetReceiver

internal object WeatherWidgetRegistry {
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
