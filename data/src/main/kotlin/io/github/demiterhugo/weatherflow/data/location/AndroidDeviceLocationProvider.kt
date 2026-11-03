package io.github.demiterhugo.weatherflow.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.repository.DeviceLocationProvider
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class AndroidDeviceLocationProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DeviceLocationProvider {
    // Permission is checked by the UI. Revocation and disabled providers propagate to its error state.
    @SuppressLint("MissingPermission")
    override suspend fun currentCoordinates(): Coordinates = withTimeoutOrNull(30_000) {
        val manager = context.getSystemService(LocationManager::class.java)
        val providers = buildList {
            if (Build.VERSION.SDK_INT >= 31) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }
        val provider = providers.firstOrNull { manager.isProviderEnabled(it) }
            ?: throw IOException("No approximate location provider available")

        suspendCancellableCoroutine { continuation ->
            val cancellation = CancellationSignal()
            continuation.invokeOnCancellation { cancellation.cancel() }
            LocationManagerCompat.getCurrentLocation(
                manager, provider, cancellation, ContextCompat.getMainExecutor(context),
            ) { location ->
                if (continuation.isActive) {
                    if (location == null) {
                        continuation.resumeWithException(IOException("Location unavailable"))
                    } else {
                        continuation.resume(Coordinates(location.latitude, location.longitude))
                    }
                }
            }
        }
    } ?: throw IOException("Location request timed out")
}
