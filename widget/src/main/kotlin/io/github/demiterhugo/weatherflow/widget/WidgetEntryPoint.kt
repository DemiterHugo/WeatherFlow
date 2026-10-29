package io.github.demiterhugo.weatherflow.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.demiterhugo.weatherflow.domain.repository.WeatherLocationRepository
import io.github.demiterhugo.weatherflow.domain.repository.WeatherRepository

/** Widgets and WorkManager are constructed by Android, outside Hilt's constructor injection. */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetEntryPoint {
    fun weatherRepository(): WeatherRepository
    fun locationRepository(): WeatherLocationRepository
}
