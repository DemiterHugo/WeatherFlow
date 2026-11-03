package io.github.demiterhugo.weatherflow.domain.repository

import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.RadarImage
import kotlinx.coroutines.flow.Flow

/** A 256 × 256 Web Mercator basemap at zoom 7, centered like [RadarRepository]'s images. */
interface RadarMapRepository {
    /** Cache only, including stale map tiles. Missing tiles produce null, never a network request. */
    fun observeMap(coordinates: Coordinates): Flow<RadarImage?>

    /** Fetch only the displayed viewport. Existing tiles follow HTTP caching and revalidation. */
    suspend fun refreshMap(coordinates: Coordinates)
}
