package io.github.demiterhugo.weatherflow.data.local

import io.github.demiterhugo.weatherflow.data.madrid
import io.github.demiterhugo.weatherflow.data.remote.toDomain
import io.github.demiterhugo.weatherflow.data.weatherResponse
import io.github.demiterhugo.weatherflow.domain.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrentWeatherEntityTest {
    @Test
    fun roundTripPreservesWeatherAndObservationTime() {
        val weather = weatherResponse().toDomain(madrid)

        assertEquals(weather, weather.toEntity().toDomain())
    }

    @Test
    fun unrecognizedStoredConditionFallsBackToUnknown() {
        val stored = weatherResponse().toDomain(madrid).toEntity().copy(condition = "FUTURE_CONDITION")

        assertEquals(WeatherCondition.UNKNOWN, stored.toDomain().condition)
    }
}
