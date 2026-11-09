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

    @Test fun playbackUsesSixLocalizedTimesInChronologicalOrder() {
        val frames = snapshot().toUi(context, status, clock).frames
        assertEquals(6, frames.size)
        assertEquals(6, frames.map { it.label }.toSet().size)
        assertEquals(android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date.from(radarTestTime)),
            frames.last().label)
        assertNotEquals(frames.first().overlay.getPixel(20, 20), frames.last().overlay.getPixel(20, 20))
    }

    @Test fun stalenessUsesNewestFrameAndNotTheHistoricalFrameBeingPlayed() {
        assertNull(snapshot().toUi(context, status, clock).message)
        val oldClock = Clock.fixed(radarTestTime.plusSeconds(1801), ZoneOffset.UTC)
        assertTrue(snapshot().toUi(context, status, oldClock).message!!.contains("30"))
    }

    @Test fun missingMapDoesNotDisplayUnlocatedPrecipitationAndFailureKeepsCachedFrames() {
        assertTrue(snapshot().copy(map = null).toUi(context, status, clock).frames.isEmpty())
        val cached = snapshot().toUi(context, status.copy(radarFailed = true), clock)
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
        val ui = original.copy(sequence = sequence).toUi(context, status, clock)
        assertTrue(ui.message!!.contains("coverage"))
        assertEquals(6, ui.frames.size)
    }
}
