package io.github.demiterhugo.weatherflow.widget.glance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
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
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.demiterhugo.weatherflow.widget.R
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetWeatherText
import io.github.demiterhugo.weatherflow.widget.radar.WidgetRadarUi

@Composable
internal fun RadarWeatherWidgetContent(
    text: WidgetWeatherText, radar: WidgetRadarUi,
    openApp: Action, refresh: Action, attribution: Action, rainAttribution: Action, mapAttribution: Action,
) {
    val context = LocalContext.current
    Column(GlanceModifier.fillMaxSize().appWidgetBackground().background(widgetColor(R.color.widget_background))
        .cornerRadius(24.dp).padding(12.dp)) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text.location, GlanceModifier.defaultWeight().clickable(openApp),
                style = TextStyle(color = widgetColor(R.color.widget_foreground), fontSize = 16.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            text.temperature?.let { Text(it, style = TextStyle(color = widgetColor(R.color.widget_foreground), fontSize = 26.sp)) }
        }
        Row(GlanceModifier.fillMaxWidth()) {
            Column(GlanceModifier.defaultWeight()) { text.condition?.let { RadarLabel(it) } }
            text.observedAt?.let { RadarLabel(it, 10) }
        }
        RadarLabel(context.getString(R.string.widget_radar_static), 13)
        val image = remember(radar) { radar.latestImage() }
        Box(GlanceModifier.defaultWeight().fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (image != null) Image(ImageProvider(image), context.getString(R.string.widget_radar_description), GlanceModifier.fillMaxSize())
            else RadarLabel(radar.message ?: context.getString(R.string.widget_radar_empty))
        }
        Column {
            radar.frames.lastOrNull()?.let { RadarLabel(it.label) }
            if (image != null) radar.message?.let { RadarLabel(it) }
            text.message?.let { RadarLabel(it) }
        }
        Row(GlanceModifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            val refreshModifier = GlanceModifier.defaultWeight().padding(vertical = 12.dp)
            Text(text.refresh, if (text.refreshing || radar.refreshing) refreshModifier else refreshModifier.clickable(refresh),
                style = TextStyle(color = widgetColor(R.color.widget_foreground), fontSize = 14.sp, fontWeight = FontWeight.Bold))
            Text(text.open, GlanceModifier.clickable(openApp).padding(vertical = 12.dp),
                style = TextStyle(color = widgetColor(R.color.widget_foreground), fontSize = 14.sp))
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
private fun RadarLabel(value: String, size: Int = 12, action: Action? = null) {
    Text(value, modifier = if (action == null) GlanceModifier else GlanceModifier.clickable(action),
        style = TextStyle(color = widgetColor(R.color.widget_secondary), fontSize = size.sp), maxLines = 2)
}
