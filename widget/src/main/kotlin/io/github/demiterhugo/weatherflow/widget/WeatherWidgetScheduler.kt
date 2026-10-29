package io.github.demiterhugo.weatherflow.widget

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object WeatherWidgetScheduler {
    internal const val PERIODIC_WORK = "weather-widget-periodic"
    internal const val REFRESH_WORK = "weather-widget-refresh"
    internal const val RENDER_WORK = "weather-widget-render"
    internal const val FETCH_WEATHER = "fetch_weather"

    internal fun start(context: Context) {
        val work = PeriodicWorkRequestBuilder<WeatherWidgetTickWorker>(1, TimeUnit.HOURS)
            .setInitialDelay(1, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, work,
        )
    }

    internal fun refresh(context: Context) {
        val work = OneTimeWorkRequestBuilder<WeatherWidgetWorker>()
            .setInputData(workDataOf(FETCH_WEATHER to true))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        // Periodic ticks and manual taps use the same unique queue.
        WorkManager.getInstance(context).enqueueUniqueWork(REFRESH_WORK, ExistingWorkPolicy.KEEP, work)
    }

    /** Called after foreground location/cache changes; does not make another HTTP request. */
    fun render(context: Context) {
        val work = OneTimeWorkRequestBuilder<WeatherWidgetWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            RENDER_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE, work,
        )
    }

    internal fun stop(context: Context) {
        val manager = WorkManager.getInstance(context)
        manager.cancelUniqueWork(PERIODIC_WORK)
        manager.cancelUniqueWork(REFRESH_WORK)
        manager.cancelUniqueWork(RENDER_WORK)
    }

    internal fun stopIfUnused(context: Context) {
        if (!WeatherWidgetRegistry.hasWidgets(context)) stop(context)
    }
}
