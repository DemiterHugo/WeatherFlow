package io.github.demiterhugo.weatherflow.widget.remote

import android.annotation.SuppressLint
import androidx.compose.remote.creation.compose.action.pendingIntentAction
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.verticalGradient
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemotePaint
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetWeatherText
import io.github.demiterhugo.weatherflow.widget.radar.WidgetRadarUi

internal data class RemoteRadarLabels(
    val title: String,
    val description: String,
    val empty: String,
    val play: String,
    val rainCredit: String,
    val mapCredit: String,
)

/** The widget profile test covers the alpha22 drawing and clock expressions. */
@SuppressLint("RestrictedApi")
@Composable
@RemoteComposable
internal fun RemoteRadarContent(
    radar: WidgetRadarUi,
    text: WidgetWeatherText,
    actions: RemoteWeatherActions,
    labels: RemoteRadarLabels,
) {
    val open = pendingIntentAction { actions.open }
    val refresh = pendingIntentAction { actions.refresh }
    val foreground = Color(0xFFF5FAFF)
    val panel = Color(0xFF10263B)
    val accent = Color(0xFFBDEADD)
    val map = radar.map
    if (map == null || radar.frames.isEmpty()) {
        RemoteColumn(RemoteModifier.fillMaxSize().background(panel.rc).padding(14.rdp)) {
            RemoteText(labels.title.rs, RemoteModifier.clickable(open), color = foreground.rc,
                fontSize = 17.rsp, fontWeight = FontWeight.Bold, maxLines = 1)
            RemoteBox(RemoteModifier.weight(1f.rf)) {}
            RemoteText((radar.message ?: labels.empty).rs, color = foreground.rc,
                fontSize = 14.rsp, fontWeight = FontWeight.Normal, maxLines = 3)
            RemoteText(text.refresh.rs,
                RemoteModifier.clickable(refresh, enabled = !radar.refreshing).padding(vertical = 8.rdp),
                color = accent.rc, fontSize = 15.rsp, fontWeight = FontWeight.Bold, maxLines = 1)
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

    val rainCredit = pendingIntentAction { actions.rainAttribution }
    val mapCredit = pendingIntentAction { actions.mapAttribution }
    val scrim = RemoteBrush.verticalGradient(
        listOf(Color(0xEE10263B).rc, Color(0x1610263B).rc, Color(0xEE10263B).rc),
    )
    RemoteBox(RemoteModifier.fillMaxSize().background(panel.rc)) {
        // Draw both layers with the same crop. The clock changes the overlay only.
        RemoteCanvas(RemoteModifier.fillMaxSize().semantics { contentDescription = labels.description.rs }) {
            drawScaledBitmap(base, scaleType = ContentScale.Crop)
            overlays.forEachIndexed { frameIndex, overlay ->
                drawConditionally(index.isEqualTo(frameIndex.ri)) {
                    drawScaledBitmap(overlay, scaleType = ContentScale.Crop)
                }
            }
            drawRect(paint = RemotePaint { with(scrim) { applyTo(this@RemotePaint, size) } })
        }

        RemoteColumn(RemoteModifier.fillMaxSize().padding(12.rdp)) {
            RemoteRow(RemoteModifier.fillMaxWidth(), verticalAlignment = RemoteAlignment.CenterVertically) {
                RemoteText(labels.title.rs, RemoteModifier.weight(1f.rf).clickable(open),
                    color = foreground.rc, fontSize = 17.rsp, fontWeight = FontWeight.Bold, maxLines = 1)
                RemoteBox(
                    RemoteModifier.clip(RemoteRoundedCornerShape(12.rdp)).background(panel.rc)
                        .padding(horizontal = 9.rdp, vertical = 4.rdp),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    RemoteText(timestamp, color = foreground.rc, fontSize = 14.rsp,
                        fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            RemoteBox(RemoteModifier.weight(1f.rf)) {}
            radar.message?.let {
                RemoteText(it.rs,
                    RemoteModifier.clickable(refresh, enabled = !radar.refreshing).background(panel.rc)
                        .padding(horizontal = 6.rdp, vertical = 2.rdp),
                    color = foreground.rc, fontSize = 12.rsp, fontWeight = FontWeight.Normal, maxLines = 2)
            }
            RemoteRow(RemoteModifier.fillMaxWidth(), verticalAlignment = RemoteAlignment.CenterVertically) {
                if (last > 0) {
                    RemoteBox(
                        RemoteModifier.width(48.rdp).height(48.rdp)
                            .clip(RemoteRoundedCornerShape(16.rdp)).background(accent.rc)
                            .clickable(valueChange(replayStart, now - frameDuration))
                            .semantics { contentDescription = labels.play.rs },
                        contentAlignment = RemoteAlignment.Center,
                    ) {
                        RemoteText("▶".rs, color = panel.rc, fontSize = 19.rsp,
                            fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
                RemoteColumn(RemoteModifier.weight(1f.rf).padding(start = 8.rdp)) {
                    RemoteRow(RemoteModifier.fillMaxWidth().height(4.rdp)) {
                        radar.frames.indices.forEach { frameIndex ->
                            val color = index.isEqualTo(frameIndex.ri).select(accent.rc, Color(0x80CAE2F3).rc)
                            RemoteBox(RemoteModifier.weight(1f.rf).padding(end = 3.rdp).height(4.rdp).background(color)) {}
                        }
                    }
                    RemoteRow(
                        RemoteModifier.fillMaxWidth().padding(top = 5.rdp),
                        horizontalArrangement = RemoteArrangement.SpaceBetween,
                    ) {
                        RemoteText(labels.mapCredit.rs, RemoteModifier.clickable(mapCredit),
                            color = foreground.rc, fontSize = 12.rsp, fontWeight = FontWeight.Normal, maxLines = 1)
                        RemoteText(labels.rainCredit.rs, RemoteModifier.clickable(rainCredit),
                            color = foreground.rc, fontSize = 12.rsp, fontWeight = FontWeight.Normal, maxLines = 1)
                    }
                }
            }
        }
    }
}
