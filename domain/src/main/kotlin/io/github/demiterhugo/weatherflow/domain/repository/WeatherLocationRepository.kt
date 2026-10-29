package io.github.demiterhugo.weatherflow.domain.repository

import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation
import kotlinx.coroutines.flow.Flow

interface WeatherLocationRepository {
    /** Emits Barcelona until a selection has been saved, then subsequent selections. */
    fun observeLocation(): Flow<WeatherLocation>

    suspend fun selectLocation(location: WeatherLocation)
}
