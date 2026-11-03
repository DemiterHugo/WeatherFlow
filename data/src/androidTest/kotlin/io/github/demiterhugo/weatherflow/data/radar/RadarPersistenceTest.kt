package io.github.demiterhugo.weatherflow.data.radar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.demiterhugo.weatherflow.data.radar.local.RadarDiskCache
import io.github.demiterhugo.weatherflow.data.radar.local.RadarSnapshotCodec
import io.github.demiterhugo.weatherflow.data.radar.remote.RadarManifest
import io.github.demiterhugo.weatherflow.data.radar.remote.RadarManifestFrame
import io.github.demiterhugo.weatherflow.data.radar.remote.RadarRemoteDataSource
import io.github.demiterhugo.weatherflow.data.radar.remote.RadarTimeline
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RadarPersistenceTest {
    private lateinit var directory: File
    private val decoder = AndroidRadarImageDecoder()
    private val coordinates = Coordinates(41.3874, 2.1686)
    private val clock = Clock.fixed(Instant.ofEpochSecond(12_000), ZoneOffset.UTC)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        directory = File(context.cacheDir, "radar-test-${UUID.randomUUID()}")
    }

    @After
    fun tearDown() { directory.deleteRecursively() }

    @Test
    fun actualPngSequenceAndCoverageSurviveReopeningWithoutNetworkRequests() = runBlocking {
        val remote = FakeRemote()
        val repository = repository(remote)
        repository.refreshRadar(coordinates)
        val expected = repository.observeRadar(coordinates).first()
        assertEquals(7, remote.imageRequests)
        remote.imageRequests = 0
        assertEquals(expected, repository(remote).observeRadar(coordinates).first())
        assertEquals(0, remote.imageRequests)
        assertEquals(6, expected?.frames?.size)
        assertTrue(expected?.coverage?.centerCovered == true)
    }

    @Test
    fun failedDownloadPreservesTheCompletePreviouslyCommittedArchive() = runBlocking {
        val remote = FakeRemote()
        val repository = repository(remote)
        repository.refreshRadar(coordinates)
        val expected = repository.observeRadar(coordinates).first()
        remote.generated = 12_000
        remote.imageRequests = 0
        remote.failAt = 2
        assertTrue(runCatching { repository.refreshRadar(coordinates) }.exceptionOrNull() is IOException)
        assertEquals(expected, repository(remote).observeRadar(coordinates).first())
        assertEquals(1, directory.listFiles().orEmpty().size)
    }

    private fun repository(remote: RadarRemoteDataSource) = OfflineFirstRadarRepository(
        remote, RadarDiskCache(directory, RadarSnapshotCodec(decoder)), decoder, clock,
    )

    private class FakeRemote : RadarRemoteDataSource {
        var generated = 6000L
        var imageRequests = 0
        var failAt: Int? = null

        override suspend fun manifest() = RadarManifest(
            "2.0", generated, "https://tilecache.rainviewer.com",
            RadarTimeline((5 downTo 0).map { offset ->
                val time = generated - offset * 600
                RadarManifestFrame(time, "/v2/radar/id-$time")
            }),
        )

        override suspend fun image(url: HttpUrl): ByteArray {
            imageRequests++
            if (imageRequests == failAt) throw IOException("Interrupted download")
            val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(if ("/coverage/" in url.encodedPath) Color.TRANSPARENT else Color.BLUE)
                return ByteArrayOutputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it); it.toByteArray() }
            } finally {
                bitmap.recycle()
            }
        }
    }
}
