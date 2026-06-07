package com.youtubestream.app.playback

import kotlinx.coroutines.flow.StateFlow

/**
 * The UI's contract with playback. [PlaybackConnection] implements it; ViewModels and composables
 * depend on this interface so they can be unit-tested on the JVM with a fake (the real connection
 * needs a live MediaController). Lifecycle (connect/release) stays off this interface on purpose.
 */
interface PlaybackController {
    val state: StateFlow<PlayerUiState>
    fun setQueueAndPlay(tracks: List<PlayableTrack>, startIndex: Int = 0, startPositionMs: Long = 0L)
    fun togglePlayPause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun toggleShuffle()
    fun cycleRepeat()

    /** Stop playback and clear the queue — the now-playing UI hides (currentMediaId → null). */
    fun stop()
}
