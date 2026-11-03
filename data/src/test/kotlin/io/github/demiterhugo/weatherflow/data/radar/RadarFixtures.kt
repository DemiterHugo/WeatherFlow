package io.github.demiterhugo.weatherflow.data.radar

import io.github.demiterhugo.weatherflow.data.radar.remote.RadarManifest
import io.github.demiterhugo.weatherflow.data.radar.remote.RadarManifestFrame
import io.github.demiterhugo.weatherflow.data.radar.remote.RadarTimeline
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.RadarCoverage
import io.github.demiterhugo.weatherflow.domain.model.RadarCoverageAvailability
import io.github.demiterhugo.weatherflow.domain.model.RadarFrame
import io.github.demiterhugo.weatherflow.domain.model.RadarImage
import io.github.demiterhugo.weatherflow.domain.model.RadarSequence
import java.time.Instant

internal val radarCoordinates = Coordinates(41.3874, 2.1686)
internal val radarTime: Instant = Instant.ofEpochSecond(6000)

internal fun radarManifest(generated: Long = 6000) = RadarManifest(
    "2.0", generated, "https://tilecache.rainviewer.com",
    RadarTimeline((5 downTo 0).map { offset ->
        val time = generated - offset * 600
        RadarManifestFrame(time, "/v2/radar/id-$time")
    }),
)

// Repository/cache tests replace image decoding. Android instrumentation checks real PNG decoding.
internal class FakeRadarImageDecoder : RadarImageDecoder {
    override fun image(bytes: ByteArray): RadarImage {
        if (bytes.isEmpty() || bytes.first() == (-1).toByte()) throw InvalidRadarImageException()
        return RadarImage(bytes)
    }

    override fun coverage(bytes: ByteArray, checkedAt: Instant) =
        RadarCoverage(image(bytes), RadarCoverageAvailability.PARTIAL, true, checkedAt)
}

internal fun radarSequence(
    coordinates: Coordinates = radarCoordinates,
    generated: Long = 6000,
    bytes: ByteArray = byteArrayOf(1, 2, 3),
) = RadarSequence(
    coordinates, 7, 256, Instant.ofEpochSecond(generated), radarTime,
    radarManifest(generated).radar.past.map { RadarFrame(it.path, Instant.ofEpochSecond(it.time), RadarImage(bytes)) },
    FakeRadarImageDecoder().coverage(byteArrayOf(3), radarTime),
)
