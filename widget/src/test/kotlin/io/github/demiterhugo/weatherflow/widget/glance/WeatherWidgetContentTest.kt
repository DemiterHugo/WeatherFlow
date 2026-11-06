package io.github.demiterhugo.weatherflow.widget.glance

import io.github.demiterhugo.weatherflow.widget.R
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetWeatherText

import android.content.Intent
import android.net.Uri
import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.demiterhugo.weatherflow.domain.model.WeatherCondition
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
import org.robolectric.annotation.GraphicsMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 37])
class WeatherWidgetContentTest {
    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    @Config(sdk = [37])
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun squareWidgetFitsItsTemperatureConditionAndCredit() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val open = actionStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://open-meteo.com/")))
        val result = GlanceRemoteViews().compose(context, compactSize) {
            WeatherWidgetContent(text, open, open, open)
        }
        val root = result.remoteViews.apply(context, FrameLayout(context))
        val density = context.resources.displayMetrics.density
        val width = (compactSize.width.value * density).toInt()
        val height = (compactSize.height.value * density).toInt()
        root.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, width, height)
        fun texts(view: View): List<TextView> = when (view) {
            is TextView -> listOf(view)
            is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
            else -> emptyList()
        }
        fun View.absoluteTop(): Int = top + ((parent as? View)?.absoluteTop() ?: 0)
        val visible = texts(root).associateBy { it.text.toString() }
        for (label in listOf("Barcelona", "19°", "Partly cloudy", context.getString(R.string.widget_weather_source))) {
            val view = checkNotNull(visible[label])
            assertTrue(view.height > 0 && view.absoluteTop() + view.height <= height)
            assertTrue(view.layout.getEllipsisCount(0) == 0)
        }
    }

    private val compactSize = DpSize(132.dp, 144.dp)
    private val text = WidgetWeatherText(
        location = "Barcelona", temperature = "18.5 °C", condition = "Partly cloudy",
        observedAt = "Valid 29/10/2026 13:00", humidity = "Humidity 65%", wind = "Wind 11.5 km/h",
        message = null, failed = false, refreshing = false, refresh = "Refresh", open = "Open app",
        attribution = "Open-Meteo · CC BY 4.0", modelEstimate = "Weather-model estimates",
        compactTemperature = "19°", weatherCondition = WeatherCondition.PARTLY_CLOUDY,
    )
    private val action = actionRunCallback<RefreshWeatherAction>()

    @Test
    fun openAndAttributionUseTheirIntendedActions() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        setAppWidgetSize(compactSize)
        val open = Intent(Intent.ACTION_MAIN).setClassName("io.github.demiterhugo.weatherflow", "MainActivity")
        val provider = Intent(Intent.ACTION_VIEW, Uri.parse("https://open-meteo.com/"))
        provideComposable {
            WeatherWidgetContent(text, actionStartActivity(open), action, actionStartActivity(provider))
        }
        onNode(hasTestTag("widget_open")).assertHasStartActivityClickAction(open)
        onNode(hasTestTag("widget_attribution")).assertHasStartActivityClickAction(provider)
    }

    @Test
    fun emptyStateOffersRefreshAndOpenWithoutSampleReadings() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        setAppWidgetSize(compactSize)
        val empty = text.copy(temperature = null, compactTemperature = null, condition = null, observedAt = null, message = "No saved weather")
        provideComposable { WeatherWidgetContent(empty, action, action, action) }

        onNode(hasTestTag("widget_temperature")).assertDoesNotExist()
        onNode(hasTestTag("widget_status")).assertHasText("No saved weather")
        onNode(hasTestTag("widget_refresh")).assertHasText("Refresh")
        onNode(hasTestTag("widget_refresh")).assertHasRunCallbackClickAction<RefreshWeatherAction>()
    }

    @Test
    fun failureRetainsCachedReadingsAndShowsMessage() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        setAppWidgetSize(compactSize)
        provideComposable { WeatherWidgetContent(text.copy(message = "Update failed", failed = true), action, action, action) }

        onNode(hasTestTag("widget_temperature")).assertHasText("19°")
        onNode(hasTestTag("widget_status")).assertHasText("Update failed")
    }

    @Test
    fun updatingKeepsLastWeatherVisible() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        setAppWidgetSize(compactSize)
        provideComposable { WeatherWidgetContent(text.copy(message = "Updating…", refreshing = true), action, action, action) }

        onNode(hasTestTag("widget_temperature")).assertHasText("19°")
        onNode(hasTestTag("widget_status")).assertHasText("Updating…")
    }
}
