package io.github.demiterhugo.weatherflow.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetStatus
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetStatusStorage
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation
import io.github.demiterhugo.weatherflow.widget.radar.WidgetRadarRefreshController

class WeatherWidgetWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        if (!WeatherWidgetRegistry.hasWidgets(applicationContext)) return Result.success()
        val radarOnly = inputData.getBoolean(WeatherWidgetScheduler.RADAR_ONLY, false)
        if (!inputData.getBoolean(WeatherWidgetScheduler.FETCH_WEATHER, false) && !radarOnly) {
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
        val weatherSucceeded = radarOnly || WidgetRefreshController(
            dependencies.locationRepository(), dependencies.weatherRepository(), display,
        ).refresh()
        val radarSucceeded = WidgetRadarRefreshController(
            dependencies.locationRepository(), dependencies.radarRepository(), dependencies.radarMapRepository(),
            needsMap = { WeatherWidgetRegistry.hasRadarWidgets(applicationContext) },
        ) { status ->
            if (WeatherWidgetRegistry.hasWidgets(applicationContext)) {
                WidgetStatusStorage.saveRadar(applicationContext, status)
                WeatherWidgetRenderer.renderAll(applicationContext)
            }
        }.refresh()
        val succeeded = weatherSucceeded && radarSucceeded
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
