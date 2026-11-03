package io.github.demiterhugo.weatherflow.data.radar.remote

import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Spaces requests below the provider's published per-IP ceiling; other apps on the IP may also use it. */
internal class RadarRequestGate(
    private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000 },
) {
    private val mutex = Mutex()
    private var lastStarted: Long? = null
    private var blockedUntil: Long? = null

    suspend fun awaitTurn() = mutex.withLock {
        val now = nowMillis()
        blockedUntil?.let { if (it > now) throw IOException("Radar provider cooldown is still active") }
        lastStarted?.let { delay((it + 750 - now).coerceAtLeast(0)) }
        lastStarted = nowMillis()
    }

    suspend fun postponeFor(millis: Long) = mutex.withLock {
        val until = nowMillis() + millis.coerceIn(0, 86_400_000)
        blockedUntil = maxOf(blockedUntil ?: until, until)
    }
}
