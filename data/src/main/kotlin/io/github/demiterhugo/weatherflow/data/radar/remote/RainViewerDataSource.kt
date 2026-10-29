package io.github.demiterhugo.weatherflow.data.radar.remote

import java.io.IOException
import java.time.Clock
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

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
        val result = execute(url, maxBytes)
        if (!result.successful) {
            if (result.code == 429 || result.code == 503) {
                gate.postponeFor(retryDelay(result.retryAfter).coerceAtLeast(if (result.code == 429) 60_000 else 0))
            }
            throw IOException("Radar HTTP ${result.code}")
        }
        return result.bytes
    }

    // Keep cancellation attached to the call until the body has been read, not only until headers arrive.
    private suspend fun execute(url: HttpUrl, maxBytes: Int): HttpResult = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "WeatherFlow/1.0 (Android; personal radar demo)")
            .build()
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                val result = try {
                    response.use {
                        val bytes = if (it.isSuccessful) {
                            val body = it.body ?: throw IOException("Empty radar response")
                            if (body.contentLength() > maxBytes) throw IOException("Radar response exceeds size limit")
                            val source = body.source()
                            source.request(maxBytes.toLong() + 1)
                            source.buffer.readByteArray().also { data ->
                                if (data.size > maxBytes) throw IOException("Radar response exceeds size limit")
                            }
                        } else byteArrayOf()
                        HttpResult(it.code, it.isSuccessful, bytes, it.header("Retry-After"))
                    }
                } catch (e: IOException) {
                    continuation.resumeWithException(e)
                    return
                }
                // Close the body before resuming, so a close failure cannot resume twice.
                continuation.resume(result)
            }
        })
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

    private data class HttpResult(val code: Int, val successful: Boolean, val bytes: ByteArray, val retryAfter: String?)
}
