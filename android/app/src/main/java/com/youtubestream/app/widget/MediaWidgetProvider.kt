package com.youtubestream.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
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

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in WidgetIntents.COMMAND_ACTIONS) {
            val connection = (context.applicationContext as App).container.playbackConnection
            // Warm path: the controller is connected, so run the command now (main thread).
            if (connection.state.value.isConnected) {
                connection.dispatchWidgetAction(intent.action)
            }
            // Cold path (process was killed) is handled in Task 7.
            return
        }
        super.onReceive(context, intent)   // let AppWidgetProvider dispatch APPWIDGET_UPDATE → onUpdate
    }
}
