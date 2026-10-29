package io.github.demiterhugo.weatherflow.domain.repository

import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.CurrentWeather
import kotlinx.coroutines.flow.Flow

/** Shared weather data access contract for application and widget consumers. */
interface WeatherRepository {

    /**
     * Observes weather for [coordinates] without triggering a network refresh.
     *
     * Each collector first receives the latest available observation, or `null` when none
     * exists, then subsequent updates for those coordinates. Observations may be stale;
     * consumers can inspect [CurrentWeather.observedAt].
     */
    fun observeCurrentWeather(coordinates: Coordinates): Flow<CurrentWeather?>

    /**
     * Fetches current weather and publishes it to observers for [coordinates].
     *
     * Returns after the successful update has been stored. Failures propagate as exceptions
     * and retain the last valid observation. Cancellation must propagate normally.
     * Implementations must be safe to call from any coroutine dispatcher.
     */
    suspend fun refreshCurrentWeather(coordinates: Coordinates)
}
