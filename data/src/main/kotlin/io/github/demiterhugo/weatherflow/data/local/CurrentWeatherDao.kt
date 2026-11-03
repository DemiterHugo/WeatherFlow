package io.github.demiterhugo.weatherflow.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal abstract class CurrentWeatherDao {
    @Query("SELECT * FROM current_weather WHERE latitude = :latitude AND longitude = :longitude")
    abstract fun observe(latitude: Double, longitude: Double): Flow<CurrentWeatherEntity?>

    @Query("SELECT * FROM current_weather WHERE latitude = :latitude AND longitude = :longitude")
    protected abstract suspend fun get(latitude: Double, longitude: Double): CurrentWeatherEntity?

    @Upsert
    protected abstract suspend fun upsert(weather: CurrentWeatherEntity)

    /** Compare and write atomically, including when concurrent requests finish out of order. */
    @Transaction
    open suspend fun saveIfNewer(weather: CurrentWeatherEntity) {
        val previous = get(weather.latitude, weather.longitude)
        if (previous == null || weather.observedAtEpochSecond >= previous.observedAtEpochSecond) {
            upsert(weather)
        }
    }
}
