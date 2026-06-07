package com.youtubestream.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentSearchDao {
    /** REPLACE on the `query` PK = dedup + bump recency in one statement. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: RecentSearch)

    /** Newest first, capped. Reactive: re-emits whenever a query is saved or removed. */
    @Query("SELECT * FROM recent_searches ORDER BY usedAt DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<RecentSearch>>

    @Query("DELETE FROM recent_searches WHERE `query` = :value")
    suspend fun delete(value: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clear()
}
