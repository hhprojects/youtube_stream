package com.youtubestream.app.data.repository

import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.local.Playlist
import com.youtubestream.app.data.local.PlaylistDao
import com.youtubestream.app.data.local.PlaylistSong
import com.youtubestream.app.data.local.PlaylistSummary
import kotlinx.coroutines.flow.Flow

/**
 * Manual-playlist data access. Timestamps are passed in (no clock here) so callers stay testable —
 * same convention as PlayHistoryRepository.record(id, now).
 */
class PlaylistRepository(private val dao: PlaylistDao) {

    fun observeSummaries(): Flow<List<PlaylistSummary>> = dao.observeSummaries()

    fun observePlaylist(id: Long): Flow<Playlist?> = dao.observePlaylist(id)

    fun observeSongs(playlistId: Long): Flow<List<LibrarySong>> = dao.observeSongs(playlistId)

    suspend fun create(name: String, now: Long): Long =
        dao.insertPlaylist(Playlist(name = name, dateCreated = now, dateModified = now))

    suspend fun rename(id: Long, name: String, now: Long) = dao.rename(id, name, now)

    suspend fun setCover(id: Long, url: String?, now: Long) = dao.setCover(id, url, now)

    suspend fun delete(id: Long) = dao.deletePlaylist(id)

    /** Appends to the end; re-adding a song already in the playlist is a no-op (composite PK + IGNORE). */
    suspend fun addSong(playlistId: Long, songId: String, now: Long) {
        val nextPos = dao.maxPosition(playlistId) + 1
        dao.insertMember(PlaylistSong(playlistId, songId, nextPos, now))
    }

    suspend fun removeSong(playlistId: Long, songId: String) = dao.deleteMember(playlistId, songId)

    /** Persist a new order produced by PlaylistReorder.reorder(...). */
    suspend fun setOrder(playlistId: Long, orderedSongIds: List<String>) =
        dao.setOrder(playlistId, orderedSongIds)
}
