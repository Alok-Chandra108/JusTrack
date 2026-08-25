package com.alok.justrack.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.alok.justrack.data.worker.AirDateWorker

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // Reschedule all alarms on boot
                val workRequest = OneTimeWorkRequestBuilder<AirDateWorker>().build()
                WorkManager.getInstance(context).enqueue(workRequest)
            }
            "com.alok.justrack.SHOW_NOTIFICATION" -> {
                val id = intent.getIntExtra("NOTIFICATION_ID", 0)
                val title = intent.getStringExtra("TITLE") ?: "Show Reminder"
                val message = intent.getStringExtra("MESSAGE") ?: "An episode is airing now!"
                val mediaId = intent.getIntExtra("MEDIA_ID", -1)
                val mediaType = intent.getStringExtra("MEDIA_TYPE") ?: "tv"

                NotificationHelper.showNotification(
                    context, id, title, message, mediaId, mediaType
                )
            }
        }
    }
}
