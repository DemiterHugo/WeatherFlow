package io.github.demiterhugo.weatherflow.domain.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CurrentWeatherTest {

    private val weather = CurrentWeather(
        coordinates = Coordinates(latitude = 40.4168, longitude = -3.7038),
        temperatureCelsius = 18.5,
        feelsLikeCelsius = 17.0,
        humidityPercent = 65,
        windSpeedMetersPerSecond = 3.2,
        condition = WeatherCondition.PARTLY_CLOUDY,
        observedAt = Instant.parse("2026-10-29T12:00:00Z"),
    )

    @Test
    fun acceptsNegativeTemperatures() {
        val coldWeather = weather.copy(temperatureCelsius = -25.0, feelsLikeCelsius = -35.0)

        assertEquals(-25.0, coldWeather.temperatureCelsius, 0.0)
        assertEquals(-35.0, coldWeather.feelsLikeCelsius, 0.0)
    }

    @Test
    fun rejectsNonFiniteTemperatures() {
        for (temperature in listOf(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)) {
            assertThrows("Temperature: $temperature", IllegalArgumentException::class.java) {
                weather.copy(temperatureCelsius = temperature)
            }
        }
    }

    @Test
    fun rejectsNonFiniteFeelsLikeTemperatures() {
        for (temperature in listOf(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)) {
            assertThrows("Feels-like temperature: $temperature", IllegalArgumentException::class.java) {
                weather.copy(feelsLikeCelsius = temperature)
            }
        }
    }

    @Test
    fun acceptsInclusiveHumidityBoundaries() {
        for (humidity in listOf(0, 100)) {
            assertEquals(humidity, weather.copy(humidityPercent = humidity).humidityPercent)
        }
    }

    @Test
    fun rejectsHumidityOutsidePercentageRange() {
        for (humidity in listOf(-1, 101)) {
            assertThrows("Humidity: $humidity", IllegalArgumentException::class.java) {
                weather.copy(humidityPercent = humidity)
            }
        }
    }

    @Test
    fun acceptsCalmWind() {
        assertEquals(0.0, weather.copy(windSpeedMetersPerSecond = 0.0).windSpeedMetersPerSecond, 0.0)
    }

    @Test
    fun rejectsNegativeWindSpeed() {
        assertThrows(IllegalArgumentException::class.java) {
            weather.copy(windSpeedMetersPerSecond = -0.1)
        }
    }

    @Test
    fun rejectsNonFiniteWindSpeeds() {
        for (speed in listOf(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)) {
            assertThrows("Wind speed: $speed", IllegalArgumentException::class.java) {
                weather.copy(windSpeedMetersPerSecond = speed)
            }
        }
    }
}
