package com.youtubestream.app.ui.discover

import com.youtubestream.app.data.local.LibrarySong
import com.youtubestream.app.data.local.PlayEvent

/**
 * The videoId of the most-recently-played song that has one — the seed for the "related" shelf.
 * Songs are keyed by filename now, so we resolve each play to its library row and read its videoId
 * (imported/Pi-only songs have null → skipped). null → no eligible seed, shelf omitted. Pure: JVM-tested.
 */
fun selectRelatedSeed(history: List<PlayEvent>, songs: List<LibrarySong>): String? {
    val videoIdById = songs.associate { it.id to it.videoId }
    return history.sortedByDescending { it.playedAt }
        .firstNotNullOfOrNull { videoIdById[it.songId] }
}
