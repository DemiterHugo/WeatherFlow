package io.github.demiterhugo.weatherflow.domain.model

import java.time.Instant

/** Potential coverage of the tile, not a guarantee that every contributing radar is operational. */
enum class RadarCoverageAvailability { FULL, PARTIAL, NONE }

/** Transparent mask pixels indicate coverage; opaque pixels indicate unavailable areas. */
data class RadarCoverage(
    val mask: RadarImage,
    val availability: RadarCoverageAvailability,
    val centerCovered: Boolean,
    val checkedAt: Instant,
) {
    init {
        require(checkedAt >= Instant.EPOCH)
        require(availability != RadarCoverageAvailability.FULL || centerCovered)
        require(availability != RadarCoverageAvailability.NONE || !centerCovered)
    }
}
