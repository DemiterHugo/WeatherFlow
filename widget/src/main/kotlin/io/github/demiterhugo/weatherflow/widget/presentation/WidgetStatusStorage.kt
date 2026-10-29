package io.github.demiterhugo.weatherflow.widget.presentation

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.widgetStatusPreferences by preferencesDataStore("weather_widget_status")

/** A single fetch supplies both providers, so its persisted progress and result are shared too. */
internal object WidgetStatusStorage {
    fun observe(context: Context, location: WeatherLocation?) =
        context.applicationContext.widgetStatusPreferences.data
            .map { WidgetStatus.from(it, location) }
            .catch { error ->
                if (error !is IOException) throw error
                emit(WidgetStatus(location, failed = true))
            }

    suspend fun read(context: Context, location: WeatherLocation?) = observe(context, location).first()

    suspend fun save(context: Context, status: WidgetStatus) {
        context.applicationContext.widgetStatusPreferences.edit { saved ->
            val coordinates = status.location?.coordinates
            if (coordinates == null) {
                saved.remove(WidgetStatus.LATITUDE)
                saved.remove(WidgetStatus.LONGITUDE)
            } else {
                saved[WidgetStatus.LATITUDE] = coordinates.latitude
                saved[WidgetStatus.LONGITUDE] = coordinates.longitude
            }
            saved[WidgetStatus.REFRESHING] = status.refreshing
            saved[WidgetStatus.FAILED] = status.failed
        }
    }
}
