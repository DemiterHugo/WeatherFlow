package io.github.demiterhugo.weatherflow.widget.glance

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.state.updateAppWidgetState
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation

internal data class WidgetStatus(
    val location: WeatherLocation? = null,
    val refreshing: Boolean = false,
    val failed: Boolean = false,
) {
    suspend fun save(context: Context, ids: List<GlanceId>) {
        ids.forEach { id ->
            updateAppWidgetState(context, id) { saved ->
                val coordinates = location?.coordinates
                if (coordinates != null) {
                    saved[LATITUDE] = coordinates.latitude
                    saved[LONGITUDE] = coordinates.longitude
                } else {
                    saved.remove(LATITUDE)
                    saved.remove(LONGITUDE)
                }
                saved[REFRESHING] = refreshing
                saved[FAILED] = failed
            }
        }
    }

    companion object {
        private val LATITUDE = doublePreferencesKey("status_latitude")
        private val LONGITUDE = doublePreferencesKey("status_longitude")
        private val REFRESHING = booleanPreferencesKey("refreshing")
        private val FAILED = booleanPreferencesKey("failed")

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
