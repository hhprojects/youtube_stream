package com.youtubestream.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    // ---- playlists ----

    @Insert
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("UPDATE playlists SET name = :name, dateModified = :modifiedAt WHERE id = :id")
    suspend fun rename(id: Long, name: String, modifiedAt: Long)

    @Query("UPDATE playlists SET coverArtUrl = :url, dateModified = :modifiedAt WHERE id = :id")
    suspend fun setCover(id: Long, url: String?, modifiedAt: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistRow(id: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :id")
    suspend fun deleteAllMembers(id: Long)

    /** Delete a playlist and its memberships atomically (single chokepoint cleanup). */
    @Transaction
    suspend fun deletePlaylist(id: Long) {
        deleteAllMembers(id)
        deletePlaylistRow(id)
    }

    /** A single playlist's metadata (name/cover) for the detail header. Null after it's deleted. */
    @Query("SELECT * FROM playlists WHERE id = :id")
    fun observePlaylist(id: Long): Flow<Playlist?>

    // ---- landing summaries (count + first-song art via JOIN so orphans are invisible) ----

    @Query(
        """
        SELECT p.id AS id, p.name AS name, p.coverArtUrl AS coverArtUrl,
               (SELECT COUNT(*) FROM playlist_songs ps
                  JOIN library_songs s ON s.id = ps.songId
                 WHERE ps.playlistId = p.id) AS songCount,
               (SELECT s.artworkUrl FROM playlist_songs ps
                  JOIN library_songs s ON s.id = ps.songId
                 WHERE ps.playlistId = p.id
                 ORDER BY ps.position ASC LIMIT 1) AS firstArtworkUrl
        FROM playlists p
        ORDER BY p.dateModified DESC
        """,
    )
    fun observeSummaries(): Flow<List<PlaylistSummary>>

    // ---- members ----

    @Query(
        """
        SELECT s.* FROM playlist_songs ps
          JOIN library_songs s ON s.id = ps.songId
         WHERE ps.playlistId = :playlistId
         ORDER BY ps.position ASC
        """,
    )
    fun observeSongs(playlistId: Long): Flow<List<LibrarySong>>

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun maxPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)   // re-adding a song already present is a no-op
    suspend fun insertMember(member: PlaylistSong)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun deleteMember(playlistId: Long, songId: String)

    @Query("UPDATE playlist_songs SET position = :position WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun setPosition(playlistId: Long, songId: String, position: Int)

    /** Rewrite positions for the whole playlist from an already-reordered id list (fed by PlaylistReorder). */
    @Transaction
    suspend fun setOrder(playlistId: Long, orderedSongIds: List<String>) {
        orderedSongIds.forEachIndexed { index, songId -> setPosition(playlistId, songId, index) }
    }
}
