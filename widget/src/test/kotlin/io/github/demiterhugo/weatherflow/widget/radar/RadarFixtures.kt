package io.github.demiterhugo.weatherflow.widget.radar

import android.graphics.Bitmap
import android.graphics.Color
import io.github.demiterhugo.weatherflow.domain.model.*
import java.io.ByteArrayOutputStream
import java.time.Instant

internal val radarTestTime: Instant = Instant.parse("2026-10-30T12:00:00Z")

internal fun radarTestImage(color: Int): RadarImage {
    val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
    try {
        bitmap.eraseColor(color)
        return ByteArrayOutputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it); RadarImage(it.toByteArray()) }
    } finally { bitmap.recycle() }
}

internal fun radarTestSequence(coordinates: Coordinates = WeatherLocation.Barcelona.coordinates): RadarSequence = RadarSequence(
    coordinates, 7, 256, radarTestTime, radarTestTime,
    (5 downTo 0).map { RadarFrame("frame-$it", radarTestTime.minusSeconds(it * 600L), radarTestImage(Color.argb(80, 0, 60 + it * 20, 255))) },
    RadarCoverage(radarTestImage(Color.TRANSPARENT), RadarCoverageAvailability.FULL, true, radarTestTime),
)
