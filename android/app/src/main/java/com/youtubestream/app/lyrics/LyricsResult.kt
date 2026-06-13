package com.youtubestream.app.lyrics

/** What the repository resolves for a song. Loading is a UI-only state (null), not modelled here. */
sealed interface LyricsResult {
    data class Synced(val lines: List<LyricLine>) : LyricsResult
    data class Plain(val text: String) : LyricsResult

    /** lrclib was asked and has nothing. */
    data object None : LyricsResult

    /** The Pi/lrclib couldn't be reached — distinct from None so we don't cache it as a miss. */
    data object Unavailable : LyricsResult
}

/** The minimal song identity the lyrics lookup needs — built from PlayerUiState (no Media3, no Room). */
data class SongRef(
    val id: String,        // == LibrarySong.id == the Pi filename == PlayerUiState.currentMediaId
    val title: String,
    val artist: String,
    val durationMs: Long,
)
