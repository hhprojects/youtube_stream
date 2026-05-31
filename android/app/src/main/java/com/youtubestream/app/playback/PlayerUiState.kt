package com.youtubestream.app.playback

/** Everything the UI needs to render the player, derived from the Media3 controller. */
data class PlayerUiState(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val currentMediaId: String? = null,
    val title: String = "",
    val artist: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: AppRepeatMode = AppRepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
)
