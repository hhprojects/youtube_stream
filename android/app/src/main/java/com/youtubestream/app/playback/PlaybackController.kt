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
    fun setSpeed(speed: Float)
    /** Seek relative to the current position, clamped to [0, duration]. Used by podcast skip ±. */
    fun seekBy(deltaMs: Long)

    // --- Queue editing (absolute timeline indices) ---
    /** Jump playback to the queue item at [index] and play it. */
    fun playQueueItem(index: Int)
    /** Reorder: move the queue item at [from] to [to]. */
    fun moveQueueItem(from: Int, to: Int)
    /** Remove the queue item at [index]. */
    fun removeQueueItem(index: Int)
    /** Clear the up-next list (everything after the current item); the current track keeps playing. */
    fun clearUpNext()

    // --- Sleep timer ---
    /** Pause playback after [durationMs] from now. Replaces any existing timer. */
    fun setSleepTimer(durationMs: Long)
    /** Pause playback when the current track finishes. Replaces any existing timer. */
    fun setSleepTimerEndOfTrack()
    /** Cancel any pending sleep timer. */
    fun cancelSleepTimer()

    /** Stop playback and clear the queue — the now-playing UI hides (currentMediaId → null). */
    fun stop()
}
