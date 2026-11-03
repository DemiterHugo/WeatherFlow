package io.github.demiterhugo.weatherflow.data.radar.remote

import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RadarRequestGateTest {
    @Test
    fun concurrentRequestsAreSpacedBelowPublishedLimit() = runTest {
        val gate = RadarRequestGate { testScheduler.currentTime }
        val starts = (1..8).map { async { gate.awaitTurn(); testScheduler.currentTime } }.awaitAll()
        assertEquals((0..7).map { it * 750L }, starts)
    }

    @Test
    fun cooldownFailsImmediatelyAndAllowsCallsAfterRetryWindow() = runTest {
        val gate = RadarRequestGate { testScheduler.currentTime }
        gate.postponeFor(60_000)
        assertTrue(runCatching { gate.awaitTurn() }.exceptionOrNull() is IOException)
        assertEquals(0L, testScheduler.currentTime)
        testScheduler.advanceTimeBy(60_000)
        gate.awaitTurn()
    }

    @Test
    fun shorterCooldownCannotOverrideLongerRetryAfter() = runTest {
        val gate = RadarRequestGate { testScheduler.currentTime }
        gate.postponeFor(120_000)
        gate.postponeFor(10_000)
        testScheduler.advanceTimeBy(60_000)
        assertTrue(runCatching { gate.awaitTurn() }.exceptionOrNull() is IOException)
        testScheduler.advanceTimeBy(60_000)
        gate.awaitTurn()
    }
}
