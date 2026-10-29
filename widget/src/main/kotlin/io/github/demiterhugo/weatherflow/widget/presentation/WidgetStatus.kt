package io.github.demiterhugo.weatherflow.widget.presentation

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation

internal data class WidgetStatus(
    val location: WeatherLocation? = null,
    val refreshing: Boolean = false,
    val failed: Boolean = false,
) {
    companion object {
        internal val LATITUDE = doublePreferencesKey("status_latitude")
        internal val LONGITUDE = doublePreferencesKey("status_longitude")
        internal val REFRESHING = booleanPreferencesKey("refreshing")
        internal val FAILED = booleanPreferencesKey("failed")

        fun from(preferences: Preferences, location: WeatherLocation?): WidgetStatus {
            val sameLocation = location != null &&
                preferences[LATITUDE] == location.coordinates.latitude &&
                preferences[LONGITUDE] == location.coordinates.longitude
            // A location-storage failure has no coordinates and should still be visible.
            val unknownLocationFailure = location == null && preferences[LATITUDE] == null
            return WidgetStatus(
                location,
                refreshing = sameLocation && preferences[REFRESHING] == true,
                failed = (sameLocation || unknownLocationFailure) && preferences[FAILED] == true,
            )
        }
    }
}
