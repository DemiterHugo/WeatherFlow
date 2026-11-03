package io.github.demiterhugo.weatherflow.widget.radar

import io.github.demiterhugo.weatherflow.domain.repository.RadarMapRepository
import io.github.demiterhugo.weatherflow.domain.repository.RadarRepository
import io.github.demiterhugo.weatherflow.domain.repository.WeatherLocationRepository
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal class WidgetRadarRefreshController(
    private val locations: WeatherLocationRepository,
    private val radar: RadarRepository,
    private val maps: RadarMapRepository,
    private val needsMap: () -> Boolean,
    private val display: suspend (WidgetStatus) -> Unit,
) {
    suspend fun refresh(): Boolean {
        var status = WidgetStatus()
        try {
            val location = locations.observeLocation().first()
            status = WidgetStatus(location, radarRefreshing = true)
            display(status)
            status = status.copy(radarFailed = !attempt { radar.refreshRadar(location.coordinates) })
            if (needsMap()) status = status.copy(mapFailed = !attempt { maps.refreshMap(location.coordinates) })
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            status = status.copy(radarFailed = true)
        } finally {
            withContext(NonCancellable) { display(status.copy(radarRefreshing = false)) }
        }
        return !status.radarFailed && !status.mapFailed
    }

    private suspend fun attempt(block: suspend () -> Unit): Boolean = try {
        block()
        true
    } catch (_: Exception) {
        // Repository operation timeouts are failures; cancellation of this worker still propagates.
        currentCoroutineContext().ensureActive()
        false
    }
}
