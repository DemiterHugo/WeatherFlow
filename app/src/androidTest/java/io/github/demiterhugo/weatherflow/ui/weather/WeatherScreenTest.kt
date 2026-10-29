package io.github.demiterhugo.weatherflow.ui.weather

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.demiterhugo.weatherflow.R
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.testWeather
import io.github.demiterhugo.weatherflow.ui.theme.WeatherFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeatherScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun loadingShowsProgressAndDisablesRefresh() {
        showScreen(WeatherUiState(isRefreshing = true))

        composeRule.onNodeWithTag("weather_loading").assertIsDisplayed()
        scrollToTag("weather_refresh")
        composeRule.onNodeWithTag("weather_refresh").assertIsNotEnabled()
    }

    @Test
    fun contentShowsWeatherAndConvertsWindForDisplay() {
        showScreen(WeatherUiState(weather = testWeather, isReadingCache = false))

        composeRule.onNodeWithText("Barcelona").assertIsDisplayed()
        composeRule.onNodeWithTag("weather_temperature").assertIsDisplayed()
        val windLabel = context.getString(R.string.weather_wind)
        composeRule.onNodeWithTag("weather_content").performScrollToNode(hasText(windLabel))
        val locale = context.resources.configuration.locales[0]
        composeRule.onNodeWithText(context.getString(
            R.string.weather_wind_kmh, formatWeatherNumber(11.52, locale),
        )).assertIsDisplayed()
    }

    @Test
    fun initialErrorOffersRetry() {
        var retries = 0
        showScreen(WeatherUiState(isReadingCache = false, refreshFailed = true), onRefresh = { retries++ })

        composeRule.onNodeWithText(context.getString(R.string.weather_refresh_error_empty)).assertIsDisplayed()
        scrollToTag("weather_refresh")
        composeRule.onNodeWithTag("weather_refresh").performClick()

        composeRule.runOnIdle { assertEquals(1, retries) }
    }

    @Test
    fun refreshFailureKeepsCachedWeatherVisible() {
        showScreen(WeatherUiState(weather = testWeather, isReadingCache = false, refreshFailed = true))

        composeRule.onNodeWithText(context.getString(R.string.weather_refresh_error_cached)).assertIsDisplayed()
        scrollToTag("weather_temperature")
        composeRule.onNodeWithTag("weather_temperature").assertIsDisplayed()
    }

    @Test
    fun cachedWeatherRemainsVisibleDuringRefresh() {
        showScreen(WeatherUiState(weather = testWeather, isReadingCache = false, isRefreshing = true))

        composeRule.onNodeWithTag("weather_temperature").assertIsDisplayed()
        scrollToTag("weather_refreshing")
        composeRule.onNodeWithTag("weather_refreshing").assertIsDisplayed()
        scrollToTag("weather_refresh")
        composeRule.onNodeWithTag("weather_refresh").assertIsNotEnabled()
    }

    @Test
    fun storageErrorUsesItsOwnMessage() {
        showScreen(WeatherUiState(isReadingCache = false, cacheFailed = true))

        composeRule.onNodeWithText(context.getString(R.string.weather_cache_error)).assertIsDisplayed()
    }

    @Test
    fun providerAttributionIsVisibleAndClickable() {
        var clicks = 0
        showScreen(
            WeatherUiState(weather = testWeather, isReadingCache = false),
            onAttributionClick = { clicks++ },
        )
        val attribution = context.getString(R.string.weather_attribution)
        composeRule.onNodeWithTag("weather_content").performScrollToNode(hasText(attribution))

        composeRule.onNodeWithText(attribution).assertIsDisplayed().performClick()

        composeRule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun largeTextCanScrollToRefreshControl() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                WeatherFlowTheme(dynamicColor = false) {
                    WeatherScreen(WeatherUiState(weather = testWeather, isReadingCache = false), {}, {})
                }
            }
        }

        scrollToTag("weather_refresh")
        composeRule.onNodeWithTag("weather_refresh").assertIsDisplayed()
    }

    @Test
    fun permissionDenialLeavesBarcelonaVisibleAndExplainsResult() {
        showScreen(WeatherUiState(isReadingCache = false, locationPermissionDenied = true))
        composeRule.onNodeWithText("Barcelona").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.weather_location_denied)).assertIsDisplayed()
    }

    @Test
    fun deviceSelectionOffersBothLocationActions() {
        var currentClicks = 0
        var barcelonaClicks = 0
        composeRule.setContent {
            WeatherFlowTheme(dynamicColor = false) {
                WeatherScreen(
                    WeatherUiState(location = WeatherLocation.Device(Coordinates(48.8566, 2.3522))), {}, {},
                    onUseDeviceLocation = { currentClicks++ }, onUseBarcelona = { barcelonaClicks++ },
                )
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.weather_location_device)).assertIsDisplayed()
        composeRule.onNodeWithTag("weather_use_location").performClick()
        composeRule.onNodeWithTag("weather_use_barcelona").performClick()
        composeRule.runOnIdle {
            assertEquals(1, currentClicks)
            assertEquals(1, barcelonaClicks)
        }
    }

    @Test
    fun widgetButtonReachesPinRequestCallback() {
        var clicks = 0
        composeRule.setContent {
            WeatherFlowTheme(dynamicColor = false) {
                WeatherScreen(
                    WeatherUiState(weather = testWeather, isReadingCache = false), {}, {},
                    onAddWidget = { clicks++ },
                )
            }
        }
        scrollToTag("weather_add_widget")
        composeRule.onNodeWithTag("weather_add_widget").performClick()
        composeRule.runOnIdle { assertEquals(1, clicks) }
    }

    private fun showScreen(
        state: WeatherUiState,
        onRefresh: () -> Unit = {},
        onAttributionClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            WeatherFlowTheme(dynamicColor = false) {
                WeatherScreen(state, onRefresh, onAttributionClick)
            }
        }
    }

    private fun scrollToTag(tag: String) {
        composeRule.onNodeWithTag("weather_content").performScrollToNode(hasTestTag(tag))
    }
}
