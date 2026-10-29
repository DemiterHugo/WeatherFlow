package io.github.demiterhugo.weatherflow.data.repository

import io.github.demiterhugo.weatherflow.data.local.CurrentWeatherDao
import io.github.demiterhugo.weatherflow.data.local.toDomain
import io.github.demiterhugo.weatherflow.data.local.toEntity
import io.github.demiterhugo.weatherflow.data.remote.OpenMeteoApi
import io.github.demiterhugo.weatherflow.data.remote.toDomain
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.CurrentWeather
import io.github.demiterhugo.weatherflow.domain.repository.WeatherRepository
import javax.inject.Inject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class OfflineFirstWeatherRepository @Inject constructor(
    private val api: OpenMeteoApi,
    private val dao: CurrentWeatherDao,
) : WeatherRepository {

    override fun observeCurrentWeather(coordinates: Coordinates): Flow<CurrentWeather?> =
        dao.observe(coordinates.latitude, coordinates.longitude)
            .map { it?.toDomain() }
            .distinctUntilChanged()

    override suspend fun refreshCurrentWeather(coordinates: Coordinates) {
        val weather = api.getCurrentWeather(coordinates.latitude, coordinates.longitude)
            .toDomain(coordinates)
        currentCoroutineContext().ensureActive()
        dao.saveIfNewer(weather.toEntity())
    }
}
