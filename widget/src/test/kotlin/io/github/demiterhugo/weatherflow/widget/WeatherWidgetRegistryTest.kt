package io.github.demiterhugo.weatherflow.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.demiterhugo.weatherflow.widget.glance.WeatherWidgetReceiver
import io.github.demiterhugo.weatherflow.widget.remote.RemoteWeatherWidgetReceiver
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class WeatherWidgetRegistryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = shadowOf(AppWidgetManager.getInstance(context))

    @Test
    fun emptyHostHasNoWeatherWidgets() {
        assertFalse(WeatherWidgetRegistry.hasWidgets(context))
    }

    @Test
    fun glanceAloneKeepsWeatherUpdatesActive() {
        bind(1, WeatherWidgetReceiver::class.java)
        assertTrue(WeatherWidgetRegistry.hasWidgets(context))
        assertTrue(WeatherWidgetRegistry.remoteIds(context).isEmpty())
    }

    @Test
    fun remoteAloneKeepsWeatherUpdatesActive() {
        bind(2, RemoteWeatherWidgetReceiver::class.java)
        assertTrue(WeatherWidgetRegistry.hasWidgets(context))
        assertArrayEquals(intArrayOf(2), WeatherWidgetRegistry.remoteIds(context))
    }

    @Config(sdk = [26, 35, 36])
    @Test
    fun olderDevicesExcludeRemoteProviderEvenIfAnIdExists() {
        bind(2, RemoteWeatherWidgetReceiver::class.java)
        assertFalse(context.resources.getBoolean(io.github.demiterhugo.weatherflow.widget.R.bool.remote_widget_enabled))
        assertFalse(WeatherWidgetRegistry.hasWidgets(context))
        assertTrue(WeatherWidgetRegistry.remoteIds(context).isEmpty())
    }

    @Test
    fun supportedDeviceEnablesRemoteProvider() {
        assertTrue(context.resources.getBoolean(io.github.demiterhugo.weatherflow.widget.R.bool.remote_widget_enabled))
    }

    private fun bind(id: Int, type: Class<*>) {
        manager.addBoundWidget(id, AppWidgetProviderInfo().apply { provider = ComponentName(context, type) })
    }
}
