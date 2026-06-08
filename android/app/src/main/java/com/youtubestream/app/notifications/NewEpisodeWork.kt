package com.youtubestream.app.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val WORK_NAME = "new-episode-check"

/** Schedule the ~6-hourly, network-constrained new-episode check. KEEP = schedule once; don't reset on relaunch. */
fun scheduleNewEpisodeCheck(context: Context) {
    val request = PeriodicWorkRequestBuilder<NewEpisodeCheckWorker>(6, TimeUnit.HOURS)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    WorkManager.getInstance(context)
        .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
}
