package io.github.demiterhugo.weatherflow.data.radar.remote

import io.github.demiterhugo.weatherflow.data.radar.radarCoordinates
import io.github.demiterhugo.weatherflow.data.radar.radarManifest
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import java.io.IOException
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RadarManifestMapperTest {
    @Test
    fun sortsDeduplicatesAndSelectsSixMostRecentFrames() {
        val timeline = radarManifest().radar.past + RadarManifestFrame(1000, "/v2/radar/old")
        val manifest = radarManifest().copy(radar = RadarTimeline(timeline.reversed() + timeline.first()))
        assertEquals(radarManifest().radar.past, manifest.toDownloadPlan(radarCoordinates).frames)
    }

    @Test
    fun usesOpaqueProviderPathsAndSeparateCoverageEndpoint() {
        val manifest = radarManifest().copy(radar = RadarTimeline(listOf(RadarManifestFrame(6000, "/v2/radar/a34b9f"))))
        val plan = manifest.toDownloadPlan(radarCoordinates)
        assertEquals("https://tilecache.rainviewer.com/v2/radar/a34b9f/256/7/41.38740000/2.16860000/2/1_0.png", plan.imageUrl(plan.frames.single()).toString())
        assertEquals("https://tilecache.rainviewer.com/v2/coverage/0/256/7/41.38740000/2.16860000/0/0_0.png", plan.coverageUrl().toString())
    }

    @Test
    fun integralCoordinatesContainDecimalPointsRegardlessOfLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            val plan = radarManifest().toDownloadPlan(Coordinates(0.0, -70.0))
            assertEquals("/v2/coverage/0/256/7/0.00000000/-70.00000000/0/0_0.png", plan.coverageUrl().encodedPath)
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun rejectsUnsupportedManifestVersionsAndInvalidTileHosts() {
        val manifest = radarManifest()
        listOf("http://tilecache.rainviewer.com", "not a url", "https://user:pass@example.com", "https://example.com/path", "https://example.com/?x=1").forEach { host ->
            assertThrows(IOException::class.java) { manifest.copy(host = host).toDownloadPlan(radarCoordinates) }
        }
        assertThrows(IOException::class.java) { manifest.copy(version = "3.0").toDownloadPlan(radarCoordinates) }
        assertThrows(IOException::class.java) { manifest.copy(generated = 0).toDownloadPlan(radarCoordinates) }
    }

    @Test
    fun rejectsEmptyFutureConflictingAndMalformedFrames() {
        val frame = radarManifest().radar.past.last()
        listOf(
            emptyList(), List(101) { frame }, listOf(frame.copy(time = 0)), listOf(frame.copy(time = 6001)),
            listOf(frame.copy(path = "/v2/radar/../other")), listOf(frame, frame.copy(path = "/v2/radar/conflict")),
            listOf(frame, frame.copy(time = 5400)),
        ).forEach { frames ->
            assertThrows(IOException::class.java) { radarManifest().copy(radar = RadarTimeline(frames)).toDownloadPlan(radarCoordinates) }
        }
    }
}
