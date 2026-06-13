package com.youtubestream.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LyricsDao {
    @Query("SELECT * FROM lyrics WHERE songId = :songId")
    suspend fun get(songId: String): Lyrics?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(lyrics: Lyrics)
}
