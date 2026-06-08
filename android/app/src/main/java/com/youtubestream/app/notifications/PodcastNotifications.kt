package com.youtubestream.app.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.youtubestream.app.MainActivity
import com.youtubestream.app.R

object PodcastNotifications {
    private const val CHANNEL_ID = "new_episodes"
    private const val NOTIFICATION_ID = 4001

    /** Create the "New episodes" channel. Safe to call repeatedly (createNotificationChannel is idempotent). */
    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "New episodes",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Alerts when shows you follow publish new episodes" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Post the summary. On Android 13+ without POST_NOTIFICATIONS this is a silent no-op (no crash). */
    // POST_NOTIFICATIONS is declared in the manifest + requested at MainActivity startup; notify() just drops
    // the post (no SecurityException) if the user declined, so suppressing the lint guard is safe here.
    @SuppressLint("MissingPermission")
    fun notifyNewEpisodes(context: Context, text: String) {
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_podcast)
            .setContentTitle("Podcasts")
            .setContentText(text)
            .setContentIntent(MainActivity.openPodcastPendingIntent(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n)
    }
}
