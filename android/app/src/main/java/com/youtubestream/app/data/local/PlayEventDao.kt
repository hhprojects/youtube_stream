package com.youtubestream.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayEventDao {
    @Insert
    suspend fun insert(event: PlayEvent)

    /** Reactive: re-emits whenever a play is recorded. Newest first. */
    @Query("SELECT * FROM play_events ORDER BY playedAt DESC")
    fun observeAll(): Flow<List<PlayEvent>>

    /**
     * "Recently played" smart playlist: distinct in-library songs, most-recent play first.
     * The JOIN to library_songs makes plays of songs no longer in the library invisible (the same
     * orphan-safety the playlist queries use), so a deleted/re-imported song just drops out.
     */
    @Query(
        """
        SELECT s.* FROM library_songs s
          JOIN play_events p ON p.songId = s.id
         GROUP BY s.id
         ORDER BY MAX(p.playedAt) DESC
         LIMIT :limit
        """,
    )
    fun observeRecentlyPlayed(limit: Int): Flow<List<LibrarySong>>

    /** "Most played" smart playlist: in-library songs by all-time play count desc, tie-break most-recent. */
    @Query(
        """
        SELECT s.* FROM library_songs s
          JOIN play_events p ON p.songId = s.id
         GROUP BY s.id
         ORDER BY COUNT(p.songId) DESC, MAX(p.playedAt) DESC
         LIMIT :limit
        """,
    )
    fun observeMostPlayed(limit: Int): Flow<List<LibrarySong>>
}
