package com.youtubestream.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {
    // --- episodes ---
    @Query("SELECT * FROM podcast_episodes ORDER BY dateAdded DESC")
    fun observeDownloadedEpisodes(): Flow<List<PodcastEpisode>>

    @Query("SELECT * FROM podcast_episodes WHERE resumePositionMs > 0 AND isFinished = 0 ORDER BY lastPlayedAt DESC")
    fun observeContinueListening(): Flow<List<PodcastEpisode>>

    @Query("SELECT EXISTS(SELECT 1 FROM podcast_episodes WHERE id = :id)")
    suspend fun episodeExists(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisode(episode: PodcastEpisode)

    @Query("DELETE FROM podcast_episodes WHERE id = :id")
    suspend fun deleteEpisodeById(id: String)

    @Query("UPDATE podcast_episodes SET resumePositionMs = :ms, lastPlayedAt = :playedAt WHERE id = :id")
    suspend fun updateResumePosition(id: String, ms: Long, playedAt: Long)

    @Query("UPDATE podcast_episodes SET isFinished = 1 WHERE id = :id")
    suspend fun markFinished(id: String)

    // --- followed shows ---
    @Query("SELECT * FROM followed_shows ORDER BY dateFollowed DESC")
    fun observeFollowedShows(): Flow<List<FollowedShow>>

    @Query("SELECT EXISTS(SELECT 1 FROM followed_shows WHERE showId = :showId)")
    fun observeIsFollowing(showId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun follow(show: FollowedShow)

    @Query("DELETE FROM followed_shows WHERE showId = :showId")
    suspend fun unfollow(showId: String)

    @Query("UPDATE followed_shows SET lastSeenEpisodeVideoId = :videoId WHERE showId = :showId")
    suspend fun updateLastSeenEpisode(showId: String, videoId: String)
}
