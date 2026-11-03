package io.github.demiterhugo.weatherflow.domain.model

/** Provider-independent conditions. Map unrecognized provider codes to [UNKNOWN]. */
enum class WeatherCondition {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    FOG,
    DRIZZLE,
    RAIN,
    SNOW,
    THUNDERSTORM,
    UNKNOWN,
}
