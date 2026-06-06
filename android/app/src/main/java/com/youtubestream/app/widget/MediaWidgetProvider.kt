package com.youtubestream.app.widget

import android.appwidget.AppWidgetProvider

/**
 * Home-screen media widget. An AppWidgetProvider is a BroadcastReceiver that runs in the app's own
 * process on the main thread — so it can reach the app-scoped PlaybackConnection directly.
 * Display (onUpdate) is added in Task 5; command handling (onReceive) in Tasks 6–7.
 */
class MediaWidgetProvider : AppWidgetProvider()
