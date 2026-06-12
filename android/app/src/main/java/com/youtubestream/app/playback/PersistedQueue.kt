package com.youtubestream.app.playback

import kotlinx.serialization.Serializable

/**
 * A snapshot of the play queue, persisted so it survives the process being killed: the ordered
 * tracks, which one is current, how far into it we were, and the player's shuffle/repeat modes
 * (a fresh ExoPlayer defaults both to off — without these the user's toggles vanish on restore).
 * Every field has a default so an older or partial payload still decodes (forward/backward
 * compatible).
 */
@Serializable
data class PersistedQueue(
    val tracks: List<PlayableTrack> = emptyList(),
    val currentIndex: Int = 0,
    val positionMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: AppRepeatMode = AppRepeatMode.OFF,
)
