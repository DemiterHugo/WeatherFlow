package io.github.demiterhugo.weatherflow.domain.model

import java.time.Instant

/**
 * Weather observed for [coordinates], using explicit, provider-independent units.
 *
 * [observedAt] is the observation instant, not the time when the app downloaded the data.
 * Temperatures must be finite; no arbitrary weather-specific temperature limits are imposed.
 */
data class CurrentWeather(
    val coordinates: Coordinates,
    val temperatureCelsius: Double,
    val feelsLikeCelsius: Double,
    val humidityPercent: Int,
    val windSpeedMetersPerSecond: Double,
    val condition: WeatherCondition,
    val observedAt: Instant,
) {
    init {
        require(temperatureCelsius.isFinite()) {
            "Temperature must be finite."
        }
        require(feelsLikeCelsius.isFinite()) {
            "Feels-like temperature must be finite."
        }
        require(humidityPercent in 0..100) {
            "Humidity must be between 0 and 100 percent."
        }
        require(windSpeedMetersPerSecond.isFinite() && windSpeedMetersPerSecond >= 0.0) {
            "Wind speed must be finite and non-negative."
        }
    }
}
