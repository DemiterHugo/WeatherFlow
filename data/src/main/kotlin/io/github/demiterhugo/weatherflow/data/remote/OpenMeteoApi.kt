package io.github.demiterhugo.weatherflow.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

internal interface OpenMeteoApi {

    @GET(
        "v1/forecast" +
            "?current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,weather_code" +
            "&temperature_unit=celsius&wind_speed_unit=ms&timeformat=unixtime&timezone=GMT"
    )
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
    ): OpenMeteoResponse
}
