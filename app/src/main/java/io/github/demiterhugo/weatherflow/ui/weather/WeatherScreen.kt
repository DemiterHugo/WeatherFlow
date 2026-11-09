package io.github.demiterhugo.weatherflow.ui.weather

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.demiterhugo.weatherflow.R
import io.github.demiterhugo.weatherflow.domain.model.CurrentWeather
import io.github.demiterhugo.weatherflow.domain.model.WeatherCondition
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation
import java.time.ZoneId
import java.util.Locale

@Composable
fun WeatherScreen(
    state: WeatherUiState,
    onRefresh: () -> Unit,
    onAttributionClick: () -> Unit,
    modifier: Modifier = Modifier,
    onUseDeviceLocation: () -> Unit = {},
    onUseBarcelona: () -> Unit = {},
    onAddWidget: (() -> Unit)? = null,
    onAddRemoteWidget: (() -> Unit)? = null,
) {
    val locale = LocalConfiguration.current.locales[0]

    Scaffold(modifier = modifier.fillMaxSize()) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets).testTag("weather_content"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val locationName = if (state.location is WeatherLocation.Device) {
                R.string.weather_location_device
            } else R.string.weather_city_barcelona
            item {
                Row(
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF315DE4)),
                    )
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onRefresh,
                        enabled = !state.isRefreshing,
                        modifier = Modifier.testTag("weather_refresh"),
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_weather_refresh),
                            contentDescription = stringResource(when {
                                state.isRefreshing -> R.string.weather_updating
                                state.refreshFailed || state.cacheFailed -> R.string.weather_retry
                                else -> R.string.weather_refresh
                            }),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            item {
                Column(Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    val weather = state.weather
                    if (weather == null) {
                        Text(stringResource(locationName), style = MaterialTheme.typography.headlineSmall)
                    } else {
                        CurrentWeatherCard(weather, locale, stringResource(locationName))
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = onUseDeviceLocation,
                            enabled = !state.isLocating,
                            modifier = Modifier.testTag("weather_use_location"),
                        ) {
                            Text(stringResource(if (state.isLocating) R.string.weather_locating else R.string.weather_use_location))
                        }
                        if (state.location is WeatherLocation.Device) {
                            TextButton(onClick = onUseBarcelona, modifier = Modifier.testTag("weather_use_barcelona")) {
                                Text(stringResource(R.string.weather_use_barcelona))
                            }
                        }
                    }
                    if (state.locationPermissionDenied || state.locationFailed) {
                        Text(
                            stringResource(if (state.locationPermissionDenied) R.string.weather_location_denied else R.string.weather_location_failed),
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
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

            item {
                WidgetComparison(onAddWidget, onAddRemoteWidget)
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
private fun CurrentWeatherCard(weather: CurrentWeather, locale: Locale, location: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(location, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                    Text(stringResource(weather.condition.labelResource()))
                }
                Text(
                    stringResource(R.string.weather_temperature, formatWeatherNumber(weather.temperatureCelsius, locale)),
                    modifier = Modifier.testTag("weather_temperature"),
                    style = MaterialTheme.typography.headlineLarge,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WeatherMetric(
                    stringResource(R.string.weather_humidity),
                    stringResource(R.string.weather_percentage, weather.humidityPercent),
                )
                WeatherMetric(
                    stringResource(R.string.weather_wind),
                    stringResource(R.string.weather_wind_kmh, formatWeatherNumber(weather.windSpeedMetersPerSecond * 3.6, locale)),
                )
            }
            Text(
                stringResource(R.string.weather_feels_like, formatWeatherNumber(weather.feelsLikeCelsius, locale)),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(R.string.weather_observed_at, formatObservationTime(weather.observedAt, locale, ZoneId.systemDefault())),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun WeatherMetric(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun WidgetComparison(onAddGlance: (() -> Unit)?, onAddRemote: (() -> Unit)?) {
    Column(
        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.widget_comparison_title), style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.widget_comparison_intro), style = MaterialTheme.typography.bodySmall)
        WidgetChoice(
            R.string.widget_comparison_glance, R.string.widget_comparison_static,
            R.string.weather_add_widget, "weather_add_widget", onAddGlance,
        )
        WidgetChoice(
            R.string.widget_comparison_remote, R.string.widget_comparison_animated,
            R.string.weather_add_remote_widget, "weather_add_remote_widget", onAddRemote, emphasized = true,
        )
        if (onAddGlance == null || (onAddRemote == null && android.os.Build.VERSION.SDK_INT >= 37)) {
            Text(stringResource(R.string.widget_comparison_picker), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun WidgetChoice(
    @StringRes title: Int,
    @StringRes description: Int,
    @StringRes buttonLabel: Int,
    buttonTag: String,
    onAdd: (() -> Unit)?,
    emphasized: Boolean = false,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(io.github.demiterhugo.weatherflow.widget.R.drawable.widget_radar_preview),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer).padding(6.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column {
                    Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(description), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (onAdd != null) {
                val buttonModifier = Modifier.fillMaxWidth().testTag(buttonTag)
                if (emphasized) {
                    Button(onClick = onAdd, modifier = buttonModifier) { Text(stringResource(buttonLabel)) }
                } else {
                    OutlinedButton(onClick = onAdd, modifier = buttonModifier) { Text(stringResource(buttonLabel)) }
                }
            }
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
