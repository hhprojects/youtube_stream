package com.youtubestream.app.widget

import com.youtubestream.app.playback.AppRepeatMode
import com.youtubestream.app.playback.PlayerUiState

/**
 * The widget's minimal, immutable view of playback — the RemoteViews equivalent of [PlayerUiState].
 * Deliberately omits position/duration so [equals] is stable across the 500 ms position ticks; that
 * is what lets the updater's distinctUntilChanged skip needless RemoteViews rebuilds.
 */
data class WidgetModel(
    val isConnected: Boolean = false,
    val hasTrack: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val artworkUri: String? = null,
    val isPlaying: Boolean = false,
    val shuffleOn: Boolean = false,
    val repeatMode: AppRepeatMode = AppRepeatMode.OFF,
    val upNextTitle: String? = null,
) {
    companion object {
        fun from(state: PlayerUiState): WidgetModel = WidgetModel(
            isConnected = state.isConnected,
            hasTrack = state.currentMediaId != null,
            title = state.title,
            artist = state.artist,
            artworkUri = state.artworkUri,
            isPlaying = state.isPlaying,
            shuffleOn = state.shuffleEnabled,
            repeatMode = state.repeatMode,
            // Timeline-order next track. Note: with shuffle on this is not the true play-order next
            // (the shuffle order is not exposed in PlayerUiState) — accepted for v1 per the spec.
            upNextTitle = state.queue.getOrNull(state.currentIndex + 1)?.title?.takeIf { it.isNotBlank() },
        )
    }
}
