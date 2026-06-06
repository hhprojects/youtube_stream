package com.youtubestream.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.youtubestream.app.playback.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** Artwork is downsampled to this square before crossing IPC to the launcher (Binder ~1 MB limit). */
private const val ARTWORK_PX = 256

/**
 * Pushes RemoteViews to the placed media widgets whenever playback state changes. Owned by
 * AppContainer (app-scoped). Reads the single source of truth (PlaybackController.state) — no
 * duplicate player listener.
 */
class WidgetUpdater(
    private val context: Context,
    private val controller: PlaybackController,
    private val scope: CoroutineScope,
) {
    /** Begin mirroring state → widget. Call once at app start. */
    fun start() {
        controller.state
            .map { WidgetModel.from(it) }
            .distinctUntilChanged()   // ignore position ticks: WidgetModel omits position/duration
            .onEach { push(it) }
            .launchIn(scope)
    }

    /** One-shot push (incl. artwork) for placement/resize, where distinctUntilChanged won't fire. */
    fun refresh() {
        scope.launch { push(WidgetModel.from(controller.state.value)) }
    }

    private suspend fun push(model: WidgetModel) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, MediaWidgetProvider::class.java))
        if (ids.isEmpty()) return   // no widget placed — skip the bitmap decode entirely
        val artwork = model.artworkUri?.let { loadArtwork(it) }
        val views = MediaWidgetRenderer.render(context, model, artwork, WidgetIntents.clickIntents(context))
        manager.updateAppWidget(ids, views)
    }

    private suspend fun loadArtwork(url: String): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(ARTWORK_PX, ARTWORK_PX)   // downsample
            .allowHardware(false)           // RemoteViews bitmaps cross IPC; hardware bitmaps can't be parceled
            .build()
        val result = context.imageLoader.execute(request)
        return (result as? SuccessResult)?.image?.toBitmap()
    }
}
