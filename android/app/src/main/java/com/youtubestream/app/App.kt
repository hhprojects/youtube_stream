package com.youtubestream.app

import android.app.Application
import com.youtubestream.app.di.AppContainer
import com.youtubestream.app.notifications.PodcastNotifications
import com.youtubestream.app.notifications.scheduleNewEpisodeCheck

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        PodcastNotifications.ensureChannel(this)
        scheduleNewEpisodeCheck(this)
    }
}
