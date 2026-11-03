package io.github.demiterhugo.weatherflow.data.location

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation
import io.github.demiterhugo.weatherflow.domain.repository.WeatherLocationRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class DataStoreWeatherLocationRepository @Inject constructor(
    private val preferences: DataStore<Preferences>,
) : WeatherLocationRepository {
    override fun observeLocation() = preferences.data.map { saved ->
        val latitude = saved[LATITUDE]
        val longitude = saved[LONGITUDE]
        if (latitude == null || longitude == null) {
            WeatherLocation.Barcelona
        } else {
            WeatherLocation.Device(Coordinates(latitude, longitude))
        }
    }.distinctUntilChanged()

    override suspend fun selectLocation(location: WeatherLocation) {
        preferences.edit { saved ->
            when (location) {
                WeatherLocation.Barcelona -> {
                    saved.remove(LATITUDE)
                    saved.remove(LONGITUDE)
                }
                is WeatherLocation.Device -> {
                    saved[LATITUDE] = location.coordinates.latitude
                    saved[LONGITUDE] = location.coordinates.longitude
                }
            }
        }
    }

    private companion object {
        val LATITUDE = doublePreferencesKey("latitude")
        val LONGITUDE = doublePreferencesKey("longitude")
    }
}
