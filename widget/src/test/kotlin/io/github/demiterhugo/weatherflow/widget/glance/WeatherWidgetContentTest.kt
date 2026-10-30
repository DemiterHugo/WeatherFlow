package io.github.demiterhugo.weatherflow.widget.glance

import io.github.demiterhugo.weatherflow.widget.presentation.WidgetWeatherText

import android.content.Intent
import android.net.Uri
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.testing.unit.assertHasRunCallbackClickAction
import androidx.glance.appwidget.testing.unit.assertHasStartActivityClickAction
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.assertHasText
import androidx.glance.testing.unit.hasTestTag
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import io.github.demiterhugo.weatherflow.widget.radar.*
import android.graphics.Color
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetStatus
import java.time.Clock
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 37])
class WeatherWidgetContentTest {
    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    @Config(sdk = [37])
    @Test
    fun radarLayoutInflatesItsImageTimestampAndAllCreditsWithoutTruncatingChildren() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val radar = WidgetRadarSnapshot(WeatherLocation.Barcelona, radarTestSequence(), radarTestImage(Color.WHITE))
            .toUi(context, WidgetStatus(WeatherLocation.Barcelona), false, Clock.fixed(radarTestTime, ZoneOffset.UTC))
        val open = actionStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://open-meteo.com/")))
        val result = GlanceRemoteViews().compose(context, RADAR_SIZE) {
            WeatherWidgetContent(text, open, open, open, radar)
        }
        val root = result.remoteViews.apply(context, FrameLayout(context))
        val strings = mutableListOf<String>()
        val textViews = mutableMapOf<String, TextView>()
        var images = 0
        fun read(view: View) {
            if (view is TextView) {
                strings += view.text.toString()
                textViews[view.text.toString()] = view
            }
            if (view is android.widget.ImageView && view.drawable != null) images++
            if (view is ViewGroup) repeat(view.childCount) { read(view.getChildAt(it)) }
        }
        read(root)
        root.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, 600, 900)
        fun View.absoluteLeft(): Int = left + ((parent as? View)?.absoluteLeft() ?: 0)
        val location = checkNotNull(textViews[text.location])
        val temperature = checkNotNull(textViews[text.temperature])
        assertTrue(temperature.absoluteLeft() > location.absoluteLeft() + location.paint.measureText(text.location) + 8)
        assertTrue(images > 0)
        assertTrue(strings.contains(radar.frames.single().label))
        assertTrue(strings.any { it.contains("RainViewer") })
        assertTrue(strings.any { it.contains("OpenStreetMap") })
        assertTrue(strings.contains(text.attribution))
    }

    private val text = WidgetWeatherText(
        location = "Barcelona", temperature = "18.5 °C", condition = "Partly cloudy",
        observedAt = "Valid 29/10/2026 13:00", humidity = "Humidity 65%", wind = "Wind 11.5 km/h",
        message = null, failed = false, refreshing = false, refresh = "Refresh", open = "Open app",
        attribution = "Open-Meteo · CC BY 4.0", modelEstimate = "Weather-model estimates",
    )
    private val action = actionRunCallback<RefreshWeatherAction>()

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    @Test
    fun expandedUpdatingStateTranslatesToRealViewsWithoutDroppingAttribution() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val open = actionStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://open-meteo.com/")))
        val result = GlanceRemoteViews().compose(context, EXPANDED_SIZE) {
            WeatherWidgetContent(text.copy(message = "Updating…", refreshing = true), open, open, open)
        }
        val root = result.remoteViews.apply(context, FrameLayout(context))
        val visibleText = mutableListOf<String>()
        fun read(view: View) {
            if (view is TextView) visibleText += view.text.toString()
            if (view is ViewGroup) repeat(view.childCount) { read(view.getChildAt(it)) }
        }
        read(root)
        assertTrue(visibleText.containsAll(listOfNotNull(text.attribution, text.wind, text.humidity, "Updating…", text.open, text.refresh)))
    }

    @Test
    fun refreshOpenAndAttributionUseTheirIntendedActions() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        setAppWidgetSize(COMPACT_SIZE)
        val open = Intent(Intent.ACTION_MAIN).setClassName("io.github.demiterhugo.weatherflow", "MainActivity")
        val provider = Intent(Intent.ACTION_VIEW, Uri.parse("https://open-meteo.com/"))
        provideComposable {
            WeatherWidgetContent(text, actionStartActivity(open), action, actionStartActivity(provider))
        }
        onNode(hasTestTag("widget_refresh")).assertHasRunCallbackClickAction<RefreshWeatherAction>()
        onNode(hasTestTag("widget_open")).assertHasStartActivityClickAction(open)
        onNode(hasTestTag("widget_attribution")).assertHasStartActivityClickAction(provider)
    }

    @Test
    fun compactShowsTemperatureTimeAndAttribution() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        setAppWidgetSize(COMPACT_SIZE)
        provideComposable { WeatherWidgetContent(text, action, action, action) }

        onNode(hasTestTag("widget_location")).assertHasText("Barcelona")
        onNode(hasTestTag("widget_temperature")).assertHasText("18.5 °C")
        onNode(hasTestTag("widget_time")).assertHasText("Valid 29/10/2026 13:00")
        onNode(hasTestTag("widget_attribution")).assertHasText(text.attribution)
        onNode(hasTestTag("widget_humidity")).assertDoesNotExist()
        onNode(hasTestTag("widget_wind")).assertDoesNotExist()
    }

    @Test
    fun expandedAddsHumidityWindAndEstimateLabel() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        setAppWidgetSize(EXPANDED_SIZE)
        provideComposable { WeatherWidgetContent(text, action, action, action) }

        onNode(hasTestTag("widget_humidity")).assertHasText("Humidity 65%")
        onNode(hasTestTag("widget_wind")).assertHasText("Wind 11.5 km/h")
        onNode(hasTestTag("widget_model_estimate")).assertHasText(text.modelEstimate)
    }

    @Test
    fun emptyStateOffersRefreshAndOpenWithoutSampleReadings() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        setAppWidgetSize(COMPACT_SIZE)
        val empty = text.copy(temperature = null, condition = null, observedAt = null, message = "No saved weather")
        provideComposable { WeatherWidgetContent(empty, action, action, action) }

        onNode(hasTestTag("widget_temperature")).assertDoesNotExist()
        onNode(hasTestTag("widget_status")).assertHasText("No saved weather")
        onNode(hasTestTag("widget_refresh")).assertHasText("Refresh")
        onNode(hasTestTag("widget_open")).assertHasText("Open app")
    }

    @Test
    fun failureRetainsCachedReadingsAndShowsMessage() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        setAppWidgetSize(COMPACT_SIZE)
        provideComposable { WeatherWidgetContent(text.copy(message = "Update failed", failed = true), action, action, action) }

        onNode(hasTestTag("widget_temperature")).assertHasText("18.5 °C")
        onNode(hasTestTag("widget_status")).assertHasText("Update failed")
    }

    @Test
    fun updatingKeepsLastWeatherVisible() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        setAppWidgetSize(COMPACT_SIZE)
        provideComposable { WeatherWidgetContent(text.copy(message = "Updating…", refreshing = true), action, action, action) }

        onNode(hasTestTag("widget_temperature")).assertHasText("18.5 °C")
        onNode(hasTestTag("widget_status")).assertHasText("Updating…")
    }
}
