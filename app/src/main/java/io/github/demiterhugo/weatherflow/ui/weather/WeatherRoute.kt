package io.github.demiterhugo.weatherflow.ui.weather

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.demiterhugo.weatherflow.widget.WeatherWidgetScheduler
import io.github.demiterhugo.weatherflow.widget.glance.WeatherWidgetReceiver
import io.github.demiterhugo.weatherflow.widget.remote.RemoteWeatherWidgetReceiver

@Composable
fun WeatherRoute(viewModel: WeatherViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val widgetManager = AppWidgetManager.getInstance(context)
    LaunchedEffect(state.location) { WeatherWidgetScheduler.locationChanged(context) }
    LaunchedEffect(state.location, state.weather) {
        WeatherWidgetScheduler.render(context)
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.useDeviceLocation() else viewModel.locationPermissionDenied()
    }

    WeatherScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onAttributionClick = { uriHandler.openUri("https://open-meteo.com/") },
        onUseDeviceLocation = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                viewModel.useDeviceLocation()
            } else {
                locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        },
        onUseBarcelona = viewModel::useBarcelona,
        onAddWidget = if (widgetManager.isRequestPinAppWidgetSupported) {
            { widgetManager.requestPinAppWidget(ComponentName(context, WeatherWidgetReceiver::class.java), null, null) }
        } else null,
        onAddRemoteWidget = if (Build.VERSION.SDK_INT >= 37 && widgetManager.isRequestPinAppWidgetSupported) {
            { widgetManager.requestPinAppWidget(ComponentName(context, RemoteWeatherWidgetReceiver::class.java), null, null) }
        } else null,
    )
}
