package io.github.demiterhugo.weatherflow.ui.weather

import io.github.demiterhugo.weatherflow.domain.model.CurrentWeather
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation

data class WeatherUiState(
    val weather: CurrentWeather? = null,
    val isReadingCache: Boolean = true,
    val isRefreshing: Boolean = false,
    val refreshFailed: Boolean = false,
    val cacheFailed: Boolean = false,
    val location: WeatherLocation = WeatherLocation.Barcelona,
    val isLocating: Boolean = false,
    val locationFailed: Boolean = false,
    val locationPermissionDenied: Boolean = false,
) {
    val isLoading: Boolean
        get() = weather == null && (isReadingCache || isRefreshing)
}
