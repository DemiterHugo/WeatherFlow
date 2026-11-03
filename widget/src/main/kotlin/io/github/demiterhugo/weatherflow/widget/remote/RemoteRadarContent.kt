package io.github.demiterhugo.weatherflow.widget.remote

import android.annotation.SuppressLint
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteImage
import androidx.compose.remote.creation.compose.layout.RemoteStateLayout
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemoteTimeVariables
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import io.github.demiterhugo.weatherflow.widget.radar.WidgetRadarUi

/** alpha22's integer clock expression is restricted; capture and launcher tests cover its use. */
@SuppressLint("RestrictedApi")
@Composable
@RemoteComposable
internal fun RemoteRadarContent(radar: WidgetRadarUi, palette: RemoteWeatherPalette, description: String) {
    val background = radar.map
    if (background == null || radar.frames.isEmpty()) {
        RemoteText((radar.message ?: description).rs, color = palette.secondary.rc, fontSize = 12.rsp,
            fontWeight = FontWeight.Normal, maxLines = 3)
        return
    }
    val base = remember(background) { RemoteImageBitmap(background.asImageBitmap()) }
    val index = RemoteTimeVariables.secondOfHour % radar.frames.size
    RemoteStateLayout(index, *radar.frames.indices.toList().toIntArray(), modifier = RemoteModifier.fillMaxSize()) { frameIndex ->
        val frame = radar.frames[frameIndex]
        val overlay = remember(frame.overlay) { RemoteImageBitmap(frame.overlay.asImageBitmap()) }
        RemoteColumn(RemoteModifier.fillMaxSize()) {
            RemoteBox(RemoteModifier.weight(1f.rf).fillMaxWidth()) {
                RemoteImage(base, description.rs, RemoteModifier.fillMaxSize())
                RemoteImage(overlay, null, RemoteModifier.fillMaxSize())
            }
            RemoteText(frame.label.rs, color = palette.secondary.rc, fontSize = 12.rsp,
                fontWeight = FontWeight.Normal, maxLines = 1)
        }
    }
}
