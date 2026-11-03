package io.github.demiterhugo.weatherflow.data.remote

import io.github.demiterhugo.weatherflow.data.madrid
import io.github.demiterhugo.weatherflow.data.weatherResponse
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OpenMeteoMapperTest {
    @Test
    fun mapsRequestedLocationAndExplicitUnits() {
        val weather = weatherResponse().toDomain(madrid)

        assertEquals(madrid, weather.coordinates)
        assertEquals(18.5, weather.temperatureCelsius, 0.0)
        assertEquals(17.0, weather.feelsLikeCelsius, 0.0)
        assertEquals(65, weather.humidityPercent)
        assertEquals(3.2, weather.windSpeedMetersPerSecond, 0.0)
        assertEquals(Instant.ofEpochSecond(1_700_000_000), weather.observedAt)
    }

    @Test
    fun rejectsUnexpectedUnitsInsteadOfSilentlyMisinterpretingValues() {
        val response = weatherResponse()
        val invalidUnits = listOf(
            response.currentUnits.copy(time = "iso8601"),
            response.currentUnits.copy(temperature = "°F"),
            response.currentUnits.copy(apparentTemperature = "°F"),
            response.currentUnits.copy(humidity = "fraction"),
            response.currentUnits.copy(windSpeed = "km/h"),
        )

        for (units in invalidUnits) {
            assertThrows("Units: $units", IllegalArgumentException::class.java) {
                response.copy(currentUnits = units).toDomain(madrid)
            }
        }
    }

    @Test
    fun validatesProviderValuesBeforeTheyCanReachTheCache() {
        val response = weatherResponse()
        val invalidReadings = listOf(
            response.current.copy(temperature = Double.NaN),
            response.current.copy(apparentTemperature = Double.POSITIVE_INFINITY),
            response.current.copy(humidity = 101),
            response.current.copy(windSpeed = -1.0),
        )

        for (reading in invalidReadings) {
            assertThrows(IllegalArgumentException::class.java) {
                response.copy(current = reading).toDomain(madrid)
            }
        }
    }
}
