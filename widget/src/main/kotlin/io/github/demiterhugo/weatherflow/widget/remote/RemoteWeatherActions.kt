package io.github.demiterhugo.weatherflow.widget.remote

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

internal data class RemoteWeatherActions(
    val refresh: PendingIntent,
    val open: PendingIntent,
    val attribution: PendingIntent,
    val rainAttribution: PendingIntent = attribution,
    val mapAttribution: PendingIntent = attribution,
) {
    companion object {
        fun create(context: Context, widgetId: Int): RemoteWeatherActions {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val refreshIntent = Intent(context, RemoteWeatherWidgetReceiver::class.java)
                .setAction(RemoteWeatherWidgetReceiver.ACTION_REFRESH)
                .setData("weatherflow://widget/remote/$widgetId/refresh".toUri())
            val launchIntent = requireNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return RemoteWeatherActions(
                refresh = PendingIntent.getBroadcast(context, widgetId, refreshIntent, flags),
                open = PendingIntent.getActivity(context, widgetId, launchIntent, flags),
                attribution = PendingIntent.getActivity(
                    context, widgetId, Intent(Intent.ACTION_VIEW, "https://open-meteo.com/".toUri()), flags,
                ),
                rainAttribution = PendingIntent.getActivity(
                    context, widgetId, Intent(Intent.ACTION_VIEW, "https://www.rainviewer.com/".toUri()), flags,
                ),
                mapAttribution = PendingIntent.getActivity(
                    context, widgetId, Intent(Intent.ACTION_VIEW, "https://www.openstreetmap.org/copyright".toUri()), flags,
                ),
            )
        }
    }
}
