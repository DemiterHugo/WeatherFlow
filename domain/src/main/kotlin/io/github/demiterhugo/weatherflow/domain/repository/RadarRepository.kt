package io.github.demiterhugo.weatherflow.domain.repository

import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.RadarSequence
import kotlinx.coroutines.flow.Flow

interface RadarRepository {
    /** Reads complete cached snapshots only; null means absent, evicted or corrupt. Never fetches. */
    fun observeRadar(coordinates: Coordinates): Flow<RadarSequence?>

    /**
     * Fetches up to six recent historical frames and coverage at zoom 7, 256 × 256 PNG.
     * Coordinates must be inside Web Mercator's latitude range (±85.05112878°).
     * Errors propagate while retaining the previous complete snapshot; cancellation propagates.
     * Cache retention is bounded, so consumers must handle eviction and inspect generation times.
     */
    suspend fun refreshRadar(coordinates: Coordinates)
}
