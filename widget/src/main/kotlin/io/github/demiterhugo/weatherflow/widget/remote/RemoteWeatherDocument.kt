package io.github.demiterhugo.weatherflow.widget.remote

import android.annotation.SuppressLint
import android.content.Context
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.compose.remote.creation.compose.action.PendingIntentAction
import androidx.compose.remote.creation.compose.capture.CapturedDocument
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.ui.graphics.Color
import io.github.demiterhugo.weatherflow.widget.R
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetWeatherText
import io.github.demiterhugo.weatherflow.widget.radar.WidgetRadarUi
import io.github.demiterhugo.weatherflow.widget.radar.WidgetRadarSize

/**
 * The widget profile and PendingIntent ID mapping are restricted APIs. Keep this integration
 * pinned to alpha22 and covered by document/action tests.
 */
internal object RemoteWeatherDocument {
    @RequiresApi(37)
    @SuppressLint("RestrictedApi")
    suspend fun capture(
        context: Context,
        text: WidgetWeatherText,
        actions: RemoteWeatherActions,
        widthDp: Int,
        heightDp: Int,
        radar: WidgetRadarUi? = null,
    ): CapturedDocument {
        val metrics = context.resources.displayMetrics
        val showRadar = radar != null && WidgetRadarSize.fits(widthDp, heightDp)
        val palette = if (showRadar) RemoteWeatherPalette(
            background = Color(0xFF10253B),
            foreground = Color(0xFFF3F8FF),
            secondary = Color(0xFFB5CBE1),
            error = Color(0xFFFFB4A9),
            action = Color(0xFF8DF2CE),
            mapBackground = Color(0xFF203C55),
        ) else RemoteWeatherPalette(
            Color(context.getColor(R.color.widget_background)),
            Color(context.getColor(R.color.widget_foreground)),
            Color(context.getColor(R.color.widget_secondary)),
            Color(context.getColor(R.color.widget_error)),
            Color(context.getColor(R.color.widget_action)),
            Color(context.getColor(R.color.widget_map_background)),
        )
        val document = captureSingleRemoteDocument(
            context = context,
            creationDisplayInfo = RemoteCreationDisplayInfo(
                width = (widthDp * metrics.density).toInt(),
                height = (heightDp * metrics.density).toInt(),
                densityDpi = metrics.densityDpi,
                fontScale = context.resources.configuration.fontScale,
            ),
            profile = widgetProfile(),
        ) {
            RemoteWeatherContent(
                text, actions, palette, expanded = widthDp >= 300 && heightDp >= 300,
                radar = radar.takeIf { showRadar },
                radarLabels = RemoteRadarLabels(
                    provider = context.getString(R.string.widget_remote_label),
                    mode = context.getString(R.string.widget_radar_animated),
                    description = context.getString(R.string.widget_radar_description),
                    empty = context.getString(R.string.widget_radar_empty),
                    play = context.getString(R.string.widget_radar_play),
                    rainCredit = context.getString(R.string.widget_radar_attribution),
                    mapCredit = context.getString(R.string.widget_map_attribution),
                    mapSize = WidgetRadarSize.remoteMapSize(widthDp, heightDp),
                ),
                radarResizeHint = context.getString(R.string.widget_radar_resize),
            )
        }
        require(document.bytes.size <= 512 * 1024) { "Remote widget document exceeds its transport budget" }
        return document
    }

    @SuppressLint("RestrictedApi")
    private fun widgetProfile(): Profile {
        val base = RcPlatformProfiles.WIDGETS_V7
        // alpha22's widget factory drops WriterEvents, breaking pendingIntentAction.
        // Preserve the platform profile and forward the callback to the Android writer.
        return Profile(base.apiLevel, base.operationsProfiles, base.platform) { display, profile, callback ->
            RemoteComposeWriterAndroid(display, null, profile, callback)
        }
    }

    @RequiresApi(37)
    @SuppressLint("RestrictedApi")
    fun toRemoteViews(document: CapturedDocument): RemoteViews {
        val instructions = RemoteViews.DrawInstructions.Builder(listOf(document.bytes)).build()
        return RemoteViews(instructions).apply {
            document.pendingIntents.forEach { index, pendingIntent ->
                setOnClickPendingIntent(PendingIntentAction.FIRST_ACTION_ID + index, pendingIntent)
            }
        }
    }
}
