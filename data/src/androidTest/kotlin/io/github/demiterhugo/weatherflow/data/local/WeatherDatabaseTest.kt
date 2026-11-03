package io.github.demiterhugo.weatherflow.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeatherDatabaseTest {
    private lateinit var context: Context
    private lateinit var database: WeatherDatabase
    private lateinit var databaseName: String

    private val reading = CurrentWeatherEntity(
        latitude = 40.4168,
        longitude = -3.7038,
        temperatureCelsius = 18.5,
        feelsLikeCelsius = 17.0,
        humidityPercent = 65,
        windSpeedMetersPerSecond = 3.2,
        condition = "PARTLY_CLOUDY",
        observedAtEpochSecond = 1_700_000_000,
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        databaseName = "weatherflow-test-${UUID.randomUUID()}.db"
        database = openDatabase()
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun emptyDatabaseEmitsNull() = runBlocking {
        assertNull(withTimeout(5_000) { database.currentWeatherDao().observe(40.4168, -3.7038).first() })
    }

    @Test
    fun storesAndReadsAllFields() = runBlocking {
        database.currentWeatherDao().saveIfNewer(reading)

        assertEquals(reading, withTimeout(5_000) {
            database.currentWeatherDao().observe(reading.latitude, reading.longitude).first()
        })
    }

    @Test
    fun dataSurvivesClosingAndReopeningDatabase() = runBlocking {
        database.currentWeatherDao().saveIfNewer(reading)
        database.close()
        database = openDatabase()

        assertEquals(reading, withTimeout(5_000) {
            database.currentWeatherDao().observe(reading.latitude, reading.longitude).first()
        })
    }

    @Test
    fun updatesAnAlreadyActiveObserver() = runBlocking {
        val initial = CompletableDeferred<Unit>()
        val updated = CompletableDeferred<CurrentWeatherEntity>()
        val dao = database.currentWeatherDao()
        val observer = launch {
            dao.observe(reading.latitude, reading.longitude).collect { value ->
                if (value == null) initial.complete(Unit) else updated.complete(value)
            }
        }

        try {
            withTimeout(5_000) {
                initial.await()
                dao.saveIfNewer(reading)
                assertEquals(reading, updated.await())
            }
        } finally {
            observer.cancelAndJoin()
        }
    }

    @Test
    fun locationsDoNotOverwriteEachOther() = runBlocking {
        val other = reading.copy(latitude = 41.3874, longitude = 2.1686, temperatureCelsius = 22.0)
        val dao = database.currentWeatherDao()
        dao.saveIfNewer(reading)
        dao.saveIfNewer(other)

        withTimeout(5_000) {
            assertEquals(reading, dao.observe(reading.latitude, reading.longitude).first())
            assertEquals(other, dao.observe(other.latitude, other.longitude).first())
        }
    }

    @Test
    fun olderObservationCannotOverwriteNewerData() = runBlocking {
        val newer = reading.copy(observedAtEpochSecond = reading.observedAtEpochSecond + 900)
        val dao = database.currentWeatherDao()
        dao.saveIfNewer(newer)
        dao.saveIfNewer(reading)

        assertEquals(newer, withTimeout(5_000) { dao.observe(reading.latitude, reading.longitude).first() })
    }

    @Test
    fun sameTimestampCanReceiveACorrectedReading() = runBlocking {
        val corrected = reading.copy(temperatureCelsius = 19.0)
        val dao = database.currentWeatherDao()
        dao.saveIfNewer(reading)
        dao.saveIfNewer(corrected)

        assertEquals(corrected, withTimeout(5_000) {
            dao.observe(reading.latitude, reading.longitude).first()
        })
    }

    @Test
    fun concurrentWritesKeepNewestObservation() = runBlocking {
        val dao = database.currentWeatherDao()
        withTimeout(10_000) {
            coroutineScope {
                repeat(20) { index ->
                    launch(Dispatchers.IO) {
                        dao.saveIfNewer(reading.copy(observedAtEpochSecond = reading.observedAtEpochSecond + index))
                    }
                }
            }
            assertEquals(
                reading.observedAtEpochSecond + 19,
                dao.observe(reading.latitude, reading.longitude).first()?.observedAtEpochSecond,
            )
        }
    }

    private fun openDatabase(): WeatherDatabase =
        Room.databaseBuilder(context, WeatherDatabase::class.java, databaseName).build()
}
