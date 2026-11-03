package io.github.demiterhugo.weatherflow.widget.radar

import io.github.demiterhugo.weatherflow.domain.model.RadarImage
import io.github.demiterhugo.weatherflow.domain.model.RadarSequence
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation
import io.github.demiterhugo.weatherflow.domain.repository.RadarMapRepository
import io.github.demiterhugo.weatherflow.domain.repository.RadarRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal data class WidgetRadarSnapshot(
    val location: WeatherLocation? = null,
    val sequence: RadarSequence? = null,
    val map: RadarImage? = null,
    val readFailed: Boolean = false,
)

internal class WidgetRadarReader(private val radar: RadarRepository, private val maps: RadarMapRepository) {
    fun observe(location: WeatherLocation?): Flow<WidgetRadarSnapshot> {
        if (location == null) return flowOf(WidgetRadarSnapshot())
        val sequence = radar.observeRadar(location.coordinates).map { it to false }.catch {
            if (it is CancellationException) throw it
            emit(null to true)
        }
        val basemap = maps.observeMap(location.coordinates).map { it to false }.catch {
            if (it is CancellationException) throw it
            emit(null to true)
        }
        return combine(sequence, basemap) { (frames, radarFailed), (map, mapFailed) ->
            WidgetRadarSnapshot(location, frames?.takeIf { it.coordinates == location.coordinates }, map, radarFailed || mapFailed)
        }
    }

    suspend fun read(location: WeatherLocation?) = observe(location).first()
}
