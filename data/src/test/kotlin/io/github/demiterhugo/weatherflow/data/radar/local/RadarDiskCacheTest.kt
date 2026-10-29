package io.github.demiterhugo.weatherflow.data.radar.local

import io.github.demiterhugo.weatherflow.data.radar.FakeRadarImageDecoder
import io.github.demiterhugo.weatherflow.data.radar.radarCoordinates
import io.github.demiterhugo.weatherflow.data.radar.radarSequence
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.RadarSequence
import java.io.File
import java.io.IOException
import java.util.Random
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RadarDiskCacheTest {
    @get:Rule val folder = TemporaryFolder()
    private val codec = RadarSnapshotCodec(FakeRadarImageDecoder())

    @Test
    fun emptyCacheEmitsNullAndCompleteSnapshotSurvivesReopening() = runBlocking {
        val cache = cache()
        assertNull(cache.observe(radarCoordinates).first())
        val sequence = radarSequence()
        cache.save(sequence)
        assertEquals(sequence, cache().observe(radarCoordinates).first())
        assertEquals(1, snapshots().size)
    }

    @Test
    fun activeObserverReceivesOnlyCompleteCommittedSnapshot() = runBlocking {
        val cache = cache()
        var received: RadarSequence? = null
        val observation = launch(start = CoroutineStart.UNDISPATCHED) {
            received = cache.observe(radarCoordinates).first { it != null }
        }
        cache.save(radarSequence())
        observation.join()
        assertEquals(radarSequence(), received)
        assertEquals(6, received?.frames?.size)
    }

    @Test
    fun olderWritesAreIgnoredAndLocationsRemainIsolated() = runBlocking {
        val cache = cache()
        val newer = radarSequence(generated = 6600)
        val other = radarSequence(coordinates = Coordinates(48.8566, 2.3522))
        cache.save(newer)
        cache.save(other)
        cache.save(radarSequence())
        assertEquals(newer, cache.read(radarCoordinates))
        assertEquals(other, cache.read(other.coordinates))
    }

    @Test
    fun entryBudgetEvictsOldestSnapshotAndObserverCanSeeCacheMiss() = runBlocking {
        val cache = cache(maxEntries = 1)
        cache.save(radarSequence())
        val other = radarSequence(coordinates = Coordinates(48.8566, 2.3522))
        cache.save(other)
        assertNull(cache.observe(radarCoordinates).first())
        assertEquals(other, cache.read(other.coordinates))
        assertEquals(1, snapshots().size)
    }

    @Test
    fun byteBudgetAlsoBoundsCacheWhenEntryBudgetHasRoom() = runBlocking {
        val first = radarSequence(bytes = randomBytes(5000))
        cache().save(first)
        val budget = snapshots().single().length() + 256
        val cache = cache(maxBytes = budget)
        val other = radarSequence(coordinates = Coordinates(48.8566, 2.3522), bytes = first.frames.first().image.copyBytes())
        cache.save(other)
        assertNull(cache.read(radarCoordinates))
        assertNotNull(cache.read(other.coordinates))
        assertTrue(snapshots().sumOf { it.length() } <= budget)
    }

    @Test
    fun oversizedReplacementFailsWithoutLosingPreviousSnapshotOrLeavingTemporaryFiles() = runBlocking {
        val previous = radarSequence()
        cache().save(previous)
        val budget = snapshots().single().length() + 128
        val cache = cache(maxBytes = budget)
        val failure = runCatching { cache.save(radarSequence(generated = 6600, bytes = randomBytes(10_000))) }.exceptionOrNull()
        assertTrue(failure is IOException)
        assertEquals(previous, cache.read(radarCoordinates))
        assertTrue(folder.root.listFiles().orEmpty().none { it.extension == "tmp" })
    }

    @Test
    fun corruptSnapshotBecomesCacheMissAndCanBeReplaced() = runBlocking {
        val cache = cache()
        cache.save(radarSequence())
        snapshots().single().writeText("truncated snapshot")
        assertNull(cache.read(radarCoordinates))
        cache.save(radarSequence())
        assertEquals(radarSequence(), cache.read(radarCoordinates))
    }

    @Test
    fun interruptedTemporaryFileIsDiscardedWhileCommittedSnapshotSurvives() = runBlocking {
        cache().save(radarSequence())
        val interrupted = File(folder.root, "radar-interrupted.tmp").apply { writeText("incomplete") }
        assertEquals(radarSequence(), cache().read(radarCoordinates))
        assertTrue(!interrupted.exists())
    }

    @Test
    fun invalidImagesInsideAnOtherwiseValidArchiveAreRejected() = runBlocking {
        cache().save(radarSequence())
        codec.write(radarSequence(bytes = byteArrayOf(-1)), snapshots().single())
        assertNull(cache().read(radarCoordinates))
    }

    private fun cache(maxBytes: Long = 8L * 1024 * 1024, maxEntries: Int = 4) =
        RadarDiskCache(folder.root, codec, maxBytes, maxEntries)

    private fun snapshots(): List<File> = folder.root.listFiles().orEmpty().filter { it.extension == "radar" }

    private fun randomBytes(size: Int) = ByteArray(size).also { Random(42).nextBytes(it); it[0] = 1 }
}
