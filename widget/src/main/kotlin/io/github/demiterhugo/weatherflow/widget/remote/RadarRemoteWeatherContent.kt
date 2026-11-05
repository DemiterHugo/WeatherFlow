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
import androidx.compose.ui.text.font.FontWeight
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetWeatherText
import io.github.demiterhugo.weatherflow.widget.radar.WidgetRadarUi

internal data class RemoteRadarLabels(
    val provider: String,
    val mode: String,
    val description: String,
    val empty: String,
    val rainCredit: String,
    val mapCredit: String,
    val mapSize: Int,
)

@RemoteComposable
@Composable
internal fun RadarRemoteWeatherContent(
    text: WidgetWeatherText, actions: RemoteWeatherActions, palette: RemoteWeatherPalette,
    radar: WidgetRadarUi, labels: RemoteRadarLabels,
) {
    val open = pendingIntentAction { actions.open }
    val refresh = pendingIntentAction { actions.refresh }
    val attribution = pendingIntentAction { actions.attribution }
    val rainCredit = pendingIntentAction { actions.rainAttribution }
    val mapCredit = pendingIntentAction { actions.mapAttribution }
    RemoteColumn(RemoteModifier.fillMaxSize().background(palette.background.rc).padding(12.rdp)) {
        RemoteRow(RemoteModifier.fillMaxWidth()) {
            RemoteText(labels.provider.rs, RemoteModifier.weight(1f.rf).clickable(open),
                color = palette.foreground.rc, fontSize = 15.rsp, fontWeight = FontWeight.Bold, maxLines = 1)
            RemoteText(labels.mode.rs, color = palette.secondary.rc, fontSize = 11.rsp,
                fontWeight = FontWeight.Normal, maxLines = 1)
        }
        RemoteBox(RemoteModifier.height(8.rdp)) {}
        RemoteRadarContent(radar, text, palette, labels)
        if (radar.frames.isNotEmpty()) radar.message?.let {
            RemoteText(it.rs, color = palette.secondary.rc, fontSize = 10.rsp, fontWeight = FontWeight.Normal, maxLines = 1)
        }
        text.message?.let {
            RemoteText(it.rs, color = palette.error.rc, fontSize = 10.rsp, fontWeight = FontWeight.Normal, maxLines = 1)
        }
        RemoteBox(RemoteModifier.weight(1f.rf)) {}
        RemoteRow(RemoteModifier.fillMaxWidth().height(44.rdp), horizontalArrangement = RemoteArrangement.SpaceBetween) {
            RemoteText(text.refresh.rs,
                RemoteModifier.clickable(refresh, enabled = !text.refreshing && !radar.refreshing).padding(vertical = 10.rdp),
                color = palette.action.rc, fontSize = 13.rsp, fontWeight = FontWeight.Bold, maxLines = 1)
            RemoteText(text.open.rs, RemoteModifier.clickable(open).padding(vertical = 10.rdp),
                color = palette.action.rc, fontSize = 13.rsp, fontWeight = FontWeight.Normal, maxLines = 1)
        }
        RemoteColumn {
            RemoteRow(RemoteModifier.fillMaxWidth(), horizontalArrangement = RemoteArrangement.SpaceBetween) {
                RemoteText(text.attribution.rs, RemoteModifier.clickable(attribution),
                    color = palette.secondary.rc, fontSize = 10.rsp, fontWeight = FontWeight.Normal, maxLines = 1)
                RemoteText(labels.rainCredit.rs, RemoteModifier.clickable(rainCredit),
                    color = palette.secondary.rc, fontSize = 10.rsp, fontWeight = FontWeight.Normal, maxLines = 1)
            }
            RemoteText(labels.mapCredit.rs, RemoteModifier.clickable(mapCredit),
                color = palette.secondary.rc, fontSize = 10.rsp, fontWeight = FontWeight.Normal, maxLines = 1)
        }
    }
}
