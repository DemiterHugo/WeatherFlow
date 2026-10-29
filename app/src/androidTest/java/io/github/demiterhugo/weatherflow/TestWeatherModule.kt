package io.github.demiterhugo.weatherflow

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.demiterhugo.weatherflow.data.di.WeatherRepositoryModule
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.CurrentWeather
import io.github.demiterhugo.weatherflow.domain.model.WeatherCondition
import io.github.demiterhugo.weatherflow.domain.repository.WeatherRepository
import java.time.Instant
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal val testWeather = CurrentWeather(
    coordinates = Coordinates(40.4168, -3.7038),
    temperatureCelsius = 18.5,
    feelsLikeCelsius = 17.0,
    humidityPercent = 65,
    windSpeedMetersPerSecond = 3.2,
    condition = WeatherCondition.PARTLY_CLOUDY,
    observedAt = Instant.parse("2026-10-29T12:00:00Z"),
)

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [WeatherRepositoryModule::class])
object TestWeatherModule {
    @Provides
    @Singleton
    fun provideTestRepository(): TestWeatherRepository = TestWeatherRepository()

    @Provides
    fun provideWeatherRepository(repository: TestWeatherRepository): WeatherRepository = repository
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
