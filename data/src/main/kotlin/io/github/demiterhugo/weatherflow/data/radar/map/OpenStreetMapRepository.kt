package io.github.demiterhugo.weatherflow.data.radar.map

import io.github.demiterhugo.weatherflow.data.remote.readBytes
import io.github.demiterhugo.weatherflow.data.radar.AndroidRadarImageDecoder
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.RadarImage
import io.github.demiterhugo.weatherflow.domain.repository.RadarMapRepository
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

internal class OpenStreetMapRepository(
    private val client: OkHttpClient,
    private val renderer: BasemapRenderer,
    private val baseUrl: HttpUrl = "https://tile.openstreetmap.org/".toHttpUrl(),
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val validate: (ByteArray) -> Unit = { AndroidRadarImageDecoder().image(it); Unit },
) : RadarMapRepository {
    private val revision = MutableStateFlow(0L)
    private val mutex = Mutex()

    override fun observeMap(coordinates: Coordinates): Flow<RadarImage?> = revision.map {
        mutex.withLock { load(coordinates, cacheOnly = true) }
    }.distinctUntilChanged().flowOn(io)

    override suspend fun refreshMap(coordinates: Coordinates) = withContext(io) {
        mutex.withLock {
            withTimeout(45_000) {
                load(coordinates, cacheOnly = false) ?: throw IOException("Map is incomplete")
                revision.value++
                Unit
            }
        }
    }

    private suspend fun load(coordinates: Coordinates, cacheOnly: Boolean): RadarImage? {
        val tiles = mutableListOf<Pair<MapTile, ByteArray>>()
        for (tile in mapTiles(coordinates)) {
            val url = baseUrl.newBuilder().addPathSegments("7/${tile.x}/${tile.y}.png").build()
            val request = Request.Builder().url(url)
                .header("User-Agent", "WeatherFlow/1.0 (+https://github.com/DemiterHugo/WeatherFlow)")
                .apply { if (cacheOnly) cacheControl(CacheControl.FORCE_CACHE) }
                .build()
            val response = client.readBytes(request, 512 * 1024)
            if (cacheOnly && response.code == 504) return null
            if (!response.successful) throw IOException("Map HTTP ${response.code}")
            try {
                validate(response.bytes)
            } catch (error: IOException) {
                // A successful HTTP response containing invalid imagery must not poison the cache.
                client.cache?.urls()?.let { urls ->
                    while (urls.hasNext()) if (urls.next() == url.toString()) urls.remove()
                }
                throw error
            }
            tiles += tile to response.bytes
        }
        return renderer.render(tiles)
    }

    companion object {
        fun create(directory: File): OpenStreetMapRepository {
            return OpenStreetMapRepository(createClient(directory), AndroidBasemapRenderer())
        }

        internal fun createClient(directory: File): OkHttpClient = OkHttpClient.Builder()
                .cache(Cache(directory, 16L * 1024 * 1024))
                .connectTimeout(10, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS)
                .followRedirects(false)
                .retryOnConnectionFailure(false)
                .addNetworkInterceptor { chain ->
                    val response = chain.proceed(chain.request())
                    if (response.header("Cache-Control") == null && response.header("Expires") == null) {
                        response.newBuilder().header("Cache-Control", "public, max-age=604800").build()
                    } else response
                }.build()
    }
}
