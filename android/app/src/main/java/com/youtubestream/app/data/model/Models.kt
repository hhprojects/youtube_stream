package com.youtubestream.app.data.model

import com.youtubestream.app.data.local.LibrarySong

/** Clean domain model the UI consumes — backend DTO quirks are mapped away in the repository. */
data class SearchResult(
    val id: String,
    val title: String,
    val channel: String,
    val durationSeconds: Int?,
    val thumbnailUrl: String?,
)

/** A song on the Pi (for the Import screen). No real duration — the Pi reports the string 'Unknown'. */
data class PiSong(
    val id: String,
    val title: String,
    val artist: String,
    val filename: String,
    val downloadUrl: String,
    val size: Long,
    val thumbnailUrl: String? = null,   // from the Pi sidecar; null when unknown
    val videoId: String? = null,        // from the Pi sidecar; null for Pi-only files
)

/** Progress of a two-step download (metadata POST, then file stream). */
sealed interface DownloadState {
    data class InProgress(val fraction: Float) : DownloadState
    data class Completed(val song: LibrarySong) : DownloadState
    data class Failed(val error: Throwable) : DownloadState
}
