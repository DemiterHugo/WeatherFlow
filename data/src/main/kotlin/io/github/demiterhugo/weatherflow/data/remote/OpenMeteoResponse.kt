package io.github.demiterhugo.weatherflow.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OpenMeteoResponse(
    val current: OpenMeteoCurrent,
    @SerialName("current_units") val currentUnits: OpenMeteoUnits,
)

@Serializable
internal data class OpenMeteoCurrent(
    val time: Long,
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("apparent_temperature") val apparentTemperature: Double,
    @SerialName("relative_humidity_2m") val humidity: Int,
    @SerialName("wind_speed_10m") val windSpeed: Double,
    @SerialName("weather_code") val weatherCode: Int,
)

@Serializable
internal data class OpenMeteoUnits(
    val time: String,
    @SerialName("temperature_2m") val temperature: String,
    @SerialName("apparent_temperature") val apparentTemperature: String,
    @SerialName("relative_humidity_2m") val humidity: String,
    @SerialName("wind_speed_10m") val windSpeed: String,
)
