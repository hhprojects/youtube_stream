package com.youtubestream.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A downloaded song on this device. Doubles as the domain model for the library. */
@Entity(tableName = "library_songs")
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
