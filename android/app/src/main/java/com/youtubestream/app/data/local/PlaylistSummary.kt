package com.youtubestream.app.data.local

/** One row for the playlists-first landing. songCount + firstArtworkUrl are derived by the JOIN query. */
data class PlaylistSummary(
    val id: Long,
    val name: String,
    val coverArtUrl: String?,      // custom cover (may be null)
    val songCount: Int,            // counted via JOIN → orphaned memberships excluded
    val firstArtworkUrl: String?,  // first existing member's art → auto cover when coverArtUrl is null
)
