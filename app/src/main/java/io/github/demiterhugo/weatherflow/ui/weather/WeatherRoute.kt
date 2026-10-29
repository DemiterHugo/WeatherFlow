package io.github.demiterhugo.weatherflow.ui.weather

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun WeatherRoute(viewModel: WeatherViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    WeatherScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onAttributionClick = { uriHandler.openUri("https://open-meteo.com/") },
    )
}
