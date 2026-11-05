package io.github.demiterhugo.weatherflow.widget.glance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
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
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.demiterhugo.weatherflow.widget.R
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetWeatherText
import io.github.demiterhugo.weatherflow.widget.radar.WidgetRadarUi

@Composable
internal fun RadarWeatherWidgetContent(
    text: WidgetWeatherText,
    radar: WidgetRadarUi,
    openApp: Action,
    refresh: Action,
    attribution: Action,
    rainAttribution: Action,
    mapAttribution: Action,
) {
    val context = LocalContext.current
    val image = remember(radar) { radar.latestImage() }
    Column(
        GlanceModifier.fillMaxSize().appWidgetBackground().background(widgetColor(R.color.widget_background))
            .cornerRadius(24.dp).padding(12.dp),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                context.getString(R.string.widget_glance_label),
                GlanceModifier.defaultWeight().clickable(openApp),
                style = TextStyle(color = widgetColor(R.color.widget_foreground), fontSize = 15.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            RadarLabel(context.getString(R.string.widget_radar_static), 11)
        }
        Spacer(GlanceModifier.height(8.dp))
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(GlanceModifier.defaultWeight().padding(end = 8.dp)) {
                Text(text.location, style = TextStyle(color = widgetColor(R.color.widget_foreground),
                    fontSize = 17.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                text.condition?.let { RadarLabel(it, 13, maxLines = 1) }
                text.observedAt?.let { RadarLabel(it, 10) }
            }
            text.temperature?.let {
                Text(it, style = TextStyle(color = widgetColor(R.color.widget_foreground), fontSize = 34.sp), maxLines = 1)
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (image != null) {
                Image(
                    ImageProvider(image), context.getString(R.string.widget_radar_description),
                    GlanceModifier.size(64.dp).cornerRadius(8.dp),
                )
            }
            Column(GlanceModifier.defaultWeight().padding(start = if (image == null) 0.dp else 10.dp)) {
                RadarLabel(context.getString(R.string.widget_radar_latest), 13, maxLines = 1)
                val caption = if (image != null) radar.frames.last().label else
                    radar.message ?: context.getString(R.string.widget_radar_empty)
                RadarLabel(caption, 11)
            }
        }
        Column {
            if (image != null) radar.message?.let { RadarLabel(it, 10, maxLines = 1) }
            text.message?.let { RadarLabel(it, 10, maxLines = 1) }
        }
        Spacer(GlanceModifier.defaultWeight())
        Row(GlanceModifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            val refreshModifier = GlanceModifier.defaultWeight().padding(vertical = 10.dp)
            Text(
                text.refresh,
                if (text.refreshing || radar.refreshing) refreshModifier else refreshModifier.clickable(refresh),
                style = TextStyle(color = widgetColor(R.color.widget_action), fontSize = 13.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Text(text.open, GlanceModifier.clickable(openApp).padding(vertical = 10.dp),
                style = TextStyle(color = widgetColor(R.color.widget_action), fontSize = 13.sp), maxLines = 1)
        }
        Column {
            Row(GlanceModifier.fillMaxWidth()) {
                Column(GlanceModifier.defaultWeight()) { RadarLabel(text.attribution, 10, attribution) }
                RadarLabel(context.getString(R.string.widget_radar_attribution), 10, rainAttribution)
            }
            RadarLabel(context.getString(R.string.widget_map_attribution), 10, mapAttribution)
        }
    }
}

@Composable
private fun RadarLabel(value: String, size: Int, action: Action? = null, maxLines: Int = 2) {
    Text(
        value, modifier = if (action == null) GlanceModifier else GlanceModifier.clickable(action),
        style = TextStyle(color = widgetColor(R.color.widget_secondary), fontSize = size.sp),
        maxLines = maxLines,
    )
}
