package io.github.demiterhugo.weatherflow

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApplicationStartupTest {

    @Test
    fun mainActivityStartsWithHiltApplication() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.application is WeatherFlowApplication)
            }
        }
    }
}
