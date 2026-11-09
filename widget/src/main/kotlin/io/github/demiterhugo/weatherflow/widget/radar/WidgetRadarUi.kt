package io.github.demiterhugo.weatherflow.widget.radar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.format.DateFormat
import io.github.demiterhugo.weatherflow.domain.model.RadarCoverageAvailability
import io.github.demiterhugo.weatherflow.domain.model.RadarImage
import io.github.demiterhugo.weatherflow.widget.R
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetStatus
import java.time.Clock
import java.time.Duration
import java.util.Date

internal data class RadarFrameUi(val overlay: Bitmap, val label: String)

internal data class WidgetRadarUi(
    val map: Bitmap? = null,
    val frames: List<RadarFrameUi> = emptyList(),
    val message: String? = null,
    val refreshing: Boolean = false,
)

internal fun WidgetRadarSnapshot.toUi(
    context: Context, status: WidgetStatus, clock: Clock = Clock.systemUTC(),
): WidgetRadarUi {
    val sequence = sequence
    val message = buildList {
        when {
            status.radarRefreshing -> add(context.getString(R.string.widget_radar_updating))
            status.radarFailed || readFailed -> add(context.getString(R.string.widget_radar_error))
        }
        if (sequence != null) {
            when {
                !sequence.coverage.centerCovered -> add(context.getString(R.string.widget_radar_no_coverage))
                sequence.coverage.availability == RadarCoverageAvailability.PARTIAL ->
                    add(context.getString(R.string.widget_radar_partial_coverage))
            }
            if (Duration.between(sequence.frames.last().generatedAt, clock.instant()) > Duration.ofMinutes(30)) {
                add(context.getString(R.string.widget_radar_stale))
            }
        }
        if (sequence == null && !status.radarRefreshing) add(context.getString(R.string.widget_radar_empty))
        if (map == null && sequence != null) add(context.getString(R.string.widget_radar_map_unavailable))
        else if (status.mapFailed) add(context.getString(R.string.widget_radar_map_cached))
    }.distinct().joinToString(" · ").ifEmpty { null }
    if (sequence == null || map == null) return WidgetRadarUi(message = message, refreshing = status.radarRefreshing)
    return try {
        val background = decode(map)
        val mask = decode(sequence.coverage.mask)
        val frames = try {
            sequence.frames.map { frame ->
                val source = decode(frame.image)
                val overlay = try { source.copy(Bitmap.Config.ARGB_8888, true) } finally { source.recycle() }
                val canvas = Canvas(overlay)
                canvas.drawBitmap(mask, 0f, 0f, Paint().apply { alpha = 110 })
                val marker = Paint(Paint.ANTI_ALIAS_FLAG)
                marker.color = Color.WHITE
                canvas.drawCircle(128f, 128f, 5f, marker)
                marker.color = Color.rgb(24, 72, 152)
                canvas.drawCircle(128f, 128f, 3f, marker)
                val date = Date.from(frame.generatedAt)
                RadarFrameUi(overlay, DateFormat.getTimeFormat(context).format(date))
            }
        } finally { mask.recycle() }
        WidgetRadarUi(background, frames, message, status.radarRefreshing)
    } catch (_: IllegalArgumentException) {
        WidgetRadarUi(message = context.getString(R.string.widget_radar_error), refreshing = status.radarRefreshing)
    }
}

private fun decode(image: RadarImage): Bitmap {
    val bytes = image.copyBytes()
    return requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
}
