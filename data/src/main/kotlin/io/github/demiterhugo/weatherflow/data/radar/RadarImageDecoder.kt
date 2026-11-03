package io.github.demiterhugo.weatherflow.data.radar

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.github.demiterhugo.weatherflow.data.radar.remote.MAX_RADAR_IMAGE_BYTES
import io.github.demiterhugo.weatherflow.data.radar.remote.RADAR_IMAGE_SIZE
import io.github.demiterhugo.weatherflow.domain.model.RadarCoverage
import io.github.demiterhugo.weatherflow.domain.model.RadarCoverageAvailability
import io.github.demiterhugo.weatherflow.domain.model.RadarImage
import java.io.IOException
import java.time.Instant

internal interface RadarImageDecoder {
    fun image(bytes: ByteArray): RadarImage
    fun coverage(bytes: ByteArray, checkedAt: Instant): RadarCoverage
}

internal class InvalidRadarImageException : IOException("Invalid radar PNG")

internal class AndroidRadarImageDecoder : RadarImageDecoder {
    override fun image(bytes: ByteArray): RadarImage {
        decode(bytes).recycle()
        return RadarImage(bytes)
    }

    override fun coverage(bytes: ByteArray, checkedAt: Instant): RadarCoverage {
        val bitmap = decode(bytes)
        try {
            val pixels = IntArray(RADAR_IMAGE_SIZE * RADAR_IMAGE_SIZE)
            bitmap.getPixels(pixels, 0, RADAR_IMAGE_SIZE, 0, 0, RADAR_IMAGE_SIZE, RADAR_IMAGE_SIZE)
            val covered = pixels.count { it ushr 24 == 0 }
            val availability = when (covered) {
                0 -> RadarCoverageAvailability.NONE
                pixels.size -> RadarCoverageAvailability.FULL
                else -> RadarCoverageAvailability.PARTIAL
            }
            return RadarCoverage(
                RadarImage(bytes), availability,
                pixels[(RADAR_IMAGE_SIZE / 2) * RADAR_IMAGE_SIZE + RADAR_IMAGE_SIZE / 2] ushr 24 == 0,
                checkedAt,
            )
        } finally {
            bitmap.recycle()
        }
    }

    private fun decode(bytes: ByteArray): Bitmap {
        if (bytes.size !in 45..MAX_RADAR_IMAGE_BYTES ||
            !bytes.take(8).toByteArray().contentEquals(PNG_SIGNATURE) ||
            !bytes.takeLast(12).toByteArray().contentEquals(PNG_END)
        ) throw InvalidRadarImageException()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth != RADAR_IMAGE_SIZE || bounds.outHeight != RADAR_IMAGE_SIZE || bounds.outMimeType != "image/png") {
            throw InvalidRadarImageException()
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: throw InvalidRadarImageException()
    }

    private companion object {
        val PNG_SIGNATURE = byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10)
        val PNG_END = byteArrayOf(0, 0, 0, 0, 73, 69, 78, 68, 174.toByte(), 66, 96, 130.toByte())
    }
}
