package com.youtubestream.app.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.youtubestream.app.MainActivity
import com.youtubestream.app.playback.PlaybackController

/** The widget→app intent contract: button actions, PendingIntent builders, and the open-Player extra. */
object WidgetIntents {
    const val ACTION_TOGGLE = "com.youtubestream.app.widget.TOGGLE"
    const val ACTION_PREV = "com.youtubestream.app.widget.PREV"
    const val ACTION_NEXT = "com.youtubestream.app.widget.NEXT"
    const val ACTION_SHUFFLE = "com.youtubestream.app.widget.SHUFFLE"
    const val ACTION_REPEAT = "com.youtubestream.app.widget.REPEAT"
    const val EXTRA_OPEN_PLAYER = "com.youtubestream.app.widget.OPEN_PLAYER"

    val COMMAND_ACTIONS = setOf(ACTION_TOGGLE, ACTION_PREV, ACTION_NEXT, ACTION_SHUFFLE, ACTION_REPEAT)

    // Distinct request codes so the system does not collapse the PendingIntents into one.
    private const val RC_TOGGLE = 100
    private const val RC_PREV = 101
    private const val RC_NEXT = 102
    private const val RC_SHUFFLE = 103
    private const val RC_REPEAT = 104

    fun clickIntents(context: Context) = WidgetClickIntents(
        toggle = command(context, ACTION_TOGGLE, RC_TOGGLE),
        previous = command(context, ACTION_PREV, RC_PREV),
        next = command(context, ACTION_NEXT, RC_NEXT),
        shuffle = command(context, ACTION_SHUFFLE, RC_SHUFFLE),
        repeat = command(context, ACTION_REPEAT, RC_REPEAT),
        body = MainActivity.openPlayerPendingIntent(context),
    )

    private fun command(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MediaWidgetProvider::class.java).setAction(action)
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE)
    }
}

/** Maps a widget button action to the matching playback control. */
internal fun PlaybackController.dispatchWidgetAction(action: String?) {
    when (action) {
        WidgetIntents.ACTION_TOGGLE -> togglePlayPause()
        WidgetIntents.ACTION_PREV -> previous()
        WidgetIntents.ACTION_NEXT -> next()
        WidgetIntents.ACTION_SHUFFLE -> toggleShuffle()
        WidgetIntents.ACTION_REPEAT -> cycleRepeat()
    }
}
