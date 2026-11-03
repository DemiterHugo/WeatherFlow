package io.github.demiterhugo.weatherflow.widget.glance

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import io.github.demiterhugo.weatherflow.widget.WeatherWidgetScheduler

class RefreshWeatherAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WeatherWidgetScheduler.refresh(context)
    }
}
