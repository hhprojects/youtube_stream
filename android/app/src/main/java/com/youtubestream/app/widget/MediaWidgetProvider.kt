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
            val action = intent.action
            if (connection.state.value.isConnected) {
                // Warm: run now (main thread).
                connection.dispatchWidgetAction(action)
            } else {
                // Cold: the process just started and connect() is still async. Hold the receiver
                // alive with goAsync() and let the connection apply the command after it restores
                // the last queue, then finish().
                val pendingResult = goAsync()
                connection.runWhenReady(onApplied = { pendingResult.finish() }) {
                    dispatchWidgetAction(action)
                }
            }
            return
        }
        super.onReceive(context, intent)   // let AppWidgetProvider dispatch APPWIDGET_UPDATE → onUpdate
    }
}
