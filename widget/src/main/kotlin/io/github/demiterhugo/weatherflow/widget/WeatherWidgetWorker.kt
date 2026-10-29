package io.github.demiterhugo.weatherflow.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetStatus
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetStatusStorage
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation

class WeatherWidgetWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        if (!WeatherWidgetRegistry.hasWidgets(applicationContext)) return Result.success()
        if (!inputData.getBoolean(WeatherWidgetScheduler.FETCH_WEATHER, false)) {
            WeatherWidgetRenderer.renderAll(applicationContext)
            return Result.success()
        }

        val dependencies = EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)
        val display = object : WidgetRefreshDisplay {
            override suspend fun show(location: WeatherLocation?, refreshing: Boolean, failed: Boolean) {
                if (WeatherWidgetRegistry.hasWidgets(applicationContext)) {
                    WidgetStatusStorage.save(applicationContext, WidgetStatus(location, refreshing, failed))
                    WeatherWidgetRenderer.renderAll(applicationContext)
                }
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
        if (!WeatherWidgetRegistry.hasWidgets(applicationContext)) WeatherWidgetScheduler.stop(applicationContext)
        else WeatherWidgetScheduler.refresh(applicationContext)
        return Result.success()
    }
}
