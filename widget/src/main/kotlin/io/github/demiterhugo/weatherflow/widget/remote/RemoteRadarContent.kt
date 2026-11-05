package io.github.demiterhugo.weatherflow.widget.remote

import android.annotation.SuppressLint
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemoteTimeVariables
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetWeatherText
import io.github.demiterhugo.weatherflow.widget.radar.WidgetRadarUi

/** The alpha22 integer expressions and conditional drawing are covered by the widget profile test. */
@SuppressLint("RestrictedApi")
@Composable
@RemoteComposable
internal fun RemoteRadarContent(
    radar: WidgetRadarUi,
    text: WidgetWeatherText,
    palette: RemoteWeatherPalette,
    labels: RemoteRadarLabels,
) {
    val map = radar.map
    if (map == null || radar.frames.isEmpty()) {
        RemoteColumn(RemoteModifier.fillMaxWidth().height(labels.mapSize.rdp)) {
            RemoteText(text.location.rs, color = palette.foreground.rc, fontSize = 17.rsp,
                fontWeight = FontWeight.Bold, maxLines = 1)
            RemoteText((radar.message ?: labels.empty).rs, color = palette.secondary.rc,
                fontSize = 13.rsp, fontWeight = FontWeight.Normal, maxLines = 3)
        }
        return
    }

    val base = remember(map) { RemoteImageBitmap(map.asImageBitmap()) }
    val overlays = remember(radar.frames) {
        radar.frames.map { RemoteImageBitmap(it.overlay.asImageBitmap()) }
    }
    // The continuous clock requests repaints in One UI. Include the date and hour
    // so an old tap cannot trigger a replay when the clock rolls into the next hour.
    val now = RemoteTimeVariables.continuousSeconds +
        RemoteTimeVariables.hourOfDay.toRemoteFloat() * 3600f.rf +
        RemoteTimeVariables.dayOfMonth.toRemoteFloat() * 86400f.rf
    val frameSeconds = 0.85f
    val frameDuration = frameSeconds.rf
    val last = radar.frames.lastIndex
    // One UI evaluates the tap's standalone clock expression and stores its result.
    val replayStart = rememberMutableRemoteFloat(-2f)
    val elapsed = now - replayStart - frameDuration
    val playing = replayStart.isGreaterThanOrEqualTo(0f.rf) and
        elapsed.isGreaterThanOrEqualTo(0f.rf) and elapsed.isLessThan(frameDuration * last.toFloat().rf)
    val index = playing.select((elapsed / frameDuration).toRemoteInt(), last.ri)
    val timestamp = radar.frames.dropLast(1).foldIndexed(radar.frames.last().label.rs) { frameIndex, label, frame ->
        index.isEqualTo(frameIndex.ri).select(frame.label.rs, label)
    }

    RemoteRow(RemoteModifier.fillMaxWidth().height(labels.mapSize.rdp)) {
        // Keep the map and layout in place. Only the precipitation layer changes.
        RemoteCanvas(
            RemoteModifier.width(labels.mapSize.rdp).height(labels.mapSize.rdp)
                .background(palette.mapBackground.rc)
                .semantics { contentDescription = labels.description.rs },
        ) {
            drawScaledBitmap(base)
            overlays.forEachIndexed { frameIndex, overlay ->
                drawConditionally(index.isEqualTo(frameIndex.ri)) {
                    drawScaledBitmap(overlay)
                }
            }
        }
        RemoteColumn(RemoteModifier.weight(1f.rf).padding(start = 12.rdp)) {
            RemoteText(text.location.rs, color = palette.foreground.rc, fontSize = 17.rsp,
                fontWeight = FontWeight.Bold, maxLines = 1)
            text.temperature?.let {
                RemoteText(it.rs, color = palette.secondary.rc, fontSize = 13.rsp,
                    fontWeight = FontWeight.Normal, maxLines = 1)
            }
            RemoteBox(RemoteModifier.weight(1f.rf)) {}
            RemoteBox(RemoteModifier.fillMaxWidth().height(28.rdp)) {
                RemoteText(timestamp, color = palette.secondary.rc, fontSize = 11.rsp,
                    fontWeight = FontWeight.Normal, maxLines = 2)
            }
            RemoteRow(RemoteModifier.fillMaxWidth().height(4.rdp)) {
                radar.frames.indices.forEach { frameIndex ->
                    val color = index.isEqualTo(frameIndex.ri).select(palette.action.rc, palette.secondary.rc)
                    RemoteBox(RemoteModifier.weight(1f.rf).padding(end = 3.rdp).height(3.rdp).background(color)) {}
                }
            }
            if (last > 0) {
                RemoteBox(
                    RemoteModifier.fillMaxWidth().height(48.rdp)
                        .clickable(valueChange(replayStart, now - frameDuration)),
                    contentAlignment = RemoteAlignment.CenterStart,
                ) {
                    RemoteText(labels.play.rs, color = palette.action.rc, fontSize = 14.rsp,
                        fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}
