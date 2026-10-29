package io.github.demiterhugo.weatherflow.ui.weather

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.demiterhugo.weatherflow.R
import io.github.demiterhugo.weatherflow.domain.model.CurrentWeather
import io.github.demiterhugo.weatherflow.domain.model.WeatherCondition
import java.time.ZoneId
import java.util.Locale

@Composable
fun WeatherScreen(
    state: WeatherUiState,
    onRefresh: () -> Unit,
    onAttributionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]

    Scaffold(modifier = modifier.fillMaxSize()) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets).testTag("weather_content"),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.weather_city_madrid),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        stringResource(R.string.weather_demo_location),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.isLoading) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.testTag("weather_loading"))
                        Text(stringResource(R.string.weather_loading))
                    }
                }
            } else if (state.cacheFailed || state.refreshFailed) {
                item {
                    WeatherErrorNotice(state)
                }
            }

            state.weather?.let { weather ->
                item { CurrentWeatherCard(weather, locale) }
                item {
                    WeatherMetric(
                        stringResource(R.string.weather_humidity),
                        stringResource(R.string.weather_percentage, weather.humidityPercent),
                    )
                }
                item {
                    WeatherMetric(
                        stringResource(R.string.weather_wind),
                        stringResource(
                            R.string.weather_wind_kmh,
                            formatWeatherNumber(weather.windSpeedMetersPerSecond * 3.6, locale),
                        ),
                    )
                }
            }

            if (!state.isLoading && state.weather == null && !state.cacheFailed && !state.refreshFailed) {
                item { Text(stringResource(R.string.weather_no_data)) }
            }

            if (state.isRefreshing && state.weather != null) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().testTag("weather_refreshing"),
                    )
                }
            }

            item {
                Button(
                    onClick = onRefresh,
                    enabled = !state.isRefreshing,
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().testTag("weather_refresh"),
                ) {
                    Text(stringResource(when {
                        state.isRefreshing -> R.string.weather_updating
                        state.refreshFailed || state.cacheFailed -> R.string.weather_retry
                        else -> R.string.weather_refresh
                    }))
                }
            }

            item {
                Column(
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TextButton(onClick = onAttributionClick) {
                        Text(stringResource(R.string.weather_attribution))
                    }
                    Text(
                        stringResource(R.string.weather_model_estimate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentWeatherCard(weather: CurrentWeather, locale: Locale) {
    Card(
        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.weather_current_conditions), style = MaterialTheme.typography.labelLarge)
            Text(
                stringResource(R.string.weather_temperature, formatWeatherNumber(weather.temperatureCelsius, locale)),
                modifier = Modifier.testTag("weather_temperature"),
                style = MaterialTheme.typography.displayLarge,
            )
            Text(stringResource(weather.condition.labelResource()), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.weather_feels_like, formatWeatherNumber(weather.feelsLikeCelsius, locale)))
            Text(
                stringResource(
                    R.string.weather_observed_at,
                    formatObservationTime(weather.observedAt, locale, ZoneId.systemDefault()),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun WeatherMetric(label: String, value: String) {
    Card(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun WeatherErrorNotice(state: WeatherUiState) {
    val message = when {
        state.cacheFailed -> R.string.weather_cache_error
        state.weather != null -> R.string.weather_refresh_error_cached
        else -> R.string.weather_refresh_error_empty
    }
    Surface(
        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Text(stringResource(message), modifier = Modifier.padding(20.dp))
    }
}

@StringRes
private fun WeatherCondition.labelResource(): Int = when (this) {
    WeatherCondition.CLEAR -> R.string.weather_condition_clear
    WeatherCondition.PARTLY_CLOUDY -> R.string.weather_condition_partly_cloudy
    WeatherCondition.CLOUDY -> R.string.weather_condition_cloudy
    WeatherCondition.FOG -> R.string.weather_condition_fog
    WeatherCondition.DRIZZLE -> R.string.weather_condition_drizzle
    WeatherCondition.RAIN -> R.string.weather_condition_rain
    WeatherCondition.SNOW -> R.string.weather_condition_snow
    WeatherCondition.THUNDERSTORM -> R.string.weather_condition_thunderstorm
    WeatherCondition.UNKNOWN -> R.string.weather_condition_unknown
}
