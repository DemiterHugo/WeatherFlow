package io.github.demiterhugo.weatherflow.widget

import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.CurrentWeather
import io.github.demiterhugo.weatherflow.domain.model.WeatherCondition
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation
import io.github.demiterhugo.weatherflow.domain.repository.WeatherLocationRepository
import io.github.demiterhugo.weatherflow.domain.repository.WeatherRepository
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetWeatherReaderTest {
    private val selected = MutableStateFlow<WeatherLocation>(WeatherLocation.Barcelona)
    private val locations = object : WeatherLocationRepository {
        override fun observeLocation(): Flow<WeatherLocation> = selected
        override suspend fun selectLocation(location: WeatherLocation) { selected.value = location }
    }
    private val caches = mutableMapOf<Coordinates, MutableStateFlow<CurrentWeather?>>()
    private var readsFail = false
    private val repository = object : WeatherRepository {
        override fun observeCurrentWeather(coordinates: Coordinates): Flow<CurrentWeather?> =
            if (readsFail) flow { throw IOException("Storage unavailable") }
            else caches.getOrPut(coordinates) { MutableStateFlow(null) }
        override suspend fun refreshCurrentWeather(coordinates: Coordinates) = error("Reading a widget must not access the network")
    }
    private val reader = WidgetWeatherReader(locations, repository)

    @Test
    fun absentCacheShowsSelectedLocationWithoutFabricatingWeather() = runTest {
        val snapshot = reader.read()
        assertEquals(WeatherLocation.Barcelona, snapshot.location)
        assertNull(snapshot.weather)
        assertFalse(snapshot.readFailed)
    }

    @Test
    fun readsWeatherForSavedDeviceCoordinates() = runTest {
        val location = WeatherLocation.Device(Coordinates(48.8566, 2.3522))
        selected.value = location
        caches[location.coordinates] = MutableStateFlow(weather(location.coordinates))
        assertEquals(weather(location.coordinates), reader.read().weather)
        assertEquals(location, reader.read().location)
    }

    @Test
    fun databaseFailureKeepsLocationAndExposesError() = runTest {
        readsFail = true
        val snapshot = reader.read()
        assertTrue(snapshot.readFailed)
        assertEquals(WeatherLocation.Barcelona, snapshot.location)
        assertNull(snapshot.weather)
    }

    @Test
    fun locationStorageFailureDoesNotInventACity() = runTest {
        val broken = object : WeatherLocationRepository {
            override fun observeLocation(): Flow<WeatherLocation> = flow { throw IOException("Storage unavailable") }
            override suspend fun selectLocation(location: WeatherLocation) = Unit
        }
        val snapshot = WidgetWeatherReader(broken, repository).read()
        assertNull(snapshot.location)
        assertTrue(snapshot.readFailed)
    }

    @Test(expected = CancellationException::class)
    fun cancellationPropagates() = runTest {
        val cancelled = object : WeatherLocationRepository {
            override fun observeLocation(): Flow<WeatherLocation> = flow { throw CancellationException("Stopped") }
            override suspend fun selectLocation(location: WeatherLocation) = Unit
        }
        WidgetWeatherReader(cancelled, repository).read()
    }

    @Test
    fun activeSessionReceivesCacheChangesAndStopsObservingOldLocation() = runTest {
        val results = mutableListOf<WidgetWeatherSnapshot>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            reader.observe().onEach { results += it }.collect()
        }
        runCurrent()
        val barcelona = WeatherLocation.Barcelona.coordinates
        caches.getValue(barcelona).value = weather(barcelona)
        runCurrent()
        assertEquals(weather(barcelona), results.last().weather)

        val device = WeatherLocation.Device(Coordinates(48.8566, 2.3522))
        selected.value = device
        runCurrent()
        assertEquals(device, results.last().location)
        assertNull(results.last().weather)

        caches.getValue(barcelona).value = weather(barcelona).copy(temperatureCelsius = 99.0)
        runCurrent()
        assertNull(results.last().weather)
        caches.getValue(device.coordinates).value = weather(device.coordinates)
        runCurrent()
        assertEquals(device.coordinates, results.last().weather?.coordinates)
    }

    private fun weather(coordinates: Coordinates) = CurrentWeather(
        coordinates, 18.5, 17.0, 65, 3.2, WeatherCondition.PARTLY_CLOUDY,
        Instant.parse("2026-10-29T12:00:00Z"),
    )
}
