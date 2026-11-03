package io.github.demiterhugo.weatherflow.widget.radar

import io.github.demiterhugo.weatherflow.domain.model.*
import io.github.demiterhugo.weatherflow.domain.repository.*
import java.io.IOException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class WidgetRadarReaderTest {
    private var requests = 0
    private var fail = false
    private val radar = object : RadarRepository {
        override fun observeRadar(coordinates: Coordinates): Flow<RadarSequence?> = flow {
            if (fail) throw IOException("cache error")
            emit(null)
        }
        override suspend fun refreshRadar(coordinates: Coordinates) { requests++ }
    }
    private val maps = object : RadarMapRepository {
        override fun observeMap(coordinates: Coordinates): Flow<RadarImage?> = flowOf(null)
        override suspend fun refreshMap(coordinates: Coordinates) { requests++ }
    }

    @Test fun readingMissingCacheNeverFetchesOrChangesSelectedLocation() = runTest {
        val location = WeatherLocation.Device(Coordinates(48.8566, 2.3522))
        val value = WidgetRadarReader(radar, maps).read(location)
        assertEquals(location, value.location)
        assertNull(value.sequence)
        assertNull(value.map)
        assertEquals(0, requests)
    }

    @Test fun storageFailureIsReportedAndUnknownLocationDoesNotReadOrFetch() = runTest {
        fail = true
        assertTrue(WidgetRadarReader(radar, maps).read(WeatherLocation.Barcelona).readFailed)
        assertEquals(WidgetRadarSnapshot(), WidgetRadarReader(radar, maps).read(null))
        assertEquals(0, requests)
    }
}
