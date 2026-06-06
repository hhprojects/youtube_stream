package com.youtubestream.app.data.repository

import com.youtubestream.app.data.local.PlayEvent
import com.youtubestream.app.data.local.PlayEventDao
import kotlinx.coroutines.flow.Flow

/** Records plays and exposes the play-history stream. On-device only — works fully offline. */
class PlayHistoryRepository(private val dao: PlayEventDao) {

    suspend fun record(songId: String, at: Long) =
        dao.insert(PlayEvent(songId = songId, playedAt = at))

    fun observe(): Flow<List<PlayEvent>> = dao.observeAll()
}
