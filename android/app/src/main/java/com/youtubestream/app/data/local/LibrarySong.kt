package com.youtubestream.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A downloaded song on this device. Doubles as the domain model for the library. */
@Entity(
    tableName = "library_songs",
    // filename is the file's stable cross-path identity: a Search→Download row keys on id=videoId
    // while the same file imported from the Pi keys on id=filename. A unique index makes REPLACE
    // (INSERT OR REPLACE) collapse those two on the filename collision → one row per file, not two.
    indices = [Index(value = ["filename"], unique = true)],
)
data class LibrarySong(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val filename: String,
    val localPath: String,
    val size: Long,
    val dateAdded: Long,
    val artworkUrl: String? = null,   // remote YouTube thumbnail; null for imported songs
)
