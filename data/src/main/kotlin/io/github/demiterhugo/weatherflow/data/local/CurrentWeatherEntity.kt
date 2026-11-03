package io.github.demiterhugo.weatherflow.data.local

import androidx.room.Entity
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.CurrentWeather
import io.github.demiterhugo.weatherflow.domain.model.WeatherCondition
import java.time.Instant

@Entity(tableName = "current_weather", primaryKeys = ["latitude", "longitude"])
internal data class CurrentWeatherEntity(
    val latitude: Double,
    val longitude: Double,
    val temperatureCelsius: Double,
    val feelsLikeCelsius: Double,
    val humidityPercent: Int,
    val windSpeedMetersPerSecond: Double,
    val condition: String,
    val observedAtEpochSecond: Long,
)

internal fun CurrentWeather.toEntity(): CurrentWeatherEntity = CurrentWeatherEntity(
    latitude = coordinates.latitude,
    longitude = coordinates.longitude,
    temperatureCelsius = temperatureCelsius,
    feelsLikeCelsius = feelsLikeCelsius,
    humidityPercent = humidityPercent,
    windSpeedMetersPerSecond = windSpeedMetersPerSecond,
    condition = condition.name,
    observedAtEpochSecond = observedAt.epochSecond,
)

internal fun CurrentWeatherEntity.toDomain(): CurrentWeather = CurrentWeather(
    coordinates = Coordinates(latitude, longitude),
    temperatureCelsius = temperatureCelsius,
    feelsLikeCelsius = feelsLikeCelsius,
    humidityPercent = humidityPercent,
    windSpeedMetersPerSecond = windSpeedMetersPerSecond,
    condition = WeatherCondition.entries.firstOrNull { it.name == condition } ?: WeatherCondition.UNKNOWN,
    observedAt = Instant.ofEpochSecond(observedAtEpochSecond),
)
