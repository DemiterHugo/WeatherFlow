package io.github.demiterhugo.weatherflow.widget.glance

import android.content.res.Configuration
import androidx.annotation.ColorRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.demiterhugo.weatherflow.widget.R

internal val COMPACT_SIZE = DpSize(180.dp, 220.dp)
internal val EXPANDED_SIZE = DpSize(300.dp, 300.dp)

@Composable
internal fun WeatherWidgetContent(text: WidgetWeatherText, openApp: Action, refresh: Action, attribution: Action) {
    val expanded = LocalSize.current.width >= EXPANDED_SIZE.width && LocalSize.current.height >= EXPANDED_SIZE.height
    Column(
        modifier = GlanceModifier.fillMaxSize().appWidgetBackground()
            .background(widgetColor(R.color.widget_background)).cornerRadius(24.dp).padding(16.dp),
    ) {
        Text(
            text.location,
            modifier = GlanceModifier.fillMaxWidth().clickable(openApp).semantics { testTag = "widget_location" },
            style = TextStyle(color = widgetColor(R.color.widget_foreground), fontSize = 18.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
        text.temperature?.let {
            Text(
                it, modifier = GlanceModifier.semantics { testTag = "widget_temperature" },
                style = TextStyle(color = widgetColor(R.color.widget_foreground), fontSize = if (expanded) 42.sp else 32.sp),
                maxLines = 1,
            )
        }
        text.condition?.let { WidgetLabel(it, "widget_condition") }
        if (expanded) {
            Column {
                text.humidity?.let { WidgetLabel(it, "widget_humidity") }
                text.wind?.let { WidgetLabel(it, "widget_wind") }
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        Column {
            text.message?.let { WidgetLabel(it, "widget_status", error = text.failed) }
            text.observedAt?.let { WidgetLabel(it, "widget_time") }
        }
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text.refresh,
                modifier = GlanceModifier.defaultWeight().clickable(refresh).padding(vertical = 12.dp)
                    .semantics { testTag = "widget_refresh" },
                style = TextStyle(color = widgetColor(R.color.widget_foreground), fontSize = 14.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Text(
                text.open,
                modifier = GlanceModifier.clickable(openApp).padding(vertical = 12.dp).semantics { testTag = "widget_open" },
                style = TextStyle(color = widgetColor(R.color.widget_foreground), fontSize = 14.sp),
                maxLines = 1,
            )
        }
        Column {
            if (expanded) WidgetLabel(text.modelEstimate, "widget_model_estimate")
            Text(
                text.attribution,
                modifier = GlanceModifier.clickable(attribution).semantics { testTag = "widget_attribution" },
                style = TextStyle(color = widgetColor(R.color.widget_secondary), fontSize = 10.sp),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun WidgetLabel(text: String, tag: String, error: Boolean = false) {
    Text(
        text,
        modifier = GlanceModifier.semantics { testTag = tag },
        style = TextStyle(color = widgetColor(if (error) R.color.widget_error else R.color.widget_secondary), fontSize = 12.sp),
        maxLines = 2,
    )
}

/** Resolve both resource variants now so the launcher can select day/night without waking the app. */
@Composable
private fun widgetColor(@ColorRes resource: Int): ColorProvider {
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
