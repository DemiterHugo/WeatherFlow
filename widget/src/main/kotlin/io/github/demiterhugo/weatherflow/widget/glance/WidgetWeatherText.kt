package io.github.demiterhugo.weatherflow.widget.glance

import android.content.Context
import io.github.demiterhugo.weatherflow.domain.model.WeatherCondition
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation
import io.github.demiterhugo.weatherflow.widget.R
import io.github.demiterhugo.weatherflow.widget.WidgetWeatherSnapshot
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Presentation values are separate from Glance, which keeps content tests independent of Android. */
internal data class WidgetWeatherText(
    val location: String,
    val temperature: String?,
    val condition: String?,
    val observedAt: String?,
    val humidity: String?,
    val wind: String?,
    val message: String?,
    val failed: Boolean,
    val refreshing: Boolean,
    val refresh: String,
    val open: String,
    val attribution: String,
    val modelEstimate: String,
)

internal fun WidgetWeatherSnapshot.toText(context: Context, status: WidgetStatus): WidgetWeatherText {
    val locale = context.resources.configuration.locales[0]
    val format = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1; isGroupingUsed = false }
    val weather = weather
    val failed = readFailed || status.failed
    return WidgetWeatherText(
        location = context.getString(when (location) {
            WeatherLocation.Barcelona -> R.string.widget_barcelona
            is WeatherLocation.Device -> R.string.widget_device_location
            null -> R.string.widget_unknown_location
        }),
        temperature = weather?.let { context.getString(R.string.widget_temperature, format.format(it.temperatureCelsius)) },
        condition = weather?.let { context.getString(it.condition.widgetLabel()) },
        observedAt = weather?.let {
            val time = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(locale).withZone(ZoneId.systemDefault()).format(it.observedAt)
            context.getString(R.string.widget_time, time)
        },
        humidity = weather?.let { context.getString(R.string.widget_humidity, it.humidityPercent) },
        wind = weather?.let { context.getString(R.string.widget_wind, format.format(it.windSpeedMetersPerSecond * 3.6)) },
        message = when {
            status.refreshing -> context.getString(R.string.widget_updating)
            failed -> context.getString(if (weather == null) R.string.widget_error_empty else R.string.widget_error_cached)
            weather == null -> context.getString(R.string.widget_empty)
            else -> null
        },
        failed = failed,
        refreshing = status.refreshing,
        refresh = context.getString(R.string.widget_refresh),
        open = context.getString(R.string.widget_open),
        attribution = context.getString(R.string.widget_attribution),
        modelEstimate = context.getString(R.string.widget_model_estimate),
    )
}

private fun WeatherCondition.widgetLabel() = when (this) {
    WeatherCondition.CLEAR -> R.string.widget_clear
    WeatherCondition.PARTLY_CLOUDY -> R.string.widget_partly_cloudy
    WeatherCondition.CLOUDY -> R.string.widget_cloudy
    WeatherCondition.FOG -> R.string.widget_fog
    WeatherCondition.DRIZZLE -> R.string.widget_drizzle
    WeatherCondition.RAIN -> R.string.widget_rain
    WeatherCondition.SNOW -> R.string.widget_snow
    WeatherCondition.THUNDERSTORM -> R.string.widget_thunderstorm
    WeatherCondition.UNKNOWN -> R.string.widget_unknown
}
