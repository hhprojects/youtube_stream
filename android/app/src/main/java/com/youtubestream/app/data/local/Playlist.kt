package com.youtubestream.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A user-created playlist. Membership lives in [PlaylistSong]; smart playlists are NOT stored here. */
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val coverArtUrl: String? = null,   // custom cover; null → auto (first song's art) → placeholder
    val dateCreated: Long,
    val dateModified: Long,            // bumped on rename / membership change → landing orders by this
)
