package io.github.demiterhugo.weatherflow.domain.repository

import io.github.demiterhugo.weatherflow.domain.model.Coordinates

interface DeviceLocationProvider {
    /** Requires foreground location permission. Fails if no current fix can be obtained. */
    suspend fun currentCoordinates(): Coordinates
}
