package io.github.demiterhugo.weatherflow.widget.presentation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 37])
class WidgetStatusStorageTest {
    @Test
    fun radarAndWeatherResultsUseIndependentCoordinatesAndDoNotOverwriteEachOther() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val barcelona = WeatherLocation.Barcelona
        val other = WeatherLocation.Device(Coordinates(48.8566, 2.3522))
        try {
            WidgetStatusStorage.save(context, WidgetStatus(barcelona, failed = true))
            WidgetStatusStorage.saveRadar(context, WidgetStatus(other, radarFailed = true, mapFailed = true))
            assertTrue(WidgetStatusStorage.read(context, barcelona).failed)
            assertFalse(WidgetStatusStorage.read(context, barcelona).radarFailed)
            assertTrue(WidgetStatusStorage.read(context, other).radarFailed)
            assertFalse(WidgetStatusStorage.read(context, other).failed)
        } finally {
            WidgetStatusStorage.save(context, WidgetStatus())
            WidgetStatusStorage.saveRadar(context, WidgetStatus())
        }
    }

    @Test
    fun bothRenderersReadTheSamePersistedProgressAndFailureForSelectedCoordinates() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val location = WeatherLocation.Barcelona
        try {
            WidgetStatusStorage.save(context, WidgetStatus(location, refreshing = true))
            val glance = WidgetStatusStorage.read(context, location)
            val remote = WidgetStatusStorage.read(context, location)
            assertEquals(glance, remote)
            assertTrue(glance.refreshing)

            WidgetStatusStorage.save(context, WidgetStatus(location, failed = true))
            assertTrue(WidgetStatusStorage.read(context, location).failed)
            val other = WidgetStatusStorage.read(context, WeatherLocation.Device(Coordinates(48.8566, 2.3522)))
            assertFalse(other.failed)
            assertFalse(other.refreshing)
        } finally {
            WidgetStatusStorage.save(context, WidgetStatus())
        }
    }
}
