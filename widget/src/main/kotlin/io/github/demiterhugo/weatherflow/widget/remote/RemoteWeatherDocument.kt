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
    ): CapturedDocument {
        val metrics = context.resources.displayMetrics
        val palette = RemoteWeatherPalette(
            Color(context.getColor(R.color.widget_background)),
            Color(context.getColor(R.color.widget_foreground)),
            Color(context.getColor(R.color.widget_secondary)),
            Color(context.getColor(R.color.widget_error)),
        )
        return captureSingleRemoteDocument(
            context = context,
            creationDisplayInfo = RemoteCreationDisplayInfo(
                width = (widthDp * metrics.density).toInt(),
                height = (heightDp * metrics.density).toInt(),
                densityDpi = metrics.densityDpi,
                fontScale = context.resources.configuration.fontScale,
            ),
            profile = widgetProfile(),
        ) {
            RemoteWeatherContent(text, actions, palette, expanded = widthDp >= 300 && heightDp >= 300)
        }
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
