package io.github.demiterhugo.weatherflow.widget.presentation

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation

internal data class WidgetStatus(
    val location: WeatherLocation? = null,
    val refreshing: Boolean = false,
    val failed: Boolean = false,
    val radarRefreshing: Boolean = false,
    val radarFailed: Boolean = false,
    val mapFailed: Boolean = false,
) {
    companion object {
        internal val LATITUDE = doublePreferencesKey("status_latitude")
        internal val LONGITUDE = doublePreferencesKey("status_longitude")
        internal val REFRESHING = booleanPreferencesKey("refreshing")
        internal val FAILED = booleanPreferencesKey("failed")
        internal val RADAR_LATITUDE = doublePreferencesKey("radar_status_latitude")
        internal val RADAR_LONGITUDE = doublePreferencesKey("radar_status_longitude")
        internal val RADAR_REFRESHING = booleanPreferencesKey("radar_refreshing")
        internal val RADAR_FAILED = booleanPreferencesKey("radar_failed")
        internal val MAP_FAILED = booleanPreferencesKey("map_failed")

        fun from(preferences: Preferences, location: WeatherLocation?): WidgetStatus {
            val sameLocation = location != null &&
                preferences[LATITUDE] == location.coordinates.latitude &&
                preferences[LONGITUDE] == location.coordinates.longitude
            // A location-storage failure has no coordinates and should still be visible.
            val unknownLocationFailure = location == null && preferences[LATITUDE] == null
            val sameRadarLocation = location != null &&
                preferences[RADAR_LATITUDE] == location.coordinates.latitude &&
                preferences[RADAR_LONGITUDE] == location.coordinates.longitude
            return WidgetStatus(
                location,
                refreshing = sameLocation && preferences[REFRESHING] == true,
                failed = (sameLocation || unknownLocationFailure) && preferences[FAILED] == true,
                radarRefreshing = sameRadarLocation && preferences[RADAR_REFRESHING] == true,
                radarFailed = sameRadarLocation && preferences[RADAR_FAILED] == true,
                mapFailed = sameRadarLocation && preferences[MAP_FAILED] == true,
            )
        }
    }
}
