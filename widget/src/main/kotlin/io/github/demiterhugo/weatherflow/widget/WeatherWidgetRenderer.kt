package io.github.demiterhugo.weatherflow.widget

import android.content.Context
import android.os.Build
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.EntryPointAccessors
import io.github.demiterhugo.weatherflow.widget.glance.WeatherGlanceWidget
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetStatusStorage
import io.github.demiterhugo.weatherflow.widget.presentation.toText
import io.github.demiterhugo.weatherflow.widget.remote.RemoteWeatherRenderer
import io.github.demiterhugo.weatherflow.widget.radar.WidgetRadarReader
import io.github.demiterhugo.weatherflow.widget.radar.toUi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object WeatherWidgetRenderer {
    private val renderMutex = Mutex()

    // Foreground renders and fetch completion may overlap. Read current storage under the lock
    // so a slower capture cannot overwrite a newer location's document.
    suspend fun renderAll(context: Context) = renderMutex.withLock {
        WeatherGlanceWidget().updateAll(context)
        if (Build.VERSION.SDK_INT >= 37 && WeatherWidgetRegistry.remoteIds(context).isNotEmpty()) {
            val dependencies = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
            val snapshot = WidgetWeatherReader(dependencies.locationRepository(), dependencies.weatherRepository()).read()
            val status = WidgetStatusStorage.read(context, snapshot.location)
            val radar = WidgetRadarReader(dependencies.radarRepository(), dependencies.radarMapRepository()).read(snapshot.location)
            RemoteWeatherRenderer.renderAll(context, snapshot.toText(context, status), radar.toUi(context, status))
        }
    }
}
