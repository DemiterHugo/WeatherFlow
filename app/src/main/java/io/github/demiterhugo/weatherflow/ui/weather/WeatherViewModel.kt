package io.github.demiterhugo.weatherflow.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation
import io.github.demiterhugo.weatherflow.domain.repository.DeviceLocationProvider
import io.github.demiterhugo.weatherflow.domain.repository.WeatherLocationRepository
import io.github.demiterhugo.weatherflow.domain.repository.WeatherRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val locations: WeatherLocationRepository,
    private val deviceLocation: DeviceLocationProvider,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(WeatherUiState())
    val uiState = mutableUiState.asStateFlow()

    private var locationObservationJob: Job? = null
    private var observationJob: Job? = null
    private var refreshJob: Job? = null
    private var locatingJob: Job? = null
    private var locationReady = false

    init {
        observeLocation()
    }

    /** UI actions run on the main thread; duplicate refreshes and location requests are ignored. */
    fun refresh() {
        if (!locationReady) {
            if (locationObservationJob?.isActive != true) observeLocation()
            return
        }
        refreshWeather()
    }

    fun useDeviceLocation() {
        if (locatingJob?.isActive == true) return
        mutableUiState.update {
            it.copy(isLocating = true, locationFailed = false, locationPermissionDenied = false)
        }
        locatingJob = viewModelScope.launch {
            try {
                val selected = WeatherLocation.Device(deviceLocation.currentCoordinates())
                locations.selectLocation(selected)
                if (selected == mutableUiState.value.location) refreshWeather()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SecurityException) {
                locationPermissionDenied()
            } catch (_: Exception) {
                mutableUiState.update { it.copy(locationFailed = true) }
            } finally {
                mutableUiState.update { it.copy(isLocating = false) }
            }
        }
    }

    fun useBarcelona() {
        locatingJob?.cancel()
        mutableUiState.update {
            it.copy(isLocating = false, locationFailed = false, locationPermissionDenied = false)
        }
        locatingJob = viewModelScope.launch {
            try {
                locations.selectLocation(WeatherLocation.Barcelona)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableUiState.update { it.copy(locationFailed = true) }
            }
        }
    }

    fun locationPermissionDenied() {
        mutableUiState.update { it.copy(locationPermissionDenied = true, locationFailed = false) }
    }

    private fun observeLocation() {
        locationObservationJob = viewModelScope.launch {
            try {
                locations.observeLocation().collect { location ->
                    refreshJob?.cancelAndJoin()
                    observationJob?.cancelAndJoin()
                    locationReady = true
                    // Never display the previous place's weather under the new place's name.
                    mutableUiState.update { WeatherUiState(location = location, isLocating = it.isLocating) }
                    observeWeather(location)
                    refreshWeather()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                locationReady = false
                mutableUiState.update { it.copy(isReadingCache = false, locationFailed = true) }
            }
        }
    }

    private fun refreshWeather() {
        if (refreshJob?.isActive == true || !locationReady) return
        val location = mutableUiState.value.location
        if (observationJob?.isActive != true) observeWeather(location)
        mutableUiState.update { it.copy(isRefreshing = true, refreshFailed = false) }
        refreshJob = viewModelScope.launch {
            try {
                repository.refreshCurrentWeather(location.coordinates)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableUiState.update { it.copy(refreshFailed = true) }
            } finally {
                mutableUiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private fun observeWeather(location: WeatherLocation) {
        mutableUiState.update { it.copy(isReadingCache = true) }
        observationJob = viewModelScope.launch {
            try {
                repository.observeCurrentWeather(location.coordinates).collect { weather ->
                    mutableUiState.update {
                        it.copy(weather = weather, isReadingCache = false, cacheFailed = false)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableUiState.update { it.copy(isReadingCache = false, cacheFailed = true) }
            }
        }
    }
}
