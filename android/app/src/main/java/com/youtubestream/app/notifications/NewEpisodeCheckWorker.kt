package com.youtubestream.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.youtubestream.app.App
import com.youtubestream.app.data.model.newEpisodeNotificationText

/** Periodic: check followed shows for new episodes and post one summarized notification. Notify-only. */
class NewEpisodeCheckWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = (applicationContext as App).container.podcastRepository
        return try {
            val newOnes = repo.checkForNewEpisodes()
            newEpisodeNotificationText(newOnes)?.let { PodcastNotifications.notifyNewEpisodes(applicationContext, it) }
            Result.success()
        } catch (e: Exception) {
            Result.retry()   // backend/network hiccup → WorkManager retries with backoff
        }
    }
}
