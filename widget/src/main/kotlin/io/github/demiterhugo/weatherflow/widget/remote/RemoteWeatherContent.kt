package io.github.demiterhugo.weatherflow.widget.remote

import androidx.compose.remote.creation.compose.action.pendingIntentAction
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetWeatherText
import io.github.demiterhugo.weatherflow.widget.radar.WidgetRadarUi

internal data class RemoteWeatherPalette(val background: Color, val foreground: Color, val secondary: Color, val error: Color)

@RemoteComposable
@Composable
internal fun RemoteWeatherContent(
    text: WidgetWeatherText,
    actions: RemoteWeatherActions,
    palette: RemoteWeatherPalette,
    expanded: Boolean,
    radar: WidgetRadarUi? = null,
    radarLabels: RemoteRadarLabels? = null,
    radarResizeHint: String? = null,
) {
    if (radar != null && radarLabels != null) {
        RadarRemoteWeatherContent(text, actions, palette, radar, radarLabels)
        return
    }
    val open = pendingIntentAction { actions.open }
    val refresh = pendingIntentAction { actions.refresh }
    val attribution = pendingIntentAction { actions.attribution }
    RemoteColumn(RemoteModifier.fillMaxSize().background(palette.background.rc).padding(16.rdp)) {
        RemoteColumn {
            RemoteText(
                text.location.rs, modifier = RemoteModifier.clickable(open),
                color = palette.foreground.rc, fontSize = 18.rsp, fontWeight = FontWeight.Bold, maxLines = 1,
            )
            text.temperature?.let {
                RemoteText(it.rs, color = palette.foreground.rc, fontSize = (if (expanded) 42 else 32).rsp,
                    fontWeight = FontWeight.Normal, maxLines = 1)
            }
            text.condition?.let { Label(it, palette.secondary) }
            if (expanded) {
                text.humidity?.let { Label(it, palette.secondary) }
                text.wind?.let { Label(it, palette.secondary) }
            }
        }
        RemoteBox(RemoteModifier.weight(1f.rf)) {}
        RemoteColumn {
            text.message?.let { Label(it, if (text.failed) palette.error else palette.secondary) }
            text.observedAt?.let { Label(it, palette.secondary) }
            RemoteRow(
                RemoteModifier.fillMaxWidth().height(48.rdp),
                horizontalArrangement = RemoteArrangement.SpaceBetween,
            ) {
                RemoteText(
                    text.refresh.rs,
                    modifier = RemoteModifier.clickable(refresh, enabled = !text.refreshing).padding(vertical = 12.rdp),
                    color = palette.foreground.rc, fontSize = 14.rsp, fontWeight = FontWeight.Bold,
                )
                RemoteText(
                    text.open.rs, modifier = RemoteModifier.clickable(open).padding(vertical = 12.rdp),
                    color = palette.foreground.rc, fontSize = 14.rsp, fontWeight = FontWeight.Normal,
                )
            }
            if (expanded) Label(text.modelEstimate, palette.secondary)
            if (expanded) radarResizeHint?.let { Label(it, palette.secondary) }
            RemoteText(
                text.attribution.rs, modifier = RemoteModifier.clickable(attribution),
                color = palette.secondary.rc, fontSize = 10.rsp, fontWeight = FontWeight.Normal, maxLines = 1,
            )
        }
    }
}

@RemoteComposable
@Composable
private fun Label(text: String, color: Color) {
    RemoteText(text.rs, color = color.rc, fontSize = 12.rsp, fontWeight = FontWeight.Normal, maxLines = 2)
}
