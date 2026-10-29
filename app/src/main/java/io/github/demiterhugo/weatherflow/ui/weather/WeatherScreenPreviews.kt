package io.github.demiterhugo.weatherflow.ui.weather

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.CurrentWeather
import io.github.demiterhugo.weatherflow.domain.model.WeatherCondition
import io.github.demiterhugo.weatherflow.ui.theme.WeatherFlowTheme
import java.time.Instant

@Preview(name = "Weather", showBackground = true)
@Preview(name = "Large text", showBackground = true, fontScale = 2f)
@Composable
private fun WeatherContentPreview() {
    WeatherFlowTheme(dynamicColor = false) {
        WeatherScreen(
            state = WeatherUiState(
                weather = CurrentWeather(
                    Coordinates(41.3874, 2.1686), 18.5, 17.0, 65, 3.2,
                    WeatherCondition.PARTLY_CLOUDY, Instant.parse("2026-10-29T12:00:00Z"),
                ),
                isReadingCache = false,
            ),
            onRefresh = {},
            onAttributionClick = {},
        )
    }
}

@Preview(name = "Loading", showBackground = true)
@Composable
private fun WeatherLoadingPreview() {
    WeatherFlowTheme(dynamicColor = false) {
        WeatherScreen(WeatherUiState(isRefreshing = true), {}, {})
    }
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun WeatherErrorPreview() {
    WeatherFlowTheme(dynamicColor = false) {
        WeatherScreen(WeatherUiState(isReadingCache = false, refreshFailed = true), {}, {})
    }
}
