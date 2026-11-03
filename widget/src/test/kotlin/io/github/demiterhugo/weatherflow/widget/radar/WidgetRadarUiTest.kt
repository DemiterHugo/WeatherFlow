package io.github.demiterhugo.weatherflow.widget.radar

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import io.github.demiterhugo.weatherflow.domain.model.WeatherLocation
import io.github.demiterhugo.weatherflow.domain.model.RadarCoverageAvailability
import io.github.demiterhugo.weatherflow.widget.presentation.WidgetStatus
import java.time.Clock
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetRadarUiTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val status = WidgetStatus(WeatherLocation.Barcelona)
    private val clock = Clock.fixed(radarTestTime, ZoneOffset.UTC)
    private fun snapshot() = WidgetRadarSnapshot(WeatherLocation.Barcelona, radarTestSequence(), radarTestImage(Color.WHITE))

    @Test fun staticAndAnimatedViewsUseSameLastFrameAndMatchingTimestamp() {
        val snapshot = snapshot()
        val static = snapshot.toUi(context, status, false, clock)
        val animated = snapshot.toUi(context, status, true, clock)
        assertEquals(1, static.frames.size)
        assertEquals(6, animated.frames.size)
        assertEquals(animated.frames.last().label, static.frames.single().label)
        assertTrue(static.frames.single().label.endsWith("6/6"))
        assertEquals(animated.latestImage()!!.getPixel(20, 20), static.latestImage()!!.getPixel(20, 20))
        assertNotEquals(Color.WHITE, static.latestImage()!!.getPixel(20, 20))
    }

    @Test fun stalenessUsesNewestFrameAndNotTheHistoricalFrameBeingPlayed() {
        assertNull(snapshot().toUi(context, status, true, clock).message)
        val oldClock = Clock.fixed(radarTestTime.plusSeconds(1801), ZoneOffset.UTC)
        assertTrue(snapshot().toUi(context, status, true, oldClock).message!!.contains("30"))
    }

    @Test fun missingMapDoesNotDisplayUnlocatedPrecipitationAndFailureKeepsCachedFrames() {
        assertTrue(snapshot().copy(map = null).toUi(context, status, false, clock).frames.isEmpty())
        val cached = snapshot().toUi(context, status.copy(radarFailed = true), true, clock)
        assertEquals(6, cached.frames.size)
        assertNotNull(cached.message)
    }

    @Test fun noCoverageIsExplicitAndNoRainClaimIsInvented() {
        val original = snapshot()
        val source = original.sequence!!
        val coverage = source.coverage.copy(mask = radarTestImage(Color.BLACK), availability = RadarCoverageAvailability.NONE, centerCovered = false)
        val sequence = io.github.demiterhugo.weatherflow.domain.model.RadarSequence(
            source.coordinates, source.zoom, source.imageSize, source.manifestGeneratedAt, source.fetchedAt, source.frames, coverage,
        )
        val ui = original.copy(sequence = sequence).toUi(context, status, false, clock)
        assertTrue(ui.message!!.contains("coverage"))
        assertEquals(1, ui.frames.size)
    }
}
