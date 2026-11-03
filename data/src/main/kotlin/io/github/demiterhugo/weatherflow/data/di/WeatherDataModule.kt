package io.github.demiterhugo.weatherflow.data.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.demiterhugo.weatherflow.data.local.CurrentWeatherDao
import io.github.demiterhugo.weatherflow.data.local.WeatherDatabase
import io.github.demiterhugo.weatherflow.data.remote.OpenMeteoApi
import io.github.demiterhugo.weatherflow.data.remote.WeatherApiFactory
import io.github.demiterhugo.weatherflow.data.repository.OfflineFirstWeatherRepository
import io.github.demiterhugo.weatherflow.domain.repository.WeatherRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object WeatherDataModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WeatherDatabase =
        Room.databaseBuilder(context, WeatherDatabase::class.java, "weatherflow.db").build()

    @Provides
    fun provideCurrentWeatherDao(database: WeatherDatabase): CurrentWeatherDao =
        database.currentWeatherDao()

    @Provides
    @Singleton
    fun provideWeatherApi(): OpenMeteoApi = WeatherApiFactory.create()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class WeatherRepositoryModule {
    @Binds
    @Singleton
    internal abstract fun bindWeatherRepository(repository: OfflineFirstWeatherRepository): WeatherRepository
}
