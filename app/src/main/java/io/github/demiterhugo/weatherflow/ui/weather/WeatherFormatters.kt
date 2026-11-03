package io.github.demiterhugo.weatherflow.ui.weather

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal fun formatWeatherNumber(value: Double, locale: Locale): String =
    NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = 1
        isGroupingUsed = false
    }.format(value)

internal fun formatObservationTime(time: Instant, locale: Locale, zone: ZoneId): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(locale)
        .withZone(zone)
        .format(time)
