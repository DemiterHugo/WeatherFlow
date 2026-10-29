package io.github.demiterhugo.weatherflow.ui.weather

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherFormattersTest {
    @Test
    fun formatsDecimalsWithSpanishLocale() {
        assertEquals("18,5", formatWeatherNumber(18.5, Locale.forLanguageTag("es-ES")))
    }

    @Test
    fun formatsDecimalsWithEnglishLocale() {
        assertEquals("18.5", formatWeatherNumber(18.5, Locale.US))
    }

    @Test
    fun roundsConvertedWindToOneDecimalPlace() {
        assertEquals("11.5", formatWeatherNumber(3.2 * 3.6, Locale.US))
    }

    @Test
    fun preservesNegativeTemperaturesWithoutUnnecessaryDecimals() {
        assertEquals("-5", formatWeatherNumber(-5.0, Locale.US))
    }

    @Test
    fun usesMadridWinterOffsetForObservationTime() {
        val formatted = formatObservationTime(
            Instant.parse("2026-10-29T12:00:00Z"), Locale.UK, ZoneId.of("Europe/Madrid"),
        )

        assertTrue(formatted.contains("13:00"))
        assertTrue(formatted.contains("2026"))
    }

    @Test
    fun usesMadridSummerOffsetForObservationTime() {
        val formatted = formatObservationTime(
            Instant.parse("2026-07-01T12:00:00Z"), Locale.UK, ZoneId.of("Europe/Madrid"),
        )

        assertTrue(formatted.contains("14:00"))
    }
}
