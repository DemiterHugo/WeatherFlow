package io.github.demiterhugo.weatherflow.widget.glance

import android.content.res.Configuration
import androidx.annotation.ColorRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.demiterhugo.weatherflow.domain.model.WeatherCondition
import io.github.demiterhugo.weatherflow.widget.R
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetWeatherText

@Composable
internal fun WeatherWidgetContent(
    text: WidgetWeatherText, openApp: Action, refresh: Action, attribution: Action,
) {
    val context = LocalContext.current
    val foreground = widgetColor(R.color.widget_glance_ink)
    val secondary = widgetColor(R.color.widget_glance_subtle)
    val temperature = text.compactTemperature
    val temperatureSize = if (LocalSize.current.height >= 175.dp) 58.sp else 46.sp
    Column(
        modifier = GlanceModifier.fillMaxSize().appWidgetBackground()
            .background(widgetColor(R.color.widget_glance_surface)).cornerRadius(30.dp).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(GlanceModifier.fillMaxWidth().clickable(openApp).semantics { testTag = "widget_open" }) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text.location, GlanceModifier.defaultWeight().semantics { testTag = "widget_location" },
                    style = TextStyle(color = foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1)
                text.weatherCondition?.icon()?.let {
                    Box(
                        GlanceModifier.size(34.dp).background(widgetColor(R.color.widget_glance_accent))
                            .cornerRadius(17.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(ImageProvider(it), null, GlanceModifier.size(22.dp), colorFilter = ColorFilter.tint(foreground))
                    }
                }
            }
            temperature?.let {
                Text(it, GlanceModifier.semantics { testTag = "widget_temperature" },
                    style = TextStyle(color = foreground, fontSize = temperatureSize), maxLines = 1)
            }
        }
        val status = text.message
        if (status != null) {
            val modifier = if (text.refreshing) GlanceModifier else GlanceModifier.clickable(refresh)
            Text(status, modifier.semantics { testTag = "widget_status" },
                style = TextStyle(color = secondary, fontSize = 13.sp), maxLines = 2)
            if (temperature == null && !text.refreshing) {
                Text(text.refresh, GlanceModifier.clickable(refresh).semantics { testTag = "widget_refresh" },
                    style = TextStyle(color = foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1)
            }
        } else {
            text.condition?.let { Text(it, style = TextStyle(color = secondary, fontSize = 14.sp), maxLines = 1) }
        }
        if (temperature != null) {
            Spacer(GlanceModifier.size(3.dp))
            Text(
                context.getString(R.string.widget_weather_source),
                GlanceModifier.clickable(attribution).semantics { testTag = "widget_attribution" },
                style = TextStyle(color = secondary, fontSize = 12.sp), maxLines = 1,
            )
        }
    }
}

private fun WeatherCondition.icon(): Int? = when (this) {
    WeatherCondition.CLEAR -> R.drawable.widget_weather_sun
    WeatherCondition.PARTLY_CLOUDY, WeatherCondition.CLOUDY, WeatherCondition.FOG -> R.drawable.widget_weather_cloud
    WeatherCondition.DRIZZLE, WeatherCondition.RAIN, WeatherCondition.THUNDERSTORM -> R.drawable.widget_weather_rain
    WeatherCondition.SNOW -> R.drawable.widget_weather_snow
    WeatherCondition.UNKNOWN -> null
}

/** Resolve both resource variants now so the launcher can select day/night without waking the app. */
@Composable
internal fun widgetColor(@ColorRes resource: Int): ColorProvider {
    val context = LocalContext.current
    return remember(context, resource) {
        fun color(mode: Int): Color {
            val configuration = Configuration(context.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or mode
            }
            return Color(context.createConfigurationContext(configuration).getColor(resource))
        }
        androidx.glance.color.ColorProvider(
            color(Configuration.UI_MODE_NIGHT_NO), color(Configuration.UI_MODE_NIGHT_YES),
        )
    }
}
