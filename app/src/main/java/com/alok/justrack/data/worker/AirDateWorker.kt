package com.alok.justrack.data.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alok.justrack.data.model.MediaType
import com.alok.justrack.data.repository.MediaRepository
import com.alok.justrack.util.DateUtils
import com.alok.justrack.util.NotificationReceiver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.ZoneId

@HiltWorker
class AirDateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: MediaRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val watchlist = repository.getWatchlist()
        val tvShows = watchlist.filter { it.mediaType == MediaType.TV }

        for (show in tvShows) {
            val futureEpisodes = repository.getFutureEpisodes(show.id)
            for (episode in futureEpisodes) {
                scheduleNotification(show.title, episode, show.id)
            }
        }

        return Result.success()
    }

    private fun scheduleNotification(showName: String, episode: com.alok.justrack.data.model.Episode, showId: String) {
        val airDateStr = episode.airDate ?: return
        val airDate = DateUtils.parseDate(airDateStr) ?: return
        
        // Use 8 PM IST for notifications on the air date
        val notificationTime = airDate.atTime(20, 0)
        val epochMillis = notificationTime.atZone(ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli()

        if (epochMillis < System.currentTimeMillis()) return

        val intent = Intent(applicationContext, NotificationReceiver::class.java).apply {
            action = "com.alok.justrack.SHOW_NOTIFICATION"
            putExtra("NOTIFICATION_ID", episode.id.hashCode())
            putExtra("TITLE", "New Episode: $showName")
            putExtra("MESSAGE", "S${episode.seasonNumber} E${episode.episodeNumber}: ${episode.name} is airing today!")
            putExtra("MEDIA_ID", showId.toIntOrNull() ?: -1)
            putExtra("MEDIA_TYPE", "tv")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            episode.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, epochMillis, pendingIntent)
            }
        } catch (e: Exception) {
            // Fallback for security exceptions or other issues
            alarmManager.set(AlarmManager.RTC_WAKEUP, epochMillis, pendingIntent)
        }
    }
}
