package io.github.demiterhugo.weatherflow.widget.remote

import android.annotation.SuppressLint
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteImage
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteStateLayout
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemoteTimeVariables
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetWeatherText
import io.github.demiterhugo.weatherflow.widget.radar.WidgetRadarUi

/** alpha22's integer clock expression is restricted; capture and launcher tests cover its use. */
@SuppressLint("RestrictedApi")
@Composable
@RemoteComposable
internal fun RemoteRadarContent(
    radar: WidgetRadarUi,
    text: WidgetWeatherText,
    palette: RemoteWeatherPalette,
    labels: RemoteRadarLabels,
) {
    val mapSize = labels.mapSize
    val rowHeight = maxOf(mapSize, 116)
    val background = radar.map
    if (background == null || radar.frames.isEmpty()) {
        RadarFrameRow(text, palette, mapSize, rowHeight, timestamp = null) {
            RemoteText(
                (radar.message ?: labels.empty).rs,
                modifier = RemoteModifier.padding(8.rdp),
                color = palette.secondary.rc,
                fontSize = 11.rsp,
                fontWeight = FontWeight.Normal,
                maxLines = 4,
            )
        }
        return
    }

    val base = remember(background) { RemoteImageBitmap(background.asImageBitmap()) }
    val index = RemoteTimeVariables.secondOfHour % radar.frames.size
    RemoteStateLayout(
        index, *radar.frames.indices.toList().toIntArray(),
        modifier = RemoteModifier.fillMaxWidth().height(rowHeight.rdp),
    ) { frameIndex ->
        val frame = radar.frames[frameIndex]
        val overlay = remember(frame.overlay) { RemoteImageBitmap(frame.overlay.asImageBitmap()) }
        RadarFrameRow(text, palette, mapSize, rowHeight, frame.label) {
            RemoteImage(base, labels.description.rs, RemoteModifier.fillMaxSize())
            RemoteImage(overlay, null, RemoteModifier.fillMaxSize())
        }
    }
}

@Composable
@RemoteComposable
private fun RadarFrameRow(
    text: WidgetWeatherText,
    palette: RemoteWeatherPalette,
    mapSize: Int,
    rowHeight: Int,
    timestamp: String?,
    mapContent: @Composable @RemoteComposable () -> Unit,
) {
    RemoteRow(
        RemoteModifier.fillMaxWidth().height(rowHeight.rdp),
        verticalAlignment = RemoteAlignment.CenterVertically,
    ) {
        RemoteBox(
            RemoteModifier.width(mapSize.rdp).height(mapSize.rdp)
                .background(palette.mapBackground.rc),
            contentAlignment = RemoteAlignment.Center,
            content = mapContent,
        )
        RemoteColumn(RemoteModifier.weight(1f.rf).padding(start = 12.rdp)) {
            RemoteText(text.location.rs, color = palette.foreground.rc, fontSize = 15.rsp,
                fontWeight = FontWeight.Bold, maxLines = 1)
            text.temperature?.let {
                RemoteText(it.rs, color = palette.foreground.rc, fontSize = 25.rsp,
                    fontWeight = FontWeight.Normal, maxLines = 1)
            }
            text.condition?.let {
                RemoteText(it.rs, color = palette.secondary.rc, fontSize = 11.rsp,
                    fontWeight = FontWeight.Normal, maxLines = 1)
            }
            text.observedAt?.let {
                RemoteText(it.rs, color = palette.secondary.rc, fontSize = 10.rsp,
                    fontWeight = FontWeight.Normal, maxLines = 2)
            }
            timestamp?.let {
                RemoteText(it.rs, color = palette.secondary.rc, fontSize = 11.rsp,
                    fontWeight = FontWeight.Normal, maxLines = 2)
            }
        }
    }
}
