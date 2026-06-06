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
}
