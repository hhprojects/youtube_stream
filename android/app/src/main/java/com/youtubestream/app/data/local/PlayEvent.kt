package com.youtubestream.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One play event. Append-only log — the source of truth for "For You" shelves;
 * counts/recency are derived, never stored denormalized.
 * [songId] joins to [LibrarySong.id] (verified: the queue mapper sets mediaId = id).
 */
@Entity(
    tableName = "play_events",
    indices = [Index("songId"), Index("playedAt")],
)
data class PlayEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val playedAt: Long,   // epoch millis
)
