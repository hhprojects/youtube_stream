package com.youtubestream.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A downloaded podcast episode — the offline store, parallel to [LibrarySong] but with
 * podcast-specific fields. PK = filename (the same canonical-PK convention as songs).
 * No foreign key to followed_shows: showId is a logical link (unfollowing must not delete downloads).
 */
@Entity(tableName = "podcast_episodes")
data class PodcastEpisode(
    @PrimaryKey val id: String,            // = filename
    val videoId: String,                   // YouTube id — download + dedupe key
    val title: String,
    val showName: String,                  // the show/author (podcast's "artist")
    val showId: String?,                   // the show's browseId — logical link, no FK
    val durationSeconds: Int,              // parsed from ytmusicapi "1 hr 49 min" at insert time
    val filename: String,
    val localPath: String,
    val size: Long,
    val dateAdded: Long,
    val artworkUrl: String? = null,        // episode thumbnail (i.ytimg.com/vi/<videoId>/...)
    val publishedDate: String? = null,     // raw ytmusicapi `date` — DISPLAY-ONLY, unreliable; never used for logic
    val description: String? = null,       // show notes
    val resumePositionMs: Long = 0,        // where you left off — the key podcast field
    val isFinished: Boolean = false,       // reached ~95% → drops out of "Continue listening"
    val lastPlayedAt: Long? = null,        // recency ordering for "Continue listening"
)
