package io.github.demiterhugo.weatherflow.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CurrentWeatherEntity::class], version = 1, exportSchema = true)
internal abstract class WeatherDatabase : RoomDatabase() {
    abstract fun currentWeatherDao(): CurrentWeatherDao
}
