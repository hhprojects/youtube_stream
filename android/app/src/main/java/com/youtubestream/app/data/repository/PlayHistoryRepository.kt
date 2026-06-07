package com.youtubestream.app.data.repository

import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.local.PlayEvent
import com.youtubestream.app.data.local.PlayEventDao
import kotlinx.coroutines.flow.Flow

/** Records plays and exposes the play-history stream. On-device only — works fully offline. */
class PlayHistoryRepository(private val dao: PlayEventDao) {

    suspend fun record(songId: String, at: Long) =
        dao.insert(PlayEvent(songId = songId, playedAt = at))

    fun observe(): Flow<List<PlayEvent>> = dao.observeAll()

    /** "Recently played" smart playlist: distinct in-library songs, most-recent first. */
    fun observeRecentlyPlayed(limit: Int = 100): Flow<List<LibrarySong>> = dao.observeRecentlyPlayed(limit)

    /** "Most played" smart playlist: in-library songs by play count, then recency. */
    fun observeMostPlayed(limit: Int = 100): Flow<List<LibrarySong>> = dao.observeMostPlayed(limit)
}
