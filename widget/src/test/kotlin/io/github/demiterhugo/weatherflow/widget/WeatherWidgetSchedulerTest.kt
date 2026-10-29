package io.github.demiterhugo.weatherflow.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 37])
class WeatherWidgetSchedulerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var manager: WorkManager

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context, Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        manager = WorkManager.getInstance(context)
    }

    @After
    fun tearDown() {
        manager.cancelAllWork().result.get(10, TimeUnit.SECONDS)
        WorkManagerTestInitHelper.closeWorkDatabase()
    }

    @Test
    fun repeatedInstallationKeepsOneHourlyTaskWithNetworkConstraint() {
        repeat(3) { WeatherWidgetScheduler.start(context) }
        val work = manager.getWorkInfosForUniqueWork(WeatherWidgetScheduler.PERIODIC_WORK).get(10, TimeUnit.SECONDS)

        assertEquals(1, work.size)
        assertEquals(NetworkType.CONNECTED, work.single().constraints.requiredNetworkType)
        assertEquals(TimeUnit.HOURS.toMillis(1), work.single().periodicityInfo?.repeatIntervalMillis)
    }

    @Test
    fun removingLastWidgetCancelsPeriodicWork() {
        WeatherWidgetScheduler.start(context)
        WeatherWidgetScheduler.stop(context)
        val work = manager.getWorkInfosForUniqueWork(WeatherWidgetScheduler.PERIODIC_WORK).get(10, TimeUnit.SECONDS)

        assertEquals(WorkInfo.State.CANCELLED, work.single().state)
    }
}
