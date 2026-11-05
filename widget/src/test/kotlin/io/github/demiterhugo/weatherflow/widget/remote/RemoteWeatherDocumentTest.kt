package io.github.demiterhugo.weatherflow.widget.remote

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.compose.remote.creation.compose.capture.CapturedDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.operations.TextData
import androidx.compose.remote.core.operations.BitmapData
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.layout.managers.StateLayout
import androidx.compose.remote.core.operations.layout.modifiers.ValueFloatExpressionChangeActionOperation
import androidx.compose.remote.core.RemoteComposeBuffer
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetWeatherText
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import io.github.demiterhugo.weatherflow.widget.radar.*
import android.graphics.Color
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetStatus
import java.time.Clock
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class RemoteWeatherDocumentTest {
    @Test
    fun radarDocumentDrawsSixFramesWithoutSwappingLayoutsAndFitsTransportBudget() = runBlocking {
        val radar = WidgetRadarSnapshot(WeatherLocation.Barcelona, radarTestSequence(), radarTestImage(Color.WHITE))
            .toUi(context, WidgetStatus(WeatherLocation.Barcelona), true, Clock.fixed(radarTestTime, ZoneOffset.UTC))
        val radarActions = actions.copy(rainAttribution = pending("rain"), mapAttribution = pending("map"))
        val document = RemoteWeatherDocument.capture(context, text, radarActions, 260, 270, radar)
        val content = document.decodedContent()
        for (frame in radar.frames) assertTrue(content.contains(frame.label))
        assertTrue(content.contains("RainViewer"))
        assertTrue(content.contains("OpenStreetMap"))
        assertEquals(5, document.actions().size)
        assertTrue(document.bytes.size < 512 * 1024)
        val operations = document.decodedOperations()
        assertEquals(7, operations.filterIsInstance<BitmapData>().size)
        assertEquals(6, operations.filterIsInstance<ConditionalOperations>().size)
        assertTrue(operations.none { it is StateLayout })
        assertEquals(1, operations.filterIsInstance<ValueFloatExpressionChangeActionOperation>().size)
        assertNotNull(RemoteWeatherDocument.toRemoteViews(document))
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val text = WidgetWeatherText(
        "Barcelona", "19 °C", "Cloudy", "Valid 29/10/26 17:30", "Humidity 74%", "Wind 10 km/h",
        null, false, false, "Refresh", "Open app", "Open-Meteo · CC BY 4.0", "Weather-model estimates",
    )
    private fun CapturedDocument.actions() = buildSet {
        pendingIntents.forEach { _, action -> add(action) }
    }

    private fun CapturedDocument.decodedOperations(): List<Operation> {
        val buffer = bytes.inputStream().use { RemoteComposeBuffer.fromInputStream(it) }
        val operations = arrayListOf<Operation>()
        buffer.inflateFromBuffer(operations)
        return operations
    }

    private fun CapturedDocument.decodedContent(): String = decodedOperations()
        .filterIsInstance<TextData>().joinToString("\n") { it.mText }

    private fun pending(name: String) = PendingIntent.getActivity(
        context, 0, Intent(Intent.ACTION_VIEW).setData(android.net.Uri.parse("weatherflow://test/$name")),
        PendingIntent.FLAG_IMMUTABLE,
    )
    private val actions = RemoteWeatherActions(pending("refresh"), pending("open"), pending("attribution"))

    @Test
    fun compactDocumentContainsRealWeatherAndAllThreeActions() = runBlocking {
        val document = RemoteWeatherDocument.capture(context, text, actions, 180, 220)
        assertTrue(document.bytes.isNotEmpty())
        val content = document.decodedContent()
        assertTrue(content.contains("Barcelona"))
        assertTrue(content.contains("19 °C"))
        assertTrue(content.contains(text.attribution))
        assertFalse(content.contains("Humidity 74%"))
        assertEquals(setOf(actions.refresh, actions.open, actions.attribution), document.actions())
        assertNotNull(RemoteWeatherDocument.toRemoteViews(document))
    }

    @Test
    fun expandedUpdatingDocumentPreservesAttributionAndNavigation() = runBlocking {
        val document = RemoteWeatherDocument.capture(
            context, text.copy(message = "Updating…", refreshing = true), actions, 300, 300,
        )
        assertTrue(document.bytes.isNotEmpty())
        val content = document.decodedContent()
        assertTrue(content.contains("Humidity 74%"))
        assertTrue(content.contains("Wind 10 km/h"))
        assertTrue(content.contains("Updating…"))
        assertTrue(content.contains(text.attribution))
        assertTrue(document.actions().contains(actions.attribution))
        assertTrue(document.actions().contains(actions.open))
        assertFalse(document.actions().contains(actions.refresh))
    }

    @Test
    fun emptyAndErrorDocumentsCanStillRefreshAndOpenApp() = runBlocking {
        val empty = text.copy(temperature = null, condition = null, humidity = null, wind = null, observedAt = null,
            message = "Weather unavailable", failed = true)
        val document = RemoteWeatherDocument.capture(context, empty, actions, 180, 220)
        assertTrue(document.decodedContent().contains("Weather unavailable"))
        assertFalse(document.decodedContent().contains("19 °C"))
        assertEquals(setOf(actions.refresh, actions.open, actions.attribution), document.actions())
    }

    @Test
    fun refreshPendingIntentsAreExplicitImmutableAndIsolatedPerWidget() {
        // Register a launch activity for this library-test package.
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        val info = android.content.pm.ResolveInfo().apply {
            activityInfo = android.content.pm.ActivityInfo().apply {
                packageName = context.packageName
                name = "TestActivity"
                applicationInfo = context.applicationInfo
                enabled = true
                exported = true
            }
        }
        shadowOf(context.packageManager).addResolveInfoForIntent(intent, info)
        val first = RemoteWeatherActions.create(context, 1)
        val second = RemoteWeatherActions.create(context, 2)
        val refresh = shadowOf(first.refresh)

        assertTrue(refresh.isBroadcast)
        assertTrue(refresh.isImmutable)
        assertEquals(RemoteWeatherWidgetReceiver.ACTION_REFRESH, refresh.savedIntent.action)
        assertEquals(RemoteWeatherWidgetReceiver::class.java.name, refresh.savedIntent.component?.className)
        assertNotEquals(first.refresh, second.refresh)
        assertEquals("https://open-meteo.com/", shadowOf(first.attribution).savedIntent.dataString)
    }
}
