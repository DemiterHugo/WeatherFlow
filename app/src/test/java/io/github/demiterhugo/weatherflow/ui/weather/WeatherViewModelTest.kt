package io.github.demiterhugo.weatherflow.ui.weather

import androidx.lifecycle.ViewModelStore
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.CurrentWeather
import io.github.demiterhugo.weatherflow.domain.model.WeatherCondition
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation
import io.github.demiterhugo.weatherflow.domain.repository.DeviceLocationProvider
import io.github.demiterhugo.weatherflow.domain.repository.WeatherLocationRepository
import io.github.demiterhugo.weatherflow.domain.repository.WeatherRepository
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val store = ViewModelStore()
    private val barcelona = Coordinates(41.3874, 2.1686)
    private val weather = CurrentWeather(
        barcelona, 18.5, 17.0, 65, 3.2, WeatherCondition.PARTLY_CLOUDY,
        Instant.parse("2026-10-29T12:00:00Z"),
    )
    private val repository = FakeWeatherRepository()
    private val locations = FakeLocationRepository()
    private var locate: suspend () -> Coordinates = { Coordinates(48.8566, 2.3522) }
    private val deviceLocation = object : DeviceLocationProvider {
        override suspend fun currentCoordinates() = locate()
    }

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun startsWithLoadingAndRequestsBarcelonaExactlyOnce() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.onRefresh = { gate.await() }
        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.isLoading)
        runCurrent()

        assertEquals(listOf(barcelona), repository.requests)
        assertEquals(listOf(barcelona), repository.observations)
        assertTrue(viewModel.uiState.value.isRefreshing)
        assertNull(viewModel.uiState.value.weather)
    }

    @Test
    fun displaysCachedWeatherWhileRefreshIsPending() = runTest {
        repository.cached.value = weather
        repository.onRefresh = { awaitCancellation() }
        val viewModel = createViewModel()
        runCurrent()

        assertEquals(weather, viewModel.uiState.value.weather)
        assertTrue(viewModel.uiState.value.isRefreshing)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun successfulRefreshDisplaysNewDataAndStopsProgress() = runTest {
        repository.onRefresh = { repository.cached.value = weather }
        val viewModel = createViewModel()
        runCurrent()

        assertEquals(weather, viewModel.uiState.value.weather)
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertFalse(viewModel.uiState.value.refreshFailed)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun failedInitialRefreshExposesErrorWithoutData() = runTest {
        repository.onRefresh = { throw IOException("Offline") }
        val viewModel = createViewModel()
        runCurrent()

        assertTrue(viewModel.uiState.value.refreshFailed)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.weather)
    }

    @Test
    fun failedRefreshPreservesDisplayedCache() = runTest {
        repository.cached.value = weather
        repository.onRefresh = { throw IOException("Offline") }
        val viewModel = createViewModel()
        runCurrent()

        assertEquals(weather, viewModel.uiState.value.weather)
        assertTrue(viewModel.uiState.value.refreshFailed)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun retryClearsPreviousErrorAndCanRecover() = runTest {
        repository.onRefresh = { throw IOException("Offline") }
        val viewModel = createViewModel()
        runCurrent()
        repository.onRefresh = { repository.cached.value = weather }

        viewModel.refresh()
        assertFalse(viewModel.uiState.value.refreshFailed)
        runCurrent()

        assertEquals(weather, viewModel.uiState.value.weather)
        assertEquals(2, repository.requests.size)
        assertFalse(viewModel.uiState.value.refreshFailed)
    }

    @Test
    fun repeatedTapsDoNotStartOverlappingRequests() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.onRefresh = { gate.await() }
        val viewModel = createViewModel()
        runCurrent()

        repeat(5) { viewModel.refresh() }
        runCurrent()
        assertEquals(1, repository.requests.size)

        gate.complete(Unit)
        runCurrent()
        viewModel.refresh()
        runCurrent()
        assertEquals(2, repository.requests.size)
    }

    @Test
    fun failedCacheObservationCanBeRestartedOnRetry() = runTest {
        repository.failObservation = true
        val viewModel = createViewModel()
        runCurrent()
        assertTrue(viewModel.uiState.value.cacheFailed)

        repository.failObservation = false
        repository.cached.value = weather
        viewModel.refresh()
        runCurrent()

        assertEquals(weather, viewModel.uiState.value.weather)
        assertFalse(viewModel.uiState.value.cacheFailed)
        assertEquals(2, repository.observations.size)
    }

    @Test
    fun requestCancellationIsNotReportedAsAnError() = runTest {
        repository.onRefresh = { throw CancellationException("Cancelled") }
        val viewModel = createViewModel()
        runCurrent()

        assertFalse(viewModel.uiState.value.refreshFailed)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun clearingViewModelCancelsPendingRefresh() = runTest {
        var cancelled = false
        repository.onRefresh = {
            try {
                awaitCancellation()
            } finally {
                cancelled = true
            }
        }
        val viewModel = createViewModel()
        runCurrent()

        store.clear()
        runCurrent()

        assertTrue(cancelled)
        assertFalse(viewModel.uiState.value.refreshFailed)
    }

    @Test
    fun deviceLocationIsPersistedAndRefreshesSelectedCoordinates() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.useDeviceLocation()
        runCurrent()

        val selected = WeatherLocation.Device(Coordinates(48.8566, 2.3522))
        assertEquals(selected, locations.selected.value)
        assertEquals(selected, viewModel.uiState.value.location)
        assertEquals(listOf(barcelona, selected.coordinates), repository.requests)
        assertFalse(viewModel.uiState.value.isLocating)
    }

    @Test
    fun switchingLocationClearsPreviousWeatherAndCancelsItsRefresh() = runTest {
        repository.cached.value = weather
        var cancelled = false
        repository.onRefresh = { try { awaitCancellation() } finally { cancelled = true } }
        val viewModel = createViewModel()
        runCurrent()
        repository.cached.value = null
        viewModel.useDeviceLocation()
        runCurrent()

        assertTrue(cancelled)
        assertNull(viewModel.uiState.value.weather)
        assertEquals(Coordinates(48.8566, 2.3522), repository.requests.last())
    }

    @Test
    fun unavailableLocationPreservesSelectionAndWeather() = runTest {
        repository.cached.value = weather
        locate = { throw IOException("Location disabled") }
        val viewModel = createViewModel()
        runCurrent()
        viewModel.useDeviceLocation()
        runCurrent()

        assertEquals(WeatherLocation.Barcelona, viewModel.uiState.value.location)
        assertEquals(weather, viewModel.uiState.value.weather)
        assertTrue(viewModel.uiState.value.locationFailed)
        assertFalse(viewModel.uiState.value.isLocating)
        assertEquals(1, repository.requests.size)
    }

    @Test
    fun deniedPermissionDoesNotStartLocationLookup() = runTest {
        locate = { error("Permission denial must not access location") }
        val viewModel = createViewModel()
        runCurrent()
        viewModel.locationPermissionDenied()

        assertTrue(viewModel.uiState.value.locationPermissionDenied)
        assertEquals(WeatherLocation.Barcelona, locations.selected.value)
    }

    @Test
    fun repeatedLocationTapsStartOneLookupAndBarcelonaCancelsIt() = runTest {
        var requests = 0
        var cancelled = false
        locate = {
            requests++
            try { awaitCancellation() } finally { cancelled = true }
        }
        val viewModel = createViewModel()
        runCurrent()
        repeat(3) { viewModel.useDeviceLocation() }
        runCurrent()
        assertEquals(1, requests)
        assertTrue(viewModel.uiState.value.isLocating)

        viewModel.useBarcelona()
        runCurrent()
        assertTrue(cancelled)
        assertFalse(viewModel.uiState.value.isLocating)
        assertFalse(viewModel.uiState.value.locationFailed)
    }

    @Test
    fun savedDeviceSelectionIsRestoredAndCanReturnToBarcelona() = runTest {
        locations.selected.value = WeatherLocation.Device(Coordinates(48.8566, 2.3522))
        val viewModel = createViewModel()
        runCurrent()
        assertEquals(locations.selected.value, viewModel.uiState.value.location)

        viewModel.useBarcelona()
        runCurrent()
        assertEquals(WeatherLocation.Barcelona, locations.selected.value)
        assertEquals(barcelona, repository.requests.last())
    }

    private fun createViewModel(): WeatherViewModel = WeatherViewModel(repository, locations, deviceLocation).also {
        store.put("weather", it)
    }

    private class FakeLocationRepository : WeatherLocationRepository {
        val selected = MutableStateFlow<WeatherLocation>(WeatherLocation.Barcelona)
        override fun observeLocation(): Flow<WeatherLocation> = selected
        override suspend fun selectLocation(location: WeatherLocation) { selected.value = location }
    }

    private class FakeWeatherRepository : WeatherRepository {
        val cached = MutableStateFlow<CurrentWeather?>(null)
        val requests = mutableListOf<Coordinates>()
        val observations = mutableListOf<Coordinates>()
        var failObservation = false
        var onRefresh: suspend () -> Unit = {}

        override fun observeCurrentWeather(coordinates: Coordinates): Flow<CurrentWeather?> {
            observations += coordinates
            return if (failObservation) flow { throw IOException("Database unavailable") } else cached
        }

        override suspend fun refreshCurrentWeather(coordinates: Coordinates) {
            requests += coordinates
            onRefresh()
        }
    }
}
