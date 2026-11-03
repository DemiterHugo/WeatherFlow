package io.github.demiterhugo.weatherflow.data.radar.remote

import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import java.io.IOException
import java.time.Instant
import java.util.Locale
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal const val RADAR_ZOOM = 7
internal const val RADAR_IMAGE_SIZE = 256
internal const val MAX_MERCATOR_LATITUDE = 85.05112878

internal data class RadarDownloadPlan(
    val generatedAt: Instant,
    val frames: List<RadarManifestFrame>,
    val host: HttpUrl,
    val coordinates: Coordinates,
) {
    fun imageUrl(frame: RadarManifestFrame): HttpUrl = url("${frame.path}/$RADAR_IMAGE_SIZE/$RADAR_ZOOM/$center/2/1_0.png")
    fun coverageUrl(): HttpUrl = url("/v2/coverage/0/$RADAR_IMAGE_SIZE/$RADAR_ZOOM/$center/0/0_0.png")

    // The provider requires decimal points even for integral coordinates, in every device locale.
    private val center: String get() = String.format(Locale.ROOT, "%.8f/%.8f", coordinates.latitude, coordinates.longitude)
    private fun url(path: String): HttpUrl = host.newBuilder().encodedPath(path).build()
}

internal fun RadarManifest.toDownloadPlan(coordinates: Coordinates): RadarDownloadPlan {
    val base = host.toHttpUrlOrNull()
    if (!version.startsWith("2.") || generated <= 0 || base == null || !base.isHttps ||
        base.username.isNotEmpty() || base.password.isNotEmpty() || base.encodedPath != "/" ||
        base.query != null || base.fragment != null
    ) throw IOException("Unsupported radar manifest")
    if (radar.past.isEmpty() || radar.past.size > 100) throw IOException("Invalid radar timeline")
    if (radar.past.any { it.time <= 0 || it.time > generated || !FRAME_PATH.matches(it.path) }) {
        throw IOException("Invalid historical radar frame")
    }
    val unique = radar.past.distinct()
    if (unique.distinctBy { it.time }.size != unique.size || unique.distinctBy { it.path }.size != unique.size) {
        throw IOException("Conflicting radar frame identifiers or times")
    }
    return RadarDownloadPlan(Instant.ofEpochSecond(generated), unique.sortedBy { it.time }.takeLast(6), base, coordinates)
}

private val FRAME_PATH = Regex("/v2/radar/[A-Za-z0-9_-]+")
