package io.github.demiterhugo.weatherflow.data

import io.github.demiterhugo.weatherflow.data.remote.OpenMeteoCurrent
import io.github.demiterhugo.weatherflow.data.remote.OpenMeteoResponse
import io.github.demiterhugo.weatherflow.data.remote.OpenMeteoUnits
import io.github.demiterhugo.weatherflow.domain.model.Coordinates

internal val madrid = Coordinates(40.4168, -3.7038)
internal val barcelona = Coordinates(41.3874, 2.1686)

internal fun weatherResponse(time: Long = 1_700_000_000, temperature: Double = 18.5) = OpenMeteoResponse(
    current = OpenMeteoCurrent(time, temperature, 17.0, 65, 3.2, 2),
    currentUnits = OpenMeteoUnits("unixtime", "°C", "°C", "%", "m/s"),
)
