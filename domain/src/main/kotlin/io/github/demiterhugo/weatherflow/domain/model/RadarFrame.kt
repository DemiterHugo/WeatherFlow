package io.github.demiterhugo.weatherflow.domain.model

import java.time.Instant

/** A historical composite. Its generation time is not the observation time of every radar. */
data class RadarFrame(
    val sourceId: String,
    val generatedAt: Instant,
    val image: RadarImage,
) {
    init {
        require(sourceId.isNotBlank()) { "Radar frame identifier must not be blank." }
        require(generatedAt >= Instant.EPOCH) { "Radar frame time must not precede the Unix epoch." }
    }
}
