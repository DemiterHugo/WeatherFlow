package io.github.demiterhugo.weatherflow.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.demiterhugo.weatherflow.data.radar.AndroidRadarImageDecoder
import io.github.demiterhugo.weatherflow.data.radar.OfflineFirstRadarRepository
import io.github.demiterhugo.weatherflow.data.radar.local.RadarDiskCache
import io.github.demiterhugo.weatherflow.data.radar.local.RadarSnapshotCodec
import io.github.demiterhugo.weatherflow.data.radar.remote.RainViewerDataSource
import io.github.demiterhugo.weatherflow.domain.repository.RadarRepository
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RadarDataModule {
    @Provides
    @Singleton
    fun provideRadarRepository(@ApplicationContext context: Context): RadarRepository {
        val decoder = AndroidRadarImageDecoder()
        val cache = RadarDiskCache(File(context.cacheDir, "radar"), RadarSnapshotCodec(decoder))
        return OfflineFirstRadarRepository(RainViewerDataSource(), cache, decoder)
    }
}
