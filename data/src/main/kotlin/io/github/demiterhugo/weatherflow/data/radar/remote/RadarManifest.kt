package io.github.demiterhugo.weatherflow.data.radar.remote

import kotlinx.serialization.Serializable

@Serializable
internal data class RadarManifest(
    val version: String,
    val generated: Long,
    val host: String,
    val radar: RadarTimeline,
)

@Serializable
internal data class RadarTimeline(val past: List<RadarManifestFrame>)

@Serializable
internal data class RadarManifestFrame(val time: Long, val path: String)
