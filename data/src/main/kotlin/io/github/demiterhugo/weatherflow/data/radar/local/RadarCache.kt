package io.github.demiterhugo.weatherflow.data.radar.local

import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.RadarSequence
import kotlinx.coroutines.flow.Flow

internal interface RadarCache {
    fun observe(coordinates: Coordinates): Flow<RadarSequence?>
    suspend fun read(coordinates: Coordinates): RadarSequence?
    suspend fun save(sequence: RadarSequence)
}
