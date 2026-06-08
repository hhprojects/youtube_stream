package com.youtubestream.app.playlist

import com.youtubestream.app.data.local.PlaylistSong

/** Pure helper: the membership rows to insert when appending songs to a playlist. */
object PlaylistAppend {
    /**
     * Rows to append after the current highest position [maxPosition] (which is -1 for an empty
     * playlist, matching `PlaylistDao.maxPosition`). Positions run contiguously from `maxPosition + 1`
     * in list order; the DAO's IGNORE-on-conflict skips songs already present, leaving harmless gaps.
     */
    fun appendedMembers(playlistId: Long, songIds: List<String>, maxPosition: Int, now: Long): List<PlaylistSong> =
        songIds.mapIndexed { i, id -> PlaylistSong(playlistId, id, maxPosition + 1 + i, now) }
}
