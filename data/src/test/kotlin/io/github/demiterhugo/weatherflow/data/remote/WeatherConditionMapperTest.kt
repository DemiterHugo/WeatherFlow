package io.github.demiterhugo.weatherflow.data.remote

import io.github.demiterhugo.weatherflow.domain.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class WeatherConditionMapperTest(private val code: Int, private val expected: WeatherCondition) {
    @Test
    fun mapsProviderCodeToDomainCondition() {
        assertEquals(expected, code.toWeatherCondition())
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "WMO {0}: {1}")
        fun cases(): List<Array<Any>> = buildList {
            fun codes(condition: WeatherCondition, vararg values: Int) {
                values.forEach { add(arrayOf(it, condition)) }
            }
            codes(WeatherCondition.CLEAR, 0, 1)
            codes(WeatherCondition.PARTLY_CLOUDY, 2)
            codes(WeatherCondition.CLOUDY, 3)
            codes(WeatherCondition.FOG, 45, 48)
            codes(WeatherCondition.DRIZZLE, 51, 53, 55, 56, 57)
            codes(WeatherCondition.RAIN, 61, 63, 65, 66, 67, 80, 81, 82)
            codes(WeatherCondition.SNOW, 71, 73, 75, 77, 85, 86)
            codes(WeatherCondition.THUNDERSTORM, 95, 96, 97, 99)
            codes(WeatherCondition.UNKNOWN, -1, 4, 999)
        }
    }
}
