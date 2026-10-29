package io.github.demiterhugo.weatherflow.domain.model

/** One shared location for the weather screen and all widget instances. */
sealed interface WeatherLocation {
    val coordinates: Coordinates

    data object Barcelona : WeatherLocation {
        override val coordinates = Coordinates(41.3874, 2.1686)
    }

    /** A saved foreground location fix; this does not imply background tracking. */
    data class Device(override val coordinates: Coordinates) : WeatherLocation
}
