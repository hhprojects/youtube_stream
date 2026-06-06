package com.youtubestream.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.youtubestream.app.App

/**
 * Home-screen media widget. An AppWidgetProvider is a BroadcastReceiver that runs in the app's own
 * process on the main thread — so it can reach the app-scoped PlaybackConnection directly.
 */
class MediaWidgetProvider : AppWidgetProvider() {

    /** Placement / resize / reboot: push one full refresh (incl. artwork). */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        (context.applicationContext as App).container.widgetUpdater.refresh()
    }
}
