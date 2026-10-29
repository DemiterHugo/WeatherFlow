package io.github.demiterhugo.weatherflow.widget

import io.github.demiterhugo.weatherflow.domain.model.CurrentWeather
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation
import io.github.demiterhugo.weatherflow.domain.repository.WeatherLocationRepository
import io.github.demiterhugo.weatherflow.domain.repository.WeatherRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

internal data class WidgetWeatherSnapshot(
    val location: WeatherLocation? = null,
    val weather: CurrentWeather? = null,
    val readFailed: Boolean = false,
)

internal class WidgetWeatherReader(
    private val locations: WeatherLocationRepository,
    private val weather: WeatherRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe() = locations.observeLocation().flatMapLatest { location ->
        weather.observeCurrentWeather(location.coordinates)
            .map { WidgetWeatherSnapshot(location, it) }
            .onStart { emit(WidgetWeatherSnapshot(location)) }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(WidgetWeatherSnapshot(location = location, readFailed = true))
            }
    }.catch { error ->
        if (error is CancellationException) throw error
        emit(WidgetWeatherSnapshot(readFailed = true))
    }

    /** Reads persisted state only. Network access belongs to the refresh worker. */
    suspend fun read(): WidgetWeatherSnapshot {
        var location: WeatherLocation? = null
        return try {
            location = locations.observeLocation().first()
            WidgetWeatherSnapshot(location, weather.observeCurrentWeather(location.coordinates).first())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            WidgetWeatherSnapshot(location = location, readFailed = true)
        }
    }
}
