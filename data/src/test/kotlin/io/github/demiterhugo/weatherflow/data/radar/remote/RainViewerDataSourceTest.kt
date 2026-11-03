package io.github.demiterhugo.weatherflow.data.radar.remote

import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import okhttp3.mockwebserver.MockResponse
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RainViewerDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var source: RainViewerDataSource

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        source = RainViewerDataSource(
            manifestUrl = server.url("/public/weather-maps.json"),
            clock = Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC),
        )
    }

    @After
    fun tearDown() { server.shutdown() }

    @Test
    fun requestsManifestAndIgnoresUnusedNowcastAndSatelliteFields() = runTest {
        server.enqueue(MockResponse().setBody("""{"version":"2.0","generated":6000,"host":"https://tilecache.rainviewer.com","radar":{"past":[{"time":6000,"path":"/v2/radar/abc"}],"nowcast":[{"time":6600,"path":"future"}]},"satellite":{"infrared":[]}}"""))
        val manifest = source.manifest()
        assertEquals(1, manifest.radar.past.size)
        assertEquals("/v2/radar/abc", manifest.radar.past.single().path)
        val request = server.takeRequest(5, TimeUnit.SECONDS)
        assertEquals("/public/weather-maps.json", request?.path)
        assertTrue(request?.getHeader("User-Agent")?.startsWith("WeatherFlow/") == true)
    }

    @Test
    fun returnsSmallAndChunkedImageBodiesWithoutPadding() = runTest {
        val bytes = byteArrayOf(137.toByte(), 80, 78, 71, 1, 2, 3)
        server.enqueue(MockResponse().setChunkedBody(Buffer().write(bytes), 2))
        assertArrayEquals(bytes, source.image(server.url("/frame.png")))
    }

    @Test
    fun rejectsOversizedDeclaredAndChunkedImages() = runTest {
        server.enqueue(MockResponse().setBody("x").setHeader("Content-Length", MAX_RADAR_IMAGE_BYTES + 1))
        assertTrue(runCatching { source.image(server.url("/large.png")) }.exceptionOrNull() is IOException)
        server.enqueue(MockResponse().setChunkedBody("x".repeat(MAX_RADAR_IMAGE_BYTES + 1), 8192))
        assertTrue(runCatching { source.image(server.url("/chunked.png")) }.exceptionOrNull() is IOException)
    }

    @Test
    fun malformedManifestAndOversizedManifestPropagate() = runTest {
        server.enqueue(MockResponse().setBody("{}"))
        assertTrue(runCatching { source.manifest() }.exceptionOrNull() is SerializationException)
        server.enqueue(MockResponse().setBody("x".repeat(128 * 1024 + 1)))
        assertTrue(runCatching { source.manifest() }.exceptionOrNull() is IOException)
    }

    @Test
    fun rateLimitPropagatesAndSubsequentCallDoesNotHitServerDuringCooldown() = runTest {
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "120"))
        assertTrue(runCatching { source.manifest() }.exceptionOrNull() is IOException)
        assertTrue(runCatching { source.manifest() }.exceptionOrNull() is IOException)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun honorsHttpDateRetryAfterOnServiceUnavailable() = runTest {
        server.enqueue(MockResponse().setResponseCode(503).setHeader("Retry-After", "Thu, 29 Oct 2026 10:02:00 GMT"))
        assertTrue(runCatching { source.manifest() }.exceptionOrNull() is IOException)
        assertTrue(runCatching { source.manifest() }.exceptionOrNull() is IOException)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun cancellingCallerCancelsARequestWaitingForTheBody() = runTest {
        val idle = CompletableDeferred<Unit>()
        val client = OkHttpClient.Builder().build().apply {
            dispatcher.idleCallback = Runnable { idle.complete(Unit) }
        }
        val source = RainViewerDataSource(client = client)
        server.enqueue(MockResponse().setBody("x".repeat(4096)).throttleBody(1, 100, TimeUnit.MILLISECONDS))
        val request = async(Dispatchers.IO) { source.image(server.url("/slow.png")) }
        assertNotNull(server.takeRequest(5, TimeUnit.SECONDS))
        request.cancelAndJoin()
        // OkHttp uses real threads; a virtual test timeout would race its cancellation callback.
        withContext(Dispatchers.IO) { withTimeout(5_000) { idle.await() } }
        assertTrue(request.isCancelled)
        assertEquals(0, client.dispatcher.runningCallsCount())
    }

    @Test
    fun connectionFailuresPropagateWithoutAutomaticRetries() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertTrue(runCatching { source.manifest() }.exceptionOrNull() is IOException)
        assertEquals(1, server.requestCount)
    }
}
