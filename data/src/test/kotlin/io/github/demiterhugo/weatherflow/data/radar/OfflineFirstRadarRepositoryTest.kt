package io.github.demiterhugo.weatherflow.data.radar

import io.github.demiterhugo.weatherflow.data.radar.local.RadarCache
import io.github.demiterhugo.weatherflow.data.radar.remote.RadarManifest
import io.github.demiterhugo.weatherflow.data.radar.remote.RadarRemoteDataSource
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.RadarSequence
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstRadarRepositoryTest {
    private val remote = FakeRemote()
    private val cache = FakeCache()
    private val clock = MutableClock()

    @Test
    fun observationReadsOnlyCacheAndPublishesAfterCompleteRefresh() = runTest {
        val repository = repository()
        val emitted = mutableListOf<RadarSequence?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.observeRadar(radarCoordinates).toList(emitted) }
        assertEquals(listOf<RadarSequence?>(null), emitted)
        assertEquals(0, remote.manifestRequests)
        remote.onImage = {
            assertNull(cache.read(radarCoordinates))
            byteArrayOf(1, 2, 3)
        }
        repository.refreshRadar(radarCoordinates)
        runCurrent()
        assertEquals(2, emitted.size)
        val sequence = checkNotNull(emitted.last())
        assertEquals(radarCoordinates, sequence.coordinates)
        assertEquals(6, sequence.frames.size)
        assertEquals((3000L..6000L step 600).toList(), sequence.frames.map { it.generatedAt.epochSecond })
        assertEquals(7, remote.images.size)
        assertEquals(1, cache.saves)
        assertEquals(sequence, repository.observeRadar(radarCoordinates).first())
    }

    @Test
    fun failedFrameOrCoverageDownloadRetainsPreviousSequence() = runTest {
        val repository = repository()
        repository.refreshRadar(radarCoordinates)
        val previous = cache.read(radarCoordinates)
        remote.value = radarManifest(6600)
        val failure = IOException("Offline")
        remote.onImage = { throw failure }
        assertFailure(failure, runCatching { repository.refreshRadar(radarCoordinates) }.exceptionOrNull())
        assertEquals(previous, cache.read(radarCoordinates))

        clock.time = clock.time.plus(Duration.ofDays(2))
        remote.value = radarManifest()
        assertFailure(failure, runCatching { repository.refreshRadar(radarCoordinates) }.exceptionOrNull())
        assertEquals(previous, cache.read(radarCoordinates))
        assertEquals(1, cache.saves)
    }

    @Test
    fun invalidImageAndStorageFailureCannotReplaceCachedSequence() = runTest {
        val repository = repository()
        repository.refreshRadar(radarCoordinates)
        val previous = cache.read(radarCoordinates)
        remote.value = radarManifest(6600)
        remote.onImage = { byteArrayOf(-1) }
        assertTrue(runCatching { repository.refreshRadar(radarCoordinates) }.exceptionOrNull() is InvalidRadarImageException)
        assertEquals(previous, cache.read(radarCoordinates))

        remote.onImage = { byteArrayOf(1) }
        val failure = IOException("Disk full")
        cache.failure = failure
        assertFailure(failure, runCatching { repository.refreshRadar(radarCoordinates) }.exceptionOrNull())
        assertEquals(previous, cache.read(radarCoordinates))
    }

    @Test
    fun changedLocationFetchesItsOwnFramesAndCoverageAndPreservesOtherLocation() = runTest {
        val repository = repository()
        repository.refreshRadar(radarCoordinates)
        val previous = cache.read(radarCoordinates)
        val other = Coordinates(48.8566, 2.3522)
        remote.images.clear()
        repository.refreshRadar(other)
        assertEquals(7, remote.images.size)
        assertTrue(remote.images.all { "/48.85660000/2.35220000/" in it.encodedPath })
        assertEquals(other, cache.read(other)?.coordinates)
        assertEquals(previous, cache.read(radarCoordinates))
    }

    @Test
    fun unchangedManifestReusesImagesAndCoverageWithoutInventingNewFrameTimes() = runTest {
        val repository = repository()
        repository.refreshRadar(radarCoordinates)
        val previous = checkNotNull(cache.read(radarCoordinates))
        remote.images.clear()
        clock.time = clock.time.plusSeconds(300)
        repository.refreshRadar(radarCoordinates)
        val sequence = checkNotNull(cache.read(radarCoordinates))
        assertTrue(remote.images.isEmpty())
        assertEquals(2, remote.manifestRequests)
        assertEquals(previous.frames, sequence.frames)
        assertEquals(previous.coverage.checkedAt, sequence.coverage.checkedAt)
        assertEquals(previous.manifestGeneratedAt, sequence.manifestGeneratedAt)
        assertEquals(clock.instant(), sequence.fetchedAt)
    }

    @Test
    fun rollingTimelineDownloadsOnlyNewFrame() = runTest {
        val repository = repository()
        repository.refreshRadar(radarCoordinates)
        remote.value = radarManifest(6600)
        remote.images.clear()
        repository.refreshRadar(radarCoordinates)
        assertEquals(1, remote.images.size)
        assertTrue("/id-6600/" in remote.images.single().encodedPath)
        assertEquals(3600L, cache.read(radarCoordinates)?.frames?.first()?.generatedAt?.epochSecond)
    }

    @Test
    fun coverageIsRevalidatedAfterTwentyFourHoursEvenWhenFramesAreUnchanged() = runTest {
        val repository = repository()
        repository.refreshRadar(radarCoordinates)
        remote.images.clear()
        clock.time = clock.time.plus(Duration.ofHours(24))
        repository.refreshRadar(radarCoordinates)
        assertEquals(1, remote.images.size)
        assertTrue("/coverage/" in remote.images.single().encodedPath)
        assertEquals(clock.instant(), cache.read(radarCoordinates)?.coverage?.checkedAt)
    }

    @Test
    fun olderManifestCannotReplaceNewerSnapshot() = runTest {
        val repository = repository()
        remote.value = radarManifest(6600)
        repository.refreshRadar(radarCoordinates)
        val previous = cache.read(radarCoordinates)
        remote.value = radarManifest()
        remote.images.clear()
        repository.refreshRadar(radarCoordinates)
        assertEquals(previous, cache.read(radarCoordinates))
        assertTrue(remote.images.isEmpty())
    }

    @Test
    fun sourceAndCallerCancellationPropagateAndPreserveCache() = runTest {
        val repository = repository()
        repository.refreshRadar(radarCoordinates)
        val previous = cache.read(radarCoordinates)
        remote.value = radarManifest(6600)
        val failure = CancellationException("Cancelled")
        remote.onImage = { throw failure }
        assertFailure(failure, runCatching { repository.refreshRadar(radarCoordinates) }.exceptionOrNull())
        assertEquals(previous, cache.read(radarCoordinates))

        remote.onImage = { currentCoroutineContext().cancel(); byteArrayOf(1) }
        val task = launch { repository.refreshRadar(radarCoordinates) }
        task.join()
        assertTrue(task.isCancelled)
        assertEquals(previous, cache.read(radarCoordinates))
    }

    @Test
    fun unsupportedPolarCoordinatesFailBeforeNetworkAccess() = runTest {
        val repository = repository()
        assertTrue(runCatching { repository.refreshRadar(Coordinates(90.0, 0.0)) }.exceptionOrNull() is IllegalArgumentException)
        assertEquals(0, remote.manifestRequests)
    }

    private fun TestScope.repository() = OfflineFirstRadarRepository(
        remote, cache, FakeRadarImageDecoder(), clock, StandardTestDispatcher(testScheduler),
    )

    private fun assertFailure(expected: Exception, actual: Throwable?) {
        // Coroutine debug stack recovery can copy exceptions across dispatcher boundaries.
        assertEquals(expected.javaClass, actual?.javaClass)
        assertEquals(expected.message, actual?.message)
    }

    private class FakeRemote : RadarRemoteDataSource {
        var value: RadarManifest = radarManifest()
        var manifestRequests = 0
        val images = mutableListOf<HttpUrl>()
        var onImage: suspend (HttpUrl) -> ByteArray = { byteArrayOf(1, 2, 3) }
        override suspend fun manifest(): RadarManifest { manifestRequests++; return value }
        override suspend fun image(url: HttpUrl): ByteArray { images += url; return onImage(url) }
    }

    private class FakeCache : RadarCache {
        val state = MutableStateFlow<Map<Coordinates, RadarSequence>>(emptyMap())
        var failure: IOException? = null
        var saves = 0
        override fun observe(coordinates: Coordinates): Flow<RadarSequence?> = state.map { it[coordinates] }
        override suspend fun read(coordinates: Coordinates): RadarSequence? = state.value[coordinates]
        override suspend fun save(sequence: RadarSequence) {
            failure?.let { throw it }
            saves++
            state.value += sequence.coordinates to sequence
        }
    }

    private class MutableClock : Clock() {
        var time: Instant = radarTime
        override fun instant(): Instant = time
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
    }
}
