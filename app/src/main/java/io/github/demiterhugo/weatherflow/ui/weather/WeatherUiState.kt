package io.github.demiterhugo.weatherflow.ui.weather

import io.github.demiterhugo.weatherflow.domain.model.CurrentWeather

data class WeatherUiState(
    val weather: CurrentWeather? = null,
    val isReadingCache: Boolean = true,
    val isRefreshing: Boolean = false,
    val refreshFailed: Boolean = false,
    val cacheFailed: Boolean = false,
) {
    val isLoading: Boolean
        get() = weather == null && (isReadingCache || isRefreshing)
}
