package io.github.demiterhugo.weatherflow.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.demiterhugo.weatherflow.data.location.AndroidDeviceLocationProvider
import io.github.demiterhugo.weatherflow.data.location.DataStoreWeatherLocationRepository
import io.github.demiterhugo.weatherflow.domain.repository.DeviceLocationProvider
import io.github.demiterhugo.weatherflow.domain.repository.WeatherLocationRepository
import javax.inject.Singleton

private val Context.weatherLocationPreferences by preferencesDataStore("weather_location")

@Module
@InstallIn(SingletonComponent::class)
internal object LocationPreferencesModule {
    @Provides
    @Singleton
    fun providePreferences(@ApplicationContext context: Context): DataStore<Preferences> =
        context.weatherLocationPreferences
}

@Module
@InstallIn(SingletonComponent::class)
abstract class WeatherLocationModule {
    @Binds
    @Singleton
    internal abstract fun bindLocationRepository(repository: DataStoreWeatherLocationRepository): WeatherLocationRepository

    @Binds
    internal abstract fun bindLocationProvider(provider: AndroidDeviceLocationProvider): DeviceLocationProvider
}
