package io.github.demiterhugo.weatherflow

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class ApplicationStartupTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var repository: TestWeatherRepository

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun mainActivityDisplaysWeatherFromInjectedRepository() {
        composeRule.onNodeWithText("Madrid").assertIsDisplayed()
        composeRule.onNodeWithTag("weather_temperature").assertTextContains("°C", substring = true)
        composeRule.runOnIdle { assertEquals(1, repository.refreshCount) }
    }

    @Test
    fun refreshButtonReachesInjectedViewModel() {
        composeRule.onNodeWithTag("weather_content").performScrollToNode(hasTestTag("weather_refresh"))
        composeRule.onNodeWithTag("weather_refresh").performClick()

        composeRule.runOnIdle { assertEquals(2, repository.refreshCount) }
    }

    @Test
    fun activityRecreationRetainsViewModelWithoutAnotherRequest() {
        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithText("Madrid").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(1, repository.refreshCount)
        }
    }
}
