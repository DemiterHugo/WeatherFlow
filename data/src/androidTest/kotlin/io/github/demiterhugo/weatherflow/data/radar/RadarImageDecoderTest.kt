package io.github.demiterhugo.weatherflow.data.radar

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.demiterhugo.weatherflow.data.radar.remote.MAX_RADAR_IMAGE_BYTES
import io.github.demiterhugo.weatherflow.domain.model.RadarCoverageAvailability
import java.io.ByteArrayOutputStream
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RadarImageDecoderTest {
    private val decoder = AndroidRadarImageDecoder()
    private val time = Instant.ofEpochSecond(6000)

    @Test
    fun validatesActualPngWithoutChangingItsEncodedContent() {
        val bytes = image { _, _ -> Color.BLUE }
        assertTrue(decoder.image(bytes).copyBytes().contentEquals(bytes))
    }

    @Test
    fun transparentMaskIndicatesFullPotentialCoverage() {
        val coverage = decoder.coverage(image { _, _ -> Color.TRANSPARENT }, time)
        assertEquals(RadarCoverageAvailability.FULL, coverage.availability)
        assertTrue(coverage.centerCovered)
        assertEquals(time, coverage.checkedAt)
    }

    @Test
    fun opaqueMaskIndicatesUnavailableCoverage() {
        val coverage = decoder.coverage(image { _, _ -> Color.BLACK }, time)
        assertEquals(RadarCoverageAvailability.NONE, coverage.availability)
        assertFalse(coverage.centerCovered)
    }

    @Test
    fun partialCoverageReportsWhetherSelectedCenterIsCovered() {
        val maskedCenter = decoder.coverage(image { x, _ -> if (x < 128) Color.TRANSPARENT else Color.BLACK }, time)
        assertEquals(RadarCoverageAvailability.PARTIAL, maskedCenter.availability)
        assertFalse(maskedCenter.centerCovered)
        val coveredCenter = decoder.coverage(image { x, _ -> if (x < 128) Color.BLACK else Color.TRANSPARENT }, time)
        assertEquals(RadarCoverageAvailability.PARTIAL, coveredCenter.availability)
        assertTrue(coveredCenter.centerCovered)
    }

    @Test
    fun rejectsWrongDimensionsBeforeFullDecode() {
        val bytes = image(size = 512) { _, _ -> Color.BLUE }
        assertThrows(InvalidRadarImageException::class.java) { decoder.image(bytes) }
    }

    @Test
    fun rejectsJpegTruncatedAndOversizedResponses() {
        val valid = image { _, _ -> Color.BLUE }
        val jpeg = image(format = Bitmap.CompressFormat.JPEG) { _, _ -> Color.BLUE }
        listOf(jpeg, valid.copyOf(valid.size - 5), byteArrayOf(1), ByteArray(MAX_RADAR_IMAGE_BYTES + 1)).forEach {
            assertThrows(InvalidRadarImageException::class.java) { decoder.image(it) }
        }
    }

    private fun image(
        size: Int = 256,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        pixel: (Int, Int) -> Int,
    ): ByteArray {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        try {
            bitmap.setPixels(IntArray(size * size) { pixel(it % size, it / size) }, 0, size, 0, 0, size, size)
            return ByteArrayOutputStream().use { bitmap.compress(format, 100, it); it.toByteArray() }
        } finally {
            bitmap.recycle()
        }
    }
}
