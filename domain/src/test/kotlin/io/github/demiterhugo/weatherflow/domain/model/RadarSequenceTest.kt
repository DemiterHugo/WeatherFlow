package io.github.demiterhugo.weatherflow.domain.model

import java.time.Instant
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RadarSequenceTest {
    private val image = RadarImage(byteArrayOf(1, 2, 3))
    private val time = Instant.ofEpochSecond(6000)
    private val coverage = RadarCoverage(image, RadarCoverageAvailability.FULL, true, time)
    private val frame = RadarFrame("frame", time, image)

    @Test
    fun encodedImageIsImmutableAndComparesByContent() {
        val source = byteArrayOf(1, 2, 3)
        val value = RadarImage(source)
        source[0] = 99
        value.copyBytes()[1] = 99
        assertArrayEquals(byteArrayOf(1, 2, 3), value.copyBytes())
        assertEquals(image, value)
        assertEquals(image.hashCode(), value.hashCode())
        assertEquals(3, value.byteCount)
        assertThrows(IllegalArgumentException::class.java) { RadarImage(byteArrayOf()) }
    }

    @Test
    fun frameListIsAnImmutableSnapshotOfTheInput() {
        val source = mutableListOf(frame)
        val sequence = sequence(source)
        source.clear()
        assertEquals(listOf(frame), sequence.frames)
        assertThrows(UnsupportedOperationException::class.java) { (sequence.frames as MutableList).clear() }
        assertEquals(sequence(listOf(frame)), sequence)
        assertEquals(sequence(listOf(frame)).hashCode(), sequence.hashCode())
    }

    @Test
    fun rejectsEmptyOversizedUnorderedAndConflictingTimelines() {
        val earlier = frame.copy(sourceId = "older", generatedAt = time.minusSeconds(600))
        listOf(
            emptyList(), List(7) { frame }, listOf(frame, earlier), listOf(frame, frame),
            listOf(earlier.copy(sourceId = frame.sourceId), frame), listOf(frame.copy(generatedAt = time.plusSeconds(1))),
        ).forEach { frames -> assertThrows(IllegalArgumentException::class.java) { sequence(frames) } }
    }

    @Test
    fun acceptsOneToSixHistoricalFramesAndPreservesTheirGenerationTimes() {
        val frames = (5 downTo 0).map { frame.copy(sourceId = "frame-$it", generatedAt = time.minusSeconds(it * 600L)) }
        assertEquals(frames, sequence(frames).frames)
    }

    @Test
    fun rejectsInvalidFrameIdentifiersTimesAndCoverageClaims() {
        assertThrows(IllegalArgumentException::class.java) { frame.copy(sourceId = " ") }
        assertThrows(IllegalArgumentException::class.java) { frame.copy(generatedAt = Instant.EPOCH.minusSeconds(1)) }
        assertThrows(IllegalArgumentException::class.java) { coverage.copy(centerCovered = false) }
        assertThrows(IllegalArgumentException::class.java) { coverage.copy(availability = RadarCoverageAvailability.NONE) }
        assertThrows(IllegalArgumentException::class.java) { coverage.copy(checkedAt = Instant.EPOCH.minusSeconds(1)) }
    }

    private fun sequence(frames: List<RadarFrame>) = RadarSequence(
        Coordinates(41.3874, 2.1686), 7, 256, time, time, frames, coverage,
    )
}
