package io.github.demiterhugo.weatherflow.widget.remote

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import io.github.demiterhugo.weatherflow.widget.R
import io.github.demiterhugo.weatherflow.widget.WeatherWidgetRegistry
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetWeatherText
import io.github.demiterhugo.weatherflow.widget.radar.WidgetRadarUi
import kotlinx.coroutines.CancellationException

@RequiresApi(37)
internal object RemoteWeatherRenderer {
    suspend fun renderAll(context: Context, text: WidgetWeatherText, radar: WidgetRadarUi? = null) {
        val manager = AppWidgetManager.getInstance(context)
        WeatherWidgetRegistry.remoteIds(context).forEach { id ->
            val actions = RemoteWeatherActions.create(context, id)
            try {
                val options = manager.getAppWidgetOptions(id)
                val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                val widthKey = if (landscape) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
                val heightKey = if (landscape) AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
                val document = RemoteWeatherDocument.capture(
                    context, text, actions,
                    widthDp = options.getInt(widthKey, 180).coerceAtLeast(180),
                    heightDp = options.getInt(heightKey, 220).coerceAtLeast(220),
                    radar = radar,
                )
                manager.updateAppWidget(id, RemoteWeatherDocument.toRemoteViews(document))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // A document failure must not prevent Glance from displaying refreshed weather.
                Log.e("WeatherFlowRemote", "Unable to create the experimental widget document", error)
                manager.updateAppWidget(id, unavailableView(context, actions))
            }
        }
    }

    private fun unavailableView(context: Context, actions: RemoteWeatherActions) =
        RemoteViews(context.packageName, R.layout.remote_weather_unavailable).apply {
            setOnClickPendingIntent(R.id.remote_unavailable_open, actions.open)
            setOnClickPendingIntent(R.id.remote_unavailable_refresh, actions.refresh)
        }
}
