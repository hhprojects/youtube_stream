package com.youtubestream.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    /** Reactive: re-emits whenever the table changes (fixes the RN refresh race). */
    @Query("SELECT * FROM library_songs ORDER BY dateAdded DESC")
    fun observeAll(): Flow<List<LibrarySong>>

    @Query("SELECT EXISTS(SELECT 1 FROM library_songs WHERE id = :id)")
    suspend fun exists(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: LibrarySong)

    @Query("DELETE FROM library_songs WHERE id = :id")
    suspend fun deleteById(id: String)

    /** One-time cleanup: blanks every row's artwork so wrong guesses show the placeholder instead. */
    @Query("UPDATE library_songs SET artworkUrl = NULL")
    suspend fun clearAllArtwork()
}
