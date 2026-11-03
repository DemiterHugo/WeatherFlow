package io.github.demiterhugo.weatherflow.widget

import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.CurrentWeather
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation
import io.github.demiterhugo.weatherflow.domain.repository.WeatherLocationRepository
import io.github.demiterhugo.weatherflow.domain.repository.WeatherRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetRefreshControllerTest {
    private val selected = MutableStateFlow<WeatherLocation>(WeatherLocation.Barcelona)
    private val locations = object : WeatherLocationRepository {
        override fun observeLocation(): Flow<WeatherLocation> = selected
        override suspend fun selectLocation(location: WeatherLocation) { selected.value = location }
    }
    private val events = mutableListOf<String>()
    private var fetch: suspend (Coordinates) -> Unit = {}
    private val repository = object : WeatherRepository {
        override fun observeCurrentWeather(coordinates: Coordinates): Flow<CurrentWeather?> = error("Refresh does not observe cache")
        override suspend fun refreshCurrentWeather(coordinates: Coordinates) {
            events += "fetch"
            fetch(coordinates)
        }
    }
    private val shown = mutableListOf<DisplayState>()
    private val display = object : WidgetRefreshDisplay {
        override suspend fun show(location: WeatherLocation?, refreshing: Boolean, failed: Boolean) {
            shown += DisplayState(location, refreshing, failed)
            events += if (refreshing) "loading" else "finished"
        }
    }
    private val controller = WidgetRefreshController(locations, repository, display)

    @Test
    fun showsProgressBeforeRequestAndClearsItAfterSuccessfulStorage() = runTest {
        assertTrue(controller.refresh())
        assertEquals(listOf("loading", "fetch", "finished"), events)
        assertEquals(DisplayState(WeatherLocation.Barcelona, true, false), shown.first())
        assertEquals(DisplayState(WeatherLocation.Barcelona, false, false), shown.last())
    }

    @Test
    fun refreshUsesSavedDeviceCoordinates() = runTest {
        val device = WeatherLocation.Device(Coordinates(48.8566, 2.3522))
        selected.value = device
        fetch = { assertEquals(device.coordinates, it) }
        assertTrue(controller.refresh())
        assertEquals(device, shown.last().location)
    }

    @Test
    fun failedRefreshClearsProgressAndMarksCachedDataAsFailed() = runTest {
        fetch = { throw IOException("Offline") }
        assertFalse(controller.refresh())
        assertEquals(DisplayState(WeatherLocation.Barcelona, false, true), shown.last())
    }

    @Test
    fun cancellationClearsProgressAndPropagatesWithoutErrorNotice() = runTest {
        fetch = { awaitCancellation() }
        val job = launch { controller.refresh() }
        runCurrent()
        assertTrue(shown.last().refreshing)
        job.cancelAndJoin()
        assertEquals(DisplayState(WeatherLocation.Barcelona, false, false), shown.last())
        assertTrue(job.isCancelled)
    }

    @Test
    fun switchingSelectionDuringFetchKeepsStatusScopedToOriginalLocation() = runTest {
        val device = WeatherLocation.Device(Coordinates(48.8566, 2.3522))
        fetch = { selected.value = device; throw IOException("Offline") }
        assertFalse(controller.refresh())
        assertEquals(WeatherLocation.Barcelona, shown.last().location)
        assertEquals(device, selected.value)
    }

    @Test(expected = CancellationException::class)
    fun explicitCancellationIsNotConvertedToFailure() = runTest {
        fetch = { throw CancellationException("Stopped") }
        controller.refresh()
    }

    private data class DisplayState(val location: WeatherLocation?, val refreshing: Boolean, val failed: Boolean)
}
