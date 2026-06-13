package com.youtubestream.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * On-device lyrics cache — a DERIVED cache of the Pi's `.lyrics.json`, re-fillable from the Pi if wiped.
 * A row present = "checked": syncedLrc != null → synced; else plain != null → plain; else → a negative
 * (none found), re-checked after the TTL in LyricsRepository. Keyed by songId (= filename) and kept in its
 * OWN table so a LibrarySong REPLACE-upsert on re-download/import can't wipe it.
 */
@Entity(tableName = "lyrics")
data class Lyrics(
    @PrimaryKey val songId: String,
    val syncedLrc: String?,
    val plain: String?,
    val fetchedAt: Long,
)
