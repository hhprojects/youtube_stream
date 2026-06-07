package com.youtubestream.app.ui.discover

import com.youtubestream.app.data.local.PlayEvent

private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")

/**
 * The most-recently-played song id that is a real YouTube videoId (11 chars). Imported songs key on
 * a filename (e.g. "Song.m4a") and are skipped. null → no eligible seed, so the related shelf is omitted.
 * Pure: zero Android imports → JVM-tested.
 */
fun selectRelatedSeed(history: List<PlayEvent>): String? =
    history.sortedByDescending { it.playedAt }
        .map { it.songId }
        .firstOrNull { VIDEO_ID.matches(it) }
