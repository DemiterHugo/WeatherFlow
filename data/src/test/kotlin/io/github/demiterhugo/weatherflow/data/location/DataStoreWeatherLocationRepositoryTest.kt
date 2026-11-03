package io.github.demiterhugo.weatherflow.data.location

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreWeatherLocationRepositoryTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun emptyPreferencesSelectBarcelona() = runBlocking {
        withRepository { assertEquals(WeatherLocation.Barcelona, it.observeLocation().first()) }
    }

    @Test
    fun savedDeviceCoordinatesSurviveReopeningStorage() = runBlocking {
        val selected = WeatherLocation.Device(Coordinates(48.8566, 2.3522))
        withRepository { it.selectLocation(selected) }
        withRepository { assertEquals(selected, it.observeLocation().first()) }
    }

    @Test
    fun choosingBarcelonaRemovesPreviousDeviceSelection() = runBlocking {
        withRepository {
            it.selectLocation(WeatherLocation.Device(Coordinates(48.8566, 2.3522)))
            it.selectLocation(WeatherLocation.Barcelona)
            assertEquals(WeatherLocation.Barcelona, it.observeLocation().first())
        }
    }

    private suspend fun withRepository(block: suspend (DataStoreWeatherLocationRepository) -> Unit) {
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope) {
            File(temporaryFolder.root, "location.preferences_pb")
        }
        try {
            block(DataStoreWeatherLocationRepository(store))
        } finally {
            scope.cancel()
            job.join()
        }
    }
}
