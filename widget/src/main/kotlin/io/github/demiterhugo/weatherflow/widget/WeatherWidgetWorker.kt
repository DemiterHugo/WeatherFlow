package io.github.demiterhugo.weatherflow.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import io.github.demiterhugo.weatherflow.widget.glance.WeatherGlanceWidget
import io.github.demiterhugo.weatherflow.widget.glance.WidgetStatus
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation

class WeatherWidgetWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(applicationContext)
        val ids = manager.getGlanceIds(WeatherGlanceWidget::class.java)
        if (ids.isEmpty()) return Result.success()
        val widget = WeatherGlanceWidget()
        if (!inputData.getBoolean(WeatherWidgetScheduler.FETCH_WEATHER, false)) {
            widget.updateAll(applicationContext)
            return Result.success()
        }

        val dependencies = EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)
        val display = object : WidgetRefreshDisplay {
            override suspend fun show(location: WeatherLocation?, refreshing: Boolean, failed: Boolean) {
                WidgetStatus(location, refreshing, failed).save(
                    applicationContext, manager.getGlanceIds(WeatherGlanceWidget::class.java),
                )
                widget.updateAll(applicationContext)
            }
        }
        val succeeded = WidgetRefreshController(
            dependencies.locationRepository(), dependencies.weatherRepository(), display,
        ).refresh()
        return when {
            succeeded -> Result.success()
            runAttemptCount < 2 -> Result.retry()
            else -> Result.failure()
        }
    }
}

/** The timer enqueues the same unique refresh used by the widget's Refresh action. */
class WeatherWidgetTickWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val ids = GlanceAppWidgetManager(applicationContext).getGlanceIds(WeatherGlanceWidget::class.java)
        if (ids.isEmpty()) WeatherWidgetScheduler.stop(applicationContext)
        else WeatherWidgetScheduler.refresh(applicationContext)
        return Result.success()
    }
}
