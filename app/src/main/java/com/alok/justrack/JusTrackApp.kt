package com.alok.justrack

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.alok.justrack.data.worker.AirDateWorker
import com.alok.justrack.util.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class JusTrackApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Notification Channels
        NotificationHelper.createNotificationChannel(this)

        // Schedule AirDateWorker
        scheduleAirDateWorker()
    }

    private fun scheduleAirDateWorker() {
        val workRequest = PeriodicWorkRequestBuilder<AirDateWorker>(
            1, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "AirDateWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

