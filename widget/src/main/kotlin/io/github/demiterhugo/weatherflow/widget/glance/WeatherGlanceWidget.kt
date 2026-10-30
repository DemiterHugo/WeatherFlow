package io.github.demiterhugo.weatherflow.widget.glance

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import dagger.hilt.android.EntryPointAccessors
import io.github.demiterhugo.weatherflow.widget.WidgetEntryPoint
import io.github.demiterhugo.weatherflow.widget.WidgetWeatherReader
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetStatus
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetStatusStorage
import io.github.demiterhugo.weatherflow.widget.presentation.toText
import io.github.demiterhugo.weatherflow.widget.radar.WidgetRadarReader
import io.github.demiterhugo.weatherflow.widget.radar.WidgetRadarSnapshot
import io.github.demiterhugo.weatherflow.widget.radar.toUi
import androidx.compose.runtime.key

class WeatherGlanceWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(COMPACT_SIZE, EXPANDED_SIZE, RADAR_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dependencies = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val reader = WidgetWeatherReader(dependencies.locationRepository(), dependencies.weatherRepository())
        val radarReader = WidgetRadarReader(dependencies.radarRepository(), dependencies.radarMapRepository())
        val initial = reader.read()
        val launchIntent = requireNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val attributionIntent = Intent(Intent.ACTION_VIEW, "https://open-meteo.com/".toUri())
        val rainIntent = Intent(Intent.ACTION_VIEW, "https://www.rainviewer.com/".toUri())
        val mapIntent = Intent(Intent.ACTION_VIEW, "https://www.openstreetmap.org/copyright".toUri())
        provideContent {
            val snapshot by remember { reader.observe() }.collectAsState(initial)
            val status by remember(snapshot.location) {
                WidgetStatusStorage.observe(context, snapshot.location)
            }.collectAsState(WidgetStatus(location = snapshot.location))
            key(snapshot.location) {
                val radar by remember(snapshot.location) { radarReader.observe(snapshot.location) }
                    .collectAsState(WidgetRadarSnapshot(location = snapshot.location))
                val radarUi = remember(radar, status) { radar.toUi(context, status, animate = false) }
                WeatherWidgetContent(
                    text = snapshot.toText(context, status),
                    openApp = actionStartActivity(launchIntent),
                    refresh = actionRunCallback<RefreshWeatherAction>(),
                    attribution = actionStartActivity(attributionIntent),
                    radar = radarUi,
                    rainAttribution = actionStartActivity(rainIntent),
                    mapAttribution = actionStartActivity(mapIntent),
                )
            }
        }
    }
}
