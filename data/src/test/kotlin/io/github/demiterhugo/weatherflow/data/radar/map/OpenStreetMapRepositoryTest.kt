package io.github.demiterhugo.weatherflow.data.radar.map

import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.RadarImage
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OpenStreetMapRepositoryTest {
    @get:Rule val folder = TemporaryFolder()
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private val coordinates = Coordinates(41.3874, 2.1686)
    private val renderer = BasemapRenderer { tiles -> RadarImage(tiles.flatMap { it.second.toList() }.toByteArray()) }

    @Before fun setUp() {
        server = MockWebServer().apply { start() }
        client = OpenStreetMapRepository.createClient(folder.root)
    }

    @After fun tearDown() { client.cache?.close(); server.shutdown() }

    private fun repository(validate: (ByteArray) -> Unit = {}) =
        OpenStreetMapRepository(client, renderer, server.url("/"), validate = validate)

    private fun enqueueTiles(cacheControl: String = "public, max-age=604800") {
        repeat(mapTiles(coordinates).size) { server.enqueue(MockResponse().setHeader("Cache-Control", cacheControl).setHeader("ETag", "map-v1").setBody("tile")) }
    }

    @Test fun observationNeverRequestsNetworkAndReopeningUsesHttpCache() = runBlocking {
        val repository = repository()
        assertNull(repository.observeMap(coordinates).first())
        assertEquals(0, server.requestCount)
        enqueueTiles()
        repository.refreshMap(coordinates)
        val expected = repository.observeMap(coordinates).first()
        assertNotNull(expected)
        assertEquals(expected, repository().observeMap(coordinates).first())
        repository.refreshMap(coordinates)
        assertEquals(mapTiles(coordinates).size, server.requestCount)
        val request = server.takeRequest()
        assertEquals("/7/64/47.png", request.path)
        assertTrue(request.getHeader("User-Agent")!!.contains("WeatherFlow"))
        assertNull(request.getHeader("Cache-Control"))
    }

    @Test fun tilesWithoutExpiryHeadersAreCachedForAtLeastSevenDays() = runBlocking {
        repeat(mapTiles(coordinates).size) { server.enqueue(MockResponse().setBody("tile")) }
        val repository = repository()
        repository.refreshMap(coordinates)
        repository.refreshMap(coordinates)
        assertEquals(mapTiles(coordinates).size, server.requestCount)
    }

    @Test fun expiredTilesAreConditionallyRevalidatedAndStaleMapIsAvailableOffline() = runBlocking {
        val repository = repository()
        enqueueTiles("public, max-age=0")
        repository.refreshMap(coordinates)
        val expected = repository.observeMap(coordinates).first()
        repeat(mapTiles(coordinates).size) { server.enqueue(MockResponse().setResponseCode(304).setHeader("Cache-Control", "max-age=604800")) }
        repository.refreshMap(coordinates)
        assertEquals(expected, repository.observeMap(coordinates).first())
        repeat(mapTiles(coordinates).size) { server.takeRequest() }
        assertEquals("map-v1", server.takeRequest().getHeader("If-None-Match"))
    }

    @Test fun failuresDoNotPublishAnIncompleteViewport() = runBlocking {
        val repository = repository()
        server.enqueue(MockResponse().setHeader("Cache-Control", "max-age=604800").setBody("first"))
        server.enqueue(MockResponse().setResponseCode(503))
        assertTrue(runCatching { repository.refreshMap(coordinates) }.exceptionOrNull() is IOException)
        assertNull(repository.observeMap(coordinates).first())
        assertEquals(2, server.requestCount)
    }

    @Test fun invalidTileIsRemovedFromCacheAndCanBeFetchedAgain() = runBlocking {
        server.enqueue(MockResponse().setHeader("Cache-Control", "max-age=604800").setBody("invalid"))
        assertTrue(runCatching { repository { throw IOException("bad image") }.refreshMap(coordinates) }.exceptionOrNull() is IOException)
        enqueueTiles()
        val repository = repository()
        repository.refreshMap(coordinates)
        assertNotNull(repository.observeMap(coordinates).first())
        assertEquals(mapTiles(coordinates).size + 1, server.requestCount)
    }
}
