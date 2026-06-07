package com.youtubestream.app.playback

/** One row in the player's up-next list. */
data class QueueItem(
    val mediaId: String,
    val title: String,
    val artist: String,
)

/** Everything the UI needs to render the player, derived from the Media3 controller. */
data class PlayerUiState(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val currentMediaId: String? = null,
    val title: String = "",
    val artist: String = "",
    val artworkUri: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: AppRepeatMode = AppRepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val queue: List<QueueItem> = emptyList(),
    val currentIndex: Int = 0,
    val playbackSpeed: Float = 1f,
    val isPodcast: Boolean = false,
)
