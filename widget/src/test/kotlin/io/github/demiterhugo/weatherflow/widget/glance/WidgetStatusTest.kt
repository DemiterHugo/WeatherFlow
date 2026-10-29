package io.github.demiterhugo.weatherflow.widget.glance

import io.github.demiterhugo.weatherflow.widget.presentation.WidgetStatus

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation
import org.junit.Assert.*
import org.junit.Test

class WidgetStatusTest {
    private val preferences = preferencesOf(
        doublePreferencesKey("status_latitude") to WeatherLocation.Barcelona.coordinates.latitude,
        doublePreferencesKey("status_longitude") to WeatherLocation.Barcelona.coordinates.longitude,
        booleanPreferencesKey("refreshing") to true,
        booleanPreferencesKey("failed") to true,
    )

    @Test
    fun statusAppliesToTheLocationThatWasRefreshed() {
        val status = WidgetStatus.from(preferences, WeatherLocation.Barcelona)
        assertTrue(status.refreshing)
        assertTrue(status.failed)
    }

    @Test
    fun previousLocationsProgressAndErrorAreNotShownForNewLocation() {
        val status = WidgetStatus.from(preferences, WeatherLocation.Device(Coordinates(48.8566, 2.3522)))
        assertFalse(status.refreshing)
        assertFalse(status.failed)
    }
}
