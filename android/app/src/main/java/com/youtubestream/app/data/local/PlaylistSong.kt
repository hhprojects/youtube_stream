package com.youtubestream.app.data.local

import androidx.room.Entity
import androidx.room.Index

/**
 * Many-to-many join: which songs are in which playlist, and in what order.
 *
 * NO @ForeignKey — [songId] is a *logical* join to LibrarySong.id, exactly like PlayEvent.
 * library_songs upserts via INSERT OR REPLACE (filename-dedup), which is delete-then-insert;
 * an enforced ON DELETE CASCADE child would be wiped on every artwork edit / re-import, and
 * can't coexist with the two-rows-into-one filename collapse at all. Correctness comes from the
 * JOIN to library_songs (orphans produce no row → invisible). See the spec for the full rationale.
 *
 * Composite PK (playlistId, songId) → a song appears at most once per playlist.
 */
@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    indices = [Index("songId"), Index(value = ["playlistId", "position"])],
)
data class PlaylistSong(
    val playlistId: Long,
    val songId: String,   // → LibrarySong.id (logical join)
    val position: Int,    // 0-based; drives manual order
    val dateAdded: Long,
)
