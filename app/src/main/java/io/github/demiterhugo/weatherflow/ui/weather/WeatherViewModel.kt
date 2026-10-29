package io.github.demiterhugo.weatherflow.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.repository.WeatherRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(WeatherUiState())
    val uiState = mutableUiState.asStateFlow()

    private var observationJob: Job? = null
    private var refreshJob: Job? = null

    init {
        observeWeather()
        refresh()
    }

    /** UI actions run on the main thread; ignore taps while a refresh is in progress. */
    fun refresh() {
        if (refreshJob?.isActive == true) return

        if (observationJob?.isActive != true) observeWeather()
        mutableUiState.update { it.copy(isRefreshing = true, refreshFailed = false) }
        refreshJob = viewModelScope.launch {
            try {
                repository.refreshCurrentWeather(MADRID)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableUiState.update { it.copy(refreshFailed = true) }
            } finally {
                mutableUiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private fun observeWeather() {
        mutableUiState.update { it.copy(isReadingCache = true) }
        observationJob = viewModelScope.launch {
            try {
                repository.observeCurrentWeather(MADRID).collect { weather ->
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

    private companion object {
        val MADRID = Coordinates(latitude = 40.4168, longitude = -3.7038)
    }
}
