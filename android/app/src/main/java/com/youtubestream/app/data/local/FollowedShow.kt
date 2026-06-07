package com.youtubestream.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A podcast show the user follows. PK = the show's ytmusicapi browseId. */
@Entity(tableName = "followed_shows")
data class FollowedShow(
    @PrimaryKey val showId: String,           // = browseId
    val title: String,
    val author: String? = null,
    val artworkUrl: String? = null,
    val dateFollowed: Long,
    // Newest episode videoId seen at last check (NOT a date — the date field is unreliable).
    // New episodes = those above this id in get_podcast's newest-first list. Advanced after notifying.
    val lastSeenEpisodeVideoId: String? = null,
)
