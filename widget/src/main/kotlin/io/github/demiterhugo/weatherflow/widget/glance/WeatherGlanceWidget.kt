package io.github.demiterhugo.weatherflow.widget.glance

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import dagger.hilt.android.EntryPointAccessors
import io.github.demiterhugo.weatherflow.widget.WidgetEntryPoint
import io.github.demiterhugo.weatherflow.widget.WidgetWeatherReader

class WeatherGlanceWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(COMPACT_SIZE, EXPANDED_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dependencies = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val reader = WidgetWeatherReader(dependencies.locationRepository(), dependencies.weatherRepository())
        val initial = reader.read()
        val launchIntent = requireNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val attributionIntent = Intent(Intent.ACTION_VIEW, "https://open-meteo.com/".toUri())
        provideContent {
            val snapshot by remember { reader.observe() }.collectAsState(initial)
            val status = WidgetStatus.from(currentState<Preferences>(), snapshot.location)
            WeatherWidgetContent(
                text = snapshot.toText(context, status),
                openApp = actionStartActivity(launchIntent),
                refresh = actionRunCallback<RefreshWeatherAction>(),
                attribution = actionStartActivity(attributionIntent),
            )
        }
    }
}
