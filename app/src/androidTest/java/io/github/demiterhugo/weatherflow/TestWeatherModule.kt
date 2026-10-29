package io.github.demiterhugo.weatherflow

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.demiterhugo.weatherflow.data.di.WeatherRepositoryModule
import io.github.demiterhugo.weatherflow.data.di.WeatherLocationModule
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation
import io.github.demiterhugo.weatherflow.domain.repository.WeatherLocationRepository
import io.github.demiterhugo.weatherflow.domain.repository.DeviceLocationProvider
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.CurrentWeather
import io.github.demiterhugo.weatherflow.domain.model.WeatherCondition
import io.github.demiterhugo.weatherflow.domain.repository.WeatherRepository
import java.time.Instant
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal val testWeather = CurrentWeather(
    coordinates = Coordinates(41.3874, 2.1686),
    temperatureCelsius = 18.5,
    feelsLikeCelsius = 17.0,
    humidityPercent = 65,
    windSpeedMetersPerSecond = 3.2,
    condition = WeatherCondition.PARTLY_CLOUDY,
    observedAt = Instant.parse("2026-10-29T12:00:00Z"),
)

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [WeatherRepositoryModule::class, WeatherLocationModule::class])
object TestWeatherModule {
    @Provides
    @Singleton
    fun provideTestRepository(): TestWeatherRepository = TestWeatherRepository()

    @Provides
    fun provideWeatherRepository(repository: TestWeatherRepository): WeatherRepository = repository

    @Provides
    @Singleton
    fun provideLocations(): WeatherLocationRepository = object : WeatherLocationRepository {
        private val location = MutableStateFlow<WeatherLocation>(WeatherLocation.Barcelona)
        override fun observeLocation(): Flow<WeatherLocation> = location
        override suspend fun selectLocation(location: WeatherLocation) { this.location.value = location }
    }

    @Provides
    fun provideDeviceLocation(): DeviceLocationProvider = object : DeviceLocationProvider {
        override suspend fun currentCoordinates() = Coordinates(48.8566, 2.3522)
    }
}

class TestWeatherRepository : WeatherRepository {
    private val cached = MutableStateFlow<CurrentWeather?>(testWeather)
    var refreshCount = 0
        private set

    override fun observeCurrentWeather(coordinates: Coordinates): Flow<CurrentWeather?> = cached

    override suspend fun refreshCurrentWeather(coordinates: Coordinates) {
        refreshCount++
        cached.value = testWeather
    }
}
