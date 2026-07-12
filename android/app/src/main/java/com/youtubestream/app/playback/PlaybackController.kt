package com.youtubestream.app.playback

import kotlinx.coroutines.flow.StateFlow

/**
 * The UI's contract with playback. [PlaybackConnection] implements it; ViewModels and composables
 * depend on this interface so they can be unit-tested on the JVM with a fake (the real connection
 * needs a live MediaController). Lifecycle (connect/release) stays off this interface on purpose.
 */
interface PlaybackController {
    val state: StateFlow<PlayerUiState>
    /**
     * Replace the playing context. A still-pending manual queue survives — it is re-inserted right
     * after the new start item (Spotify semantics). [shuffled] = explicit shuffle-play: random start
     * track, the rest permuted, the toggle lit. Shuffle is otherwise sticky for music: a lit toggle
     * carries over to the new queue (the [startIndex] track plays first, the rest permuted). Podcast
     * queues always build in natural order and turn the toggle off.
     */
    fun setQueueAndPlay(
        tracks: List<PlayableTrack>,
        startIndex: Int = 0,
        startPositionMs: Long = 0L,
        shuffled: Boolean = false,
    )
    fun togglePlayPause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun toggleShuffle()
    fun cycleRepeat()
    fun setSpeed(speed: Float)
    /** Seek relative to the current position, clamped to [0, duration]. Used by podcast skip ±. */
    fun seekBy(deltaMs: Long)

    // --- Queue insertion ---
    /** Append [tracks] to the end of the queue (starts a new queue + plays if nothing is queued). */
    fun addToQueue(tracks: List<PlayableTrack>)
    /** Insert [tracks] right after the current item (starts a new queue + plays if nothing is queued). */
    fun playNext(tracks: List<PlayableTrack>)

    // --- Queue editing (absolute timeline indices) ---
    /** Jump playback to the queue item at [index] and play it. */
    fun playQueueItem(index: Int)
    /** Reorder: move the queue item at [from] to [to]. */
    fun moveQueueItem(from: Int, to: Int)
    /** Remove the queue item at [index]. */
    fun removeQueueItem(index: Int)
    /** Clear the upcoming context ("Next up", after the manual block); current track + queue stay. */
    fun clearUpNext()
    /** Clear the manual "Next in queue" block; the current track and the upcoming context stay. */
    fun clearManualQueue()

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
