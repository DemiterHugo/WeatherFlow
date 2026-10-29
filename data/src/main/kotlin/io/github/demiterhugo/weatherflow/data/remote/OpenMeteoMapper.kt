package io.github.demiterhugo.weatherflow.data.remote

import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.CurrentWeather
import io.github.demiterhugo.weatherflow.domain.model.WeatherCondition
import java.time.Instant

internal fun OpenMeteoResponse.toDomain(coordinates: Coordinates): CurrentWeather {
    require(currentUnits.time == "unixtime") { "Expected Unix timestamp seconds." }
    require(currentUnits.temperature == "°C" && currentUnits.apparentTemperature == "°C") {
        "Expected temperatures in Celsius."
    }
    require(currentUnits.humidity == "%") { "Expected relative humidity in percent." }
    require(currentUnits.windSpeed == "m/s") { "Expected wind speed in meters per second." }

    return CurrentWeather(
        // Keep the requested location; the API response coordinates refer to its model grid cell.
        coordinates = coordinates,
        temperatureCelsius = current.temperature,
        feelsLikeCelsius = current.apparentTemperature,
        humidityPercent = current.humidity,
        windSpeedMetersPerSecond = current.windSpeed,
        condition = current.weatherCode.toWeatherCondition(),
        observedAt = Instant.ofEpochSecond(current.time),
    )
}

/** WMO interpretation codes documented at https://open-meteo.com/en/docs. */
internal fun Int.toWeatherCondition(): WeatherCondition = when (this) {
    0, 1 -> WeatherCondition.CLEAR
    2 -> WeatherCondition.PARTLY_CLOUDY
    3 -> WeatherCondition.CLOUDY
    45, 48 -> WeatherCondition.FOG
    51, 53, 55, 56, 57 -> WeatherCondition.DRIZZLE
    61, 63, 65, 66, 67, 80, 81, 82 -> WeatherCondition.RAIN
    71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOW
    95, 96, 97, 99 -> WeatherCondition.THUNDERSTORM
    else -> WeatherCondition.UNKNOWN
}
