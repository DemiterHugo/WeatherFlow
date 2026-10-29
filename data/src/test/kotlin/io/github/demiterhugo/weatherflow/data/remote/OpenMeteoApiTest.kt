package io.github.demiterhugo.weatherflow.data.remote

import io.github.demiterhugo.weatherflow.data.madrid
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

class OpenMeteoApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: OpenMeteoApi

    private val responseBody: String
        get() = checkNotNull(javaClass.getResource("/open-meteo-current.json")).readText()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = WeatherApiFactory.create(server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun requestsRequiredFieldsUnitsAndUtcTimestamp() = runTest {
        server.enqueue(MockResponse().setBody(responseBody))

        val response = api.getCurrentWeather(madrid.latitude, madrid.longitude)
        val request = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(request)
        val url = checkNotNull(request?.requestUrl)
        assertEquals("/v1/forecast", url.encodedPath)
        assertEquals(madrid.latitude.toString(), url.queryParameter("latitude"))
        assertEquals(madrid.longitude.toString(), url.queryParameter("longitude"))
        assertEquals(
            setOf("temperature_2m", "apparent_temperature", "relative_humidity_2m", "wind_speed_10m", "weather_code"),
            url.queryParameter("current")?.split(',')?.toSet(),
        )
        assertEquals("celsius", url.queryParameter("temperature_unit"))
        assertEquals("ms", url.queryParameter("wind_speed_unit"))
        assertEquals("unixtime", url.queryParameter("timeformat"))
        assertEquals("GMT", url.queryParameter("timezone"))
        assertEquals(18.5, response.current.temperature, 0.0)
        assertEquals(madrid, response.toDomain(madrid).coordinates)
    }

    @Test
    fun propagatesHttpFailures() = runTest {
        server.enqueue(MockResponse().setResponseCode(503).setBody("unavailable"))

        val failure = runCatching { api.getCurrentWeather(madrid.latitude, madrid.longitude) }.exceptionOrNull()

        assertTrue(failure is HttpException)
        assertEquals(503, (failure as HttpException).code())
    }

    @Test
    fun rejectsMissingRequiredValues() = runTest {
        server.enqueue(MockResponse().setBody(responseBody.replace("\"temperature_2m\": 18.5,", "")))

        val failure = runCatching { api.getCurrentWeather(madrid.latitude, madrid.longitude) }.exceptionOrNull()

        assertTrue(failure is SerializationException)
    }

    @Test
    fun rejectsNullReadings() = runTest {
        server.enqueue(MockResponse().setBody(responseBody.replace("\"temperature_2m\": 18.5", "\"temperature_2m\": null")))

        val failure = runCatching { api.getCurrentWeather(madrid.latitude, madrid.longitude) }.exceptionOrNull()

        assertTrue(failure is SerializationException)
    }
}
