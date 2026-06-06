package com.youtubestream.app.widget

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import com.youtubestream.app.R
import com.youtubestream.app.playback.AppRepeatMode

/** The PendingIntents the widget's five buttons + body fire. Injected to keep the renderer decoupled. */
data class WidgetClickIntents(
    val toggle: PendingIntent,
    val previous: PendingIntent,
    val next: PendingIntent,
    val shuffle: PendingIntent,
    val repeat: PendingIntent,
    val body: PendingIntent,
)

private const val ALPHA_ACTIVE = 255
private const val ALPHA_INACTIVE = 110

/** Builds the widget's RemoteViews from a [WidgetModel]. The single source of widget rendering. */
object MediaWidgetRenderer {

    fun render(
        context: Context,
        model: WidgetModel,
        artwork: Bitmap?,
        clicks: WidgetClickIntents,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_media)

        // --- Text ---
        views.setTextViewText(R.id.widget_title, model.title.ifBlank { "Nothing playing" })
        views.setTextViewText(R.id.widget_artist, model.artist)
        if (model.upNextTitle != null) {
            views.setTextViewText(R.id.widget_up_next, "Up next: ${model.upNextTitle}")
            views.setViewVisibility(R.id.widget_up_next, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_up_next, View.GONE)
        }

        // --- Artwork (bitmap if loaded, else placeholder) ---
        if (artwork != null) {
            views.setImageViewBitmap(R.id.widget_artwork, artwork)
        } else {
            views.setImageViewResource(R.id.widget_artwork, R.drawable.ic_widget_music_note)
        }

        // --- Play / pause ---
        views.setImageViewResource(
            R.id.widget_play_pause,
            if (model.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
        )

        // --- Shuffle (alpha reflects on/off) ---
        views.setInt(R.id.widget_shuffle, "setImageAlpha", if (model.shuffleOn) ALPHA_ACTIVE else ALPHA_INACTIVE)

        // --- Repeat (icon + alpha reflect OFF / TRACK / QUEUE) ---
        when (model.repeatMode) {
            AppRepeatMode.OFF -> {
                views.setImageViewResource(R.id.widget_repeat, R.drawable.ic_widget_repeat)
                views.setInt(R.id.widget_repeat, "setImageAlpha", ALPHA_INACTIVE)
            }
            AppRepeatMode.TRACK -> {
                views.setImageViewResource(R.id.widget_repeat, R.drawable.ic_widget_repeat_one)
                views.setInt(R.id.widget_repeat, "setImageAlpha", ALPHA_ACTIVE)
            }
            AppRepeatMode.QUEUE -> {
                views.setImageViewResource(R.id.widget_repeat, R.drawable.ic_widget_repeat)
                views.setInt(R.id.widget_repeat, "setImageAlpha", ALPHA_ACTIVE)
            }
        }

        // --- Clicks ---
        views.setOnClickPendingIntent(R.id.widget_body, clicks.body)
        views.setOnClickPendingIntent(R.id.widget_previous, clicks.previous)
        views.setOnClickPendingIntent(R.id.widget_play_pause, clicks.toggle)
        views.setOnClickPendingIntent(R.id.widget_next, clicks.next)
        views.setOnClickPendingIntent(R.id.widget_shuffle, clicks.shuffle)
        views.setOnClickPendingIntent(R.id.widget_repeat, clicks.repeat)

        return views
    }
}
