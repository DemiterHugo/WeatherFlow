package io.github.demiterhugo.weatherflow.data.radar

import io.github.demiterhugo.weatherflow.data.radar.local.RadarCache
import io.github.demiterhugo.weatherflow.data.radar.remote.MAX_MERCATOR_LATITUDE
import io.github.demiterhugo.weatherflow.data.radar.remote.RADAR_IMAGE_SIZE
import io.github.demiterhugo.weatherflow.data.radar.remote.RADAR_ZOOM
import io.github.demiterhugo.weatherflow.data.radar.remote.RadarRemoteDataSource
import io.github.demiterhugo.weatherflow.data.radar.remote.toDownloadPlan
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.RadarFrame
import io.github.demiterhugo.weatherflow.domain.model.RadarSequence
import io.github.demiterhugo.weatherflow.domain.repository.RadarRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

internal class OfflineFirstRadarRepository(
    private val remote: RadarRemoteDataSource,
    private val cache: RadarCache,
    private val decoder: RadarImageDecoder,
    private val clock: Clock = Clock.systemUTC(),
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : RadarRepository {
    private val refreshMutex = Mutex()

    override fun observeRadar(coordinates: Coordinates): Flow<RadarSequence?> = cache.observe(coordinates)

    override suspend fun refreshRadar(coordinates: Coordinates) {
        require(coordinates.latitude in -MAX_MERCATOR_LATITUDE..MAX_MERCATOR_LATITUDE) {
            "Radar latitude is outside Web Mercator coverage."
        }
        withContext(io) {
            refreshMutex.withLock {
                withTimeout(60_000) {
                    val previous = cache.read(coordinates)
                    val plan = remote.manifest().toDownloadPlan(coordinates)
                    if (previous != null && previous.manifestGeneratedAt > plan.generatedAt) return@withTimeout
                    val reusable = previous?.frames.orEmpty().associateBy { it.sourceId }
                    val frames = plan.frames.map { frame ->
                        val generatedAt = Instant.ofEpochSecond(frame.time)
                        reusable[frame.path]?.takeIf { it.generatedAt == generatedAt }
                            ?: RadarFrame(frame.path, generatedAt, decoder.image(remote.image(plan.imageUrl(frame))))
                    }
                    val now = clock.instant()
                    val coverage = previous?.coverage?.takeIf {
                        val age = Duration.between(it.checkedAt, now)
                        !age.isNegative && age < Duration.ofHours(24)
                    } ?: decoder.coverage(remote.image(plan.coverageUrl()), clock.instant())
                    currentCoroutineContext().ensureActive()
                    cache.save(
                        RadarSequence(
                            coordinates = coordinates,
                            zoom = RADAR_ZOOM,
                            imageSize = RADAR_IMAGE_SIZE,
                            manifestGeneratedAt = plan.generatedAt,
                            fetchedAt = clock.instant(),
                            frames = frames,
                            coverage = coverage,
                        ),
                    )
                }
            }
        }
    }
}
