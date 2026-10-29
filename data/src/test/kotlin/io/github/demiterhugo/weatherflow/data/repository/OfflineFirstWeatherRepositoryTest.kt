package io.github.demiterhugo.weatherflow.data.repository

import io.github.demiterhugo.weatherflow.data.barcelona
import io.github.demiterhugo.weatherflow.data.local.CurrentWeatherDao
import io.github.demiterhugo.weatherflow.data.local.CurrentWeatherEntity
import io.github.demiterhugo.weatherflow.data.local.toDomain
import io.github.demiterhugo.weatherflow.data.local.toEntity
import io.github.demiterhugo.weatherflow.data.madrid
import io.github.demiterhugo.weatherflow.data.remote.OpenMeteoApi
import io.github.demiterhugo.weatherflow.data.remote.OpenMeteoResponse
import io.github.demiterhugo.weatherflow.data.remote.toDomain
import io.github.demiterhugo.weatherflow.data.weatherResponse
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.CurrentWeather
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstWeatherRepositoryTest {
    private val api = FakeWeatherApi()
    private val dao = FakeWeatherDao()
    private val repository = OfflineFirstWeatherRepository(api, dao)

    @Test
    fun observingEmptyCacheEmitsNullWithoutFetching() = runTest {
        assertNull(repository.observeCurrentWeather(madrid).first())
        assertTrue(api.requests.isEmpty())
    }

    @Test
    fun observingCachedWeatherEmitsItImmediatelyWithoutFetching() = runTest {
        val weather = weatherResponse().toDomain(madrid)
        dao.saveIfNewer(weather.toEntity())

        assertEquals(weather, repository.observeCurrentWeather(madrid).first())
        assertTrue(api.requests.isEmpty())
    }

    @Test
    fun successfulRefreshPublishesToActiveObserversAndNewCollectors() = runTest {
        val emissions = mutableListOf<CurrentWeather?>()
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            repository.observeCurrentWeather(madrid).toList(emissions)
        }

        repository.refreshCurrentWeather(madrid)
        runCurrent()

        val expected = weatherResponse().toDomain(madrid)
        assertEquals(listOf(null, expected), emissions)
        assertEquals(expected, repository.observeCurrentWeather(madrid).first())
        assertEquals(listOf(madrid), api.requests)
    }

    @Test
    fun refreshingOneLocationPreservesOtherLocations() = runTest {
        val otherWeather = weatherResponse(temperature = 12.0).toDomain(barcelona)
        dao.saveIfNewer(otherWeather.toEntity())

        repository.refreshCurrentWeather(madrid)

        assertEquals(otherWeather, repository.observeCurrentWeather(barcelona).first())
        assertEquals(madrid, repository.observeCurrentWeather(madrid).first()?.coordinates)
    }

    @Test
    fun networkFailurePropagatesAndPreservesCache() = runTest {
        val cached = weatherResponse().toDomain(madrid)
        dao.saveIfNewer(cached.toEntity())
        val failure = IOException("Offline")
        api.onRequest = { throw failure }

        assertSame(failure, runCatching { repository.refreshCurrentWeather(madrid) }.exceptionOrNull())
        assertEquals(cached, repository.observeCurrentWeather(madrid).first())
    }

    @Test
    fun invalidProviderResponseCannotOverwriteCache() = runTest {
        val cached = weatherResponse().toDomain(madrid)
        dao.saveIfNewer(cached.toEntity())
        val response = weatherResponse()
        api.onRequest = { response.copy(current = response.current.copy(humidity = 101)) }

        val failure = runCatching { repository.refreshCurrentWeather(madrid) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertEquals(cached, repository.observeCurrentWeather(madrid).first())
    }

    @Test
    fun storageFailurePropagatesAndPreservesCache() = runTest {
        val cached = weatherResponse().toDomain(madrid)
        dao.saveIfNewer(cached.toEntity())
        val failure = IOException("Disk full")
        dao.writeFailure = failure

        assertSame(failure, runCatching { repository.refreshCurrentWeather(madrid) }.exceptionOrNull())
        assertEquals(cached, repository.observeCurrentWeather(madrid).first())
    }

    @Test
    fun sourceCancellationPropagatesWithoutClearingCache() = runTest {
        val cached = weatherResponse().toDomain(madrid)
        dao.saveIfNewer(cached.toEntity())
        val failure = CancellationException("Request cancelled")
        api.onRequest = { throw failure }

        assertSame(failure, runCatching { repository.refreshCurrentWeather(madrid) }.exceptionOrNull())
        assertEquals(cached, repository.observeCurrentWeather(madrid).first())
    }

    @Test
    fun cancelledCallerCannotSaveAResponse() = runTest {
        api.onRequest = {
            currentCoroutineContext().cancel()
            weatherResponse()
        }

        val refresh = launch { repository.refreshCurrentWeather(madrid) }
        refresh.join()

        assertTrue(refresh.isCancelled)
        assertNull(repository.observeCurrentWeather(madrid).first())
    }

    @Test
    fun lateOlderResponseCannotOverwriteNewerObservation() = runTest {
        val delayed = CompletableDeferred<OpenMeteoResponse>()
        var calls = 0
        val newer = weatherResponse(time = 1_700_000_900, temperature = 20.0)
        api.onRequest = { if (++calls == 1) delayed.await() else newer }

        val earlierRequest = launch { repository.refreshCurrentWeather(madrid) }
        runCurrent()
        repository.refreshCurrentWeather(madrid)
        delayed.complete(weatherResponse())
        earlierRequest.join()

        assertEquals(newer.toDomain(madrid), repository.observeCurrentWeather(madrid).first())
    }

    private class FakeWeatherApi : OpenMeteoApi {
        val requests = mutableListOf<Coordinates>()
        var onRequest: suspend () -> OpenMeteoResponse = { weatherResponse() }

        override suspend fun getCurrentWeather(latitude: Double, longitude: Double): OpenMeteoResponse {
            requests += Coordinates(latitude, longitude)
            return onRequest()
        }
    }

    private class FakeWeatherDao : CurrentWeatherDao() {
        private val rows = MutableStateFlow<Map<Coordinates, CurrentWeatherEntity>>(emptyMap())
        var writeFailure: IOException? = null

        override fun observe(latitude: Double, longitude: Double): Flow<CurrentWeatherEntity?> =
            rows.map { it[Coordinates(latitude, longitude)] }

        override suspend fun get(latitude: Double, longitude: Double): CurrentWeatherEntity? =
            rows.value[Coordinates(latitude, longitude)]

        override suspend fun upsert(weather: CurrentWeatherEntity) {
            writeFailure?.let { throw it }
            rows.value += (weather.toDomain().coordinates to weather)
        }
    }
}
