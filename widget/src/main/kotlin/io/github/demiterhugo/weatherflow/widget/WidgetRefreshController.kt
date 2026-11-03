package io.github.demiterhugo.weatherflow.widget

import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation
import io.github.demiterhugo.weatherflow.domain.repository.WeatherLocationRepository
import io.github.demiterhugo.weatherflow.domain.repository.WeatherRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Persists progress/error state before asking the launcher to rebuild its RemoteViews. */
internal interface WidgetRefreshDisplay {
    suspend fun show(location: WeatherLocation?, refreshing: Boolean, failed: Boolean)
}

internal class WidgetRefreshController(
    private val locations: WeatherLocationRepository,
    private val weather: WeatherRepository,
    private val display: WidgetRefreshDisplay,
) {
    /** Returns false on failure, retaining the repository's previous weather. */
    suspend fun refresh(): Boolean {
        var location: WeatherLocation? = null
        var failed = false
        try {
            location = locations.observeLocation().first()
            display.show(location, refreshing = true, failed = false)
            weather.refreshCurrentWeather(location.coordinates)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            failed = true
        } finally {
            // Cancellation still clears progress. External cancellation is never treated as a network error.
            withContext(NonCancellable) {
                display.show(location, refreshing = false, failed = failed)
            }
        }
        return !failed
    }
}
