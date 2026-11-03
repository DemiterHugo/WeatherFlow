package io.github.demiterhugo.weatherflow.domain.model

import java.time.Instant
import java.util.Collections

/** A complete, location-specific snapshot, ordered from oldest to newest. */
class RadarSequence(
    val coordinates: Coordinates,
    val zoom: Int,
    val imageSize: Int,
    val manifestGeneratedAt: Instant,
    val fetchedAt: Instant,
    frames: List<RadarFrame>,
    val coverage: RadarCoverage,
) {
    val frames: List<RadarFrame> = Collections.unmodifiableList(frames.toList())

    init {
        require(zoom in 0..22)
        require(imageSize in 1..512)
        require(manifestGeneratedAt >= Instant.EPOCH && fetchedAt >= Instant.EPOCH)
        require(this.frames.size in 1..6) { "A radar sequence must contain between one and six frames." }
        require(this.frames.zipWithNext().all { (a, b) -> a.generatedAt < b.generatedAt }) {
            "Radar frames must have unique, increasing generation times."
        }
        require(this.frames.map { it.sourceId }.distinct().size == this.frames.size)
        require(this.frames.last().generatedAt <= manifestGeneratedAt) { "Future frames are not supported." }
    }

    override fun equals(other: Any?): Boolean = other is RadarSequence &&
        coordinates == other.coordinates && zoom == other.zoom && imageSize == other.imageSize &&
        manifestGeneratedAt == other.manifestGeneratedAt && fetchedAt == other.fetchedAt &&
        frames == other.frames && coverage == other.coverage

    override fun hashCode(): Int = java.util.Objects.hash(
        coordinates, zoom, imageSize, manifestGeneratedAt, fetchedAt, frames, coverage,
    )
}
