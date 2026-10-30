package io.github.demiterhugo.weatherflow.widget.radar

import io.github.demiterhugo.weatherflow.domain.model.*
import io.github.demiterhugo.weatherflow.domain.repository.*
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetStatus
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetRadarRefreshControllerTest {
    private val selected = MutableStateFlow<WeatherLocation>(WeatherLocation.Barcelona)
    private val locations = object : WeatherLocationRepository {
        override fun observeLocation() = selected
        override suspend fun selectLocation(location: WeatherLocation) { selected.value = location }
    }
    private var radarFetch: suspend () -> Unit = {}
    private var mapFetch: suspend () -> Unit = {}
    private var mapsRequested = 0
    private val radar = object : RadarRepository {
        override fun observeRadar(coordinates: Coordinates): Flow<RadarSequence?> = error("No reads during refresh")
        override suspend fun refreshRadar(coordinates: Coordinates) { assertEquals(selected.value.coordinates, coordinates); radarFetch() }
    }
    private val maps = object : RadarMapRepository {
        override fun observeMap(coordinates: Coordinates): Flow<RadarImage?> = error("No reads during refresh")
        override suspend fun refreshMap(coordinates: Coordinates) { mapsRequested++; mapFetch() }
    }
    private val shown = mutableListOf<WidgetStatus>()
    private fun controller(needsMap: Boolean = true) = WidgetRadarRefreshController(locations, radar, maps, { needsMap }) { shown += it }

    @Test fun successPublishesScopedProgressAndIndependentResults() = runTest {
        assertTrue(controller().refresh())
        assertTrue(shown.first().radarRefreshing)
        assertEquals(WidgetStatus(WeatherLocation.Barcelona), shown.last())
        assertEquals(1, mapsRequested)
    }

    @Test fun compactWidgetsDoNotDownloadUnseenMapTiles() = runTest {
        assertTrue(controller(needsMap = false).refresh())
        assertEquals(0, mapsRequested)
    }

    @Test fun radarFailureStillAllowsMapUpdateAndPreservesWeatherStatus() = runTest {
        radarFetch = { throw IOException("Radar offline") }
        assertFalse(controller().refresh())
        assertEquals(1, mapsRequested)
        assertTrue(shown.last().radarFailed)
        assertFalse(shown.last().mapFailed)
        assertFalse(shown.last().failed)
    }

    @Test fun mapFailureDoesNotBecomeWeatherOrRadarFailure() = runTest {
        mapFetch = { throw IOException("Map offline") }
        assertFalse(controller().refresh())
        assertTrue(shown.last().mapFailed)
        assertFalse(shown.last().radarFailed)
    }

    @Test fun operationTimeoutIsReportedWhileExternalCancellationPropagates() = runTest {
        radarFetch = { withTimeout(100) { delay(1000) } }
        assertFalse(controller().refresh())
        assertTrue(shown.last().radarFailed)
        radarFetch = { awaitCancellation() }
        val job = launch { controller().refresh() }
        runCurrent()
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
        assertFalse(shown.last().radarRefreshing)
    }
}
