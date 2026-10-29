package io.github.demiterhugo.weatherflow.widget

import android.content.Context
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import io.github.demiterhugo.weatherflow.widget.glance.WeatherWidgetReceiver
import io.github.demiterhugo.weatherflow.widget.remote.RemoteWeatherWidgetReceiver
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
import org.robolectric.Shadows.shadowOf

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
        WeatherWidgetScheduler.stopIfUnused(context)
        val work = manager.getWorkInfosForUniqueWork(WeatherWidgetScheduler.PERIODIC_WORK).get(10, TimeUnit.SECONDS)

        assertEquals(WorkInfo.State.CANCELLED, work.single().state)
    }

    @Config(sdk = [37])
    @Test
    fun removingGlanceKeepsScheduleWhenRemoteStillExists() {
        bind(RemoteWeatherWidgetReceiver::class.java)
        WeatherWidgetScheduler.start(context)
        WeatherWidgetReceiver().onDisabled(context)
        assertEquals(WorkInfo.State.ENQUEUED, periodicState())
    }

    @Config(sdk = [37])
    @Test
    fun removingRemoteKeepsScheduleWhenGlanceStillExists() {
        bind(WeatherWidgetReceiver::class.java)
        WeatherWidgetScheduler.start(context)
        RemoteWeatherWidgetReceiver().onDisabled(context)
        assertEquals(WorkInfo.State.ENQUEUED, periodicState())
    }

    @Config(sdk = [37])
    @Test
    fun removingFinalRemoteWidgetCancelsSchedule() {
        WeatherWidgetScheduler.start(context)
        RemoteWeatherWidgetReceiver().onDisabled(context)
        assertEquals(WorkInfo.State.CANCELLED, periodicState())
    }

    private fun bind(type: Class<*>) {
        shadowOf(AppWidgetManager.getInstance(context)).addBoundWidget(
            1, AppWidgetProviderInfo().apply { provider = ComponentName(context, type) },
        )
    }

    private fun periodicState() = manager.getWorkInfosForUniqueWork(WeatherWidgetScheduler.PERIODIC_WORK)
        .get(10, TimeUnit.SECONDS).single().state
}
