package io.github.demiterhugo.weatherflow.data.radar.remote

import java.io.IOException
import java.time.Clock
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit
import io.github.demiterhugo.weatherflow.data.remote.readBytes
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

internal const val MAX_RADAR_IMAGE_BYTES = 512 * 1024

internal interface RadarRemoteDataSource {
    suspend fun manifest(): RadarManifest
    suspend fun image(url: HttpUrl): ByteArray
}

internal class RainViewerDataSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(false)
        .build(),
    private val manifestUrl: HttpUrl = "https://api.rainviewer.com/public/weather-maps.json".toHttpUrl(),
    private val gate: RadarRequestGate = RadarRequestGate(),
    private val clock: Clock = Clock.systemUTC(),
) : RadarRemoteDataSource {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun manifest(): RadarManifest =
        json.decodeFromString(get(manifestUrl, 128 * 1024).decodeToString())

    override suspend fun image(url: HttpUrl): ByteArray = get(url, MAX_RADAR_IMAGE_BYTES)

    private suspend fun get(url: HttpUrl, maxBytes: Int): ByteArray {
        gate.awaitTurn()
        val result = client.readBytes(
            Request.Builder().url(url).header("User-Agent", "WeatherFlow/1.0 (Android; personal radar demo)").build(),
            maxBytes,
        )
        if (!result.successful) {
            if (result.code == 429 || result.code == 503) {
                gate.postponeFor(retryDelay(result.headers["Retry-After"]).coerceAtLeast(if (result.code == 429) 60_000 else 0))
            }
            throw IOException("Radar HTTP ${result.code}")
        }
        return result.bytes
    }

    private fun retryDelay(value: String?): Long {
        value?.toLongOrNull()?.let { return it.coerceIn(0, 86_400) * 1000 }
        return try {
            value?.let {
                (ZonedDateTime.parse(it, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() - clock.millis())
                    .coerceIn(0, 86_400_000)
            } ?: 60_000
        } catch (_: DateTimeParseException) {
            60_000
        }
    }

}
